package com.madmaxlgndklr.pokedex.settings

import com.madmaxlgndklr.pokedex.battle.FakeHeldItemApiService
import com.madmaxlgndklr.pokedex.battle.FakeHeldItemDao
import com.madmaxlgndklr.pokedex.data.repository.HeldItemRepository
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakeMoveDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.repository.FakePokemonDetailCacheDao
import com.madmaxlgndklr.pokedex.repository.FakePokemonListCacheDao
import com.madmaxlgndklr.pokedex.repository.fakeSettingsRepo
import com.madmaxlgndklr.pokedex.ui.settings.SettingsViewModel
import com.madmaxlgndklr.pokedex.ui.settings.SyncOptions
import com.madmaxlgndklr.pokedex.ui.settings.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelSyncTest {
    private val dispatcher = StandardTestDispatcher()

    private fun makeRepo() = PokemonRepository(
        FakePokeApiService(),
        FakeCaughtPokemonDao(),
        FakePokemonListCacheDao(),
        FakePokemonDetailCacheDao(),
        FakeMoveDao()
    )

    private fun makeHeldItemRepo() = HeldItemRepository(FakeHeldItemApiService(), FakeHeldItemDao())

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    @Test fun `syncWithOptions data-only ends in Done`() = runTest {
        val vm = SettingsViewModel(fakeSettingsRepo(), makeRepo(), makeHeldItemRepo())
        vm.syncWithOptions(SyncOptions(syncData = true, syncMoves = false, syncCries = false, syncItems = false))
        advanceUntilIdle()
        assertTrue(vm.syncState.value is SyncState.Done)
    }

    @Test fun `syncWithOptions cries-false never emits CRIES phase`() = runTest {
        val vm = SettingsViewModel(fakeSettingsRepo(), makeRepo(), makeHeldItemRepo())
        val emittedStates = mutableListOf<SyncState>()
        val job = launch { vm.syncState.collect { emittedStates.add(it) } }
        vm.syncWithOptions(SyncOptions(syncData = true, syncMoves = false, syncCries = false, syncItems = false))
        advanceUntilIdle()
        job.cancel()
        val phases = emittedStates.filterIsInstance<SyncState.Syncing>().map { it.phase }
        assertFalse("CRIES phase must not appear", phases.contains("CRIES"))
        assertTrue("Must end in Done", vm.syncState.value is SyncState.Done)
    }

    @Test fun `syncWithOptions all-false goes directly to Done`() = runTest {
        val vm = SettingsViewModel(fakeSettingsRepo(), makeRepo(), makeHeldItemRepo())
        vm.syncWithOptions(SyncOptions(syncData = false, syncMoves = false, syncCries = false, syncItems = false))
        advanceUntilIdle()
        assertTrue(vm.syncState.value is SyncState.Done)
    }

    @Test fun `second syncWithOptions call ignored while syncing`() = runTest {
        val vm = SettingsViewModel(fakeSettingsRepo(), makeRepo(), makeHeldItemRepo())
        vm.syncWithOptions(SyncOptions())
        vm.syncWithOptions(SyncOptions())
        advanceUntilIdle()
        assertTrue(vm.syncState.value is SyncState.Done)
    }

    @Test fun `resetSyncState returns to Idle`() = runTest {
        val vm = SettingsViewModel(fakeSettingsRepo(), makeRepo(), makeHeldItemRepo())
        vm.syncWithOptions(SyncOptions(syncData = false, syncMoves = false, syncCries = false, syncItems = false))
        advanceUntilIdle()
        assertTrue(vm.syncState.value is SyncState.Done)
        vm.resetSyncState()
        assertTrue(vm.syncState.value is SyncState.Idle)
    }

    @Test fun `SyncOptions defaults all true`() {
        val all = SyncOptions()
        assertTrue(all.syncData && all.syncMoves && all.syncCries)
        val none = SyncOptions(false, false, false)
        assertFalse(none.syncData || none.syncMoves || none.syncCries)
    }
}
