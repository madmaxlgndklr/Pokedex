# Team Switching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current 1v1 battle with a full 6v6 engine supporting voluntary and faint-forced switching, per-battle slot overrides, and a type-aware opponent AI that switches strategically.

**Architecture:** `BattleState.Ongoing` gains team arrays and active indices with convenience `player`/`opponent` properties for call-site compatibility. A new `BattleState.PendingSwitch` sealed case explicitly models the forced-switch moment. `TurnAction` replaces the existing `MoveAction` pattern so the engine can receive either a move or a switch from either side. The opponent AI is upgraded from `aiPickMove` to `aiPickAction` using a `switchScore` function.

**Tech Stack:** Kotlin, JUnit 4, `kotlinx-coroutines-test`, existing fake test doubles in `app/src/test/java/com/madmaxlgndklr/pokedex/repository/`

**Project root:** `/home/madmaxlgndklr/Git/sandbox/Pokedex`

Run tests with:
```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.*" 2>&1 | tail -20
```

---

## File Map

| Action | File |
|--------|------|
| **Modify** | `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/BattleEngine.kt` |
| **Modify** | `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleViewModel.kt` |
| **Modify** | `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt` |
| **Create** | `app/src/test/java/com/madmaxlgndklr/pokedex/battle/TeamBattleEngineTest.kt` |
| **Modify** | `app/src/test/java/com/madmaxlgndklr/pokedex/battle/BattleSetupTest.kt` |

---

## Existing code you must understand before starting

**`BattleEngine.kt`** (current state):

```kotlin
// BattleState sealed class — currently only Ongoing(player, opponent, log)
// MoveAction(pokemon, move, target) — current action type
// BattleEngine.resolveTurn(playerAction: MoveAction, opponentAction: MoveAction, state, gen)
// BattleEngine.aiPickMove(pokemon) — picks highest-power move
// BattleEngine.startBattle(player, opponent, gen)
```

**`TurnBattleViewModel.kt`** — `submitMove(moveIndex)` is the entry point for player actions. It calls `BattleEngine.aiPickMove(ongoing.opponent)` and `BattleEngine.resolveTurn(playerAction, aiAction, ongoing, gen)`.

**`TurnBattleScreen.kt`** — `OngoingBattleView` receives `onMove: (Int) -> Unit` and `onForfeit: () -> Unit`. The `when (battleState)` block in `TurnBattleScreen` dispatches to sub-composables by state type.

---

## Task 1: Replace BattleState and introduce TurnAction

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/BattleEngine.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/battle/TeamBattleEngineTest.kt`

- [ ] **Step 1: Write failing tests for the new data types**

Create `app/src/test/java/com/madmaxlgndklr/pokedex/battle/TeamBattleEngineTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run to confirm compilation failure**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.BattleStateModelTest" 2>&1 | tail -30
```

Expected: FAILED — `BattleState.Ongoing` doesn't have `playerTeam`/`playerActiveIndex`, `TurnAction` doesn't exist.

- [ ] **Step 3: Replace BattleState and add TurnAction in BattleEngine.kt**

Replace the entire `BattleEngine.kt` with the following. **Read the current file first** — you need to keep `BattleMove`, `BattlePokemon`, and `BattleEngine` object intact; only `BattleState`, `MoveAction` change and `TurnAction` is new:

```kotlin
package com.madmaxlgndklr.pokedex.ui.battle

import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import kotlin.random.Random

data class BattlePokemon(
    val detail: PokemonDetail,
    val level: Int = 50,
    val maxHp: Int,
    val currentHp: Int,
    val moves: List<BattleMove>,
    val statConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
    val nature: Nature = Natures.HARDY,
    val heldItem: HeldItem? = null
)

data class BattleMove(
    val name: String,
    val type: String,
    val category: String,
    val power: Int?,
    val maxPp: Int,
    val currentPp: Int
)

/** Replaces the old MoveAction — engine now receives a sealed action per side. */
sealed class TurnAction {
    data class UseMove(val move: BattleMove) : TurnAction()
    data class SwitchTo(val targetIndex: Int) : TurnAction()
}

/** Kept for backward compat; internal engine helper only. */
private data class MoveAction(
    val pokemon: BattlePokemon,
    val move: BattleMove,
    val target: BattlePokemon
)

sealed class BattleState {
    data class Ongoing(
        val playerTeam: List<BattlePokemon>,
        val playerActiveIndex: Int,
        val opponentTeam: List<BattlePokemon>,
        val opponentActiveIndex: Int,
        val log: List<String>
    ) : BattleState() {
        val player: BattlePokemon get() = playerTeam[playerActiveIndex]
        val opponent: BattlePokemon get() = opponentTeam[opponentActiveIndex]
    }

    data class PendingSwitch(
        val playerTeam: List<BattlePokemon>,
        val playerActiveIndex: Int,
        val opponentTeam: List<BattlePokemon>,
        val opponentActiveIndex: Int,
        val log: List<String>
    ) : BattleState()

    data class Won(val log: List<String>) : BattleState()
    data class Lost(val log: List<String>) : BattleState()
}

object BattleEngine {

    fun startBattle(player: BattlePokemon, opponent: BattlePokemon, gen: Int): BattleState.Ongoing {
        return BattleState.Ongoing(
            playerTeam = listOf(player),
            playerActiveIndex = 0,
            opponentTeam = listOf(opponent),
            opponentActiveIndex = 0,
            log = listOf("A wild ${opponent.detail.name.uppercase()} appeared!")
        )
    }

