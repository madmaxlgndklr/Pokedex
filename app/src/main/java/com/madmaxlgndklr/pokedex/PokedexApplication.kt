package com.madmaxlgndklr.pokedex

import android.app.Application
import com.madmaxlgndklr.pokedex.data.local.AppDatabase
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.local.settingsDataStore
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.data.repository.HeldItemRepository
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.ui.common.CryPlayer
import com.madmaxlgndklr.pokedex.ui.common.NetworkObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    val heldItemRepository by lazy {
        HeldItemRepository(
            RetrofitClient.api,
            database.heldItemDao()
        )
    }
    val settingsRepository by lazy { SettingsRepository(settingsDataStore) }
    val networkObserver by lazy { NetworkObserver(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            filesDir.listFiles { _, n -> n.endsWith(".tmp") }?.forEach { it.delete() }
        }
        CryPlayer.init(this, networkObserver)
    }
}
