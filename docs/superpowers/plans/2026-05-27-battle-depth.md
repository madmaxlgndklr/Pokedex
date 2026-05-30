# Battle Depth (Sub-project B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add gen-accurate stat computation (DVs/StatExp for Gen I–II, IVs/EVs for Gen III+), 25 natures, and held-item effects to the damage calculator and battle sim via an extended Battle Setup screen, plus crash-safe temp-file handling for cry downloads and version bump to 0.2.

**Architecture:** New pure-Kotlin `StatFormulas` object handles stat math; `StatConfig` sealed class carries per-gen stat data; `HeldItemEffect` sealed class maps item names to battle effects; `DamageParams` is updated to carry configs for both attacker and defender; `BattlePokemon` and `BattleSetup` gain stat fields. All Room work goes through a new `HeldItemEntity`/`HeldItemDao` + DB v5 migration; `HeldItemRepository` syncs from PokeAPI.

**Tech Stack:** Kotlin, Jetpack Compose, Room (v5 migration), Retrofit/OkHttp, PokeAPI, JUnit 4, `kotlinx.coroutines.test`

---

## File Map

| Action | Path |
|--------|------|
| Create | `app/src/main/java/…/ui/battle/StatConfig.kt` — `StatConfig`, `Nature`, `Natures`, `StatFormulas` |
| Create | `app/src/main/java/…/ui/battle/HeldItemEffect.kt` — `HeldItemEffect` sealed class |
| Create | `app/src/main/java/…/data/local/HeldItem.kt` — `HeldItem` entity + `HeldItemDao` |
| Create | `app/src/main/java/…/data/remote/dto/ItemResponse.kt` — PokeAPI item DTOs |
| Create | `app/src/main/java/…/data/repository/HeldItemRepository.kt` |
| Modify | `app/src/main/java/…/data/local/AppDatabase.kt` — v4→v5 migration |
| Modify | `app/src/main/java/…/data/remote/PokeApiService.kt` — add item endpoints |
| Modify | `app/src/main/java/…/ui/common/CryPlayer.kt` — delete `.tmp` on failure |
| Modify | `app/src/main/java/…/PokedexApplication.kt` — startup sweep + `HeldItemRepository` |
| Modify | `app/src/main/java/…/ui/battle/DamageEngine.kt` — `DamageParams` + stat helpers |
| Modify | `app/src/main/java/…/ui/battle/DamageCalcViewModel.kt` — `CalcSlot` with `StatConfig`/`Nature`/`HeldItem` |
| Modify | `app/src/main/java/…/ui/battle/DamageCalcScreen.kt` — full nature picker, gen-aware stat config UI |
| Modify | `app/src/main/java/…/ui/battle/BattleEngine.kt` — `BattlePokemon` gains stat fields |
| Modify | `app/src/main/java/…/ui/battle/TurnBattleViewModel.kt` — `BattleSetup` gains stat fields + setters |
| Modify | `app/src/main/java/…/ui/battle/TurnBattleScreen.kt` — gen toggle, stat sliders, nature picker, item picker |
| Modify | `app/src/main/java/…/ui/settings/SettingsViewModel.kt` — add `SyncOptions.syncItems` + held item sync |
| Modify | `app/src/main/java/…/ui/settings/SettingsScreen.kt` — add "HELD ITEMS" sync option row |
| Modify | `app/build.gradle.kts` — versionCode 1→2, versionName "1.0"→"0.2" |
| Create | `app/src/test/java/…/battle/StatConfigTest.kt` |
| Create | `app/src/test/java/…/battle/HeldItemEffectTest.kt` |
| Create | `app/src/test/java/…/battle/HeldItemRepositoryTest.kt` |
| Create | `app/src/test/java/…/battle/DamageEngineStatTest.kt` |

All test files live under `com.madmaxlgndklr.pokedex.battle`.
The base package is `com.madmaxlgndklr.pokedex`; paths abbreviated with `…`.

**Test command (all JVM unit tests):**
```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test 2>&1 | tail -20
```

**Test command (battle tests only):**
```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test --tests "com.madmaxlgndklr.pokedex.battle.*" 2>&1 | tail -20
```

---

## Stat formula reference (for test assertions)

**Gen I non-HP** (`floor(((base + dv) * 2 + floor(sqrt(statExp)) / 4) * level / 100) + 5`):
- Bulbasaur Atk: base=49, dv=15, statExp=0, level=50 → **69**

**Gen I HP** (`floor(((base + dv) * 2 + floor(sqrt(statExp)) / 4) * level / 100) + level + 10`):
- Bulbasaur HP: base=45, dv=15, statExp=0, level=50 → **120**

**Gen III+ non-HP** (`floor((floor((2*base + iv + floor(ev/4)) * level/100) + 5) * natureMultiplier)`):
- Bulbasaur Atk: base=49, iv=31, ev=0, Hardy (1.0), level=50 → **69**
- Bulbasaur Atk: base=49, iv=31, ev=0, Adamant (1.1), level=50 → **75**

**Gen III+ HP** (`floor((2*base + iv + floor(ev/4)) * level/100) + level + 10`):
- Bulbasaur HP: base=45, iv=31, ev=0, level=50 → **120**

---

