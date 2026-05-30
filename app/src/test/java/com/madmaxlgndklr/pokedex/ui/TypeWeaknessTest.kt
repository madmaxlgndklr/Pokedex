package com.madmaxlgndklr.pokedex.ui

import com.madmaxlgndklr.pokedex.ui.common.typeWeaknesses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TypeWeaknessTest {

    @Test
    fun `single grass type has correct 2x weaknesses`() {
        val result = typeWeaknesses(listOf("grass"))
        assertEquals(2f, result["fire"] ?: 1f, 0.01f)
        assertEquals(2f, result["ice"] ?: 1f, 0.01f)
        assertEquals(2f, result["poison"] ?: 1f, 0.01f)
        assertEquals(2f, result["flying"] ?: 1f, 0.01f)
        assertEquals(2f, result["bug"] ?: 1f, 0.01f)
    }

    @Test
    fun `single grass type has correct 0_5x resistances`() {
        val result = typeWeaknesses(listOf("grass"))
        assertEquals(0.5f, result["water"] ?: 1f, 0.01f)
        assertEquals(0.5f, result["electric"] ?: 1f, 0.01f)
        assertEquals(0.5f, result["grass"] ?: 1f, 0.01f)
        assertEquals(0.5f, result["ground"] ?: 1f, 0.01f)
    }

    @Test
    fun `dual grass poison type stacks multipliers`() {
        val result = typeWeaknesses(listOf("grass", "poison"))
        // Ground is 0.5x on grass, 2x on poison = 1x net -> not in results
        assertFalse(result.containsKey("ground"))
        // Psychic is 1x on grass, 2x on poison = 2x
        assertEquals(2f, result["psychic"] ?: 1f, 0.01f)
        // Fire is 2x on grass, 1x on poison = 2x
        assertEquals(2f, result["fire"] ?: 1f, 0.01f)
    }

    @Test
    fun `ghost type is immune to normal`() {
        val result = typeWeaknesses(listOf("ghost"))
        assertEquals(0f, result["normal"] ?: 1f, 0.01f)
    }

    @Test
    fun `water ground dual type cancels electric`() {
        val result = typeWeaknesses(listOf("water", "ground"))
        assertEquals(0f, result["electric"] ?: 1f, 0.01f)
    }

    @Test
    fun `normal effectiveness types are excluded from result`() {
        val result = typeWeaknesses(listOf("fire"))
        assertFalse(result.containsKey("normal"))
        assertFalse(result.containsKey("fighting"))
    }
}
