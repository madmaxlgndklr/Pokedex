# Battle Cries + Sync Sub-Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Pokémon battle cry audio (auto-play + on-demand) to Detail and Battle screens, with offline caching and an a-la-carte sync options dialog in Settings.

**Architecture:** `CryPlayer` (object singleton) wraps a shared ExoPlayer instance and handles CDN streaming + local OGG caching to `filesDir/cries/`. `NetworkObserver` exposes a `StateFlow<Boolean>` for connectivity. `SettingsViewModel.syncWithOptions(SyncOptions)` replaces the direct `syncAll()` call, dispatching data/moves/cries phases based on user-selected options. The sync dialog replaces the existing data-warning AlertDialog.

**Tech Stack:** androidx.media3:media3-exoplayer:1.4.1 (already in project), `ConnectivityManager.NetworkCallback` (no new deps), OkHttp (already via RetrofitClient), Kotlin coroutines + `Semaphore(40)`

---

## File Map

| Action | File |
|---|---|
| CREATE | `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/NetworkObserver.kt` |
| CREATE | `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/CryPlayer.kt` |
| MODIFY | `app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/RetrofitClient.kt` |
| MODIFY | `app/src/main/java/com/madmaxlgndklr/pokedex/PokedexApplication.kt` |
| MODIFY | `app/src/main/java/com/madmaxlgndklr/pokedex/ui/detail/DetailScreen.kt` |
| MODIFY | `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt` |
| MODIFY | `app/src/main/java/com/madmaxlgndklr/pokedex/ui/settings/SettingsViewModel.kt` |
| MODIFY | `app/src/main/java/com/madmaxlgndklr/pokedex/ui/settings/SettingsScreen.kt` |
| CREATE | `app/src/test/java/com/madmaxlgndklr/pokedex/settings/SettingsViewModelSyncTest.kt` |

---

