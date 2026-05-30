package com.madmaxlgndklr.pokedex.ui.battle

import com.google.gson.Gson
import com.madmaxlgndklr.pokedex.data.local.HeldItem

data class StatConfigDto(val type: String, val arr1: List<Int>, val arr2: List<Int>)

data class HeldItemDto(
    val id: Int,
    val name: String,
    val displayName: String,
    val effectSummary: String
) {
    fun toHeldItem() = HeldItem(id, name, displayName, effectSummary)
}

data class SlotConfigDto(
    val level: Int? = null,
    val nature: String? = null,
    val moves: List<String>? = null,
    val statConfig: StatConfigDto? = null,
    val heldItem: HeldItemDto? = null
)

data class BattleConfigDto(
    val level: Int = 50,
    val moves: List<String> = emptyList(),
    val nature: String = "Hardy",
    val statConfig: StatConfigDto = StatConfigDto("gen3plus", List(6) { 31 }, List(6) { 0 }),
    val heldItem: HeldItemDto? = null,
    val slots: Map<String, SlotConfigDto> = emptyMap()
)

fun StatConfig.toDto(): StatConfigDto = when (this) {
    is StatConfig.Gen12Config -> StatConfigDto("gen12", dvs.toList(), statExp.toList())
    is StatConfig.Gen3PlusConfig -> StatConfigDto("gen3plus", ivs.toList(), evs.toList())
}

fun StatConfigDto.toStatConfig(): StatConfig = when (type) {
    "gen12" -> StatConfig.Gen12Config(arr1.toIntArray(), arr2.toIntArray())
    else -> StatConfig.Gen3PlusConfig(arr1.toIntArray(), arr2.toIntArray())
}

fun HeldItem.toDto() = HeldItemDto(id, name, displayName, effectSummary)

fun BattleSetup.toDto() = BattleConfigDto(
    level = level,
    moves = selectedMoveNames,
    nature = nature.name,
    statConfig = statConfig.toDto(),
    heldItem = heldItem?.toDto(),
    slots = teamOverrides
        .mapKeys { it.key.toString() }
        .mapValues { (_, ov) ->
            SlotConfigDto(
                level = ov.level,
                nature = ov.nature?.name,
                moves = ov.selectedMoveNames,
                statConfig = ov.statConfig?.toDto(),
                heldItem = ov.heldItem?.toDto()
            )
        }
)

private val gson = Gson()

fun BattleConfigDto.toJson(): String = gson.toJson(this)

fun String.toBattleConfigDto(): BattleConfigDto? = try {
    gson.fromJson(this, BattleConfigDto::class.java)
} catch (e: Exception) {
    null
}
