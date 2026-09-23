package com.benedy.deudas.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.repository.DebtCrmRepository
import com.benedy.deudas.data.settings.CompanySettings
import com.benedy.deudas.data.settings.SettingsRepository
import com.benedy.deudas.ui.util.CrmTextExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val companyName: String = "",
    val companyPhone: String = "",
    val receiptFooter: String = "",
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val exporting: Boolean = false,
    /** One-shot snackbar / status for text export. */
    val exportStatus: String? = null,
    val exportError: Boolean = false,
    /** When set, UI should launch share sheet then clear. */
    val pendingShare: CrmTextExport.Result? = null
)

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val crm: DebtCrmRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo.settings.collect { s ->
                // Only hydrate form once so typing isn't overwritten by Flow emissions
                if (!_ui.value.loaded) {
                    _ui.update {
                        it.copy(
                            companyName = s.companyName,
                            companyPhone = s.companyPhone,
                            receiptFooter = s.receiptFooter,
                            loaded = true
                        )
                    }
                }
            }
        }
    }

    fun onCompanyName(v: String) = _ui.update { it.copy(companyName = v, saved = false, error = null) }
    fun onCompanyPhone(v: String) = _ui.update { it.copy(companyPhone = v, saved = false, error = null) }
    fun onReceiptFooter(v: String) = _ui.update { it.copy(receiptFooter = v, saved = false, error = null) }

    fun save() {
        val s = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null, saved = false) }
            try {
                repo.save(
                    CompanySettings(
                        companyName = s.companyName,
                        companyPhone = s.companyPhone,
                        receiptFooter = s.receiptFooter
                    )
                )
                _ui.update { it.copy(saving = false, saved = true) }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        saving = false,
                        error = e.message?.takeIf { m -> m.isNotBlank() } ?: "save"
                    )
                }
            }
        }
    }

    fun clearSavedFlag() = _ui.update { it.copy(saved = false) }

    fun clearExportStatus() = _ui.update {
        it.copy(exportStatus = null, exportError = false)
    }

    fun clearPendingShare() = _ui.update { it.copy(pendingShare = null) }

    /**
     * Builds a readable .txt from Room CRM data, writes to cache (+ Downloads when possible),
     * then signals the UI to open the share sheet.
     */
    fun exportToText(context: Context) {
        if (_ui.value.exporting) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    exporting = true,
                    exportStatus = null,
                    exportError = false,
                    pendingShare = null
                )
            }
            try {
                val result = withContext(Dispatchers.IO) {
                    val payload = crm.exportBackupPayload()
                    val report = CrmTextExport.buildReport(
                        clients = payload.clients,
                        debts = payload.debts,
                        payments = payload.payments,
                        products = payload.products
                    )
                    CrmTextExport.writeAndPrepareShare(appContext, report)
                }
                _ui.update {
                    it.copy(
                        exporting = false,
                        exportStatus = "Archivo listo",
                        exportError = false,
                        pendingShare = result
                    )
                }
            } catch (e: Exception) {
                val msg = e.message?.takeIf { it.isNotBlank() }
                    ?: "No se pudo exportar"
                _ui.update {
                    it.copy(
                        exporting = false,
                        exportStatus = msg,
                        exportError = true,
                        pendingShare = null
                    )
                }
            }
        }
    }
}

class SettingsViewModelFactory(
    private val repo: SettingsRepository,
    private val crm: DebtCrmRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(repo, crm) as T
}
