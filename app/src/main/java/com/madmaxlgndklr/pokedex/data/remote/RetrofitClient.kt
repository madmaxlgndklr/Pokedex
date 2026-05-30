package com.madmaxlgndklr.pokedex.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    const val SERVER_ROOT = "https://madmaxlgndklrpokeapi.com"
    private const val BASE_URL = "$SERVER_ROOT/api/v2/"

    fun spriteUrl(id: Int) = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"
    fun shinySpriteUrl(id: Int) = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/$id.png"

    fun spriteUrl(id: Int, name: String, mode: String): String {
        val padded = id.toString().padStart(3, '0')
        val fallback = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"
        return when (mode) {
            "retro" -> if (id in 1..251) "$SERVER_ROOT/assets/pokemon_gen1sprites/crystal-jp-$padded.png" else fallback
            "ds"    -> if (id in 1..649) "$SERVER_ROOT/assets/pokemon_gen5_anim_sprites/$padded.gif" else fallback
            else    -> "$SERVER_ROOT/assets/pokemon_generation_${genFromId(id)}_gifs/$name.gif"
        }
    }

    private fun genFromId(id: Int): Int = when (id) {
        in 1..151   -> 1
        in 152..251 -> 2
        in 252..386 -> 3
        in 387..493 -> 4
        in 494..649 -> 5
        in 650..721 -> 6
        in 722..809 -> 7
        else        -> 8
    }

    val httpClient: OkHttpClient by lazy { OkHttpClient.Builder().build() }

    val api: PokeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PokeApiService::class.java)
    }
}