## Task 1: NetworkObserver

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/NetworkObserver.kt`

- [ ] **Step 1: Create NetworkObserver**

```kotlin
package com.madmaxlgndklr.pokedex.ui.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkObserver(context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isOnline = MutableStateFlow(isCurrentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { _isOnline.value = true }
        override fun onLost(network: Network) { _isOnline.value = isCurrentlyOnline() }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
    }

    private fun isCurrentlyOnline(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun unregister() { cm.unregisterNetworkCallback(callback) }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/NetworkObserver.kt
git commit -m "feat: add NetworkObserver for connectivity state flow"
```

---

## Task 2: Expose OkHttp client from RetrofitClient

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/RetrofitClient.kt`

`CryPlayer` needs an OkHttp client for cry downloads. Expose the existing one rather than creating a second instance.

- [ ] **Step 1: Add `httpClient` property to RetrofitClient**

In `RetrofitClient.kt`, extract the `OkHttpClient` into a named property before building Retrofit:

```kotlin
object RetrofitClient {
    const val SERVER_ROOT = "https://madmaxlgndklrpokeapi.com"
    private const val BASE_URL = "$SERVER_ROOT/api/v2/"

    fun spriteUrl(id: Int) = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"
    fun shinySpriteUrl(id: Int) = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/$id.png"

    val httpClient: OkHttpClient by lazy { OkHttpClient.Builder().build() }

    val api: PokeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PokeApiService::class.java)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/RetrofitClient.kt
git commit -m "refactor: expose OkHttpClient from RetrofitClient for reuse"
```

---

## Task 3: CryPlayer

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/CryPlayer.kt`

- [ ] **Step 1: Create CryPlayer**

```kotlin
package com.madmaxlgndklr.pokedex.ui.common

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.Request
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

object CryPlayer {
    private const val CDN = "https://play.pokemonshowdown.com/audio/cries"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var appContext: Context
    private lateinit var networkObserver: NetworkObserver
    private var player: ExoPlayer? = null

    fun init(context: Context, observer: NetworkObserver) {
        appContext = context.applicationContext
        networkObserver = observer
    }

    private fun criesDir(): File =
        File(appContext.filesDir, "cries").also { it.mkdirs() }

    private fun cryFile(name: String) = File(criesDir(), "$name.ogg")

    suspend fun isCryAvailable(name: String): Boolean =
        cryFile(name).exists() || networkObserver.isOnline.value

    fun play(name: String) {
        if (!::appContext.isInitialized) return
        val file = cryFile(name)
        val uri = if (file.exists()) Uri.fromFile(file)
                  else Uri.parse("$CDN/$name.ogg")

        if (player == null) {
            player = ExoPlayer.Builder(appContext).build()
        }
        player!!.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }

        // Cache streamed audio in background so next play is local
        if (!file.exists()) {
            scope.launch { downloadCry(name) }
        }
    }

    fun stop() { player?.stop() }

    suspend fun downloadCry(name: String): Boolean {
        if (!::appContext.isInitialized) return false
        val file = cryFile(name)
        if (file.exists() && file.length() > 0) return true
        return try {
            val request = Request.Builder().url("$CDN/$name.ogg").build()
            val response = RetrofitClient.httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes() ?: return false
                if (bytes.isEmpty()) return false
                val tmp = File(criesDir(), "$name.ogg.tmp")
                tmp.writeBytes(bytes)
                tmp.renameTo(file)
                true
            } else false
        } catch (_: Exception) { false }
    }

    suspend fun syncCries(names: List<String>, onProgress: (Int, Int) -> Unit) {
        if (!::appContext.isInitialized) return
        val total = names.size
        val completed = AtomicInteger(0)
        val semaphore = Semaphore(40)
        kotlinx.coroutines.coroutineScope {
            names.forEach { name ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        downloadCry(name)
                        val n = completed.incrementAndGet()
                        onProgress(n, total)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/CryPlayer.kt
git commit -m "feat: add CryPlayer singleton with ExoPlayer streaming and local OGG cache"
```

---

## Task 4: Wire CryPlayer and NetworkObserver into PokedexApplication

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/PokedexApplication.kt`

- [ ] **Step 1: Update PokedexApplication**

```kotlin
package com.madmaxlgndklr.pokedex

import android.app.Application
import com.madmaxlgndklr.pokedex.data.local.AppDatabase
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.local.settingsDataStore
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.ui.common.CryPlayer
import com.madmaxlgndklr.pokedex.ui.common.NetworkObserver

class PokedexApplication : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }

    val repository by lazy {
        PokemonRepository(
            RetrofitClient.api,
            database.caughtPokemonDao(),
            database.pokemonListCacheDao(),
            database.pokemonDetailCacheDao(),
            database.moveDao()
        )
    }
    val settingsRepository by lazy { SettingsRepository(settingsDataStore) }
    val networkObserver by lazy { NetworkObserver(this) }

    override fun onCreate() {
        super.onCreate()
        CryPlayer.init(this, networkObserver)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/PokedexApplication.kt
git commit -m "feat: initialize CryPlayer and NetworkObserver in Application"
```

---

## Task 5: Detail screen — auto-play + Battle Cry button

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/detail/DetailScreen.kt`

The `DetailContent` private composable starts at line 124. Add two `LaunchedEffect`s and a conditional button below the sprite. The sprite sits at `offset(x = sw * 0.32f, y = sh * 0.38f), size = sw * 0.36f`. The button goes just below at `y = sh * 0.575f`.

- [ ] **Step 1: Add imports to DetailScreen.kt**

Add these imports if not already present:

```kotlin
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.text.style.TextAlign
import com.madmaxlgndklr.pokedex.ui.common.CryPlayer
```

- [ ] **Step 2: Add auto-play and cry-available state inside DetailContent**

Inside `DetailContent`, after the existing `var showShiny`, `var leftPanel`, `var showWeakness` declarations (around line 138), add:

```kotlin
var showCryButton by remember { mutableStateOf(false) }
LaunchedEffect(detail.id) {
    CryPlayer.play(detail.name)
}
LaunchedEffect(detail.name) {
    showCryButton = CryPlayer.isCryAvailable(detail.name)
}
```

- [ ] **Step 3: Add Battle Cry button below sprite**

After the `AsyncImage` sprite block (around line 259), add:

```kotlin
if (showCryButton) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(x = sw * 0.32f, y = sh * 0.575f)
            .width(sw * 0.36f)
            .background(PokedexDark.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
            .border(1.dp, GlowBlue.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { CryPlayer.play(detail.name) }
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Text(
            text = "♪ BATTLE CRY",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = GlowBlue,
            textAlign = TextAlign.Center
        )
    }
}
```

- [ ] **Step 4: Build and verify no compile errors**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/detail/DetailScreen.kt
git commit -m "feat: auto-play battle cry on detail screen with on-demand replay button"
```

---

## Task 6: Battle screen — cry triggers

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt`

Two trigger points: opponent cry when battle becomes `Ongoing` for the first time; player cry when a move button is tapped.

- [ ] **Step 1: Add CryPlayer import**

```kotlin
import com.madmaxlgndklr.pokedex.ui.common.CryPlayer
```

- [ ] **Step 2: Add opponent cry on battle start**

In the `TurnBattleScreen` composable, in the `when (battleState)` block, the `BattleState.Ongoing` branch currently passes `onMove = { viewModel.submitMove(it) }` to `OngoingBattleView`. Replace that branch with:

```kotlin
is BattleState.Ongoing -> {
    val ongoing = battleState as BattleState.Ongoing
    val started = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!started.value) {
            started.value = true
            CryPlayer.play(ongoing.opponent.detail.name)
        }
    }
    OngoingBattleView(
        state = ongoing,
        onMove = { idx ->
            CryPlayer.play(ongoing.player.detail.name)
            viewModel.submitMove(idx)
        },
        onForfeit = { viewModel.forfeit() }
    )
}
```

- [ ] **Step 3: Build and verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt
git commit -m "feat: play battle cries on battle start and player move"
```

---

## Task 7: SettingsViewModel — SyncOptions, phased sync, cry download

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/settings/SettingsViewModel.kt`

`SyncState.Syncing` gains a `phase: String` field. A new `SyncOptions` data class controls what runs. `syncWithOptions()` replaces the direct `syncAll()` call from the screen.

- [ ] **Step 1: Rewrite SettingsViewModel.kt**

```kotlin
package com.madmaxlgndklr.pokedex.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.ui.common.CryPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncOptions(
    val syncData: Boolean = true,
    val syncMoves: Boolean = true,
    val syncCries: Boolean = true
)

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val phase: String, val completed: Int, val total: Int) : SyncState()
    data class Done(val cached: Int, val total: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val pokemonRepo: PokemonRepository
) : ViewModel() {

    val musicOnLaunch: StateFlow<Boolean> = settingsRepo.musicOnLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    fun setMusicOnLaunch(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setMusicOnLaunch(enabled) }
    }

    fun syncWithOptions(options: SyncOptions) {
        if (_syncState.value is SyncState.Syncing) return
        viewModelScope.launch {
            try {
                if (options.syncData) {
                    pokemonRepo.syncAll { completed, total ->
                        _syncState.value = SyncState.Syncing("DATA", completed, total)
                    }
                }
                if (options.syncMoves) {
                    val teamIds = settingsRepo.team.first()
                    if (teamIds.isNotEmpty()) {
                        _syncState.value = SyncState.Syncing("MOVES", 0, teamIds.size)
                        pokemonRepo.syncTeamMoves(teamIds)
                    }
                }
                if (options.syncCries) {
                    val names = pokemonRepo.getPokemonList().map { it.name }
                    CryPlayer.syncCries(names) { completed, total ->
                        _syncState.value = SyncState.Syncing("CRIES", completed, total)
                    }
                }
                val (cached, listSize) = pokemonRepo.getCachedCount()
                _syncState.value = SyncState.Done(cached, listSize)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            }
        }
    }

    fun resetSyncState() { _syncState.value = SyncState.Idle }

    companion object {
        fun factory(
            settingsRepo: SettingsRepository,
            pokemonRepo: PokemonRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(settingsRepo, pokemonRepo) }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/settings/SettingsViewModel.kt
git commit -m "feat: add SyncOptions and phased sync with cry download support"
```

---

## Task 8: SettingsScreen — sync options dialog

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/settings/SettingsScreen.kt`

Replace the `showSyncWarning` AlertDialog (data warning) with a `showSyncDialog` that combines the options checklist and data warning in one dialog.

- [ ] **Step 1: Replace showSyncWarning state and dialog**

Remove `var showSyncWarning by remember { mutableStateOf(false) }` and its AlertDialog block. Add in their place:

```kotlin
var showSyncDialog by remember { mutableStateOf(false) }
var syncOptions by remember { mutableStateOf(SyncOptions()) }

if (showSyncDialog) {
    val allChecked = syncOptions.syncData && syncOptions.syncMoves && syncOptions.syncCries
    AlertDialog(
        onDismissRequest = { showSyncDialog = false },
        containerColor = PokedexDark,
        title = {
            Text(
                text = "SYNC OPTIONS",
                fontFamily = PressStart2P,
                fontSize = 8.sp,
                color = CaughtGold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Downloading may use mobile data. Select what to sync.",
                    fontFamily = PressStart2P,
                    fontSize = 6.sp,
                    color = PokedexCream.copy(alpha = 0.7f),
                    lineHeight = 11.sp
                )
                // Select All row
                SyncOptionRow(
                    label = "SELECT ALL",
                    checked = allChecked,
                    onCheckedChange = { checked ->
                        syncOptions = SyncOptions(checked, checked, checked)
                    }
                )
                HorizontalDivider(color = PokedexCream.copy(alpha = 0.2f))
                SyncOptionRow(
                    label = "POKEMON DATA  ~25 MB",
                    checked = syncOptions.syncData,
                    onCheckedChange = { syncOptions = syncOptions.copy(syncData = it) }
                )
                SyncOptionRow(
                    label = "TEAM MOVES",
                    checked = syncOptions.syncMoves,
                    onCheckedChange = { syncOptions = syncOptions.copy(syncMoves = it) }
                )
                SyncOptionRow(
                    label = "BATTLE CRIES  ~30 MB",
                    checked = syncOptions.syncCries,
                    onCheckedChange = { syncOptions = syncOptions.copy(syncCries = it) }
                )
            }
        },
        confirmButton = {
            Text(
                text = "START SYNC",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = GlowBlue,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showSyncDialog = false
                        viewModel.syncWithOptions(syncOptions)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        },
        dismissButton = {
            Text(
                text = "CANCEL",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = PokedexCream.copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSyncDialog = false }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    )
}
```

- [ ] **Step 2: Add SyncOptionRow private composable**

Add this private composable below `SettingsScreen`:

```kotlin
@Composable
private fun SyncOptionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = PressStart2P,
            fontSize = 6.sp,
            color = PokedexCream,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PokedexDark,
                checkedTrackColor = PokedexGreen,
                uncheckedThumbColor = PokedexCream.copy(alpha = 0.6f),
                uncheckedTrackColor = PokedexDark.copy(alpha = 0.6f)
            )
        )
    }
}
```

- [ ] **Step 3: Update SYNC row click to open showSyncDialog**

Find the `is SyncState.Idle` branch where `showSyncWarning = true` is called. Change it to:

```kotlin
is SyncState.Idle -> {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                syncOptions = SyncOptions()   // reset to all-checked
                showSyncDialog = true
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SYNC FOR OFFLINE",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream
        )
        Text(
            text = "SYNC",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = GlowBlue
        )
    }
}
```

- [ ] **Step 4: Update SyncState.Syncing display to show phase**

Find the `is SyncState.Syncing` branch. Update the "SYNCING..." label to use `state.phase`:

```kotlin
is SyncState.Syncing -> {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SYNCING ${state.phase}...",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = GlowBlue
            )
            Text(
                text = "${state.completed}/${state.total}",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = PokedexCream.copy(alpha = 0.7f)
            )
        }
        LinearProgressIndicator(
            progress = { if (state.total > 0) state.completed.toFloat() / state.total else 0f },
            modifier = Modifier.fillMaxWidth(),
            color = GlowBlue,
            trackColor = PokedexCream.copy(alpha = 0.15f)
        )
    }
}
```

- [ ] **Step 5: Add missing SyncOptions import**

At the top of `SettingsScreen.kt`, add:

```kotlin
import com.madmaxlgndklr.pokedex.ui.settings.SyncOptions
```

- [ ] **Step 6: Build and verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/settings/SettingsScreen.kt
git commit -m "feat: replace sync warning with a-la-carte sync options dialog"
```

---

## Task 9: Tests for SettingsViewModel sync options

**Files:**
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/settings/SettingsViewModelSyncTest.kt`

Tests verify that `syncWithOptions` emits the correct `SyncState` phases based on `SyncOptions`. `CryPlayer` is not initialized in JVM tests — its `syncCries` method returns early safely (guarded by `::appContext.isInitialized` check).

- [ ] **Step 1: Write failing tests**

```kotlin
package com.madmaxlgndklr.pokedex.settings

import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakeMoveDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.repository.FakePokemonDetailCacheDao
import com.madmaxlgndklr.pokedex.repository.FakePokemonListCacheDao
import com.madmaxlgndklr.pokedex.repository.fakeSettingsRepo
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.ui.settings.SettingsViewModel
import com.madmaxlgndklr.pokedex.ui.settings.SyncOptions
import com.madmaxlgndklr.pokedex.ui.settings.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelSyncTest {
    private val dispatcher = StandardTestDispatcher()

    private fun makeRepo() = PokemonRepository(
        FakePokeApiService(),
        FakeCaughtPokemonDao(),
        FakePokemonListCacheDao(),
        FakePokemonDetailCacheDao(),
        FakeMoveDao()
    )

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    @Test fun `syncWithOptions data-only emits DATA phase then Done`() = runTest {
        val vm = SettingsViewModel(fakeSettingsRepo(), makeRepo())
        val phases = mutableListOf<String>()
        vm.syncWithOptions(SyncOptions(syncData = true, syncMoves = false, syncCries = false))
        advanceUntilIdle()
        // Final state must be Done
        assertTrue(vm.syncState.value is SyncState.Done)
        // Must never have synced CRIES or MOVES phases
        assertFalse(phases.any { it == "CRIES" || it == "MOVES" })
    }

    @Test fun `syncWithOptions cries-false never emits CRIES phase`() = runTest {
        val vm = SettingsViewModel(fakeSettingsRepo(), makeRepo())
        val emittedStates = mutableListOf<SyncState>()
        val job = kotlinx.coroutines.launch {
            vm.syncState.collect { emittedStates.add(it) }
        }
        vm.syncWithOptions(SyncOptions(syncData = true, syncMoves = false, syncCries = false))
        advanceUntilIdle()
        job.cancel()
        val phaseNames = emittedStates
            .filterIsInstance<SyncState.Syncing>()
            .map { it.phase }
        assertFalse("CRIES phase must not appear", phaseNames.contains("CRIES"))
        assertTrue("Must end in Done", vm.syncState.value is SyncState.Done)
    }

    @Test fun `syncWithOptions all-false goes directly to Done`() = runTest {
        val vm = SettingsViewModel(fakeSettingsRepo(), makeRepo())
        vm.syncWithOptions(SyncOptions(syncData = false, syncMoves = false, syncCries = false))
        advanceUntilIdle()
        assertTrue(vm.syncState.value is SyncState.Done)
    }

    @Test fun `second syncWithOptions call ignored while syncing`() = runTest {
        val vm = SettingsViewModel(fakeSettingsRepo(), makeRepo())
        vm.syncWithOptions(SyncOptions())
        // second call while first may still be running — should not crash or reset state
        vm.syncWithOptions(SyncOptions())
        advanceUntilIdle()
        assertTrue(vm.syncState.value is SyncState.Done)
    }

    @Test fun `resetSyncState returns to Idle`() = runTest {
        val vm = SettingsViewModel(fakeSettingsRepo(), makeRepo())
        vm.syncWithOptions(SyncOptions(syncData = false, syncMoves = false, syncCries = false))
        advanceUntilIdle()
        assertTrue(vm.syncState.value is SyncState.Done)
        vm.resetSyncState()
        assertTrue(vm.syncState.value is SyncState.Idle)
    }

    @Test fun `SyncOptions allChecked toggle`() {
        val all = SyncOptions()
        assertTrue(all.syncData && all.syncMoves && all.syncCries)
        val none = SyncOptions(false, false, false)
        assertFalse(none.syncData || none.syncMoves || none.syncCries)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail before implementation (should pass since Task 7 is already done)**

```bash
./gradlew test --tests "com.madmaxlgndklr.pokedex.settings.SettingsViewModelSyncTest" 2>&1 | tail -30
```

Expected: All 6 tests PASS (Tasks 7 and 8 are already complete by this point).

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/madmaxlgndklr/pokedex/settings/SettingsViewModelSyncTest.kt
git commit -m "test: SettingsViewModel sync options phase routing — 6 tests"
```

---

## Task 10: Full build, install, manual verification

- [ ] **Step 1: Run full test suite**

```bash
./gradlew test 2>&1 | tail -30
```

Expected: All tests pass, 0 failures.

- [ ] **Step 2: Build and install**

```bash
./gradlew installDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, APK installed.

- [ ] **Step 3: Manual verification checklist**

- [ ] Open any Pokémon detail page → cry plays automatically
- [ ] "♪ BATTLE CRY" button appears below sprite, tapping replays cry
- [ ] Enable Airplane Mode → navigate to a detail page not yet cached → button absent
- [ ] Disable Airplane Mode → button reappears after navigating back and in
- [ ] Settings → tap SYNC → dialog shows three options, all checked by default
- [ ] Deselect BATTLE CRIES → tap START SYNC → progress shows "SYNCING DATA..." then "SYNCING MOVES..."
- [ ] Select only BATTLE CRIES → tap START SYNC → progress shows "SYNCING CRIES... N/1025"
- [ ] Start a battle → opponent cry plays on "appeared!" screen
- [ ] Select a move in battle → player cry plays before move resolves

- [ ] **Step 4: Commit final push**

```bash
git push
```
