package com.benedy.deudas.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.local.entity.PlanFrequency
import com.benedy.deudas.data.local.entity.ProductEntity
import com.benedy.deudas.data.repository.DebtCrmRepository
import com.benedy.deudas.ui.util.suggestedPlanPercent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddDebtUiState(
    val description: String = "",
    val amount: String = "",
    val selectedProductId: Long? = null,
    /** Optional delivery/due date as epoch millis. */
    val fechaEntrega: Long? = null,
    /** Null = Ninguno. Otherwise WEEKLY / BIWEEKLY / MONTHLY. */
    val planFrequency: String? = null,
    /** Percent text when a frequency is selected. */
    val planPercent: String = "",
    val error: String? = null,
    val saved: Boolean = false,
    val saving: Boolean = false
) {
    /** Live cuota = amount * percent/100 when plan is complete. */
    fun calculatedCuota(): Double? {
        val freq = planFrequency ?: return null
        if (freq !in PlanFrequency.ALL) return null
        val amt = amount.replace(',', '.').toDoubleOrNull() ?: return null
        val pct = planPercent.replace(',', '.').toDoubleOrNull() ?: return null
        if (amt <= 0 || pct < 1 || pct > 100) return null
        return amt * (pct / 100.0)
    }
}

class AddDebtViewModel(
    private val repo: DebtCrmRepository,
    private val clientId: Long
) : ViewModel() {
    val products: StateFlow<List<ProductEntity>> = repo.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(AddDebtUiState())
    val ui: StateFlow<AddDebtUiState> = _ui.asStateFlow()

    fun onDescription(v: String) = _ui.update {
        it.copy(description = v, selectedProductId = null, error = null)
    }

    fun onAmount(v: String) = _ui.update { it.copy(amount = v, error = null) }

    fun onFechaEntrega(millis: Long?) = _ui.update { it.copy(fechaEntrega = millis) }

    fun clearFechaEntrega() = _ui.update { it.copy(fechaEntrega = null) }

    fun onPlanFrequency(frequency: String?) {
        _ui.update { state ->
            val nextFreq = frequency?.takeIf { it in PlanFrequency.ALL }
            val suggested = suggestedPlanPercent(nextFreq)
            val nextPercent = when {
                nextFreq == null -> ""
                state.planPercent.isNotBlank() && state.planFrequency != null -> state.planPercent
                suggested != null -> {
                    if (suggested == suggested.toLong().toDouble()) {
                        suggested.toLong().toString()
                    } else {
                        suggested.toString()
                    }
                }
                else -> state.planPercent
            }
            state.copy(
                planFrequency = nextFreq,
                planPercent = nextPercent,
                error = null
            )
        }
    }

    fun onPlanPercent(v: String) = _ui.update { it.copy(planPercent = v, error = null) }

    fun selectProduct(product: ProductEntity) {
        _ui.update {
            it.copy(
                selectedProductId = product.id,
                description = product.name,
                amount = product.price.toString(),
                error = null
            )
        }
    }

    fun save() {
        val s = _ui.value
        val amount = s.amount.replace(',', '.').toDoubleOrNull()
        if (s.description.isBlank() || amount == null || amount <= 0) {
            _ui.update {
                it.copy(error = if (s.description.isBlank()) "required" else "amount")
            }
            return
        }
        val freq = s.planFrequency
        var planPercent: Double? = null
        if (freq != null) {
            val pct = s.planPercent.replace(',', '.').toDoubleOrNull()
            if (pct == null || pct < 1.0 || pct > 100.0) {
                _ui.update { it.copy(error = "plan_percent") }
                return
            }
            planPercent = pct
        }
        viewModelScope.launch {
            _ui.update { it.copy(saving = true) }
            repo.addDebt(
                clientId = clientId,
                description = s.description,
                amount = amount,
                fechaEntrega = s.fechaEntrega,
                planFrequency = freq,
                planPercent = planPercent
            )
            _ui.update { it.copy(saving = false, saved = true) }
        }
    }
}
