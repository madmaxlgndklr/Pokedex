package com.madmaxlgndklr.pokedex

import android.app.Application
import com.madmaxlgndklr.pokedex.data.local.AppDatabase
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.local.settingsDataStore
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository

class PokedexApplication : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy {
        PokemonRepository(RetrofitClient.api, database.caughtPokemonDao())
    }
    val settingsRepository by lazy { SettingsRepository(settingsDataStore) }
}
