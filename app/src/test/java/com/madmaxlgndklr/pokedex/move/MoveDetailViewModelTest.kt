package com.madmaxlgndklr.pokedex.move

import com.google.gson.Gson
import com.madmaxlgndklr.pokedex.data.local.MoveEntity
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.Move
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakeMoveDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.repository.FakePokemonDetailCacheDao
import com.madmaxlgndklr.pokedex.repository.FakePokemonListCacheDao
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.move.MoveDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoveDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()

    private fun buildRepo(moveDao: FakeMoveDao = FakeMoveDao(), api: FakePokeApiService = FakePokeApiService()) =
        PokemonRepository(api, FakeCaughtPokemonDao(), FakePokemonListCacheDao(), FakePokemonDetailCacheDao(), moveDao)

    @Test
    fun `initial state is Loading`() {
        val vm = MoveDetailViewModel(buildRepo(), "flamethrower")
        assertTrue(vm.uiState.value is UiState.Loading)
    }

    @Test
    fun `loads move from api when cache is empty`() = runTest {
        val vm = MoveDetailViewModel(buildRepo(), "flamethrower")
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is UiState.Success)
        val move = (state as UiState.Success<Move>).data
        assertEquals("flamethrower", move.name)
        assertEquals("fire", move.type)
        assertEquals("special", move.category)
        assertEquals(90, move.power)
        assertEquals(100, move.accuracy)
        assertEquals(10, move.pp)
        assertTrue(move.effectText.isNotEmpty())
    }

    @Test
    fun `loads move from cache on cache hit`() = runTest {
        val moveDao = FakeMoveDao()
        val cachedMove = Move(
            name = "flamethrower",
            type = "fire",
            category = "special",
            power = 90,
            accuracy = 100,
            pp = 10,
            effectText = "Cached effect.",
            learnedBy = listOf(PokemonSummary(4, "charmander")),
            totalLearnersCount = 1
        )
        moveDao.insert(MoveEntity("flamethrower", Gson().toJson(cachedMove)))

        val vm = MoveDetailViewModel(buildRepo(moveDao = moveDao), "flamethrower")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("Cached effect.", (state as UiState.Success<Move>).data.effectText)
    }

    @Test
    fun `transitions to Error on network failure`() = runTest {
        val failingApi = object : FakePokeApiService() {
            override suspend fun getMove(name: String) = throw RuntimeException("Network error")
        }
        val vm = MoveDetailViewModel(buildRepo(api = failingApi), "flamethrower")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is UiState.Error)
    }

    @Test
    fun `learners list is capped at 60`() = runTest {
        val bigLearners = (1..80).map {
            com.madmaxlgndklr.pokedex.data.remote.dto.NamedDto(
                "pokemon-$it",
                "http://10.0.2.2:89/api/v2/pokemon/$it/"
            )
        }
        val api = object : FakePokeApiService() {
            override suspend fun getMove(name: String) = com.madmaxlgndklr.pokedex.data.remote.dto.MoveResponse(
                name = name,
                type = com.madmaxlgndklr.pokedex.data.remote.dto.NamedDto("fire"),
                damageClass = com.madmaxlgndklr.pokedex.data.remote.dto.NamedDto("special"),
                power = 90,
                accuracy = 100,
                pp = 10,
                effectEntries = listOf(
                    com.madmaxlgndklr.pokedex.data.remote.dto.MoveEffectEntryDto("Effect.", com.madmaxlgndklr.pokedex.data.remote.dto.NamedDto("en"))
                ),
                learnedByPokemon = bigLearners
            )
        }
        val vm = MoveDetailViewModel(buildRepo(api = api), "flamethrower")
        advanceUntilIdle()

        val state = vm.uiState.value as UiState.Success<Move>
        assertEquals(60, state.data.learnedBy.size)
        assertEquals(80, state.data.totalLearnersCount)
        assertEquals(20, state.data.totalLearnersCount - state.data.learnedBy.size)
    }
}
