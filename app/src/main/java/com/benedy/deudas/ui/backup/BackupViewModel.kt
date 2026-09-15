package com.benedy.deudas.ui.backup

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.auth.AuthRepository
import com.benedy.deudas.data.backup.BackupPayload
import com.benedy.deudas.data.backup.DriveAuthHelper
import com.benedy.deudas.data.backup.DriveBackupException
import com.benedy.deudas.data.backup.DriveBackupRepository
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,
    val pendingConsent: IntentSender? = null,
    val showRestoreConfirm: Boolean = false
)

enum class BackupAction {
    BACKUP,
    RESTORE
}

class BackupViewModel(
    private val authRepository: AuthRepository,
    private val crmRepository: DebtCrmRepository,
    private val driveAuth: DriveAuthHelper = DriveAuthHelper(),
    private val driveBackup: DriveBackupRepository = DriveBackupRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private var pendingAction: BackupAction? = null

    val isGuest: Boolean
        get() = authRepository.session.value?.isGuest == true

    val isGoogleSignedIn: Boolean
        get() = authRepository.session.value?.let { !it.isGuest } == true

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null, isError = false) }
    }

    fun clearPendingConsent() {
        _uiState.update { it.copy(pendingConsent = null) }
    }

    fun requestBackup(activity: Activity) {
        if (!ensureGoogleUser()) return
        startAuthorizedAction(activity, BackupAction.BACKUP)
    }

    fun requestRestoreConfirm() {
        if (!ensureGoogleUser()) return
        _uiState.update { it.copy(showRestoreConfirm = true) }
    }

    fun dismissRestoreConfirm() {
        _uiState.update { it.copy(showRestoreConfirm = false) }
    }

    fun confirmRestore(activity: Activity) {
        _uiState.update { it.copy(showRestoreConfirm = false) }
        if (!ensureGoogleUser()) return
        startAuthorizedAction(activity, BackupAction.RESTORE)
    }

    fun onAuthorizationResult(activity: Activity, data: Intent?) {
        val action = pendingAction
        if (action == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, pendingConsent = null, statusMessage = null) }
            when (val outcome = driveAuth.resultFromIntent(activity, data)) {
                is DriveAuthHelper.AuthOutcome.Success -> runDriveAction(action, outcome.accessToken)
                is DriveAuthHelper.AuthOutcome.Error -> {
                    _uiState.update {
                        it.copy(isBusy = false, isError = true, statusMessage = outcome.message)
                    }
                }
                is DriveAuthHelper.AuthOutcome.NeedsUserConsent -> {
                    _uiState.update {
                        it.copy(isBusy = false, isError = true, statusMessage = "Se requiere autorización.")
                    }
                }
            }
        }
    }

    fun onAuthorizationCancelled() {
        pendingAction = null
        _uiState.update {
            it.copy(
                isBusy = false,
                pendingConsent = null,
                isError = true,
                statusMessage = "Autorización de Drive cancelada."
            )
        }
    }

    private fun ensureGoogleUser(): Boolean {
        if (isGuest || !isGoogleSignedIn) {
            _uiState.update {
                it.copy(
                    isError = true,
                    statusMessage = "Conecta con Google para usar el respaldo en Drive."
                )
            }
            return false
        }
        return true
    }

    private fun startAuthorizedAction(activity: Activity, action: BackupAction) {
        pendingAction = action
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            when (val outcome = driveAuth.authorize(activity)) {
                is DriveAuthHelper.AuthOutcome.Success -> runDriveAction(action, outcome.accessToken)
                is DriveAuthHelper.AuthOutcome.NeedsUserConsent -> {
                    _uiState.update {
                        it.copy(isBusy = false, pendingConsent = outcome.intentSender)
                    }
                }
                is DriveAuthHelper.AuthOutcome.Error -> {
                    pendingAction = null
                    _uiState.update {
                        it.copy(isBusy = false, isError = true, statusMessage = outcome.message)
                    }
                }
            }
        }
    }

    private suspend fun runDriveAction(action: BackupAction, accessToken: String) {
        try {
            when (action) {
                BackupAction.BACKUP -> {
                    val payload = crmRepository.exportBackupPayload()
                    withContext(Dispatchers.IO) {
                        driveBackup.uploadBackup(accessToken, payload.toJson())
                    }
                    val count = payload.clients.size + payload.products.size +
                        payload.debts.size + payload.payments.size
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            isError = false,
                            statusMessage = "Respaldo guardado en Google Drive ($count registros)."
                        )
                    }
                }
                BackupAction.RESTORE -> {
                    val json = withContext(Dispatchers.IO) {
                        driveBackup.downloadBackup(accessToken)
                    }
                    val payload = BackupPayload.fromJson(json)
                    crmRepository.importBackupPayload(payload)
                    val count = payload.clients.size + payload.products.size +
                        payload.debts.size + payload.payments.size
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            isError = false,
                            statusMessage = "Datos restaurados desde Google Drive ($count registros)."
                        )
                    }
                }
            }
        } catch (e: DriveBackupException) {
            _uiState.update {
                it.copy(isBusy = false, isError = true, statusMessage = e.message)
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isBusy = false,
                    isError = true,
                    statusMessage = e.message ?: "Error en el respaldo"
                )
            }
        } finally {
            pendingAction = null
        }
    }
}

class BackupViewModelFactory(
    private val authRepository: AuthRepository,
    private val crmRepository: DebtCrmRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            return BackupViewModel(authRepository, crmRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
