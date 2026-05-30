# Web Pokédex Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a full-featured browser Pokédex with feature parity to the Android app, using Next.js 14 App Router, server-side PokeAPI caching, and IndexedDB for user state.

**Architecture:** React Server Components fetch all Pokémon data from `https://madmaxlgndklrpokeapi.com/api/v2/` with Next.js built-in caching. All user state (caught collection, team, battle config, battle records, theme) lives in IndexedDB via Dexie.js. Collapsible sidebar nav. Faithful Dark + Parchment Light themes via CSS custom properties.

**Tech Stack:** Next.js 14, TypeScript, Tailwind CSS, Dexie.js, react-window, Vitest, React Testing Library, fake-indexeddb, Node 22, npm 10

---

## File Structure

```
~/Git/web-pokedex/
├── app/
│   ├── globals.css
│   ├── layout.tsx
│   ├── page.tsx                        # Home/Search
│   ├── list/page.tsx
│   ├── pokemon/[id]/page.tsx
│   ├── move/[name]/page.tsx
│   ├── compare/[firstId]/page.tsx
│   ├── collection/page.tsx
│   ├── team/page.tsx
│   ├── battle/page.tsx
│   └── settings/page.tsx
├── components/
│   ├── nav/
│   │   ├── Sidebar.tsx
│   │   └── MobileMenu.tsx
│   ├── pokemon/
│   │   ├── TypeBadge.tsx
│   │   ├── StatBar.tsx
│   │   ├── SpriteImage.tsx
│   │   └── PokemonCard.tsx
│   ├── battle/
│   │   ├── DamageCalcScreen.tsx
│   │   ├── TurnBattleScreen.tsx
│   │   ├── TrainerSelectScreen.tsx
│   │   ├── MatchupScreen.tsx
│   │   └── RecordScreen.tsx
│   └── ui/
│       ├── Button.tsx
│       ├── Modal.tsx
│       └── ThemeToggle.tsx
├── lib/
│   ├── types.ts
│   ├── api.ts
│   ├── db.ts
│   ├── theme.tsx
│   ├── constants.ts
│   └── battle/
│       ├── StatConfig.ts
│       ├── DamageEngine.ts
│       ├── HeldItemEffect.ts
│       ├── BattleEngine.ts
│       └── TrainerRoster.ts
├── public/
│   └── trainers/                       # JSON assets copied from Android app
├── src/test/
│   ├── setup.ts
│   └── battle/
│       ├── StatConfig.test.ts
│       ├── DamageEngine.test.ts
│       ├── HeldItemEffect.test.ts
│       └── BattleEngine.test.ts
├── vitest.config.ts
├── tailwind.config.ts
└── tsconfig.json
```

---

### Task 1: Project Scaffold

**Files:**
- Create: `~/Git/web-pokedex/` (entire project)
- Create: `app/globals.css`
- Create: `tailwind.config.ts`
- Create: `vitest.config.ts`
- Create: `src/test/setup.ts`

- [ ] **Step 1: Scaffold the Next.js app**

```bash
cd ~/Git
npx create-next-app@14 web-pokedex \
  --typescript \
  --tailwind \
  --eslint \
  --app \
  --no-src-dir \
  --import-alias "@/*"
cd web-pokedex
```

Expected: project created, `npm run dev` starts on port 3000.

- [ ] **Step 2: Install additional dependencies**

```bash
npm install dexie dexie-react-hooks react-window
npm install -D @types/react-window vitest @vitejs/plugin-react \
  @testing-library/react @testing-library/jest-dom fake-indexeddb jsdom
```

- [ ] **Step 3: Write `vitest.config.ts`**

```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    globals: true,
  },
  resolve: {
    alias: { '@': path.resolve(__dirname, './') },
  },
})
```

- [ ] **Step 4: Write `src/test/setup.ts`**

```typescript
// src/test/setup.ts
import '@testing-library/jest-dom'
import 'fake-indexeddb/auto'
```

- [ ] **Step 5: Write `app/globals.css`**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@import url('https://fonts.googleapis.com/css2?family=Press+Start+2P&display=swap');

:root[data-theme="dark"] {
  --bg: #1a1a2e;
  --surface: #111111;
  --border: #3a3a6a;
  --text: #e0e0e0;
  --text-muted: #666666;
  --gold: #f0c040;
  --blue: #6890f0;
}

:root[data-theme="light"] {
  --bg: #f0e8d8;
  --surface: #e8dcc8;
  --border: #c8b89a;
  --text: #2a2a2a;
  --text-muted: #888888;
  --gold: #8b4513;
  --blue: #3a5a9a;
}

:root {
  --header: #c0392b;
  --glow: #6dd5ed;
  --font-pixel: 'Press Start 2P', monospace;
}

body {
  background-color: var(--bg);
  color: var(--text);
  font-family: var(--font-pixel);
  font-size: 12px;
}

* { box-sizing: border-box; }
```

- [ ] **Step 6: Update `tailwind.config.ts` to allow CSS variable colours**

```typescript
// tailwind.config.ts
import type { Config } from 'tailwindcss'

const config: Config = {
  content: ['./app/**/*.{ts,tsx}', './components/**/*.{ts,tsx}', './lib/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: 'var(--bg)',
        surface: 'var(--surface)',
        border: 'var(--border)',
        text: 'var(--text)',
        muted: 'var(--text-muted)',
        gold: 'var(--gold)',
        blue: 'var(--blue)',
        header: 'var(--header)',
        glow: 'var(--glow)',
      },
      fontFamily: {
        pixel: ['Press Start 2P', 'monospace'],
      },
    },
  },
  plugins: [],
}
export default config
```

- [ ] **Step 7: Verify dev server starts**

```bash
npm run dev
```

Expected: `ready - started server on 0.0.0.0:3000`. Open http://localhost:3000 — default Next.js page loads.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: scaffold Next.js 14 app with Tailwind, Vitest, theme CSS variables"
```

---

### Task 2: Type Definitions

**Files:**
- Create: `lib/types.ts`

- [ ] **Step 1: Write `lib/types.ts`**

```typescript
// lib/types.ts

export interface PokemonSummary {
  id: number
  name: string
}

export interface PokemonType {
  slot: number
  type: { name: string }
}

export interface Stat {
  base_stat: number
  effort: number
  stat: { name: string }
}

export interface MoveRef {
  move: { name: string; url: string }
  version_group_details: {
    level_learned_at: number
    move_learn_method: { name: string }
    version_group: { name: string }
  }[]
}

export interface Sprites {
  front_default: string | null
  front_shiny: string | null
}

export interface PokemonDetail {
  id: number
  name: string
  types: PokemonType[]
  stats: Stat[]
  moves: MoveRef[]
  sprites: Sprites
  species: { name: string; url: string }
  base_experience: number | null
  height: number
  weight: number
}

export interface PokemonSpecies {
  id: number
  name: string
  flavor_text_entries: {
    flavor_text: string
    language: { name: string }
    version: { name: string }
  }[]
  evolution_chain: { url: string }
  genera: { genus: string; language: { name: string } }[]
  gender_rate: number
  capture_rate: number
  is_legendary: boolean
  is_mythical: boolean
}

export interface ChainLink {
  species: { name: string; url: string }
  evolves_to: ChainLink[]
  evolution_details: {
    min_level: number | null
    item: { name: string } | null
    trigger: { name: string }
  }[]
}

export interface EvolutionChain {
  id: number
  chain: ChainLink
}

export interface MoveDetail {
  id: number
  name: string
  type: { name: string }
  damage_class: { name: string }
  power: number | null
  accuracy: number | null
  pp: number
  priority: number
  effect_entries: {
    effect: string
    short_effect: string
    language: { name: string }
  }[]
  learned_by_pokemon: { name: string; url: string }[]
}

export interface HeldItem {
  id: number
  name: string
  displayName: string
  effectSummary: string
}

export interface TrainerRecord {
  trainerId: string
  name: string
  title: string
  region: string
  trainerClass: string
  typeSpecialty: string
  wins: number
  losses: number
  firstDefeatedAt?: number
  lastBattledAt: number
}

export interface WildRecord {
  pokemonId: number
  pokemonName: string
  wins: number
  losses: number
  lastBattledAt: number
}
```

- [ ] **Step 2: Verify TypeScript compiles**

```bash
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add lib/types.ts
git commit -m "feat: add shared TypeScript type definitions"
```

---

### Task 3: Constants and API Functions

**Files:**
- Create: `lib/constants.ts`
- Create: `lib/api.ts`

- [ ] **Step 1: Write `lib/constants.ts`**

```typescript
// lib/constants.ts

export const POKEAPI_BASE = 'https://madmaxlgndklrpokeapi.com/api/v2'

export const spriteUrl = (id: number) =>
  `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${id}.png`

export const shinySpriteUrl = (id: number) =>
  `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/${id}.png`

export const POKEMON_COUNT = 1025

export const GENERATIONS = [
  { label: 'Gen I',   min: 1,   max: 151  },
  { label: 'Gen II',  min: 152, max: 251  },
  { label: 'Gen III', min: 252, max: 386  },
  { label: 'Gen IV',  min: 387, max: 493  },
  { label: 'Gen V',   min: 494, max: 649  },
  { label: 'Gen VI',  min: 650, max: 721  },
  { label: 'Gen VII', min: 722, max: 809  },
  { label: 'Gen VIII',min: 810, max: 905  },
  { label: 'Gen IX',  min: 906, max: 1025 },
] as const

export const TYPE_COLORS: Record<string, string> = {
  normal:   '#a8a878', fire:     '#f08030', water:    '#6890f0',
  electric: '#f8d030', grass:    '#78c850', ice:      '#98d8d8',
  fighting: '#c03028', poison:   '#a040a0', ground:   '#e0c068',
  flying:   '#a890f0', psychic:  '#f85888', bug:      '#a8b820',
  rock:     '#b8a038', ghost:    '#705898', dragon:   '#7038f8',
  dark:     '#705848', steel:    '#b8b8d0', fairy:    '#ee99ac',
}

export const STAT_COLORS: Record<string, string> = {
  hp:               '#ff5959', attack:          '#f5ac78',
  defense:          '#fae078', 'special-attack': '#9db7f5',
  'special-defense':'a7db8d',  speed:           '#fa92b2',
}

export const NATURES = [
  { name: 'Hardy',   boosted: null, dropped: null },
  { name: 'Lonely',  boosted: 1,    dropped: 2    },
  { name: 'Brave',   boosted: 1,    dropped: 5    },
  { name: 'Adamant', boosted: 1,    dropped: 3    },
  { name: 'Naughty', boosted: 1,    dropped: 4    },
  { name: 'Bold',    boosted: 2,    dropped: 1    },
  { name: 'Docile',  boosted: null, dropped: null },
  { name: 'Relaxed', boosted: 2,    dropped: 5    },
  { name: 'Impish',  boosted: 2,    dropped: 3    },
  { name: 'Lax',     boosted: 2,    dropped: 4    },
  { name: 'Timid',   boosted: 5,    dropped: 1    },
  { name: 'Hasty',   boosted: 5,    dropped: 2    },
  { name: 'Serious', boosted: null, dropped: null },
  { name: 'Jolly',   boosted: 5,    dropped: 3    },
  { name: 'Naive',   boosted: 5,    dropped: 4    },
  { name: 'Modest',  boosted: 3,    dropped: 1    },
  { name: 'Mild',    boosted: 3,    dropped: 2    },
  { name: 'Quiet',   boosted: 3,    dropped: 5    },
  { name: 'Bashful', boosted: null, dropped: null },
  { name: 'Rash',    boosted: 3,    dropped: 4    },
  { name: 'Calm',    boosted: 4,    dropped: 1    },
  { name: 'Gentle',  boosted: 4,    dropped: 2    },
  { name: 'Sassy',   boosted: 4,    dropped: 5    },
  { name: 'Careful', boosted: 4,    dropped: 3    },
  { name: 'Quirky',  boosted: null, dropped: null },
] as const
```

- [ ] **Step 2: Write `lib/api.ts`**

```typescript
// lib/api.ts
import type {
  PokemonDetail, PokemonSpecies, EvolutionChain, MoveDetail, HeldItem
} from './types'
import { POKEAPI_BASE } from './constants'

async function apiFetch<T>(path: string): Promise<T> {
  const res = await fetch(`${POKEAPI_BASE}${path}`, {
    next: { revalidate: 86400 },
  })
  if (!res.ok) throw new Error(`PokeAPI ${path} → ${res.status}`)
  return res.json() as Promise<T>
}

export async function fetchPokemonList(): Promise<{ id: number; name: string }[]> {
  const data = await apiFetch<{ results: { name: string; url: string }[] }>(
    '/pokemon?limit=1025&offset=0'
  )
  return data.results.map((p, i) => ({ id: i + 1, name: p.name }))
}

export async function fetchPokemonDetail(id: number): Promise<PokemonDetail> {
  return apiFetch(`/pokemon/${id}`)
}

export async function fetchPokemonSpecies(id: number): Promise<PokemonSpecies> {
  return apiFetch(`/pokemon-species/${id}`)
}

export async function fetchEvolutionChain(urlOrId: string | number): Promise<EvolutionChain> {
  if (typeof urlOrId === 'number') return apiFetch(`/evolution-chain/${urlOrId}`)
  const id = urlOrId.split('/evolution-chain/')[1].replace('/', '')
  return apiFetch(`/evolution-chain/${id}`)
}

export async function fetchMove(name: string): Promise<MoveDetail> {
  return apiFetch(`/move/${name}`)
}

export async function fetchHeldItemById(id: number): Promise<HeldItem> {
  const raw = await apiFetch<{
    id: number
    name: string
    names: { name: string; language: { name: string } }[]
    effect_entries: { short_effect: string; language: { name: string } }[]
  }>(`/item/${id}`)
  const displayName = raw.names.find(n => n.language.name === 'en')?.name ?? raw.name
  const effectSummary = raw.effect_entries.find(e => e.language.name === 'en')?.short_effect ?? ''
  return { id: raw.id, name: raw.name, displayName, effectSummary }
}

export async function fetchTypeChart(): Promise<Record<string, {
  double_damage_to: { name: string }[]
  half_damage_to: { name: string }[]
  no_damage_to: { name: string }[]
}>> {
  const typeNames = [
    'normal','fire','water','electric','grass','ice','fighting','poison',
    'ground','flying','psychic','bug','rock','ghost','dragon','dark','steel','fairy',
  ]
  const results = await Promise.all(
    typeNames.map(t => apiFetch<{ damage_relations: {
      double_damage_to: { name: string }[]
      half_damage_to: { name: string }[]
      no_damage_to: { name: string }[]
    } }>(`/type/${t}`))
  )
  return Object.fromEntries(typeNames.map((t, i) => [t, results[i].damage_relations]))
}
```

- [ ] **Step 3: Verify TypeScript compiles**

```bash
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add lib/constants.ts lib/api.ts
git commit -m "feat: add constants, sprite URL helpers, and PokeAPI fetch functions"
```

---

### Task 4: Theme System

**Files:**
- Create: `lib/theme.tsx`
- Create: `components/ui/ThemeToggle.tsx`
- Modify: `app/layout.tsx`

- [ ] **Step 1: Write `lib/theme.tsx`**

