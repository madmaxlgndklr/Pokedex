package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ItemResultDto(val name: String, val url: String)

data class ItemAttributeResponse(
    @SerializedName("items") val items: List<ItemResultDto>
)

data class ItemNameDto(val name: String, val language: NamedDto)

data class ItemDetailResponse(
    val id: Int,
    val name: String,
    val names: List<ItemNameDto>
)