    fun startBattle(playerTeam: List<BattlePokemon>, opponentTeam: List<BattlePokemon>, gen: Int): BattleState.Ongoing {
        require(playerTeam.isNotEmpty()) { "Player team must not be empty" }
        require(opponentTeam.isNotEmpty()) { "Opponent team must not be empty" }
        return BattleState.Ongoing(
            playerTeam = playerTeam,
            playerActiveIndex = 0,
            opponentTeam = opponentTeam,
            opponentActiveIndex = 0,
            log = listOf("A wild ${opponentTeam[0].detail.name.uppercase()} appeared!")
        )
    }

    fun buildBattlePokemon(
        detail: PokemonDetail,
        level: Int,
        moves: List<BattleMove>,
        statConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
        nature: Nature = Natures.HARDY,
        heldItem: HeldItem? = null
    ): BattlePokemon {
        val hpBase = detail.stats.firstOrNull { it.name == "hp" }?.value ?: 45
        val maxHp = StatFormulas.computeHp(hpBase, statConfig, level)
        return BattlePokemon(detail, level, maxHp, maxHp, moves, statConfig, nature, heldItem)
    }

    /**
     * Resolves one full turn given both sides' chosen actions.
     *
     * Voluntary switch priority: player SwitchTo always resolves before opponent moves.
     * Faint after first move: if the second mover fainted, its queued move is discarded.
     * Returns PendingSwitch when the player's active Pokémon has fainted but the player
     * still has living team members.
     */
    fun resolveTurn(
        playerAction: TurnAction,
        opponentAction: TurnAction,
        state: BattleState.Ongoing,
        gen: Int
    ): BattleState {
        // Voluntary switch: happens before opponent's move
        if (playerAction is TurnAction.SwitchTo) {
            val newIndex = playerAction.targetIndex.coerceIn(0, state.playerTeam.lastIndex)
            var updatedState = state.copy(playerActiveIndex = newIndex)
            // Opponent now attacks the incoming Pokémon
            if (opponentAction is TurnAction.UseMove) {
                val log = mutableListOf<String>()
                log.add("${state.player.detail.name.uppercase()} withdrew!")
                log.add("Go, ${updatedState.player.detail.name.uppercase()}!")
                val (newOpponent, newPlayer) = applyMoveInternal(
                    updatedState.opponent, opponentAction.move, updatedState.player, gen, log
                )
                updatedState = updatedState.copy(
                    playerTeam = updatedState.playerTeam.toMutableList().also { it[updatedState.playerActiveIndex] = newPlayer },
                    opponentTeam = updatedState.opponentTeam.toMutableList().also { it[updatedState.opponentActiveIndex] = newOpponent },
                    log = state.log + log
                )
                if (newPlayer.currentHp <= 0) {
                    val endLog = updatedState.log + listOf("${newPlayer.detail.name.uppercase()} fainted!")
                    val alive = updatedState.playerTeam.count { it.currentHp > 0 }
                    return if (alive == 0) BattleState.Lost(endLog)
                    else BattleState.PendingSwitch(
                        playerTeam = updatedState.playerTeam,
                        playerActiveIndex = updatedState.playerActiveIndex,
                        opponentTeam = updatedState.opponentTeam,
                        opponentActiveIndex = updatedState.opponentActiveIndex,
                        log = endLog
                    )
                }
            } else {
                // Opponent also switches — both switch, no attacks
                val oppNewIndex = (opponentAction as? TurnAction.SwitchTo)?.targetIndex
                    ?.coerceIn(0, updatedState.opponentTeam.lastIndex) ?: updatedState.opponentActiveIndex
                val switchLog = listOf(
                    "${state.player.detail.name.uppercase()} withdrew!",
                    "Go, ${updatedState.player.detail.name.uppercase()}!"
                )
                updatedState = updatedState.copy(
                    opponentActiveIndex = oppNewIndex,
                    log = state.log + switchLog
                )
            }
            return updatedState
        }

        // Both use moves — standard priority resolution
        val playerMove = (playerAction as TurnAction.UseMove).move
        val opponentMove = when (opponentAction) {
            is TurnAction.UseMove -> opponentAction.move
            is TurnAction.SwitchTo -> null  // handled below
        }

        val playerSpeed = statValue(state.player, "speed")
        val opponentSpeed = statValue(state.opponent, "speed")
        val playerGoesFirst = when {
            playerSpeed > opponentSpeed -> true
            playerSpeed < opponentSpeed -> false
            else -> Random.nextBoolean()
        }

        var playerTeam = state.playerTeam.toMutableList()
        var opponentTeam = state.opponentTeam.toMutableList()
        var playerIdx = state.playerActiveIndex
        var opponentIdx = state.opponentActiveIndex
        val log = mutableListOf<String>()

        fun player() = playerTeam[playerIdx]
        fun opponent() = opponentTeam[opponentIdx]

        // Handle opponent switching mid-priority (e.g., opponent SwitchTo when player moves)
        if (opponentMove == null && opponentAction is TurnAction.SwitchTo) {
            val newOppIdx = opponentAction.targetIndex.coerceIn(0, opponentTeam.lastIndex)
            log.add("Opponent withdrew ${opponent().detail.name.uppercase()}!")
            opponentIdx = newOppIdx
            log.add("Opponent sent out ${opponent().detail.name.uppercase()}!")
        }

        fun applyMove(attacker: BattlePokemon, move: BattleMove, defender: BattlePokemon): Pair<BattlePokemon, BattlePokemon> =
            applyMoveInternal(attacker, move, defender, gen, log)

        fun commitMove(newAttacker: BattlePokemon, newDefender: BattlePokemon, attackerIsPlayer: Boolean) {
            if (attackerIsPlayer) {
                playerTeam[playerIdx] = newAttacker
                opponentTeam[opponentIdx] = newDefender
            } else {
                opponentTeam[opponentIdx] = newAttacker
                playerTeam[playerIdx] = newDefender
            }
        }

        // First move
        val firstIsPlayer = playerGoesFirst
        val (firstAttacker, firstMove, firstDefender) =
            if (firstIsPlayer) Triple(player(), playerMove, opponent())
            else if (opponentMove != null) Triple(opponent(), opponentMove, player())
            else Triple(player(), playerMove, opponent())  // fallback if opp switched

        val (fa, fd) = applyMove(firstAttacker, firstMove, firstDefender)
        commitMove(fa, fd, firstIsPlayer)

        // Check for faint after first move
        if (!firstIsPlayer && playerTeam[playerIdx].currentHp <= 0) {
            // Opponent's move fainted the player's Pokémon
            log.add("${player().detail.name.uppercase()} fainted!")
            val endLog = state.log + log
            val alive = playerTeam.count { it.currentHp > 0 }
            return if (alive == 0) BattleState.Lost(endLog)
            else BattleState.PendingSwitch(
                playerTeam = playerTeam, playerActiveIndex = playerIdx,
                opponentTeam = opponentTeam, opponentActiveIndex = opponentIdx,
                log = endLog
            )
        }
        if (firstIsPlayer && opponentTeam[opponentIdx].currentHp <= 0) {
            // Player's move fainted the opponent
            log.add("${opponent().detail.name.uppercase()} fainted!")
            val endLog = state.log + log
            val aliveOpp = opponentTeam.count { it.currentHp > 0 }
            if (aliveOpp == 0) return BattleState.Won(endLog)
            // AI sends out next
            val nextIdx = aiForcedSwitchIndex(opponentTeam)
                ?: return BattleState.Won(endLog)
            opponentIdx = nextIdx
            log.add("A new ${opponent().detail.name.uppercase()} appeared!")
        }

        // Second move (skip if the second mover already fainted, or opponent switched)
        val secondIsPlayer = !firstIsPlayer
        if (opponentMove != null || secondIsPlayer) {
            val (secondAttacker, secondMove, secondDefender) =
                if (secondIsPlayer) Triple(player(), playerMove, opponent())
                else Triple(opponent(), opponentMove!!, player())

            if (secondAttacker.currentHp > 0) {
                val (sa, sd) = applyMove(secondAttacker, secondMove, secondDefender)
                commitMove(sa, sd, secondIsPlayer)

                if (!secondIsPlayer && playerTeam[playerIdx].currentHp <= 0) {
                    log.add("${player().detail.name.uppercase()} fainted!")
                    val endLog = state.log + log
                    val alive = playerTeam.count { it.currentHp > 0 }
                    return if (alive == 0) BattleState.Lost(endLog)
                    else BattleState.PendingSwitch(
                        playerTeam = playerTeam, playerActiveIndex = playerIdx,
                        opponentTeam = opponentTeam, opponentActiveIndex = opponentIdx,
                        log = endLog
                    )
                }
                if (secondIsPlayer && opponentTeam[opponentIdx].currentHp <= 0) {
                    log.add("${opponent().detail.name.uppercase()} fainted!")
                    val endLog = state.log + log
                    val aliveOpp = opponentTeam.count { it.currentHp > 0 }
                    if (aliveOpp == 0) return BattleState.Won(endLog)
                    val nextIdx = aiForcedSwitchIndex(opponentTeam)
                        ?: return BattleState.Won(endLog)
                    opponentIdx = nextIdx
                    log.add("A new ${opponent().detail.name.uppercase()} appeared!")
                }
            }
        }

        return state.copy(
            playerTeam = playerTeam,
            playerActiveIndex = playerIdx,
            opponentTeam = opponentTeam,
            opponentActiveIndex = opponentIdx,
            log = state.log + log
        )
    }

