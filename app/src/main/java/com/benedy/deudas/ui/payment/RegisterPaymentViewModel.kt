package com.benedy.deudas.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterPaymentUiState(
    val amount: String = "",
    val note: String = "",
    val error: String? = null,
    val paymentId: Long? = null,
    val saving: Boolean = false,
    val amountPrefillDone: Boolean = false
)

class RegisterPaymentViewModel(
    private val repo: DebtCrmRepository,
    private val clientId: Long,
    @Suppress("UNUSED_PARAMETER") preselectedDebtId: Long?
) : ViewModel() {
    /** Open debts oldest-first for waterfall preview. */
    val openDebts: StateFlow<List<DebtEntity>> = repo.observeOpenDebts(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalRemaining: StateFlow<Double> = openDebts
        .map { list -> list.sumOf { it.remainingBalance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    private val _ui = MutableStateFlow(RegisterPaymentUiState())
    val ui: StateFlow<RegisterPaymentUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            totalRemaining.collect { total ->
                if (!_ui.value.amountPrefillDone && total > 0) {
                    _ui.update {
                        it.copy(
                            amount = if (it.amount.isBlank()) formatAmount(total) else it.amount,
                            amountPrefillDone = true
                        )
                    }
                }
            }
        }
    }

    private fun formatAmount(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    }

    fun onAmount(v: String) = _ui.update { it.copy(amount = v, error = null) }
    fun onNote(v: String) = _ui.update { it.copy(note = v) }

    fun fillTotal() {
        val total = totalRemaining.value
        if (total > 0) {
            _ui.update { it.copy(amount = formatAmount(total), error = null) }
        }
    }

    /** True when amount parses and is within (0, totalRemaining]. */
    fun canConfirm(): Boolean {
        val amount = _ui.value.amount.replace(',', '.').toDoubleOrNull() ?: return false
        val total = totalRemaining.value
        return amount > 0 && total > 0 && amount <= total + 1e-9
    }

    fun save() {
        val s = _ui.value
        val amount = s.amount.replace(',', '.').toDoubleOrNull()
        val total = totalRemaining.value
        if (amount == null || amount <= 0) {
            _ui.update { it.copy(error = "amount") }
            return
        }
        if (total <= 0) {
            _ui.update { it.copy(error = "no_debts") }
            return
        }
        if (amount > total + 1e-9) {
            _ui.update { it.copy(error = "over_total") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            try {
                val result = repo.registerClientPaymentWaterfall(
                    clientId = clientId,
                    amount = amount,
                    note = s.note.ifBlank { null }
                )
                _ui.update { it.copy(saving = false, paymentId = result.receiptPaymentId) }
            } catch (e: IllegalArgumentException) {
                val code = when (e.message) {
                    "over_total" -> "over_total"
                    "no_debts" -> "no_debts"
                    else -> "failed"
                }
                _ui.update { it.copy(saving = false, error = code) }
            } catch (_: Exception) {
                _ui.update { it.copy(saving = false, error = "failed") }
            }
        }
    }
}
