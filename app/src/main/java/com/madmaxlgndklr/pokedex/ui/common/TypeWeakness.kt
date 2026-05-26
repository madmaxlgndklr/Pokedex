package com.madmaxlgndklr.pokedex.ui.common

private val CHART: Map<String, Map<String, Float>> = mapOf(
    "normal"   to mapOf("rock" to 0.5f, "steel" to 0.5f, "ghost" to 0f),
    "fire"     to mapOf("grass" to 2f, "ice" to 2f, "bug" to 2f, "steel" to 2f,
                        "fire" to 0.5f, "water" to 0.5f, "rock" to 0.5f, "dragon" to 0.5f),
    "water"    to mapOf("fire" to 2f, "ground" to 2f, "rock" to 2f,
                        "water" to 0.5f, "grass" to 0.5f, "dragon" to 0.5f),
    "electric" to mapOf("water" to 2f, "flying" to 2f,
                        "electric" to 0.5f, "grass" to 0.5f, "dragon" to 0.5f, "ground" to 0f),
    "grass"    to mapOf("water" to 2f, "ground" to 2f, "rock" to 2f,
                        "fire" to 0.5f, "grass" to 0.5f, "poison" to 0.5f, "flying" to 0.5f,
                        "bug" to 0.5f, "dragon" to 0.5f, "steel" to 0.5f),
    "ice"      to mapOf("grass" to 2f, "ground" to 2f, "flying" to 2f, "dragon" to 2f,
                        "fire" to 0.5f, "water" to 0.5f, "ice" to 0.5f, "steel" to 0.5f),
    "fighting" to mapOf("normal" to 2f, "ice" to 2f, "rock" to 2f, "dark" to 2f, "steel" to 2f,
                        "poison" to 0.5f, "bug" to 0.5f, "flying" to 0.5f,
                        "psychic" to 0.5f, "fairy" to 0.5f, "ghost" to 0f),
    "poison"   to mapOf("grass" to 2f, "fairy" to 2f,
                        "poison" to 0.5f, "ground" to 0.5f, "rock" to 0.5f,
                        "ghost" to 0.5f, "steel" to 0f),
    "ground"   to mapOf("fire" to 2f, "electric" to 2f, "poison" to 2f, "rock" to 2f, "steel" to 2f,
                        "grass" to 0.5f, "bug" to 0.5f, "flying" to 0f),
    "flying"   to mapOf("grass" to 2f, "fighting" to 2f, "bug" to 2f,
                        "electric" to 0.5f, "rock" to 0.5f, "steel" to 0.5f),
    "psychic"  to mapOf("fighting" to 2f, "poison" to 2f,
                        "psychic" to 0.5f, "steel" to 0.5f, "dark" to 0f),
    "bug"      to mapOf("grass" to 2f, "psychic" to 2f, "dark" to 2f,
                        "fire" to 0.5f, "fighting" to 0.5f, "flying" to 0.5f,
                        "ghost" to 0.5f, "steel" to 0.5f, "poison" to 0.5f, "fairy" to 0.5f),
    "rock"     to mapOf("fire" to 2f, "ice" to 2f, "flying" to 2f, "bug" to 2f,
                        "fighting" to 0.5f, "ground" to 0.5f, "steel" to 0.5f),
    "ghost"    to mapOf("ghost" to 2f, "psychic" to 2f,
                        "dark" to 0.5f, "normal" to 0f),
    "dragon"   to mapOf("dragon" to 2f, "steel" to 0.5f, "fairy" to 0f),
    "dark"     to mapOf("ghost" to 2f, "psychic" to 2f,
                        "fighting" to 0.5f, "dark" to 0.5f, "fairy" to 0.5f),
    "steel"    to mapOf("ice" to 2f, "rock" to 2f, "fairy" to 2f,
                        "fire" to 0.5f, "water" to 0.5f, "electric" to 0.5f, "steel" to 0.5f),
    "fairy"    to mapOf("fighting" to 2f, "dragon" to 2f, "dark" to 2f,
                        "fire" to 0.5f, "poison" to 0.5f, "steel" to 0.5f)
)

private val ALL_TYPES = listOf(
    "normal","fire","water","electric","grass","ice","fighting","poison",
    "ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"
)

fun typeWeaknesses(defenderTypes: List<String>): Map<String, Float> =
    ALL_TYPES.associateWith { attacker ->
        defenderTypes.fold(1f) { acc, defender ->
            acc * (CHART[attacker]?.get(defender) ?: 1f)
        }
    }.filter { it.value != 1f }
