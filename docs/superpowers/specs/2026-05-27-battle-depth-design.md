# Sub-project B: Battle Depth — Design Spec

**Date:** 2026-05-27  
**Branch:** feature/pokedex-app

---

## Goal

Add gen-accurate IV/EV/DV stat configuration, nature modifiers, and held items to the Pokédex battle system via a dedicated Battle Setup Screen.

## Scope

This sub-project covers stat config, natures, and held items in the damage calculator and battle sim. Full-team 6v6 battles and gym leader / trainer challenge mode are Sub-project E and are explicitly out of scope here.

---

## Architecture

### New Components

**`StatConfig`** — sealed class replacing flat `attackEVs`/`defenseEVs` in `DamageParams`:
```kotlin
sealed class StatConfig {
    data class Gen12Config(
        val dvs: IntArray,      // index 0–4: HP, Atk, Def, Spe, Spc — values 0–15
        val statExp: IntArray   // same indices — values 0–65535
    ) : StatConfig()
    data class Gen3PlusConfig(
        val ivs: IntArray,      // index 0–5: HP, Atk, Def, SpAtk, SpDef, Spe — values 0–31
        val evs: IntArray       // same indices — values 0–252, sum ≤ 510
    ) : StatConfig()
}
```

**`Nature`** — static data object (25 natures, never changes):
```kotlin
data class Nature(val name: String, val boostedStat: Int?, val droppedStat: Int?)
// boostedStat/droppedStat are stat indices (1=Atk, 2=Def, 3=SpAtk, 4=SpDef, 5=Spe), null for neutral
```

**`HeldItem`** — Room entity added in DB v5 migration:
```kotlin
@Entity(tableName = "held_items")
data class HeldItem(
    @PrimaryKey val id: Int,
    val name: String,           // PokeAPI name e.g. "choice-band"
    val displayName: String,    // e.g. "Choice Band"
    val effectSummary: String   // one-line description for UI
)
```

**`HeldItemEffect`** — sealed class keyed by item name, applied in damage calc:
```kotlin
sealed class HeldItemEffect {
    data class StatMultiplier(val statIndex: Int, val factor: Float) : HeldItemEffect()
    data class DamageMultiplier(val factor: Float) : HeldItemEffect()
    data class TypeMultiplier(val factor: Float) : HeldItemEffect()        // type-matching plates/orbs
    data class SuperEffectiveBoost(val factor: Float) : HeldItemEffect()   // Expert Belt
    object None : HeldItemEffect()

    companion object {
        // All 17 type plates + 2 type orbs listed explicitly — no string parsing needed
        private val TYPE_ITEMS = mapOf(
            "flame-plate" to "fire", "splash-plate" to "water", "zap-plate" to "electric",
            "meadow-plate" to "grass", "icicle-plate" to "ice", "fist-plate" to "fighting",
            "toxic-plate" to "poison", "earth-plate" to "ground", "sky-plate" to "flying",
            "mind-plate" to "psychic", "insect-plate" to "bug", "stone-plate" to "rock",
            "spooky-plate" to "ghost", "draco-plate" to "dragon", "dread-plate" to "dark",
            "iron-plate" to "steel", "pixie-plate" to "fairy",
            "charcoal" to "fire", "mystic-water" to "water", "magnet" to "electric",
            "miracle-seed" to "grass", "never-melt-ice" to "ice", "black-belt" to "fighting",
            "poison-barb" to "poison", "soft-sand" to "ground", "sharp-beak" to "flying",
            "twisted-spoon" to "psychic", "silver-powder" to "bug", "hard-stone" to "rock",
            "spell-tag" to "ghost", "dragon-fang" to "dragon", "black-glasses" to "dark",
            "metal-coat" to "steel", "fairy-feather" to "fairy"
        )

        fun from(name: String): HeldItemEffect = when {
            name == "choice-band"  -> StatMultiplier(statIndex = 1, factor = 1.5f)
            name == "choice-specs" -> StatMultiplier(statIndex = 3, factor = 1.5f)
            name == "life-orb"     -> DamageMultiplier(factor = 1.3f)
            name == "expert-belt"  -> SuperEffectiveBoost(factor = 1.2f)
            name in TYPE_ITEMS     -> TypeMultiplier(factor = 1.2f) // type matched at call site via TYPE_ITEMS[name]
            else                   -> None
        }

        fun typeFor(name: String): String? = TYPE_ITEMS[name]
    }
}
```