    /** Called by the UI when the player confirms a forced switch after their Pokémon fainted. */
    fun confirmSwitch(newIndex: Int, state: BattleState.PendingSwitch): BattleState {
        if (newIndex < 0 || newIndex >= state.playerTeam.size) return state
        if (newIndex == state.playerActiveIndex) return state
        if (state.playerTeam[newIndex].currentHp <= 0) return state
        val log = state.log + listOf("Go, ${state.playerTeam[newIndex].detail.name.uppercase()}!")
        return BattleState.Ongoing(
            playerTeam = state.playerTeam,
            playerActiveIndex = newIndex,
            opponentTeam = state.opponentTeam,
            opponentActiveIndex = state.opponentActiveIndex,
            log = log
        )
    }

    /** Full AI action: decides between switching and attacking. */
    fun aiPickAction(
        active: BattlePokemon,
        opponentActive: BattlePokemon,
        team: List<BattlePokemon>,
        gen: Int
    ): TurnAction {
        val currentScore = switchScore(active, opponentActive, gen)
        val best = team.indices
            .filter { i -> i != team.indexOf(active) && team[i].currentHp > 0 }
            .maxByOrNull { switchScore(team[it], opponentActive, gen) }

        if (best != null) {
            val bestScore = switchScore(team[best], opponentActive, gen)
            // Has a 4× weakness or HP < 25% with a significantly better option
            val has4xWeakness = DamageEngine.computeEffectiveness(gen, opponentActive.moves
                .filter { it.power != null && it.power > 0 }
                .maxByOrNull { it.power ?: 0 }?.type ?: "normal",
                active.detail.types) >= 4f
            val lowHp = active.currentHp.toFloat() / active.maxHp < 0.25f
            val shouldSwitch = (has4xWeakness || lowHp) && bestScore > currentScore * 1.5f
            if (shouldSwitch) return TurnAction.SwitchTo(best)
        }

        return TurnAction.UseMove(aiPickMove(active))
    }

