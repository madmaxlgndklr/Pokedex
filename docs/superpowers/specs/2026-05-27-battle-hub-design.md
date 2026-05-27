# Battle Hub — Design Spec

**Date:** 2026-05-27
**Depends on:** `2026-05-27-moves-detail-design.md` (move data infrastructure must exist first)

## Goal

Add a Battle Hub screen with three user-selectable modes: a generation-accurate damage calculator, a 1v1 turn-based battle simulator, and a team type-matchup analyzer. Accessible from the Team screen and from the Pokémon detail screen.

---

## Entry Points & Navigation

### Routes

```
battle?preloadId={pokemonId}
```

`preloadId` is optional. When present, the hub opens on the **CALC** tab with that Pokémon pre-loaded as the attacker.

### Entry Points

| Origin | Opens on tab | Preload |
|---|---|---|
| Team screen → BATTLE button | MATCHUP | none |
| Detail screen → BATTLE button | CALC | detail's Pokémon as attacker |

### Nav Changes

- `TeamScreen.kt` — add BATTLE button, navigates to `battle`
- `DetailScreen.kt` — add BATTLE button, navigates to `battle?preloadId={id}`
- `AppNavigation.kt` — register `battle` route with optional `preloadId` arg

---

## Architecture

Single `BattleHubScreen.kt` renders a tab row (CALC · BATTLE · MATCHUP) and delegates to one of three child composables. Each tab has its own ViewModel. No shared mutable state between tabs — the selected generation is the one exception, stored in `SettingsRepository` so it persists across sessions.

### New Files

| File | Responsibility |
|---|---|
| `ui/battle/BattleHubScreen.kt` | Tab container, tab state, routes `preloadId` to CALC tab |
| `ui/battle/DamageCalcScreen.kt` | CALC tab UI |
| `ui/battle/DamageCalcViewModel.kt` | CALC state — attacker, defender, move, gen, result |
| `ui/battle/TurnBattleScreen.kt` | BATTLE tab UI |
| `ui/battle/TurnBattleViewModel.kt` | Turn resolution, HP tracking, battle log |
| `ui/battle/MatchupScreen.kt` | MATCHUP tab UI |
| `ui/battle/MatchupViewModel.kt` | Derives coverage and weakness sets from two teams |
| `ui/battle/DamageEngine.kt` | Pure functions — gen-specific damage formulas, no UI deps |
| `ui/battle/BattleEngine.kt` | Turn resolution logic — speed, move priority, fainting |

---

## Tab 1 — Damage Calculator

### UI Layout

Top to bottom:

1. **Gen selector** — horizontal scrollable chip row, Gen 1–9. Selection persisted via `SettingsRepository`
2. **Attacker slot**
   - Pokémon search field (same picker pattern as Compare screen)
   - Level input (default 50, range 1–100)
   - Nature dropdown (Neutral / +Atk / −Atk / +SpA / −SpA / etc. — only attack/sp.atk relevant natures listed)
   - EV row — collapsed by default, tap label to expand. Two fields: Atk EVs and SpA EVs (0–252)
3. **Move selector** — search field; shows attacker's known moves first, falls back to full move search. Fetches move data via `PokemonRepository.getMove()` (established in moves-detail spec)
4. **Defender slot** — same fields as attacker; also shows HP EVs field when expanded (affects damage taken)
5. **CALCULATE** button
6. **Result row** — min damage / average / max, OHKO / 2HKO / 3HKO label, type effectiveness badge (e.g. "2×")

### DamageEngine.kt

Pure Kotlin object with one public entry point:

```kotlin
fun calculate(params: DamageParams): DamageResult
```

`DamageParams` holds: gen, level, attackStat, defenseStat, basePower, moveCategory, attackerTypes, moveType, stab, effectiveness, nature, criticalHit=false.

`DamageResult` holds: min, max, average (all as Int), effectivenessLabel.

Gen-specific formula variants:

| Gen | Physical/Special split | Move category source | Random factor | Notes |
|---|---|---|---|---|
| 1 | None (all use Atk/Def) | N/A | 217–255 | Badge boosts omitted |
| 2–3 | By move **type** (not category) | Type determines split | 85–100% | Special split introduced |
| 4–5 | By move **category** field | DTO `damage_class` | 85–100% | Physical/special finally per-move |
| 6–9 | Same as Gen 4–5 | DTO `damage_class` | 85–100% | Fairy type in type chart |

