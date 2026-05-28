package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.ui.battle.*
import org.junit.Assert.*
import org.junit.Test

// ---------------------------------------------------------------------------
// Test helpers
// ---------------------------------------------------------------------------

private fun fakePokemon(
    name: String,
    types: List<String> = listOf("normal"),
    hp: Int = 100,
    attack: Int = 80,
    defense: Int = 80,
    spAtk: Int = 80,
    spDef: Int = 80,
    speed: Int = 80
): BattlePokemon {
    val detail = com.madmaxlgndklr.pokedex.model.PokemonDetail(
        id = name.hashCode().and(0xFFFF),
        name = name,
        spriteUrl = "",
        types = types,
        stats = listOf(
            com.madmaxlgndklr.pokedex.model.PokemonStat("hp", hp),
            com.madmaxlgndklr.pokedex.model.PokemonStat("attack", attack),
            com.madmaxlgndklr.pokedex.model.PokemonStat("defense", defense),
            com.madmaxlgndklr.pokedex.model.PokemonStat("special-attack", spAtk),
            com.madmaxlgndklr.pokedex.model.PokemonStat("special-defense", spDef),
            com.madmaxlgndklr.pokedex.model.PokemonStat("speed", speed)
        ),
        moves = emptyList(),
        evolutionChain = emptyList(),
        flavorText = "",
        tmMoves = emptyList()
    )
    return BattlePokemon(detail, level = 50, maxHp = hp, currentHp = hp,
        moves = listOf(BattleMove("tackle", "normal", "physical", 40, 35, 35)))
}

private val TACKLE = BattleMove("tackle", "normal", "physical", 40, 35, 35)

// ---------------------------------------------------------------------------
// BattleState data model tests
// ---------------------------------------------------------------------------

class BattleStateModelTest {

    @Test
    fun `Ongoing player convenience property returns active team member`() {
        val p1 = fakePokemon("bulbasaur")
        val p2 = fakePokemon("ivysaur")
        val opp = fakePokemon("charmander")
        val state = BattleState.Ongoing(
            playerTeam = listOf(p1, p2),
            playerActiveIndex = 1,
            opponentTeam = listOf(opp),
            opponentActiveIndex = 0,
            log = emptyList()
        )
        assertEquals("ivysaur", state.player.detail.name)
    }

    @Test
    fun `Ongoing opponent convenience property returns active opponent`() {
        val player = fakePokemon("bulbasaur")
        val opp1 = fakePokemon("charmander")
        val opp2 = fakePokemon("squirtle")
        val state = BattleState.Ongoing(
            playerTeam = listOf(player),
            playerActiveIndex = 0,
            opponentTeam = listOf(opp1, opp2),
            opponentActiveIndex = 1,
            log = emptyList()
        )
        assertEquals("squirtle", state.opponent.detail.name)
    }

    @Test
    fun `TurnAction UseMove wraps a BattleMove`() {
        val action = TurnAction.UseMove(TACKLE)
        assertEquals("tackle", (action as TurnAction.UseMove).move.name)
    }

    @Test
    fun `TurnAction SwitchTo wraps a target index`() {
        val action = TurnAction.SwitchTo(2)
        assertEquals(2, (action as TurnAction.SwitchTo).targetIndex)
    }
}

// ---------------------------------------------------------------------------
// BattleEngine.resolveTurn — switching scenarios
// ---------------------------------------------------------------------------

class ResolveTurnSwitchTest {

    private val move = BattleMove("tackle", "normal", "physical", 40, 35, 35)

    private fun aliveTeamOf(n: Int): List<BattlePokemon> =
        (0 until n).map { fakePokemon("poke$it") }

    private fun ongoingState(
        playerTeam: List<BattlePokemon>,
        opponentTeam: List<BattlePokemon>,
        playerIdx: Int = 0,
        opponentIdx: Int = 0
    ) = BattleState.Ongoing(
        playerTeam = playerTeam,
        playerActiveIndex = playerIdx,
        opponentTeam = opponentTeam,
        opponentActiveIndex = opponentIdx,
        log = emptyList()
    )

    @Test
    fun `voluntary switch updates playerActiveIndex`() {
        val team = aliveTeamOf(3)
        val state = ongoingState(team, aliveTeamOf(1))
        val result = BattleEngine.resolveTurn(
            TurnAction.SwitchTo(2),
            TurnAction.UseMove(move),
            state,
            gen = 5
        )
        assertTrue("voluntary switch must produce Ongoing state", result is BattleState.Ongoing)
        assertEquals(2, (result as BattleState.Ongoing).playerActiveIndex)
    }

    @Test
    fun `voluntary switch — opponent attacks incoming pokemon not outgoing`() {
        val outgoing = fakePokemon("outgoing", hp = 200)
        val incoming = fakePokemon("incoming", hp = 1)
        val playerTeam = listOf(outgoing, incoming)
        val opponent = fakePokemon("opponent", attack = 200)
        val state = ongoingState(playerTeam, listOf(opponent))

        val result = BattleEngine.resolveTurn(
            TurnAction.SwitchTo(1),
            TurnAction.UseMove(move),
            state,
            gen = 5
        )
        when (result) {
            is BattleState.Ongoing -> {
                assertEquals(200, result.playerTeam[0].currentHp)
                assertTrue(result.playerTeam[1].currentHp < 1)
            }
            is BattleState.PendingSwitch -> {
                assertEquals(200, result.playerTeam[0].currentHp)
            }
            is BattleState.Lost -> fail("outgoing has 200 HP — Lost should be unreachable with two team members")
            else -> fail("Unexpected state: $result")
        }
    }

