package com.benedy.deudas.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddProductUiState(
    val name: String = "",
    val price: String = "",
    val notes: String = "",
    val error: String? = null,
    val saved: Boolean = false,
    val saving: Boolean = false
)

class AddProductViewModel(private val repo: DebtCrmRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AddProductUiState())
    val ui: StateFlow<AddProductUiState> = _ui.asStateFlow()

    fun onName(v: String) = _ui.update { it.copy(name = v, error = null) }
    fun onPrice(v: String) = _ui.update { it.copy(price = v, error = null) }
    fun onNotes(v: String) = _ui.update { it.copy(notes = v) }

    fun save() {
        val s = _ui.value
        val price = s.price.replace(',', '.').toDoubleOrNull()
        if (s.name.isBlank() || price == null || price < 0) {
            _ui.update { it.copy(error = if (s.name.isBlank()) "required" else "amount") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(saving = true) }
            repo.addProduct(s.name, price, s.notes.ifBlank { null })
            _ui.update { it.copy(saving = false, saved = true) }
        }
    }
}
