package com.benedy.deudas.ui.clients

import android.util.Log
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
    val direccionCasa: String = "",
    val lugarTrabajo: String = "",
    val direccionTrabajo: String = "",
    val photoPath: String? = null,
    val photoPreviewUri: String? = null,
    val creditLimitText: String = "",
    val saldoInicialText: String = "",
    val isEdit: Boolean = false,
    val editingClientId: Long? = null,
    val loaded: Boolean = true,
    val error: String? = null,
    val savedClientId: Long? = null,
    val saving: Boolean = false
)

class AddClientViewModel(
    private val repo: DebtCrmRepository,
    private val editClientId: Long? = null
) : ViewModel() {
    private val _ui = MutableStateFlow(
        AddClientUiState(
            isEdit = editClientId != null,
            editingClientId = editClientId,
            loaded = editClientId == null
        )
    )
    val ui: StateFlow<AddClientUiState> = _ui.asStateFlow()

    init {
        if (editClientId != null) {
            viewModelScope.launch {
                try {
                    val c = repo.getClient(editClientId)
                    if (c != null) {
                        _ui.update {
                            it.copy(
                                name = c.name,
                                phone = c.phone,
                                notes = c.notes.orEmpty(),
                                direccionCasa = c.direccionCasa.orEmpty(),
                                lugarTrabajo = c.lugarTrabajo.orEmpty(),
                                direccionTrabajo = c.direccionTrabajo.orEmpty(),
                                photoPath = c.photoPath,
                                creditLimitText = c.creditLimit?.let { lim ->
                                    if (lim == lim.toLong().toDouble()) lim.toLong().toString()
                                    else lim.toString()
                                }.orEmpty(),
                                loaded = true
                            )
                        }
                    } else {
                        _ui.update { it.copy(loaded = true, error = "db") }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load client for edit", e)
                    _ui.update { it.copy(loaded = true, error = "db") }
                }
            }
        }
    }

    fun onName(v: String) = _ui.update { it.copy(name = v, error = null) }
    fun onPhone(v: String) = _ui.update { it.copy(phone = v, error = null) }
    fun onNotes(v: String) = _ui.update { it.copy(notes = v) }
    fun onDireccionCasa(v: String) = _ui.update { it.copy(direccionCasa = v) }
    fun onLugarTrabajo(v: String) = _ui.update { it.copy(lugarTrabajo = v) }
    fun onDireccionTrabajo(v: String) = _ui.update { it.copy(direccionTrabajo = v) }
    fun onCreditLimit(v: String) = _ui.update { it.copy(creditLimitText = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) }
    fun onSaldoInicial(v: String) = _ui.update { it.copy(saldoInicialText = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) }

    fun onPhotoChosen(previewUri: String?, persistedRelative: String?) {
        _ui.update {
            it.copy(
                photoPreviewUri = previewUri,
                photoPath = persistedRelative ?: it.photoPath
            )
        }
    }

    fun onPhotoCleared() {
        _ui.update { it.copy(photoPreviewUri = null, photoPath = null) }
    }

    fun save() {
        val s = _ui.value
        if (s.name.isBlank() || s.phone.isBlank()) {
            _ui.update { it.copy(error = "required") }
            return
        }
        val creditLimit = parseMoney(s.creditLimitText)
        val saldoInicial = parseMoney(s.saldoInicialText)
        if (s.creditLimitText.isNotBlank() && creditLimit == null) {
            _ui.update { it.copy(error = "credit") }
            return
        }
        if (s.saldoInicialText.isNotBlank() && saldoInicial == null) {
            _ui.update { it.copy(error = "saldo") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            try {
                val id = if (s.isEdit && s.editingClientId != null) {
                    val existing = repo.getClient(s.editingClientId)
                        ?: error("Cliente no encontrado")
                    repo.updateClient(
                        existing.copy(
                            name = s.name.trim(),
                            phone = s.phone.trim(),
                            notes = s.notes.trim().ifBlank { null },
                            direccionCasa = s.direccionCasa.trim().ifBlank { null },
                            lugarTrabajo = s.lugarTrabajo.trim().ifBlank { null },
                            direccionTrabajo = s.direccionTrabajo.trim().ifBlank { null },
                            photoPath = s.photoPath?.trim()?.ifBlank { null },
                            creditLimit = creditLimit?.takeIf { it > 0 }
                        )
                    )
                    s.editingClientId
                } else {
                    repo.addClient(
                        name = s.name,
                        phone = s.phone,
                        notes = s.notes.ifBlank { null },
                        direccionCasa = s.direccionCasa.ifBlank { null },
                        lugarTrabajo = s.lugarTrabajo.ifBlank { null },
                        direccionTrabajo = s.direccionTrabajo.ifBlank { null },
                        photoPath = s.photoPath,
                        creditLimit = creditLimit?.takeIf { it > 0 },
                        saldoInicial = saldoInicial?.takeIf { it > 0 }
                    )
                }
                _ui.update { it.copy(saving = false, savedClientId = id) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save client (DB?)", e)
                _ui.update { it.copy(saving = false, error = "db") }
            }
        }
    }

    private fun parseMoney(raw: String): Double? {
        val t = raw.trim().replace(',', '.')
        if (t.isEmpty()) return null
        return t.toDoubleOrNull()
    }

    companion object {
        private const val TAG = "AddClientVM"
    }
}
