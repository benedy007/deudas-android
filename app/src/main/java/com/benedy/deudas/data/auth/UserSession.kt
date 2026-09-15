package com.benedy.deudas.data.auth

/**
 * Sesión local del usuario (Google o invitado/modo prueba).
 * Persistida en SharedPreferences (V1). Más adelante puede migrarse a DataStore.
 */
data class UserSession(
    val idToken: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isGuest: Boolean = false
)
