package com.madmaxlgndklr.pokedex.model

data class PokemonStat(val name: String, val value: Int)

data class PokemonMove(val name: String, val levelLearnedAt: Int)

data class EvolutionNode(val id: Int, val name: String)

data class EvolutionStage(val members: List<EvolutionNode>)

data class PokemonDetail(
    val id: Int,
    val name: String,
    val spriteUrl: String,
    val types: List<String>,
    val stats: List<PokemonStat>,
    val moves: List<PokemonMove>,       // level-up moves, sorted by levelLearnedAt
    val evolutionChain: List<EvolutionStage>,
    val flavorText: String,
    val height: Int = 0,
    val weight: Int = 0,
    val abilities: List<String> = emptyList(),
    val tmMoves: List<String> = emptyList() // machine/egg/tutor — no level restriction
)
