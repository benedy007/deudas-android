package com.benedy.deudas.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PaymentHistoryViewModel(
    private val repo: DebtCrmRepository,
    clientId: Long? = null
) : ViewModel() {

    val items: StateFlow<List<PaymentHistoryItem>> = if (clientId != null) {
        combine(
            repo.observePaymentsByClient(clientId),
            repo.observeClients()
        ) { payments, clients ->
            groupPaymentsForHistory(payments, clients.associateBy { it.id })
        }
    } else {
        combine(
            repo.observeAllPaymentsNewestFirst(),
            repo.observeClients()
        ) { payments, clients ->
            groupPaymentsForHistory(payments, clients.associateBy { it.id })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    fun deletePayment(receiptPaymentId: Long, onDone: () -> Unit = {}) {
        if (_deleting.value) return
        viewModelScope.launch {
            _deleting.value = true
            try {
                repo.deletePaymentGroup(receiptPaymentId)
                onDone()
            } finally {
                _deleting.value = false
            }
        }
    }
}
