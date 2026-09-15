package com.benedy.deudas.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddClientUiState(
    val name: String = "",
    val phone: String = "",
    val notes: String = "",
    val error: String? = null,
    val savedClientId: Long? = null,
    val saving: Boolean = false
)

class AddClientViewModel(
    private val repo: DebtCrmRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(AddClientUiState())
    val ui: StateFlow<AddClientUiState> = _ui.asStateFlow()

    fun onName(v: String) = _ui.update { it.copy(name = v, error = null) }
    fun onPhone(v: String) = _ui.update { it.copy(phone = v, error = null) }
    fun onNotes(v: String) = _ui.update { it.copy(notes = v) }

    fun save() {
        val s = _ui.value
        if (s.name.isBlank() || s.phone.isBlank()) {
            _ui.update { it.copy(error = "required") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(saving = true) }
            val id = repo.addClient(s.name, s.phone, s.notes.ifBlank { null })
            _ui.update { it.copy(saving = false, savedClientId = id) }
        }
    }
}