Effectiveness uses `TypeWeakness.typeWeaknesses()` from existing `TypeWeakness.kt`, filtered for the relevant gen's type chart (Gen 1 has no Steel/Dark/Fairy).

Min/max computed by substituting the random factor bounds. Average uses midpoint.

---

## Tab 2 — Turn-based Battle

### Scope

1v1 single battle. Player picks one Pokémon from their saved team. Opponent is one random Pokémon drawn from the full cached list at the same level. No switching mid-battle.

### UI Layout

```
[ Opponent name + level ]
[ Opponent sprite (front)            ]
[ ████████████████░░░░  HP 80/100   ]

[ ████████░░░░░░░░░░░░  HP 40/100   ]
[ Player sprite (back)               ]
[ Player name + level                ]

[ Battle log (last 4 lines, scrollable) ]

[ MOVE 1  PP:10 ] [ MOVE 2  PP:15  ]
[ MOVE 3  PP:5  ] [ MOVE 4  PP:10  ]
                         [ FORFEIT ]
```

Back sprite: uses the back sprite URL from the PokeAPI sprite response fields (same local instance already serves these — same pattern as front sprites with `back` variant).

### BattleEngine.kt

Pure Kotlin object. Entry point:

```kotlin
fun resolveTurn(playerAction: MoveAction, opponentAction: MoveAction, state: BattleState): BattleState
```

Turn order: higher Speed goes first. Ties broken by `Random.nextBoolean()`.

Damage via `DamageEngine.calculate()` using the gen stored in `SettingsRepository`.

AI move selection: picks the move with the highest base power that still has PP > 0. Never picks status moves (AI has no status move knowledge — keeps it playable without complex logic).

Fainting: when HP ≤ 0, battle ends. `BattleState` transitions to `Won` or `Lost`. UI shows result overlay with REMATCH button (re-rolls opponent, resets HP and PP) and BACK button.

### TurnBattleViewModel

Holds `BattleState` as `StateFlow`. Exposes:
- `submitMove(moveIndex: Int)` — resolves player + AI turn, updates state
- `forfeit()` — transitions to `Lost`
- `rematch()` — resets state with new random opponent

---

## Tab 3 — Team Matchup Analyzer

### UI Layout

Two columns side by side (split view):

```
[ YOUR TEAM        ] [ OPPONENT TEAM    ]
[ sprite sprite    ] [ sprite sprite    ]
[ sprite sprite    ] [ + tap to add     ]
[ sprite sprite    ]
                   [ SWAP ]

[ COVERAGE         ] [ COVERAGE         ]
[ Fire  Water  ... ] [ Grass  Ice   ... ]

[ WEAKNESSES       ] [ WEAKNESSES       ]
[ Rock×3  Ice×2    ] [ Fire×4  Elec×2   ]
```

**Your team** — auto-populated from `SettingsRepository` team JSON (same source as Team Builder).

**Opponent team** — empty slots shown as tappable `+` boxes. Tapping opens the existing Pokémon search picker. Up to 6 Pokémon.

**SWAP** button — swaps left and right columns so the user can analyze from the other team's perspective.

### Derivation Logic (MatchupViewModel)

**Offensive coverage** — for each Pokémon in a team, take its two types. Collect all types those types hit super-effectively (using `typeWeaknesses()` inverted). Union across the team, deduplicated.

**Defensive weaknesses** — for each Pokémon, call `typeWeaknesses()` for its types. Collect all types with multiplier > 1.0. Group by type, count occurrences across the team. Display sorted by count descending.

No API calls needed — all derivation is from cached type data and the existing `TypeWeakness.kt`.

---

## Settings

`SettingsRepository` gets one new preference:

```kotlin
val selectedGen: Flow<Int>           // default 5 (Gen 5 formula is the competitive standard)
suspend fun setGen(gen: Int)
```

---

## Testing

- `DamageEngineTest.kt` — verifies known damage values for each gen formula variant (use published damage calc references for ground truth)
- `BattleEngineTest.kt` — verifies turn order (speed), fainting detection, AI move selection
- `MatchupViewModelTest.kt` — verifies coverage and weakness derivation for known team compositions

---

## Out of Scope

- Full 6v6 team switching mid-battle
- Status conditions (burn, paralysis, sleep, etc.)
- Weather / terrain effects
- Held items
- Mega / Z-move / Dynamax
- Online / multiplayer
- Move PP depletion for the AI
