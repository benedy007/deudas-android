package com.benedy.deudas.ui.auth

import com.benedy.deudas.data.auth.UserSession

/**
 * Estado de la UI de autenticación.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val session: UserSession? = null,
    val errorMessage: String? = null,
    val isSignedIn: Boolean = false
)
