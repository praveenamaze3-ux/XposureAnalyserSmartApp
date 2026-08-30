package com.example.xposuredetectorsmart.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.xposuredetectorsmart.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val LAST_UNLOCK_TIMESTAMP = longPreferencesKey("last_unlock_timestamp")
        val CURRENT_INDUSTRY_ID = stringPreferencesKey("current_industry_id")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun recordUnlock(nowMillis: Long) {
        dataStore.edit { it[Keys.LAST_UNLOCK_TIMESTAMP] = nowMillis }
    }

    suspend fun isSessionUnlocked(nowMillis: Long): Boolean {
        val last = dataStore.data.first()[Keys.LAST_UNLOCK_TIMESTAMP] ?: return false
        return (nowMillis - last) < Constants.BIOMETRIC_SESSION_TIMEOUT_MS
    }

    /** The industry this device's registration/admin flow is set up for (one per device). */
    suspend fun getCurrentIndustryId(): String? = dataStore.data.first()[Keys.CURRENT_INDUSTRY_ID]

    suspend fun setCurrentIndustryId(industryId: String) {
        dataStore.edit { it[Keys.CURRENT_INDUSTRY_ID] = industryId }
    }
}