    @Test
    fun `faint mid-turn produces PendingSwitch when player has living members`() {
        val playerActive = fakePokemon("low-hp-player", hp = 1)
        val bench = fakePokemon("bench-player", hp = 100)
        val opponent = fakePokemon("strong-opp", attack = 500, speed = 200)

        val state = ongoingState(listOf(playerActive, bench), listOf(opponent))
        val result = BattleEngine.resolveTurn(
            TurnAction.UseMove(move),
            TurnAction.UseMove(move),
            state,
            gen = 5
        )
        assertTrue("expected PendingSwitch when player faints with bench available",
            result is BattleState.PendingSwitch)
    }

    @Test
    fun `faint with no remaining team members produces Lost`() {
        val playerActive = fakePokemon("solo-player", hp = 1)
        val opponent = fakePokemon("strong-opp", attack = 500, speed = 200)

        val state = ongoingState(listOf(playerActive), listOf(opponent))
        val result = BattleEngine.resolveTurn(
            TurnAction.UseMove(move),
            TurnAction.UseMove(move),
            state,
            gen = 5
        )
        assertTrue("expected Lost when last player pokemon faints", result is BattleState.Lost)
    }

    @Test
    fun `all opponent team members faint produces Won`() {
        val player = fakePokemon("strong-player", attack = 500, speed = 200)
        val oppActive = fakePokemon("weak-opp", hp = 1)

        val state = ongoingState(listOf(player), listOf(oppActive))
        val result = BattleEngine.resolveTurn(
            TurnAction.UseMove(move),
            TurnAction.UseMove(move),
            state,
            gen = 5
        )
        assertTrue("expected Won when last opponent faints", result is BattleState.Won)
    }

    @Test
    fun `confirmSwitch updates playerActiveIndex`() {
        val team = listOf(
            fakePokemon("fainted").copy(currentHp = 0),
            fakePokemon("alive", hp = 100)
        )
        val pending = BattleState.PendingSwitch(
            playerTeam = team, playerActiveIndex = 0,
            opponentTeam = listOf(fakePokemon("opp")), opponentActiveIndex = 0,
            log = emptyList()
        )
        val result = BattleEngine.confirmSwitch(1, pending)
        assertTrue(result is BattleState.Ongoing)
        assertEquals(1, (result as BattleState.Ongoing).playerActiveIndex)
    }

    @Test
    fun `confirmSwitch with invalid index returns PendingSwitch unchanged`() {
        val team = listOf(
            fakePokemon("fainted").copy(currentHp = 0),
            fakePokemon("alive", hp = 100)
        )
        val pending = BattleState.PendingSwitch(
            playerTeam = team, playerActiveIndex = 0,
            opponentTeam = listOf(fakePokemon("opp")), opponentActiveIndex = 0,
            log = emptyList()
        )
        assertSame(pending, BattleEngine.confirmSwitch(99, pending))
        assertSame(pending, BattleEngine.confirmSwitch(0, pending))
        val fainting = BattleState.PendingSwitch(
            playerTeam = listOf(fakePokemon("fainted").copy(currentHp = 0), fakePokemon("also-fainted").copy(currentHp = 0)),
            playerActiveIndex = 0,
            opponentTeam = listOf(fakePokemon("opp")), opponentActiveIndex = 0,
            log = emptyList()
        )
        assertSame(fainting, BattleEngine.confirmSwitch(1, fainting))
    }

    @Test
    fun `AI switches out on 4x weakness`() {
        val waterFlyingPokemon = fakePokemon("water-flying-poke", types = listOf("flying", "water"))
        val groundPokemon = fakePokemon("ground-poke", types = listOf("ground"), hp = 100)
        val electricMove = BattleMove("thunderbolt", "electric", "special", 90, 15, 15)
        val electricOpponent = fakePokemon("electric-opp").copy(
            moves = listOf(electricMove)
        )
        val team = listOf(waterFlyingPokemon, groundPokemon)

        val action = BattleEngine.aiPickAction(waterFlyingPokemon, electricOpponent, team, gen = 5)
        assertTrue("AI should switch water/flying-type away from 4x electric weakness",
            action is TurnAction.SwitchTo)
    }

    @Test
    fun `AI does not switch when no better option exists`() {
        val normalPokemon = fakePokemon("normal-poke", types = listOf("normal"), hp = 100)
        val normalOpponent = fakePokemon("normal-opp").copy(
            moves = listOf(BattleMove("tackle", "normal", "physical", 40, 35, 35))
        )
        val team = listOf(normalPokemon)

        val action = BattleEngine.aiPickAction(normalPokemon, normalOpponent, team, gen = 5)
        assertTrue("AI should not switch when alone or no better option",
            action is TurnAction.UseMove)
    }
}