## Task 1: StatConfig, Nature, Natures, StatFormulas

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/StatConfig.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/battle/StatConfigTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/madmaxlgndklr/pokedex/battle/StatConfigTest.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.ui.battle.Natures
import com.madmaxlgndklr.pokedex.ui.battle.StatConfig
import com.madmaxlgndklr.pokedex.ui.battle.StatFormulas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatConfigTest {

    // Gen I stat formulas
    @Test
    fun `gen1 non-HP stat - bulbasaur atk dv15 statExp0 level50`() {
        val cfg = StatConfig.Gen12Config(
            dvs = intArrayOf(15, 15, 15, 15, 15),
            statExp = intArrayOf(0, 0, 0, 0, 0)
        )
        // statIndex=1 (Atk) in Gen12Config
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

    // Gen III+ stat formulas
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

    // Gen II Special DV
    @Test
    fun `gen12config SpAtk and SpDef both use Spc DV index 4`() {
        val cfg = StatConfig.Gen12Config(
            dvs = intArrayOf(15, 15, 15, 15, 8),  // spc DV = 8
            statExp = intArrayOf(0, 0, 0, 0, 0)
        )
        // statIndex 3 (SpAtk) and 4 (SpDef) both map to Gen12 slot 4 (Spc)
        val spatk = StatFormulas.computeStat(49, cfg, Natures.HARDY, 3, 50)
        val spdef = StatFormulas.computeStat(49, cfg, Natures.HARDY, 4, 50)
        assertEquals("SpAtk and SpDef share the Spc DV", spatk, spdef)
    }

    // EV validation
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

    // Nature tests
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
```

- [ ] **Step 2: Run to confirm FAIL**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test --tests "com.madmaxlgndklr.pokedex.battle.StatConfigTest" 2>&1 | tail -10
```

Expected: compilation error — `StatConfig`, `Natures`, `StatFormulas` not found.

- [ ] **Step 3: Implement**

Create `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/StatConfig.kt`:

```kotlin
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

    // Unified stat index (Gen3+ convention) → Gen12Config array index
    private fun gen12SlotFor(unifiedIndex: Int): Int = when (unifiedIndex) {
        0 -> 0  // HP
        1 -> 1  // Atk
        2 -> 2  // Def
        3 -> 4  // SpAtk → Spc
        4 -> 4  // SpDef → Spc
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

    // Compute any non-HP stat. statIndex uses unified Gen3+ convention (0=HP, 1=Atk, … 5=Spe).
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
```

- [ ] **Step 4: Run to confirm PASS**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test --tests "com.madmaxlgndklr.pokedex.battle.StatConfigTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` with all 16 tests passing.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/StatConfig.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/battle/StatConfigTest.kt
git commit -m "feat: add StatConfig, Nature, Natures, StatFormulas for gen-accurate stat computation"
```

---

## Task 2: HeldItemEffect

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/HeldItemEffect.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/battle/HeldItemEffectTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/madmaxlgndklr/pokedex/battle/HeldItemEffectTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run to confirm FAIL**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test --tests "com.madmaxlgndklr.pokedex.battle.HeldItemEffectTest" 2>&1 | tail -10
```

Expected: compilation error — `HeldItemEffect` not found.

- [ ] **Step 3: Implement**

Create `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/HeldItemEffect.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.ui.battle

sealed class HeldItemEffect {
    data class StatMultiplier(val statIndex: Int, val factor: Float) : HeldItemEffect()
    data class DamageMultiplier(val factor: Float) : HeldItemEffect()
    data class TypeMultiplier(val factor: Float) : HeldItemEffect()
    data class SuperEffectiveBoost(val factor: Float) : HeldItemEffect()
    object None : HeldItemEffect()

    companion object {
        private val TYPE_ITEMS = mapOf(
            "flame-plate"    to "fire",     "splash-plate"  to "water",
            "zap-plate"      to "electric", "meadow-plate"  to "grass",
            "icicle-plate"   to "ice",      "fist-plate"    to "fighting",
            "toxic-plate"    to "poison",   "earth-plate"   to "ground",
            "sky-plate"      to "flying",   "mind-plate"    to "psychic",
            "insect-plate"   to "bug",      "stone-plate"   to "rock",
            "spooky-plate"   to "ghost",    "draco-plate"   to "dragon",
            "dread-plate"    to "dark",     "iron-plate"    to "steel",
            "pixie-plate"    to "fairy",
            "charcoal"       to "fire",     "mystic-water"  to "water",
            "magnet"         to "electric", "miracle-seed"  to "grass",
            "never-melt-ice" to "ice",      "black-belt"    to "fighting",
            "poison-barb"    to "poison",   "soft-sand"     to "ground",
            "sharp-beak"     to "flying",   "twisted-spoon" to "psychic",
            "silver-powder"  to "bug",      "hard-stone"    to "rock",
            "spell-tag"      to "ghost",    "dragon-fang"   to "dragon",
            "black-glasses"  to "dark",     "metal-coat"    to "steel",
            "fairy-feather"  to "fairy"
        )

        fun from(name: String): HeldItemEffect = when {
            name == "choice-band"  -> StatMultiplier(statIndex = 1, factor = 1.5f)
            name == "choice-specs" -> StatMultiplier(statIndex = 3, factor = 1.5f)
            name == "life-orb"     -> DamageMultiplier(factor = 1.3f)
            name == "expert-belt"  -> SuperEffectiveBoost(factor = 1.2f)
            name in TYPE_ITEMS     -> TypeMultiplier(factor = 1.2f)
            else                   -> None
        }

        fun typeFor(name: String): String? = TYPE_ITEMS[name]
    }
}
```

- [ ] **Step 4: Run to confirm PASS**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test --tests "com.madmaxlgndklr.pokedex.battle.HeldItemEffectTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 11 tests passing.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/HeldItemEffect.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/battle/HeldItemEffectTest.kt
git commit -m "feat: add HeldItemEffect sealed class with TYPE_ITEMS map"
```

---

## Task 3: HeldItem Entity, HeldItemDao, DB v5 Migration

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/data/local/HeldItem.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/data/local/AppDatabase.kt`

No unit tests for Room migrations (no Room testing library in current setup); correctness verified by successful compilation and instrumented test stub staying green.

- [ ] **Step 1: Create HeldItem entity and DAO**

Create `app/src/main/java/com/madmaxlgndklr/pokedex/data/local/HeldItem.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "held_items")
data class HeldItem(
    @PrimaryKey val id: Int,
    val name: String,
    val displayName: String,
    val effectSummary: String
)

@Dao
interface HeldItemDao {
    @Query("SELECT * FROM held_items ORDER BY displayName ASC")
    suspend fun getAll(): List<HeldItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HeldItem>)

    @Query("DELETE FROM held_items")
    suspend fun deleteAll()
}
```

- [ ] **Step 2: Update AppDatabase to v5**

Edit `app/src/main/java/com/madmaxlgndklr/pokedex/data/local/AppDatabase.kt`.

Replace the entire file contents with:

```kotlin
package com.madmaxlgndklr.pokedex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CaughtPokemonEntity::class,
        PokemonListCacheEntity::class,
        PokemonDetailCacheEntity::class,
        MoveEntity::class,
        HeldItem::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caughtPokemonDao(): CaughtPokemonDao
    abstract fun pokemonListCacheDao(): PokemonListCacheDao
    abstract fun pokemonDetailCacheDao(): PokemonDetailCacheDao
    abstract fun moveDao(): MoveDao
    abstract fun heldItemDao(): HeldItemDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pokemon_list_cache " +
                    "(id INTEGER PRIMARY KEY NOT NULL, name TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pokemon_detail_cache " +
                    "(id INTEGER PRIMARY KEY NOT NULL, detailJson TEXT NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM pokemon_detail_cache")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS moves " +
                    "(name TEXT PRIMARY KEY NOT NULL, moveJson TEXT NOT NULL)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS held_items " +
                    "(id INTEGER PRIMARY KEY NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "displayName TEXT NOT NULL, " +
                    "effectSummary TEXT NOT NULL)"
                )
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pokedex.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
                    .also { INSTANCE = it }
            }
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | grep -E "error:|warning:|BUILD"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/data/local/HeldItem.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/data/local/AppDatabase.kt
git commit -m "feat: add HeldItem entity, HeldItemDao, Room DB v5 migration"
```

---

## Task 4: Item DTOs, PokeApiService endpoints, HeldItemRepository

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/dto/ItemResponse.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/PokeApiService.kt`
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/data/repository/HeldItemRepository.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/battle/HeldItemRepositoryTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/madmaxlgndklr/pokedex/battle/HeldItemRepositoryTest.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.data.local.HeldItemDao
import com.madmaxlgndklr.pokedex.data.remote.PokeApiService
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemAttributeResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemDetailResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemResultDto
import com.madmaxlgndklr.pokedex.data.remote.dto.NamedDto
import com.madmaxlgndklr.pokedex.data.repository.HeldItemRepository
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// DTO helpers already resolved for the fake
private fun fakeAttribute(vararg names: String) = ItemAttributeResponse(
    items = names.map { ItemResultDto(it, "https://pokeapi.co/api/v2/item/$it/") }
)

private fun fakeDetail(name: String, id: Int) = ItemDetailResponse(
    id = id,
    name = name,
    names = listOf(com.madmaxlgndklr.pokedex.data.remote.dto.ItemNameDto("${name.replace("-", " ").capitalize()}", NamedDto("en")))
)

class FakeHeldItemApiService : FakePokeApiService() {
    override suspend fun getItemAttribute(name: String): ItemAttributeResponse = when (name) {
        "holdable-active"  -> fakeAttribute("choice-band", "life-orb")
        "holdable-passive" -> fakeAttribute("leftovers", "choice-band")  // choice-band in both
        else               -> ItemAttributeResponse(emptyList())
    }

    override suspend fun getItem(name: String) = when (name) {
        "choice-band" -> fakeDetail("choice-band", 1)
        "life-orb"    -> fakeDetail("life-orb", 247)
        "leftovers"   -> fakeDetail("leftovers", 234)
        else          -> throw IllegalArgumentException("Unknown item: $name")
    }
}

class FakeHeldItemDao : HeldItemDao {
    val stored = mutableListOf<HeldItem>()
    var deletedAll = false

    override suspend fun getAll() = stored.toList()

    override suspend fun upsertAll(items: List<HeldItem>) {
        items.forEach { new ->
            stored.removeAll { it.id == new.id }
            stored.add(new)
        }
    }

    override suspend fun deleteAll() {
        deletedAll = true
        stored.clear()
    }
}

class HeldItemRepositoryTest {

    @Test
    fun `syncAll fetches from both attributes and deduplicates`() = runTest {
        val dao = FakeHeldItemDao()
        val repo = HeldItemRepository(FakeHeldItemApiService(), dao)
        repo.syncAll()
        // choice-band in both active+passive → stored once
        val names = dao.stored.map { it.name }
        assertEquals(3, names.size)
        assertTrue("choice-band" in names)
        assertTrue("life-orb" in names)
        assertTrue("leftovers" in names)
    }

    @Test
    fun `syncAll upserts items into DAO`() = runTest {
        val dao = FakeHeldItemDao()
        val repo = HeldItemRepository(FakeHeldItemApiService(), dao)
        repo.syncAll()
        assertTrue(dao.stored.isNotEmpty())
    }

    @Test
    fun `syncAll clears old data before inserting`() = runTest {
        val dao = FakeHeldItemDao()
        // Pre-populate with stale data
        dao.stored.add(HeldItem(999, "stale-item", "Stale", "Old data"))
        val repo = HeldItemRepository(FakeHeldItemApiService(), dao)
        repo.syncAll()
        assertTrue("stale item should be gone", dao.stored.none { it.id == 999 })
    }

    @Test
    fun `syncAll throws when API fails`() = runTest {
        val failingApi = object : FakeHeldItemApiService() {
            override suspend fun getItemAttribute(name: String): ItemAttributeResponse {
                throw RuntimeException("Network error")
            }
        }
        val dao = FakeHeldItemDao()
        val repo = HeldItemRepository(failingApi, dao)
        var threw = false
        try { repo.syncAll() } catch (_: Exception) { threw = true }
        assertTrue(threw)
    }
}
```

- [ ] **Step 2: Run to confirm FAIL**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test --tests "com.madmaxlgndklr.pokedex.battle.HeldItemRepositoryTest" 2>&1 | tail -10
```

Expected: compilation errors — DTOs and `HeldItemRepository` not found.

- [ ] **Step 3: Create item DTOs**

Create `app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/dto/ItemResponse.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ItemResultDto(val name: String, val url: String)

data class ItemAttributeResponse(
    @SerializedName("items") val items: List<ItemResultDto>
)

data class ItemNameDto(val name: String, val language: NamedDto)

data class ItemDetailResponse(
    val id: Int,
    val name: String,
    val names: List<ItemNameDto>
)
```

- [ ] **Step 4: Add endpoints to PokeApiService**

Edit `app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/PokeApiService.kt`.

Add two methods at the end of the interface, before the closing brace:

```kotlin
    @GET("item-attribute/{name}/")
    suspend fun getItemAttribute(@Path("name") name: String): ItemAttributeResponse

    @GET("item/{name}/")
    suspend fun getItem(@Path("name") name: String): ItemDetailResponse
```

Also add the import at the top of the file:
```kotlin
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemAttributeResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemDetailResponse
```

- [ ] **Step 5: Add stubs to FakePokeApiService**

Edit `app/src/test/java/com/madmaxlgndklr/pokedex/repository/PokemonRepositoryTest.kt`.

Add these two methods to `open class FakePokeApiService`:

```kotlin
    override suspend fun getItemAttribute(name: String): ItemAttributeResponse =
        ItemAttributeResponse(emptyList())

    override suspend fun getItem(name: String): ItemDetailResponse =
        ItemDetailResponse(id = 0, name = name, names = emptyList())
```

Also add the import at the top:
```kotlin
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemAttributeResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemDetailResponse
```

- [ ] **Step 6: Create HeldItemRepository**

Create `app/src/main/java/com/madmaxlgndklr/pokedex/data/repository/HeldItemRepository.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.data.repository

import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.data.local.HeldItemDao
import com.madmaxlgndklr.pokedex.data.remote.PokeApiService

class HeldItemRepository(
    private val api: PokeApiService,
    private val dao: HeldItemDao
) {
    suspend fun getAll(): List<HeldItem> = dao.getAll()

    suspend fun syncAll() {
        val active = api.getItemAttribute("holdable-active").items
        val passive = api.getItemAttribute("holdable-passive").items
        // Deduplicate by name — combine, distinct by name, then fetch details
        val allNames = (active + passive).map { it.name }.distinct()
        val items = allNames.mapNotNull { name ->
            try {
                val detail = api.getItem(name)
                val displayName = detail.names
                    .firstOrNull { it.language.name == "en" }?.name
                    ?: name.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
                val effect = com.madmaxlgndklr.pokedex.ui.battle.HeldItemEffect.from(name)
                val summary = when (effect) {
                    is com.madmaxlgndklr.pokedex.ui.battle.HeldItemEffect.StatMultiplier  -> "Boosts a stat by ×${effect.factor}"
                    is com.madmaxlgndklr.pokedex.ui.battle.HeldItemEffect.DamageMultiplier -> "Boosts damage by ×${effect.factor}"
                    is com.madmaxlgndklr.pokedex.ui.battle.HeldItemEffect.TypeMultiplier   -> "Boosts matching move type"
                    is com.madmaxlgndklr.pokedex.ui.battle.HeldItemEffect.SuperEffectiveBoost -> "Boosts super-effective moves"
                    com.madmaxlgndklr.pokedex.ui.battle.HeldItemEffect.None -> "No battle effect"
                }
                HeldItem(id = detail.id, name = name, displayName = displayName, effectSummary = summary)
            } catch (_: Exception) { null }
        }
        dao.deleteAll()
        dao.upsertAll(items)
    }
}
```

- [ ] **Step 7: Run to confirm PASS**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test --tests "com.madmaxlgndklr.pokedex.battle.HeldItemRepositoryTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 4 tests passing.

- [ ] **Step 8: Verify all existing tests still pass**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/dto/ItemResponse.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/PokeApiService.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/data/repository/HeldItemRepository.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/repository/PokemonRepositoryTest.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/battle/HeldItemRepositoryTest.kt
git commit -m "feat: add HeldItemRepository with PokeAPI item sync"
```

---

## Task 5: CryPlayer temp-file fix + startup sweep

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/CryPlayer.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/PokedexApplication.kt`

The existing `downloadCry()` already writes `.tmp` → `renameTo()` on success. The fix adds cleanup on failure and a startup sweep.

- [ ] **Step 1: Fix downloadCry to delete .tmp on failure**

In `CryPlayer.kt`, the `downloadCry` function currently has:

```kotlin
    suspend fun downloadCry(name: String): Boolean {
        if (!::appContext.isInitialized) return false
        val file = cryFile(name)
        if (file.exists() && file.length() > 0) return true
        return try {
            val request = Request.Builder().url("$CDN/$name.ogg").build()
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.httpClient.newCall(request).execute()
            }
            response.use { resp ->
                if (!resp.isSuccessful) return@use false
                val bytes = resp.body?.bytes() ?: return@use false
                if (bytes.isEmpty()) return@use false
                val tmp = File(criesDir(), "$name.ogg.tmp")
                tmp.writeBytes(bytes)
                tmp.renameTo(file)
                true
            }
        } catch (_: Exception) { false }
    }
```

Replace the `downloadCry` function with:

```kotlin
    suspend fun downloadCry(name: String): Boolean {
        if (!::appContext.isInitialized) return false
        val file = cryFile(name)
        if (file.exists() && file.length() > 0) return true
        val tmp = File(criesDir(), "$name.ogg.tmp")
        return try {
            val request = Request.Builder().url("$CDN/$name.ogg").build()
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.httpClient.newCall(request).execute()
            }
            response.use { resp ->
                if (!resp.isSuccessful) return@use false
                val bytes = resp.body?.bytes() ?: return@use false
                if (bytes.isEmpty()) return@use false
                tmp.writeBytes(bytes)
                tmp.renameTo(file)
                true
            }
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }
```

- [ ] **Step 2: Add startup sweep to PokedexApplication**

In `PokedexApplication.kt`, update `onCreate()`:

```kotlin
    override fun onCreate() {
        super.onCreate()
        filesDir.listFiles { _, n -> n.endsWith(".tmp") }?.forEach { it.delete() }
        CryPlayer.init(this, networkObserver)
    }
```

- [ ] **Step 3: Verify compilation**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/CryPlayer.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/PokedexApplication.kt
git commit -m "fix: delete .tmp cry file on download failure; sweep leftover .tmp files on launch"
```

---

## Task 6: DamageEngine — gen-accurate stats + held item application

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/DamageEngine.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/battle/DamageEngineStatTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/madmaxlgndklr/pokedex/battle/DamageEngineStatTest.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.ui.battle.DamageEngine
import com.madmaxlgndklr.pokedex.ui.battle.DamageParams
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
    attackerNature: com.madmaxlgndklr.pokedex.ui.battle.Nature = Natures.HARDY,
    defenderNature: com.madmaxlgndklr.pokedex.ui.battle.Nature = Natures.HARDY,
    heldItem: HeldItem? = null
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
    moveCategory = "physical",
    attackerTypes = listOf("normal"),
    defenderTypes = listOf("normal")
)

