# Pokédex Android App — Design Spec

**Date:** 2026-05-24  
**Repo:** madmaxlgndklr/Pokedex  
**Branch:** feature/pokedex-app  
**Stack:** Kotlin · Jetpack Compose · MVVM · Retrofit · Room

---

## Overview

A retro-styled Android Pokédex app that connects to a locally running PokeAPI instance. Users can browse all Pokémon, search and filter by name or type, view full detail screens, and track which Pokémon they've caught via a local database.

---

## Features

1. **Browse** — scrollable grid of all Pokémon with sprite, number, name, and type badges
2. **Search & Filter** — search by name; filter by type via chip row; operates on in-memory list (no extra API calls)
3. **Detail Screen** — sprite, name, number, types, base stats, moves list (name + level), evolution chain (tappable), and Pokédex flavor text
4. **Caught Tracker** — ★ toggle on detail screen writes to Room; My Pokédex drawer section shows caught-only grid; works offline

---

## Architecture

### Layers

```
UI Layer          ViewModel Layer       Data Layer
──────────        ───────────────       ──────────────────────────────
ListScreen   ──▶  PokemonListVM   ──▶  PokemonRepository
DetailScreen ──▶  PokemonDetailVM ──▶    ├── PokeApiService (Retrofit)
SearchScreen ──▶  SearchVM        ──▶    │     └── http://10.0.2.2:89/api/v2/
MyPokedexScr ──▶  MyPokedexVM     ──▶    └── CaughtPokemonDao (Room)
```

ViewModels expose `UiState<T>` via `StateFlow`. Repository is the single source of truth. ViewModels never touch Retrofit or Room directly.

### UiState

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

### Package Structure

```
com.madmaxlgndklr.pokedex
├── data/
│   ├── remote/        Retrofit service + response DTOs
│   ├── local/         Room DB, DAO, CaughtPokemonEntity
│   └── repository/    PokemonRepository
├── ui/
│   ├── list/          ListScreen + PokemonListViewModel
│   ├── detail/        DetailScreen + PokemonDetailViewModel
│   ├── search/        SearchScreen + SearchViewModel
│   ├── mycollection/  MyCollectionScreen + MyCollectionViewModel
│   └── theme/         PokedexTheme, colors, typography
└── model/             Domain models (PokemonSummary, PokemonDetail, etc.)
```

---

## Navigation

Compose Navigation with a `ModalNavigationDrawer`. Drawer has three destinations:

| Drawer Entry | Screen | Description |
|---|---|---|
| Pokédex | ListScreen | Full Pokémon grid |
| Search | SearchScreen | Name search + type filter |
| My Pokédex | MyCollectionScreen | Caught-only grid |

All list screens navigate to `DetailScreen` on card tap, passing the Pokémon ID. Back navigates back to the originating screen.

---

## Screens

### List Screen

- 2-column `LazyVerticalGrid`
- Each `PokemonCard`: sprite (Coil), `#001` number, name, type badge chips
- TopAppBar with pixel font title and hamburger menu icon
- Retro green background (`#88CC00`)

### Search Screen

- Text input at top (search by name, filters in-memory list)
- Type filter chip row below (all 18 types; multi-select)
- Same `PokemonCard` grid below
- Filters compose: name AND selected types; zero types selected = no type filter (show all)

### Detail Screen

- Back arrow + Pokémon name in TopAppBar; ★ toggle (gold when caught) in top-right
- Large sprite centered
- Type badge chips
- Stat bars (pixel-style, labeled: HP / Attack / Defense / Sp. Atk / Sp. Def / Speed)
- Moves section: scrollable list of `LvN MoveName`
- Evolution chain: horizontal row of sprite thumbnails connected by `→` arrows; each sprite is tappable and navigates to that Pokémon's detail screen
- Pokédex entry: flavor text from the English `flavor_text_entries` (first available entry)
- Dark background (`#1A1A1A`) for readability

### My Pokédex Screen

- Same 2-column grid as List, filtered to caught Pokémon from Room
- Empty state: "NO POKÉMON CAUGHT YET" in pixel font
- Reactive — updates immediately when ★ is toggled on detail screen

