package com.madmaxlgndklr.pokedex.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val MUSIC_ON_LAUNCH = booleanPreferencesKey("music_on_launch")

    val musicOnLaunch: Flow<Boolean> = dataStore.data.map { it[MUSIC_ON_LAUNCH] ?: true }

    suspend fun setMusicOnLaunch(enabled: Boolean) {
        dataStore.edit { it[MUSIC_ON_LAUNCH] = enabled }
    }

    private val SEARCH_HISTORY_KEY = stringPreferencesKey("search_history")

    val searchHistory: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[SEARCH_HISTORY_KEY]
            ?.split("|||")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun addSearchHistory(query: String) {
        dataStore.edit { prefs ->
            val current = prefs[SEARCH_HISTORY_KEY]
                ?.split("|||")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val updated = (listOf(query) + current.filter { it != query }).take(10)
            prefs[SEARCH_HISTORY_KEY] = updated.joinToString("|||")
        }
    }

    private val TEAM_KEY = stringPreferencesKey("team")

    val team: Flow<List<Int>> = dataStore.data.map { prefs ->
        prefs[TEAM_KEY]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: emptyList()
    }

    suspend fun setTeam(ids: List<Int>) {
        dataStore.edit { prefs ->
            prefs[TEAM_KEY] = ids.take(6).joinToString(",")
        }
    }
}
