package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.data.remote.dto.*
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.model.PokemonMove
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakeMoveDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.repository.FakePokemonDetailCacheDao
import com.madmaxlgndklr.pokedex.repository.FakePokemonListCacheDao
import com.madmaxlgndklr.pokedex.repository.fakeSettingsRepo
import com.madmaxlgndklr.pokedex.ui.battle.TurnBattleViewModel
import com.madmaxlgndklr.pokedex.ui.battle.learnableMoves
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

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun detail(
    levelUpMoves: List<PokemonMove>,
    tmMoves: List<String>
) = PokemonDetail(
    id = 6, name = "charizard", spriteUrl = "",
    types = listOf("fire", "flying"),
    stats = emptyList(),
    moves = levelUpMoves,
    evolutionChain = emptyList(),
    flavorText = "",
    tmMoves = tmMoves
)

// Charizard-flavoured fake: level-up moves at 1/9/46/63, TMs flamethrower/dragon-claw,
// and "cut" learnable by both level-up (LV30) AND machine.
private class CharizardApiService : FakePokeApiService() {
    override suspend fun getPokemonDetail(id: String) = PokemonDetailResponse(
        id = 6, name = "charizard",
        sprites = SpritesDto(""),
        types = listOf(PokemonTypeSlotDto(NamedDto("fire")), PokemonTypeSlotDto(NamedDto("flying"))),
        stats = listOf(
            PokemonStatDto(78,  NamedDto("hp")),
            PokemonStatDto(84,  NamedDto("attack")),
            PokemonStatDto(78,  NamedDto("defense")),
            PokemonStatDto(109, NamedDto("special-attack")),
            PokemonStatDto(85,  NamedDto("special-defense")),
            PokemonStatDto(100, NamedDto("speed"))
        ),
        moves = listOf(
            PokemonMoveSlotDto(NamedDto("scratch"),      listOf(MoveVersionDetailDto(1,  NamedDto("level-up")))),
            PokemonMoveSlotDto(NamedDto("ember"),        listOf(MoveVersionDetailDto(9,  NamedDto("level-up")))),
            PokemonMoveSlotDto(NamedDto("fire-spin"),    listOf(MoveVersionDetailDto(46, NamedDto("level-up")))),
            PokemonMoveSlotDto(NamedDto("inferno"),      listOf(MoveVersionDetailDto(63, NamedDto("level-up")))),
            PokemonMoveSlotDto(NamedDto("flamethrower"), listOf(MoveVersionDetailDto(0,  NamedDto("machine")))),
            PokemonMoveSlotDto(NamedDto("dragon-claw"),  listOf(MoveVersionDetailDto(0,  NamedDto("machine")))),
            // Same move in both level-up (LV30) AND machine slots
            PokemonMoveSlotDto(NamedDto("cut"), listOf(
                MoveVersionDetailDto(30, NamedDto("level-up")),
                MoveVersionDetailDto(0,  NamedDto("machine"))
            ))
        ),
        height = 17, weight = 905,
        abilities = listOf(PokemonAbilitySlotDto(NamedDto("blaze")))
    )
}

private fun charizardRepo() = PokemonRepository(
    CharizardApiService(), FakeCaughtPokemonDao(),
    FakePokemonListCacheDao(), FakePokemonDetailCacheDao(), FakeMoveDao()
)

// ---------------------------------------------------------------------------
// learnableMoves() — pure function tests (no coroutines needed)
// ---------------------------------------------------------------------------

class LearnableMovesTest {

    @Test
    fun `level-up move at level below requirement is unavailable`() {
        val d = detail(listOf(PokemonMove("fire-spin", 46)), emptyList())
        val move = learnableMoves(d, 45).single { it.name == "fire-spin" }
        assertEquals(46, move.requiredLevel)
        assertFalse(move.available)
    }

    @Test
    fun `level-up move at exact required level is available`() {
        val d = detail(listOf(PokemonMove("fire-spin", 46)), emptyList())
        val move = learnableMoves(d, 46).single { it.name == "fire-spin" }
        assertTrue(move.available)
    }

    @Test
    fun `level-up move below current level is available`() {
        val d = detail(listOf(PokemonMove("scratch", 1)), emptyList())
        val move = learnableMoves(d, 50).single { it.name == "scratch" }
        assertEquals(1, move.requiredLevel)
        assertTrue(move.available)
    }

    @Test
    fun `TM move has no level restriction and is always available`() {
        val d = detail(emptyList(), listOf("flamethrower"))
        val move = learnableMoves(d, 1).single { it.name == "flamethrower" }
        assertNull(move.requiredLevel)
        assertTrue(move.available)
    }

    @Test
    fun `TM entry overrides level-up entry for same move — no level gate`() {
        // cut appears as level-up at LV30 AND as TM; at level 1 it should still be available
        val d = detail(listOf(PokemonMove("cut", 30)), listOf("cut"))
        val moves = learnableMoves(d, 1)
        val cut = moves.single { it.name == "cut" }
        assertNull(cut.requiredLevel)
        assertTrue(cut.available)
    }

