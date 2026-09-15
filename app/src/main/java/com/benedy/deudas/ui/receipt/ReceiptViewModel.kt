package com.benedy.deudas.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReceiptData(
    val clientName: String,
    val clientPhone: String,
    val amount: Double,
    val debtDescription: String,
    val dateMs: Long,
    val remaining: Double
)

class ReceiptViewModel(
    private val repo: DebtCrmRepository,
    paymentId: Long
) : ViewModel() {
    private val _data = MutableStateFlow<ReceiptData?>(null)
    val data: StateFlow<ReceiptData?> = _data.asStateFlow()

    init {
        viewModelScope.launch {
            val payment = repo.getPayment(paymentId) ?: return@launch
            val debt = repo.getDebt(payment.debtId) ?: return@launch
            val client = repo.getClient(payment.clientId) ?: return@launch
            _data.value = ReceiptData(
                clientName = client.name,
                clientPhone = client.phone,
                amount = payment.amount,
                debtDescription = debt.description,
                dateMs = payment.createdAt,
                remaining = debt.remainingBalance
            )
        }
    }
}
