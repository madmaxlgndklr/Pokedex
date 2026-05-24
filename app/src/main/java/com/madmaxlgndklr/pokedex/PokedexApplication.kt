package com.madmaxlgndklr.pokedex

import android.app.Application
import com.madmaxlgndklr.pokedex.data.local.AppDatabase
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository

class PokedexApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy {
        PokemonRepository(RetrofitClient.api, database.caughtPokemonDao())
    }
}