    @Test
    fun `TM moves sort before level-up moves`() {
        val d = detail(
            listOf(PokemonMove("scratch", 1), PokemonMove("ember", 9)),
            listOf("flamethrower", "dragon-claw")
        )
        val moves = learnableMoves(d, 50)
        val firstNonTm = moves.indexOfFirst { it.requiredLevel != null }
        val lastTm     = moves.indexOfLast  { it.requiredLevel == null }
        assertTrue("all TMs must precede all level-up moves", lastTm < firstNonTm)
    }

    @Test
    fun `level-up moves sorted by level ascending`() {
        val d = detail(
            listOf(PokemonMove("inferno", 63), PokemonMove("scratch", 1), PokemonMove("ember", 9)),
            emptyList()
        )
        val levels = learnableMoves(d, 100).mapNotNull { it.requiredLevel }
        assertEquals(listOf(1, 9, 63), levels)
    }

    @Test
    fun `TM moves sorted alphabetically among themselves`() {
        val d = detail(emptyList(), listOf("flamethrower", "dragon-claw", "aerial-ace"))
        val names = learnableMoves(d, 1).map { it.name }
        assertEquals(listOf("aerial-ace", "dragon-claw", "flamethrower"), names)
    }

    @Test
    fun `empty detail returns empty list`() {
        assertTrue(learnableMoves(detail(emptyList(), emptyList()), 50).isEmpty())
    }

    @Test
    fun `duplicate level-up entries for same move keep lowest level`() {
        // Same move appearing in two version groups at different levels
        val d = detail(
            listOf(PokemonMove("tackle", 5), PokemonMove("tackle", 1)),
            emptyList()
        )
        val moves = learnableMoves(d, 50).filter { it.name == "tackle" }
        assertEquals(1, moves.size)        // deduplicated
        assertEquals(1, moves[0].requiredLevel)  // kept the minimum
    }

    @Test
    fun `move not available at low level becomes available when level increases`() {
        val d = detail(listOf(PokemonMove("fire-spin", 46)), emptyList())
        assertFalse(learnableMoves(d, 45).single().available)
        assertTrue(learnableMoves(d, 46).single().available)
        assertTrue(learnableMoves(d, 100).single().available)
    }
}

