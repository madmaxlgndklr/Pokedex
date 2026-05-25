package com.madmaxlgndklr.pokedex.search

import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.ui.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repo = PokemonRepository(FakePokeApiService(), FakeCaughtPokemonDao())
        viewModel = SearchViewModel(repo)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `empty query returns full list after load`() = runTest {
        advanceUntilIdle()
        assertEquals(2, viewModel.filteredList.value.size)
    }

    @Test
    fun `query filters by name`() = runTest {
        advanceUntilIdle()
        viewModel.onQueryChange("ivy")
        assertEquals(1, viewModel.filteredList.value.size)
        assertEquals("ivysaur", viewModel.filteredList.value[0].name)
    }

    @Test
    fun `clearing query restores full list`() = runTest {
        advanceUntilIdle()
        viewModel.onQueryChange("ivy")
        viewModel.onQueryChange("")
        assertEquals(2, viewModel.filteredList.value.size)
    }
}
