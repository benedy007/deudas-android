package com.benedy.deudas.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReceiptAllocationLine(
    val debtDescription: String,
    val amount: Double
)

data class ReceiptData(
    val clientName: String,
    val clientPhone: String,
    val amount: Double,
    val debtDescription: String,
    val dateMs: Long,
    val remaining: Double,
    val allocations: List<ReceiptAllocationLine> = emptyList()
)

class ReceiptViewModel(
    private val repo: DebtCrmRepository,
    paymentId: Long
) : ViewModel() {
    private val _data = MutableStateFlow<ReceiptData?>(null)
    val data: StateFlow<ReceiptData?> = _data.asStateFlow()

    init {
        viewModelScope.launch {
            val payments = repo.getPaymentGroup(paymentId)
            if (payments.isEmpty()) return@launch
            val client = repo.getClient(payments.first().clientId) ?: return@launch
            val lines = payments.mapNotNull { p ->
                val debt = repo.getDebt(p.debtId) ?: return@mapNotNull null
                ReceiptAllocationLine(
                    debtDescription = debt.description,
                    amount = p.amount
                )
            }
            val totalPaid = payments.sumOf { it.amount }
            val clientRemaining = repo.getTotalRemaining(client.id)
            val concept = if (lines.size <= 1) {
                lines.firstOrNull()?.debtDescription ?: "—"
            } else {
                lines.joinToString(" · ") { "${it.debtDescription} (${formatCompact(it.amount)})" }
            }
            _data.value = ReceiptData(
                clientName = client.name,
                clientPhone = client.phone,
                amount = totalPaid,
                debtDescription = concept,
                dateMs = payments.first().createdAt,
                remaining = clientRemaining,
                allocations = lines
            )
        }
    }

    private fun formatCompact(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else "%.2f".format(v)
    }
}