    /** Backward-compat: picks highest-power move. Still used internally and by aiPickAction. */
    fun aiPickMove(pokemon: BattlePokemon): BattleMove =
        pokemon.moves
            .filter { it.currentPp > 0 && it.power != null && it.category != "status" }
            .maxByOrNull { it.power ?: 0 }
            ?: pokemon.moves.first { it.currentPp > 0 }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun switchScore(candidate: BattlePokemon, opponent: BattlePokemon, gen: Int): Float {
        val opponentBestType = opponent.moves
            .filter { it.power != null && it.power > 0 }
            .maxByOrNull { it.power ?: 0 }?.type ?: "normal"
        val typeAdvantage = DamageEngine.computeEffectiveness(gen, opponentBestType, candidate.detail.types)
        val defenseScore = if (typeAdvantage > 0f) 1f / typeAdvantage else 0f
        val hpFraction = candidate.currentHp.toFloat() / candidate.maxHp
        return defenseScore * hpFraction
    }

    private fun aiForcedSwitchIndex(team: List<BattlePokemon>): Int? =
        team.indices.firstOrNull { team[it].currentHp > 0 }

    private fun applyMoveInternal(
        attacker: BattlePokemon,
        move: BattleMove,
        defender: BattlePokemon,
        gen: Int,
        log: MutableList<String>
    ): Pair<BattlePokemon, BattlePokemon> {  // (updatedAttacker, updatedDefender)
        if (move.power == null || move.power == 0 || move.category == "status") {
            log.add("${attacker.detail.name.uppercase()} used ${move.name.uppercase().replace("-", " ")}!")
            return attacker to defender
        }
        val isPhysical = when {
            gen >= 4 -> move.category == "physical"
            gen <= 1 -> true
            else -> DamageEngine.isPhysicalGen23(move.type)
        }
        val atkStat = if (isPhysical) "attack" else "special-attack"
        val defStat = if (isPhysical) "defense" else "special-defense"
        val params = DamageParams(
            gen = gen,
            level = attacker.level,
            attackBaseStat = statValue(attacker, atkStat),
            defenseBaseStat = statValue(defender, defStat),
            attackStatIndex = if (isPhysical) 1 else 3,
            defenseStatIndex = if (isPhysical) 2 else 4,
            attackerStatConfig = attacker.statConfig,
            attackerNature = attacker.nature,
            defenderStatConfig = defender.statConfig,
            defenderNature = defender.nature,
            heldItem = attacker.heldItem,
            basePower = move.power,
            moveType = move.type,
            moveCategory = move.category,
            attackerTypes = attacker.detail.types,
            defenderTypes = defender.detail.types
        )
        val result = DamageEngine.calculate(params)
        val dmg = result.average.coerceAtMost(defender.currentHp)
        val newHp = (defender.currentHp - dmg).coerceAtLeast(0)
        log.add("${attacker.detail.name.uppercase()} used ${move.name.uppercase().replace("-", " ")}! (${result.effectivenessLabel})")
        if (result.effectivenessLabel.startsWith("0")) log.add("It had no effect!")
        else if (result.effectivenessLabel in listOf("4.0×", "2.0×")) log.add("It's super effective!")
        else if (result.effectivenessLabel in listOf("0.5×", "0.25×")) log.add("It's not very effective...")
        val updatedMove = move.copy(currentPp = (move.currentPp - 1).coerceAtLeast(0))
        val updatedAttacker = attacker.copy(moves = attacker.moves.map { if (it == move) updatedMove else it })
        val updatedDefender = defender.copy(currentHp = newHp)
        return updatedAttacker to updatedDefender
    }

    private fun statValue(pokemon: BattlePokemon, statName: String): Int =
        pokemon.detail.stats.firstOrNull { it.name == statName }?.value ?: 50
}
```

- [ ] **Step 4: Run the new tests**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.BattleStateModelTest" 2>&1 | tail -20
```

Expected: PASS — 4 tests.

- [ ] **Step 5: Run all existing tests to check for regressions**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:testDebugUnitTest 2>&1 | tail -30
```

Expected: all existing tests pass. There will be compilation failures in `TurnBattleViewModel` (still references `MoveAction` / old `BattleState` — that's fine, it will be fixed in Task 2).

> **Note:** If compilation fails entirely due to `TurnBattleViewModel.kt` or `TurnBattleScreen.kt` referencing the old API, you may need to temporarily stub out the broken references to get tests running. The cleanest approach: comment out `submitMove` body and replace with `TODO()` in `TurnBattleViewModel` temporarily, run tests, then restore. Do not commit the stub.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/BattleEngine.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/battle/TeamBattleEngineTest.kt
git commit -m "feat: replace BattleState/MoveAction with team-aware model and TurnAction"
```

---

