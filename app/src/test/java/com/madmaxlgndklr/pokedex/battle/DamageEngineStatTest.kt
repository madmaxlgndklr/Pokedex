package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.ui.battle.DamageEngine
import com.madmaxlgndklr.pokedex.ui.battle.DamageParams
import com.madmaxlgndklr.pokedex.ui.battle.Nature
import com.madmaxlgndklr.pokedex.ui.battle.Natures
import com.madmaxlgndklr.pokedex.ui.battle.StatConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun gen3PlusParams(
    attackBase: Int = 49,
    defenseBase: Int = 49,
    attackStatIndex: Int = 1,
    defenseStatIndex: Int = 2,
    attackIvs: IntArray = IntArray(6) { 31 },
    attackEvs: IntArray = IntArray(6) { 0 },
    defenseIvs: IntArray = IntArray(6) { 31 },
    defenseEvs: IntArray = IntArray(6) { 0 },
    attackerNature: Nature = Natures.HARDY,
    defenderNature: Nature = Natures.HARDY,
    heldItem: HeldItem? = null,
    moveCategory: String = "physical"
) = DamageParams(
    gen = 5,
    level = 50,
    attackBaseStat = attackBase,
    defenseBaseStat = defenseBase,
    attackStatIndex = attackStatIndex,
    defenseStatIndex = defenseStatIndex,
    attackerStatConfig = StatConfig.Gen3PlusConfig(attackIvs, attackEvs),
    attackerNature = attackerNature,
    defenderStatConfig = StatConfig.Gen3PlusConfig(defenseIvs, defenseEvs),
    defenderNature = defenderNature,
    heldItem = heldItem,
    basePower = 80,
    moveType = "normal",
    moveCategory = moveCategory,
    attackerTypes = listOf("normal"),
    defenderTypes = listOf("normal")
)

class DamageEngineStatTest {

    @Test
    fun `gen3plus Hardy nature produces positive damage`() {
        val result = DamageEngine.calculate(gen3PlusParams())
        assertTrue("average damage > 0", result.average > 0)
    }

    @Test
    fun `adamant nature increases physical damage over hardy`() {
        val hardy   = DamageEngine.calculate(gen3PlusParams(attackerNature = Natures.HARDY))
        val adamant = DamageEngine.calculate(gen3PlusParams(attackerNature = Natures.ADAMANT))
        assertTrue("Adamant should deal more physical damage than Hardy", adamant.average > hardy.average)
    }

    @Test
    fun `adamant nature does not boost special damage`() {
        val hardy   = DamageEngine.calculate(gen3PlusParams(attackStatIndex = 3, defenseStatIndex = 4, moveCategory = "special", attackerNature = Natures.HARDY))
        val adamant = DamageEngine.calculate(gen3PlusParams(attackStatIndex = 3, defenseStatIndex = 4, moveCategory = "special", attackerNature = Natures.ADAMANT))
        assertTrue("Adamant drops SpAtk so special damage should be lower", adamant.average <= hardy.average)
    }

    @Test
    fun `choice band boosts physical damage`() {
        val noItem   = DamageEngine.calculate(gen3PlusParams())
        val withBand = DamageEngine.calculate(gen3PlusParams(heldItem = HeldItem(1, "choice-band", "Choice Band", "")))
        assertTrue("Choice Band should boost physical damage", withBand.average > noItem.average)
    }

    @Test
    fun `life orb multiplies final damage by approximately 1_3`() {
        val noItem  = DamageEngine.calculate(gen3PlusParams())
        val withOrb = DamageEngine.calculate(gen3PlusParams(heldItem = HeldItem(247, "life-orb", "Life Orb", "")))
        val expected = (noItem.average * 1.3f).toInt()
        assertTrue("Life Orb damage should be ~1.3× base", withOrb.average >= expected - 1 && withOrb.average <= expected + 1)
    }

    @Test
    fun `null held item produces same result as explicit null`() {
        val noItem   = DamageEngine.calculate(gen3PlusParams(heldItem = null))
        val explicit = DamageEngine.calculate(gen3PlusParams(heldItem = null))
        assertEquals(noItem.average, explicit.average)
    }

    @Test
    fun `gen1 config produces positive damage`() {
        val params = DamageParams(
            gen = 1,
            level = 50,
            attackBaseStat = 49,
            defenseBaseStat = 49,
            attackStatIndex = 1,
            defenseStatIndex = 2,
            attackerStatConfig = StatConfig.Gen12Config(intArrayOf(15,15,15,15,15), intArrayOf(0,0,0,0,0)),
            attackerNature = Natures.HARDY,
            defenderStatConfig = StatConfig.Gen12Config(intArrayOf(15,15,15,15,15), intArrayOf(0,0,0,0,0)),
            defenderNature = Natures.HARDY,
            heldItem = null,
            basePower = 80,
            moveType = "normal",
            moveCategory = "physical",
            attackerTypes = listOf("normal"),
            defenderTypes = listOf("normal")
        )
        val result = DamageEngine.calculate(params)
        assertTrue(result.average > 0)
    }
}
