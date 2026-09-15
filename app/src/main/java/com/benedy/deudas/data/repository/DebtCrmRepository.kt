package com.benedy.deudas.data.repository

import androidx.room.withTransaction
import com.benedy.deudas.data.backup.BackupPayload
import com.benedy.deudas.data.local.DeudasDatabase
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.local.entity.PaymentEntity
import com.benedy.deudas.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.min

/**
 * Repositorio local (Room) para clientes, deudas, pagos y productos.
 * Funciona offline; sin Firebase.
 */
class DebtCrmRepository(private val db: DeudasDatabase) {

    private val clientDao = db.clientDao()
    private val debtDao = db.debtDao()
    private val paymentDao = db.paymentDao()
    private val productDao = db.productDao()

    // --- Clientes ---
    fun observeClients(): Flow<List<ClientEntity>> = clientDao.observeAll()
    fun observeClient(id: Long): Flow<ClientEntity?> = clientDao.observeById(id)
    suspend fun getClient(id: Long): ClientEntity? = clientDao.getById(id)

    suspend fun addClient(
        name: String,
        phone: String,
        notes: String?,
        direccionCasa: String? = null,
        lugarTrabajo: String? = null,
        direccionTrabajo: String? = null
    ): Long {
        fun clean(v: String?) = v?.trim()?.ifBlank { null }
        return clientDao.insert(
            ClientEntity(
                name = name.trim(),
                phone = phone.trim(),
                notes = clean(notes),
                direccionCasa = clean(direccionCasa),
                lugarTrabajo = clean(lugarTrabajo),
                direccionTrabajo = clean(direccionTrabajo)
            )
        )
    }

    suspend fun updateClient(client: ClientEntity) = clientDao.update(client)
    suspend fun deleteClient(id: Long) = clientDao.deleteById(id)

    // --- Productos ---
    fun observeProducts(): Flow<List<ProductEntity>> = productDao.observeAll()
    suspend fun getProduct(id: Long): ProductEntity? = productDao.getById(id)

    suspend fun addProduct(name: String, price: Double, notes: String?): Long {
        return productDao.insert(
            ProductEntity(
                name = name.trim(),
                price = price,
                notes = notes?.trim()?.ifBlank { null }
            )
        )
    }

    suspend fun deleteProduct(id: Long) = productDao.deleteById(id)

    // --- Deudas ---
    fun observeDebts(clientId: Long): Flow<List<DebtEntity>> = debtDao.observeByClient(clientId)
    fun observeOpenDebts(clientId: Long): Flow<List<DebtEntity>> = debtDao.observeOpenByClient(clientId)
    fun observeAllDebts(): Flow<List<DebtEntity>> = debtDao.observeAll()
    fun observeTotalRemaining(clientId: Long): Flow<Double> = debtDao.observeTotalRemaining(clientId)
    suspend fun getDebt(id: Long): DebtEntity? = debtDao.getById(id)
    suspend fun getTotalRemaining(clientId: Long): Double = debtDao.getTotalRemaining(clientId)

    suspend fun addDebt(
        clientId: Long,
        description: String,
        amount: Double,
        fechaEntrega: Long? = null
    ): Long {
        val safe = amount.coerceAtLeast(0.0)
        return debtDao.insert(
            DebtEntity(
                clientId = clientId,
                description = description.trim(),
                originalAmount = safe,
                remainingBalance = safe,
                fechaEntrega = fechaEntrega
            )
        )
    }

    // --- Pagos ---
    fun observeAllPayments(): Flow<List<PaymentEntity>> = paymentDao.observeAll()
    fun observeAllPaymentsNewestFirst(): Flow<List<PaymentEntity>> =
        paymentDao.observeAllNewestFirst()
    fun observePaymentsByClient(clientId: Long): Flow<List<PaymentEntity>> =
        paymentDao.observeByClient(clientId)
    suspend fun getPayment(id: Long): PaymentEntity? = paymentDao.getById(id)

    /**
     * Payments that belong to the same receipt as [paymentId]
     * (waterfall group, or the single payment itself).
     */
    suspend fun getPaymentGroup(paymentId: Long): List<PaymentEntity> {
        val payment = paymentDao.getById(paymentId) ?: return emptyList()
        val groupId = payment.groupId
        return if (groupId != null) {
            val grouped = paymentDao.getByGroupId(groupId)
            if (grouped.isNotEmpty()) grouped else listOf(payment)
        } else {
            listOf(payment)
        }
    }

