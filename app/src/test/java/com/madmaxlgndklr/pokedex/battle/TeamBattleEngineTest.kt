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
