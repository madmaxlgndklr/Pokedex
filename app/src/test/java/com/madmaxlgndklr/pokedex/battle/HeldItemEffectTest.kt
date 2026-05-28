package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.ui.battle.HeldItemEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeldItemEffectTest {

    @Test
    fun `choice-band returns StatMultiplier on Atk (index 1) factor 1_5`() {
        val effect = HeldItemEffect.from("choice-band")
        assertEquals(HeldItemEffect.StatMultiplier(statIndex = 1, factor = 1.5f), effect)
    }

    @Test
    fun `choice-specs returns StatMultiplier on SpAtk (index 3) factor 1_5`() {
        val effect = HeldItemEffect.from("choice-specs")
        assertEquals(HeldItemEffect.StatMultiplier(statIndex = 3, factor = 1.5f), effect)
    }

    @Test
    fun `life-orb returns DamageMultiplier factor 1_3`() {
        val effect = HeldItemEffect.from("life-orb")
        assertEquals(HeldItemEffect.DamageMultiplier(factor = 1.3f), effect)
    }

    @Test
    fun `expert-belt returns SuperEffectiveBoost factor 1_2`() {
        val effect = HeldItemEffect.from("expert-belt")
        assertEquals(HeldItemEffect.SuperEffectiveBoost(factor = 1.2f), effect)
    }

    @Test
    fun `flame-plate returns TypeMultiplier`() {
        val effect = HeldItemEffect.from("flame-plate")
        assertEquals(HeldItemEffect.TypeMultiplier(factor = 1.2f), effect)
    }

    @Test
    fun `typeFor flame-plate returns fire`() {
        assertEquals("fire", HeldItemEffect.typeFor("flame-plate"))
    }

    @Test
    fun `charcoal returns TypeMultiplier and maps to fire`() {
        assertEquals(HeldItemEffect.TypeMultiplier(1.2f), HeldItemEffect.from("charcoal"))
        assertEquals("fire", HeldItemEffect.typeFor("charcoal"))
    }

    @Test
    fun `pixie-plate maps to fairy`() {
        assertEquals("fairy", HeldItemEffect.typeFor("pixie-plate"))
    }

    @Test
    fun `leftovers returns None`() {
        assertEquals(HeldItemEffect.None, HeldItemEffect.from("leftovers"))
    }

    @Test
    fun `unknown item returns None`() {
        assertEquals(HeldItemEffect.None, HeldItemEffect.from("unknown-item-xyz"))
    }

    @Test
    fun `typeFor unknown item returns null`() {
        assertNull(HeldItemEffect.typeFor("unknown-item-xyz"))
    }
}
