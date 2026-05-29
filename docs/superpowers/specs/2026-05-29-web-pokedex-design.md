# Web Pokédex — Design Spec

**Date:** 2026-05-29
**Branch:** feature/pokedex-app (source); new repo `~/Git/web-pokedex`

---

## Goal

Build a full-featured browser-based Pokédex with feature parity to the Android app, using the same self-hosted PokeAPI backend, with independent local storage and a faithful dark/light theme.

## Architecture

Next.js 14 App Router + TypeScript. React Server Components fetch all Pokémon data from the self-hosted PokeAPI (`https://madmaxlgndklrpokeapi.com/api/v2/`) — Next.js caches these responses server-side so repeated visits are free. All user state (caught collection, team, battle config, battle records, theme preference) lives in IndexedDB via Dexie.js on the client. This split keeps the seam clean: when backend sync is added in a future iteration, only the Dexie hook implementations change — the UI stays unchanged.

## Tech Stack

- **Framework:** Next.js 14 App Router
- **Language:** TypeScript
- **Styling:** Tailwind CSS + CSS custom properties for theme switching
- **Font:** Press Start 2P (Google Fonts) — same pixel font as Android app
- **Local storage:** Dexie.js (IndexedDB wrapper)
- **API:** `https://madmaxlgndklrpokeapi.com/api/v2/` (self-hosted PokeAPI)
- **Sprites:** `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/{id}.png` (same as Android)
- **Testing:** Vitest (unit), React Testing Library (components)
- **Deployment:** Vercel initially; migrate to home server Docker stack alongside PokeAPI

---

## Project Structure

```
web-pokedex/
├── app/
│   ├── layout.tsx                  # Root layout: ThemeProvider, sidebar, font load
│   ├── page.tsx                    # Home / Search
│   ├── list/
│   │   └── page.tsx                # Full Pokédex list (server component)
│   ├── pokemon/
│   │   └── [id]/
│   │       └── page.tsx            # Pokémon detail (server component)
│   ├── move/
│   │   └── [name]/
│   │       └── page.tsx            # Move detail (server component)
│   ├── compare/
│   │   └── [firstId]/
│   │       └── page.tsx            # Compare two Pokémon
│   ├── collection/
│   │   └── page.tsx                # My Collection (client component)
│   ├── team/
│   │   └── page.tsx                # Team Builder (client component)
│   ├── battle/
│   │   └── page.tsx                # Battle Hub — all 5 tabs (client component)
│   └── settings/
│       └── page.tsx                # Settings (client component)
├── components/
│   ├── nav/
│   │   ├── Sidebar.tsx             # Collapsible sidebar: full → icon-only → hidden
│   │   └── MobileMenu.tsx          # Hamburger overlay for < 768px
│   ├── pokemon/
│   │   ├── PokemonCard.tsx         # Card used in list/collection views
│   │   ├── TypeBadge.tsx           # Type pill with colour coding
│   │   ├── StatBar.tsx             # Animated stat bar for detail view
│   │   └── SpriteImage.tsx         # Sprite with shiny toggle + caught overlay
│   ├── battle/
│   │   ├── DamageCalcScreen.tsx
│   │   ├── TurnBattleScreen.tsx
│   │   ├── TrainerSelectScreen.tsx
│   │   ├── MatchupScreen.tsx
│   │   └── RecordScreen.tsx
│   └── ui/
│       ├── Button.tsx
│       ├── Modal.tsx
│       ├── Badge.tsx
│       └── ThemeToggle.tsx         # Dark/light switch in sidebar footer
├── lib/
│   ├── api.ts                      # All PokeAPI fetch functions (server-side, typed)
│   ├── db.ts                       # Dexie schema definition + all typed hooks
│   ├── battle/
│   │   ├── BattleEngine.ts         # Port of BattleEngine.kt
│   │   ├── DamageEngine.ts         # Port of DamageEngine.kt
│   │   ├── HeldItemEffect.ts       # Port of HeldItemEffect.kt
│   │   ├── StatConfig.ts           # Port of StatConfig.kt
│   │   └── TrainerRoster.ts        # Port of TrainerRoster.kt
│   ├── theme.tsx                   # ThemeProvider context + useTheme hook
│   └── constants.ts                # Sprite URL helpers, gen ranges, nature table
├── public/
│   └── trainers/                   # Trainer JSON assets (copied from Android app)
└── ...config files
```

---

## Theme System

Two themes share the same component tree. `data-theme="dark" | "light"` is set on `<html>`. All colours are CSS custom properties; components reference variables only — never hard-coded hex.