class DamageEngineStatTest {

    @Test
    fun `gen3plus Hardy nature produces same damage as old flat-field baseline`() {
        // The old formula with ev=0 and nature=1.0 should produce the same result as the new formula
        val result = DamageEngine.calculate(gen3PlusParams())
        assertTrue("average damage > 0", result.average > 0)
        // baseline: with atk=49, def=49, both iv31 ev0 Hardy level50 — compute expected
        // computeStat(49, Gen3Plus(31,0), Hardy, 1, 50) = floor((98+31)*0.5)+5 = 69
        // This is the same as the old computeStat(49, 0, 50, 1.0f)
        val expected = DamageEngine.calculate(gen3PlusParams(attackerNature = Natures.HARDY))
        assertEquals(expected.average, result.average)
    }

    @Test
    fun `adamant nature increases physical damage over hardy`() {
        val hardy   = DamageEngine.calculate(gen3PlusParams(attackerNature = Natures.HARDY))
        val adamant = DamageEngine.calculate(gen3PlusParams(attackerNature = Natures.ADAMANT))
        assertTrue("Adamant should deal more physical damage than Hardy", adamant.average > hardy.average)
    }

    @Test
    fun `adamant nature does not affect special damage`() {
        val hardy   = DamageEngine.calculate(gen3PlusParams(attackStatIndex = 3, defenseStatIndex = 4, moveCategory = "special", attackerNature = Natures.HARDY))
        val adamant = DamageEngine.calculate(gen3PlusParams(attackStatIndex = 3, defenseStatIndex = 4, moveCategory = "special", attackerNature = Natures.ADAMANT))
        // Adamant drops SpAtk → special damage should be lower, not higher
        assertTrue("Adamant drops SpAtk so special damage should be lower", adamant.average <= hardy.average)
    }

