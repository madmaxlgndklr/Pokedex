package com.madmaxlgndklr.pokedex.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    const val SERVER_ROOT = "https://madmaxlgndklrpokeapi.com"
    private const val BASE_URL = "$SERVER_ROOT/api/v2/"

    fun spriteUrl(id: Int) = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"
    fun shinySpriteUrl(id: Int) = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/$id.png"

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
