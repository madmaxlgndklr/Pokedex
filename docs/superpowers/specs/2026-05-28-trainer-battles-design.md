# Trainer Battles Design

**Date:** 2026-05-28
**Status:** Approved
**Scope:** Sub-project E-2 — full trainer battles with opponent selection

---

## Goal

Replace the single random-Pokémon opponent with a selectable roster of named trainers from all main-series generations (I–IX). Players pick a trainer from a grouped region list, optionally choose a roster variant (original vs. rematch for Gen 1/2 gym leaders), and fight a full 6v6 battle using the team-switching engine built in E-1.

---

## Architecture

Four new files and three modified files. Trainer data ships as a bundled JSON asset; no database needed. The existing `TurnBattleViewModel` is extended with trainer-aware entry points. A new `TrainerSelectViewModel` manages the selection UI state. `BattleHubScreen` gains a fourth tab.

---

## Data Model

### `TrainerRoster.kt`

```kotlin
enum class TrainerClass { GYM_LEADER, ELITE_FOUR, CHAMPION, RIVAL }

data class TrainerPokemon(
    val pokemonId: Int,
    val level: Int,
    val moves: List<String>   // move names; resolved via existing resolveMoves()
)

data class TrainerRoster(
    val label: String,        // e.g., "Original (RBY)" or "Rematch (FRLG)"
    val team: List<TrainerPokemon>   // always 6 Pokémon
)

data class Trainer(
    val id: String,           // e.g., "kanto-brock"
    val name: String,
    val title: String,        // "Gym Leader", "Elite Four", "Champion", "Rival"
    val region: String,       // "Kanto", "Johto", "Hoenn", "Sinnoh", "Unova", "Kalos", "Alola", "Galar", "Paldea"
    val trainerClass: TrainerClass,
    val typeSpecialty: String,   // primary type, e.g. "Rock"; Champions/Rivals use "Mixed"
    val rosters: List<TrainerRoster>   // 1 for most; 2 for Gen 1/2 gym leaders
)
```

**Team size rule:** All rosters have exactly 6 Pokémon. Gen 1/2 gym leaders with fewer than 6 canonical Pokémon are handled as follows:
- If an FRLG or HGSS rematch roster exists with 6 Pokémon, that becomes the `"Rematch (FRLG)"` / `"Rematch (HGSS)"` roster.
- The original game roster is padded to 6 with same-type Pokémon at thematically consistent levels.

Trainers with exactly one roster (all Gen 3+ trainers, Elite Four, Champions) have `rosters.size == 1` and no toggle is shown in the UI.

---

## JSON Asset Format

**Path:** `app/src/main/assets/trainers.json`

```json
{
  "regions": [
    {
      "name": "Kanto",
      "trainers": [
        {
          "id": "kanto-brock",
          "name": "Brock",
          "title": "Gym Leader",
          "trainerClass": "GYM_LEADER",
          "typeSpecialty": "Rock",
          "rosters": [
            {
              "label": "Original (RBY)",
              "team": [
                { "pokemonId": 74, "level": 12, "moves": ["tackle", "defense-curl", "mud-slap", "rock-throw"] },
                { "pokemonId": 95, "level": 14, "moves": ["tackle", "screech", "bind", "rock-throw"] },
                { "pokemonId": 75, "level": 12, "moves": ["tackle", "defense-curl", "mud-slap", "magnitude"] },
                { "pokemonId": 246, "level": 10, "moves": ["tackle", "leer", "bite", "sandstorm"] },
                { "pokemonId": 111, "level": 11, "moves": ["tackle", "tail-whip", "stomp", "horn-attack"] },
                { "pokemonId": 213, "level": 11, "moves": ["tackle", "harden", "constrict", "rock-throw"] }
              ]
            },
            {
              "label": "Rematch (FRLG)",
              "team": [
                { "pokemonId": 75, "level": 51, "moves": ["earthquake", "rock-slide", "explosion", "stealth-rock"] },
                { "pokemonId": 76, "level": 51, "moves": ["earthquake", "rock-blast", "explosion", "stealth-rock"] },
                { "pokemonId": 95, "level": 52, "moves": ["earthquake", "rock-slide", "screech", "iron-tail"] },
                { "pokemonId": 248, "level": 52, "moves": ["earthquake", "crunch", "rock-slide", "dragon-dance"] },
                { "pokemonId": 112, "level": 53, "moves": ["earthquake", "megahorn", "rock-slide", "stomp"] },
                { "pokemonId": 377, "level": 54, "moves": ["earthquake", "ancientpower", "calm-mind", "psychic"] }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

---

## TrainerRepository

**File:** `app/src/main/java/com/madmaxlgndklr/pokedex/data/trainer/TrainerRepository.kt`

Reads and parses `trainers.json` from the Android asset manager using Gson. Results are cached in memory after first load.

```kotlin
class TrainerRepository(private val context: Context) {
    private var cache: List<Trainer>? = null

    fun getAll(): List<Trainer>
    fun getByRegion(region: String): List<Trainer>
    fun getById(id: String): Trainer?
}
```

No Room database. Trainer data is static and ships with the app.

---

## TrainerSelectViewModel

**File:** `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerSelectViewModel.kt`

```kotlin
class TrainerSelectViewModel(private val repo: TrainerRepository) : ViewModel() {
    val trainers: StateFlow<List<Trainer>>           // all trainers, loaded at init
    val expandedRegions: StateFlow<Set<String>>      // which region sections are expanded
    val sheetTrainer: StateFlow<Trainer?>            // trainer whose bottom sheet is open
    val sheetRosterIndex: StateFlow<Int>             // 0 or 1, for roster toggle

    fun toggleRegion(region: String)
    fun openSheet(trainer: Trainer)
    fun closeSheet()
    fun setRosterIndex(index: Int)

