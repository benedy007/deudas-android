package com.benedy.deudas.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.settings.CompanySettings
import com.benedy.deudas.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val companyName: String = "",
    val companyPhone: String = "",
    val receiptFooter: String = "",
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(
    private val repo: SettingsRepository
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
}

class SettingsViewModelFactory(
    private val repo: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(repo) as T
}
