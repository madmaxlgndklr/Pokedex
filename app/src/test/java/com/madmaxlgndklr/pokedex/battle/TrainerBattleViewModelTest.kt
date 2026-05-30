package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakeMoveDao
import com.madmaxlgndklr.pokedex.repository.FakePokemonDetailCacheDao
import com.madmaxlgndklr.pokedex.repository.FakePokemonListCacheDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.repository.fakeSettingsRepo
import com.madmaxlgndklr.pokedex.ui.battle.BattleState
import com.madmaxlgndklr.pokedex.ui.battle.TrainerClass
import com.madmaxlgndklr.pokedex.ui.battle.Trainer
import com.madmaxlgndklr.pokedex.ui.battle.TrainerPokemon
import com.madmaxlgndklr.pokedex.ui.battle.TrainerRoster
import com.madmaxlgndklr.pokedex.ui.battle.TurnBattleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

private fun fakeTrainer(rosterCount: Int = 1): Trainer {
    val roster = TrainerRoster(
        label = "Test",
        team = List(6) { TrainerPokemon(pokemonId = 1, level = 50,
            moves = listOf("flamethrower", "fly", "dragon-claw", "slash")) }
    )
    return Trainer(
        id = "test-trainer", name = "Test", title = "Gym Leader",
        region = "TestRegion", trainerClass = TrainerClass.GYM_LEADER,
        typeSpecialty = "Fire",
        rosters = if (rosterCount == 2) listOf(roster, roster.copy(label = "Rematch")) else listOf(roster)
    )
}

private fun repo() = PokemonRepository(
    FakePokeApiService(), FakeCaughtPokemonDao(),
    FakePokemonListCacheDao(), FakePokemonDetailCacheDao(), FakeMoveDao()
)

@OptIn(ExperimentalCoroutinesApi::class)
class TrainerBattleViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var vm: TurnBattleViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = TurnBattleViewModel(repo(), fakeSettingsRepo())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `loadTrainerSetup sets battleTrainer`() = runTest {
        val trainer = fakeTrainer()
        vm.loadTrainerSetup(trainer, 0, listOf(1))
        advanceUntilIdle()
        assertNotNull(vm.battleTrainer.value)
        assertEquals(trainer, vm.battleTrainer.value!!.trainer)
        assertEquals(0, vm.battleTrainer.value!!.rosterIndex)
    }

    @Test
    fun `loadTrainerSetup also loads setup`() = runTest {
        vm.loadTrainerSetup(fakeTrainer(), 0, listOf(1))
        advanceUntilIdle()
        assertNotNull(vm.setup.value)
    }

    @Test
    fun `resetToSetup clears battleTrainer`() = runTest {
        vm.loadTrainerSetup(fakeTrainer(), 0, listOf(1))
        advanceUntilIdle()
        vm.resetToSetup()
        assertNull(vm.battleTrainer.value)
    }

    @Test
    fun `startTrainerBattle produces Ongoing state with 6-pokemon opponent`() = runTest {
        val trainer = fakeTrainer()
        vm.loadSetup(listOf(1))
        advanceUntilIdle()
        vm.startTrainerBattle(trainer, 0, listOf(1))
        advanceUntilIdle()
        val state = vm.battleState.value
        assertTrue("battle must be Ongoing", state is BattleState.Ongoing)
        assertEquals("opponent team must have 6", 6,
            (state as BattleState.Ongoing).opponentTeam.size)
    }

    @Test
    fun `startTrainerBattle with rosterIndex 1 uses second roster`() = runTest {
        val roster0 = TrainerRoster("Original", List(6) {
            TrainerPokemon(1, 50, listOf("flamethrower", "fly", "dragon-claw", "slash")) })
        val roster1 = TrainerRoster("Rematch", List(6) {
            TrainerPokemon(1, 70, listOf("surf", "blizzard", "ice-beam", "skull-bash")) })
        val trainer = Trainer("t", "T", "GymLeader", "X",
            TrainerClass.GYM_LEADER, "Fire", listOf(roster0, roster1))
        vm.loadSetup(listOf(1))
        advanceUntilIdle()
        vm.startTrainerBattle(trainer, 1, listOf(1))
        advanceUntilIdle()
        val state = vm.battleState.value as? BattleState.Ongoing ?: error("not ongoing")
        assertEquals(70, state.opponentTeam[0].level)
    }

    @Test
    fun `startBattleFromSetup with battleTrainer uses trainer team as opponent`() = runTest {
        val trainer = fakeTrainer()
        vm.loadTrainerSetup(trainer, 0, listOf(1))
        advanceUntilIdle()
        vm.startBattleFromSetup(listOf(1))
        advanceUntilIdle()
        val state = vm.battleState.value as? BattleState.Ongoing ?: error("not ongoing")
        assertEquals(6, state.opponentTeam.size)
    }

    @Test
    fun `startTrainerBattle with empty teamIds is no-op`() = runTest {
        vm.startTrainerBattle(fakeTrainer(), 0, emptyList())
        advanceUntilIdle()
        assertNull(vm.battleState.value)
    }
}
