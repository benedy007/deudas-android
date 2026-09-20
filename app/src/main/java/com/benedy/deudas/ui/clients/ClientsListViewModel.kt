package com.benedy.deudas.ui.clients

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.local.entity.PaymentEntity
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ClientSortMode {
    DEBT_HIGH_TO_LOW,
    NAME_AZ,
    FECHA_ENTREGA
}

enum class ClientDebtFilter {
    ALL,
    WITH_DEBT,
    WITHOUT_DEBT
}

/**
 * List row model. UI card color priority (ClientsListScreen):
 * 1) [hasRecentPayment] → green (al día)
 * 2) else [isOverdue] → red
 * 3) else default surface
 */
data class ClientListItem(
    val client: ClientEntity,
    val totalRemaining: Double,
    /** Earliest pending debt fechaEntrega (null if none). */
    val earliestFechaEntrega: Long?,
    /** Open debt older than 1 month (createdAt) or overdue fechaEntrega. */
    val isOverdue: Boolean,
    /** Payment/abono in the last 14 days — wins over [isOverdue] for card color. */
    val hasRecentPayment: Boolean
)

class ClientsListViewModel(
    repo: DebtCrmRepository,
    /**
     * When true (Cobrar picker), only clients with total remainingBalance > 0.
     * When false (Ver clientes), show all clients.
     */
    private val onlyWithBalance: Boolean = false
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sortMode = MutableStateFlow(ClientSortMode.NAME_AZ)
    private val debtFilterMode = MutableStateFlow(
        if (onlyWithBalance) ClientDebtFilter.WITH_DEBT else ClientDebtFilter.ALL
    )

    val query: StateFlow<String> = searchQuery
    val sort: StateFlow<ClientSortMode> = sortMode
    val debtFilter: StateFlow<ClientDebtFilter> = debtFilterMode
    val showDebtFilter: Boolean = !onlyWithBalance

    /**
     * Nested combine of at most 3 flows so each transform keeps typed List
     * parameters. The 5-arg [combine] vararg overload packs values into
     * Array<*> and causes ClassCastException when casting to List.
     */
    private val clientsDebtsPayments = combine(
        repo.observeClients(),
        repo.observeAllDebts(),
        repo.observeAllPayments()
    ) { clients, debts, payments ->
        Triple(clients, debts, payments)
    }

    private val querySortFilter = combine(searchQuery, sortMode, debtFilterMode) { query, sort, filter ->
        Triple(query, sort, filter)
    }

    val clients: StateFlow<List<ClientListItem>> = combine(
        clientsDebtsPayments,
        querySortFilter
    ) { data, controls ->
        try {
            buildClientListItems(
                clients = data.first,
                debts = data.second,
                payments = data.third,
                query = controls.first,
                sort = controls.second,
                debtFilter = controls.third,
                onlyWithBalance = onlyWithBalance
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build client list items", e)
            emptyList()
        }
    }.catch { e ->
        Log.e(TAG, "Clients list flow failed", e)
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun onSortMode(mode: ClientSortMode) {
        sortMode.value = mode
    }

    fun onDebtFilter(filter: ClientDebtFilter) {
        if (!onlyWithBalance) {
            debtFilterMode.value = filter
        }
    }

    companion object {
        private const val TAG = "ClientsListVM"
        private const val ONE_MONTH_MS = 30L * 24 * 60 * 60 * 1000
        private const val FOURTEEN_DAYS_MS = 14L * 24 * 60 * 60 * 1000

        fun buildClientListItems(
            clients: List<ClientEntity>,
            debts: List<DebtEntity>,
            payments: List<PaymentEntity>,
            query: String,
            sort: ClientSortMode,
            now: Long = System.currentTimeMillis(),
            onlyWithBalance: Boolean = false,
            debtFilter: ClientDebtFilter = ClientDebtFilter.ALL
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
            }.let { list ->
                when (if (onlyWithBalance) ClientDebtFilter.WITH_DEBT else debtFilter) {
                    ClientDebtFilter.ALL -> list
                    ClientDebtFilter.WITH_DEBT -> list.filter { it.totalRemaining > 0 }
                    ClientDebtFilter.WITHOUT_DEBT -> list.filter { it.totalRemaining <= 0 }
                }
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
