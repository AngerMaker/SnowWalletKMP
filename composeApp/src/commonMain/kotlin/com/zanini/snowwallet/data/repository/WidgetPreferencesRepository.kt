package com.zanini.snowwallet.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WidgetPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val SHOW_BALANCE = booleanPreferencesKey("show_balance_widget")
    }

    val showBalance: Flow<Boolean> = dataStore.data
        .map { preferences ->
            // Padrão é mostrar (true)
            preferences[PreferencesKeys.SHOW_BALANCE] ?: true
        }

    suspend fun toggleShowBalance() {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SHOW_BALANCE] ?: true
            preferences[PreferencesKeys.SHOW_BALANCE] = !current
        }
    }
}