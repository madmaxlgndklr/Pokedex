package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PokemonSpeciesResponse(
    @SerializedName("flavor_text_entries") val flavorTextEntries: List<FlavorTextEntryDto>,
    @SerializedName("evolution_chain") val evolutionChain: EvolutionChainRefDto
)

data class FlavorTextEntryDto(
    @SerializedName("flavor_text") val flavorText: String,
    val language: NamedDto
)

data class EvolutionChainRefDto(val url: String) {
    fun extractId(): Int = url.trimEnd('/').substringAfterLast('/').toInt()
}
