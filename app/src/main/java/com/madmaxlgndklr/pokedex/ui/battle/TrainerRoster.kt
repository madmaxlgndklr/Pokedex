package com.madmaxlgndklr.pokedex.ui.battle

enum class TrainerClass { GYM_LEADER, ELITE_FOUR, CHAMPION, RIVAL }

data class TrainerPokemon(
    val pokemonId: Int,
    val level: Int,
    val moves: List<String>
)

data class TrainerRoster(
    val label: String,
    val team: List<TrainerPokemon>
)

data class Trainer(
    val id: String,
    val name: String,
    val title: String,
    val region: String,
    val trainerClass: TrainerClass,
    val typeSpecialty: String,
    val rosters: List<TrainerRoster>
)