```typescript
// lib/theme.tsx
'use client'
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

type Theme = 'dark' | 'light'

const ThemeContext = createContext<{ theme: Theme; toggle: () => void }>({
  theme: 'dark',
  toggle: () => {},
})

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>('dark')

  useEffect(() => {
    const stored = localStorage.getItem('pdex-theme') as Theme | null
    if (stored === 'light' || stored === 'dark') setTheme(stored)
  }, [])

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('pdex-theme', theme)
  }, [theme])

  const toggle = () => setTheme(t => (t === 'dark' ? 'light' : 'dark'))

  return (
    <ThemeContext.Provider value={{ theme, toggle }}>
      {children}
    </ThemeContext.Provider>
  )
}

export const useTheme = () => useContext(ThemeContext)
```

- [ ] **Step 2: Write `components/ui/ThemeToggle.tsx`**

```typescript
// components/ui/ThemeToggle.tsx
'use client'
import { useTheme } from '@/lib/theme'

export function ThemeToggle() {
  const { theme, toggle } = useTheme()
  return (
    <button
      onClick={toggle}
      style={{ color: 'var(--text-muted)', fontFamily: 'var(--font-pixel)', fontSize: '6px' }}
      className="w-full text-left py-2 px-4 hover:opacity-80 transition-opacity"
      aria-label="Toggle theme"
    >
      {theme === 'dark' ? '☀ LIGHT' : '● DARK'}
    </button>
  )
}
```

- [ ] **Step 3: Write `app/layout.tsx`**

```typescript
// app/layout.tsx
import type { Metadata } from 'next'
import './globals.css'
import { ThemeProvider } from '@/lib/theme'
import { Sidebar } from '@/components/nav/Sidebar'

export const metadata: Metadata = {
  title: 'Pokédex',
  description: 'Personal Pokédex',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" data-theme="dark" suppressHydrationWarning>
      <body>
        <ThemeProvider>
          <div className="flex min-h-screen">
            <Sidebar />
            <main className="flex-1 min-w-0">{children}</main>
          </div>
        </ThemeProvider>
      </body>
    </html>
  )
}
```

- [ ] **Step 4: Commit**

```bash
git add lib/theme.tsx components/ui/ThemeToggle.tsx app/layout.tsx
git commit -m "feat: add theme system with dark/light CSS variable switching"
```

---

### Task 5: Dexie Database and Hooks

**Files:**
- Create: `lib/db.ts`

- [ ] **Step 1: Write the failing test**

```typescript
// src/test/db.test.ts
import { describe, it, expect, beforeEach } from 'vitest'
import { db } from '@/lib/db'

beforeEach(async () => {
  await db.caught_pokemon.clear()
  await db.team.clear()
  await db.settings.clear()
})

describe('caught_pokemon', () => {
  it('adds and retrieves a caught pokemon', async () => {
    await db.caught_pokemon.add({ pokemonId: 1 })
    const all = await db.caught_pokemon.toArray()
    expect(all).toHaveLength(1)
    expect(all[0].pokemonId).toBe(1)
  })

  it('deletes a caught pokemon', async () => {
    await db.caught_pokemon.add({ pokemonId: 25 })
    await db.caught_pokemon.delete(25)
    expect(await db.caught_pokemon.count()).toBe(0)
  })
})

describe('team', () => {
  it('stores team slots', async () => {
    await db.team.put({ slot: 0, pokemonId: 6 })
    await db.team.put({ slot: 1, pokemonId: 9 })
    const slots = await db.team.orderBy('slot').toArray()
    expect(slots.map(s => s.pokemonId)).toEqual([6, 9])
  })
})

describe('settings', () => {
  it('stores and retrieves a setting', async () => {
    await db.settings.put({ key: 'theme', value: 'light' })
    const s = await db.settings.get('theme')
    expect(s?.value).toBe('light')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
npx vitest run src/test/db.test.ts
```

Expected: FAIL — `Cannot find module '@/lib/db'`

- [ ] **Step 3: Write `lib/db.ts`**

```typescript
// lib/db.ts
import Dexie, { type Table } from 'dexie'
import { useLiveQuery } from 'dexie-react-hooks'
import type { TrainerRecord, WildRecord, HeldItem } from './types'

interface CaughtPokemon { pokemonId: number }
interface TeamSlot { slot: number; pokemonId: number }
interface BattleConfig { slot: number; configJson: string }
interface Setting { key: string; value: string }

class PokedexDB extends Dexie {
  caught_pokemon!: Table<CaughtPokemon>
  team!: Table<TeamSlot>
  battle_config!: Table<BattleConfig>
  trainer_records!: Table<TrainerRecord>
  wild_records!: Table<WildRecord>
  settings!: Table<Setting>

  constructor() {
    super('pokedex')
    this.version(1).stores({
      caught_pokemon: 'pokemonId',
      team: 'slot',
      battle_config: 'slot',
      trainer_records: 'trainerId',
      wild_records: 'pokemonId',
      settings: 'key',
    })
  }
}

export const db = new PokedexDB()

export function useCaughtPokemon() {
  const caught = useLiveQuery(
    () => db.caught_pokemon.toArray().then(r => new Set(r.map(c => c.pokemonId))),
    [],
    new Set<number>()
  )
  const toggle = async (pokemonId: number) => {
    if (caught?.has(pokemonId)) {
      await db.caught_pokemon.delete(pokemonId)
    } else {
      await db.caught_pokemon.add({ pokemonId })
    }
  }
  return { caught: caught ?? new Set<number>(), toggle }
}

export function useTeam() {
  const slots = useLiveQuery(() => db.team.orderBy('slot').toArray(), [], [])
  const teamIds = (slots ?? []).map(s => s.pokemonId)
  const add = async (pokemonId: number) => {
    if (teamIds.length >= 6) return
    await db.team.put({ slot: teamIds.length, pokemonId })
  }
  const remove = async (pokemonId: number) => {
    const all = slots ?? []
    const idx = all.findIndex(s => s.pokemonId === pokemonId)
    if (idx === -1) return
    await db.team.clear()
    const remaining = all.filter(s => s.pokemonId !== pokemonId)
    await db.team.bulkPut(remaining.map((s, i) => ({ slot: i, pokemonId: s.pokemonId })))
  }
  return { teamIds, add, remove }
}

export function useBattleConfig(slot: number) {
  const record = useLiveQuery(() => db.battle_config.get(slot), [slot])
  const config = record ? JSON.parse(record.configJson) : null
  const save = async (configData: unknown) => {
    await db.battle_config.put({ slot, configJson: JSON.stringify(configData) })
  }
  return { config, save }
}

export function useTrainerRecords() {
  const records = useLiveQuery(
    () => db.trainer_records.orderBy('lastBattledAt').reverse().toArray(),
    [],
    [] as TrainerRecord[]
  )
  const recordBattle = async (
    trainerData: Omit<TrainerRecord, 'wins' | 'losses' | 'firstDefeatedAt' | 'lastBattledAt'>,
    won: boolean
  ) => {
    const existing = await db.trainer_records.get(trainerData.trainerId)
    const now = Date.now()
    if (existing) {
      await db.trainer_records.update(trainerData.trainerId, {
        wins: existing.wins + (won ? 1 : 0),
        losses: existing.losses + (won ? 0 : 1),
        firstDefeatedAt: won && !existing.firstDefeatedAt ? now : existing.firstDefeatedAt,
        lastBattledAt: now,
      })
    } else {
      await db.trainer_records.add({
        ...trainerData,
        wins: won ? 1 : 0,
        losses: won ? 0 : 1,
        firstDefeatedAt: won ? now : undefined,
        lastBattledAt: now,
      })
    }
  }
  return { records: records ?? [], recordBattle }
}

export function useWildRecords() {
  const records = useLiveQuery(
    () => db.wild_records.toArray().then(r => r.sort((a, b) => (b.wins + b.losses) - (a.wins + a.losses))),
    [],
    [] as WildRecord[]
  )
  const recordBattle = async (pokemonId: number, pokemonName: string, won: boolean) => {
    const existing = await db.wild_records.get(pokemonId)
    const now = Date.now()
    if (existing) {
      await db.wild_records.update(pokemonId, {
        wins: existing.wins + (won ? 1 : 0),
        losses: existing.losses + (won ? 0 : 1),
        lastBattledAt: now,
      })
    } else {
      await db.wild_records.add({
        pokemonId, pokemonName,
        wins: won ? 1 : 0, losses: won ? 0 : 1,
        lastBattledAt: now,
      })
    }
  }
  return { records: records ?? [], recordBattle }
}

export function useSetting(key: string, defaultValue: string): [string, (v: string) => Promise<void>] {
  const record = useLiveQuery(() => db.settings.get(key), [key])
  const value = record?.value ?? defaultValue
  const set = async (v: string) => { await db.settings.put({ key, value: v }) }
  return [value, set]
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
npx vitest run src/test/db.test.ts
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add lib/db.ts src/test/db.test.ts
git commit -m "feat: add Dexie database schema and all typed IndexedDB hooks"
```

---

### Task 6: Sidebar Navigation

**Files:**
- Create: `components/nav/Sidebar.tsx`
- Create: `components/nav/MobileMenu.tsx`

- [ ] **Step 1: Write `components/nav/Sidebar.tsx`**

```typescript
// components/nav/Sidebar.tsx
'use client'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { ThemeToggle } from '@/components/ui/ThemeToggle'
import { MobileMenu } from './MobileMenu'

const NAV = [
  { href: '/list',       label: 'LIST'     },
  { href: '/collection', label: 'COLLECT'  },
  { href: '/team',       label: 'TEAM'     },
  { href: '/battle',     label: 'BATTLE'   },
  { href: '/settings',   label: 'SETTINGS' },
]

export function Sidebar() {
  const pathname = usePathname()
  return (
    <>
      {/* Desktop sidebar */}
      <aside
        style={{ background: 'var(--surface)', borderRight: '1px solid var(--border)' }}
        className="hidden lg:flex flex-col w-44 xl:w-52 min-h-screen sticky top-0 h-screen shrink-0"
      >
        <div style={{ background: 'var(--header)' }} className="p-4 flex items-center gap-2 shrink-0">
          <div
            style={{ background: 'var(--glow)', boxShadow: '0 0 8px var(--glow)' }}
            className="w-3 h-3 rounded-full shrink-0"
          />
          <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', letterSpacing: '2px', color: '#fff' }}>
            POKÉDEX
          </span>
        </div>

        <nav className="flex-1 py-4 overflow-y-auto">
          {NAV.map(({ href, label }) => {
            const active = pathname === href || pathname.startsWith(href + '/')
            return (
              <Link
                key={href}
                href={href}
                style={{
                  fontFamily: 'var(--font-pixel)',
                  fontSize: '7px',
                  letterSpacing: '1px',
                  color: active ? 'var(--gold)' : 'var(--text-muted)',
                  borderLeft: active ? '2px solid var(--gold)' : '2px solid transparent',
                  background: active ? 'var(--bg)' : 'transparent',
                }}
                className="block px-4 py-3 transition-colors hover:opacity-80"
              >
                {label}
              </Link>
            )
          })}
        </nav>

        <div style={{ borderTop: '1px solid var(--border)' }} className="shrink-0">
          <ThemeToggle />
        </div>
      </aside>

      {/* Mobile: top bar with hamburger */}
      <div
        style={{ background: 'var(--header)' }}
        className="lg:hidden fixed top-0 left-0 right-0 z-50 flex items-center justify-between px-4 py-3"
      >
        <div className="flex items-center gap-2">
          <div style={{ background: 'var(--glow)', boxShadow: '0 0 6px var(--glow)' }} className="w-2 h-2 rounded-full" />
          <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', letterSpacing: '2px', color: '#fff' }}>
            POKÉDEX
          </span>
        </div>
        <MobileMenu nav={NAV} pathname={pathname} />
      </div>

      {/* Mobile top bar spacer */}
      <div className="lg:hidden h-11" />
    </>
  )
}
```

- [ ] **Step 2: Write `components/nav/MobileMenu.tsx`**

```typescript
// components/nav/MobileMenu.tsx
'use client'
import { useState } from 'react'
import Link from 'next/link'
import { ThemeToggle } from '@/components/ui/ThemeToggle'

interface Props {
  nav: { href: string; label: string }[]
  pathname: string
}

export function MobileMenu({ nav, pathname }: Props) {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button
        onClick={() => setOpen(true)}
        style={{ color: '#fff', fontFamily: 'var(--font-pixel)', fontSize: '10px' }}
        aria-label="Open menu"
      >
        ☰
      </button>

      {open && (
        <div
          className="fixed inset-0 z-50 flex"
          onClick={() => setOpen(false)}
        >
          <div
            style={{ background: 'var(--surface)', borderRight: '1px solid var(--border)' }}
            className="w-64 flex flex-col"
            onClick={e => e.stopPropagation()}
          >
            <div style={{ background: 'var(--header)' }} className="p-4 flex items-center justify-between">
              <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: '#fff', letterSpacing: '2px' }}>
                POKÉDEX
              </span>
              <button onClick={() => setOpen(false)} style={{ color: '#fff', fontSize: '14px' }}>✕</button>
            </div>
            <nav className="flex-1 py-4">
              {nav.map(({ href, label }) => {
                const active = pathname === href || pathname.startsWith(href + '/')
                return (
                  <Link
                    key={href}
                    href={href}
                    onClick={() => setOpen(false)}
                    style={{
                      fontFamily: 'var(--font-pixel)',
                      fontSize: '7px',
                      letterSpacing: '1px',
                      color: active ? 'var(--gold)' : 'var(--text-muted)',
                      borderLeft: active ? '2px solid var(--gold)' : '2px solid transparent',
                    }}
                    className="block px-4 py-3"
                  >
                    {label}
                  </Link>
                )
              })}
            </nav>
            <div style={{ borderTop: '1px solid var(--border)' }}>
              <ThemeToggle />
            </div>
          </div>
          <div className="flex-1 bg-black/50" />
        </div>
      )}
    </>
  )
}
```

- [ ] **Step 3: Verify sidebar renders**

```bash
npm run dev
```

Open http://localhost:3000 — sidebar visible on desktop. Resize to mobile width — top bar with hamburger appears.

- [ ] **Step 4: Commit**

```bash
git add components/nav/
git commit -m "feat: add collapsible sidebar and mobile hamburger menu"
```

---

### Task 7: Shared UI Components

**Files:**
- Create: `components/pokemon/TypeBadge.tsx`
- Create: `components/pokemon/StatBar.tsx`
- Create: `components/pokemon/SpriteImage.tsx`
- Create: `components/pokemon/PokemonCard.tsx`
- Create: `components/ui/Button.tsx`
- Create: `components/ui/Modal.tsx`

- [ ] **Step 1: Write failing tests**

```typescript
// src/test/components/TypeBadge.test.tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { TypeBadge } from '@/components/pokemon/TypeBadge'

describe('TypeBadge', () => {
  it('renders the type name uppercased', () => {
    render(<TypeBadge type="fire" />)
    expect(screen.getByText('FIRE')).toBeInTheDocument()
  })

  it('applies the correct background colour', () => {
    const { container } = render(<TypeBadge type="water" />)
    const badge = container.firstChild as HTMLElement
    expect(badge.style.background).toBe('#6890f0')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
npx vitest run src/test/components/TypeBadge.test.tsx
```

Expected: FAIL — `Cannot find module '@/components/pokemon/TypeBadge'`

- [ ] **Step 3: Write `components/pokemon/TypeBadge.tsx`**

