package com.benedy.deudas.data.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.benedy.deudas.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de autenticación: Google Sign-In clásico (play-services-auth) + modo invitado.
 * Sin Firebase. La sesión se guarda localmente para el auth gate de la app.
 *
 * WEB_CLIENT_ID (tipo Web / serverClientId) llega vía BuildConfig desde local.properties.
 * Drive: scopes adicionales vía AuthorizationClient (DriveAuthHelper) al respaldar.
 */
class AuthRepository(appContext: Context) {

    private val appContext = appContext.applicationContext

    private val prefs: SharedPreferences =
        this.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    val isSignedIn: Boolean
        get() = _session.value != null

    private fun googleSignInOptions(): GoogleSignInOptions {
        val clientId = BuildConfig.WEB_CLIENT_ID
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
    }

    private fun googleSignInClient(context: Context) =
        GoogleSignIn.getClient(context, googleSignInOptions())

    /**
     * Intent de Google Sign-In clásico para lanzar con Activity Result API.
     */
    fun getGoogleSignInIntent(activity: Activity): Intent {
        validateWebClientId()
        return googleSignInClient(activity).signInIntent
    }

    /**
     * Procesa el resultado del intent de Google Sign-In.
     * Usa [GoogleSignIn.getSignedInAccountFromIntent].
     */
    fun handleGoogleSignInResult(data: Intent?): Result<UserSession> {
        return try {
            validateWebClientId()
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                return Result.failure(
                    AuthException.Failed(
                        "Google no devolvió idToken. Revisa WEB_CLIENT_ID (tipo Web) en local.properties."
                    )
                )
            }
            val session = UserSession(
                idToken = idToken,
                displayName = account.displayName,
                email = account.email,
                photoUrl = account.photoUrl?.toString(),
                isGuest = false
            )
            saveSession(session)
            _session.value = session
            Result.success(session)
        } catch (e: ApiException) {
            Log.e(TAG, "GoogleSignIn ApiException status=${e.statusCode} msg=${e.message}", e)
            Result.failure(mapApiException(e))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "GoogleSignIn config: ${e.message}", e)
            Result.failure(AuthException.Failed(e.message ?: "WEB_CLIENT_ID no configurado."))
        } catch (e: Exception) {
            Log.e(TAG, "GoogleSignIn error inesperado: ${e.message}", e)
            Result.failure(
                AuthException.Failed(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "Error al iniciar sesión con Google. Inténtalo de nuevo."
                )
            )
        }
    }

    private fun mapApiException(e: ApiException): AuthException {
        return when (e.statusCode) {
            // 10
            CommonStatusCodes.DEVELOPER_ERROR -> AuthException.Failed(
                "Error de configuración de Google (código 10). " +
                    "El SHA-1 de la firma de Play puede no estar registrado aún; espera unos minutos e inténtalo de nuevo."
            )
            // 12501
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> AuthException.Cancelled
            // 12500
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> AuthException.Failed(
                "Error interno de Google Sign-In. Prueba de nuevo o reinicia la app."
            )
            // 7
            CommonStatusCodes.NETWORK_ERROR,
            CommonStatusCodes.TIMEOUT -> AuthException.Failed(
                "Sin conexión. Comprueba tu internet e inténtalo de nuevo."
            )
            else -> AuthException.Failed(
                e.message?.takeIf { it.isNotBlank() }
                    ?: "Error al iniciar sesión con Google (código ${e.statusCode}). Inténtalo de nuevo."
            )
        }
    }

    private fun validateWebClientId() {
        val clientId = BuildConfig.WEB_CLIENT_ID
        if (clientId.isBlank() ||
            clientId.startsWith("YOUR_WEB_CLIENT_ID") ||
            clientId.contains("dummyplaceholder") ||
            clientId.contains("xxxxxxxx")
        ) {
            throw IllegalStateException(
                "WEB_CLIENT_ID no configurado. Añádelo en local.properties."
            )
        }
    }

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

    suspend fun signOut() {
        val wasGuest = _session.value?.isGuest == true
        if (!wasGuest) {
            try {
                googleSignInClient(appContext).signOut().await()
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
        private const val TAG = "AuthRepository"
        private const val PREFS_NAME = "deudas_auth"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHOTO_URL = "photo_url"
        private const val KEY_IS_GUEST = "is_guest"
        const val GUEST_TOKEN = "guest"

        /** Mensaje visible cuando el picker cierra sin completar el login (no silencioso). */
        const val CANCEL_AFTER_PICK_MESSAGE =
            "Inicio de sesión cancelado o no completado. Si elegiste una cuenta y volviste aquí, reintenta; si sigue fallando, el código de Google puede ser 10 (configuración)."
    }
}

sealed class AuthException(message: String) : Exception(message) {
    data object Cancelled : AuthException(AuthRepository.CANCEL_AFTER_PICK_MESSAGE)
    data object NoCredential : AuthException(
        "No hay cuentas de Google disponibles. Configura una cuenta en el dispositivo."
    )
    data class Failed(val detail: String) : AuthException(detail)
}
