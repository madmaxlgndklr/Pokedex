package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.ui.battle.Natures
import com.madmaxlgndklr.pokedex.ui.battle.StatConfig
import com.madmaxlgndklr.pokedex.ui.battle.StatFormulas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatConfigTest {

    @Test
    fun `gen1 non-HP stat - bulbasaur atk dv15 statExp0 level50`() {
        val cfg = StatConfig.Gen12Config(
            dvs = intArrayOf(15, 15, 15, 15, 15),
            statExp = intArrayOf(0, 0, 0, 0, 0)
        )
        val result = StatFormulas.computeStat(49, cfg, Natures.HARDY, 1, 50)
        assertEquals(69, result)
    }

    @Test
    fun `gen1 HP stat - bulbasaur hp dv15 statExp0 level50`() {
        val cfg = StatConfig.Gen12Config(
            dvs = intArrayOf(15, 15, 15, 15, 15),
            statExp = intArrayOf(0, 0, 0, 0, 0)
        )
        val result = StatFormulas.computeHp(45, cfg, 50)
        assertEquals(120, result)
    }

    @Test
    fun `gen1 stat with non-zero statExp increases result`() {
        val cfgZero = StatConfig.Gen12Config(intArrayOf(15, 15, 15, 15, 15), intArrayOf(0, 0, 0, 0, 0))
        val cfgFull = StatConfig.Gen12Config(intArrayOf(15, 15, 15, 15, 15), intArrayOf(65535, 65535, 65535, 65535, 65535))
        val base = StatFormulas.computeStat(49, cfgZero, Natures.HARDY, 1, 50)
        val boosted = StatFormulas.computeStat(49, cfgFull, Natures.HARDY, 1, 50)
        assertTrue(boosted > base)
    }

    @Test
    fun `gen3plus non-HP stat - bulbasaur atk iv31 ev0 hardy level50`() {
        val cfg = StatConfig.Gen3PlusConfig(
            ivs = intArrayOf(31, 31, 31, 31, 31, 31),
            evs = intArrayOf(0, 0, 0, 0, 0, 0)
        )
        val result = StatFormulas.computeStat(49, cfg, Natures.HARDY, 1, 50)
        assertEquals(69, result)
    }

    @Test
    fun `gen3plus non-HP stat - bulbasaur atk iv31 ev0 adamant level50`() {
        val cfg = StatConfig.Gen3PlusConfig(
            ivs = intArrayOf(31, 31, 31, 31, 31, 31),
            evs = intArrayOf(0, 0, 0, 0, 0, 0)
        )
        val result = StatFormulas.computeStat(49, cfg, Natures.ADAMANT, 1, 50)
        assertEquals(75, result)
    }

    @Test
    fun `gen3plus HP stat - nature multiplier not applied`() {
        val cfg = StatConfig.Gen3PlusConfig(
            ivs = intArrayOf(31, 31, 31, 31, 31, 31),
            evs = intArrayOf(0, 0, 0, 0, 0, 0)
        )
        val result = StatFormulas.computeHp(45, cfg, 50)
        assertEquals(120, result)
    }

    @Test
    fun `gen3plus stat with ev252 increases result over ev0`() {
        val cfgNoEv = StatConfig.Gen3PlusConfig(intArrayOf(31,31,31,31,31,31), intArrayOf(0,0,0,0,0,0))
        val cfgEv = StatConfig.Gen3PlusConfig(intArrayOf(31,31,31,31,31,31), intArrayOf(0,252,0,0,0,0))
        val base = StatFormulas.computeStat(49, cfgNoEv, Natures.HARDY, 1, 50)
        val boosted = StatFormulas.computeStat(49, cfgEv, Natures.HARDY, 1, 50)
        assertTrue(boosted > base)
    }

    @Test
    fun `gen12config SpAtk and SpDef both use Spc DV index 4`() {
        val cfg = StatConfig.Gen12Config(
            dvs = intArrayOf(15, 15, 15, 15, 8),  // spc DV = 8
            statExp = intArrayOf(0, 0, 0, 0, 0)
        )
        val spatk = StatFormulas.computeStat(49, cfg, Natures.HARDY, 3, 50)
        val spdef = StatFormulas.computeStat(49, cfg, Natures.HARDY, 4, 50)
        assertEquals("SpAtk and SpDef share the Spc DV", spatk, spdef)
    }

    @Test
    fun `gen3plus EV sum 510 is valid`() {
        val evs = intArrayOf(0, 252, 252, 0, 6, 0)
        assertTrue(StatFormulas.isEvSumValid(evs))
    }

    @Test
    fun `gen3plus EV sum 511 is invalid`() {
        val evs = intArrayOf(0, 252, 252, 4, 3, 0)
        assertFalse(StatFormulas.isEvSumValid(evs))
    }

    @Test
    fun `all 25 natures exist in Natures ALL`() {
        assertEquals(25, Natures.ALL.size)
    }

    @Test
    fun `5 neutral natures have null boosted and dropped stats`() {
        val neutral = Natures.ALL.filter { it.boostedStat == null && it.droppedStat == null }
        assertEquals(5, neutral.size)
    }

    @Test
    fun `Adamant boosts Atk (index 1) and drops SpAtk (index 3)`() {
        assertEquals(1, Natures.ADAMANT.boostedStat)
        assertEquals(3, Natures.ADAMANT.droppedStat)
    }

    @Test
    fun `nature multiplier for boosted stat is 1_1`() {
        assertEquals(1.1f, StatFormulas.natureMultiplier(Natures.ADAMANT, 1), 0.001f)
    }

    @Test
    fun `nature multiplier for dropped stat is 0_9`() {
        assertEquals(0.9f, StatFormulas.natureMultiplier(Natures.ADAMANT, 3), 0.001f)
    }

    @Test
    fun `nature multiplier for neutral stat is 1_0`() {
        assertEquals(1.0f, StatFormulas.natureMultiplier(Natures.HARDY, 1), 0.001f)
    }

    @Test
    fun `HP always gets 1_0 nature multiplier`() {
        assertEquals(1.0f, StatFormulas.natureMultiplier(Natures.ADAMANT, 0), 0.001f)
    }
}