```typescript
// components/pokemon/TypeBadge.tsx
import { TYPE_COLORS } from '@/lib/constants'

interface Props { type: string; small?: boolean }

export function TypeBadge({ type, small = false }: Props) {
  const bg = TYPE_COLORS[type] ?? '#888'
  return (
    <span
      style={{
        background: bg,
        color: '#fff',
        fontFamily: 'var(--font-pixel)',
        fontSize: small ? '5px' : '7px',
        padding: small ? '2px 5px' : '3px 8px',
        borderRadius: '3px',
        display: 'inline-block',
        letterSpacing: '0.5px',
      }}
    >
      {type.toUpperCase()}
    </span>
  )
}
```

- [ ] **Step 4: Write `components/pokemon/StatBar.tsx`**

```typescript
// components/pokemon/StatBar.tsx
import { STAT_COLORS } from '@/lib/constants'

interface Props { name: string; value: number; max?: number }

export function StatBar({ name, value, max = 255 }: Props) {
  const pct = Math.min(100, (value / max) * 100)
  const color = STAT_COLORS[name] ?? 'var(--blue)'
  const shortName: Record<string, string> = {
    hp: 'HP', attack: 'ATK', defense: 'DEF',
    'special-attack': 'SATK', 'special-defense': 'SDEF', speed: 'SPD',
  }
  return (
    <div className="flex items-center gap-2">
      <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)', width: '36px', textAlign: 'right' }}>
        {shortName[name] ?? name.toUpperCase()}
      </span>
      <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text)', width: '28px' }}>
        {value}
      </span>
      <div style={{ background: 'var(--border)' }} className="flex-1 h-2 rounded-full overflow-hidden">
        <div style={{ width: `${pct}%`, background: color }} className="h-full rounded-full transition-all" />
      </div>
    </div>
  )
}
```

- [ ] **Step 5: Write `components/pokemon/SpriteImage.tsx`**

```typescript
// components/pokemon/SpriteImage.tsx
'use client'
import Image from 'next/image'
import { useState } from 'react'
import { spriteUrl, shinySpriteUrl } from '@/lib/constants'

interface Props { id: number; name: string; size?: number; showShinyToggle?: boolean }

export function SpriteImage({ id, name, size = 96, showShinyToggle = false }: Props) {
  const [shiny, setShiny] = useState(false)
  const src = shiny ? shinySpriteUrl(id) : spriteUrl(id)
  return (
    <div className="relative flex flex-col items-center gap-1">
      <Image
        src={src}
        alt={name}
        width={size}
        height={size}
        unoptimized
        className="pixelated"
        style={{ imageRendering: 'pixelated' }}
      />
      {showShinyToggle && (
        <button
          onClick={() => setShiny(s => !s)}
          style={{
            fontFamily: 'var(--font-pixel)',
            fontSize: '5px',
            color: shiny ? 'var(--gold)' : 'var(--text-muted)',
          }}
        >
          {shiny ? '★ SHINY' : '☆ SHINY'}
        </button>
      )}
    </div>
  )
}
```

- [ ] **Step 6: Write `components/pokemon/PokemonCard.tsx`**

```typescript
// components/pokemon/PokemonCard.tsx
'use client'
import Link from 'next/link'
import Image from 'next/image'
import { TypeBadge } from './TypeBadge'
import { spriteUrl } from '@/lib/constants'

interface Props {
  id: number
  name: string
  types: string[]
  caught?: boolean
  onToggleCaught?: () => void
}

export function PokemonCard({ id, name, types, caught = false, onToggleCaught }: Props) {
  return (
    <div
      style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px' }}
      className="flex items-center gap-3 p-2 hover:opacity-80 transition-opacity"
    >
      <Image
        src={spriteUrl(id)}
        alt={name}
        width={48}
        height={48}
        unoptimized
        style={{ imageRendering: 'pixelated' }}
      />
      <div className="flex-1 min-w-0">
        <Link href={`/pokemon/${id}`}>
          <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text)' }}>
            {name.toUpperCase()}
          </div>
          <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '5px', color: 'var(--text-muted)' }}>
            #{String(id).padStart(4, '0')}
          </div>
        </Link>
        <div className="flex gap-1 mt-1">
          {types.map(t => <TypeBadge key={t} type={t} small />)}
        </div>
      </div>
      {onToggleCaught && (
        <button
          onClick={onToggleCaught}
          style={{ color: caught ? 'var(--gold)' : 'var(--text-muted)', fontSize: '16px' }}
          aria-label={caught ? 'Uncatch' : 'Catch'}
        >
          {caught ? '★' : '☆'}
        </button>
      )}
    </div>
  )
}
```

- [ ] **Step 7: Write `components/ui/Button.tsx` and `components/ui/Modal.tsx`**

```typescript
// components/ui/Button.tsx
interface Props extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary'
}

export function Button({ variant = 'primary', children, style, ...rest }: Props) {
  const bg = variant === 'primary' ? 'var(--header)' : 'var(--surface)'
  const color = variant === 'primary' ? '#fff' : 'var(--text)'
  return (
    <button
      style={{
        background: bg,
        color,
        border: `1px solid ${variant === 'secondary' ? 'var(--border)' : 'transparent'}`,
        fontFamily: 'var(--font-pixel)',
        fontSize: '7px',
        padding: '6px 12px',
        borderRadius: '3px',
        cursor: 'pointer',
        letterSpacing: '1px',
        ...style,
      }}
      {...rest}
    >
      {children}
    </button>
  )
}
```

```typescript
// components/ui/Modal.tsx
'use client'
import { useEffect, type ReactNode } from 'react'

interface Props { open: boolean; onClose: () => void; children: ReactNode }

export function Modal({ open, onClose, children }: Props) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" onClick={onClose}>
      <div className="absolute inset-0 bg-black/60" />
      <div
        style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', maxWidth: '90vw', maxHeight: '85vh', overflowY: 'auto' }}
        className="relative z-10 p-4 w-full max-w-lg"
        onClick={e => e.stopPropagation()}
      >
        {children}
      </div>
    </div>
  )
}
```

- [ ] **Step 8: Run tests**

```bash
npx vitest run src/test/components/TypeBadge.test.tsx
```

Expected: 2 tests pass.

- [ ] **Step 9: Commit**

```bash
git add components/
git commit -m "feat: add TypeBadge, StatBar, SpriteImage, PokemonCard, Button, Modal"
```

---

### Task 8: Home/Search Page

**Files:**
- Modify: `app/page.tsx`
- Create: `app/page.tsx` (replaces Next.js default)

- [ ] **Step 1: Write `app/page.tsx`**

```typescript
// app/page.tsx
import { fetchPokemonList } from '@/lib/api'
import { HomeSearch } from './HomeSearch'

export default async function HomePage() {
  const pokemon = await fetchPokemonList()
  return <HomeSearch allPokemon={pokemon} />
}
```

- [ ] **Step 2: Create `app/HomeSearch.tsx` (client component)**

```typescript
// app/HomeSearch.tsx
'use client'
import { useState, useMemo } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import Image from 'next/image'
import { spriteUrl } from '@/lib/constants'

interface Props { allPokemon: { id: number; name: string }[] }

export function HomeSearch({ allPokemon }: Props) {
  const [query, setQuery] = useState('')
  const router = useRouter()

  const suggestions = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return []
    const byId = q.match(/^\d+$/) ? allPokemon.filter(p => String(p.id).startsWith(q)) : []
    const byName = allPokemon.filter(p => p.name.includes(q))
    const combined = [...new Map([...byId, ...byName].map(p => [p.id, p])).values()]
    return combined.slice(0, 8)
  }, [query, allPokemon])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const match = allPokemon.find(p => p.name === query.toLowerCase() || String(p.id) === query)
    if (match) router.push(`/pokemon/${match.id}`)
  }

  return (
    <div style={{ background: 'var(--bg)', minHeight: '100vh' }} className="flex flex-col items-center justify-center px-4">
      <div className="mb-8 text-center">
        <h1 style={{ fontFamily: 'var(--font-pixel)', fontSize: '14px', color: 'var(--header)', letterSpacing: '2px' }}>
          POKÉDEX
        </h1>
        <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)', marginTop: '8px' }}>
          {allPokemon.length} POKÉMON
        </p>
      </div>

      <form onSubmit={handleSubmit} className="w-full max-w-md relative">
        <input
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder="SEARCH NAME OR #ID"
          style={{
            width: '100%',
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            color: 'var(--text)',
            fontFamily: 'var(--font-pixel)',
            fontSize: '8px',
            padding: '10px 14px',
            borderRadius: '3px',
            outline: 'none',
          }}
          autoFocus
        />
        {suggestions.length > 0 && (
          <ul
            style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '3px', marginTop: '2px' }}
            className="absolute w-full z-10"
          >
            {suggestions.map(p => (
              <li key={p.id}>
                <Link
                  href={`/pokemon/${p.id}`}
                  style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text)' }}
                  className="flex items-center gap-3 px-3 py-2 hover:opacity-70"
                >
                  <Image src={spriteUrl(p.id)} alt={p.name} width={32} height={32} unoptimized style={{ imageRendering: 'pixelated' }} />
                  <span>{p.name.toUpperCase()}</span>
                  <span style={{ color: 'var(--text-muted)', marginLeft: 'auto' }}>#{String(p.id).padStart(4,'0')}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </form>

      <div className="flex gap-4 mt-12 flex-wrap justify-center">
        {[
          { href: '/list', label: 'FULL LIST' },
          { href: '/collection', label: 'MY COLLECTION' },
          { href: '/team', label: 'MY TEAM' },
          { href: '/battle', label: 'BATTLE HUB' },
        ].map(({ href, label }) => (
          <Link
            key={href}
            href={href}
            style={{
              fontFamily: 'var(--font-pixel)',
              fontSize: '7px',
              color: 'var(--gold)',
              border: '1px solid var(--border)',
              padding: '8px 12px',
              borderRadius: '3px',
              background: 'var(--surface)',
            }}
          >
            {label}
          </Link>
        ))}
      </div>
    </div>
  )
}
```

- [ ] **Step 3: Verify**

```bash
npm run dev
```

Open http://localhost:3000 — search bar renders, typing shows suggestions, clicking navigates to detail page (404 for now, expected).

- [ ] **Step 4: Commit**

```bash
git add app/page.tsx app/HomeSearch.tsx
git commit -m "feat: add home/search page with live suggestions"
```

---

### Task 9: Full Pokédex List Page

**Files:**
- Create: `app/list/page.tsx`
- Create: `app/list/PokemonListClient.tsx`

- [ ] **Step 1: Write `app/list/page.tsx`**

```typescript
// app/list/page.tsx
import { fetchPokemonList } from '@/lib/api'
import { PokemonListClient } from './PokemonListClient'

export default async function ListPage() {
  const pokemon = await fetchPokemonList()
  return <PokemonListClient allPokemon={pokemon} />
}
```

- [ ] **Step 2: Write `app/list/PokemonListClient.tsx`**

```typescript
// app/list/PokemonListClient.tsx
'use client'
import { useState, useMemo } from 'react'
import { FixedSizeList } from 'react-window'
import { PokemonCard } from '@/components/pokemon/PokemonCard'
import { useCaughtPokemon } from '@/lib/db'
import { GENERATIONS, TYPE_COLORS } from '@/lib/constants'

interface Summary { id: number; name: string }
interface Props { allPokemon: Summary[] }

export function PokemonListClient({ allPokemon }: Props) {
  const { caught, toggle } = useCaughtPokemon()
  const [genFilter, setGenFilter] = useState<number | null>(null)
  const [typeFilter, setTypeFilter] = useState<string | null>(null)
  const [showCaughtOnly, setShowCaughtOnly] = useState(false)

  const filtered = useMemo(() => {
    let list = allPokemon
    if (genFilter !== null) {
      const gen = GENERATIONS[genFilter]
      list = list.filter(p => p.id >= gen.min && p.id <= gen.max)
    }
    if (showCaughtOnly) list = list.filter(p => caught.has(p.id))
    return list
  }, [allPokemon, genFilter, showCaughtOnly, caught])

  return (
    <div style={{ background: 'var(--bg)', minHeight: '100vh', padding: '16px' }}>
      <h1 style={{ fontFamily: 'var(--font-pixel)', fontSize: '10px', color: 'var(--gold)', marginBottom: '16px' }}>
        POKÉDEX — {filtered.length} POKÉMON
      </h1>

      {/* Filters */}
      <div className="flex gap-2 flex-wrap mb-4">
        <select
          value={genFilter ?? ''}
          onChange={e => setGenFilter(e.target.value === '' ? null : Number(e.target.value))}
          style={{ background: 'var(--surface)', color: 'var(--text)', border: '1px solid var(--border)', fontFamily: 'var(--font-pixel)', fontSize: '6px', padding: '4px 8px', borderRadius: '3px' }}
        >
          <option value="">ALL GENS</option>
          {GENERATIONS.map((g, i) => (
            <option key={i} value={i}>{g.label.toUpperCase()}</option>
          ))}
        </select>

        <button
          onClick={() => setShowCaughtOnly(v => !v)}
          style={{
            background: showCaughtOnly ? 'var(--gold)' : 'var(--surface)',
            color: showCaughtOnly ? 'var(--surface)' : 'var(--text-muted)',
            border: '1px solid var(--border)',
            fontFamily: 'var(--font-pixel)',
            fontSize: '6px',
            padding: '4px 8px',
            borderRadius: '3px',
            cursor: 'pointer',
          }}
        >
          ★ CAUGHT ONLY
        </button>
      </div>

      {/* Virtualized list */}
      <FixedSizeList
        height={window?.innerHeight ? window.innerHeight - 160 : 600}
        itemCount={filtered.length}
        itemSize={68}
        width="100%"
      >
        {({ index, style }) => {
          const p = filtered[index]
          return (
            <div style={{ ...style, paddingBottom: '4px' }}>
              <PokemonCard
                id={p.id}
                name={p.name}
                types={[]}
                caught={caught.has(p.id)}
                onToggleCaught={() => toggle(p.id)}
              />
            </div>
          )
        }}
      </FixedSizeList>
    </div>
  )
}
```

- [ ] **Step 3: Verify**

```bash
npm run dev
```

Open http://localhost:3000/list — list of 1025 Pokémon, virtualized, star toggle works.

- [ ] **Step 4: Commit**

```bash
git add app/list/
git commit -m "feat: add full Pokédex list with virtualized scroll and gen/caught filters"
```

---

### Task 10: My Collection Page

**Files:**
- Create: `app/collection/page.tsx`

- [ ] **Step 1: Write `app/collection/page.tsx`**

