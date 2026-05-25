package com.madmaxlgndklr.pokedex.list

import com.madmaxlgndklr.pokedex.data.remote.PokeApiService
import com.madmaxlgndklr.pokedex.data.remote.dto.EvolutionChainResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonDetailResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonListResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonSpeciesResponse
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.list.PokemonListViewModel
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
class PokemonListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PokemonListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repo = PokemonRepository(FakePokeApiService(), FakeCaughtPokemonDao())
        viewModel = PokemonListViewModel(repo)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Loading`() {
        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    @Test
    fun `after load state is Success with list`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success<List<PokemonSummary>>).data
        assertEquals(2, list.size)
        assertEquals("bulbasaur", list[0].name)
    }

    @Test
    fun `when repository throws, state is Error`() = runTest {
        val throwingApi = object : PokeApiService {
            override suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse =
                throw RuntimeException("network failure")
            override suspend fun getPokemonDetail(id: Int): PokemonDetailResponse = throw UnsupportedOperationException()
            override suspend fun getPokemonSpecies(id: Int): PokemonSpeciesResponse = throw UnsupportedOperationException()
            override suspend fun getEvolutionChain(id: Int): EvolutionChainResponse = throw UnsupportedOperationException()
        }
        val repo = PokemonRepository(throwingApi, FakeCaughtPokemonDao())
        viewModel = PokemonListViewModel(repo)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("network failure", (state as UiState.Error).message)
    }
}
