package com.benedy.deudas.data.repository

import androidx.room.withTransaction
import com.benedy.deudas.data.backup.BackupPayload
import com.benedy.deudas.data.local.DeudasDatabase
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.CobranzaNoteEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.local.entity.PaymentEntity
import com.benedy.deudas.data.local.entity.ProductEntity
import com.benedy.deudas.data.payment.PaymentLifo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
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
    private val cobranzaNoteDao = db.cobranzaNoteDao()

    data class DashboardStats(
        val totalPorCobrar: Double = 0.0,
        val cobradoDelMes: Double = 0.0,
        val clientesEnMora: Int = 0
    )

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
        direccionTrabajo: String? = null,
        photoPath: String? = null,
        creditLimit: Double? = null,
        /** If > 0, creates an opening-balance debt for the new client. */
        saldoInicial: Double? = null,
        saldoInicialDescripcion: String = "Saldo inicial"
    ): Long {
        fun clean(v: String?) = v?.trim()?.ifBlank { null }
        return db.withTransaction {
            val id = clientDao.insert(
                ClientEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    notes = clean(notes),
                    direccionCasa = clean(direccionCasa),
                    lugarTrabajo = clean(lugarTrabajo),
                    direccionTrabajo = clean(direccionTrabajo),
                    photoPath = clean(photoPath),
                    creditLimit = creditLimit?.takeIf { it > 0 }
                )
            )
            val opening = saldoInicial ?: 0.0
            if (opening > 0) {
                debtDao.insert(
                    DebtEntity(
                        clientId = id,
                        description = saldoInicialDescripcion.trim().ifBlank { "Saldo inicial" },
                        originalAmount = opening,
                        remainingBalance = opening
                    )
                )
            }
            id
        }
    }

    suspend fun updateClient(client: ClientEntity) = clientDao.update(client)
    suspend fun deleteClient(id: Long) = clientDao.deleteById(id)

    // --- Cobranza notes ---
    fun observeCobranzaNotes(clientId: Long): Flow<List<CobranzaNoteEntity>> =
        cobranzaNoteDao.observeByClient(clientId)

    suspend fun addCobranzaNote(
        clientId: Long,
        text: String,
        promisedDate: Long? = null
    ): Long {
        val clean = text.trim()
        require(clean.isNotEmpty()) { "Nota vacía" }
        return cobranzaNoteDao.insert(
            CobranzaNoteEntity(
                clientId = clientId,
                text = clean,
                promisedDate = promisedDate
            )
        )
    }

    suspend fun deleteCobranzaNote(id: Long) = cobranzaNoteDao.deleteById(id)

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

    /**
     * Returns true if adding [amount] would push the client's open balance over creditLimit.
     * No limit configured → never exceeds.
     */
    suspend fun wouldExceedCreditLimit(clientId: Long, additionalAmount: Double): Boolean {
        val client = clientDao.getById(clientId) ?: return false
        val limit = client.creditLimit ?: return false
        if (limit <= 0) return false
        val current = debtDao.getTotalRemaining(clientId)
        return (current + additionalAmount) > limit + 1e-9
    }

    suspend fun addDebt(
        clientId: Long,
        description: String,
        amount: Double,
        fechaEntrega: Long? = null,
        planFrequency: String? = null,
        planPercent: Double? = null
    ): Long {
        val safe = amount.coerceAtLeast(0.0)
        val freq = planFrequency?.takeIf { it.isNotBlank() }
        val pct = if (freq != null) planPercent else null
        return debtDao.insert(
            DebtEntity(
                clientId = clientId,
                description = description.trim(),
                originalAmount = safe,
                remainingBalance = safe,
                fechaEntrega = fechaEntrega,
                planFrequency = freq,
                planPercent = pct
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
        val receiptPaymentId: Long,
        val groupId: Long,
        val totalPaid: Double,
        val allocations: List<Pair<DebtEntity, Double>>
    )

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

    suspend fun canDeletePayment(paymentId: Long): Boolean {
        val payment = paymentDao.getById(paymentId) ?: return false
        val clientPayments = paymentDao.getByClient(payment.clientId)
        return PaymentLifo.isDeletable(paymentId, clientPayments)
    }

    suspend fun deletePaymentGroup(paymentId: Long) {
        db.withTransaction {
            val seed = paymentDao.getById(paymentId) ?: return@withTransaction
            val clientPayments = paymentDao.getByClient(seed.clientId)
            if (!PaymentLifo.isDeletable(paymentId, clientPayments)) {
                error(PaymentLifo.NOT_LATEST_MESSAGE)
            }
            val payments = getPaymentGroup(paymentId)
            if (payments.isEmpty()) return@withTransaction
            for (payment in payments) {
                val debt = debtDao.getById(payment.debtId) ?: continue
                val restored = (debt.remainingBalance + payment.amount)
                    .coerceAtMost(debt.originalAmount)
                debtDao.update(debt.copy(remainingBalance = restored))
            }
            val groupId = payments.firstOrNull()?.groupId
            if (groupId != null && payments.any { it.groupId == groupId }) {
                paymentDao.deleteByGroupId(groupId)
            } else {
                for (p in payments) {
                    paymentDao.deleteById(p.id)
                }
            }
        }
    }

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

    // --- Dashboard ---

    fun observeDashboardStats(): Flow<DashboardStats> {
        return combine(
            debtDao.observeAll(),
            paymentDao.observeAll(),
            clientDao.observeAll()
        ) { debts, payments, _ ->
            val now = System.currentTimeMillis()
            val monthStart = startOfMonthMs(now)
            val overdueCutoff = now - 30L * 24 * 60 * 60 * 1000
            val totalPorCobrar = debts.sumOf { it.remainingBalance.coerceAtLeast(0.0) }
            val cobradoDelMes = payments
                .filter { it.createdAt >= monthStart }
                .sumOf { it.amount }
            val openByClient = debts
                .filter { it.remainingBalance > 0 }
                .groupBy { it.clientId }
            val clientesEnMora = openByClient.count { (_, clientDebts) ->
                clientDebts.any { debt ->
                    debt.createdAt < overdueCutoff ||
                        (debt.fechaEntrega != null && debt.fechaEntrega < now)
                }
            }
            DashboardStats(
                totalPorCobrar = totalPorCobrar,
                cobradoDelMes = cobradoDelMes,
                clientesEnMora = clientesEnMora
            )
        }
    }

    // --- Statement data ---

    data class StatementLine(
        val dateMs: Long,
        val label: String,
        val amount: Double,
        val isCredit: Boolean
    )

    data class ClientStatement(
        val client: ClientEntity,
        val lines: List<StatementLine>,
        val totalDebt: Double,
        val totalPaid: Double,
        val remaining: Double
    )

    suspend fun buildClientStatement(clientId: Long): ClientStatement? {
        val client = clientDao.getById(clientId) ?: return null
        val debts = debtDao.getAll().filter { it.clientId == clientId }
        val payments = paymentDao.getAll().filter { it.clientId == clientId }
        val lines = mutableListOf<StatementLine>()
        debts.forEach { d ->
            lines += StatementLine(
                dateMs = d.createdAt,
                label = "Deuda: ${d.description}",
                amount = d.originalAmount,
                isCredit = false
            )
        }
        payments.forEach { p ->
            val debtDesc = debts.find { it.id == p.debtId }?.description ?: "Pago"
            lines += StatementLine(
                dateMs = p.createdAt,
                label = "Pago · $debtDesc",
                amount = p.amount,
                isCredit = true
            )
        }
        lines.sortBy { it.dateMs }
        val totalDebt = debts.sumOf { it.originalAmount }
        val totalPaid = payments.sumOf { it.amount }
        val remaining = debts.sumOf { it.remainingBalance.coerceAtLeast(0.0) }
        return ClientStatement(
            client = client,
            lines = lines,
            totalDebt = totalDebt,
            totalPaid = totalPaid,
            remaining = remaining
        )
    }

    // --- Backup / restore ---

    suspend fun exportBackupPayload(): BackupPayload {
        return BackupPayload(
            clients = clientDao.getAll(),
            debts = debtDao.getAll(),
            payments = paymentDao.getAll(),
            products = productDao.getAll(),
            cobranzaNotes = cobranzaNoteDao.getAll()
        )
    }

    suspend fun importBackupPayload(payload: BackupPayload) {
        db.withTransaction {
            cobranzaNoteDao.deleteAll()
            paymentDao.deleteAll()
            debtDao.deleteAll()
            clientDao.deleteAll()
            productDao.deleteAll()

            if (payload.clients.isNotEmpty()) clientDao.insertAll(payload.clients)
            if (payload.products.isNotEmpty()) productDao.insertAll(payload.products)
            if (payload.debts.isNotEmpty()) debtDao.insertAll(payload.debts)
            if (payload.payments.isNotEmpty()) paymentDao.insertAll(payload.payments)
            if (payload.cobranzaNotes.isNotEmpty()) cobranzaNoteDao.insertAll(payload.cobranzaNotes)
        }
    }

    companion object {
        fun startOfMonthMs(now: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
