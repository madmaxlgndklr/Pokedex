package com.madmaxlgndklr.pokedex.ui.battle

import com.madmaxlgndklr.pokedex.model.PokemonDetail
import kotlin.random.Random

data class BattlePokemon(
    val detail: PokemonDetail,
    val level: Int = 50,
    val maxHp: Int,
    val currentHp: Int,
    val moves: List<BattleMove>
)

data class BattleMove(
    val name: String,
    val type: String,
    val category: String,
    val power: Int?,
    val maxPp: Int,
    val currentPp: Int
)

data class MoveAction(
    val pokemon: BattlePokemon,
    val move: BattleMove,
    val target: BattlePokemon
)

sealed class BattleState {
    data class Ongoing(
        val player: BattlePokemon,
        val opponent: BattlePokemon,
        val log: List<String>
    ) : BattleState()

    data class Won(val log: List<String>) : BattleState()
    data class Lost(val log: List<String>) : BattleState()
}

object BattleEngine {

    fun startBattle(player: BattlePokemon, opponent: BattlePokemon, gen: Int): BattleState.Ongoing {
        return BattleState.Ongoing(
            player = player,
            opponent = opponent,
            log = listOf("A wild ${opponent.detail.name.uppercase()} appeared!")
        )
    }

    fun buildBattlePokemon(detail: PokemonDetail, level: Int, moves: List<BattleMove>): BattlePokemon {
        val hpBase = detail.stats.firstOrNull { it.name == "hp" }?.value ?: 45
        val maxHp = computeHp(hpBase, level)
        return BattlePokemon(detail, level, maxHp, maxHp, moves)
    }

    fun resolveTurn(
        playerAction: MoveAction,
        opponentAction: MoveAction,
        state: BattleState.Ongoing,
        gen: Int
    ): BattleState {
        val playerSpeed = statValue(playerAction.pokemon, "speed")
        val opponentSpeed = statValue(opponentAction.pokemon, "speed")
        val playerGoesFirst = when {
            playerSpeed > opponentSpeed -> true
            playerSpeed < opponentSpeed -> false
            else -> Random.nextBoolean()
        }

        var player = state.player
        var opponent = state.opponent
        val log = mutableListOf<String>()

        fun applyMove(attacker: BattlePokemon, move: BattleMove, defender: BattlePokemon): Pair<BattlePokemon, BattlePokemon> {
            if (move.power == null || move.power == 0 || move.category == "status") {
                log.add("${attacker.detail.name.uppercase()} used ${move.name.uppercase().replace("-", " ")}!")
                return attacker to defender
            }
            val isPhysical = when {
                gen >= 4 -> move.category == "physical"
                gen <= 1 -> true // Gen 1 all moves use Atk/Def
                else -> DamageEngine.isPhysicalGen23(move.type)
            }
            val atkStat = if (isPhysical) "attack" else "special-attack"
            val defStat = if (isPhysical) "defense" else "special-defense"
            val params = DamageParams(
                gen = gen,
                level = attacker.level,
                attackBaseStat = statValue(attacker, atkStat),
                defenseBaseStat = statValue(defender, defStat),
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
            val updatedAttacker = if (attacker == player)
                attacker.copy(moves = attacker.moves.map { if (it == move) updatedMove else it })
            else
                attacker.copy(moves = attacker.moves.map { if (it == move) updatedMove else it })
            return updatedAttacker to defender.copy(currentHp = newHp)
        }

        val firstAttacker = if (playerGoesFirst) playerAction else opponentAction
        val secondAttacker = if (playerGoesFirst) opponentAction else playerAction

        val (fa, fd) = applyMove(firstAttacker.pokemon, firstAttacker.move, firstAttacker.target)
        if (playerGoesFirst) { player = fa; opponent = fd } else { opponent = fa; player = fd }

        if (opponent.currentHp <= 0) {
            log.add("${opponent.detail.name.uppercase()} fainted!")
            return BattleState.Won(log)
        }
        if (player.currentHp <= 0) {
            log.add("${player.detail.name.uppercase()} fainted!")
            return BattleState.Lost(log)
        }

        // Use updated player/opponent vars — not stale MoveAction references — so first-move HP changes carry over
        val (secondPokemon, secondTarget) = if (playerGoesFirst) opponent to player else player to opponent
        val (sa, sd) = applyMove(secondPokemon, secondAttacker.move, secondTarget)
        if (playerGoesFirst) { opponent = sa; player = sd } else { player = sa; opponent = sd }

        if (opponent.currentHp <= 0) {
            log.add("${opponent.detail.name.uppercase()} fainted!")
            return BattleState.Won(log)
        }
        if (player.currentHp <= 0) {
            log.add("${player.detail.name.uppercase()} fainted!")
            return BattleState.Lost(log)
        }

        return state.copy(player = player, opponent = opponent, log = state.log + log)
    }

    fun aiPickMove(pokemon: BattlePokemon): BattleMove =
        pokemon.moves
            .filter { it.currentPp > 0 && it.power != null && it.category != "status" }
            .maxByOrNull { it.power ?: 0 }
            ?: pokemon.moves.first { it.currentPp > 0 }

    private fun statValue(pokemon: BattlePokemon, statName: String): Int =
        pokemon.detail.stats.firstOrNull { it.name == statName }?.value ?: 50

    fun computeHp(base: Int, level: Int): Int =
        ((2.0 * base + 31) * level / 100 + level + 10).toInt()

}
