package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.data.trainer.TrainerRepository
import com.madmaxlgndklr.pokedex.ui.battle.TrainerSelectViewModel
import com.madmaxlgndklr.pokedex.ui.battle.Trainer
import com.madmaxlgndklr.pokedex.ui.battle.TrainerClass
import com.madmaxlgndklr.pokedex.ui.battle.TrainerRoster
import com.madmaxlgndklr.pokedex.ui.battle.TrainerPokemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainerSelectViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val samplePokemon = TrainerPokemon(1, 50, listOf("tackle", "growl", "cut", "thunder-wave"))

    private val sampleRoster = TrainerRoster("Original (RBY)", List(6) { samplePokemon })

    private val brockTrainer = Trainer(
        id = "kanto-brock",
        name = "Brock",
        title = "Gym Leader",
        region = "Kanto",
        trainerClass = TrainerClass.GYM_LEADER,
        typeSpecialty = "Rock",
        rosters = listOf(
            sampleRoster,
            TrainerRoster("Rematch (FRLG)", List(6) { samplePokemon })
        )
    )

    private val mistyTrainer = Trainer(
        id = "kanto-misty",
        name = "Misty",
        title = "Gym Leader",
        region = "Kanto",
        trainerClass = TrainerClass.GYM_LEADER,
        typeSpecialty = "Water",
        rosters = listOf(sampleRoster, TrainerRoster("Rematch (FRLG)", List(6) { samplePokemon }))
    )

    private val lanceTrainer = Trainer(
        id = "kanto-lance",
        name = "Lance",
        title = "Champion",
        region = "Kanto",
        trainerClass = TrainerClass.CHAMPION,
        typeSpecialty = "Mixed",
        rosters = listOf(sampleRoster)
    )

    private fun makeRepo(trainers: List<Trainer>): TrainerRepository {
        val json = buildString {
            append("""{"regions":[""")
            val byRegion = trainers.groupBy { it.region }
            append(byRegion.entries.joinToString(",") { (region, ts) ->
                """{"name":"$region","trainers":[${ts.joinToString(",") { t ->
                    """{"id":"${t.id}","name":"${t.name}","title":"${t.title}",""" +
                    """"trainerClass":"${t.trainerClass.name}","typeSpecialty":"${t.typeSpecialty}",""" +
                    """"rosters":[${t.rosters.joinToString(",") { r ->
                        """{"label":"${r.label}","team":[${r.team.joinToString(",") { p ->
                            """{"pokemonId":${p.pokemonId},"level":${p.level},"moves":${p.moves.map { "\"$it\"" }}}"""
                        }}]}"""
                    }}]}"""
                }}]}"""
            })
            append("]}")
        }
        return TrainerRepository(json)
    }

    private fun makeVm(trainers: List<Trainer> = listOf(brockTrainer, mistyTrainer, lanceTrainer)): TrainerSelectViewModel {
        return TrainerSelectViewModel(makeRepo(trainers))
    }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `trainers loaded at init`() = runTest {
        val vm = makeVm()
        assertEquals(3, vm.trainers.value.size)
    }

    @Test
    fun `toggleRegion expands a collapsed region`() = runTest {
        val vm = makeVm()
        assertFalse("Kanto" in vm.expandedRegions.value)
        vm.toggleRegion("Kanto")
        assertTrue("Kanto" in vm.expandedRegions.value)
    }

    @Test
    fun `toggleRegion collapses an expanded region`() = runTest {
        val vm = makeVm()
        vm.toggleRegion("Kanto")
        assertTrue("Kanto" in vm.expandedRegions.value)
        vm.toggleRegion("Kanto")
        assertFalse("Kanto" in vm.expandedRegions.value)
    }

    @Test
    fun `openSheet sets sheetTrainer`() = runTest {
        val vm = makeVm()
        assertNull(vm.sheetTrainer.value)
        vm.openSheet(brockTrainer)
        assertEquals(brockTrainer, vm.sheetTrainer.value)
    }

    @Test
    fun `closeSheet clears sheetTrainer and resets rosterIndex`() = runTest {
        val vm = makeVm()
        vm.openSheet(brockTrainer)
        vm.setRosterIndex(1)
        vm.closeSheet()
        assertNull(vm.sheetTrainer.value)
        assertEquals(0, vm.sheetRosterIndex.value)
    }

    @Test
    fun `setRosterIndex updates sheetRosterIndex`() = runTest {
        val vm = makeVm()
        assertEquals(0, vm.sheetRosterIndex.value)
        vm.setRosterIndex(1)
        assertEquals(1, vm.sheetRosterIndex.value)
    }

    @Test
    fun `empty repo produces empty trainers list`() = runTest {
        val vm = TrainerSelectViewModel(TrainerRepository("""{"regions":[]}"""))
        assertEquals(0, vm.trainers.value.size)
    }
}
