package com.madmaxlgndklr.pokedex.data.repository

import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonDao
import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonEntity
import com.madmaxlgndklr.pokedex.data.remote.PokeApiService
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.data.remote.dto.ChainLinkDto
import com.madmaxlgndklr.pokedex.data.remote.dto.EvolutionChainResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonDetailResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonSpeciesResponse
import com.madmaxlgndklr.pokedex.model.EvolutionNode
import com.madmaxlgndklr.pokedex.model.EvolutionStage
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.model.PokemonMove
import com.madmaxlgndklr.pokedex.model.PokemonStat
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PokemonRepository(
    private val api: PokeApiService,
    private val dao: CaughtPokemonDao
) {
    suspend fun getPokemonList(): List<PokemonSummary> =
        api.getPokemonList().results.map { dto ->
            PokemonSummary(id = dto.extractId(), name = dto.name)
        }

    suspend fun getPokemonDetail(id: Int): PokemonDetail = coroutineScope {
        val detailDeferred = async { api.getPokemonDetail(id.toString()) }
        val speciesDeferred = async { api.getPokemonSpecies(id) }
        val detail = detailDeferred.await()
        val species = speciesDeferred.await()
        val evoChain = api.getEvolutionChain(species.evolutionChain.extractId())
        mapDetail(detail, species, evoChain)
    }

    suspend fun searchPokemon(nameOrId: String): PokemonDetail {
        val detail = api.getPokemonDetail(nameOrId.trim().lowercase())
        val species = api.getPokemonSpecies(detail.id)
        val evoChain = api.getEvolutionChain(species.evolutionChain.extractId())
        return mapDetail(detail, species, evoChain)
    }

    fun getCaughtPokemon(): Flow<List<PokemonSummary>> =
        dao.getAll().map { entities ->
            entities.map { PokemonSummary(it.id, it.name) }
        }

    fun isCaught(id: Int): Flow<Boolean> = dao.isCaught(id)

    suspend fun setCaught(id: Int, name: String, caught: Boolean) {
        if (caught) dao.insert(CaughtPokemonEntity(id, name))
        else dao.delete(CaughtPokemonEntity(id, name))
    }

    private fun mapDetail(
        detail: PokemonDetailResponse,
        species: PokemonSpeciesResponse,
        evoChain: EvolutionChainResponse
    ): PokemonDetail {
        val levelUpMoves = detail.moves
            .flatMap { slot ->
                slot.versionGroupDetails
                    .filter { it.moveLearnMethod.name == "level-up" }
                    .map { PokemonMove(slot.move.name, it.levelLearnedAt) }
            }
            .distinctBy { it.name }
            .sortedBy { it.levelLearnedAt }

        val flavorText = species.flavorTextEntries
            .firstOrNull { it.language.name == "en" }
            ?.flavorText
            ?.replace("\n", " ")
            ?.replace("", " ")
            ?: ""

        return PokemonDetail(
            id = detail.id,
            name = detail.name,
            spriteUrl = RetrofitClient.spriteUrl(detail.id),
            types = detail.types.map { it.type.name },
            stats = detail.stats.map { PokemonStat(it.stat.name, it.baseStat) },
            moves = levelUpMoves,
            evolutionChain = parseEvolutionChain(evoChain.chain),
            flavorText = flavorText
        )
    }

    private fun parseEvolutionChain(link: ChainLinkDto): List<EvolutionStage> {
        val stages = mutableListOf<EvolutionStage>()
        var current = listOf(link)
        while (current.isNotEmpty()) {
            stages.add(EvolutionStage(current.map { node ->
                EvolutionNode(
                    id = node.species.url.trimEnd('/').substringAfterLast('/').toInt(),
                    name = node.species.name
                )
            }))
            current = current.flatMap { it.evolvesTo }
        }
        return stages
    }
}
