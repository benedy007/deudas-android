package com.benedy.deudas.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.benedy.deudas.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Repositorio de autenticación: Google Identity (Credential Manager) + modo invitado.
 * Sin Firebase. La sesión se guarda localmente para el auth gate de la app.
 *
 * WEB_CLIENT_ID (tipo Web / serverClientId) llega vía BuildConfig desde local.properties.
 * Drive: scopes adicionales vía AuthorizationClient (DriveAuthHelper) al respaldar.
 */
class AuthRepository(appContext: Context) {

    private val appContext = appContext.applicationContext

    private val prefs: SharedPreferences =
        this.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val credentialManager = CredentialManager.create(this.appContext)

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    val isSignedIn: Boolean
        get() = _session.value != null

    /**
     * Sesión de invitado / modo prueba, sin Google.
     */
    fun signInAsGuest(): UserSession {
        val session = UserSession(
            idToken = GUEST_TOKEN,
            displayName = "Invitado",
            email = null,
            photoUrl = null,
            isGuest = true
        )
        saveSession(session)
        _session.value = session
        return session
    }

    /**
     * Google Sign-In vía Credential Manager.
     * 1) GetGoogleIdOption (cuentas en el dispositivo)
     * 2) Si no hay credencial → GetSignInWithGoogleOption (botón / flujo completo)
     *
     * @param activityContext debe ser una Activity (Credential Manager muestra UI).
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<UserSession> {
        val clientId = BuildConfig.WEB_CLIENT_ID
        if (clientId.isBlank() ||
            clientId.startsWith("YOUR_WEB_CLIENT_ID") ||
            clientId.contains("dummyplaceholder") ||
            clientId.contains("xxxxxxxx")
        ) {
            return Result.failure(
                AuthException.Failed(
                    "WEB_CLIENT_ID no configurado. Añádelo en local.properties."
                )
            )
        }

        return try {
            requestGoogleIdCredential(activityContext, clientId, filterAuthorized = false)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(AuthException.Cancelled)
        } catch (e: NoCredentialException) {
            // Fallback: flujo explícito "Sign in with Google"
            try {
                requestSignInWithGoogle(activityContext, clientId)
            } catch (e2: GetCredentialCancellationException) {
                Result.failure(AuthException.Cancelled)
            } catch (e2: NoCredentialException) {
                Result.failure(AuthException.NoCredential)
            } catch (e2: GetCredentialException) {
                Result.failure(AuthException.Failed(e2.message ?: "Error de credenciales"))
            } catch (e2: Exception) {
                Result.failure(AuthException.Failed(e2.message ?: "Error desconocido"))
            }
        } catch (e: GetCredentialException) {
            Result.failure(AuthException.Failed(e.message ?: "Error de credenciales"))
        } catch (e: Exception) {
            Result.failure(AuthException.Failed(e.message ?: "Error desconocido"))
        }
    }

    private suspend fun requestGoogleIdCredential(
        activityContext: Context,
        clientId: String,
        filterAuthorized: Boolean
    ): Result<UserSession> {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterAuthorized)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .setNonce(UUID.randomUUID().toString())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = activityContext
        )
        return handleCredential(result.credential)
    }

    private suspend fun requestSignInWithGoogle(
        activityContext: Context,
        clientId: String
    ): Result<UserSession> {
        val option = GetSignInWithGoogleOption.Builder(clientId)
            .setNonce(UUID.randomUUID().toString())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = activityContext
        )
        return handleCredential(result.credential)
    }

    private fun handleCredential(
        credential: androidx.credentials.Credential
    ): Result<UserSession> {
        return when {
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleId = GoogleIdTokenCredential.createFrom(credential.data)
                    val session = UserSession(
                        idToken = googleId.idToken,
                        displayName = googleId.displayName,
                        email = googleId.id,
                        photoUrl = googleId.profilePictureUri?.toString(),
                        isGuest = false
                    )
                    saveSession(session)
                    _session.value = session
                    Result.success(session)
                } catch (e: GoogleIdTokenParsingException) {
                    Result.failure(AuthException.Failed("No se pudo leer el token de Google"))
                }
            }
            else -> Result.failure(AuthException.Failed("Tipo de credencial no soportado"))
        }
    }

    suspend fun signOut() {
        val wasGuest = _session.value?.isGuest == true
        if (!wasGuest) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
                // Continuar con limpieza local aunque falle el clear remoto
            }
        }
        clearSession()
        _session.value = null
    }

    private fun saveSession(session: UserSession) {
        prefs.edit()
            .putString(KEY_ID_TOKEN, session.idToken)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_PHOTO_URL, session.photoUrl)
            .putBoolean(KEY_IS_GUEST, session.isGuest)
            .apply()
    }

    private fun loadSession(): UserSession? {
        val token = prefs.getString(KEY_ID_TOKEN, null) ?: return null
        val isGuest = prefs.getBoolean(KEY_IS_GUEST, token == GUEST_TOKEN)
        return UserSession(
            idToken = token,
            displayName = prefs.getString(KEY_DISPLAY_NAME, null),
            email = prefs.getString(KEY_EMAIL, null),
            photoUrl = prefs.getString(KEY_PHOTO_URL, null),
            isGuest = isGuest
        )
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "deudas_auth"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHOTO_URL = "photo_url"
        private const val KEY_IS_GUEST = "is_guest"
        const val GUEST_TOKEN = "guest"
    }
}

sealed class AuthException(message: String) : Exception(message) {
    data object Cancelled : AuthException("Inicio de sesión cancelado")
    data object NoCredential : AuthException(
        "No hay cuentas de Google disponibles. Configura una cuenta en el dispositivo."
    )
    data class Failed(val detail: String) : AuthException(detail)
}