## Task 2: Wire TurnBattleViewModel to new engine API

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleViewModel.kt`
- Modify: `app/src/test/java/com/madmaxlgndklr/pokedex/battle/BattleSetupTest.kt`

- [ ] **Step 1: Write failing ViewModel tests for team switching**

Add the following class to the **bottom** of `app/src/test/java/com/madmaxlgndklr/pokedex/battle/BattleSetupTest.kt`:

```kotlin
// ---------------------------------------------------------------------------
// TurnBattleViewModel — team switching actions
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class TurnBattleViewModelSwitchTest {
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
    fun `submitSwitch does nothing when battle is not Ongoing`() = runTest {
        assertNull(vm.battleState.value)
        vm.submitSwitch(1)
        advanceUntilIdle()
        assertNull(vm.battleState.value)
    }

    @Test
    fun `confirmSwitch does nothing when battle is not PendingSwitch`() = runTest {
        assertNull(vm.battleState.value)
        vm.confirmSwitch(0)
        advanceUntilIdle()
        assertNull(vm.battleState.value)
    }

    @Test
    fun `submitMove wraps player move in TurnAction UseMove`() = runTest {
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        vm.startBattleFromSetup()
        advanceUntilIdle()
        val before = vm.battleState.value
        assertTrue("battle must be Ongoing to test submitMove", before is BattleState.Ongoing)
        vm.submitMove(0)
        advanceUntilIdle()
        // After a move, state should have changed (could be Ongoing, Won, or Lost)
        val after = vm.battleState.value
        assertNotNull(after)
        // Log must have grown — move was applied
        val beforeLog = (before as BattleState.Ongoing).log
        val afterLog = when (after) {
            is BattleState.Ongoing -> after.log
            is BattleState.Won -> after.log
            is BattleState.Lost -> after.log
            is BattleState.PendingSwitch -> after.log
            else -> emptyList()
        }
        assertTrue("log must grow after move", afterLog.size > beforeLog.size)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.TurnBattleViewModelSwitchTest" 2>&1 | tail -20
```

Expected: FAILED — `submitSwitch` and `confirmSwitch` methods don't exist yet.

- [ ] **Step 3: Update TurnBattleViewModel**

Replace the `submitMove` and `forfeit` methods, add `submitSwitch` and `confirmSwitch`, and fix `startBattleFromSetup` to use the new `startBattle(playerTeam, opponentTeam, gen)` overload. Also add a `SlotOverride` data class and `teamOverrides` map to `BattleSetup`.

The key changes to `TurnBattleViewModel.kt`:

**1. Add `SlotOverride` and `teamOverrides` to `BattleSetup`:**

```kotlin
data class SlotOverride(
    val level: Int? = null,
    val statConfig: StatConfig? = null,
    val nature: Nature? = null,
    val heldItem: HeldItem? = null
)

data class BattleSetup(
    val playerDetail: PokemonDetail,
    val level: Int,
    val selectedMoveNames: List<String>,
    val statConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
    val nature: Nature = Natures.HARDY,
    val heldItem: HeldItem? = null,
    val teamOverrides: Map<Int, SlotOverride> = emptyMap()   // key = slot index (0-5)
)
```

**2. Add `setSlotOverride` method to `TurnBattleViewModel`:**

```kotlin
fun setSlotOverride(slotIndex: Int, override: SlotOverride?) {
    val s = _setup.value ?: return
    val newOverrides = s.teamOverrides.toMutableMap()
    if (override == null) newOverrides.remove(slotIndex)
    else newOverrides[slotIndex] = override
    _setup.value = s.copy(teamOverrides = newOverrides)
}
```

**3. Replace `startBattleFromSetup` to build full team:**

```kotlin
fun startBattleFromSetup(teamIds: List<Int>) {
    val s = _setup.value ?: return
    if (s.selectedMoveNames.isEmpty()) return
    viewModelScope.launch {
        _isLoading.value = true
        try {
            val gen = settingsRepo.selectedGen.first()
            val allPokemon = repo.getPokemonList()
            val opponentDetail = repo.getPokemonDetail(allPokemon.random().id)

            // Build player team — slot 0 uses setup's full config; others get defaults + overrides
            val playerTeam = teamIds.mapIndexed { idx, pokemonId ->
                val detail = if (idx == 0) s.playerDetail else {
                    try { repo.getPokemonDetail(pokemonId) } catch (_: Exception) { s.playerDetail }
                }
                val ov = s.teamOverrides[idx]
                val level = ov?.level ?: s.level
                val statConfig = ov?.statConfig ?: s.statConfig
                val nature = ov?.nature ?: s.nature
                val heldItem = ov?.heldItem ?: s.heldItem
                val moves = if (idx == 0) {
                    resolveMoves(s.selectedMoveNames)
                } else {
                    resolveMoves(learnableMoves(detail, level).filter { it.available }.take(4).map { it.name })
                }
                BattleEngine.buildBattlePokemon(detail, level, moves, statConfig, nature, heldItem)
            }

            val opponentMoves = resolveMoves(opponentDetail.moves.take(4).map { it.name })
            val opponentBattle = BattleEngine.buildBattlePokemon(opponentDetail, s.level, opponentMoves)

            _battleState.value = BattleEngine.startBattle(playerTeam, listOf(opponentBattle), gen)
        } catch (_: Exception) {
        } finally {
            _isLoading.value = false
        }
    }
}
```

**4. Replace `submitMove` to use `TurnAction`:**

```kotlin
fun submitMove(moveIndex: Int) {
    val ongoing = _battleState.value as? BattleState.Ongoing ?: return
    viewModelScope.launch {
        val gen = settingsRepo.selectedGen.first()
        val playerMove = ongoing.player.moves.getOrNull(moveIndex) ?: return@launch
        val playerAction = TurnAction.UseMove(playerMove)
        val aiAction = BattleEngine.aiPickAction(ongoing.opponent, ongoing.player, ongoing.opponentTeam, gen)
        _battleState.value = BattleEngine.resolveTurn(playerAction, aiAction, ongoing, gen)
    }
}
```

**5. Add `submitSwitch`:**

```kotlin
fun submitSwitch(targetIndex: Int) {
    val ongoing = _battleState.value as? BattleState.Ongoing ?: return
    viewModelScope.launch {
        val gen = settingsRepo.selectedGen.first()
        val playerAction = TurnAction.SwitchTo(targetIndex)
        val aiAction = BattleEngine.aiPickAction(ongoing.opponent, ongoing.player, ongoing.opponentTeam, gen)
        _battleState.value = BattleEngine.resolveTurn(playerAction, aiAction, ongoing, gen)
    }
}
```

**6. Add `confirmSwitch`:**

```kotlin
fun confirmSwitch(newIndex: Int) {
    val pending = _battleState.value as? BattleState.PendingSwitch ?: return
    _battleState.value = BattleEngine.confirmSwitch(newIndex, pending)
}
```

**7. Update `forfeit` to use new state shape:**

```kotlin
fun forfeit() {
    val ongoing = _battleState.value as? BattleState.Ongoing ?: return
    _battleState.value = BattleState.Lost(ongoing.log + listOf("You forfeited."))
}
```

**Note:** The old `startBattleFromSetup()` (no args) is replaced by `startBattleFromSetup(teamIds: List<Int>)`. The screen will pass `teamIds` — update the call site in `TurnBattleScreen` in Task 3.

- [ ] **Step 4: Run new tests**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.TurnBattleViewModelSwitchTest" 2>&1 | tail -20
```

Expected: PASS — 3 tests.

- [ ] **Step 5: Run all tests**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:testDebugUnitTest 2>&1 | tail -30
```

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleViewModel.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/battle/BattleSetupTest.kt
git commit -m "feat: wire TurnBattleViewModel to team-aware engine API"
```

---

## Task 3: Engine unit tests — switching scenarios

**Files:**
- Modify: `app/src/test/java/com/madmaxlgndklr/pokedex/battle/TeamBattleEngineTest.kt`

- [ ] **Step 1: Write the engine scenario tests**

Add the following classes to `TeamBattleEngineTest.kt` (after `BattleStateModelTest`):

```kotlin
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
        // After switch, player active index must be 2
        assertTrue(result is BattleState.Ongoing || result is BattleState.PendingSwitch)
        if (result is BattleState.Ongoing) {
            assertEquals(2, result.playerActiveIndex)
        }
    }

    @Test
    fun `voluntary switch — opponent attacks incoming pokemon not outgoing`() {
        // Give the "in" pokemon very low HP so we can detect whether it was targeted
        val outgoing = fakePokemon("outgoing", hp = 200)
        val incoming = fakePokemon("incoming", hp = 1)  // will be fainted by tackle
        val playerTeam = listOf(outgoing, incoming)
        val opponent = fakePokemon("opponent", attack = 200)
        val state = ongoingState(playerTeam, listOf(opponent))

        val result = BattleEngine.resolveTurn(
            TurnAction.SwitchTo(1),
            TurnAction.UseMove(move),
            state,
            gen = 5
        )
        // incoming should be damaged (or fainted), outgoing should be at full HP
        when (result) {
            is BattleState.Ongoing -> {
                assertEquals(200, result.playerTeam[0].currentHp)  // outgoing untouched
                assertTrue(result.playerTeam[1].currentHp < 1)      // incoming damaged
            }
            is BattleState.PendingSwitch -> {
                assertEquals(200, result.playerTeam[0].currentHp)  // outgoing untouched
            }
            is BattleState.Lost -> { /* incoming fainted, no other members */ }
            else -> fail("Unexpected state: $result")
        }
    }

    @Test
    fun `faint mid-turn produces PendingSwitch when player has living members`() {
        // Opponent has huge attack, player active has 1 HP
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
            fakePokemon("fainted", hp = 0).copy(currentHp = 0),
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
            fakePokemon("fainted", hp = 0).copy(currentHp = 0),
            fakePokemon("alive", hp = 100)
        )
        val pending = BattleState.PendingSwitch(
            playerTeam = team, playerActiveIndex = 0,
            opponentTeam = listOf(fakePokemon("opp")), opponentActiveIndex = 0,
            log = emptyList()
        )
        // Out of bounds
        assertSame(pending, BattleEngine.confirmSwitch(99, pending))
        // Same as current active
        assertSame(pending, BattleEngine.confirmSwitch(0, pending))
        // Fainted pokemon
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
        // Water-type has 4x weakness to electric moves; electric opponent should trigger switch
        val waterPokemon = fakePokemon("water-poke", types = listOf("water"))
        val firePokemon = fakePokemon("fire-poke", types = listOf("fire"), hp = 100)
        val electricMove = BattleMove("thunderbolt", "electric", "special", 90, 15, 15)
        val electricOpponent = fakePokemon("electric-opp").copy(
            moves = listOf(electricMove)
        )
        val team = listOf(waterPokemon, firePokemon)

        val action = BattleEngine.aiPickAction(waterPokemon, electricOpponent, team, gen = 5)
        assertTrue("AI should switch water-type away from 4x electric weakness",
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
```

- [ ] **Step 2: Run to confirm tests pass**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.ResolveTurnSwitchTest" 2>&1 | tail -30
```

Expected: PASS — 9 tests.

- [ ] **Step 3: Run full suite**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:testDebugUnitTest 2>&1 | tail -30
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/madmaxlgndklr/pokedex/battle/TeamBattleEngineTest.kt
git commit -m "test: add engine tests for voluntary switch, faint-forced switch, and AI switching"
```

---

## Task 4: Update TurnBattleScreen — ongoing battle and PendingSwitch UI

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt`

This task has no new unit tests — UI changes are verified by building and running the app.

- [ ] **Step 1: Update `TurnBattleScreen` top-level dispatcher to handle `PendingSwitch`**

In `TurnBattleScreen`, the `when` block currently handles `BattleState.Ongoing`, terminal states, and `setup != null`. Add a `PendingSwitch` branch and update the `Ongoing` branch to pass a switch callback.

Replace the `Box(Modifier.fillMaxSize()) { when { ... } }` block:

```kotlin
Box(Modifier.fillMaxSize()) {
    when {
        isLoading -> {
            CircularProgressIndicator(color = GlowBlue, modifier = Modifier.align(Alignment.Center))
        }
        battleState is BattleState.PendingSwitch -> {
            val pending = battleState as BattleState.PendingSwitch
            PendingSwitchView(
                state = pending,
                onSwitch = { idx -> viewModel.confirmSwitch(idx) }
            )
        }
        battleState is BattleState.Ongoing -> {
            val ongoing = battleState as BattleState.Ongoing
            val started = remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (!started.value) {
                    started.value = true
                    CryPlayer.play(ongoing.opponent.detail.name)
                }
            }
            OngoingBattleView(
                state = ongoing,
                onMove = { idx ->
                    CryPlayer.play(ongoing.player.detail.name)
                    viewModel.submitMove(idx)
                },
                onSwitch = { idx -> viewModel.submitSwitch(idx) },
                onForfeit = { viewModel.forfeit() }
            )
        }
        battleState != null -> {
            val won = battleState is BattleState.Won
            val log = if (won) (battleState as BattleState.Won).log
                      else (battleState as BattleState.Lost).log
            BattleEndView(
                won = won,
                log = log,
                onRematch = { viewModel.resetToSetup() },
                onBack = onBack
            )
        }
        setup != null -> {
            val s = setup!!
            BattleSetupView(
                setup = s,
                moves = learnableMoves(s.playerDetail, s.level),
                viewModel = viewModel,
                onLevelChange = { viewModel.setSetupLevel(it) },
                onToggleMove = { viewModel.toggleSetupMove(it) },
                onFight = { viewModel.startBattleFromSetup(teamIds) }
            )
        }
        else -> {
            Text(
                "NO TEAM",
                fontFamily = PressStart2P, fontSize = 8.sp, color = PokedexCream.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
```

- [ ] **Step 2: Add `PendingSwitchView` composable**

Add this private composable to `TurnBattleScreen.kt`:

```kotlin
@Composable
private fun PendingSwitchView(
    state: BattleState.PendingSwitch,
    onSwitch: (Int) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "CHOOSE YOUR NEXT POKÉMON",
            fontFamily = PressStart2P, fontSize = 7.sp, color = CaughtGold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        // Show log (last 4 lines)
        state.log.takeLast(4).forEach { line ->
            Text(line, fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, lineHeight = 9.sp)
        }
        Spacer(Modifier.height(8.dp))
        state.playerTeam.forEachIndexed { idx, poke ->
            val isCurrent = idx == state.playerActiveIndex
            val isFainted = poke.currentHp <= 0
            val isAvailable = !isCurrent && !isFainted
            val hpFraction = (poke.currentHp.toFloat() / poke.maxHp).coerceIn(0f, 1f)
            val bgAlpha = if (isAvailable) 0.6f else 0.2f
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PokedexDark.copy(alpha = bgAlpha), RoundedCornerShape(6.dp))
                    .then(
                        if (isAvailable) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSwitch(idx) } else Modifier
                    )
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = RetrofitClient.spriteUrl(poke.detail.id),
                    contentDescription = poke.detail.name,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        poke.detail.name.uppercase(),
                        fontFamily = PressStart2P, fontSize = 6.sp,
                        color = if (isAvailable) PokedexCream else PokedexCream.copy(alpha = 0.3f)
                    )
                    Box(
                        Modifier.fillMaxWidth().height(4.dp)
                            .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    ) {
                        val barColor = when {
                            isFainted -> PokedexRed.copy(alpha = 0.3f)
                            hpFraction > 0.5f -> Color(0xFF44DD44)
                            hpFraction > 0.2f -> CaughtGold
                            else -> PokedexRed
                        }
                        Box(
                            Modifier.fillMaxWidth(hpFraction).height(4.dp)
                                .background(barColor, RoundedCornerShape(2.dp))
                        )
                    }
                }
                if (isFainted) {
                    Text("FAINT", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexRed.copy(alpha = 0.6f))
                }
                if (isCurrent) {
                    Text("OUT", fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue.copy(alpha = 0.6f))
                }
            }
        }
    }
}
```

- [ ] **Step 3: Update `OngoingBattleView` signature to accept `onSwitch`**

Change the signature of `OngoingBattleView`:

```kotlin
@Composable
private fun OngoingBattleView(
    state: BattleState.Ongoing,
    onMove: (Int) -> Unit,
    onSwitch: (Int) -> Unit,
    onForfeit: () -> Unit
)
```

- [ ] **Step 4: Add team strip and SWITCH button to `OngoingBattleView`**

Inside `OngoingBattleView`, after the move buttons `Column` and before the FORFEIT text, add:

```kotlin
// Team strip
Row(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    modifier = Modifier.fillMaxWidth()
) {
    state.playerTeam.forEachIndexed { idx, poke ->
        val isActive = idx == state.playerActiveIndex
        val isFainted = poke.currentHp <= 0
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(
                    when {
                        isActive -> GlowBlue.copy(alpha = 0.3f)
                        isFainted -> PokedexDark.copy(alpha = 0.2f)
                        else -> PokedexDark.copy(alpha = 0.5f)
                    },
                    RoundedCornerShape(4.dp)
                )
                .border(
                    1.dp,
                    if (isActive) GlowBlue else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
                .then(
                    if (!isActive && !isFainted)
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSwitch(idx) }
                    else Modifier
                )
        ) {
            AsyncImage(
                model = RetrofitClient.spriteUrl(poke.detail.id),
                contentDescription = poke.detail.name,
                modifier = Modifier.size(28.dp).then(
                    if (isFainted) Modifier.background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    else Modifier
                )
            )
        }
    }
}
```

- [ ] **Step 5: Compile check**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD" | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt
git commit -m "feat: add PendingSwitch UI and team strip with voluntary switch to battle screen"
```