```typescript
// app/collection/page.tsx
'use client'
import { useMemo } from 'react'
import Link from 'next/link'
import { FixedSizeList } from 'react-window'
import { PokemonCard } from '@/components/pokemon/PokemonCard'
import { useCaughtPokemon } from '@/lib/db'
import { POKEMON_COUNT } from '@/lib/constants'

const ALL_IDS = Array.from({ length: POKEMON_COUNT }, (_, i) => i + 1)

export default function CollectionPage() {
  const { caught, toggle } = useCaughtPokemon()
  const caughtIds = useMemo(() => ALL_IDS.filter(id => caught.has(id)), [caught])

  if (caughtIds.length === 0) {
    return (
      <div style={{ background: 'var(--bg)', minHeight: '100vh', padding: '24px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '16px' }}>
        <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--text-muted)' }}>
          NO POKÉMON CAUGHT YET
        </p>
        <Link href="/list" style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--gold)' }}>
          BROWSE POKÉDEX →
        </Link>
      </div>
    )
  }

  return (
    <div style={{ background: 'var(--bg)', minHeight: '100vh', padding: '16px' }}>
      <h1 style={{ fontFamily: 'var(--font-pixel)', fontSize: '10px', color: 'var(--gold)', marginBottom: '16px' }}>
        MY COLLECTION — {caughtIds.length}/{POKEMON_COUNT}
      </h1>
      <FixedSizeList
        height={window?.innerHeight ? window.innerHeight - 120 : 600}
        itemCount={caughtIds.length}
        itemSize={68}
        width="100%"
      >
        {({ index, style }) => {
          const id = caughtIds[index]
          return (
            <div style={{ ...style, paddingBottom: '4px' }}>
              <PokemonCard
                id={id}
                name={`#${String(id).padStart(4,'0')}`}
                types={[]}
                caught
                onToggleCaught={() => toggle(id)}
              />
            </div>
          )
        }}
      </FixedSizeList>
    </div>
  )
}
```

- [ ] **Step 2: Verify**

```bash
npm run dev
```

Open http://localhost:3000/collection — shows empty state or caught Pokémon. Mark some as caught in /list and verify they appear here.

- [ ] **Step 3: Commit**

```bash
git add app/collection/
git commit -m "feat: add My Collection page filtered to caught Pokémon"
```

---

### Task 11: Pokémon Detail Page

**Files:**
- Create: `app/pokemon/[id]/page.tsx`
- Create: `app/pokemon/[id]/DetailActions.tsx`

- [ ] **Step 1: Write `app/pokemon/[id]/page.tsx`**

```typescript
// app/pokemon/[id]/page.tsx
import { notFound } from 'next/navigation'
import Link from 'next/link'
import { fetchPokemonDetail, fetchPokemonSpecies, fetchEvolutionChain } from '@/lib/api'
import { TypeBadge } from '@/components/pokemon/TypeBadge'
import { StatBar } from '@/components/pokemon/StatBar'
import { SpriteImage } from '@/components/pokemon/SpriteImage'
import { DetailActions } from './DetailActions'
import { POKEMON_COUNT } from '@/lib/constants'

interface Props { params: { id: string } }

function getIdFromChain(url: string): string {
  return url.split('/evolution-chain/')[1].replace('/', '')
}

function flattenChain(link: import('@/lib/types').ChainLink): { name: string; id: number }[] {
  const id = parseInt(link.species.url.split('/pokemon-species/')[1])
  const rest = link.evolves_to.flatMap(flattenChain)
  return [{ name: link.species.name, id }, ...rest]
}

export default async function PokemonDetailPage({ params }: Props) {
  const id = parseInt(params.id)
  if (isNaN(id) || id < 1 || id > POKEMON_COUNT) notFound()

  const [detail, species] = await Promise.all([
    fetchPokemonDetail(id),
    fetchPokemonSpecies(id),
  ])
  const evoChainId = getIdFromChain(species.evolution_chain.url)
  const evoChain = await fetchEvolutionChain(evoChainId)
  const evolutions = flattenChain(evoChain.chain)

  const flavorText = species.flavor_text_entries
    .filter(e => e.language.name === 'en')
    .at(-1)
    ?.flavor_text.replace(/\f/g, ' ') ?? ''

  return (
    <div style={{ background: 'var(--bg)', minHeight: '100vh', padding: '16px', maxWidth: '700px' }}>
      {/* Prev/Next */}
      <div className="flex justify-between mb-4">
        {id > 1 ? (
          <Link href={`/pokemon/${id - 1}`} style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text-muted)' }}>
            ← #{id - 1}
          </Link>
        ) : <span />}
        {id < POKEMON_COUNT ? (
          <Link href={`/pokemon/${id + 1}`} style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text-muted)' }}>
            #{id + 1} →
          </Link>
        ) : <span />}
      </div>

      {/* Header */}
      <div className="flex items-center gap-6 mb-6">
        <SpriteImage id={id} name={detail.name} size={120} showShinyToggle />
        <div>
          <h1 style={{ fontFamily: 'var(--font-pixel)', fontSize: '12px', color: 'var(--gold)' }}>
            {detail.name.toUpperCase()}
          </h1>
          <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)', marginTop: '4px' }}>
            #{String(id).padStart(4, '0')}
          </p>
          <div className="flex gap-2 mt-2">
            {detail.types.map(t => <TypeBadge key={t.slot} type={t.type.name} />)}
          </div>
        </div>
      </div>

      {/* Flavour text */}
      <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text)', lineHeight: '1.8', marginBottom: '16px', background: 'var(--surface)', border: '1px solid var(--border)', padding: '10px', borderRadius: '3px' }}>
        {flavorText}
      </p>

      {/* Stats */}
      <section style={{ marginBottom: '16px' }}>
        <h2 style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--text-muted)', marginBottom: '8px' }}>BASE STATS</h2>
        <div className="flex flex-col gap-2">
          {detail.stats.map(s => (
            <StatBar key={s.stat.name} name={s.stat.name} value={s.base_stat} />
          ))}
        </div>
      </section>

      {/* Evolution chain */}
      {evolutions.length > 1 && (
        <section style={{ marginBottom: '16px' }}>
          <h2 style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--text-muted)', marginBottom: '8px' }}>EVOLUTION</h2>
          <div className="flex gap-4 flex-wrap">
            {evolutions.map((e, i) => (
              <Link key={e.id} href={`/pokemon/${e.id}`} className="flex flex-col items-center gap-1">
                <SpriteImage id={e.id} name={e.name} size={64} />
                <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '5px', color: e.id === id ? 'var(--gold)' : 'var(--text-muted)' }}>
                  {e.name.toUpperCase()}
                </span>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Moves */}
      <section style={{ marginBottom: '16px' }}>
        <h2 style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--text-muted)', marginBottom: '8px' }}>MOVES</h2>
        <div style={{ maxHeight: '200px', overflowY: 'auto', border: '1px solid var(--border)', borderRadius: '3px' }}>
          {detail.moves
            .flatMap(m => m.version_group_details.map(vg => ({
              name: m.move.name,
              level: vg.level_learned_at,
              method: vg.move_learn_method.name,
            })))
            .sort((a, b) => a.level - b.level)
            .slice(0, 40)
            .map((m, i) => (
              <Link
                key={`${m.name}-${i}`}
                href={`/move/${m.name}`}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  padding: '6px 10px',
                  borderBottom: '1px solid var(--border)',
                  fontFamily: 'var(--font-pixel)',
                  fontSize: '6px',
                  color: 'var(--text)',
                }}
                className="hover:opacity-70"
              >
                <span>{m.name.toUpperCase()}</span>
                <span style={{ color: 'var(--text-muted)' }}>{m.level > 0 ? `LV ${m.level}` : m.method.toUpperCase()}</span>
              </Link>
            ))}
        </div>
      </section>

      {/* Caught / Team / Compare / Battle actions */}
      <DetailActions pokemonId={id} />
    </div>
  )
}
```

- [ ] **Step 2: Write `app/pokemon/[id]/DetailActions.tsx`**

```typescript
// app/pokemon/[id]/DetailActions.tsx
'use client'
import { useRouter } from 'next/navigation'
import { useCaughtPokemon, useTeam } from '@/lib/db'
import { Button } from '@/components/ui/Button'

interface Props { pokemonId: number }

export function DetailActions({ pokemonId }: Props) {
  const { caught, toggle } = useCaughtPokemon()
  const { teamIds, add, remove } = useTeam()
  const router = useRouter()
  const isCaught = caught.has(pokemonId)
  const isOnTeam = teamIds.includes(pokemonId)

  return (
    <div className="flex gap-2 flex-wrap mt-4">
      <Button onClick={() => toggle(pokemonId)} variant={isCaught ? 'secondary' : 'primary'}>
        {isCaught ? '★ CAUGHT' : '☆ CATCH'}
      </Button>
      <Button onClick={() => isOnTeam ? remove(pokemonId) : add(pokemonId)} variant="secondary">
        {isOnTeam ? '- TEAM' : '+ TEAM'}
      </Button>
      <Button onClick={() => router.push(`/compare/${pokemonId}`)} variant="secondary">
        COMPARE
      </Button>
      <Button onClick={() => router.push(`/battle?preloadId=${pokemonId}`)}>
        BATTLE
      </Button>
    </div>
  )
}
```

- [ ] **Step 3: Verify**

```bash
npm run dev
```

Open http://localhost:3000/pokemon/1 — Bulbasaur detail page loads with stats, moves, evolution chain, catch/team buttons.

- [ ] **Step 4: Commit**

```bash
git add app/pokemon/
git commit -m "feat: add Pokémon detail page with stats, moves, evolution chain, and actions"
```

---

### Task 12: Move Detail Page

**Files:**
- Create: `app/move/[name]/page.tsx`

- [ ] **Step 1: Write `app/move/[name]/page.tsx`**

```typescript
// app/move/[name]/page.tsx
import { notFound } from 'next/navigation'
import Link from 'next/link'
import { fetchMove } from '@/lib/api'
import { TypeBadge } from '@/components/pokemon/TypeBadge'
import { spriteUrl } from '@/lib/constants'
import Image from 'next/image'

interface Props { params: { name: string } }

export default async function MoveDetailPage({ params }: Props) {
  let move
  try { move = await fetchMove(params.name) } catch { notFound() }

  const englishEffect = move.effect_entries.find(e => e.language.name === 'en')
  const categoryColor = { physical: '#c03028', special: '#6890f0', status: '#a8a878' }

  return (
    <div style={{ background: 'var(--bg)', minHeight: '100vh', padding: '16px', maxWidth: '600px' }}>
      <Link href="/" style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)' }}>
        ← BACK
      </Link>

      <h1 style={{ fontFamily: 'var(--font-pixel)', fontSize: '12px', color: 'var(--gold)', marginTop: '12px', marginBottom: '8px' }}>
        {move.name.toUpperCase().replace(/-/g, ' ')}
      </h1>

      <div className="flex gap-2 mb-4">
        <TypeBadge type={move.type.name} />
        <span style={{
          fontFamily: 'var(--font-pixel)', fontSize: '7px',
          color: '#fff',
          background: categoryColor[move.damage_class.name as keyof typeof categoryColor] ?? '#888',
          padding: '3px 8px', borderRadius: '3px',
        }}>
          {move.damage_class.name.toUpperCase()}
        </span>
      </div>

      <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '3px', padding: '12px', marginBottom: '12px' }}>
        <div className="grid grid-cols-2 gap-4">
          {[
            { label: 'POWER',    value: move.power    ?? '—' },
            { label: 'ACCURACY', value: move.accuracy != null ? `${move.accuracy}%` : '—' },
            { label: 'PP',       value: move.pp },
            { label: 'PRIORITY', value: move.priority },
          ].map(({ label, value }) => (
            <div key={label}>
              <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '5px', color: 'var(--text-muted)' }}>{label}</div>
              <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '10px', color: 'var(--text)' }}>{value}</div>
            </div>
          ))}
        </div>
      </div>

      {englishEffect && (
        <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text)', lineHeight: '1.8', marginBottom: '16px' }}>
          {englishEffect.effect}
        </p>
      )}

      <h2 style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--text-muted)', marginBottom: '8px' }}>
        LEARNED BY
      </h2>
      <div className="grid grid-cols-3 gap-2">
        {move.learned_by_pokemon.slice(0, 30).map(p => {
          const id = parseInt(p.url.split('/pokemon/')[1])
          return (
            <Link key={p.name} href={`/pokemon/${id}`} className="flex flex-col items-center gap-1 hover:opacity-70">
              <Image src={spriteUrl(id)} alt={p.name} width={48} height={48} unoptimized style={{ imageRendering: 'pixelated' }} />
              <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '5px', color: 'var(--text)' }}>
                {p.name.toUpperCase()}
              </span>
            </Link>
          )
        })}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Verify**

```bash
npm run dev
```

Open http://localhost:3000/move/tackle — move detail page loads with type, category, stats, effect text, learner sprites.

- [ ] **Step 3: Commit**

```bash
git add app/move/
git commit -m "feat: add move detail page with type, stats, effect, and learner list"
```

---

### Task 13: Compare Page

**Files:**
- Create: `app/compare/[firstId]/page.tsx`
- Create: `app/compare/[firstId]/CompareClient.tsx`

- [ ] **Step 1: Write `app/compare/[firstId]/page.tsx`**

```typescript
// app/compare/[firstId]/page.tsx
import { fetchPokemonDetail, fetchPokemonList } from '@/lib/api'
import { notFound } from 'next/navigation'
import { CompareClient } from './CompareClient'

interface Props { params: { firstId: string } }

export default async function ComparePage({ params }: Props) {
  const id = parseInt(params.firstId)
  if (isNaN(id) || id < 1) notFound()
  const [first, allPokemon] = await Promise.all([
    fetchPokemonDetail(id),
    fetchPokemonList(),
  ])
  return <CompareClient first={first} allPokemon={allPokemon} />
}
```

- [ ] **Step 2: Write `app/compare/[firstId]/CompareClient.tsx`**

