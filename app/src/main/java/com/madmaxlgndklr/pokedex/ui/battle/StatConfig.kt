package com.madmaxlgndklr.pokedex.ui.battle

import kotlin.math.floor
import kotlin.math.sqrt

sealed class StatConfig {
    data class Gen12Config(
        val dvs: IntArray,     // index 0–4: HP, Atk, Def, Spe, Spc — values 0–15
        val statExp: IntArray  // same indices — values 0–65535
    ) : StatConfig() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Gen12Config) return false
            return dvs.contentEquals(other.dvs) && statExp.contentEquals(other.statExp)
        }
        override fun hashCode() = 31 * dvs.contentHashCode() + statExp.contentHashCode()
    }

    data class Gen3PlusConfig(
        val ivs: IntArray,  // index 0–5: HP, Atk, Def, SpAtk, SpDef, Spe — values 0–31
        val evs: IntArray   // same indices — values 0–252, sum ≤ 510
    ) : StatConfig() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Gen3PlusConfig) return false
            return ivs.contentEquals(other.ivs) && evs.contentEquals(other.evs)
        }
        override fun hashCode() = 31 * ivs.contentHashCode() + evs.contentHashCode()
    }
}

data class Nature(val name: String, val boostedStat: Int?, val droppedStat: Int?)

// Stat indices (unified, Gen3+ convention): 0=HP, 1=Atk, 2=Def, 3=SpAtk, 4=SpDef, 5=Spe
// Gen12Config internal indices:             0=HP, 1=Atk, 2=Def, 3=Spe,   4=Spc
// For Gen12: unified 3 (SpAtk) and 4 (SpDef) both map to Gen12 slot 4 (Spc)
//            unified 5 (Spe) maps to Gen12 slot 3

object Natures {
    val HARDY    = Nature("Hardy",    null, null)
    val LONELY   = Nature("Lonely",   1,    2)
    val BRAVE    = Nature("Brave",    1,    5)
    val ADAMANT  = Nature("Adamant",  1,    3)
    val NAUGHTY  = Nature("Naughty",  1,    4)
    val BOLD     = Nature("Bold",     2,    1)
    val DOCILE   = Nature("Docile",   null, null)
    val RELAXED  = Nature("Relaxed",  2,    5)
    val IMPISH   = Nature("Impish",   2,    3)
    val LAX      = Nature("Lax",      2,    4)
    val TIMID    = Nature("Timid",    5,    1)
    val HASTY    = Nature("Hasty",    5,    2)
    val SERIOUS  = Nature("Serious",  null, null)
    val JOLLY    = Nature("Jolly",    5,    3)
    val NAIVE    = Nature("Naive",    5,    4)
    val MODEST   = Nature("Modest",   3,    1)
    val MILD     = Nature("Mild",     3,    2)
    val QUIET    = Nature("Quiet",    3,    5)
    val BASHFUL  = Nature("Bashful",  null, null)
    val RASH     = Nature("Rash",     3,    4)
    val CALM     = Nature("Calm",     4,    1)
    val GENTLE   = Nature("Gentle",   4,    2)
    val SASSY    = Nature("Sassy",    4,    5)
    val CAREFUL  = Nature("Careful",  4,    3)
    val QUIRKY   = Nature("Quirky",   null, null)

    val ALL = listOf(
        HARDY, LONELY, BRAVE, ADAMANT, NAUGHTY,
        BOLD, DOCILE, RELAXED, IMPISH, LAX,
        TIMID, HASTY, SERIOUS, JOLLY, NAIVE,
        MODEST, MILD, QUIET, BASHFUL, RASH,
        CALM, GENTLE, SASSY, CAREFUL, QUIRKY
    )
}

object StatFormulas {

    // Unified stat index (Gen3+ convention) -> Gen12Config array index
    private fun gen12SlotFor(unifiedIndex: Int): Int = when (unifiedIndex) {
        0 -> 0  // HP
        1 -> 1  // Atk
        2 -> 2  // Def
        3 -> 4  // SpAtk -> Spc
        4 -> 4  // SpDef -> Spc
        5 -> 3  // Spe
        else -> unifiedIndex
    }

    fun natureMultiplier(nature: Nature, statIndex: Int): Float {
        if (statIndex == 0) return 1.0f  // HP never gets a nature modifier
        return when (statIndex) {
            nature.boostedStat -> 1.1f
            nature.droppedStat -> 0.9f
            else -> 1.0f
        }
    }

    // Compute any non-HP stat. statIndex uses unified Gen3+ convention (0=HP, 1=Atk, ... 5=Spe).
    // For Gen12Config, nature is ignored (Gen I/II have no natures).
    fun computeStat(base: Int, config: StatConfig, nature: Nature, statIndex: Int, level: Int): Int {
        return when (config) {
            is StatConfig.Gen12Config -> {
                val slot = gen12SlotFor(statIndex)
                val dv = config.dvs.getOrElse(slot) { 0 }
                val se = config.statExp.getOrElse(slot) { 0 }
                val inner = (base + dv) * 2 + floor(sqrt(se.toDouble()) / 4).toInt()
                floor(inner.toDouble() * level / 100).toInt() + 5
            }
            is StatConfig.Gen3PlusConfig -> {
                val iv = config.ivs.getOrElse(statIndex) { 31 }
                val ev = config.evs.getOrElse(statIndex) { 0 }
                val inner = floor((2.0 * base + iv + floor(ev / 4.0)) * level / 100).toInt() + 5
                floor(inner * natureMultiplier(nature, statIndex)).toInt()
            }
        }
    }

    // HP uses a different formula and never gets a nature modifier.
    fun computeHp(base: Int, config: StatConfig, level: Int): Int {
        return when (config) {
            is StatConfig.Gen12Config -> {
                val dv = config.dvs.getOrElse(0) { 15 }
                val se = config.statExp.getOrElse(0) { 0 }
                val inner = (base + dv) * 2 + floor(sqrt(se.toDouble()) / 4).toInt()
                floor(inner.toDouble() * level / 100).toInt() + level + 10
            }
            is StatConfig.Gen3PlusConfig -> {
                val iv = config.ivs.getOrElse(0) { 31 }
                val ev = config.evs.getOrElse(0) { 0 }
                floor((2.0 * base + iv + floor(ev / 4.0)) * level / 100).toInt() + level + 10
            }
        }
    }

    fun isEvSumValid(evs: IntArray): Boolean = evs.sum() <= 510
}
