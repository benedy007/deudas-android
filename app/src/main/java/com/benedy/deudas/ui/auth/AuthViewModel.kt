package com.benedy.deudas.ui.auth

import android.content.Context
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
                        isLoading = false
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

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithGoogle(activityContext)
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
                    Log.e(TAG, "Google Sign-In falló: ${error::class.java.simpleName} ${error.message}", error)
                    // Cancelled se mantiene silencioso; Failed y NoCredential siempre muestran mensaje.
                    val message = when (error) {
                        is AuthException.Cancelled -> null
                        is AuthException.NoCredential -> error.message
                        is AuthException.Failed -> error.detail
                        else -> error.message ?: "Error al iniciar sesión"
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
