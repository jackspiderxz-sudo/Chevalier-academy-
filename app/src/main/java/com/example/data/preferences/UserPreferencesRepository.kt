package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chevalierDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chevalier_academy_preferences"
)

data class UserPreferencesState(
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
    val isLanguageSelectedOnLaunch: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val deviceAlertsEnabled: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE_CODE = stringPreferencesKey("interface_language_code")
        val LANGUAGE_ONBOARDED = booleanPreferencesKey("language_selected_on_launch")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEVICE_ALERTS_ENABLED = booleanPreferencesKey("device_alerts_enabled")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.chevalierDataStore.data.map { prefs ->
        val storedCode = prefs[Keys.LANGUAGE_CODE]
        val hasValidLanguage = storedCode != null && AppLanguage.entries.any { it.code == storedCode }
        UserPreferencesState(
            selectedLanguage = AppLanguage.fromCode(storedCode),
            isLanguageSelectedOnLaunch = (prefs[Keys.LANGUAGE_ONBOARDED] == true) && hasValidLanguage,
            themeMode = AppThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            deviceAlertsEnabled = prefs[Keys.DEVICE_ALERTS_ENABLED] == true
        )
    }

    suspend fun setLanguage(language: AppLanguage, markSelectedOnLaunch: Boolean = false) {
        context.chevalierDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = language.code
            if (markSelectedOnLaunch) {
                prefs[Keys.LANGUAGE_ONBOARDED] = true
            }
        }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.chevalierDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.storageKey
        }
    }

    suspend fun setDeviceAlertsEnabled(enabled: Boolean) {
        context.chevalierDataStore.edit { prefs ->
            prefs[Keys.DEVICE_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun restorePreferences(
        language: AppLanguage,
        isOnboarded: Boolean,
        themeMode: AppThemeMode,
        deviceAlertsEnabled: Boolean = false
    ) {
        context.chevalierDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = language.code
            prefs[Keys.LANGUAGE_ONBOARDED] = isOnboarded
            prefs[Keys.THEME_MODE] = themeMode.storageKey
            prefs[Keys.DEVICE_ALERTS_ENABLED] = deviceAlertsEnabled
        }
    }
}