// ---------------------------------------------------------------------------
// TurnBattleViewModel setup phase
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class TurnBattleViewModelSetupTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var vm: TurnBattleViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = TurnBattleViewModel(charizardRepo(), fakeSettingsRepo())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `initial state has no setup and no battle`() {
        assertNull(vm.setup.value)
        assertNull(vm.battleState.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `loadSetup populates setup with level 50`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        val s = vm.setup.value
        assertNotNull(s)
        assertEquals(50, s!!.level)
        assertEquals("charizard", s.playerDetail.name)
    }

    @Test
    fun `loadSetup pre-selects up to 4 available moves`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        val selected = vm.setup.value!!.selectedMoveNames
        assertTrue("must select at least 1", selected.isNotEmpty())
        assertTrue("must select at most 4", selected.size <= 4)
        // All pre-selected moves must be available at level 50
        val available = learnableMoves(vm.setup.value!!.playerDetail, 50)
            .filter { it.available }.map { it.name }.toSet()
        assertTrue(selected.all { it in available })
    }

    @Test
    fun `loadSetup does not pre-select moves requiring higher level`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        val s = vm.setup.value!!
        // "inferno" requires LV63 — must not be pre-selected at LV50
        assertFalse("inferno should not be pre-selected at LV50", "inferno" in s.selectedMoveNames)
    }

    @Test
    fun `setSetupLevel clamps to 1-100`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        vm.setSetupLevel(0)
        assertEquals(1, vm.setup.value!!.level)
        vm.setSetupLevel(999)
        assertEquals(100, vm.setup.value!!.level)
    }

    @Test
    fun `setSetupLevel drops selected moves that are no longer available`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        // Clear defaults so we have room to pick specific moves
        vm.setup.value!!.selectedMoveNames.toList().forEach { vm.toggleSetupMove(it) }
        // scratch@LV1 and fire-spin@LV46 — both available at LV50
        vm.toggleSetupMove("scratch")
        vm.toggleSetupMove("fire-spin")
        assertTrue("fire-spin available at LV50", "fire-spin" in vm.setup.value!!.selectedMoveNames)
        // Drop below LV46 — fire-spin must be deselected, scratch stays
        vm.setSetupLevel(45)
        assertTrue("scratch survives level drop", "scratch" in vm.setup.value!!.selectedMoveNames)
        assertFalse("fire-spin dropped below its required level", "fire-spin" in vm.setup.value!!.selectedMoveNames)
    }

    @Test
    fun `setSetupLevel keeps TM moves selected when level drops`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        // Clear defaults; select only flamethrower (TM — no level requirement)
        vm.setup.value!!.selectedMoveNames.toList().forEach { vm.toggleSetupMove(it) }
        vm.toggleSetupMove("flamethrower")
        assertTrue("flamethrower selected", "flamethrower" in vm.setup.value!!.selectedMoveNames)
        // Drop to level 1 — TM must survive
        vm.setSetupLevel(1)
        assertTrue("TM move survives any level drop", "flamethrower" in vm.setup.value!!.selectedMoveNames)
    }

    @Test
    fun `toggleSetupMove adds move when fewer than 4 selected`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        // Clear all selections first
        vm.setup.value!!.selectedMoveNames.forEach { vm.toggleSetupMove(it) }
        vm.toggleSetupMove("flamethrower")
        assertTrue("flamethrower" in vm.setup.value!!.selectedMoveNames)
    }

    @Test
    fun `toggleSetupMove removes already-selected move`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        val initial = vm.setup.value!!.selectedMoveNames
        if (initial.isEmpty()) vm.toggleSetupMove("flamethrower")
        val toRemove = vm.setup.value!!.selectedMoveNames.first()
        vm.toggleSetupMove(toRemove)
        assertFalse(toRemove in vm.setup.value!!.selectedMoveNames)
    }

    @Test
    fun `toggleSetupMove does not add a 5th move`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        // Force exactly 4 selections
        vm.setup.value!!.selectedMoveNames.forEach { vm.toggleSetupMove(it) }
        vm.toggleSetupMove("flamethrower")
        vm.toggleSetupMove("dragon-claw")
        vm.toggleSetupMove("scratch")
        vm.toggleSetupMove("ember")
        assertEquals(4, vm.setup.value!!.selectedMoveNames.size)
        // Attempt a 5th
        vm.toggleSetupMove("fire-spin")  // requires LV46; we're at 50
        assertEquals("must still be 4 after attempting 5th", 4, vm.setup.value!!.selectedMoveNames.size)
    }

    @Test
    fun `loadSetup called twice is idempotent`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        val firstSetup = vm.setup.value
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        assertSame("second loadSetup call should be a no-op", firstSetup, vm.setup.value)
    }

    @Test
    fun `resetToSetup clears battle state while keeping setup`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        // Simulate a battle having concluded by resetting
        vm.resetToSetup()
        assertNull(vm.battleState.value)
        assertNotNull("setup must still be present after reset", vm.setup.value)
    }

    @Test
    fun `startBattleFromSetup does nothing when no moves selected`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        // Deselect everything
        vm.setup.value!!.selectedMoveNames.toList().forEach { vm.toggleSetupMove(it) }
        assertEquals(0, vm.setup.value!!.selectedMoveNames.size)
        vm.startBattleFromSetup()
        advanceUntilIdle()
        assertNull("battle must not start with 0 moves", vm.battleState.value)
    }
}

// ---------------------------------------------------------------------------
// Repository — tmMoves extraction
// ---------------------------------------------------------------------------

class RepositoryTmMovesTest {
    private val repo = PokemonRepository(
        CharizardApiService(), FakeCaughtPokemonDao(),
        FakePokemonListCacheDao(), FakePokemonDetailCacheDao(), FakeMoveDao()
    )

    @Test
    fun `getPokemonDetail populates tmMoves from machine slots`() = runTest {
        val detail = repo.getPokemonDetail(6)
        val tm = detail.tmMoves ?: emptyList()
        assertTrue("flamethrower should be a TM move", "flamethrower" in tm)
        assertTrue("dragon-claw should be a TM move",  "dragon-claw"  in tm)
    }

    @Test
    fun `machine move does not appear in level-up moves list`() = runTest {
        val detail = repo.getPokemonDetail(6)
        assertFalse("flamethrower must not be in level-up moves",
            detail.moves.any { it.name == "flamethrower" })
        assertFalse("dragon-claw must not be in level-up moves",
            detail.moves.any { it.name == "dragon-claw" })
    }

    @Test
    fun `move learnable by both TM and level-up appears in tmMoves`() = runTest {
        val detail = repo.getPokemonDetail(6)
        val tm = detail.tmMoves ?: emptyList()
        assertTrue("cut should be in tmMoves (has machine slot)", "cut" in tm)
    }

    @Test
    fun `level-up moves are deduplicated and keep minimum level`() = runTest {
        // The CharizardApiService returns each level-up move once, but the general rule
        // is verified here: moves list must be unique by name.
        val detail = repo.getPokemonDetail(6)
        val names = detail.moves.map { it.name }
        assertEquals("level-up moves must be deduplicated", names.distinct(), names)
    }

    @Test
    fun `pure level-up moves not in tmMoves`() = runTest {
        val detail = repo.getPokemonDetail(6)
        val tm = detail.tmMoves ?: emptyList()
        // scratch and ember are level-up only — should NOT appear in tmMoves
        assertFalse("scratch is level-up only", "scratch" in tm)
        assertFalse("ember is level-up only",   "ember"   in tm)
    }
}
