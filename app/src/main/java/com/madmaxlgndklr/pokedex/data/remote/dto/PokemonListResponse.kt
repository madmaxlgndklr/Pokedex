package com.madmaxlgndklr.pokedex.data.remote.dto

data class PokemonListResponse(val results: List<PokemonResultDto>)

data class PokemonResultDto(val name: String, val url: String) {
    fun extractId(): Int = url.trimEnd('/').substringAfterLast('/').toInt()
}