    companion object {
        fun factory(repo: TrainerRepository): ViewModelProvider.Factory
    }
}
```

`sheetTrainer` is distinct from `TurnBattleViewModel.battleTrainer` — it only tracks which bottom sheet is visible. `TurnBattleViewModel.battleTrainer` is set when the player commits to a trainer (Quick Battle or Configure).

---

## TurnBattleViewModel Changes

### New state

```kotlin
data class SelectedTrainer(val trainer: Trainer, val rosterIndex: Int)

val battleTrainer: StateFlow<SelectedTrainer?>   // non-null when a trainer battle is configured
```

### New methods

```kotlin
fun loadTrainerSetup(trainer: Trainer, rosterIndex: Int, teamIds: List<Int>)
```
Sets `battleTrainer` to `SelectedTrainer(trainer, rosterIndex)`, then calls the existing `loadSetup(teamIds)`. The setup screen reads `battleTrainer` to show the trainer's name in the opponent slot.

```kotlin
fun startTrainerBattle(trainer: Trainer, rosterIndex: Int, teamIds: List<Int>)
```
Builds player team from `teamIds` using current setup defaults (same logic as `startBattleFromSetup`). Builds opponent team from `trainer.rosters[rosterIndex].team` — each `TrainerPokemon` resolved via `BattleEngine.buildBattlePokemon`. Calls `BattleEngine.startBattle(playerTeam, opponentTeam, gen)`.

### Modified methods

`resetToSetup()` clears `battleTrainer` to null.

`startBattleFromSetup(teamIds)` checks `battleTrainer`: if non-null, uses `battleTrainer.trainer.rosters[battleTrainer.rosterIndex].team` as the opponent instead of a random Pokémon.

---

## BattleHubScreen Changes

- Tab enum gains `TRAINERS`: `enum class BattleTab { CALC, BATTLE, MATCHUP, TRAINERS }`
- Tab labels relabeled: **CALC / WILD / TRAIN / MATCHUP**
- `TRAIN` tab renders `TrainerSelectScreen`
- `BattleHubScreen` receives `trainerVm: TrainerSelectViewModel`
- Quick Battle callback: `trainerVm.closeSheet(); battleVm.startTrainerBattle(trainer, trainerVm.sheetRosterIndex.value, teamIds); selectedTab = BattleTab.BATTLE`
- Configure callback: `trainerVm.closeSheet(); battleVm.loadTrainerSetup(trainer, trainerVm.sheetRosterIndex.value, teamIds); selectedTab = BattleTab.BATTLE`

---

## TrainerSelectScreen

**File:** `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerSelectScreen.kt`

### Region list

`LazyColumn` with collapsible region sections. Each section header:
- Region name in PressStart2P
- ▼/▲ expand toggle
- Tap toggles `TrainerSelectViewModel.expandedRegions`

Each trainer card (visible when section is expanded):
- Left accent bar: `typeColor(trainer.typeSpecialty)`
- Trainer name + title (`"Brock · Gym Leader"`)
- Type specialty badge
- Row of 6 × 24dp Pokémon sprites from primary roster (`rosters[0]`)
- Tap → `viewModel.openSheet(trainer)`

### Bottom sheet (`ModalBottomSheet`)

Shown when `sheetTrainer != null`.

- Header: trainer name, title, region
- **Roster toggle** (only when `trainer.rosters.size > 1`): two chips showing `roster.label`; tapping calls `viewModel.setRosterIndex()`; team preview below updates immediately
- Team preview: 6 rows — sprite (40dp) + Pokémon name + "Lv.XX"
- **QUICK BATTLE** button (PokedexRed): calls Quick Battle callback
- **CONFIGURE** button (GlowBlue outline): calls Configure callback
- Sheet is dismissible by swiping down

---

## BattleSetupView Changes

When `battleTrainer != null`, the setup screen shows the trainer's name and type specialty badge in the opponent slot instead of "RANDOM OPPONENT." All other setup controls (level, moves, nature, slot overrides) remain unchanged — the player still configures their own team.

---

## AppNavigation Changes

`TrainerRepository` instantiated once in `AppNavigation` using `LocalContext`. `TrainerSelectViewModel` created with `viewModel(factory = TrainerSelectViewModel.factory(repo))`. Both passed to `BattleHubScreen`.

---

## Error Handling

- JSON parse failure: `TrainerRepository.getAll()` returns empty list; TRAIN tab shows "TRAINERS UNAVAILABLE" message.
- `pokemonId` not found in PokeAPI cache during battle start: slot is skipped; if fewer than 1 Pokémon resolved, battle start is aborted with no state change.
- Empty `teamIds` passed to `startTrainerBattle`: no-op (existing guard).

---

## Testing

All tests are unit tests.

| Test | Assertion |
|------|-----------|
| `TrainerRepository` parses minimal JSON | Returns correct trainer count and roster labels |
| `TrainerRepository` filters by region | Returns only trainers from that region |
| `startTrainerBattle` produces Ongoing state | `BattleState.Ongoing` with opponent team size 6 |
| `startTrainerBattle` with rosterIndex=1 | Uses second roster's Pokémon |
| `loadTrainerSetup` sets battleTrainer | `battleTrainer.value == trainer` after call |
| `resetToSetup` clears battleTrainer | `battleTrainer.value == null` after call |
| `TrainerSelectViewModel toggleRegion` | Region added/removed from expandedRegions |
| `TrainerSelectViewModel setRosterIndex` | `sheetRosterIndex` updates |

---

## Out of Scope

- Trainer sprites / artwork
- Battle rewards or badges
- Trainer AI beyond the existing `aiPickAction` (type-aware switching already implemented in E-1)
- Post-battle rematch tracking
- Online / multiplayer