---

## Task 5: Per-battle slot overrides UI in setup screen

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt`

This task adds the team override cards to the setup screen. No new unit tests — compile check suffices.

- [ ] **Step 1: Add team slot cards row above the gen toggle in `BattleSetupView`**

In `BattleSetupView`, the first child of the outer `Column` is the Pokémon+level row. Add the following **after** that row (before the gen toggle `Row`):

```kotlin
// Team slot override strip
val teamOverrides by remember { mutableStateOf(setup.teamOverrides) }
// Note: teamIds is not available inside BattleSetupView — pass it from the caller.
// The strip shows which slots have overrides vs using global defaults.
// Slots beyond teamIds.size are not shown.
Text(
    "TEAM SLOTS",
    fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.5f)
)
```

Wait — `BattleSetupView` doesn't receive `teamIds`. We need to thread it through.

**Update `BattleSetupView` signature** to receive `teamIds: List<Int>`:

```kotlin
@Composable
private fun BattleSetupView(
    setup: BattleSetup,
    teamIds: List<Int>,
    moves: List<LearnableMove>,
    viewModel: TurnBattleViewModel,
    onLevelChange: (Int) -> Unit,
    onToggleMove: (String) -> Unit,
    onFight: () -> Unit
)
```

Update the call site in `TurnBattleScreen`:

```kotlin
BattleSetupView(
    setup = s,
    teamIds = teamIds,
    moves = learnableMoves(s.playerDetail, s.level),
    viewModel = viewModel,
    onLevelChange = { viewModel.setSetupLevel(it) },
    onToggleMove = { viewModel.toggleSetupMove(it) },
    onFight = { viewModel.startBattleFromSetup(teamIds) }
)
```

- [ ] **Step 2: Add the slot override strip inside `BattleSetupView`**

After the Pokémon+level row, before the gen toggle, add:

```kotlin
// Team slot strip — tap a slot to expand its overrides
var expandedSlot by remember { mutableStateOf<Int?>(null) }