---

## Visual Design

### Color Palette

| Role | Hex |
|---|---|
| Primary red | `#CC0000` |
| Dark red (drawer bg, pressed) | `#8B0000` |
| Screen green (list bg) | `#88CC00` |
| Detail bg | `#1A1A1A` |
| Text on dark | `#F5F5DC` |
| Caught gold (★) | `#FFD700` |

### Type Badge Colors (all 18 types)

| Type | Color | Type | Color |
|---|---|---|---|
| Normal | `#A8A878` | Fire | `#F08030` |
| Water | `#6890F0` | Electric | `#F8D030` |
| Grass | `#78C850` | Ice | `#98D8D8` |
| Fighting | `#C03028` | Poison | `#A040A0` |
| Ground | `#E0C068` | Flying | `#A890F0` |
| Psychic | `#F85888` | Bug | `#A8B820` |
| Rock | `#B8A038` | Ghost | `#705898` |
| Dragon | `#7038F8` | Dark | `#705848` |
| Steel | `#B8B8D0` | Fairy | `#EE99AC` |

### Typography

- **Press Start 2P** (Google Fonts) — used throughout for headings, labels, stat names, numbers
- Body text (flavor text, move names) also Press Start 2P at 8–10sp

### Key Composables

| Composable | Purpose |
|---|---|
| `TypeBadge` | Colored chip with type name |
| `StatBar` | Labeled pixel-style progress bar (value / 255) |
| `PokemonCard` | Grid card: sprite + number + name + type badges |
| `EvolutionChain` | Horizontal sprite → arrow → sprite row; tappable nodes |

---

## Data Layer

### API Endpoints

All requests hit `http://10.0.2.2:89/api/v2/` (localhost from Android emulator).

| Purpose | Endpoint |
|---|---|
| Full list | `GET /pokemon/?limit=1500&offset=0` |
| Pokémon detail | `GET /pokemon/{id}/` |
| Species (flavor text, evo URL) | `GET /pokemon-species/{id}/` |
| Evolution chain | `GET /evolution-chain/{id}/` |

The full list (~1500 name+URL entries, ~60KB) is fetched once on startup and held in memory. Detail calls run on tap; species and evolution-chain calls run in parallel via `async/await` in `PokemonDetailViewModel`.

### Room Database

Single table `caught_pokemon`:

```kotlin
@Entity(tableName = "caught_pokemon")
data class CaughtPokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val caughtAt: Long = System.currentTimeMillis()
)
```

DAO operations: `insert`, `delete`, `getAll(): Flow<List<CaughtPokemonEntity>>`, `isCaught(id: Int): Flow<Boolean>`.

### Dependencies

```toml
# Networking
retrofit = "2.11.0"
okhttp = "4.12.0"
gson-converter = "2.11.0"

# Image loading
coil-compose = "2.7.0"

# Room
room = "2.6.1"

# Compose + Navigation
compose-bom = "2024.09.00"
navigation-compose = "2.8.0"

# Fonts
google-fonts = "1.7.0"

# Coroutines
kotlinx-coroutines = "1.9.0"
```

---

## Android Manifest

```xml
<uses-permission android:name="android.permission.INTERNET" />

<application
    android:usesCleartextTraffic="true"  <!-- required for http:// local API -->
    ...>
```

---

## Error Handling

| Scenario | Behaviour |
|---|---|
| API unreachable | Error state → retro "NO SIGNAL" screen with retry button |
| Slow detail load | Loading skeleton (placeholder boxes) while 3 API calls run in parallel |
| No evolutions | `EvolutionChain` composable renders single sprite node |
| Branching evolutions (e.g. Eevee) | Fan-out row showing all branches |
| Empty search results | Empty state message, not error |
| Caught toggle offline | Room write always succeeds (local only) |

---

## Out of Scope (v1)

- Moves detail screen (damage, accuracy, description)
- Pokémon comparison
- Notifications
- Sync / cloud backup of caught tracker
- Mega evolutions / regional forms in evolution chain
