package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PokedexInfoResponse(
    val name: String,
    val descriptions: List<PokedexDescriptionDto>,
    val region: NamedDto?
)

data class PokedexDescriptionDto(
    val description: String,
    val language: NamedDto
)
