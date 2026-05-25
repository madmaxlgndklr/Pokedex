package com.madmaxlgndklr.pokedex.repository

import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonDao
import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonEntity
import com.madmaxlgndklr.pokedex.data.remote.PokeApiService
import com.madmaxlgndklr.pokedex.data.remote.dto.*
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonRepositoryTest {
    private val fakeApi = FakePokeApiService()
    private val fakeDao = FakeCaughtPokemonDao()
    private val repo = PokemonRepository(fakeApi, fakeDao)

    @Test
    fun `getPokemonList maps DTOs to domain summaries`() = runTest {
        val list = repo.getPokemonList()
        assertEquals(2, list.size)
        assertEquals(1, list[0].id)
        assertEquals("bulbasaur", list[0].name)
    }

    @Test
    fun `getPokemonDetail returns correct name and types`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertEquals("bulbasaur", detail.name)
        assertEquals(listOf("grass", "poison"), detail.types)
    }

    @Test
    fun `getPokemonDetail returns level-up moves only sorted by level`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertTrue(detail.moves.all { it.levelLearnedAt > 0 || it.levelLearnedAt == 1 })
        val levels = detail.moves.map { it.levelLearnedAt }
        assertEquals(levels.sorted(), levels)
    }

    @Test
    fun `getPokemonDetail includes flavor text`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertEquals("A strange seed was planted.", detail.flavorText)
    }

    @Test
    fun `getPokemonDetail parses linear evolution chain`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertEquals(2, detail.evolutionChain.size)
        assertEquals(1, detail.evolutionChain[0].members.size)
        assertEquals("bulbasaur", detail.evolutionChain[0].members[0].name)
    }

    @Test
    fun `setCaught inserts entity`() = runTest {
        repo.setCaught(id = 1, name = "bulbasaur", caught = true)
        assertEquals(1, fakeDao.caught.first().size)
    }

    @Test
    fun `setCaught false removes entity`() = runTest {
        repo.setCaught(id = 1, name = "bulbasaur", caught = true)
        repo.setCaught(id = 1, name = "bulbasaur", caught = false)
        assertTrue(fakeDao.caught.first().isEmpty())
    }
}

// --- Fakes ---

class FakePokeApiService : PokeApiService {
    override suspend fun getPokemonList(limit: Int, offset: Int) = PokemonListResponse(
        results = listOf(
            PokemonResultDto("bulbasaur", "http://10.0.2.2:89/api/v2/pokemon/1/"),
            PokemonResultDto("ivysaur",   "http://10.0.2.2:89/api/v2/pokemon/2/")
        )
    )

    override suspend fun getPokemonDetail(id: Int) = PokemonDetailResponse(
        id = 1,
        name = "bulbasaur",
        sprites = SpritesDto("https://example.com/1.png"),
        types = listOf(
            PokemonTypeSlotDto(NamedDto("grass")),
            PokemonTypeSlotDto(NamedDto("poison"))
        ),
        stats = listOf(
            PokemonStatDto(45, NamedDto("hp")),
            PokemonStatDto(49, NamedDto("attack"))
        ),
        moves = listOf(
            PokemonMoveSlotDto(
                move = NamedDto("tackle"),
                versionGroupDetails = listOf(
                    MoveVersionDetailDto(1, NamedDto("level-up"))
                )
            ),
            PokemonMoveSlotDto(
                move = NamedDto("cut"),
                versionGroupDetails = listOf(
                    MoveVersionDetailDto(0, NamedDto("machine"))
                )
            )
        )
    )

    override suspend fun getPokemonSpecies(id: Int) = PokemonSpeciesResponse(
        flavorTextEntries = listOf(
            FlavorTextEntryDto("A strange seed was planted.", NamedDto("en")),
            FlavorTextEntryDto("Une graine bizarre a été plantée.", NamedDto("fr"))
        ),
        evolutionChain = EvolutionChainRefDto("http://10.0.2.2:89/api/v2/evolution-chain/1/")
    )

    override suspend fun getEvolutionChain(id: Int) = EvolutionChainResponse(
        chain = ChainLinkDto(
            species = NamedDto("bulbasaur", "http://10.0.2.2:89/api/v2/pokemon-species/1/"),
            evolvesTo = listOf(
                ChainLinkDto(
                    species = NamedDto("ivysaur", "http://10.0.2.2:89/api/v2/pokemon-species/2/"),
                    evolvesTo = emptyList()
                )
            )
        )
    )
}

class FakeCaughtPokemonDao : CaughtPokemonDao {
    private val _all = MutableStateFlow<List<CaughtPokemonEntity>>(emptyList())
    val caught: Flow<List<CaughtPokemonEntity>> = _all

    override suspend fun insert(entity: CaughtPokemonEntity) {
        _all.value = _all.value.filter { it.id != entity.id } + entity
    }

    override suspend fun delete(entity: CaughtPokemonEntity) {
        _all.value = _all.value.filter { it.id != entity.id }
    }

    override fun getAll(): Flow<List<CaughtPokemonEntity>> = _all

    override fun isCaught(id: Int): Flow<Boolean> =
        MutableStateFlow(_all.value.any { it.id == id })
}
