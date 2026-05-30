package com.madmaxlgndklr.pokedex.model

data class Move(
    val name: String,
    val type: String,
    val category: String,
    val power: Int?,
    val accuracy: Int?,
    val pp: Int,
    val effectText: String,
    val learnedBy: List<PokemonSummary>,
    val totalLearnersCount: Int = 0
)