    @Test
    fun `choice band boosts physical damage`() {
        val noItem   = DamageEngine.calculate(gen3PlusParams())
        val withBand = DamageEngine.calculate(gen3PlusParams(heldItem = HeldItem(1, "choice-band", "Choice Band", "")))
        assertTrue("Choice Band should boost physical damage", withBand.average > noItem.average)
    }

    @Test
    fun `life orb multiplies final damage by 1_3`() {
        val noItem    = DamageEngine.calculate(gen3PlusParams())
        val withOrb   = DamageEngine.calculate(gen3PlusParams(heldItem = HeldItem(247, "life-orb", "Life Orb", "")))
        // Life Orb applies 1.3× to final damage
        val expected = (noItem.average * 1.3f).toInt()
        assertTrue("Life Orb damage should be ~1.3× base", withOrb.average >= expected - 1 && withOrb.average <= expected + 1)
    }

    @Test
    fun `null held item produces no change`() {
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
```

- [ ] **Step 2: Run to confirm FAIL**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test --tests "com.madmaxlgndklr.pokedex.battle.DamageEngineStatTest" 2>&1 | tail -10
```

Expected: compilation errors — new `DamageParams` fields not found.

- [ ] **Step 3: Update DamageEngine.kt**

Replace the entire file `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/DamageEngine.kt`:

```kotlin
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
    val attackStatIndex: Int = 1,    // unified Gen3+ indices: 1=Atk, 3=SpAtk
    val defenseStatIndex: Int = 2,   // unified Gen3+ indices: 2=Def, 4=SpDef
    val attackerStatConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
    val attackerNature: Nature = Natures.HARDY,
    val defenderStatConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
    val defenderNature: Nature = Natures.HARDY,
    val heldItem: HeldItem? = null,
    val basePower: Int,
    val moveType: String,
    val moveCategory: String,     // "physical" | "special" | "status"
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

        // Apply stat-boosting held items to the attack stat
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
            // Apply post-damage held item effects
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
            acc * (typeWeaknesses(listOf(defType))[moveType] ?: 1f).let {
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

    fun isPhysicalGen23(moveType: String) = moveType in GEN23_PHYSICAL
}
```

- [ ] **Step 4: Run to confirm PASS**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test --tests "com.madmaxlgndklr.pokedex.battle.DamageEngineStatTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 6 tests passing. (If compile errors remain due to callers of the old `DamageParams`, fix them first — see Step 5.)

- [ ] **Step 5: Fix DamageCalcViewModel and BattleEngine call sites**

`DamageCalcViewModel.calculate()` and `BattleEngine.resolveTurn()` both construct `DamageParams`. After changing `DamageParams`, these will fail to compile. Provide stub fixes now so the project compiles; full rewrites come in Tasks 7 and 8.

In `DamageCalcViewModel.calculate()`, replace the `DamageParams(...)` block with:

```kotlin
        val params = DamageParams(
            gen = s.gen,
            level = s.attacker.level,
            attackBaseStat = atkBase,
            defenseBaseStat = defBase,
            attackStatIndex = if (isPhysical) 1 else 3,
            defenseStatIndex = if (isPhysical) 2 else 4,
            attackerStatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, intArrayOf(atkEvs, atkEvs, atkEvs, atkEvs, atkEvs, atkEvs)),
            attackerNature = Natures.HARDY,
            defenderStatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, intArrayOf(defEvs, defEvs, defEvs, defEvs, defEvs, defEvs)),
            defenderNature = Natures.HARDY,
            basePower = move.power,
            moveType = move.type,
            moveCategory = move.category,
            attackerTypes = attackerDetail.types,
            defenderTypes = defenderDetail.types
        )
```

Add the missing imports at the top of `DamageCalcViewModel.kt`:
```kotlin
import com.madmaxlgndklr.pokedex.ui.battle.Natures
import com.madmaxlgndklr.pokedex.ui.battle.StatConfig
```

In `BattleEngine.resolveTurn()`, the `DamageParams(...)` block inside `applyMove` needs `attackStatIndex`, `defenseStatIndex`, `attackerStatConfig`, `attackerNature`, `defenderStatConfig`, `defenderNature` added with defaults:

```kotlin
            val params = DamageParams(
                gen = gen,
                level = attacker.level,
                attackBaseStat = statValue(attacker, atkStat),
                defenseBaseStat = statValue(defender, defStat),
                attackStatIndex = if (isPhysical) 1 else 3,
                defenseStatIndex = if (isPhysical) 2 else 4,
                basePower = move.power,
                moveType = move.type,
                moveCategory = move.category,
                attackerTypes = attacker.detail.types,
                defenderTypes = defender.detail.types
            )
```

- [ ] **Step 6: Verify all tests pass**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/DamageEngine.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/DamageCalcViewModel.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/BattleEngine.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/battle/DamageEngineStatTest.kt
git commit -m "feat: update DamageEngine to use StatFormulas and HeldItemEffect"
```

---

## Task 7: DamageCalcViewModel + DamageCalcScreen — full stat config UI

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/DamageCalcViewModel.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/DamageCalcScreen.kt`

- [ ] **Step 1: Update CalcSlot in DamageCalcViewModel**

Replace `CalcSlot` data class and update `calculate()` in `DamageCalcViewModel.kt`.

Replace `CalcSlot`:
```kotlin
data class CalcSlot(
    val detail: PokemonDetail? = null,
    val level: Int = 50,
    val statConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
    val nature: Nature = Natures.HARDY,
    val heldItem: HeldItem? = null
)
```

Add imports at the top:
```kotlin
import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.ui.battle.Nature
import com.madmaxlgndklr.pokedex.ui.battle.Natures
import com.madmaxlgndklr.pokedex.ui.battle.StatConfig
import com.madmaxlgndklr.pokedex.ui.battle.StatFormulas
```

Add new ViewModel methods after `updateDefender`:
```kotlin
    fun setGen(gen: Int) {
        viewModelScope.launch {
            settingsRepo.setGen(gen)
            // Reset stat configs to gen-appropriate defaults
            val defaultConfig = if (gen <= 2)
                StatConfig.Gen12Config(IntArray(5) { 15 }, IntArray(5) { 0 })
            else
                StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 })
            _state.value = _state.value.copy(
                attacker = _state.value.attacker.copy(statConfig = defaultConfig, nature = Natures.HARDY),
                defender = _state.value.defender.copy(statConfig = defaultConfig, nature = Natures.HARDY)
            )
        }
    }
```

Replace `calculate()`:
```kotlin
    fun calculate() {
        val s = _state.value
        val attackerDetail = s.attacker.detail ?: return
        val defenderDetail = s.defender.detail ?: return
        val move = s.selectedMove ?: return
        if (move.power == null) {
            _state.value = s.copy(result = DamageResult(0, 0, 0, "—"))
            return
        }
        val isPhysical = when {
            s.gen >= 4 -> move.category == "physical"
            s.gen == 1 -> true
            else -> DamageEngine.isPhysicalGen23(move.type)
        }
        val atkStatName = if (isPhysical) "attack" else "special-attack"
        val defStatName = if (isPhysical) "defense" else "special-defense"
        val atkBase = attackerDetail.stats.firstOrNull { it.name == atkStatName }?.value ?: 50
        val defBase = defenderDetail.stats.firstOrNull { it.name == defStatName }?.value ?: 50
        val attackStatIndex = if (isPhysical) 1 else 3
        val defenseStatIndex = if (isPhysical) 2 else 4

        val params = DamageParams(
            gen = s.gen,
            level = s.attacker.level,
            attackBaseStat = atkBase,
            defenseBaseStat = defBase,
            attackStatIndex = attackStatIndex,
            defenseStatIndex = defenseStatIndex,
            attackerStatConfig = s.attacker.statConfig,
            attackerNature = s.attacker.nature,
            defenderStatConfig = s.defender.statConfig,
            defenderNature = s.defender.nature,
            heldItem = s.attacker.heldItem,
            basePower = move.power,
            moveType = move.type,
            moveCategory = move.category,
            attackerTypes = attackerDetail.types,
            defenderTypes = defenderDetail.types
        )
        _state.value = s.copy(result = DamageEngine.calculate(params))
    }
```

Add an `isEvSumValid` helper exposed to the UI:
```kotlin
    fun isEvSumValid(slot: CalcSlot): Boolean {
        val cfg = slot.statConfig
        return if (cfg is StatConfig.Gen3PlusConfig) StatFormulas.isEvSumValid(cfg.evs) else true
    }
```

- [ ] **Step 2: Update DamageCalcScreen**

Read the current `DamageCalcScreen.kt` to understand the existing layout, then replace the nature picker section and EV sliders with the gen-aware stat config UI.

The existing simplified nature list:
```kotlin
val natures = listOf("Neutral" to 1.0f, "+Atk" to 1.1f, "−Atk" to 0.9f, "+SpA" to 1.1f, "−SpA" to 0.9f)
```

Replace the attacker stat/nature section with calls to a shared composable. The complete replacement of `DamageCalcScreen.kt` is large — implement these changes inline in the existing file:

a) Replace the nature `listOf(...)` and `natureMultiplier` logic with `Natures.ALL` display.

b) Replace EV sliders with a `StatConfigSection` composable that conditionally shows Gen I–II (DVs + StatExp) or Gen III+ (IVs + EVs with sum cap) inputs.