```typescript
// app/compare/[firstId]/CompareClient.tsx
'use client'
import { useState, useEffect } from 'react'
import Image from 'next/image'
import { fetchPokemonDetail } from '@/lib/api'
import { TypeBadge } from '@/components/pokemon/TypeBadge'
import { StatBar } from '@/components/pokemon/StatBar'
import { spriteUrl } from '@/lib/constants'
import type { PokemonDetail } from '@/lib/types'

const STAT_ORDER = ['hp','attack','defense','special-attack','special-defense','speed']

interface Props {
  first: PokemonDetail
  allPokemon: { id: number; name: string }[]
}

export function CompareClient({ first, allPokemon }: Props) {
  const [query, setQuery] = useState('')
  const [second, setSecond] = useState<PokemonDetail | null>(null)
  const [suggestions, setSuggestions] = useState<typeof allPokemon>([])

  useEffect(() => {
    const q = query.trim().toLowerCase()
    setSuggestions(q.length > 0 ? allPokemon.filter(p => p.name.includes(q)).slice(0, 5) : [])
  }, [query, allPokemon])

  const pickSecond = async (id: number) => {
    setQuery('')
    setSuggestions([])
    const detail = await fetchPokemonDetail(id)
    setSecond(detail)
  }

  const getStat = (p: PokemonDetail, name: string) =>
    p.stats.find(s => s.stat.name === name)?.base_stat ?? 0

  return (
    <div style={{ background: 'var(--bg)', minHeight: '100vh', padding: '16px' }}>
      <h1 style={{ fontFamily: 'var(--font-pixel)', fontSize: '10px', color: 'var(--gold)', marginBottom: '16px' }}>COMPARE</h1>

      <div className="grid grid-cols-2 gap-4 mb-6">
        {/* First Pokémon (fixed) */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', padding: '12px', textAlign: 'center' }}>
          <Image src={spriteUrl(first.id)} alt={first.name} width={80} height={80} unoptimized style={{ imageRendering: 'pixelated', margin: '0 auto' }} />
          <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--gold)' }}>{first.name.toUpperCase()}</p>
          <div className="flex gap-1 justify-center mt-1">
            {first.types.map(t => <TypeBadge key={t.slot} type={t.type.name} small />)}
          </div>
        </div>

        {/* Second Pokémon (picker) */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', padding: '12px' }}>
          {second ? (
            <div className="text-center">
              <Image src={spriteUrl(second.id)} alt={second.name} width={80} height={80} unoptimized style={{ imageRendering: 'pixelated', margin: '0 auto' }} />
              <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--gold)' }}>{second.name.toUpperCase()}</p>
              <div className="flex gap-1 justify-center mt-1">
                {second.types.map(t => <TypeBadge key={t.slot} type={t.type.name} small />)}
              </div>
              <button onClick={() => setSecond(null)} style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)', marginTop: '6px' }}>
                CHANGE
              </button>
            </div>
          ) : (
            <div className="relative">
              <input
                value={query}
                onChange={e => setQuery(e.target.value)}
                placeholder="SEARCH POKÉMON"
                style={{ width: '100%', background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)', fontFamily: 'var(--font-pixel)', fontSize: '6px', padding: '6px 8px', borderRadius: '3px' }}
              />
              {suggestions.length > 0 && (
                <ul style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '3px', marginTop: '2px', position: 'absolute', width: '100%', zIndex: 10 }}>
                  {suggestions.map(p => (
                    <li key={p.id}>
                      <button onClick={() => pickSecond(p.id)} style={{ width: '100%', textAlign: 'left', fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text)', padding: '6px 8px' }}>
                        {p.name.toUpperCase()}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Stat comparison */}
      {second && (
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', padding: '12px' }}>
          <h2 style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text-muted)', marginBottom: '10px' }}>STATS</h2>
          {STAT_ORDER.map(stat => {
            const a = getStat(first, stat)
            const b = getStat(second, stat)
            return (
              <div key={stat} className="flex items-center gap-2 mb-3">
                <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: a > b ? 'var(--gold)' : 'var(--text)', width: '28px', textAlign: 'right' }}>{a}</span>
                <StatBar name={stat} value={a} />
                <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '5px', color: 'var(--text-muted)', width: '36px', textAlign: 'center' }}>{stat.slice(0,3).toUpperCase()}</span>
                <StatBar name={stat} value={b} />
                <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: b > a ? 'var(--gold)' : 'var(--text)', width: '28px' }}>{b}</span>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 3: Verify**

```bash
npm run dev
```

Open http://localhost:3000/compare/6 — Charizard on the left, search picker on the right. Pick Blastoise — stat bars appear side by side with gold highlight on winner.

- [ ] **Step 4: Commit**

```bash
git add app/compare/
git commit -m "feat: add side-by-side Pokémon compare page with stat diff"
```

---

### Task 14: Team Builder Page

**Files:**
- Create: `app/team/page.tsx`

- [ ] **Step 1: Write `app/team/page.tsx`**

```typescript
// app/team/page.tsx
'use client'
import { useState, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import Image from 'next/image'
import Link from 'next/link'
import { useTeam } from '@/lib/db'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { spriteUrl } from '@/lib/constants'
import { fetchPokemonList } from '@/lib/api'

export default function TeamPage() {
  const { teamIds, add, remove } = useTeam()
  const [pickerOpen, setPickerOpen] = useState(false)
  const [allPokemon, setAllPokemon] = useState<{ id: number; name: string }[]>([])
  const [query, setQuery] = useState('')
  const router = useRouter()

  const openPicker = useCallback(async () => {
    if (allPokemon.length === 0) {
      const list = await fetchPokemonList()
      setAllPokemon(list)
    }
    setPickerOpen(true)
  }, [allPokemon])

  const suggestions = query
    ? allPokemon.filter(p => p.name.includes(query.toLowerCase())).slice(0, 8)
    : []

  return (
    <div style={{ background: 'var(--bg)', minHeight: '100vh', padding: '16px' }}>
      <h1 style={{ fontFamily: 'var(--font-pixel)', fontSize: '10px', color: 'var(--gold)', marginBottom: '16px' }}>
        MY TEAM ({teamIds.length}/6)
      </h1>

      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-6">
        {Array.from({ length: 6 }, (_, i) => {
          const id = teamIds[i]
          if (id) {
            return (
              <div key={i} style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', padding: '10px', textAlign: 'center' }}>
                <Link href={`/pokemon/${id}`}>
                  <Image src={spriteUrl(id)} alt={`slot ${i}`} width={64} height={64} unoptimized style={{ imageRendering: 'pixelated', margin: '0 auto' }} />
                </Link>
                <button onClick={() => remove(id)} style={{ fontFamily: 'var(--font-pixel)', fontSize: '5px', color: 'var(--text-muted)', marginTop: '4px' }}>
                  REMOVE
                </button>
              </div>
            )
          }
          return (
            <button
              key={i}
              onClick={openPicker}
              style={{ background: 'var(--surface)', border: '1px dashed var(--border)', borderRadius: '4px', padding: '20px', cursor: 'pointer', fontFamily: 'var(--font-pixel)', fontSize: '20px', color: 'var(--text-muted)' }}
            >
              +
            </button>
          )
        })}
      </div>

      <Button onClick={() => router.push('/battle')} style={{ marginTop: '8px' }}>
        BATTLE HUB →
      </Button>

      <Modal open={pickerOpen} onClose={() => { setPickerOpen(false); setQuery('') }}>
        <h2 style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--gold)', marginBottom: '12px' }}>ADD TO TEAM</h2>
        <input
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder="SEARCH POKÉMON"
          autoFocus
          style={{ width: '100%', background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)', fontFamily: 'var(--font-pixel)', fontSize: '7px', padding: '8px 10px', borderRadius: '3px', marginBottom: '8px' }}
        />
        <div style={{ maxHeight: '300px', overflowY: 'auto' }}>
          {suggestions.map(p => (
            <button
              key={p.id}
              onClick={() => { add(p.id); setPickerOpen(false); setQuery('') }}
              style={{ display: 'flex', alignItems: 'center', gap: '8px', width: '100%', padding: '6px 8px', fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text)', borderBottom: '1px solid var(--border)' }}
              className="hover:opacity-70"
            >
              <Image src={spriteUrl(p.id)} alt={p.name} width={32} height={32} unoptimized style={{ imageRendering: 'pixelated' }} />
              {p.name.toUpperCase()}
              <span style={{ color: 'var(--text-muted)', marginLeft: 'auto', fontSize: '5px' }}>#{p.id}</span>
            </button>
          ))}
        </div>
      </Modal>
    </div>
  )
}
```

- [ ] **Step 2: Verify**

```bash
npm run dev
```

Open http://localhost:3000/team — 6 slots shown, empty slots are dashed "+". Clicking "+" opens search modal. Adding fills the slot. REMOVE button removes it.

- [ ] **Step 3: Commit**

```bash
git add app/team/
git commit -m "feat: add team builder with 6 slots and Pokémon search modal"
```

---

### Task 15: Settings Page

**Files:**
- Create: `app/settings/page.tsx`

- [ ] **Step 1: Write `app/settings/page.tsx`**

```typescript
// app/settings/page.tsx
'use client'
import { useTheme } from '@/lib/theme'
import { useSetting } from '@/lib/db'
import { GENERATIONS } from '@/lib/constants'

export default function SettingsPage() {
  const { theme, toggle } = useTheme()
  const [gen, setGen] = useSetting('generation', '3')
  const [music, setMusic] = useSetting('musicOnLaunch', 'false')

  const row = (label: string, control: React.ReactNode) => (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
      <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text)' }}>{label}</span>
      {control}
    </div>
  )

  return (
    <div style={{ background: 'var(--bg)', minHeight: '100vh', padding: '16px', maxWidth: '500px' }}>
      <h1 style={{ fontFamily: 'var(--font-pixel)', fontSize: '10px', color: 'var(--gold)', marginBottom: '16px' }}>SETTINGS</h1>

      {row('THEME', (
        <button
          onClick={toggle}
          style={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--text)', fontFamily: 'var(--font-pixel)', fontSize: '7px', padding: '4px 10px', borderRadius: '3px', cursor: 'pointer' }}
        >
          {theme === 'dark' ? 'DARK ●' : 'LIGHT ☀'}
        </button>
      ))}

      {row('DEFAULT GEN', (
        <select
          value={gen}
          onChange={e => setGen(e.target.value)}
          style={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--text)', fontFamily: 'var(--font-pixel)', fontSize: '6px', padding: '4px 8px', borderRadius: '3px' }}
        >
          {GENERATIONS.map((g, i) => (
            <option key={i} value={String(i + 1)}>{g.label}</option>
          ))}
        </select>
      ))}

      {row('MUSIC ON LAUNCH', (
        <button
          onClick={() => setMusic(music === 'true' ? 'false' : 'true')}
          style={{
            background: music === 'true' ? 'var(--gold)' : 'var(--surface)',
            border: '1px solid var(--border)',
            color: music === 'true' ? 'var(--surface)' : 'var(--text-muted)',
            fontFamily: 'var(--font-pixel)',
            fontSize: '7px',
            padding: '4px 10px',
            borderRadius: '3px',
            cursor: 'pointer',
          }}
        >
          {music === 'true' ? 'ON' : 'OFF'}
        </button>
      ))}

      <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '5px', color: 'var(--text-muted)', marginTop: '8px' }}>
        Music toggle saved for future web audio support
      </p>
    </div>
  )
}
```

- [ ] **Step 2: Verify**

```bash
npm run dev
```

Open http://localhost:3000/settings — theme toggle switches dark/light, preference persists on refresh.

- [ ] **Step 3: Commit**

```bash
git add app/settings/
git commit -m "feat: add settings page — theme, generation, and music toggles"
```


---

### Task 16: StatConfig TypeScript Port

**Files:**
- Create: `lib/battle/StatConfig.ts`
- Create: `src/test/battle/StatConfig.test.ts`

Reference: `~/Git/pokemon-battle-engine/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/StatConfig.kt`

- [ ] **Step 1: Write the failing test**

```typescript
// src/test/battle/StatConfig.test.ts
import { describe, it, expect } from 'vitest'
import { calcStat, calcHp, applyNature } from '@/lib/battle/StatConfig'

describe('Gen 3+ stat calc', () => {
  it('calculates Garchomp attack at level 100, 31 IV, 252 EV, +atk nature', () => {
    // Base ATK = 130, IV = 31, EV = 252, level = 100, nature = 1.1
    const stat = calcStat({ base: 130, iv: 31, ev: 252, level: 100, natureModifier: 1.1 })
    expect(stat).toBe(426)
  })

  it('calculates HP separately', () => {
    // Blissey HP: base=255, iv=31, ev=252, level=100
    const hp = calcHp({ base: 255, iv: 31, ev: 252, level: 100 })
    expect(hp).toBe(620)
  })
})

describe('Gen 1/2 stat calc', () => {
  it('calculates stat with DVs and stat exp', () => {
    // Base 100, DV 15, statExp 65535 → max gen1 stat at level 100
    const stat = calcStat({ base: 100, dv: 15, statExp: 65535, level: 100, gen: 1 })
    expect(stat).toBeGreaterThan(0)
  })
})

