package com.benedy.deudas.data.repository

import com.benedy.deudas.data.local.DeudasDatabase
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.local.entity.PaymentEntity
import com.benedy.deudas.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio local (Room) para clientes, deudas, pagos y productos.
 * Funciona offline; sin Firebase.
 */
class DebtCrmRepository(db: DeudasDatabase) {

    private val clientDao = db.clientDao()
    private val debtDao = db.debtDao()
    private val paymentDao = db.paymentDao()
    private val productDao = db.productDao()

    // --- Clientes ---
    fun observeClients(): Flow<List<ClientEntity>> = clientDao.observeAll()
    fun observeClient(id: Long): Flow<ClientEntity?> = clientDao.observeById(id)
    suspend fun getClient(id: Long): ClientEntity? = clientDao.getById(id)

    suspend fun addClient(name: String, phone: String, notes: String?): Long {
        return clientDao.insert(
            ClientEntity(name = name.trim(), phone = phone.trim(), notes = notes?.trim()?.ifBlank { null })
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
    fun observeTotalRemaining(clientId: Long): Flow<Double> = debtDao.observeTotalRemaining(clientId)
    suspend fun getDebt(id: Long): DebtEntity? = debtDao.getById(id)

    suspend fun addDebt(clientId: Long, description: String, amount: Double): Long {
        val safe = amount.coerceAtLeast(0.0)
        return debtDao.insert(
            DebtEntity(
                clientId = clientId,
                description = description.trim(),
                originalAmount = safe,
                remainingBalance = safe
            )
        )
    }

    // --- Pagos ---
    suspend fun getPayment(id: Long): PaymentEntity? = paymentDao.getById(id)

    /**
     * Registra un pago: reduce el saldo restante (mínimo 0) y guarda historial.
     * @return id del pago creado
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
}