| Variable | Dark | Light (Parchment) |
|---|---|---|
| `--bg` | `#1a1a2e` | `#f0e8d8` |
| `--surface` | `#111111` | `#e8dcc8` |
| `--border` | `#3a3a6a` | `#c8b89a` |
| `--text` | `#e0e0e0` | `#2a2a2a` |
| `--text-muted` | `#666666` | `#888888` |
| `--gold` | `#f0c040` | `#8b4513` |
| `--blue` | `#6890f0` | `#3a5a9a` |
| `--header` | `#c0392b` | `#c0392b` (unchanged) |
| `--glow` | `#6dd5ed` | `#6dd5ed` (unchanged) |

Theme preference is persisted in IndexedDB (`settings` table, key `theme`). Default is `dark`.

---

## Data Layer (IndexedDB via Dexie.js)

All tables defined in `lib/db.ts`. No component calls Dexie directly — all access goes through typed hooks exported from the same file. The hooks are the sync-migration seam.

### Schema

```typescript
// lib/db.ts
class PokedexDB extends Dexie {
  caught_pokemon!: Table<{ pokemonId: number }>;
  team!: Table<{ slot: number; pokemonId: number }>;
  battle_config!: Table<{ slot: number; configJson: string }>;
  trainer_records!: Table<TrainerRecord>;
  wild_records!: Table<WildRecord>;
  settings!: Table<{ key: string; value: string }>;
}
```

### Tables

| Table | Primary key | Purpose | Android equivalent |
|---|---|---|---|
| `caught_pokemon` | `pokemonId` | Caught/uncaught status | `CaughtPokemonEntity` |
| `team` | `slot` (0–5) | Active team Pokémon IDs | DataStore (team preference) |
| `battle_config` | `slot` (0–5) | Persisted battle setup per slot | `BattleConfigStore` / DataStore |
| `trainer_records` | `trainerId` | W/L record per trainer | `TrainerRecord` (Room) |
| `wild_records` | `pokemonId` | W/L record per wild Pokémon | `WildRecord` (Room) |
| `settings` | `key` | Theme, gen filter, music flag | DataStore preferences |

### Hooks (exported from `lib/db.ts`)

```typescript
useCaughtPokemon(): { caught: Set<number>; toggle: (id: number) => void }
useTeam(): { teamIds: number[]; add: (id: number) => void; remove: (id: number) => void }
useBattleConfig(slot: number): { config: SlotConfig | null; save: (c: SlotConfig) => void }
useTrainerRecords(): { records: TrainerRecord[]; record: (trainer, won) => void }
useWildRecords(): { records: WildRecord[]; record: (id, name, won) => void }
useSetting(key: string, defaultValue: string): [string, (v: string) => void]
```

---

## Screen Mapping

### Home / Search (`/`)
- Full-width search input with live suggestions (debounced, calls `/api/v2/pokemon?limit=1025` cached on server)
- Styled landing section with Pokédex graphic and quick-nav links (List, Collection, Team, Battle)
- No opening animation (Android-specific hardware effect)

### Full Pokédex List (`/list`)
- Server component fetches paginated list from PokeAPI
- Client component renders virtualized scroll (react-window, `FixedSizeList`)
- Filter controls: type (multi-select), generation (1–9), caught/uncaught toggle
- Each row: sprite, name, ID, type badges, caught star indicator
- Click navigates to `/pokemon/[id]`

### My Collection (`/collection`)
- Same layout as Full List, filtered to `caught_pokemon` IDs from IndexedDB
- Caught star interactive — toggles caught status inline

### Pokémon Detail (`/pokemon/[id]`)
- Server component: fetches `PokemonDetail`, `PokemonSpecies`, evolution chain
- Sprite section: normal/shiny toggle, caught star overlay
- Type badges, base stat bars with gen-accurate colour coding
- Move table: learned moves with level/method, each move name links to `/move/[name]`
- Evolution chain with sprites, clickable to navigate
- Pokédex flavour text (latest English entry from species)
- Action row: Caught toggle, Team toggle, Compare → `/compare/[id]`, Battle → `/battle?preloadId=[id]`
- Prev / Next arrows navigate between Pokémon IDs (1–1025)

### Move Detail (`/move/[name]`)
- Server component: fetches move data
- Type badge, category (Physical/Special/Status), power, accuracy, PP, priority
- Effect text (full description)
- Pokémon learner list with sprites, sorted by ID