if (teamIds.size > 1) {
    Text(
        "TEAM  (TAP TO CUSTOMIZE)",
        fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.5f)
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        itemsIndexed(teamIds) { idx, _ ->
            val hasOverride = setup.teamOverrides.containsKey(idx)
            val isExpanded = expandedSlot == idx
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        when {
                            isExpanded -> GlowBlue.copy(alpha = 0.3f)
                            hasOverride -> CaughtGold.copy(alpha = 0.2f)
                            else -> PokedexDark.copy(alpha = 0.4f)
                        },
                        RoundedCornerShape(4.dp)
                    )
                    .border(
                        1.dp,
                        when {
                            isExpanded -> GlowBlue
                            hasOverride -> CaughtGold
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(4.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expandedSlot = if (isExpanded) null else idx }
                    .padding(4.dp)
            ) {
                Text(
                    "${idx + 1}",
                    fontFamily = PressStart2P, fontSize = 7.sp,
                    color = if (isExpanded) GlowBlue else PokedexCream.copy(alpha = 0.7f)
                )
            }
        }
    }

    // Expanded slot controls
    val slot = expandedSlot
    if (slot != null && slot < teamIds.size) {
        val ov = setup.teamOverrides[slot]
        val slotLevel = ov?.level ?: setup.level
        val slotStatConfig = ov?.statConfig ?: setup.statConfig
        val slotNature = ov?.nature ?: setup.nature
        val slotHeldItem = ov?.heldItem ?: setup.heldItem

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PokedexDark.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SLOT ${slot + 1}",
                    fontFamily = PressStart2P, fontSize = 6.sp, color = GlowBlue
                )
                if (ov != null) {
                    Text(
                        "RESET",
                        fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexRed,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.setSlotOverride(slot, null) }
                    )
                }
            }
            LevelPicker(
                level = slotLevel,
                onLevelChange = { newLevel ->
                    val newOv = (ov ?: SlotOverride()).copy(level = newLevel.coerceIn(1, 100))
                    viewModel.setSlotOverride(slot, newOv)
                }
            )
            // Nature (gen 3+) — always visible when slot is expanded
            if (gen >= 3) {
                Text(
                    "NATURE: ${slotNature.name.uppercase()}",
                    fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue
                )
                NaturePicker(
                    selectedNature = slotNature,
                    onNatureSelected = { nature ->
                        val newOv = (ov ?: SlotOverride()).copy(nature = nature)
                        viewModel.setSlotOverride(slot, newOv)
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 3: Add missing import for `itemsIndexed`**

At the top of `TurnBattleScreen.kt`, ensure this import is present (add it if not):

```kotlin
import androidx.compose.foundation.lazy.itemsIndexed
```

- [ ] **Step 4: Compile check**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD" | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt
git commit -m "feat: add per-slot team override UI to battle setup screen"
```

---

## Task 6: Full test sweep and compile verification

**Files:** None — verification only.

- [ ] **Step 1: Run full unit test suite**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:testDebugUnitTest 2>&1 | tail -40
```

Expected: all tests pass. Note the test count — should be ≥ all tests from before plus the new ones.

- [ ] **Step 2: Full debug build**

```
cd /home/madmaxlgndklr/Git/sandbox/Pokedex
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|warning:|BUILD" | tail -30
```

Expected: `BUILD SUCCESSFUL` with 0 errors.

- [ ] **Step 3: If any failures, fix and re-run before committing**

Common issues to check:
- `MoveAction` still referenced anywhere → replace with `TurnAction.UseMove`
- Old `BattleState.Ongoing(player, opponent, log)` constructor called anywhere → update to team-based constructor
- `startBattleFromSetup()` called without args in navigation → update to `startBattleFromSetup(teamIds)`
- `aiPickMove` called directly on opponent from outside the engine → replace with `aiPickAction`

Search for stale references:

```
grep -rn "MoveAction\|\.player,\s*\.opponent\|startBattleFromSetup()" \
  /home/madmaxlgndklr/Git/sandbox/Pokedex/app/src/main \
  --include="*.kt"
```

- [ ] **Step 4: Commit any fixes**

```bash
git add -p  # stage only the fix files
git commit -m "fix: clean up stale MoveAction and old BattleState references"
```