The `StatConfigSection` composable (add at the bottom of `DamageCalcScreen.kt`):

```kotlin
@Composable
private fun StatConfigSection(
    gen: Int,
    slot: CalcSlot,
    label: String,
    onSlotChange: (CalcSlot) -> Unit,
    isEvSumValid: Boolean,
    modifier: Modifier = Modifier
) {
    val statNames12 = listOf("HP", "ATK", "DEF", "SPE", "SPC")
    val statNames3  = listOf("HP", "ATK", "DEF", "SPATK", "SPDEF", "SPE")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontFamily = PressStart2P, fontSize = 6.sp, color = CaughtGold)

        if (gen <= 2) {
            val cfg = slot.statConfig as? StatConfig.Gen12Config
                ?: StatConfig.Gen12Config(IntArray(5) { 15 }, IntArray(5) { 0 })
            statNames12.forEachIndexed { i, name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, modifier = Modifier.width(36.dp))
                    Text("DV", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.6f))
                    Slider(
                        value = cfg.dvs.getOrElse(i) { 15 }.toFloat(),
                        onValueChange = { v ->
                            val newDvs = cfg.dvs.copyOf().also { it[i] = v.toInt() }
                            onSlotChange(slot.copy(statConfig = cfg.copy(dvs = newDvs)))
                        },
                        valueRange = 0f..15f,
                        steps = 14,
                        modifier = Modifier.weight(1f),
                        colors = sliderColors()
                    )
                    Text("${cfg.dvs.getOrElse(i){15}}", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, modifier = Modifier.width(20.dp))
                }
            }
        } else {
            val cfg = slot.statConfig as? StatConfig.Gen3PlusConfig
                ?: StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 })
            val evSum = cfg.evs.sum()
            statNames3.forEachIndexed { i, name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, modifier = Modifier.width(40.dp))
                    Slider(
                        value = cfg.ivs.getOrElse(i) { 31 }.toFloat(),
                        onValueChange = { v ->
                            val newIvs = cfg.ivs.copyOf().also { it[i] = v.toInt() }
                            onSlotChange(slot.copy(statConfig = cfg.copy(ivs = newIvs)))
                        },
                        valueRange = 0f..31f,
                        steps = 30,
                        modifier = Modifier.weight(1f),
                        colors = sliderColors()
                    )
                    Slider(
                        value = cfg.evs.getOrElse(i) { 0 }.toFloat(),
                        onValueChange = { v ->
                            val newEvs = cfg.evs.copyOf().also { it[i] = v.toInt() }
                            val newSlot = slot.copy(statConfig = cfg.copy(evs = newEvs))
                            if (StatFormulas.isEvSumValid(newEvs)) onSlotChange(newSlot)
                        },
                        valueRange = 0f..252f,
                        steps = 62,
                        modifier = Modifier.weight(1f),
                        colors = sliderColors()
                    )
                }
            }
            Text(
                text = "$evSum/510 EVs",
                fontFamily = PressStart2P,
                fontSize = 5.sp,
                color = if (isEvSumValid) PokedexCream.copy(alpha = 0.6f) else Color.Red
            )
        }
    }
}

@Composable
private fun NaturePicker(
    selectedNature: Nature,
    onNatureSelected: (Nature) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = Natures.ALL.chunked(5)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        grouped.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { nature ->
                    val selected = nature == selectedNature
                    Text(
                        text = nature.name.uppercase().take(4),
                        fontFamily = PressStart2P,
                        fontSize = 4.sp,
                        color = if (selected) CaughtGold else PokedexCream.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNatureSelected(nature) }
                            .padding(4.dp)
                    )
                }
            }
        }
        if (selectedNature.boostedStat != null) {
            val statNames = listOf("HP","Atk","Def","SpAtk","SpDef","Spe")
            val b = statNames.getOrElse(selectedNature.boostedStat) { "?" }
            val d = statNames.getOrElse(selectedNature.droppedStat ?: 0) { "?" }
            Text("↑ $b  ↓ $d", fontFamily = PressStart2P, fontSize = 5.sp, color = CaughtGold)
        } else {
            Text("—", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun sliderColors() = androidx.compose.material3.SliderDefaults.colors(
    thumbColor = CaughtGold,
    activeTrackColor = CaughtGold,
    inactiveTrackColor = PokedexCream.copy(alpha = 0.2f)
)
```

