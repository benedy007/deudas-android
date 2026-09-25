package com.benedy.deudas.ui.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.auth.AuthException
import com.benedy.deudas.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            session = authRepository.session.value,
            isSignedIn = authRepository.isSignedIn
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.session.collect { session ->
                _uiState.update {
                    it.copy(
                        session = session,
                        isSignedIn = session != null,
                        isLoading = if (session != null) false else it.isLoading
                    )
                }
            }
        }
    }

    /** Modo prueba / invitado — ruta principal V1, sin Google. */
    fun continueAsGuest() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        authRepository.signInAsGuest()
        _uiState.update {
            it.copy(
                isLoading = false,
                isSignedIn = true,
                errorMessage = null
            )
        }
    }

    /**
     * Prepara el intent de Google Sign-In y marca loading.
     * Devuelve null si falla la configuración (errorMessage ya seteado).
     * El Activity Result launcher debe llamar a [onGoogleIntentResult] al volver.
     */
    fun beginGoogleSignIn(activity: Activity): Intent? {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        return try {
            authRepository.getGoogleSignInIntent(activity)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo crear intent de Google Sign-In: ${e.message}", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = e.message
                        ?: "No se pudo iniciar Google Sign-In. Revisa WEB_CLIENT_ID."
                )
            }
            null
        }
    }

    /**
     * Procesa el resultado del Activity Result de Google Sign-In.
     * Cancelación / fallo SIEMPRE setean [AuthUiState.errorMessage] (nunca silencioso).
     */
    fun onGoogleIntentResult(resultCode: Int, data: Intent?) {
        viewModelScope.launch {
            if (resultCode != Activity.RESULT_OK) {
                Log.w(TAG, "Google Sign-In RESULT_CANCELED/resultCode=$resultCode")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = AuthRepository.CANCEL_AFTER_PICK_MESSAGE
                    )
                }
                return@launch
            }

            val result = authRepository.handleGoogleSignInResult(data)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Google Sign-In exitoso")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedIn = true,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(
                        TAG,
                        "Google Sign-In falló: ${error::class.java.simpleName} ${error.message}",
                        error
                    )
                    val message = when (error) {
                        is AuthException.Cancelled ->
                            error.message ?: AuthRepository.CANCEL_AFTER_PICK_MESSAGE
                        is AuthException.NoCredential ->
                            error.message ?: AuthRepository.CANCEL_AFTER_PICK_MESSAGE
                        is AuthException.Failed -> error.detail
                        else -> error.message
                            ?: "Error al iniciar sesión con Google. Inténtalo de nuevo."
                    }
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = message)
                    }
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signOut()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    session = null,
                    isSignedIn = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
