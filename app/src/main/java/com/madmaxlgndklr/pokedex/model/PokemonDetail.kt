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
    val moves: List<PokemonMove>,
    val evolutionChain: List<EvolutionStage>,
    val flavorText: String
)