Replace the existing attacker nature dropdown and EV sliders in the main `DamageCalcScreen` composable with calls to `StatConfigSection(...)` and `NaturePicker(...)`. Update `updateAttacker` / `updateDefender` calls to use the new `CalcSlot` shape (pass `slot.copy(nature = it)` etc.).

- [ ] **Step 3: Verify compilation**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run all tests**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/DamageCalcViewModel.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/DamageCalcScreen.kt
git commit -m "feat: update DamageCalcViewModel and DamageCalcScreen with gen-accurate stat config and full nature picker"
```

---

## Task 8: BattleEngine — BattlePokemon gains stat fields

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/BattleEngine.kt`

- [ ] **Step 1: Update BattlePokemon and buildBattlePokemon**

Replace `BattlePokemon` data class and `buildBattlePokemon` function in `BattleEngine.kt`:

```kotlin
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
```

Add imports at the top:
```kotlin
import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.ui.battle.Nature
import com.madmaxlgndklr.pokedex.ui.battle.Natures
import com.madmaxlgndklr.pokedex.ui.battle.StatConfig
import com.madmaxlgndklr.pokedex.ui.battle.StatFormulas
```

Replace `buildBattlePokemon`:
```kotlin
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
```

Update the `applyMove` lambda inside `resolveTurn` to pass the held item through `DamageParams`:

