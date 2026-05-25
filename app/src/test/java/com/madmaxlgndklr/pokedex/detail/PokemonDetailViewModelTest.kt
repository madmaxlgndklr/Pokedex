package com.madmaxlgndklr.pokedex.detail

import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.detail.PokemonDetailViewModel
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
class PokemonDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PokemonDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repo = PokemonRepository(FakePokeApiService(), FakeCaughtPokemonDao())
        viewModel = PokemonDetailViewModel(repo, pokemonId = 1)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Loading`() {
        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    @Test
    fun `after load returns correct detail`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val detail = (state as UiState.Success<PokemonDetail>).data
        assertEquals("bulbasaur", detail.name)
        assertEquals(listOf("grass", "poison"), detail.types)
        assertEquals("A strange seed was planted.", detail.flavorText)
    }

    @Test
    fun `toggling caught inserts into dao`() = runTest {
        val job = launch { viewModel.isCaught.collect {} }
        advanceUntilIdle()
        viewModel.toggleCaught()
        advanceUntilIdle()
        assertTrue(viewModel.isCaught.value)
        job.cancel()
    }
}
