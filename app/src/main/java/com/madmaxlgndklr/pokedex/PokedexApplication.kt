package com.madmaxlgndklr.pokedex

import android.app.Application
import com.madmaxlgndklr.pokedex.data.local.AppDatabase
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.local.settingsDataStore
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.ui.common.CryPlayer
import com.madmaxlgndklr.pokedex.ui.common.NetworkObserver

class PokedexApplication : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }

    val repository by lazy {
        PokemonRepository(
            RetrofitClient.api,
            database.caughtPokemonDao(),
            database.pokemonListCacheDao(),
            database.pokemonDetailCacheDao(),
            database.moveDao()
        )
    }
    val settingsRepository by lazy { SettingsRepository(settingsDataStore) }
    val networkObserver by lazy { NetworkObserver(this) }

    override fun onCreate() {
        super.onCreate()
        filesDir.listFiles { _, n -> n.endsWith(".tmp") }?.forEach { it.delete() }
        CryPlayer.init(this, networkObserver)
    }
}
