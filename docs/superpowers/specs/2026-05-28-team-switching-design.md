# Team Switching Design

**Date:** 2026-05-28
**Status:** Approved
**Scope:** Sub-project E-1 — 6v6 battles with full switching mechanics

---

## Goal

Replace the current 1v1 battle engine with a 6v6 system supporting both faint-forced and voluntary switching, per-battle slot overrides, and a type-aware opponent AI that switches strategically.

## Architecture

`BattleState.Ongoing` expands to hold both full teams and active indices. A new `BattleState.PendingSwitch` sealed case models the "game is paused, player must pick a replacement" moment explicitly. `TurnAction` is a new sealed class replacing the implicit `MoveAction`-only pattern, enabling the engine to receive either a move or a switch request as a player action.

The opponent AI is upgraded from `aiPickMove` to `aiPickAction`, which scores team members on type advantage and HP and decides whether attacking or switching is better.

---

## Data Model

### BattleState

```kotlin
sealed class BattleState {
    data class Ongoing(
        val playerTeam: List<BattlePokemon>,
        val playerActiveIndex: Int,
        val opponentTeam: List<BattlePokemon>,
        val opponentActiveIndex: Int,
        val log: List<String>
    ) : BattleState() {
        val player: BattlePokemon get() = playerTeam[playerActiveIndex]
        val opponent: BattlePokemon get() = opponentTeam[opponentActiveIndex]
    }

    data class PendingSwitch(
        val playerTeam: List<BattlePokemon>,
        val playerActiveIndex: Int,
        val opponentTeam: List<BattlePokemon>,
        val opponentActiveIndex: Int,
        val log: List<String>
    ) : BattleState()

    data class Won(val log: List<String>) : BattleState()
    data class Lost(val log: List<String>) : BattleState()
}
```

The `player` and `opponent` convenience properties on `Ongoing` preserve call-site compatibility for code that only needs the active Pokémon.

### TurnAction

```kotlin
sealed class TurnAction {
    data class UseMove(val move: BattleMove) : TurnAction()
    data class SwitchTo(val targetIndex: Int) : TurnAction()
}
```

Replaces `MoveAction`. The opponent AI always produces a `TurnAction`; the player UI produces one per turn.

---

## Turn Resolution

`BattleEngine.resolveTurn(playerAction: TurnAction, opponentAction: TurnAction, state: BattleState.Ongoing, gen: Int): BattleState`

### Voluntary switch (player picks SwitchTo)

1. Player's active Pokémon switches out immediately (switch always has priority over moves).
2. Opponent's chosen move attacks the newly active (incoming) Pokémon.
3. If the incoming Pokémon faints from that attack and the player has remaining members → `PendingSwitch`. If no remaining members → `Lost`.
4. Otherwise → `Ongoing` with updated `playerActiveIndex`.

### Both players use moves

Speed-priority resolution (existing logic). After each move resolves:

- **Opponent faints:** AI immediately auto-selects the next team member via `aiForcedSwitch`. If no available members → `Won`. Otherwise update `opponentActiveIndex` and continue.
- **Player faints:** Return `PendingSwitch` immediately. Opponent's queued second move is discarded — you cannot attack a fainted Pokémon before a replacement is sent out.

### PendingSwitch resolution

`BattleEngine.confirmSwitch(newIndex: Int, state: BattleState.PendingSwitch): BattleState`

Validates `newIndex` is in bounds, not the current active index, and the target Pokémon has HP > 0. Returns `Ongoing` with updated `playerActiveIndex`. No opponent attack on switch-in after a faint.

---

## Opponent AI

`BattleEngine.aiPickAction(active: BattlePokemon, opponentActive: BattlePokemon, team: List<BattlePokemon>, gen: Int): TurnAction`

Scores each available (non-fainted, non-active) team member:

```kotlin
private fun switchScore(candidate: BattlePokemon, opponent: BattlePokemon): Float {
    val typeAdvantage = TypeChart.effectiveness(candidate.detail.types, opponent.detail.types)
    val hpFraction = candidate.currentHp.toFloat() / candidate.maxHp
    return typeAdvantage * hpFraction
}
```

Switch conditions (either triggers a switch):
- Current active has a 4× weakness to opponent's best-power move type.
- Current active HP < 25% **and** a bench member scores > 1.5× the current active's score.

Best switch candidate = highest `switchScore` among available members. If no candidate scores better than the current active, use `aiPickMove` (existing logic) and return `UseMove`.

`aiForcedSwitch` (for faint replacement): picks the bench member with the highest `switchScore` against the current opponent.

---

## Per-Battle Team Overrides

### Data

```kotlin
data class SlotOverride(
    val level: Int? = null,
    val statConfig: StatConfig? = null,
    val nature: Nature? = null,
    val heldItem: HeldItem? = null
)
```

`BattleSetup` in `TurnBattleViewModel` gains `teamOverrides: Map<Int, SlotOverride>`. The existing global level/statConfig/nature/heldItem fields remain as defaults applied to any slot without an override.

### Setup UI

The existing single-slot setup controls become the global defaults. The setup screen adds a horizontal team strip above them showing the 6 saved team slots as sprite cards.

Tapping a slot opens a bottom sheet with the same level/stat/nature/item controls pre-populated from the global defaults, letting the user customize that slot. A "Reset to defaults" button in the sheet clears the override for that slot.

The START BATTLE button remains gated on `canStartBattle` (team must have ≥ 1 Pokémon).

### Battle Screen

- Active Pokémon display unchanged.
- Compact 6-slot team strip at the bottom (mini-sprites, fainted = greyed out, active = highlighted).
- "SWITCH" button alongside move buttons — opens team picker bottom sheet showing available members (non-fainted, non-active) with name, HP bar, type badges.
- In `PendingSwitch` state: team picker appears automatically, move buttons hidden, sheet is non-dismissible until a valid pick is made.

---

## Error Handling

- Team with 0 Pokémon: `canStartBattle` returns false; START BATTLE is disabled.
- `playerActiveIndex` / `opponentActiveIndex` out of bounds: clamp to 0 and log a warning (defensive guard; shouldn't occur in normal play).
- `confirmSwitch` called with invalid index (fainted, active, or out-of-range): no-op, return current `PendingSwitch` unchanged.
- Opponent team is built as a list of 1 random Pokémon (preserving existing behaviour). `opponentActiveIndex` starts at 0. All team-based logic works correctly with a 1-member list; gym leaders will supply real multi-member teams in Sub-project E-2.

---

## Testing

All tests are unit tests against `BattleEngine` and `TurnBattleViewModel` — no UI or instrumented tests.

| Test | Assertion |
|------|-----------|
| Voluntary switch | Opponent attacks incoming Pokémon, not the one that switched out |
| Voluntary switch — incoming faints | Returns `PendingSwitch` if remaining team members exist |
| Faint mid-turn | Second move discarded; `PendingSwitch` returned |
| `confirmSwitch` | Returns `Ongoing` with correct `playerActiveIndex` |
| `confirmSwitch` — invalid index | Returns `PendingSwitch` unchanged |
| Blackout | All 6 player HP = 0 → `Lost` |
| Win | All 6 opponent HP = 0 → `Won` |
| AI switch trigger | 4× weakness → AI returns `SwitchTo` |
| AI no-switch | Balanced matchup → AI returns `UseMove` |
| Forced AI switch on faint | Picks best available bench member |

---

## Out of Scope

- Entry hazards (Stealth Rock, Spikes)
- Pursuit trap mechanic
- Switch animations
- Gym leaders / trainer teams (Sub-project E-2)
