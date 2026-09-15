package com.benedy.deudas.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterPaymentUiState(
    val selectedDebtId: Long? = null,
    val amount: String = "",
    val note: String = "",
    val error: String? = null,
    val paymentId: Long? = null,
    val saving: Boolean = false
)

class RegisterPaymentViewModel(
    private val repo: DebtCrmRepository,
    clientId: Long,
    preselectedDebtId: Long?
) : ViewModel() {
    val openDebts: StateFlow<List<DebtEntity>> = repo.observeOpenDebts(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(
        RegisterPaymentUiState(selectedDebtId = preselectedDebtId)
    )
    val ui: StateFlow<RegisterPaymentUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            openDebts.collect { debts ->
                val current = _ui.value.selectedDebtId
                if (current == null && debts.isNotEmpty()) {
                    val first = debts.first()
                    _ui.update {
                        it.copy(
                            selectedDebtId = first.id,
                            amount = if (it.amount.isBlank()) first.remainingBalance.toString() else it.amount
                        )
                    }
                } else if (current != null && itAmountBlankForDebt(debts, current)) {
                    val debt = debts.find { it.id == current }
                    if (debt != null && _ui.value.amount.isBlank()) {
                        _ui.update { it.copy(amount = debt.remainingBalance.toString()) }
                    }
                }
            }
        }
    }

    private fun itAmountBlankForDebt(debts: List<DebtEntity>, id: Long) = true

    fun selectDebt(debt: DebtEntity) {
        _ui.update {
            it.copy(
                selectedDebtId = debt.id,
                amount = debt.remainingBalance.toString(),
                error = null
            )
        }
    }

    fun onAmount(v: String) = _ui.update { it.copy(amount = v, error = null) }
    fun onNote(v: String) = _ui.update { it.copy(note = v) }

    fun save() {
        val s = _ui.value
        val debtId = s.selectedDebtId
        val amount = s.amount.replace(',', '.').toDoubleOrNull()
        if (debtId == null || amount == null || amount <= 0) {
            _ui.update { it.copy(error = if (debtId == null) "required" else "amount") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(saving = true) }
            val id = repo.registerPayment(debtId, amount, s.note.ifBlank { null })
            _ui.update { it.copy(saving = false, paymentId = id) }
        }
    }
}
