package com.benedy.deudas.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.local.entity.ProductEntity
import com.benedy.deudas.data.repository.DebtCrmRepository
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
    val error: String? = null,
    val saved: Boolean = false,
    val saving: Boolean = false
)

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
        viewModelScope.launch {
            _ui.update { it.copy(saving = true) }
            repo.addDebt(clientId, s.description, amount)
            _ui.update { it.copy(saving = false, saved = true) }
        }
    }
}
