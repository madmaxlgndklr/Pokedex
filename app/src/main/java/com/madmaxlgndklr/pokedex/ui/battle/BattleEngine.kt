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

    /** Backward-compat: single-Pokémon battle (wraps both in single-element lists). */
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
     * Faint after first move: if the player fainted, returns PendingSwitch immediately.
     * Opponent auto-replaces on faint with the next available team member.
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
            } else if (opponentAction is TurnAction.SwitchTo) {
                val oppNewIndex = opponentAction.targetIndex.coerceIn(0, updatedState.opponentTeam.lastIndex)
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
            is TurnAction.SwitchTo -> null
        }

        val playerSpeed = statValue(state.player, "speed")
        val opponentSpeed = statValue(state.opponent, "speed")
        val playerGoesFirst = when {
            playerSpeed > opponentSpeed -> true
            playerSpeed < opponentSpeed -> false
            else -> Random.nextBoolean()
        }

        val playerTeam = state.playerTeam.toMutableList()
        val opponentTeam = state.opponentTeam.toMutableList()
        var playerIdx = state.playerActiveIndex
        var opponentIdx = state.opponentActiveIndex
        val log = mutableListOf<String>()

        fun player() = playerTeam[playerIdx]
        fun opponent() = opponentTeam[opponentIdx]

        // Handle opponent switching
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

        // First move — track who is actually attacking (may differ from speed priority when opp switched)
        val firstIsPlayer = playerGoesFirst
        val firstAttackerIsPlayer: Boolean
        val (firstAttacker, firstMove, firstDefender) = when {
            firstIsPlayer -> { firstAttackerIsPlayer = true; Triple(player(), playerMove, opponent()) }
            opponentMove != null -> { firstAttackerIsPlayer = false; Triple(opponent(), opponentMove, player()) }
            else -> { firstAttackerIsPlayer = true; Triple(player(), playerMove, opponent()) }  // opp switched, player always attacks
        }

        val (fa, fd) = applyMove(firstAttacker, firstMove, firstDefender)
        commitMove(fa, fd, firstAttackerIsPlayer)

        // Check faint after first move — check simultaneous faint first
        val playerFaintedFirst = playerTeam[playerIdx].currentHp <= 0
        val opponentFaintedFirst = opponentTeam[opponentIdx].currentHp <= 0
        if (playerFaintedFirst && opponentFaintedFirst) {
            log.add("${player().detail.name.uppercase()} fainted!")
            log.add("${opponent().detail.name.uppercase()} fainted!")
            return BattleState.Lost(state.log + log)   // player loses on simultaneous faint
        }
        if (!firstIsPlayer && playerFaintedFirst) {
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
        if (firstIsPlayer && opponentFaintedFirst) {
            log.add("${opponent().detail.name.uppercase()} fainted!")
            val endLog = state.log + log
            val aliveOpp = opponentTeam.count { it.currentHp > 0 }
            if (aliveOpp == 0) return BattleState.Won(endLog)
            val nextIdx = aiForcedSwitchIndex(opponentTeam) ?: return BattleState.Won(endLog)
            opponentIdx = nextIdx
            log.add("A new ${opponent().detail.name.uppercase()} appeared!")
        }

        // Second move
        val secondIsPlayer = !firstIsPlayer
        if (opponentMove != null || secondIsPlayer) {
            val (secondAttacker, secondMove, secondDefender) =
                if (secondIsPlayer) Triple(player(), playerMove, opponent())
                else Triple(opponent(), opponentMove!!, player())

            if (secondAttacker.currentHp > 0) {
                val (sa, sd) = applyMove(secondAttacker, secondMove, secondDefender)
                commitMove(sa, sd, !firstAttackerIsPlayer)

                val playerFaintedSecond = playerTeam[playerIdx].currentHp <= 0
                val opponentFaintedSecond = opponentTeam[opponentIdx].currentHp <= 0
                if (playerFaintedSecond && opponentFaintedSecond) {
                    log.add("${player().detail.name.uppercase()} fainted!")
                    log.add("${opponent().detail.name.uppercase()} fainted!")
                    return BattleState.Lost(state.log + log)
                }
                if (!secondIsPlayer && playerFaintedSecond) {
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
                if (secondIsPlayer && opponentFaintedSecond) {
                    log.add("${opponent().detail.name.uppercase()} fainted!")
                    val endLog = state.log + log
                    val aliveOpp = opponentTeam.count { it.currentHp > 0 }
                    if (aliveOpp == 0) return BattleState.Won(endLog)
                    val nextIdx = aiForcedSwitchIndex(opponentTeam) ?: return BattleState.Won(endLog)
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
        val bestIdx = team.indices
            .filter { i -> team[i] !== active && team[i].currentHp > 0 }
            .maxByOrNull { switchScore(team[it], opponentActive, gen) }

        if (bestIdx != null) {
            val bestScore = switchScore(team[bestIdx], opponentActive, gen)
            val opponentBestMoveType = opponentActive.moves
                .filter { it.power != null && it.power > 0 }
                .maxByOrNull { it.power ?: 0 }?.type ?: "normal"
            val has4xWeakness = DamageEngine.computeEffectiveness(gen, opponentBestMoveType, active.detail.types) >= 4f
            val lowHp = active.currentHp.toFloat() / active.maxHp < 0.25f
            val shouldSwitch = (has4xWeakness || lowHp) && bestScore > currentScore * 1.5f
            if (shouldSwitch) return TurnAction.SwitchTo(bestIdx)
        }

        return TurnAction.UseMove(aiPickMove(active))
    }

    /** Picks highest-power move. Used by aiPickAction and kept for backward compat. */
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
        val defenseScore = if (typeAdvantage == 0f) 10f else 1f / typeAdvantage
        val hpFraction = candidate.currentHp.toFloat() / candidate.maxHp
        // Type advantage is primary; HP is a tie-breaker weighted at 25%
        return defenseScore * (0.75f + hpFraction * 0.25f)
    }

    private fun aiForcedSwitchIndex(team: List<BattlePokemon>): Int? =
        team.indices.firstOrNull { team[it].currentHp > 0 }

    private fun applyMoveInternal(
        attacker: BattlePokemon,
        move: BattleMove,
        defender: BattlePokemon,
        gen: Int,
        log: MutableList<String>
    ): Pair<BattlePokemon, BattlePokemon> {
        if (move.power == null || move.power == 0 || move.category == "status") {
            log.add("${attacker.detail.name.uppercase()} used ${move.name.uppercase().replace("-", " ")}!")
            return attacker to defender
        }
        val isPhysical = when {
            gen >= 4 -> move.category == "physical"
            else -> DamageEngine.isPhysicalGen23(move.type)  // Gen 1/2/3: type determines physical/special
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