### Compare (`/compare/[firstId]`)
- First Pokémon loaded from route param (server component)
- Second Pokémon selected via inline search (client component)
- Side-by-side stat comparison with difference indicators (+/-)

### Team Builder (`/team`)
- 6 slot cards; empty slots show a "+" prompt
- Clicking a filled slot navigates to that Pokémon's detail
- Clicking "+" opens Pokémon search modal to fill the slot
- Battle button → `/battle`

### Battle Hub (`/battle`)
- All client-side (no server data fetching on this page)
- Tab strip: CALC | WILD | TRAIN | MATCH | LOG
- Tabs correspond 1:1 to Android tabs: DamageCalcScreen, TurnBattleScreen, TrainerSelectScreen, MatchupScreen, RecordScreen
- Battle engine logic lives in `lib/battle/` — TypeScript ports with identical logic to the Kotlin originals
- Trainer JSON assets in `public/trainers/` (same files copied from Android app)
- Optional `?preloadId=` query param preloads a Pokémon into the damage calc

### Settings (`/settings`)
- Theme toggle: Dark ↔ Light (persisted to IndexedDB)
- Generation preference: affects damage calc default gen
- Music toggle: persisted but no-op on web (carried forward for future)
- Held items sync: fetches held items from PokeAPI and caches locally

---

## Navigation

Collapsible left sidebar (`components/nav/Sidebar.tsx`):

| Viewport | State | Behaviour |
|---|---|---|
| ≥ 1024px | Full | Section labels + active indicator + POKÉDEX branding at top |
| 768px–1023px | Icon-only | Labels hidden, icons remain, tooltips on hover |
| < 768px | Hidden | Hamburger button toggles full-height overlay (`MobileMenu.tsx`) |

Sections in sidebar order: LIST → COLLECTION → TEAM → BATTLE → SETTINGS

Sidebar footer: ThemeToggle component (dark/light switch).

---

## Battle Engine Port

The following Kotlin files have no Android-specific dependencies and port directly to TypeScript:

| Kotlin source | TypeScript target |
|---|---|
| `BattleEngine.kt` | `lib/battle/BattleEngine.ts` |
| `DamageEngine.kt` | `lib/battle/DamageEngine.ts` |
| `HeldItemEffect.kt` | `lib/battle/HeldItemEffect.ts` |
| `StatConfig.kt` | `lib/battle/StatConfig.ts` |
| `TrainerRoster.kt` | `lib/battle/TrainerRoster.ts` |

Kotlin sealed classes → TypeScript discriminated unions. Kotlin `data class` → TypeScript `interface`. All battle logic is pure functions with no side effects; unit tests transfer 1:1.

---

## Testing

### Unit tests (Vitest)
`src/test/battle/` — mirrors `pokemon-battle-engine/src/test/`:
- `DamageEngine.test.ts` — stat calc, type effectiveness, crit, held item modifiers
- `BattleEngine.test.ts` — turn resolution, KO detection, team exhaustion
- `HeldItemEffect.test.ts` — item effect application
- `StatConfig.test.ts` — IV/EV/DV stat calculations, nature modifiers

### Component tests (React Testing Library)
- `TypeBadge.test.tsx` — correct colour per type
- `StatBar.test.tsx` — bar width proportional to stat value
- `PokemonCard.test.tsx` — renders name, ID, types; caught state reflected
- `DamageCalcScreen.test.tsx` — input changes update damage output

Dexie.js mocked with `fake-indexeddb` for all component tests touching IndexedDB — provides an in-memory IndexedDB implementation that works in Node.js without a browser.

---

## Deployment

**Phase 1 (initial):** Deploy to Vercel. Zero-config, free tier, auto-deploys on push to `main`. HTTPS out of the box. Set `POKEAPI_BASE_URL=https://madmaxlgndklrpokeapi.com/api/v2` as an environment variable.

**Phase 2 (migrate):** Containerise with Docker. Add `web-pokedex` service to existing home server Docker Compose stack alongside the PokeAPI services. Nginx reverse proxy handles routing and TLS. This is the natural setup for the future backend-sync iteration since both services share a host.

---

## Future: Backend Sync

When shared data between Android and web is added:
1. Add a backend service (Next.js API routes or separate service) with a database
2. Replace `lib/db.ts` hook implementations — `useCaughtPokemon`, `useTeam`, etc. — with API calls
3. Add authentication (session or token-based)
4. Update Android app to call the same API endpoints instead of local Room/DataStore
5. UI layer on both platforms requires no changes

The data model is already designed for this: IndexedDB schema matches Room schema exactly.
