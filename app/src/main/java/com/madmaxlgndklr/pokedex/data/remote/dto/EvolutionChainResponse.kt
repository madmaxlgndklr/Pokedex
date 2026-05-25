package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EvolutionChainResponse(val chain: ChainLinkDto)

data class ChainLinkDto(
    val species: NamedDto,
    @SerializedName("evolves_to") val evolvesTo: List<ChainLinkDto>
)
