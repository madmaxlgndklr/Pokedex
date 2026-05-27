package com.madmaxlgndklr.pokedex.ui.battle

import com.madmaxlgndklr.pokedex.ui.common.typeWeaknesses
import kotlin.math.floor
import kotlin.math.roundToInt

data class DamageParams(
    val gen: Int,
    val level: Int,
    val attackBaseStat: Int,
    val defenseBaseStat: Int,
    val attackEVs: Int = 0,           // 0–252
    val defenseEVs: Int = 0,
    val natureMultiplier: Float = 1f, // 0.9, 1.0, or 1.1
    val basePower: Int,
    val moveType: String,
    val moveCategory: String,         // "physical" | "special" | "status"
    val attackerTypes: List<String>,
    val defenderTypes: List<String>,
    val criticalHit: Boolean = false
)

data class DamageResult(
    val min: Int,
    val max: Int,
    val average: Int,
    val effectivenessLabel: String
)

object DamageEngine {

    // Gen 2-3 physical types — everything else is special
    private val GEN23_PHYSICAL = setOf(
        "normal", "fighting", "flying", "poison", "ground",
        "rock", "bug", "ghost", "steel"
    )

    fun calculate(params: DamageParams): DamageResult {
        if (params.moveCategory == "status" || params.basePower == 0) {
            return DamageResult(0, 0, 0, "—")
        }

        val effectiveness = computeEffectiveness(params.gen, params.moveType, params.defenderTypes)
        val stab = if (params.moveType in params.attackerTypes) 1.5f else 1f

        val atk = computeStat(
            base = params.attackBaseStat,
            evs = params.attackEVs,
            level = params.level,
            nature = params.natureMultiplier
        )
        val def = computeStat(
            base = params.defenseBaseStat,
            evs = params.defenseEVs,
            level = params.level,
            nature = 1f
        )

        val base = floor(
            (floor(2.0 * params.level / 5 + 2) * params.basePower * atk / def) / 50 + 2
        ).toInt()

        val critMult = when {
            !params.criticalHit -> 1f
            params.gen <= 5 -> 2f
            else -> 1.5f
        }

        val (randMin, randMax) = if (params.gen == 1) 217f / 255f to 1f else 0.85f to 1f

        fun finalDamage(rand: Float) =
            (base * stab * effectiveness * critMult * rand).roundToInt().coerceAtLeast(1)

        val min = finalDamage(randMin)
        val max = finalDamage(randMax)
        val avg = finalDamage((randMin + randMax) / 2f)

        return DamageResult(min, max, avg, effectivenessLabel(effectiveness))
    }

    // Returns the stat value at a given level with 31 IVs assumed
    private fun computeStat(base: Int, evs: Int, level: Int, nature: Float): Int {
        val inner = floor((2.0 * base + 31 + floor(evs / 4.0)) * level / 100).toInt() + 5
        return floor(inner * nature).toInt()
    }

    fun computeEffectiveness(gen: Int, moveType: String, defenderTypes: List<String>): Float {
        val filtered = when {
            gen == 1 -> defenderTypes.filterNot { it in setOf("steel", "dark", "fairy") }
            gen <= 5 -> defenderTypes.filterNot { it == "fairy" }
            else -> defenderTypes
        }
        return filtered.fold(1f) { acc, defType ->
            acc * (typeWeaknesses(listOf(defType))[moveType] ?: 1f).let {
                // typeWeaknesses returns only non-1 values; re-check if absent → 1f
                if (typeWeaknesses(listOf(defType)).containsKey(moveType)) it else 1f
            }
        }
    }

    private fun effectivenessLabel(e: Float) = when {
        e == 0f -> "0×"
        e < 1f  -> "${e}×"
        e > 1f  -> "${e}×"
        else    -> "1×"
    }

    // Whether a move type is physical in Gen 2-3
    fun isPhysicalGen23(moveType: String) = moveType in GEN23_PHYSICAL
}