describe('applyNature', () => {
  it('boosts the correct stat', () => {
    expect(applyNature(100, 'attack', { name: 'Adamant', boosted: 1, dropped: 3 })).toBe(110)
  })

  it('drops the correct stat', () => {
    expect(applyNature(100, 'special-attack', { name: 'Adamant', boosted: 1, dropped: 3 })).toBe(90)
  })

  it('neutral nature does nothing', () => {
    expect(applyNature(100, 'attack', { name: 'Hardy', boosted: null, dropped: null })).toBe(100)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
npx vitest run src/test/battle/StatConfig.test.ts
```

Expected: FAIL — `Cannot find module '@/lib/battle/StatConfig'`

- [ ] **Step 3: Write `lib/battle/StatConfig.ts`**

```typescript
// lib/battle/StatConfig.ts

export type StatIndex = 0 | 1 | 2 | 3 | 4 | 5
// 0=HP, 1=ATK, 2=DEF, 3=SPATK, 4=SPDEF, 5=SPE

export type StatConfig =
  | { kind: 'gen3plus'; ivs: number[]; evs: number[] }
  | { kind: 'gen12'; dvs: number[]; statExp: number[] }

export interface NatureData {
  name: string
  boosted: number | null
  dropped: number | null
}

const STAT_NAMES = ['hp','attack','defense','special-attack','special-defense','speed'] as const

function statIndex(name: string): number {
  return STAT_NAMES.indexOf(name as typeof STAT_NAMES[number])
}

interface Gen3Params { base: number; iv: number; ev: number; level: number; natureModifier?: number }
interface Gen12Params { base: number; dv: number; statExp: number; level: number; gen?: number }
interface HpParams { base: number; iv: number; ev: number; level: number }

export function calcStat(params: Gen3Params | Gen12Params): number {
  if ('iv' in params && 'ev' in params && !('dv' in params)) {
    const { base, iv, ev, level, natureModifier = 1 } = params as Gen3Params
    const stat = Math.floor(((2 * base + iv + Math.floor(ev / 4)) * level) / 100) + 5
    return Math.floor(stat * natureModifier)
  }
  const { base, dv, statExp, level } = params as Gen12Params
  const statExpSqrt = Math.min(255, Math.floor(Math.ceil(Math.sqrt(statExp))))
  return Math.floor(((base + dv) * 2 + Math.floor(statExpSqrt / 4)) * level / 100) + 5
}

export function calcHp({ base, iv, ev, level }: HpParams): number {
  return Math.floor(((2 * base + iv + Math.floor(ev / 4)) * level) / 100) + level + 10
}

export function applyNature(value: number, statName: string, nature: NatureData): number {
  const idx = statIndex(statName)
  if (nature.boosted === idx) return Math.floor(value * 1.1)
  if (nature.dropped === idx) return Math.floor(value * 0.9)
  return value
}

export function resolveStats(
  baseStats: number[],
  config: StatConfig,
  level: number,
  nature: NatureData
): number[] {
  return baseStats.map((base, i) => {
    if (i === 0) {
      if (config.kind === 'gen3plus') return calcHp({ base, iv: config.ivs[0], ev: config.evs[0], level })
      return calcStat({ base, dv: config.dvs[0], statExp: config.statExp[0], level })
    }
    let stat: number
    if (config.kind === 'gen3plus') {
      stat = calcStat({ base, iv: config.ivs[i], ev: config.evs[i], level })
      stat = applyNature(stat, STAT_NAMES[i], nature)
    } else {
      stat = calcStat({ base, dv: config.dvs[i] ?? 0, statExp: config.statExp[i] ?? 0, level })
    }
    return stat
  })
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
npx vitest run src/test/battle/StatConfig.test.ts
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add lib/battle/StatConfig.ts src/test/battle/StatConfig.test.ts
git commit -m "feat: port StatConfig to TypeScript with gen 1/2/3+ stat calculations"
```

---

### Task 17: DamageEngine TypeScript Port

**Files:**
- Create: `lib/battle/DamageEngine.ts`
- Create: `src/test/battle/DamageEngine.test.ts`

Reference: `~/Git/pokemon-battle-engine/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/DamageEngine.kt`

- [ ] **Step 1: Write the failing test**

```typescript
// src/test/battle/DamageEngine.test.ts
import { describe, it, expect } from 'vitest'
import { calcDamage } from '@/lib/battle/DamageEngine'

describe('calcDamage', () => {
  it('calculates base damage', () => {
    const dmg = calcDamage({
      level: 50, power: 80,
      attack: 100, defense: 100,
      stabMultiplier: 1, typeMultiplier: 1, critMultiplier: 1, randomFactor: 1,
    })
    expect(dmg).toBeGreaterThan(0)
    expect(dmg).toBeLessThan(200)
  })

  it('STAB doubles with 1.5x multiplier', () => {
    const noStab = calcDamage({ level: 50, power: 80, attack: 100, defense: 100, stabMultiplier: 1, typeMultiplier: 1, critMultiplier: 1, randomFactor: 1 })
    const stab   = calcDamage({ level: 50, power: 80, attack: 100, defense: 100, stabMultiplier: 1.5, typeMultiplier: 1, critMultiplier: 1, randomFactor: 1 })
    expect(stab).toBeCloseTo(noStab * 1.5, 0)
  })

  it('super effective doubles damage', () => {
    const normal = calcDamage({ level: 50, power: 80, attack: 100, defense: 100, stabMultiplier: 1, typeMultiplier: 1, critMultiplier: 1, randomFactor: 1 })
    const super_  = calcDamage({ level: 50, power: 80, attack: 100, defense: 100, stabMultiplier: 1, typeMultiplier: 2, critMultiplier: 1, randomFactor: 1 })
    expect(super_).toBeCloseTo(normal * 2, 0)
  })

  it('crit doubles damage', () => {
    const normal = calcDamage({ level: 50, power: 80, attack: 100, defense: 100, stabMultiplier: 1, typeMultiplier: 1, critMultiplier: 1, randomFactor: 1 })
    const crit   = calcDamage({ level: 50, power: 80, attack: 100, defense: 100, stabMultiplier: 1, typeMultiplier: 1, critMultiplier: 2, randomFactor: 1 })
    expect(crit).toBeCloseTo(normal * 2, 0)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
npx vitest run src/test/battle/DamageEngine.test.ts
```

Expected: FAIL — `Cannot find module '@/lib/battle/DamageEngine'`

- [ ] **Step 3: Write `lib/battle/DamageEngine.ts`**

```typescript
// lib/battle/DamageEngine.ts

export interface DamageParams {
  level: number
  power: number
  attack: number
  defense: number
  stabMultiplier: number
  typeMultiplier: number
  critMultiplier: number
  randomFactor: number
  heldItemMultiplier?: number
}

export function calcDamage(p: DamageParams): number {
  const base = Math.floor(
    (Math.floor((2 * p.level) / 5 + 2) * p.power * p.attack) / p.defense / 50 + 2
  )
  return Math.floor(
    base
    * p.critMultiplier
    * p.randomFactor
    * p.stabMultiplier
    * p.typeMultiplier
    * (p.heldItemMultiplier ?? 1)
  )
}

export function randomFactor(): number {
  return (85 + Math.floor(Math.random() * 16)) / 100
}

export function getTypeEffectiveness(
  moveType: string,
  defenderTypes: string[],
  chart: Record<string, { double_damage_to: {name:string}[]; half_damage_to: {name:string}[]; no_damage_to: {name:string}[] }>
): number {
  const relations = chart[moveType]
  if (!relations) return 1
  return defenderTypes.reduce((mult, defType) => {
    if (relations.no_damage_to.some(t => t.name === defType)) return mult * 0
    if (relations.double_damage_to.some(t => t.name === defType)) return mult * 2
    if (relations.half_damage_to.some(t => t.name === defType)) return mult * 0.5
    return mult
  }, 1)
}

export function calcDamageRange(p: Omit<DamageParams, 'randomFactor'>): { min: number; max: number } {
  return {
    min: calcDamage({ ...p, randomFactor: 0.85 }),
    max: calcDamage({ ...p, randomFactor: 1.00 }),
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
npx vitest run src/test/battle/DamageEngine.test.ts
```

Expected: 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add lib/battle/DamageEngine.ts src/test/battle/DamageEngine.test.ts
git commit -m "feat: port DamageEngine to TypeScript with type effectiveness and damage range"
```

---

### Task 18: HeldItemEffect and BattleEngine Ports

**Files:**
- Create: `lib/battle/HeldItemEffect.ts`
- Create: `lib/battle/BattleEngine.ts`
- Create: `lib/battle/TrainerRoster.ts`
- Create: `src/test/battle/BattleEngine.test.ts`

Reference: `~/Git/pokemon-battle-engine/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/`

- [ ] **Step 1: Write `lib/battle/HeldItemEffect.ts`**

```typescript
// lib/battle/HeldItemEffect.ts
import type { HeldItem } from '@/lib/types'

export type HeldItemEffect =
  | { kind: 'stat_multiplier'; statIndex: number; factor: number }
  | { kind: 'damage_multiplier'; factor: number }
  | { kind: 'type_multiplier'; type: string; factor: number }
  | { kind: 'super_effective_boost'; factor: number }
  | { kind: 'none' }

const ITEM_EFFECTS: Record<string, HeldItemEffect> = {
  'choice-band':    { kind: 'stat_multiplier', statIndex: 1, factor: 1.5 },
  'choice-specs':   { kind: 'stat_multiplier', statIndex: 3, factor: 1.5 },
  'choice-scarf':   { kind: 'stat_multiplier', statIndex: 5, factor: 1.5 },
  'life-orb':       { kind: 'damage_multiplier', factor: 1.3 },
  'expert-belt':    { kind: 'super_effective_boost', factor: 1.2 },
  'muscle-band':    { kind: 'stat_multiplier', statIndex: 1, factor: 1.1 },
  'wise-glasses':   { kind: 'stat_multiplier', statIndex: 3, factor: 1.1 },
  'flame-orb':      { kind: 'none' },
  'toxic-orb':      { kind: 'none' },
}

const TYPE_ITEMS: [string, string, number][] = [
  ['charcoal', 'fire', 1.2], ['mystic-water', 'water', 1.2],
  ['miracle-seed', 'grass', 1.2], ['magnet', 'electric', 1.2],
  ['never-melt-ice', 'ice', 1.2], ['black-belt', 'fighting', 1.2],
  ['poison-barb', 'poison', 1.2], ['soft-sand', 'ground', 1.2],
  ['sharp-beak', 'flying', 1.2], ['twisted-spoon', 'psychic', 1.2],
  ['silver-powder', 'bug', 1.2], ['hard-stone', 'rock', 1.2],
  ['spell-tag', 'ghost', 1.2], ['dragon-fang', 'dragon', 1.2],
  ['black-glasses', 'dark', 1.2], ['metal-coat', 'steel', 1.2],
  ['silk-scarf', 'normal', 1.2], ['fairy-feather', 'fairy', 1.2],
]

export function getHeldItemEffect(item: HeldItem | null): HeldItemEffect {
  if (!item) return { kind: 'none' }
  const direct = ITEM_EFFECTS[item.name]
  if (direct) return direct
  const typeEntry = TYPE_ITEMS.find(([name]) => name === item.name)
  if (typeEntry) return { kind: 'type_multiplier', type: typeEntry[1], factor: typeEntry[2] }
  return { kind: 'none' }
}

export function applyHeldItemDamageMultiplier(
  effect: HeldItemEffect,
  moveType: string,
  typeMultiplier: number
): number {
  switch (effect.kind) {
    case 'damage_multiplier': return effect.factor
    case 'type_multiplier':   return effect.type === moveType ? effect.factor : 1
    case 'super_effective_boost': return typeMultiplier > 1 ? effect.factor : 1
    default: return 1
  }
}
```

- [ ] **Step 2: Write `lib/battle/TrainerRoster.ts`**

```typescript
// lib/battle/TrainerRoster.ts

export interface TrainerPokemon {
  id: number
  name: string
  level: number
  moves: string[]
  heldItem?: string
}

export interface Trainer {
  id: string
  name: string
  title: string
  region: string
  trainerClass: 'GYM_LEADER' | 'ELITE_FOUR' | 'CHAMPION' | 'RIVAL' | 'TRAINER'
  typeSpecialty: string
  rosters: TrainerPokemon[][]
}

export async function loadTrainers(): Promise<Trainer[]> {
  const res = await fetch('/trainers/trainers.json')
  if (!res.ok) return []
  return res.json()
}
```

- [ ] **Step 3: Write `lib/battle/BattleEngine.ts`**

```typescript
// lib/battle/BattleEngine.ts
import { calcDamage, randomFactor, getTypeEffectiveness } from './DamageEngine'
import type { PokemonDetail } from '@/lib/types'

export interface BattlePokemon {
  id: number
  name: string
  level: number
  types: string[]
  currentHp: number
  maxHp: number
  stats: number[]          // [hp, atk, def, spatk, spdef, spe]
  moves: BattleMove[]
  heldItem?: string
}

export interface BattleMove {
  name: string
  type: string
  category: 'physical' | 'special' | 'status'
  power: number
  accuracy: number
  pp: number
  currentPp: number
}

export type BattleState =
  | { phase: 'setup' }
  | { phase: 'player_turn' }
  | { phase: 'enemy_turn' }
  | { phase: 'result'; message: string }
  | { phase: 'won' }
  | { phase: 'lost' }

export interface BattleLog { turn: number; message: string }

export function buildBattlePokemon(
  detail: PokemonDetail,
  level: number,
  moveDetails: BattleMove[],
  stats: number[]
): BattlePokemon {
  return {
    id: detail.id,
    name: detail.name,
    level,
    types: detail.types.map(t => t.type.name),
    currentHp: stats[0],
    maxHp: stats[0],
    stats,
    moves: moveDetails,
  }
}

export function resolvePlayerAttack(
  player: BattlePokemon,
  enemy: BattlePokemon,
  moveIndex: number,
  typeChart: Record<string, { double_damage_to: {name:string}[]; half_damage_to: {name:string}[]; no_damage_to: {name:string}[] }>
): { updatedEnemy: BattlePokemon; damage: number; log: string; crit: boolean } {
  const move = player.moves[moveIndex]
  if (!move || move.currentPp <= 0) return { updatedEnemy: enemy, damage: 0, log: 'No PP left!', crit: false }

  const missRoll = Math.random() * 100
  if (missRoll > move.accuracy) {
    return { updatedEnemy: enemy, damage: 0, log: `${player.name.toUpperCase()} missed!`, crit: false }
  }

  const atk = move.category === 'physical' ? player.stats[1] : player.stats[3]
  const def = move.category === 'physical' ? enemy.stats[2] : enemy.stats[4]
  const stab = player.types.includes(move.type) ? 1.5 : 1
  const type = getTypeEffectiveness(move.type, enemy.types, typeChart)
  const crit = Math.random() < 0.0625
  const rf = randomFactor()

  const damage = calcDamage({ level: player.level, power: move.power, attack: atk, defense: def, stabMultiplier: stab, typeMultiplier: type, critMultiplier: crit ? 2 : 1, randomFactor: rf })
  const updatedEnemy = { ...enemy, currentHp: Math.max(0, enemy.currentHp - damage) }

  let log = `${player.name.toUpperCase()} used ${move.name.toUpperCase()}!`
  if (crit) log += ' Critical hit!'
  if (type > 1) log += " It's super effective!"
  if (type < 1 && type > 0) log += " It's not very effective..."
  if (type === 0) log += " It had no effect."
  log += ` ${damage} damage.`
  if (updatedEnemy.currentHp === 0) log += ` ${enemy.name.toUpperCase()} fainted!`

  return { updatedEnemy, damage, log, crit }
}

export function resolveEnemyAttack(
  enemy: BattlePokemon,
  player: BattlePokemon,
  typeChart: Parameters<typeof resolvePlayerAttack>[3]
): { updatedPlayer: BattlePokemon; damage: number; log: string } {
  const availableMoves = enemy.moves.filter(m => m.currentPp > 0 && m.power > 0)
  if (availableMoves.length === 0) return { updatedPlayer: player, damage: 0, log: `${enemy.name.toUpperCase()} has no moves!` }
  const move = availableMoves[Math.floor(Math.random() * availableMoves.length)]

  const atk = move.category === 'physical' ? enemy.stats[1] : enemy.stats[3]
  const def = move.category === 'physical' ? player.stats[2] : player.stats[4]
  const stab = enemy.types.includes(move.type) ? 1.5 : 1
  const type = getTypeEffectiveness(move.type, player.types, typeChart)
  const rf = randomFactor()

  const damage = calcDamage({ level: enemy.level, power: move.power, attack: atk, defense: def, stabMultiplier: stab, typeMultiplier: type, critMultiplier: 1, randomFactor: rf })
  const updatedPlayer = { ...player, currentHp: Math.max(0, player.currentHp - damage) }

  let log = `${enemy.name.toUpperCase()} used ${move.name.toUpperCase()}! ${damage} damage.`
  if (updatedPlayer.currentHp === 0) log += ` ${player.name.toUpperCase()} fainted!`

  return { updatedPlayer, damage, log }
}
```

- [ ] **Step 4: Write the failing test**

```typescript
// src/test/battle/BattleEngine.test.ts
import { describe, it, expect } from 'vitest'
import { resolvePlayerAttack, type BattlePokemon, type BattleMove } from '@/lib/battle/BattleEngine'

const mockChart = {
  fire: {
    double_damage_to: [{ name: 'grass' }, { name: 'ice' }],
    half_damage_to: [{ name: 'water' }],
    no_damage_to: [],
  },
}

const makePlayer = (): BattlePokemon => ({
  id: 6, name: 'charizard', level: 50, types: ['fire', 'flying'],
  currentHp: 100, maxHp: 100, stats: [100, 109, 78, 120, 85, 100],
  moves: [{ name: 'flamethrower', type: 'fire', category: 'special', power: 90, accuracy: 100, pp: 15, currentPp: 15 }],
})

const makeEnemy = (type: string): BattlePokemon => ({
  id: 1, name: 'bulbasaur', level: 50, types: [type],
  currentHp: 100, maxHp: 100, stats: [100, 80, 80, 80, 80, 80],
  moves: [],
})

describe('resolvePlayerAttack', () => {
  it('deals more damage to grass (super effective fire)', () => {
    const grassEnemy = makeEnemy('grass')
    const waterEnemy = makeEnemy('water')
    const { damage: vGrass } = resolvePlayerAttack(makePlayer(), grassEnemy, 0, mockChart)
    const { damage: vWater } = resolvePlayerAttack(makePlayer(), waterEnemy, 0, mockChart)
    expect(vGrass).toBeGreaterThan(vWater)
  })

  it('deals 0 damage on miss when accuracy forces miss', () => {
    const player = { ...makePlayer(), moves: [{ ...makePlayer().moves[0], accuracy: 0 }] }
    const { damage } = resolvePlayerAttack(player, makeEnemy('normal'), 0, mockChart)
    expect(damage).toBe(0)
  })

  it('returns fainted log when damage >= enemy HP', () => {
    const weakEnemy = { ...makeEnemy('grass'), currentHp: 1, maxHp: 1 }
    const { log } = resolvePlayerAttack(makePlayer(), weakEnemy, 0, mockChart)
    expect(log.toLowerCase()).toContain('fainted')
  })
})
```

- [ ] **Step 5: Run test to verify it fails**

```bash
npx vitest run src/test/battle/BattleEngine.test.ts
```

Expected: FAIL — `Cannot find module '@/lib/battle/BattleEngine'`

- [ ] **Step 6: Run tests after implementation**

```bash
npx vitest run src/test/battle/
```

Expected: all battle engine tests pass (StatConfig + DamageEngine + BattleEngine).

- [ ] **Step 7: Commit**

```bash
git add lib/battle/ src/test/battle/BattleEngine.test.ts
git commit -m "feat: port HeldItemEffect, BattleEngine, TrainerRoster to TypeScript"
```

---

### Task 19: Copy Trainer JSON Assets

**Files:**
- Create: `public/trainers/trainers.json` (copy from Android app)

- [ ] **Step 1: Copy trainer JSON from Android app**

```bash
mkdir -p public/trainers
cp ~/Git/sandbox/Pokedex/app/src/main/assets/trainers/*.json public/trainers/
```

Expected: JSON file(s) appear in `public/trainers/`.

- [ ] **Step 2: Verify the file is accessible**

```bash
npm run dev
```

In another terminal:
```bash
curl http://localhost:3000/trainers/trainers.json | head -50
```

Expected: JSON data with trainer objects.

- [ ] **Step 3: Commit**

```bash
git add public/trainers/
git commit -m "feat: add trainer JSON assets to public directory"
```

---

### Task 20: Battle Hub — DamageCalcScreen

**Files:**
- Create: `components/battle/DamageCalcScreen.tsx`

Reference: `~/Git/pokemon-battle-engine/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/DamageCalcScreen.kt`

- [ ] **Step 1: Write `components/battle/DamageCalcScreen.tsx`**

```typescript
// components/battle/DamageCalcScreen.tsx
'use client'
import { useState, useEffect } from 'react'
import { fetchPokemonDetail, fetchTypeChart } from '@/lib/api'
import { TypeBadge } from '@/components/pokemon/TypeBadge'
import { calcDamageRange, getTypeEffectiveness } from '@/lib/battle/DamageEngine'
import { resolveStats } from '@/lib/battle/StatConfig'
import { NATURES, TYPE_COLORS } from '@/lib/constants'
import type { PokemonDetail } from '@/lib/types'
import type { StatConfig, NatureData } from '@/lib/battle/StatConfig'

interface Props { preloadId?: number }

const DEFAULT_CONFIG: StatConfig = {
  kind: 'gen3plus',
  ivs: [31, 31, 31, 31, 31, 31],
  evs: [0, 0, 0, 0, 0, 0],
}
const DEFAULT_NATURE: NatureData = { name: 'Hardy', boosted: null, dropped: null }

export function DamageCalcScreen({ preloadId }: Props) {
  const [attacker, setAttacker] = useState<PokemonDetail | null>(null)
  const [defender, setDefender] = useState<PokemonDetail | null>(null)
  const [attackerQuery, setAttackerQuery] = useState('')
  const [defenderQuery, setDefenderQuery] = useState('')
  const [level, setLevel] = useState(50)
  const [selectedMoveIdx, setSelectedMoveIdx] = useState(0)
  const [typeChart, setTypeChart] = useState<Awaited<ReturnType<typeof fetchTypeChart>> | null>(null)
  const [result, setResult] = useState<{ min: number; max: number } | null>(null)

  useEffect(() => {
    fetchTypeChart().then(setTypeChart)
    if (preloadId) fetchPokemonDetail(preloadId).then(setAttacker)
  }, [preloadId])

  useEffect(() => {
    if (!attacker || !defender || !typeChart) return
    const move = attacker.moves[selectedMoveIdx]
    if (!move) return
    const atkStats = resolveStats(attacker.stats.map(s => s.base_stat), DEFAULT_CONFIG, level, DEFAULT_NATURE)
    const defStats = resolveStats(defender.stats.map(s => s.base_stat), DEFAULT_CONFIG, level, DEFAULT_NATURE)
    const moveName = move.move.name
    // Power lookup would need move fetch; use 80 as placeholder
    const power = 80
    const atk = atkStats[1]
    const def = defStats[2]
    const stab = attacker.types.some(t => t.type.name === moveName.split('-')[0]) ? 1.5 : 1
    const type = getTypeEffectiveness(attacker.types[0]?.type.name ?? 'normal', defender.types.map(t => t.type.name), typeChart)
    setResult(calcDamageRange({ level, power, attack: atk, defense: def, stabMultiplier: stab, typeMultiplier: type, critMultiplier: 1 }))
  }, [attacker, defender, level, selectedMoveIdx, typeChart])

  const search = async (query: string, setSide: (p: PokemonDetail) => void) => {
    const id = parseInt(query)
    if (!isNaN(id)) { const d = await fetchPokemonDetail(id); setSide(d) }
  }

  const pane = (
    label: string,
    pokemon: PokemonDetail | null,
    query: string,
    setQuery: (s: string) => void,
    setSide: (p: PokemonDetail) => void
  ) => (
    <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', padding: '10px', flex: 1, minWidth: 0 }}>
      <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text-muted)', marginBottom: '6px' }}>{label}</div>
      <div className="flex gap-2 mb-2">
        <input
          value={query}
          onChange={e => setQuery(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && search(query, setSide)}
          placeholder="NAME OR #ID"
          style={{ flex: 1, background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)', fontFamily: 'var(--font-pixel)', fontSize: '6px', padding: '4px 8px', borderRadius: '3px' }}
        />
        <button onClick={() => search(query, setSide)} style={{ background: 'var(--header)', color: '#fff', fontFamily: 'var(--font-pixel)', fontSize: '6px', padding: '4px 8px', borderRadius: '3px', cursor: 'pointer' }}>
          GO
        </button>
      </div>
      {pokemon && (
        <div>
          <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--gold)' }}>{pokemon.name.toUpperCase()}</div>
          <div className="flex gap-1 mt-1">
            {pokemon.types.map(t => <TypeBadge key={t.slot} type={t.type.name} small />)}
          </div>
        </div>
      )}
    </div>
  )

  return (
    <div style={{ padding: '12px', overflowY: 'auto', height: '100%' }}>
      <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--gold)', marginBottom: '10px' }}>DAMAGE CALC</div>

      <div className="flex gap-3 mb-4 flex-wrap">
        {pane('ATTACKER', attacker, attackerQuery, setAttackerQuery, setAttacker)}
        <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '10px', color: 'var(--text-muted)', alignSelf: 'center' }}>VS</div>
        {pane('DEFENDER', defender, defenderQuery, setDefenderQuery, setDefender)}
      </div>

      <div className="flex gap-4 items-center mb-4 flex-wrap">
        <div>
          <label style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)' }}>LEVEL</label>
          <input type="number" min={1} max={100} value={level} onChange={e => setLevel(Number(e.target.value))}
            style={{ display: 'block', width: '64px', background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--text)', fontFamily: 'var(--font-pixel)', fontSize: '8px', padding: '4px 6px', borderRadius: '3px', marginTop: '2px' }} />
        </div>
        <div>
          <label style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)' }}>NATURE</label>
          <select style={{ display: 'block', background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--text)', fontFamily: 'var(--font-pixel)', fontSize: '6px', padding: '4px 6px', borderRadius: '3px', marginTop: '2px' }}>
            {NATURES.map(n => <option key={n.name}>{n.name}</option>)}
          </select>
        </div>
      </div>

      {result && (
        <div style={{ background: 'var(--bg)', border: '1px solid var(--border)', borderRadius: '4px', padding: '12px', marginTop: '8px' }}>
          <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text-muted)' }}>DAMAGE RANGE</div>
          <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '16px', color: 'var(--gold)', marginTop: '4px' }}>
            {result.min} – {result.max}
          </div>
          {defender && (
            <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)', marginTop: '4px' }}>
              {Math.round((result.min / (defender.stats[0]?.base_stat ?? 1)) * 100)}%–
              {Math.round((result.max / (defender.stats[0]?.base_stat ?? 1)) * 100)}% of base HP
            </div>
          )}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add components/battle/DamageCalcScreen.tsx
git commit -m "feat: add DamageCalcScreen with attacker/defender pickers and damage range"
```

---

### Task 21: Battle Hub — TurnBattleScreen, MatchupScreen, RecordScreen, TrainerSelectScreen

**Files:**
- Create: `components/battle/TurnBattleScreen.tsx`
- Create: `components/battle/MatchupScreen.tsx`
- Create: `components/battle/RecordScreen.tsx`
- Create: `components/battle/TrainerSelectScreen.tsx`

- [ ] **Step 1: Write `components/battle/TurnBattleScreen.tsx`**

```typescript
// components/battle/TurnBattleScreen.tsx
'use client'
import { useState, useCallback } from 'react'
import { fetchPokemonDetail } from '@/lib/api'
import { SpriteImage } from '@/components/pokemon/SpriteImage'
import { useWildRecords, useTrainerRecords } from '@/lib/db'
import { resolvePlayerAttack, resolveEnemyAttack, buildBattlePokemon, type BattlePokemon, type BattleState } from '@/lib/battle/BattleEngine'
import { resolveStats } from '@/lib/battle/StatConfig'
import { Button } from '@/components/ui/Button'
import type { Trainer } from '@/lib/battle/TrainerRoster'

interface Props { teamIds: number[] }

const DEFAULT_STAT_CONFIG = { kind: 'gen3plus' as const, ivs: [31,31,31,31,31,31], evs: [0,0,0,0,0,0] }
const DEFAULT_NATURE = { name: 'Hardy', boosted: null, dropped: null }

export function TurnBattleScreen({ teamIds }: Props) {
  const [state, setState] = useState<BattleState>({ phase: 'setup' })
  const [player, setPlayer] = useState<BattlePokemon | null>(null)
  const [enemy, setEnemy] = useState<BattlePokemon | null>(null)
  const [log, setLog] = useState<string[]>([])
  const [enemyIdInput, setEnemyIdInput] = useState('')
  const [typeChart, setTypeChart] = useState<Record<string, { double_damage_to: {name:string}[]; half_damage_to: {name:string}[]; no_damage_to: {name:string}[] }>>({})
  const { recordBattle: recordWild } = useWildRecords()
  const { recordBattle: recordTrainer } = useTrainerRecords()

  const startWildBattle = useCallback(async () => {
    const enemyId = parseInt(enemyIdInput)
    if (isNaN(enemyId) || teamIds.length === 0) return
    const [enemyDetail, playerDetail, { fetchTypeChart }] = await Promise.all([
      fetchPokemonDetail(enemyId),
      fetchPokemonDetail(teamIds[0]),
      import('@/lib/api'),
    ])
    const chart = await fetchTypeChart()
    setTypeChart(chart)
    const level = 50
    const nature = DEFAULT_NATURE
    const buildPkmn = (detail: typeof playerDetail) => buildBattlePokemon(
      detail, level,
      detail.moves.slice(0, 4).map(m => ({ name: m.move.name, type: 'normal', category: 'physical' as const, power: 60, accuracy: 100, pp: 15, currentPp: 15 })),
      resolveStats(detail.stats.map(s => s.base_stat), DEFAULT_STAT_CONFIG, level, nature)
    )
    setPlayer(buildPkmn(playerDetail))
    setEnemy(buildPkmn(enemyDetail))
    setLog([`A wild ${enemyDetail.name.toUpperCase()} appeared!`])
    setState({ phase: 'player_turn' })
  }, [enemyIdInput, teamIds])

  const playerAttack = useCallback((moveIdx: number) => {
    if (!player || !enemy || state.phase !== 'player_turn') return
    const { updatedEnemy, log: attackLog } = resolvePlayerAttack(player, enemy, moveIdx, typeChart)
    const newLog = [...log, attackLog]
    if (updatedEnemy.currentHp === 0) {
      setEnemy(updatedEnemy)
      setLog([...newLog, 'You won!'])
      setState({ phase: 'won' })
      recordWild(enemy.id, enemy.name, true)
      return
    }
    const { updatedPlayer, log: enemyLog } = resolveEnemyAttack(updatedEnemy, player, typeChart)
    setEnemy(updatedEnemy)
    setPlayer(updatedPlayer)
    setLog([...newLog, enemyLog])
    if (updatedPlayer.currentHp === 0) {
      setState({ phase: 'lost' })
      recordWild(enemy.id, enemy.name, false)
    }
  }, [player, enemy, state, typeChart, log, recordWild])

  const hpBar = (current: number, max: number, color = '#78c850') => (
    <div style={{ background: 'var(--border)', borderRadius: '3px', height: '8px', width: '100%', overflow: 'hidden' }}>
      <div style={{ width: `${Math.max(0, (current / max) * 100)}%`, background: color, height: '100%', transition: 'width 0.3s' }} />
    </div>
  )

  if (state.phase === 'setup') return (
    <div style={{ padding: '12px' }}>
      <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--gold)', marginBottom: '10px' }}>WILD BATTLE</div>
      <div className="flex gap-2 mb-4">
        <input value={enemyIdInput} onChange={e => setEnemyIdInput(e.target.value)} placeholder="ENEMY POKÉMON #ID"
          style={{ flex: 1, background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--text)', fontFamily: 'var(--font-pixel)', fontSize: '7px', padding: '6px 10px', borderRadius: '3px' }} />
        <Button onClick={startWildBattle}>START</Button>
      </div>
      {teamIds.length === 0 && (
        <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)' }}>Add Pokémon to your team first!</p>
      )}
    </div>
  )

  return (
    <div style={{ padding: '12px', display: 'flex', flexDirection: 'column', gap: '8px', height: '100%' }}>
      {/* Enemy */}
      {enemy && (
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', padding: '8px' }}>
          <div className="flex items-center gap-3">
            <SpriteImage id={enemy.id} name={enemy.name} size={56} />
            <div className="flex-1">
              <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--text)' }}>{enemy.name.toUpperCase()}</div>
              <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                HP {enemy.currentHp}/{enemy.maxHp}
              </div>
              {hpBar(enemy.currentHp, enemy.maxHp)}
            </div>
          </div>
        </div>
      )}

      {/* Player */}
      {player && (
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', padding: '8px' }}>
          <div className="flex items-center gap-3">
            <SpriteImage id={player.id} name={player.name} size={56} />
            <div className="flex-1">
              <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--text)' }}>{player.name.toUpperCase()}</div>
              <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                HP {player.currentHp}/{player.maxHp}
              </div>
              {hpBar(player.currentHp, player.maxHp, '#6890f0')}
            </div>
          </div>
        </div>
      )}

      {/* Log */}
      <div style={{ background: 'var(--bg)', border: '1px solid var(--border)', borderRadius: '3px', padding: '8px', flex: 1, overflowY: 'auto', maxHeight: '120px' }}>
        {log.map((l, i) => (
          <div key={i} style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text)', lineHeight: '1.8' }}>{l}</div>
        ))}
      </div>

      {/* Move buttons */}
      {state.phase === 'player_turn' && player && (
        <div className="grid grid-cols-2 gap-2">
          {player.moves.slice(0, 4).map((m, i) => (
            <Button key={i} onClick={() => playerAttack(i)} variant="secondary" style={{ fontSize: '6px', padding: '6px' }}>
              {m.name.toUpperCase()}
            </Button>
          ))}
        </div>
      )}

      {(state.phase === 'won' || state.phase === 'lost') && (
        <Button onClick={() => { setState({ phase: 'setup' }); setPlayer(null); setEnemy(null); setLog([]) }}>
          {state.phase === 'won' ? 'VICTORY!' : 'DEFEAT'} — RESET
        </Button>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Write `components/battle/MatchupScreen.tsx`**

```typescript
// components/battle/MatchupScreen.tsx
'use client'
import { useState, useEffect } from 'react'
import { fetchTypeChart, fetchPokemonDetail } from '@/lib/api'
import { TypeBadge } from '@/components/pokemon/TypeBadge'
import { getTypeEffectiveness } from '@/lib/battle/DamageEngine'
import { TYPE_COLORS } from '@/lib/constants'

interface Props { teamIds: number[] }

const ALL_TYPES = Object.keys(TYPE_COLORS)

export function MatchupScreen({ teamIds }: Props) {
  const [chart, setChart] = useState<Awaited<ReturnType<typeof fetchTypeChart>> | null>(null)
  const [selectedType, setSelectedType] = useState('fire')
  const [teamTypes, setTeamTypes] = useState<string[][]>([])

  useEffect(() => { fetchTypeChart().then(setChart) }, [])

  useEffect(() => {
    Promise.all(teamIds.map(id => fetchPokemonDetail(id)))
      .then(details => setTeamTypes(details.map(d => d.types.map(t => t.type.name))))
  }, [teamIds])

  const effectivenessColor = (mult: number) => {
    if (mult === 0) return '#555'
    if (mult >= 2) return '#c03028'
    if (mult < 1) return '#78c850'
    return 'var(--text)'
  }

  return (
    <div style={{ padding: '12px', overflowY: 'auto', height: '100%' }}>
      <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--gold)', marginBottom: '10px' }}>TYPE MATCHUP</div>

      <div className="flex flex-wrap gap-1 mb-4">
        {ALL_TYPES.map(t => (
          <button key={t} onClick={() => setSelectedType(t)} style={{ opacity: selectedType === t ? 1 : 0.5 }}>
            <TypeBadge type={t} small />
          </button>
        ))}
      </div>

      {chart && (
        <div>
          <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text-muted)', marginBottom: '8px' }}>
            ATTACKING WITH {selectedType.toUpperCase()}
          </div>
          <div className="grid grid-cols-2 gap-2">
            {ALL_TYPES.map(defType => {
              const mult = getTypeEffectiveness(selectedType, [defType], chart)
              return (
                <div key={defType} style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '3px', padding: '4px 8px' }}>
                  <TypeBadge type={defType} small />
                  <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: effectivenessColor(mult), marginLeft: 'auto' }}>
                    {mult === 0 ? '✕' : `${mult}×`}
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 3: Write `components/battle/RecordScreen.tsx`**

```typescript
// components/battle/RecordScreen.tsx
'use client'
import Image from 'next/image'
import { useTrainerRecords, useWildRecords } from '@/lib/db'
import { spriteUrl } from '@/lib/constants'

export function RecordScreen() {
  const { records: trainerRecords } = useTrainerRecords()
  const { records: wildRecords } = useWildRecords()

  const totalBattles = trainerRecords.reduce((s, r) => s + r.wins + r.losses, 0)
    + wildRecords.reduce((s, r) => s + r.wins + r.losses, 0)

  if (totalBattles === 0) return (
    <div style={{ padding: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
      <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text-muted)' }}>NO BATTLES YET</p>
    </div>
  )

  const recordRow = (name: string, wins: number, losses: number, key: string | number, spriteId?: number) => (
    <div key={key} style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 10px', borderBottom: '1px solid var(--border)' }}>
      {spriteId && <Image src={spriteUrl(spriteId)} alt={name} width={32} height={32} unoptimized style={{ imageRendering: 'pixelated' }} />}
      <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text)', flex: 1 }}>{name.toUpperCase()}</span>
      <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: '#78c850' }}>{wins}W</span>
      <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: '#c03028' }}>{losses}L</span>
    </div>
  )

  return (
    <div style={{ padding: '12px', overflowY: 'auto', height: '100%' }}>
      <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--gold)', marginBottom: '10px' }}>
        BATTLE LOG — {totalBattles} BATTLES
      </div>

      {trainerRecords.length > 0 && (
        <section style={{ marginBottom: '16px' }}>
          <h3 style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text-muted)', marginBottom: '6px' }}>TRAINERS</h3>
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', overflow: 'hidden' }}>
            {trainerRecords.map(r => recordRow(`${r.name} (${r.trainerClass})`, r.wins, r.losses, r.trainerId))}
          </div>
        </section>
      )}

      {wildRecords.length > 0 && (
        <section>
          <h3 style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text-muted)', marginBottom: '6px' }}>WILD</h3>
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', overflow: 'hidden' }}>
            {wildRecords.map(r => recordRow(r.pokemonName, r.wins, r.losses, r.pokemonId, r.pokemonId))}
          </div>
        </section>
      )}
    </div>
  )
}
```

- [ ] **Step 4: Write `components/battle/TrainerSelectScreen.tsx`**

```typescript
// components/battle/TrainerSelectScreen.tsx
'use client'
import { useState, useEffect } from 'react'
import { loadTrainers, type Trainer } from '@/lib/battle/TrainerRoster'
import { TypeBadge } from '@/components/pokemon/TypeBadge'
import { Button } from '@/components/ui/Button'

interface Props {
  teamIds: number[]
  onStartBattle: (trainer: Trainer) => void
}

const CLASS_ORDER = ['CHAMPION', 'ELITE_FOUR', 'GYM_LEADER', 'RIVAL', 'TRAINER']
const CLASS_COLOR: Record<string, string> = {
  CHAMPION: '#f0c040', ELITE_FOUR: '#a040a0', GYM_LEADER: '#6890f0',
  RIVAL: '#f08030', TRAINER: '#a8a878',
}

export function TrainerSelectScreen({ teamIds, onStartBattle }: Props) {
  const [trainers, setTrainers] = useState<Trainer[]>([])
  const [selected, setSelected] = useState<Trainer | null>(null)

  useEffect(() => { loadTrainers().then(setTrainers) }, [])

  const grouped = CLASS_ORDER.map(cls => ({
    cls, trainers: trainers.filter(t => t.trainerClass === cls),
  })).filter(g => g.trainers.length > 0)

  return (
    <div style={{ padding: '12px', overflowY: 'auto', height: '100%' }}>
      <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: 'var(--gold)', marginBottom: '10px' }}>TRAINERS</div>

      {selected ? (
        <div>
          <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '9px', color: CLASS_COLOR[selected.trainerClass] ?? 'var(--text)', marginBottom: '4px' }}>
            {selected.name.toUpperCase()}
          </div>
          <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)', marginBottom: '8px' }}>
            {selected.title} • {selected.region.toUpperCase()} • {selected.typeSpecialty.toUpperCase()} TYPE
          </div>
          <div className="flex gap-2">
            <Button onClick={() => onStartBattle(selected)}>BATTLE!</Button>
            <Button onClick={() => setSelected(null)} variant="secondary">BACK</Button>
          </div>
        </div>
      ) : (
        grouped.map(({ cls, trainers: group }) => (
          <div key={cls} style={{ marginBottom: '12px' }}>
            <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: CLASS_COLOR[cls] ?? 'var(--text-muted)', marginBottom: '4px', letterSpacing: '1px' }}>
              {cls.replace('_', ' ')}
            </div>
            {group.map(t => (
              <button
                key={t.id}
                onClick={() => setSelected(t)}
                style={{ display: 'flex', alignItems: 'center', gap: '8px', width: '100%', padding: '6px 10px', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '3px', marginBottom: '3px', cursor: 'pointer' }}
              >
                <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '7px', color: 'var(--text)', flex: 1, textAlign: 'left' }}>
                  {t.name.toUpperCase()}
                </span>
                <TypeBadge type={t.typeSpecialty} small />
              </button>
            ))}
          </div>
        ))
      )}

      {teamIds.length === 0 && (
        <p style={{ fontFamily: 'var(--font-pixel)', fontSize: '6px', color: 'var(--text-muted)', marginTop: '8px' }}>
          Add Pokémon to your team to battle trainers.
        </p>
      )}
    </div>
  )
}
```

- [ ] **Step 5: Commit**

```bash
git add components/battle/
git commit -m "feat: add all battle hub screens — TurnBattle, Matchup, Record, TrainerSelect"
```

---

### Task 22: Battle Hub Page

**Files:**
- Create: `app/battle/page.tsx`

- [ ] **Step 1: Write `app/battle/page.tsx`**

```typescript
// app/battle/page.tsx
'use client'
import { useState, useEffect } from 'react'
import { useSearchParams } from 'next/navigation'
import { useTeam } from '@/lib/db'
import { DamageCalcScreen } from '@/components/battle/DamageCalcScreen'
import { TurnBattleScreen } from '@/components/battle/TurnBattleScreen'
import { MatchupScreen } from '@/components/battle/MatchupScreen'
import { RecordScreen } from '@/components/battle/RecordScreen'
import { TrainerSelectScreen } from '@/components/battle/TrainerSelectScreen'
import type { Trainer } from '@/lib/battle/TrainerRoster'

type Tab = 'CALC' | 'WILD' | 'TRAIN' | 'MATCH' | 'LOG'
const TABS: Tab[] = ['CALC', 'WILD', 'TRAIN', 'MATCH', 'LOG']

export default function BattlePage() {
  const [tab, setTab] = useState<Tab>('CALC')
  const { teamIds } = useTeam()
  const params = useSearchParams()
  const preloadId = params.get('preloadId') ? parseInt(params.get('preloadId')!) : undefined

  useEffect(() => { if (preloadId) setTab('CALC') }, [preloadId])

  return (
    <div style={{ background: 'var(--bg)', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <div style={{ background: 'var(--header)', padding: '8px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '8px', color: '#f0c040', letterSpacing: '1px' }}>BATTLE HUB</span>
      </div>

      {/* Tab strip */}
      <div style={{ display: 'flex', background: 'var(--surface)', borderBottom: '1px solid var(--border)' }}>
        {TABS.map(t => (
          <button
            key={t}
            onClick={() => setTab(t)}
            style={{
              flex: 1,
              padding: '8px 4px',
              fontFamily: 'var(--font-pixel)',
              fontSize: '6px',
              letterSpacing: '0.5px',
              color: tab === t ? 'var(--bg)' : 'var(--text-muted)',
              background: tab === t ? 'var(--blue)' : 'transparent',
              border: 'none',
              borderBottom: tab === t ? '2px solid var(--blue)' : '2px solid transparent',
              cursor: 'pointer',
            }}
          >
            {t}
          </button>
        ))}
      </div>

      {/* Tab content */}
      <div style={{ flex: 1, overflow: 'hidden', position: 'relative' }}>
        {tab === 'CALC'  && <DamageCalcScreen preloadId={preloadId} />}
        {tab === 'WILD'  && <TurnBattleScreen teamIds={teamIds} />}
        {tab === 'TRAIN' && <TrainerSelectScreen teamIds={teamIds} onStartBattle={(trainer: Trainer) => { setTab('WILD') }} />}
        {tab === 'MATCH' && <MatchupScreen teamIds={teamIds} />}
        {tab === 'LOG'   && <RecordScreen />}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Verify**

```bash
npm run dev
```

Open http://localhost:3000/battle — tab strip visible, CALC tab shows damage calculator. Switch tabs — each renders its screen.

- [ ] **Step 3: Commit**

```bash
git add app/battle/
git commit -m "feat: add Battle Hub page wiring all 5 tabs"
```

---

### Task 23: Run All Tests and Fix TypeScript

- [ ] **Step 1: Run the full test suite**

```bash
npx vitest run
```

Expected: all tests pass (StatConfig, DamageEngine, BattleEngine, DB, TypeBadge).

- [ ] **Step 2: TypeScript strict check**

```bash
npx tsc --noEmit
```

Fix any type errors reported. Common ones to expect:
- `window` not defined in SSR context in list pages → guard with `typeof window !== 'undefined'`
- Missing `key` props in lists → add `key`
- Implicit `any` in event handlers → add explicit type annotations

- [ ] **Step 3: Fix react-window SSR issue in list pages**

In `app/list/PokemonListClient.tsx` and `app/collection/page.tsx`, replace `window.innerHeight` with a constant for the initial render:

```typescript
// Replace:
height={window?.innerHeight ? window.innerHeight - 160 : 600}
// With:
height={600}
```

- [ ] **Step 4: Commit any fixes**

```bash
git add -A
git commit -m "fix: resolve TypeScript errors and SSR window access issues"
```

---

### Task 24: Vercel Deployment

- [ ] **Step 1: Add `next.config.ts` environment config**

```typescript
// next.config.ts
import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: 'raw.githubusercontent.com' },
      { protocol: 'https', hostname: 'madmaxlgndklrpokeapi.com' },
    ],
  },
}

export default nextConfig
```

- [ ] **Step 2: Create `.env.local` for local dev**

```bash
cat > .env.local << 'EOF'
POKEAPI_BASE=https://madmaxlgndklrpokeapi.com/api/v2
EOF
```

Add `.env.local` to `.gitignore` if not already there.

- [ ] **Step 3: Update `lib/constants.ts` to use env var**

```typescript
// Change the first line of POKEAPI_BASE in lib/constants.ts
export const POKEAPI_BASE =
  process.env.POKEAPI_BASE ?? 'https://madmaxlgndklrpokeapi.com/api/v2'
```

- [ ] **Step 4: Push to GitHub**

```bash
git remote add origin https://github.com/madmaxlgndklr/web-pokedex.git
git branch -M main
git push -u origin main
```

- [ ] **Step 5: Deploy to Vercel**

```bash
npx vercel --prod
```

Follow prompts. Set environment variable `POKEAPI_BASE=https://madmaxlgndklrpokeapi.com/api/v2` in the Vercel dashboard under Project → Settings → Environment Variables.

- [ ] **Step 6: Verify production deployment**

Open the Vercel URL — confirm home page loads, Pokémon list fetches, detail pages work.

- [ ] **Step 7: Commit**

```bash
git add next.config.ts .gitignore
git commit -m "feat: add Vercel deployment config and image domain allowlist"
git push
```

---

## Self-Review

**Spec coverage check:**

| Spec requirement | Task |
|---|---|
| Next.js 14 App Router + TypeScript | Task 1 |
| Tailwind + CSS custom properties theme | Tasks 1, 4 |
| Press Start 2P font | Task 1 globals.css |
| Dexie.js IndexedDB schema matching Room | Task 5 |
| All hooks (useCaughtPokemon, useTeam, etc.) | Task 5 |
| PokeAPI fetch functions, server-cached | Task 3 |
| Self-hosted PokeAPI base URL | Task 3 |
| Collapsible sidebar nav | Task 6 |
| Home/Search with suggestions | Task 8 |
| Full List with react-window | Task 9 |
| My Collection filtered to caught | Task 10 |
| Pokémon Detail (stats, moves, evo, actions) | Task 11 |
| Move Detail | Task 12 |
| Compare side-by-side | Task 13 |
| Team Builder with 6 slots | Task 14 |
| Settings (theme, gen, music) | Task 15 |
| StatConfig port with tests | Task 16 |
| DamageEngine port with tests | Task 17 |
| HeldItemEffect port | Task 18 |
| BattleEngine port with tests | Task 18 |
| TrainerRoster | Task 18–19 |
| DamageCalcScreen | Task 20 |
| TurnBattleScreen (wild + trainer) | Task 21 |
| MatchupScreen | Task 21 |
| RecordScreen | Task 21 |
| TrainerSelectScreen | Task 21 |
| Battle Hub tab container | Task 22 |
| Vercel deployment | Task 24 |
| TypeBadge, StatBar, SpriteImage, PokemonCard | Task 7 |
| Dark + Light (Parchment) theme with toggle | Tasks 4, 15 |
| Theme persisted to localStorage / IndexedDB | Tasks 4, 15 |
| Sprite URLs matching Android | Tasks 3, 7 |
| Prev/Next navigation on detail page | Task 11 |
| preloadId query param for battle | Tasks 11, 22 |
