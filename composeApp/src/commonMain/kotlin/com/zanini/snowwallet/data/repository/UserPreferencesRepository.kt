// src/commonMain/kotlin/com/zanini/snowwallet/data/repository/UserPreferencesRepository.kt
package com.zanini.snowwallet.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val SHOW_BALANCE_KEY = booleanPreferencesKey("show_balance")
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    private val LAST_PAYMENT_TYPE_KEY = stringPreferencesKey("last_payment_type")
    private val LAST_PAYMENT_ID_KEY = longPreferencesKey("last_payment_id")

    private val APP_THEME_COLOR_KEY = stringPreferencesKey("app_theme_color")

    val showBalance: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[SHOW_BALANCE_KEY] ?: true }

    val isDarkMode: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[DARK_MODE_KEY] ?: false }

    val appThemeColor: Flow<String> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[APP_THEME_COLOR_KEY] ?: "PURPLE" }

    val lastPaymentOption: Flow<Pair<String, Long>?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map {
            val type = it[LAST_PAYMENT_TYPE_KEY]
            val id = it[LAST_PAYMENT_ID_KEY]
            if (type != null && id != null) type to id else null
        }

    suspend fun updateShowBalance(show: Boolean) {
        dataStore.edit { it[SHOW_BALANCE_KEY] = show }
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }

    suspend fun updateAppThemeColor(colorName: String) {
        dataStore.edit { it[APP_THEME_COLOR_KEY] = colorName }
    }

    suspend fun updateLastPaymentOption(type: String, id: Long) {
        dataStore.edit {
            it[LAST_PAYMENT_TYPE_KEY] = type
            it[LAST_PAYMENT_ID_KEY] = id
        }
    }
}