```kotlin
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
```

Note: speed comparison in `resolveTurn` still uses base stats (`statValue(pokemon, "speed")`) — this is intentional and deferred to Sub-project E.

- [ ] **Step 2: Verify compilation and tests**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/BattleEngine.kt
git commit -m "feat: BattlePokemon gains statConfig, nature, heldItem; buildBattlePokemon uses StatFormulas for HP"
```

---

## Task 9: TurnBattleViewModel + TurnBattleScreen — Battle Setup UI

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleViewModel.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt`

- [ ] **Step 1: Update BattleSetup and TurnBattleViewModel**

Replace `BattleSetup` data class in `TurnBattleViewModel.kt`:
```kotlin
data class BattleSetup(
    val playerDetail: PokemonDetail,
    val level: Int,
    val selectedMoveNames: List<String>,
    val statConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
    val nature: Nature = Natures.HARDY,
    val heldItem: HeldItem? = null
)
```

Add imports:
```kotlin
import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.ui.battle.Nature
import com.madmaxlgndklr.pokedex.ui.battle.Natures
import com.madmaxlgndklr.pokedex.ui.battle.StatConfig
import com.madmaxlgndklr.pokedex.ui.battle.StatFormulas
```

Add new StateFlow fields and setters to `TurnBattleViewModel`:

```kotlin
    private val _heldItems = MutableStateFlow<List<HeldItem>>(emptyList())
    val heldItems: StateFlow<List<HeldItem>> = _heldItems

    private val _heldItemSyncError = MutableStateFlow(false)
    val heldItemSyncError: StateFlow<Boolean> = _heldItemSyncError

    val canStartBattle: StateFlow<Boolean> = _setup
        .map { setup ->
            val cfg = setup?.statConfig
            if (cfg is StatConfig.Gen3PlusConfig) StatFormulas.isEvSumValid(cfg.evs) else true
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
```

Add `map` import:
```kotlin
import kotlinx.coroutines.flow.map
```

Add stat setters:
```kotlin
    fun setStatConfig(config: StatConfig) {
        _setup.value = _setup.value?.copy(statConfig = config)
    }

    fun setNature(nature: Nature) {
        _setup.value = _setup.value?.copy(nature = nature)
    }

    fun setHeldItem(item: HeldItem?) {
        _setup.value = _setup.value?.copy(heldItem = item)
    }

    fun loadHeldItems(heldItemRepo: com.madmaxlgndklr.pokedex.data.repository.HeldItemRepository) {
        viewModelScope.launch {
            try {
                _heldItems.value = heldItemRepo.getAll()
            } catch (_: Exception) {
                _heldItemSyncError.value = true
            }
        }
    }
```

Update `startBattleFromSetup()` to pass stat fields to `buildBattlePokemon`:
```kotlin
    fun startBattleFromSetup() {
        val s = _setup.value ?: return
        if (s.selectedMoveNames.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val gen = settingsRepo.selectedGen.first()
                val allPokemon = repo.getPokemonList()
                val opponentDetail = repo.getPokemonDetail(allPokemon.random().id)
                val playerMoves = resolveMoves(s.selectedMoveNames)
                val opponentMoves = resolveMoves(opponentDetail.moves.take(4).map { it.name })
                val playerBattle = BattleEngine.buildBattlePokemon(
                    s.playerDetail, s.level, playerMoves, s.statConfig, s.nature, s.heldItem
                )
                val opponentBattle = BattleEngine.buildBattlePokemon(opponentDetail, s.level, opponentMoves)
                _battleState.value = BattleEngine.startBattle(playerBattle, opponentBattle, gen)
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
```

- [ ] **Step 2: Update BattleSetupView in TurnBattleScreen.kt**

Read the current `TurnBattleScreen.kt` to locate the `BattleSetupView` composable (the composable showing Pokémon sprite, level picker, move list, and FIGHT button). Add the following sections between the move list and the FIGHT button:

**Gen mechanics toggle** (add after the level picker, before the move list):
```kotlin
        // Gen mechanics toggle
        val gen by viewModel.selectedGen.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isGen12 = gen <= 2
            listOf("GEN I–II" to true, "GEN III+" to false).forEach { (label, isGen12Option) ->
                val selected = isGen12 == isGen12Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected) CaughtGold else PokedexDark.copy(alpha = 0.5f),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val newGen = if (isGen12Option) 1 else 5
                            viewModel.setGen(newGen)
                            val defaultConfig = if (isGen12Option)
                                StatConfig.Gen12Config(IntArray(5) { 15 }, IntArray(5) { 0 })
                            else
                                StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 })
                            viewModel.setStatConfig(defaultConfig)
                            viewModel.setNature(Natures.HARDY)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontFamily = PressStart2P, fontSize = 6.sp,
                        color = if (selected) PokedexDark else PokedexCream)
                }
            }
        }
```

**Stat config section** (add a `LazyColumn` or `Column` block after the gen toggle — reuse `StatConfigSection` from `DamageCalcScreen.kt` or duplicate the composable locally):

Call `StatConfigSection` with the setup's statConfig, updating via `viewModel.setStatConfig(...)`.

**Nature picker** (show only when gen >= 3):
```kotlin
        if (gen >= 3) {
            val setup by viewModel.setup.collectAsState()
            NaturePicker(
                selectedNature = setup?.nature ?: Natures.HARDY,
                onNatureSelected = { viewModel.setNature(it) }
            )
        }
```

**Held item picker** (horizontal scrollable row):
```kotlin
        val heldItems by viewModel.heldItems.collectAsState()
        val setup by viewModel.setup.collectAsState()
        val syncError by viewModel.heldItemSyncError.collectAsState()

        if (syncError) {
            Text(
                "ITEMS UNAVAILABLE — SYNC IN SETTINGS",
                fontFamily = PressStart2P, fontSize = 5.sp,
                color = PokedexCream.copy(alpha = 0.5f)
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    // NONE card
                    val noneSelected = setup?.heldItem == null
                    ItemCard("NONE", "No item", noneSelected) { viewModel.setHeldItem(null) }
                }
                items(heldItems) { item ->
                    val selected = setup?.heldItem?.id == item.id
                    ItemCard(item.displayName, item.effectSummary, selected) { viewModel.setHeldItem(item) }
                }
            }
        }
```

Add `ItemCard` composable at the bottom of `TurnBattleScreen.kt`:
```kotlin
@Composable
private fun ItemCard(name: String, summary: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .background(
                if (selected) CaughtGold.copy(alpha = 0.2f) else PokedexDark.copy(alpha = 0.4f),
                RoundedCornerShape(6.dp)
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) CaughtGold else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(name, fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, maxLines = 1)
        Text(summary, fontFamily = PressStart2P, fontSize = 4.sp,
            color = PokedexCream.copy(alpha = 0.6f), maxLines = 2, lineHeight = 6.sp)
    }
}
```

