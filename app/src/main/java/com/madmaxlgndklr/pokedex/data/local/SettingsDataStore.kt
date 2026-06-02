package com.madmaxlgndklr.pokedex.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val MUSIC_ON_LAUNCH = booleanPreferencesKey("music_on_launch")

    val musicOnLaunch: Flow<Boolean> = dataStore.data.map { it[MUSIC_ON_LAUNCH] ?: true }

    suspend fun setMusicOnLaunch(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[MUSIC_ON_LAUNCH] = enabled
            prefs[SETTINGS_UPDATED_AT_KEY] = System.currentTimeMillis()
        }
    }

    private val SEARCH_HISTORY_KEY = stringPreferencesKey("search_history")

    val searchHistory: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[SEARCH_HISTORY_KEY]
            ?.split("")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun addSearchHistory(query: String) {
        dataStore.edit { prefs ->
            val current = prefs[SEARCH_HISTORY_KEY]
                ?.split("")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val updated = (listOf(query) + current.filter { it != query }).take(10)
            prefs[SEARCH_HISTORY_KEY] = updated.joinToString("")
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
            prefs[TEAM_UPDATED_AT_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun setTeam(ids: List<Int>, updatedAt: Long) {
        dataStore.edit { prefs ->
            prefs[TEAM_KEY] = ids.take(6).joinToString(",")
            prefs[TEAM_UPDATED_AT_KEY] = updatedAt
        }
    }

    private val SELECTED_GEN_KEY = intPreferencesKey("selected_gen")

    val selectedGen: Flow<Int> = dataStore.data.map { it[SELECTED_GEN_KEY] ?: 5 }

    suspend fun setGen(gen: Int) {
        dataStore.edit { prefs ->
            prefs[SELECTED_GEN_KEY] = gen
            prefs[SETTINGS_UPDATED_AT_KEY] = System.currentTimeMillis()
        }
    }

    private val BATTLE_CONFIG_KEY = stringPreferencesKey("battle_config_v1")

    suspend fun saveBattleConfig(json: String) {
        dataStore.edit { prefs ->
            prefs[BATTLE_CONFIG_KEY] = json
            prefs[BATTLE_CONFIG_UPDATED_AT_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun saveBattleConfig(json: String, updatedAt: Long) {
        dataStore.edit { prefs ->
            prefs[BATTLE_CONFIG_KEY] = json
            prefs[BATTLE_CONFIG_UPDATED_AT_KEY] = updatedAt
        }
    }

    suspend fun loadBattleConfigJson(): String? =
        dataStore.data.map { it[BATTLE_CONFIG_KEY] }.first()

    // Group A — trainerName
    private val TRAINER_NAME_KEY = stringPreferencesKey("trainer_name")

    val trainerName: Flow<String> = dataStore.data.map { it[TRAINER_NAME_KEY] ?: "" }

    suspend fun setTrainerName(name: String) {
        val now = System.currentTimeMillis()
        dataStore.edit { prefs ->
            prefs[TRAINER_NAME_KEY] = name.take(16)
            prefs[SETTINGS_UPDATED_AT_KEY] = now
        }
    }

    // Group B — sync timestamp keys
    private val TEAM_UPDATED_AT_KEY = longPreferencesKey("team_updated_at")
    val teamUpdatedAt: Flow<Long> = dataStore.data.map { it[TEAM_UPDATED_AT_KEY] ?: 0L }
    suspend fun setTeamUpdatedAt(ts: Long) { dataStore.edit { it[TEAM_UPDATED_AT_KEY] = ts } }

    private val BATTLE_CONFIG_UPDATED_AT_KEY = longPreferencesKey("battle_config_updated_at")
    val battleConfigUpdatedAt: Flow<Long> = dataStore.data.map { it[BATTLE_CONFIG_UPDATED_AT_KEY] ?: 0L }
    suspend fun setBattleConfigUpdatedAt(ts: Long) { dataStore.edit { it[BATTLE_CONFIG_UPDATED_AT_KEY] = ts } }

    private val SETTINGS_UPDATED_AT_KEY = longPreferencesKey("settings_updated_at")
    val settingsUpdatedAt: Flow<Long> = dataStore.data.map { it[SETTINGS_UPDATED_AT_KEY] ?: 0L }
    suspend fun setSettingsUpdatedAt(ts: Long) { dataStore.edit { it[SETTINGS_UPDATED_AT_KEY] = ts } }

    private val SPRITE_MODE_KEY = stringPreferencesKey("sprite_mode")

    val spriteMode: Flow<String> = dataStore.data.map { it[SPRITE_MODE_KEY] ?: "modern" }

    suspend fun setSpriteMode(mode: String) {
        dataStore.edit {
            it[SPRITE_MODE_KEY] = mode
            it[SETTINGS_UPDATED_AT_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun setSpriteMode(mode: String, updatedAt: Long) {
        dataStore.edit {
            it[SPRITE_MODE_KEY] = mode
            it[SETTINGS_UPDATED_AT_KEY] = updatedAt
        }
    }
}
