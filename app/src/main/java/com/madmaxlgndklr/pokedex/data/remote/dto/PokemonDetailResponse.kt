package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PokemonDetailResponse(
    val id: Int,
    val name: String,
    val sprites: SpritesDto,
    val types: List<PokemonTypeSlotDto>,
    val stats: List<PokemonStatDto>,
    val moves: List<PokemonMoveSlotDto>,
    val height: Int = 0,
    val weight: Int = 0,
    val abilities: List<PokemonAbilitySlotDto> = emptyList()
)

data class SpritesDto(@SerializedName("front_default") val frontDefault: String?)

data class PokemonTypeSlotDto(val type: NamedDto)

data class PokemonStatDto(
    @SerializedName("base_stat") val baseStat: Int,
    val stat: NamedDto
)

data class PokemonAbilitySlotDto(val ability: NamedDto)

data class PokemonMoveSlotDto(
    val move: NamedDto,
    @SerializedName("version_group_details") val versionGroupDetails: List<MoveVersionDetailDto>
)

data class MoveVersionDetailDto(
    @SerializedName("level_learned_at") val levelLearnedAt: Int,
    @SerializedName("move_learn_method") val moveLearnMethod: NamedDto
)

data class NamedDto(val name: String, val url: String = "")