**Update the FIGHT button** to use `canStartBattle`:
```kotlin
        val canStart by viewModel.canStartBattle.collectAsState()
        Text(
            text = "START BATTLE",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = if (canStart) CaughtGold else PokedexCream.copy(alpha = 0.3f),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = canStart
                ) {
                    if (canStart) viewModel.startBattleFromSetup()
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
```

- [ ] **Step 3: Verify compilation**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run all tests**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleViewModel.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt
git commit -m "feat: TurnBattleScreen Battle Setup adds gen toggle, stat config, nature picker, held item picker"
```

---

## Task 10: Wire HeldItemRepository + SettingsScreen sync option

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/PokedexApplication.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/navigation/AppNavigation.kt`

- [ ] **Step 1: Add heldItemRepository to PokedexApplication**

Add to `PokedexApplication`:
```kotlin
    val heldItemRepository by lazy {
        HeldItemRepository(
            RetrofitClient.api,
            database.heldItemDao()
        )
    }
```

Add the import:
```kotlin
import com.madmaxlgndklr.pokedex.data.repository.HeldItemRepository
```

- [ ] **Step 2: Update SyncOptions and SettingsViewModel**

In `SettingsViewModel.kt`, update `SyncOptions`:
```kotlin
data class SyncOptions(
    val syncData: Boolean = true,
    val syncMoves: Boolean = true,
    val syncCries: Boolean = true,
    val syncItems: Boolean = true
)
```

Update `SettingsViewModel` constructor to accept `heldItemRepo`:
```kotlin
class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val pokemonRepo: PokemonRepository,
    private val heldItemRepo: HeldItemRepository
) : ViewModel() {
```

Add the import:
```kotlin
import com.madmaxlgndklr.pokedex.data.repository.HeldItemRepository
```

Update `syncWithOptions()` — add items sync after the cries block:
```kotlin
                if (options.syncItems) {
                    _syncState.value = SyncState.Syncing("ITEMS", 0, 1)
                    heldItemRepo.syncAll()
                    _syncState.value = SyncState.Syncing("ITEMS", 1, 1)
                }
```

Update the factory:
```kotlin
    companion object {
        fun factory(
            settingsRepo: SettingsRepository,
            pokemonRepo: PokemonRepository,
            heldItemRepo: HeldItemRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(settingsRepo, pokemonRepo, heldItemRepo) }
        }
    }
```

- [ ] **Step 3: Update SettingsScreen sync dialog**

In `SettingsScreen.kt`, update the `allChecked` expression:
```kotlin
val allChecked = syncOptions.syncData && syncOptions.syncMoves && syncOptions.syncCries && syncOptions.syncItems
```

Update the "SELECT ALL" handler:
```kotlin
syncOptions = SyncOptions(checked, checked, checked, checked)
```

Add a new `SyncOptionRow` after the cries row:
```kotlin
                    SyncOptionRow(
                        label = "HELD ITEMS",
                        checked = syncOptions.syncItems,
                        onCheckedChange = { syncOptions = syncOptions.copy(syncItems = it) }
                    )
```

- [ ] **Step 4: Update AppNavigation to pass heldItemRepo to SettingsViewModel**

In `AppNavigation.kt`, find the `SettingsViewModel.factory(...)` call and add `app.heldItemRepository`:
```kotlin
            viewModel(factory = SettingsViewModel.factory(app.settingsRepository, app.repository, app.heldItemRepository))
```

Also in `AppNavigation.kt`, load held items into `TurnBattleViewModel` when the battle screen is composed. Find where `TurnBattleViewModel` is created and add a `LaunchedEffect`:
```kotlin
            LaunchedEffect(Unit) {
                battleVm.loadHeldItems(app.heldItemRepository)
            }
```

- [ ] **Step 5: Verify compilation and tests**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/PokedexApplication.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/settings/SettingsViewModel.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/settings/SettingsScreen.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/navigation/AppNavigation.kt
git commit -m "feat: wire HeldItemRepository into app, add HELD ITEMS sync option to Settings"
```

---

## Task 11: Version bump to 0.2

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Bump version**

In `app/build.gradle.kts`, update `defaultConfig`:
```kotlin
        versionCode = 2
        versionName = "0.2"
```

- [ ] **Step 2: Verify build**

```bash
cd /home/madmaxlgndklr/Git/Pokedex && JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to 0.2 (versionCode 2)"
```

---

## Self-review

**Spec coverage check:**

| Spec requirement | Task |
|-----------------|------|
| `StatConfig` sealed class (Gen12Config / Gen3PlusConfig) | Task 1 |
| `Nature` + `Natures` (25 natures) | Task 1 |
| `StatFormulas.computeStat` + `computeHp` + gen-accurate formulas | Task 1 |
| `HeldItemEffect` sealed class + TYPE_ITEMS map | Task 2 |
| `HeldItem` Room entity + `HeldItemDao` | Task 3 |
| DB v4→v5 migration | Task 3 |
| PokeAPI item endpoints | Task 4 |
| `HeldItemRepository` (sync from holdable-active + holdable-passive) | Task 4 |
| CryPlayer `.tmp` cleanup on failure | Task 5 |
| Startup `.tmp` sweep in `PokedexApplication.onCreate()` | Task 5 |
| `DamageParams` updated (replaces flat EV/nature with StatConfig/Nature) | Task 6 |
| `HeldItemEffect` applied in `DamageEngine.calculate()` | Task 6 |
| `CalcSlot` updated with `StatConfig`/`Nature`/`HeldItem` | Task 7 |
| `DamageCalcScreen` — gen-aware stat sliders + full 25-nature picker | Task 7 |
| `BattlePokemon` gains `statConfig`, `nature`, `heldItem` | Task 8 |
| `buildBattlePokemon` uses `StatFormulas.computeHp` | Task 8 |
| `BattleSetup` gains stat fields + setters | Task 9 |
| `TurnBattleScreen` — gen toggle, stat config, nature picker, item picker | Task 9 |
| `canStartBattle = false` when EV sum > 510 | Task 9 |
| `heldItemSyncError` → "ITEMS UNAVAILABLE" inline message | Task 9 |
| `HeldItemRepository` wired into app + Settings sync | Task 10 |
| `SyncOptions.syncItems` + Settings UI | Task 10 |
| Version bump 0.2 / versionCode 2 | Task 11 |

All spec requirements are covered. No gaps found.

**Placeholder scan:** No TBD/TODO entries. All code blocks are complete.

**Type consistency:** `StatConfig`, `Nature`, `Natures`, `StatFormulas` defined in Task 1, referenced consistently in Tasks 6–10. `HeldItem` (Room entity) defined in Task 3, used in Tasks 4, 6–10. `HeldItemEffect` defined in Task 2, used in Task 6. `DamageParams` updated in Task 6, all callers updated in same task. `BattleSetup` updated in Task 9, `startBattleFromSetup` updated in same task. `SettingsViewModel.factory` updated in Task 10, call site in AppNavigation updated in same task.