    /**
     * Legacy single-debt payment (kept for compatibility).
     */
    suspend fun registerPayment(
        debtId: Long,
        amount: Double,
        note: String?
    ): Long {
        val debt = debtDao.getById(debtId) ?: error("Deuda no encontrada")
        val paid = amount.coerceAtLeast(0.0)
        val newRemaining = (debt.remainingBalance - paid).coerceAtLeast(0.0)
        debtDao.update(debt.copy(remainingBalance = newRemaining))
        return paymentDao.insert(
            PaymentEntity(
                debtId = debtId,
                clientId = debt.clientId,
                amount = paid,
                note = note?.trim()?.ifBlank { null }
            )
        )
    }

    data class WaterfallResult(
        /** First payment id — used to open the receipt. */
        val receiptPaymentId: Long,
        val groupId: Long,
        val totalPaid: Double,
        val allocations: List<Pair<DebtEntity, Double>>
    )

    /**
     * Applies [amount] to the client's TOTAL open debt, oldest first (createdAt ASC).
     * Creates one PaymentEntity per affected debt, all sharing [groupId].
     * Caps at total remaining (no overpay rows).
     */
    suspend fun registerClientPaymentWaterfall(
        clientId: Long,
        amount: Double,
        note: String?
    ): WaterfallResult {
        val paidRequested = amount.coerceAtLeast(0.0)
        if (paidRequested <= 0) error("Monto inválido")

        return db.withTransaction {
            val openDebts = debtDao.getOpenByClientOldestFirst(clientId)
            if (openDebts.isEmpty()) throw IllegalArgumentException("no_debts")

            val totalOpen = openDebts.sumOf { it.remainingBalance }
            if (paidRequested > totalOpen + 1e-9) {
                throw IllegalArgumentException("over_total")
            }
            var remainingToApply = paidRequested
            val now = System.currentTimeMillis()
            val groupId = now
            val cleanNote = note?.trim()?.ifBlank { null }
            val allocations = mutableListOf<Pair<DebtEntity, Double>>()
            var firstPaymentId: Long? = null

            for (debt in openDebts) {
                if (remainingToApply <= 0.0) break
                val apply = min(remainingToApply, debt.remainingBalance)
                if (apply <= 0.0) continue

                val newBalance = (debt.remainingBalance - apply).coerceAtLeast(0.0)
                debtDao.update(debt.copy(remainingBalance = newBalance))
                val paymentId = paymentDao.insert(
                    PaymentEntity(
                        debtId = debt.id,
                        clientId = clientId,
                        amount = apply,
                        note = cleanNote,
                        createdAt = now,
                        groupId = groupId
                    )
                )
                if (firstPaymentId == null) firstPaymentId = paymentId
                allocations += debt to apply
                remainingToApply -= apply
            }

            val receiptId = firstPaymentId ?: error("No se pudo registrar el pago")
            WaterfallResult(
                receiptPaymentId = receiptId,
                groupId = groupId,
                totalPaid = allocations.sumOf { it.second },
                allocations = allocations
            )
        }
    }

    /**
     * Cobrar con producto: crea deuda por el precio del producto y opcionalmente registra pago parcial/total.
     */
    suspend fun chargeProduct(
        clientId: Long,
        product: ProductEntity,
        payNow: Double = 0.0,
        paymentNote: String? = null
    ): Pair<Long, Long?> {
        val debtId = addDebt(clientId, product.name, product.price)
        val paymentId = if (payNow > 0) {
            registerPayment(debtId, payNow, paymentNote)
        } else null
        return debtId to paymentId
    }

    // --- Backup / restore ---

    suspend fun exportBackupPayload(): BackupPayload {
        return BackupPayload(
            clients = clientDao.getAll(),
            debts = debtDao.getAll(),
            payments = paymentDao.getAll(),
            products = productDao.getAll()
        )
    }

    /**
     * Replaces all local CRM data with the backup snapshot (same IDs).
     * Deletes in FK-safe order, then inserts clients → products → debts → payments.
     */
    suspend fun importBackupPayload(payload: BackupPayload) {
        db.withTransaction {
            paymentDao.deleteAll()
            debtDao.deleteAll()
            clientDao.deleteAll()
            productDao.deleteAll()

            if (payload.clients.isNotEmpty()) clientDao.insertAll(payload.clients)
            if (payload.products.isNotEmpty()) productDao.insertAll(payload.products)
            if (payload.debts.isNotEmpty()) debtDao.insertAll(payload.debts)
            if (payload.payments.isNotEmpty()) paymentDao.insertAll(payload.payments)
        }
    }
}
