package com.madmaxlgndklr.pokedex.data.remote

import com.madmaxlgndklr.pokedex.data.remote.dto.EvolutionChainResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemAttributeResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemDetailResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.MoveResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokedexInfoResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonDetailResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonListResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonSpeciesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokeApiService {
    @GET("pokemon/")
    suspend fun getPokemonList(
        @Query("limit") limit: Int = 1500,
        @Query("offset") offset: Int = 0
    ): PokemonListResponse

    @GET("pokemon/{id}/")
    suspend fun getPokemonDetail(@Path("id") id: String): PokemonDetailResponse

    @GET("pokemon-species/{id}/")
    suspend fun getPokemonSpecies(@Path("id") id: Int): PokemonSpeciesResponse

    @GET("evolution-chain/{id}/")
    suspend fun getEvolutionChain(@Path("id") id: Int): EvolutionChainResponse

    @GET("pokedex/{name}/")
    suspend fun getPokedexInfo(@Path("name") name: String): PokedexInfoResponse

    @GET("move/{name}/")
    suspend fun getMove(@Path("name") name: String): MoveResponse

    @GET("item-attribute/{name}/")
    suspend fun getItemAttribute(@Path("name") name: String): ItemAttributeResponse

    @GET("item/{name}/")
    suspend fun getItem(@Path("name") name: String): ItemDetailResponse
}
