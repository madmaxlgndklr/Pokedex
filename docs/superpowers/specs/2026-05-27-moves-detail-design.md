# Moves Detail Screen — Design Spec

**Date:** 2026-05-27

## Goal

When a user taps a move name on the Pokémon detail screen, navigate to a dedicated Move Detail screen showing the move's stats, effect description, and a list of Pokémon that can learn it.

---

## Data Layer

### API

New PokeAPI endpoint already available on the local instance:

```
GET /api/v2/move/{name}/
```

Relevant fields from the response:
- `name` — move name
- `type.name` — type string (fire, water, etc.)
- `damage_class.name` — physical / special / status
- `power` — integer or null (status moves)
- `accuracy` — integer or null
- `pp` — integer
- `effect_entries[].short_effect` — one-line effect description (English)
- `learned_by_pokemon[]` — list of `{ name, url }` for every Pokémon that learns it

### New Files — Data Layer

| File | Responsibility |
|---|---|
| `data/remote/dto/MoveResponse.kt` | Gson-mapped DTO for the move endpoint |
| `data/local/MoveEntity.kt` | Room `@Entity` — stores move by name, serializes `learnedBy` as JSON string |
| `data/local/MoveDao.kt` | `@Dao` — `insert(MoveEntity)`, `getByName(name): MoveEntity?` |

`AppDatabase` bumped to **v4** with a migration that adds the `moves` table (no data loss — existing tables unchanged).

`PokemonRepository` gets one new method:

```kotlin
suspend fun getMove(name: String): Move
```

Cache-first: checks `MoveDao`, fetches from API and stores if missing.

### Domain Model

```kotlin
data class Move(
    val name: String,
    val type: String,
    val category: String,       // "physical" | "special" | "status"
    val power: Int?,
    val accuracy: Int?,
    val pp: Int,
    val effectText: String,
    val learnedBy: List<PokemonSummary>  // reuses existing PokemonSummary(id, name)
)
```

Learners list capped at **60** entries for display. If `learned_by_pokemon` returns more, store all in cache but only pass the first 60 (sorted by Pokédex number) to the UI, with a count label showing how many were omitted.

---

## UI

### New Files — UI Layer

| File | Responsibility |
|---|---|
| `ui/move/MoveDetailViewModel.kt` | Loads `Move` via repository; exposes `UiState<Move>` |
| `ui/move/MoveDetailScreen.kt` | Full detail layout |

### Screen Layout

Follows the existing `BoxWithConstraints` + `pdex_open_v2` background pattern.

Top to bottom:

1. **Back button** — top-left, same style as detail screen
2. **Move name** — Press Start 2P, all caps
3. **Type badge + category chip** — inline row
   - Type badge: reuses `TypeBadge` composable
   - Category chip: color-coded — Physical (orange `#F08030`), Special (blue `#6890F0`), Status (gray `#A8A878`)
4. **Stats row** — three labeled boxes side by side:
   - Power: integer or `—` if null
   - Accuracy: integer + `%` or `—` if null
   - PP: integer
5. **Effect text** — styled paragraph, Press Start 2P at small size or a readable fallback font
6. **LEARNED BY** section header
7. **Lazy vertical grid** (2 columns) of `PokemonCard` composables — same cards used on the list screen (sprite + number + name). If learners were truncated, a final row shows "+ N more Pokémon" in muted text.

### Loading / Error States

Reuses existing `UiState` pattern:
- `Loading` → centered `CircularProgressIndicator`
- `Error` → centered error text with retry button
- `Success` → full layout above

---

## Navigation

New route added to `AppNavigation.kt`:

```
move/{moveName}
```

In `DetailScreen.kt`, each move row in the moves list becomes clickable:

```kotlin
navController.navigate("move/$moveName")
```

No changes needed to `BottomNavBar` — this is a push destination, not a tab.

---

## Testing

One new unit test file: `MoveDetailViewModelTest.kt`

- Verifies `UiState` transitions: Loading → Success on cache hit, Loading → Success on fetch, Loading → Error on network failure
- Verifies learners list is capped at 60 and count label is correct when truncated

---

## Out of Scope

- Move flavor text by game version
- Contest stats / Z-move / Max move data
- Learners list pagination (beyond the 60-cap + count label)
- Move history / generation introduced
