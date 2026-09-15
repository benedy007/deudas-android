package com.benedy.deudas.ui.clientdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.CobranzaNoteEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.repository.DebtCrmRepository
import com.benedy.deudas.ui.util.formatDate
import com.benedy.deudas.ui.util.formatMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClientDetailViewModel(
    private val repo: DebtCrmRepository,
    private val clientId: Long
) : ViewModel() {
    val client: StateFlow<ClientEntity?> = repo.observeClient(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val debts: StateFlow<List<DebtEntity>> = repo.observeDebts(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalRemaining: StateFlow<Double> = repo.observeTotalRemaining(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val cobranzaNotes: StateFlow<List<CobranzaNoteEntity>> = repo.observeCobranzaNotes(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _noteText = MutableStateFlow("")
    val noteText: StateFlow<String> = _noteText.asStateFlow()
    private val _promisedDateMs = MutableStateFlow<Long?>(null)
    val promisedDateMs: StateFlow<Long?> = _promisedDateMs.asStateFlow()
    private val _noteError = MutableStateFlow<String?>(null)
    val noteError: StateFlow<String?> = _noteError.asStateFlow()
    private val _statementBusy = MutableStateFlow(false)
    val statementBusy: StateFlow<Boolean> = _statementBusy.asStateFlow()

    fun onNoteText(v: String) {
        _noteText.value = v
        _noteError.value = null
    }

    fun onPromisedDate(ms: Long?) {
        _promisedDateMs.value = ms
    }

    fun addCobranzaNote() {
        val text = _noteText.value.trim()
        if (text.isEmpty()) {
            _noteError.value = "required"
            return
        }
        viewModelScope.launch {
            try {
                repo.addCobranzaNote(clientId, text, _promisedDateMs.value)
                _noteText.value = ""
                _promisedDateMs.value = null
                _noteError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "addCobranzaNote failed", e)
                _noteError.value = "db"
            }
        }
    }

    fun deleteCobranzaNote(id: Long) {
        viewModelScope.launch {
            try {
                repo.deleteCobranzaNote(id)
            } catch (e: Exception) {
                Log.e(TAG, "deleteCobranzaNote failed", e)
            }
        }
    }

    fun buildReminderMessage(
        clientName: String,
        remaining: Double,
        debts: List<DebtEntity>,
        companyName: String
    ): String {
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "DO"))
        val open = debts.filter { it.remainingBalance > 0 }
        val earliest = open.mapNotNull { it.fechaEntrega }.minOrNull()
        val brand = companyName.ifBlank { "Deudas" }
        return buildString {
            appendLine("Hola $clientName,")
            appendLine()
            appendLine("Te escribimos de *$brand* para recordarte tu saldo pendiente.")
            appendLine()
            appendLine("Saldo actual: *${formatMoney(remaining)}*")
            if (earliest != null) {
                appendLine("Fecha de entrega / vencimiento: ${dateFmt.format(Date(earliest))}")
            }
            if (open.isNotEmpty()) {
                appendLine()
                appendLine("Detalle:")
                open.take(5).forEach { d ->
                    append("• ${d.description}: ${formatMoney(d.remainingBalance)}")
                    d.fechaEntrega?.let { append(" (entrega ${dateFmt.format(Date(it))})") }
                    appendLine()
                }
                if (open.size > 5) appendLine("… y ${open.size - 5} más")
            }
            appendLine()
            appendLine("¿Cuándo podrías realizar el pago? Quedamos atentos.")
            appendLine()
            append("— $brand")
        }
    }

    suspend fun loadStatement(): DebtCrmRepository.ClientStatement? =
        repo.buildClientStatement(clientId)

    fun setStatementBusy(busy: Boolean) {
        _statementBusy.value = busy
    }

    companion object {
        private const val TAG = "ClientDetailVM"
    }
}
