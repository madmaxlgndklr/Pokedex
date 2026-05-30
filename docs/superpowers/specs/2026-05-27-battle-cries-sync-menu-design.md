# Design: Battle Cries + Sync Sub-Menu

**Date:** 2026-05-27
**Branch:** feature/pokedex-app
**Sub-project:** A — Audio

---

## Overview

Add Pokémon battle cry audio to the Detail screen and Battle screen, with offline-first caching and an a-la-carte sync dialog in Settings.

---

## 1. CryPlayer (audio core)

`object CryPlayer` in `ui/common/CryPlayer.kt`.

- Holds a single `ExoPlayer` instance, initialized lazily via `applicationContext` passed from `PokedexApplication.onCreate()`.
- Public API:
  - `fun play(pokemonName: String)` — stops any current cry, then streams or plays locally.
  - `fun stop()` — stops playback without starting another.
  - `suspend fun isCryAvailable(name: String): Boolean` — returns `true` if local file exists OR `NetworkObserver.isOnline` is `true`.
- URL pattern: `https://play.pokemonshowdown.com/audio/cries/{name}.ogg`
  - Names from PokeAPI are already lowercase-hyphenated; no normalization needed for most Pokémon.
  - 404s (e.g. `farfetch-d`, `flabebe`) fail silently — ExoPlayer swallows the error.
- Local file path: `filesDir/cries/{name}.ogg`
  - After a successful stream, the bytes are saved to this path so subsequent plays are local.
- `play()` always calls `stop()` first — overlapping cries never stack.
- No manual caching layer needed; OkHttp's existing connection pool handles CDN requests.

---

## 2. NetworkObserver

`class NetworkObserver(context: Context)` in `ui/common/NetworkObserver.kt`.

- Wraps `ConnectivityManager.NetworkCallback`.
- Exposes `val isOnline: StateFlow<Boolean>`.
- Initialized in `PokedexApplication` alongside `CryPlayer`; passed to ViewModels via their factory.

---

## 3. Detail Screen integration

**File:** `ui/detail/DetailScreen.kt` + `DetailViewModel.kt`

- `DetailViewModel` receives `NetworkObserver` and exposes `isOnline: StateFlow<Boolean>`.
- On screen composition: `LaunchedEffect(pokemonId) { CryPlayer.play(name) }` — auto-plays on every navigation to the screen.
- "Battle Cry" button rendered **only** when `isCryAvailable(name) == true`:
  - Positioned below the Pokémon sprite.
  - Label: "Battle Cry"
  - `onClick`: `CryPlayer.play(name)`
- When offline and not cached, the button is absent entirely (not disabled — absent).

---

## 4. Battle Screen integration

**File:** `ui/battle/TurnBattleScreen.kt` (Composable only — no ViewModel changes)

Trigger points:

| Event | Cry played |
|---|---|
| Battle starts (`BattleState.Ongoing` appears for first time) | Opponent's cry |
| Player taps a move button | Player's cry (before `viewModel.submitMove()`) |

Implementation:
- `LaunchedEffect(Unit)` inside `OngoingBattleView` → `CryPlayer.play(opponent.detail.name)`.
- Move button `onClick` → `CryPlayer.play(player.detail.name)` then `viewModel.submitMove(index)`.

---

## 5. Offline caching behaviour

| State | Button visible | Audio source |
|---|---|---|
| Online, not cached | Yes | Stream from CDN → save to `filesDir/cries/` |
| Online, cached | Yes | Local file |
| Offline, cached | Yes | Local file |
| Offline, not cached | No | — |

---

## 6. Sync sub-menu

**File:** `ui/settings/SettingsScreen.kt` + `SettingsViewModel.kt`

### Dialog

Replaces the current single "Sync All" button behaviour. Tapping "Sync" opens a modal dialog:

```
┌─ Sync Options ──────────────────────┐
│  ☑ Select All                       │
│  ─────────────────────────────────  │
│  ☑ Pokémon data  (~25 MB)           │
│  ☑ Team moves                       │
│  ☑ Battle cries  (~30 MB)           │
│                                     │
│         [Cancel]  [Start Sync]      │
└─────────────────────────────────────┘
```

- "Select All / Deselect All" toggles all items simultaneously.
- Checking any item unchecks "Select All" (if not all are checked).
- All items checked by default each time the dialog opens (no persistence).
- "Start Sync" dismisses the dialog and starts the selected operations.
- "Cancel" dismisses with no action.

### SyncOptions data class

```kotlin
data class SyncOptions(
    val syncData: Boolean = true,
    val syncMoves: Boolean = true,
    val syncCries: Boolean = true
)
```

Local Compose state (`remember { mutableStateOf(SyncOptions()) }`) — not persisted to DataStore.

### Sync execution order

1. If `syncData`: existing `syncAll()` (Pokémon list + detail cache, `Semaphore(40)`)
2. If `syncMoves`: existing `syncTeamMoves()`
3. If `syncCries`: new `syncCries()` — downloads all 1025 OGG files to `filesDir/cries/`
   - Same `Semaphore(40)` parallel pattern as detail sync
   - Progress reported as: `Downloading cries… 412 / 1025`
   - Skips files that already exist locally

### SettingsViewModel additions

- `fun syncWithOptions(options: SyncOptions)` — replaces direct `syncAll()` call from the UI; runs the selected phases in order.
- `syncCries()` private suspend function: iterates 1–1025, skips existing files, downloads via OkHttp (reuses existing client), writes to `filesDir/cries/{name}.ogg`. Name is derived from the pokemon list cache (id → name).

---

## 7. Error handling

- CDN 404 / network error during playback: ExoPlayer fails silently, no crash.
- CDN error during sync download: log and skip that Pokémon, continue batch — do not abort sync.
- Partial file written on interrupted download: delete incomplete file before writing, check file size > 0 after write.

---

## 8. Testing

- `CryPlayerTest`: mock ExoPlayer, verify `play()` calls `stop()` first; verify local file check logic.
- `NetworkObserverTest`: verify `isOnline` emits correct values on network callbacks.
- `SettingsViewModelTest`: verify `syncWithOptions(SyncOptions(syncCries = false))` does not call `syncCries()`; verify progress counter increments.
- Manual: detail screen auto-play, button visibility toggle on/off airplane mode, battle cry triggers.
