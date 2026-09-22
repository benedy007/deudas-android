package com.benedy.deudas.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "company_settings"
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<CompanySettings> = context.settingsDataStore.data.map { prefs ->
        CompanySettings(
            companyName = prefs[KEY_COMPANY_NAME].orEmpty(),
            companyPhone = prefs[KEY_COMPANY_PHONE].orEmpty(),
            receiptFooter = prefs[KEY_RECEIPT_FOOTER].orEmpty()
        )
    }

    /** Locally cached last successful Drive backup time (epoch ms), or null. */
    val lastBackupAtMs: Flow<Long?> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_LAST_BACKUP_AT]
    }

    suspend fun save(settings: CompanySettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_COMPANY_NAME] = settings.companyName.trim()
            prefs[KEY_COMPANY_PHONE] = settings.companyPhone.trim()
            prefs[KEY_RECEIPT_FOOTER] = settings.receiptFooter.trim()
        }
    }

    suspend fun setLastBackupAt(epochMs: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_LAST_BACKUP_AT] = epochMs
        }
    }

    companion object {
        private val KEY_COMPANY_NAME = stringPreferencesKey("company_name")
        private val KEY_COMPANY_PHONE = stringPreferencesKey("company_phone")
        private val KEY_RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")
        private val KEY_LAST_BACKUP_AT = longPreferencesKey("last_backup_at_ms")
    }
}
