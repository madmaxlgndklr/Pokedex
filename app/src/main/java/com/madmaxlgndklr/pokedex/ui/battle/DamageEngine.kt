package com.madmaxlgndklr.pokedex.ui.battle

import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.ui.common.typeWeaknesses
import kotlin.math.floor
import kotlin.math.roundToInt

data class DamageParams(
    val gen: Int,
    val level: Int,
    val attackBaseStat: Int,
    val defenseBaseStat: Int,
    val attackStatIndex: Int = 1,
    val defenseStatIndex: Int = 2,
    val attackerStatConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
    val attackerNature: Nature = Natures.HARDY,
    val defenderStatConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
    val defenderNature: Nature = Natures.HARDY,
    val heldItem: HeldItem? = null,
    val basePower: Int,
    val moveType: String,
    val moveCategory: String,
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

        var atk = StatFormulas.computeStat(
            params.attackBaseStat, params.attackerStatConfig,
            params.attackerNature, params.attackStatIndex, params.level
        )
        val def = StatFormulas.computeStat(
            params.defenseBaseStat, params.defenderStatConfig,
            params.defenderNature, params.defenseStatIndex, params.level
        )

        val itemEffect = params.heldItem?.let { HeldItemEffect.from(it.name) } ?: HeldItemEffect.None
        if (itemEffect is HeldItemEffect.StatMultiplier && itemEffect.statIndex == params.attackStatIndex) {
            atk = floor(atk * itemEffect.factor).toInt()
        }

        val base = floor(
            (floor(2.0 * params.level / 5 + 2) * params.basePower * atk / def) / 50 + 2
        ).toInt()

        val critMult = when {
            !params.criticalHit -> 1f
            params.gen <= 5 -> 2f
            else -> 1.5f
        }

        val (randMin, randMax) = if (params.gen == 1) 217f / 255f to 1f else 0.85f to 1f

        fun finalDamage(rand: Float): Int {
            var dmg = (base * stab * effectiveness * critMult * rand).roundToInt().coerceAtLeast(1)
            when (itemEffect) {
                is HeldItemEffect.DamageMultiplier -> dmg = floor(dmg * itemEffect.factor).toInt()
                is HeldItemEffect.SuperEffectiveBoost -> if (effectiveness > 1f) dmg = floor(dmg * itemEffect.factor).toInt()
                is HeldItemEffect.TypeMultiplier -> {
                    val itemType = HeldItemEffect.typeFor(params.heldItem?.name ?: "")
                    if (itemType != null && itemType == params.moveType) dmg = floor(dmg * itemEffect.factor).toInt()
                }
                else -> Unit
            }
            return dmg
        }

        val min = finalDamage(randMin)
        val max = finalDamage(randMax)
        val avg = finalDamage((randMin + randMax) / 2f)

        return DamageResult(min, max, avg, effectivenessLabel(effectiveness))
    }

    fun computeEffectiveness(gen: Int, moveType: String, defenderTypes: List<String>): Float {
        val filtered = when {
            gen == 1 -> defenderTypes.filterNot { it in setOf("steel", "dark", "fairy") }
            gen <= 5 -> defenderTypes.filterNot { it == "fairy" }
            else -> defenderTypes
        }
        return filtered.fold(1f) { acc, defType ->
            acc * (typeWeaknesses(listOf(defType))[moveType] ?: 1f)
        }
    }

    private fun effectivenessLabel(e: Float) = when {
        e == 0f -> "0×"
        e < 1f  -> "${e}×"
        e > 1f  -> "${e}×"
        else    -> "1×"
    }

    fun isPhysicalGen23(moveType: String) = moveType in GEN23_PHYSICAL
}