Items with battle-sim-only effects (Leftovers, Sitrus Berry, Rocky Helmet) are stored in Room and displayed in the picker but return `HeldItemEffect.None` from `from()` — their effects are deferred to Sub-project E.

**`HeldItemDao`** — standard Room DAO: `getAll()`, `upsertAll()`, `deleteAll()`.

**`HeldItemRepository`** — fetches from PokeAPI `/item-attribute/holdable-active/` and `/item-attribute/holdable-passive/`, resolves each item URL, stores results in Room.

**`BattleSetupViewModel`** — new ViewModel managing pre-battle state:
- `generationMechanics: StateFlow<GenMechanics>` (Gen12 | Gen3Plus)
- `statConfig: StateFlow<StatConfig>` (resets to defaults on mechanic switch)
- `nature: StateFlow<Nature>` (ignored for Gen12, defaults to Hardy for Gen3+)
- `heldItem: StateFlow<HeldItem?>` (null = no item)
- `heldItems: StateFlow<List<HeldItem>>` (loaded from Room)
- `canStartBattle: StateFlow<Boolean>` (false if Gen3+ EV sum > 510)

### Updated Components

**`DamageParams`** — replace flat EV/nature fields:
```kotlin
data class DamageParams(
    val playerPokemon: PokemonDetail,
    val opponentPokemon: PokemonDetail,
    val moveIndex: Int,
    val statConfig: StatConfig = StatConfig.Gen3PlusConfig(
        ivs = IntArray(6) { 31 },
        evs = IntArray(6) { 0 }
    ),
    val nature: Nature = Natures.HARDY,
    val heldItem: HeldItem? = null
)
```

**`DamageCalculator`** — new helpers:
- `computeStat(base: Int, config: StatConfig, nature: Nature, statIndex: Int, level: Int): Int`
- `applyHeldItem(damage: Int, effect: HeldItemEffect, moveType: String, effectiveness: Float): Int`

**Room DB** — bump to v5 with `AutoMigration(from = 4, to = 5)` adding `held_items` table.

---

## Stat Formulas

### Gen I (HP):
```
HP = floor((base + dv) * 2 + floor(sqrt(statExp)) / 4) * level / 100) + level + 10
```
### Gen I (other stats):
```
Stat = floor((base + dv) * 2 + floor(sqrt(statExp)) / 4) * level / 100) + 5
```
### Gen III+ (HP):
```
HP = floor((2 * base + iv + floor(ev / 4)) * level / 100) + level + 10
```
### Gen III+ (other stats):
```
Stat = floor((floor((2 * base + iv + floor(ev / 4)) * level / 100) + 5) * natureMultiplier)
```
Nature multiplier: 1.1 (boosted), 0.9 (dropped), 1.0 (neutral/HP).

Gen II uses the Gen I formula with the Special stat split into SpAtk and SpDef — each inherits the Special DV.

---

## Battle Setup Screen

**Navigation:** existing battle trigger → `BattleSetupScreen` → `TurnBattleScreen` (unchanged).

**Layout:** `pdex_open_v2` background, scrollable `LazyColumn` inside the blue panel.

**Sections (top to bottom):**

1. **Header** — "BATTLE SETUP" in `CaughtGold`, `PressStart2P`
2. **Pokémon & Opponent** — existing picker pattern, no redesign
3. **Mechanics toggle** — `GEN I–II` / `GEN III+` button pair, updates `generationMechanics`
4. **Stat config** — conditional on mechanic:
   - *Gen I–II:* Five labeled rows (HP, ATK, DEF, SPE, SPC). Each: DV slider 0–15 + Stat Exp numeric input 0–65535.
   - *Gen III+:* Six labeled rows (HP, ATK, DEF, SPATK, SPDEF, SPE). Each: IV slider 0–31 + EV slider 0–252. Running EV total shown as `"[used]/510 EVs"`, turns red when exceeded.
