package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MoveResponse(
    val name: String,
    val type: NamedDto,
    @SerializedName("damage_class") val damageClass: NamedDto,
    val power: Int?,
    val accuracy: Int?,
    val pp: Int,
    @SerializedName("effect_entries") val effectEntries: List<MoveEffectEntryDto>,
    @SerializedName("learned_by_pokemon") val learnedByPokemon: List<NamedDto>
)

data class MoveEffectEntryDto(
    @SerializedName("short_effect") val shortEffect: String,
    val language: NamedDto
)
