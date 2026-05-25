package com.madmaxlgndklr.pokedex.mycollection

import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonEntity
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.ui.mycollection.MyCollectionViewModel
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
class MyCollectionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val fakeDao = FakeCaughtPokemonDao()
    private lateinit var viewModel: MyCollectionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repo = PokemonRepository(FakePokeApiService(), fakeDao)
        viewModel = MyCollectionViewModel(repo)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `initially empty`() = runTest {
        val job = launch { viewModel.caughtList.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.caughtList.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `reflects newly caught pokemon`() = runTest {
        val job = launch { viewModel.caughtList.collect {} }
        advanceUntilIdle()
        fakeDao.insert(CaughtPokemonEntity(id = 1, name = "bulbasaur"))
        advanceUntilIdle()
        assertEquals(1, viewModel.caughtList.value.size)
        assertEquals("bulbasaur", viewModel.caughtList.value[0].name)
        job.cancel()
    }
}
