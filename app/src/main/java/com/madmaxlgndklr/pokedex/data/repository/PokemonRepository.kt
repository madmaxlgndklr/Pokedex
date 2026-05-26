package com.madmaxlgndklr.pokedex.data.repository

import com.google.gson.Gson
import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonDao
import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonEntity
import com.madmaxlgndklr.pokedex.data.local.PokemonDetailCacheDao
import com.madmaxlgndklr.pokedex.data.local.PokemonDetailCacheEntity
import com.madmaxlgndklr.pokedex.data.local.PokemonListCacheDao
import com.madmaxlgndklr.pokedex.data.local.PokemonListCacheEntity
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

class PokemonRepository(
    private val api: PokeApiService,
    private val dao: CaughtPokemonDao,
    private val listCacheDao: PokemonListCacheDao,
    private val detailCacheDao: PokemonDetailCacheDao
) {
    private val gson = Gson()

    suspend fun getPokemonList(): List<PokemonSummary> {
        val cached = listCacheDao.getAll()
        if (cached.isNotEmpty()) return cached.map { PokemonSummary(it.id, it.name) }

        val list = api.getPokemonList().results.map { dto ->
            PokemonSummary(id = dto.extractId(), name = dto.name)
        }
        listCacheDao.insertAll(list.map { PokemonListCacheEntity(it.id, it.name) })
        return list
    }

    suspend fun getPokemonDetail(id: Int): PokemonDetail {
        detailCacheDao.getById(id)?.let {
            return gson.fromJson(it.detailJson, PokemonDetail::class.java)
        }
        return fetchAndCacheDetail(id.toString())
    }

    suspend fun searchPokemon(nameOrId: String): PokemonDetail {
        val trimmed = nameOrId.trim().lowercase()

        trimmed.toIntOrNull()?.let { id ->
            detailCacheDao.getById(id)?.let {
                return gson.fromJson(it.detailJson, PokemonDetail::class.java)
            }
        } ?: run {
            listCacheDao.getByName(trimmed)?.let { entry ->
                detailCacheDao.getById(entry.id)?.let {
                    return gson.fromJson(it.detailJson, PokemonDetail::class.java)
                }
                return fetchAndCacheDetail(entry.id.toString())
            }
        }

        return fetchAndCacheDetail(trimmed)
    }

    suspend fun getCachedCount(): Pair<Int, Int> =
        detailCacheDao.count() to listCacheDao.count()

    suspend fun syncAll(onProgress: (completed: Int, total: Int) -> Unit) {
        if (listCacheDao.count() == 0) {
            val list = api.getPokemonList().results.map { dto ->
                PokemonSummary(id = dto.extractId(), name = dto.name)
            }
            listCacheDao.insertAll(list.map { PokemonListCacheEntity(it.id, it.name) })
        }

        val allPokemon = listCacheDao.getAll()
        val uncached = allPokemon.filter { detailCacheDao.getById(it.id) == null }
        val total = allPokemon.size
        val completed = AtomicInteger(total - uncached.size)

        onProgress(completed.get(), total)

        val semaphore = Semaphore(10)
        coroutineScope {
            uncached.map { entity ->
                async {
                    semaphore.withPermit {
                        try { fetchAndCacheDetail(entity.id.toString()) } catch (_: Exception) {}
                        onProgress(completed.incrementAndGet(), total)
                    }
                }
            }.forEach { it.await() }
        }
    }

    private suspend fun fetchAndCacheDetail(idOrName: String): PokemonDetail = coroutineScope {
        val detailDeferred = async { api.getPokemonDetail(idOrName) }
        val speciesDeferred = async { api.getPokemonSpecies(idOrName.toIntOrNull() ?: detailDeferred.await().id) }
        val detail = detailDeferred.await()
        val species = speciesDeferred.await()
        val evoChain = api.getEvolutionChain(species.evolutionChain.extractId())
        val pokemonDetail = mapDetail(detail, species, evoChain)
        detailCacheDao.insert(PokemonDetailCacheEntity(pokemonDetail.id, gson.toJson(pokemonDetail)))
        pokemonDetail
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
