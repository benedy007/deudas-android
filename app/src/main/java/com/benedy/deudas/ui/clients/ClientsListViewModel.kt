package com.benedy.deudas.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.local.entity.PaymentEntity
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ClientSortMode {
    DEBT_HIGH_TO_LOW,
    NAME_AZ,
    FECHA_ENTREGA
}

data class ClientListItem(
    val client: ClientEntity,
    val totalRemaining: Double,
    /** Earliest pending debt fechaEntrega (null if none). */
    val earliestFechaEntrega: Long?,
    /** Open debt older than 1 month (createdAt) or overdue fechaEntrega. */
    val isOverdue: Boolean,
    /** Payment/abono in the last 14 days. */
    val hasRecentPayment: Boolean
)

class ClientsListViewModel(
    repo: DebtCrmRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sortMode = MutableStateFlow(ClientSortMode.NAME_AZ)

    val query: StateFlow<String> = searchQuery
    val sort: StateFlow<ClientSortMode> = sortMode

    val clients: StateFlow<List<ClientListItem>> = combine(
        repo.observeClients(),
        repo.observeAllDebts(),
        repo.observeAllPayments(),
        searchQuery,
        sortMode
    ) { clients, debts, payments, query, sort ->
        buildClientListItems(clients, debts, payments, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun onSortMode(mode: ClientSortMode) {
        sortMode.value = mode
    }

    companion object {
        private const val ONE_MONTH_MS = 30L * 24 * 60 * 60 * 1000
        private const val FOURTEEN_DAYS_MS = 14L * 24 * 60 * 60 * 1000

        fun buildClientListItems(
            clients: List<ClientEntity>,
            debts: List<DebtEntity>,
            payments: List<PaymentEntity>,
            query: String,
            sort: ClientSortMode,
            now: Long = System.currentTimeMillis()
        ): List<ClientListItem> {
            val overdueCutoff = now - ONE_MONTH_MS
            val recentCutoff = now - FOURTEEN_DAYS_MS

            val debtsByClient = debts.groupBy { it.clientId }
            val paymentsByClient = payments.groupBy { it.clientId }

            val q = query.trim()
            val filtered = if (q.isEmpty()) {
                clients
            } else {
                clients.filter { it.name.contains(q, ignoreCase = true) }
            }

            val items = filtered.map { client ->
                val clientDebts = debtsByClient[client.id].orEmpty()
                val openDebts = clientDebts.filter { it.remainingBalance > 0 }
                val totalRemaining = openDebts.sumOf { it.remainingBalance }
                val earliestFecha = openDebts
                    .mapNotNull { it.fechaEntrega }
                    .minOrNull()
                val isOverdue = openDebts.any { debt ->
                    debt.createdAt < overdueCutoff ||
                        (debt.fechaEntrega != null && debt.fechaEntrega < now)
                }
                val hasRecentPayment = paymentsByClient[client.id].orEmpty()
                    .any { it.createdAt >= recentCutoff }

                ClientListItem(
                    client = client,
                    totalRemaining = totalRemaining,
                    earliestFechaEntrega = earliestFecha,
                    isOverdue = isOverdue,
                    hasRecentPayment = hasRecentPayment
                )
            }

            return when (sort) {
                ClientSortMode.DEBT_HIGH_TO_LOW ->
                    items.sortedByDescending { it.totalRemaining }
                ClientSortMode.NAME_AZ ->
                    items.sortedBy { it.client.name.lowercase() }
                ClientSortMode.FECHA_ENTREGA ->
                    items.sortedWith(
                        compareBy<ClientListItem> { it.earliestFechaEntrega == null }
                            .thenBy { it.earliestFechaEntrega ?: Long.MAX_VALUE }
                            .thenBy { it.client.name.lowercase() }
                    )
            }
        }
    }
}
