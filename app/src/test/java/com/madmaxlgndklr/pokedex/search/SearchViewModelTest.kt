package com.madmaxlgndklr.pokedex.search

import com.madmaxlgndklr.pokedex.data.remote.PokeApiService
import com.madmaxlgndklr.pokedex.data.remote.dto.EvolutionChainResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonDetailResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonListResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonSpeciesResponse
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.repository.FakePokemonDetailCacheDao
import com.madmaxlgndklr.pokedex.repository.FakePokemonListCacheDao
import com.madmaxlgndklr.pokedex.ui.search.SearchUiState
import com.madmaxlgndklr.pokedex.ui.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repo = PokemonRepository(FakePokeApiService(), FakeCaughtPokemonDao(), FakePokemonListCacheDao(), FakePokemonDetailCacheDao())
        viewModel = SearchViewModel(repo)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Idle`() {
        assertTrue(viewModel.uiState.value is SearchUiState.Idle)
    }

    @Test
    fun `search with valid name emits navigation event with correct id`() = runTest {
        val events = mutableListOf<Int>()
        val job = launch { viewModel.navigationEvent.collect { events.add(it) } }
        viewModel.onQueryChange("bulbasaur")
        viewModel.search()
        advanceUntilIdle()
        assertEquals(listOf(1), events)
        job.cancel()
    }

    @Test
    fun `search resets uiState to Idle on success`() = runTest {
        val job = launch { viewModel.navigationEvent.collect {} }
        viewModel.onQueryChange("bulbasaur")
        viewModel.search()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is SearchUiState.Idle)
        job.cancel()
    }

    @Test
    fun `search sets NotFound when api throws`() = runTest {
        val throwingApi = object : PokeApiService {
            override suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse = throw UnsupportedOperationException()
            override suspend fun getPokemonDetail(id: String): PokemonDetailResponse = throw RuntimeException("not found")
            override suspend fun getPokemonSpecies(id: Int): PokemonSpeciesResponse = throw UnsupportedOperationException()
            override suspend fun getEvolutionChain(id: Int): EvolutionChainResponse = throw UnsupportedOperationException()
        }
        val repo = PokemonRepository(throwingApi, FakeCaughtPokemonDao(), FakePokemonListCacheDao(), FakePokemonDetailCacheDao())
        viewModel = SearchViewModel(repo)
        viewModel.onQueryChange("missingno")
        viewModel.search()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is SearchUiState.NotFound)
    }

    @Test
    fun `onQueryChange resets NotFound state to Idle`() = runTest {
        val throwingApi = object : PokeApiService {
            override suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse = throw UnsupportedOperationException()
            override suspend fun getPokemonDetail(id: String): PokemonDetailResponse = throw RuntimeException("not found")
            override suspend fun getPokemonSpecies(id: Int): PokemonSpeciesResponse = throw UnsupportedOperationException()
            override suspend fun getEvolutionChain(id: Int): EvolutionChainResponse = throw UnsupportedOperationException()
        }
        val repo = PokemonRepository(throwingApi, FakeCaughtPokemonDao(), FakePokemonListCacheDao(), FakePokemonDetailCacheDao())
        viewModel = SearchViewModel(repo)
        viewModel.onQueryChange("missingno")
        viewModel.search()
        advanceUntilIdle()
        viewModel.onQueryChange("bulbasaur")
        assertTrue(viewModel.uiState.value is SearchUiState.Idle)
    }
}
