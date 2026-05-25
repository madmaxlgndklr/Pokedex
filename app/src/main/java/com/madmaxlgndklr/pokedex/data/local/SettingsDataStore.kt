package com.madmaxlgndklr.pokedex.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
}