5. **Nature picker** — Gen III+ only. 5×5 grid of nature names matching the in-game table. Selected nature highlighted in `CaughtGold`. Tapping shows inline boost/drop display (e.g., "↑ ATK  ↓ SPATK"). Neutral natures show "—".
6. **Held item picker** — horizontal scrollable row of `HeldItem` cards from Room. "NONE" card always first, selected by default. Each card shows `displayName` + `effectSummary`.
7. **START BATTLE** — disabled (greyed) if Gen III+ EV sum > 510. On tap: build `DamageParams` from current state, navigate to `TurnBattleScreen`.

---

## Temp-File Safety

Applies to both `CryPlayer.downloadCry()` and `HeldItemRepository` item fetches.

Pattern: write to `${finalPath}.tmp` → `renameTo(finalPath)` on success → delete `.tmp` on failure.

Startup sweep in `PokedexApplication.onCreate()`:
```kotlin
filesDir.listFiles { _, name -> name.endsWith(".tmp") }?.forEach { it.delete() }
```

`renameTo()` is atomic on the same filesystem (`filesDir` is always internal storage — same partition).

---

## Error Handling

- **Held item sync failure:** `HeldItemRepository` throws; `BattleSetupViewModel` catches and exposes `heldItemSyncError: StateFlow<Boolean>`. Setup screen shows "ITEMS UNAVAILABLE — SYNC IN SETTINGS" inline, held item section hidden. Battle can still start without an item.
- **PokeAPI item endpoint empty:** Treat as sync failure above.
- **EV cap exceeded:** `canStartBattle = false`, START BATTLE button disabled. No dialog — the EV counter turning red is the signal.
- **Corrupt `.tmp` files:** Swept on launch before any repository initializes.

---

## Testing

**`StatConfigTest`** — pure math, no Android deps:
- Gen I stat formula: Bulbasaur base 45 Atk, DV=15, StatExp=0, level=50 → assert known value
- Gen I HP formula: same Pokémon, HP variant
- Gen III+ stat formula: known base + IV=31 + EV=252 + boosting nature → assert known value
- Gen III+ HP: nature multiplier not applied
- Gen II Special DV inherited by SpAtk and SpDef
- EV sum validation: [0,252,252,0,0,0] → valid; [0,252,252,4,4,0] → invalid (512 > 510)

**`NatureTest`**:
- All 25 natures parse correctly (boostedStat, droppedStat)
- 5 neutral natures return 1.0 multiplier for all stat indices
- Adamant: stat index 1 (Atk) → 1.1, stat index 3 (SpAtk) → 0.9

**`HeldItemEffectTest`**:
- `from("choice-band")` → `StatMultiplier(1, 1.5f)`
- `from("life-orb")` → `DamageMultiplier(1.3f)`
- `from("expert-belt")` → `SuperEffectiveBoost(1.2f)`
- `from("leftovers")` → `None`
- `from("unknown-item")` → `None`

**`DamageCalculatorTest`** — extends existing:
- Gen I config produces same result as old flat-field baseline (regression)
- Gen III+ config with Adamant nature increases physical damage vs Hardy
- Choice Band boosts computed attack stat before damage formula
- Life Orb multiplies final damage output by 1.3
- `heldItem = null` produces no change (baseline parity)

**`HeldItemRepositoryTest`** — mocked DAO and OkHttp:
- Sync: `.tmp` file created before final file exists
- On success: `renameTo()` called, final file exists, `.tmp` gone
- On failure: `.tmp` deleted, final file absent
- Startup sweep: pre-existing `.tmp` files deleted before any repo call

**`BattleSetupViewModelTest`**:
- EV sum = 511 → `canStartBattle = false`
- EV sum = 510 → `canStartBattle = true`
- Switching to Gen I–II → `statConfig` resets to default DVs/StatExp, nature multiplier = 1.0
- Selecting held item → `heldItem` state updates
- Gen I–II + any nature → computed nature multiplier is always 1.0
