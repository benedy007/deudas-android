package com.benedy.deudas.data.backup

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.benedy.deudas.BuildConfig
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

/**
 * Incremental Drive authorization via AuthorizationClient (separate from Credential Manager sign-in).
 * Scope: drive.appdata — app-private folder in the user's Drive.
 */
class DriveAuthHelper {

    /**
     * Requests Drive appdata access. Returns either an access token immediately,
     * or an IntentSender that the UI must launch for user consent.
     */
    suspend fun authorize(activity: Activity): AuthOutcome {
        val clientId = BuildConfig.WEB_CLIENT_ID
        if (clientId.isBlank() ||
            clientId.startsWith("YOUR_WEB_CLIENT_ID") ||
            clientId.contains("dummyplaceholder")
        ) {
            return AuthOutcome.Error("WEB_CLIENT_ID no configurado.")
        }

        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
                .build()

            val result = Identity.getAuthorizationClient(activity)
                .authorize(request)
                .await()

            when {
                result.hasResolution() -> {
                    val pending = result.pendingIntent
                        ?: return AuthOutcome.Error("No se pudo iniciar la autorización de Drive.")
                    AuthOutcome.NeedsUserConsent(pending.intentSender)
                }
                !result.accessToken.isNullOrBlank() -> AuthOutcome.Success(result.accessToken!!)
                else -> AuthOutcome.Error("No se obtuvo token de acceso de Drive.")
            }
        } catch (e: Exception) {
            AuthOutcome.Error(e.message ?: "Error al autorizar Google Drive")
        }
    }

    fun resultFromIntent(activity: Activity, data: Intent?): AuthOutcome {
        return try {
            val result: AuthorizationResult = Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (token.isNullOrBlank()) {
                AuthOutcome.Error("Autorización de Drive cancelada o sin token.")
            } else {
                AuthOutcome.Success(token)
            }
        } catch (e: ApiException) {
            AuthOutcome.Error(e.message ?: "Autorización de Drive fallida")
        } catch (e: Exception) {
            AuthOutcome.Error(e.message ?: "Error al procesar autorización")
        }
    }

    sealed class AuthOutcome {
        data class Success(val accessToken: String) : AuthOutcome()
        data class NeedsUserConsent(val intentSender: IntentSender) : AuthOutcome()
        data class Error(val message: String) : AuthOutcome()
    }

    companion object {
        /** https://www.googleapis.com/auth/drive.appdata */
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
