# Discovery & Collection Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add five features to the list, search, and collection screens: Surprise Me (random Pokémon navigation), type filter alongside the existing region filter, sort options on Full List, search history (last 10 searches), and a per-generation completion tracker on My Dex.

**Architecture:** Each feature is self-contained. Type filter reuses the `RegionFilterDialog` pattern with a new `TypeFilterDialog`. Sort order is a simple enum added to `FullListViewModel`. Search history stores the last 10 query strings in `SettingsRepository` (DataStore). Completion tracker is a pure derived state in `MyCollectionViewModel` using `Generation.idRange` and the full Pokémon list count.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore Preferences, existing `Generation` enum, existing `typeColor()` from `ui/theme/Color.kt`, PressStart2P font

---

## File Structure

| File | Change |
|------|--------|
| `ui/search/SearchViewModel.kt` | Add `randomPokemon()`, search history read/write |
| `ui/search/SearchScreen.kt` | Add Surprise Me button, search history display |
| `data/local/SettingsRepository.kt` | Add `searchHistory` preference (JSON list) |
| `ui/common/TypeFilterDialog.kt` | NEW — dialog for selecting types to filter |
| `ui/list/FullListViewModel.kt` | Add type filter state + sort order state |
| `ui/list/FullListScreen.kt` | Add type filter button + sort button |
| `ui/mycollection/MyCollectionViewModel.kt` | Add type filter state + completion stats |
| `ui/mycollection/MyCollectionScreen.kt` | Add type filter button + completion display |

---

### Task 1: Surprise Me button

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/search/SearchViewModel.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/search/SearchScreen.kt`

- [ ] **Step 1: Add `randomPokemon()` to SearchViewModel**

Add this function inside `SearchViewModel`:

```kotlin
fun randomPokemon() {
    if (_uiState.value is SearchUiState.Loading) return
    _uiState.value = SearchUiState.Loading
    viewModelScope.launch {
        try {
            val id = (1..1025).random()
            val detail = repository.searchPokemon(id.toString())
            _query.value = ""
            _uiState.value = SearchUiState.Idle
            _navigationEvent.emit(detail.id)
        } catch (e: Exception) {
            _uiState.value = SearchUiState.NotFound
        }
    }
}
```

- [ ] **Step 2: Add the Surprise Me button to SearchScreen**

In `SearchScreen`, inside the `BoxWithConstraints` block, add after the `BottomNavBar` and before the `BootDiagnosticOverlay`:

```kotlin
// Surprise Me — shown only when interactive and not loading
if (phase == PokedexPhase.INTERACTIVE && uiState !is SearchUiState.Loading) {
    Text(
        text = "? SURPRISE ME",
        fontFamily = PressStart2P,
        fontSize = 6.sp,
        color = GlowBlue.copy(alpha = 0.8f),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 76.dp)
            .alpha(contentAlpha)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { viewModel.randomPokemon() }
    )
}
```

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/search/SearchViewModel.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/search/SearchScreen.kt
git commit -m "feat: add Surprise Me button that navigates to a random Pokémon"
```

---

### Task 2: Search history — storage

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/data/local/SettingsRepository.kt`

- [ ] **Step 1: Read the current SettingsRepository**

Read `app/src/main/java/com/madmaxlgndklr/pokedex/data/local/SettingsRepository.kt` to see existing structure before editing.

- [ ] **Step 2: Add search history key and methods**

Add to `SettingsRepository` after the existing `musicOnLaunch` preference:

```kotlin
private val SEARCH_HISTORY_KEY = stringPreferencesKey("search_history")

val searchHistory: Flow<List<String>> = dataStore.data.map { prefs ->
    prefs[SEARCH_HISTORY_KEY]
        ?.split("|||")
        ?.filter { it.isNotBlank() }
        ?: emptyList()
}

suspend fun addSearchHistory(query: String) {
    dataStore.edit { prefs ->
        val current = prefs[SEARCH_HISTORY_KEY]
            ?.split("|||")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val updated = (listOf(query) + current.filter { it != query }).take(10)
        prefs[SEARCH_HISTORY_KEY] = updated.joinToString("|||")
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/data/local/SettingsRepository.kt
git commit -m "feat: add search history storage (last 10 entries) to SettingsRepository"
```

---

### Task 3: Search history — ViewModel and UI

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/search/SearchViewModel.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/search/SearchScreen.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/navigation/AppNavigation.kt`

**Context:** `SearchViewModel` currently takes only `repository: PokemonRepository`. It needs `settingsRepo: SettingsRepository` added to save/read search history.

- [ ] **Step 1: Update `SearchViewModel` to accept settingsRepo and expose history**

Add `settingsRepo` to the constructor and add history state:

```kotlin
class SearchViewModel(
    private val repository: PokemonRepository,
    private val settingsRepo: com.madmaxlgndklr.pokedex.data.local.SettingsRepository
) : ViewModel() {
```

Add after existing StateFlow declarations:

```kotlin
val searchHistory: StateFlow<List<String>> = settingsRepo.searchHistory
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

In the `search()` function, after `_navigationEvent.emit(detail.id)`:

```kotlin
settingsRepo.addSearchHistory(q)
```

Update the factory:

```kotlin
companion object {
    fun factory(
        repository: PokemonRepository,
        settingsRepo: com.madmaxlgndklr.pokedex.data.local.SettingsRepository
    ): ViewModelProvider.Factory = viewModelFactory {
        initializer { SearchViewModel(repository, settingsRepo) }
    }
}
```

- [ ] **Step 2: Update AppNavigation to pass settingsRepo to SearchViewModel**

In `AppNavigation.kt`, find:

```kotlin
val vm: SearchViewModel = viewModel(factory = SearchViewModel.factory(repo))
```

Replace with:

```kotlin
val vm: SearchViewModel = viewModel(factory = SearchViewModel.factory(repo, settingsRepo))
```

- [ ] **Step 3: Add search history display to SearchScreen**

In `SearchScreen`, add `val searchHistory by viewModel.searchHistory.collectAsState()` after existing state declarations.

Add the history display inside `BoxWithConstraints`, shown when query is empty and phase is INTERACTIVE:

```kotlin
if (phase == PokedexPhase.INTERACTIVE && query.isEmpty() && searchHistory.isNotEmpty()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = sh * 0.67f)
            .padding(horizontal = 24.dp)
            .alpha(contentAlpha),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        searchHistory.take(5).forEach { item ->
            Text(
                text = "▸ ${item.uppercase()}",
                fontFamily = PressStart2P,
                fontSize = 6.sp,
                color = PokedexCream.copy(alpha = 0.55f),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.onQueryChange(item)
                    viewModel.search()
                }
            )
        }
    }
}
```

- [ ] **Step 4: Build to verify**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/search/SearchViewModel.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/search/SearchScreen.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/navigation/AppNavigation.kt
git commit -m "feat: show last 5 search history entries on home screen, tap to re-search"
```

---

### Task 4: Type filter dialog

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/TypeFilterDialog.kt`

The type filter dialog mirrors `RegionFilterDialog` but lists all 18 Pokémon types as colored chips instead of checkboxes. Tapping a type toggles its selection.

- [ ] **Step 1: Create `TypeFilterDialog.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

val ALL_POKEMON_TYPES = listOf(
    "normal","fire","water","electric","grass","ice","fighting","poison",
    "ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"
)

@Composable
fun TypeFilterDialog(
    selectedTypes: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PokedexDark,
        title = {
            Text(
                "FILTER BY TYPE",
                fontFamily = PressStart2P,
                fontSize = 8.sp,
                color = CaughtGold
            )
        },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ALL_POKEMON_TYPES.forEach { type ->
                    val selected = type in selectedTypes
                    Box(
                        modifier = Modifier
                            .background(
                                typeColor(type).copy(alpha = if (selected) 1f else 0.35f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggle(type) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = type.uppercase(),
                            fontFamily = PressStart2P,
                            fontSize = 6.sp,
                            color = PokedexCream.copy(alpha = if (selected) 1f else 0.55f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Text(
                "DONE",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = GlowBlue,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        },
        dismissButton = {
            Text(
                "CLEAR",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = PokedexCream.copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClear() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/TypeFilterDialog.kt
git commit -m "feat: add TypeFilterDialog composable with colored type chip toggles"
```

---

### Task 5: Type filter + sort options in FullListViewModel and FullListScreen

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/list/FullListViewModel.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/list/FullListScreen.kt`

**Context:** Type filtering requires knowing each Pokémon's types, but `PokemonSummary` only has `id` and `name`. Type filtering on the list screen requires either loading type data for all Pokémon (expensive) or filtering via the detail cache. The pragmatic approach: filter by type is applied using the **detail cache** — only Pokémon whose cached detail includes the selected type(s) are shown; uncached Pokémon pass through. This means type filter works best after a full sync.

Sort options: `NUMBER` (default, ascending id), `NAME` (alphabetical).

- [ ] **Step 1: Add `SortOrder` enum and type filter + sort to `FullListViewModel`**

Add `SortOrder` enum before the class:

```kotlin
enum class SortOrder { NUMBER, NAME }
```

Add inside `FullListViewModel`:

```kotlin
private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
val selectedTypes: StateFlow<Set<String>> = _selectedTypes

private val _sortOrder = MutableStateFlow(SortOrder.NUMBER)
val sortOrder: StateFlow<SortOrder> = _sortOrder
```

Replace the existing `filteredState` with a version that also applies type filter and sort:

```kotlin
val filteredState: StateFlow<UiState<List<PokemonSummary>>> =
    combine(_uiState, _selectedGens, _selectedTypes, _sortOrder) { state, gens, types, sort ->
        when (state) {
            is UiState.Success -> {
                var list = state.data
                if (gens.isNotEmpty()) list = list.filter { p -> gens.any { p.id in it.idRange } }
                if (types.isNotEmpty()) {
                    list = list.filter { p ->
                        val cached = repository.getCachedTypes(p.id)
                        cached == null || cached.any { it in types }
                    }
                }
                list = when (sort) {
                    SortOrder.NUMBER -> list.sortedBy { it.id }
                    SortOrder.NAME   -> list.sortedBy { it.name }
                }
                UiState.Success(list)
            }
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
```

Add toggle/clear functions:

```kotlin
fun toggleType(type: String) {
    _selectedTypes.value = _selectedTypes.value.toMutableSet().apply {
        if (!add(type)) remove(type)
    }
}

fun clearTypes() { _selectedTypes.value = emptySet() }

fun setSortOrder(order: SortOrder) { _sortOrder.value = order }
```

Note: `repository.getCachedTypes(id)` is a new method needed on `PokemonRepository` — see next step.

- [ ] **Step 2: Add `getCachedTypes` to PokemonRepository**

Add a `suspend` function to `PokemonRepository`. The `combine` lambda in Flow is a suspend lambda so this can be called directly from it:

```kotlin
suspend fun getCachedTypes(id: Int): List<String>? {
    val entity = detailCacheDao.getById(id) ?: return null
    return try {
        gson.fromJson(entity.detailJson, PokemonDetail::class.java).types
    } catch (e: Exception) {
        null
    }
}
```

- [ ] **Step 3: Add type filter button and sort button to FullListScreen**

In `FullListScreen.kt`, add state and dialog at the top of the composable (after existing `showFilterDialog`):

```kotlin
val selectedTypes by viewModel.selectedTypes.collectAsState()
val sortOrder by viewModel.sortOrder.collectAsState()
var showTypeDialog by remember { mutableStateOf(false) }
var showSortMenu by remember { mutableStateOf(false) }

if (showTypeDialog) {
    TypeFilterDialog(
        selectedTypes = selectedTypes,
        onToggle = { viewModel.toggleType(it) },
        onClear = { viewModel.clearTypes() },
        onDismiss = { showTypeDialog = false }
    )
}
```

Replace the existing region filter `Row` with a two-button row:

```kotlin
Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier = Modifier
        .fillMaxWidth()
        .offset(y = stripTop - 28.dp)
) {
    // Region filter
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null)
            { showFilterDialog = true }
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.FilterList,
            contentDescription = "Region",
            tint = if (selectedGens.isNotEmpty()) CaughtGold else PokedexCream,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = if (selectedGens.isNotEmpty()) " GEN(${selectedGens.size})" else " GEN",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = if (selectedGens.isNotEmpty()) CaughtGold else PokedexCream
        )
    }
    Text(
        text = "|",
        fontFamily = PressStart2P,
        fontSize = 5.sp,
        color = PokedexCream.copy(alpha = 0.3f)
    )
    // Type filter
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null)
            { showTypeDialog = true }
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = if (selectedTypes.isNotEmpty()) "TYPE(${selectedTypes.size})" else "TYPE",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = if (selectedTypes.isNotEmpty()) CaughtGold else PokedexCream
        )
    }
    Text(
        text = "|",
        fontFamily = PressStart2P,
        fontSize = 5.sp,
        color = PokedexCream.copy(alpha = 0.3f)
    )
    // Sort toggle
    Text(
        text = if (sortOrder == SortOrder.NUMBER) "# SORT" else "A SORT",
        fontFamily = PressStart2P,
        fontSize = 5.sp,
        color = PokedexCream,
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                viewModel.setSortOrder(
                    if (sortOrder == SortOrder.NUMBER) SortOrder.NAME else SortOrder.NUMBER
                )
            }
            .padding(horizontal = 8.dp)
    )
}
```

Add missing imports to `FullListScreen.kt`:
```kotlin
import com.madmaxlgndklr.pokedex.ui.common.TypeFilterDialog
import com.madmaxlgndklr.pokedex.ui.list.SortOrder
```

- [ ] **Step 4: Build to verify**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/list/FullListViewModel.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/list/FullListScreen.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/data/repository/PokemonRepository.kt
git commit -m "feat: type filter and sort options (# / A-Z) on Full List screen"
```

---

### Task 6: Type filter in MyCollectionViewModel and MyCollectionScreen

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/mycollection/MyCollectionViewModel.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/mycollection/MyCollectionScreen.kt`

**Context:** `MyCollectionViewModel` already has `_selectedGens`. We add `_selectedTypes` the same way. Type filtering on caught Pokémon also uses `getCachedTypes` since all caught Pokémon have been viewed (and thus cached).

- [ ] **Step 1: Add type filter to MyCollectionViewModel**

Add `_selectedTypes` and update `caughtList` to filter by it. Also pass `repository` as a field (it's currently a constructor parameter but not stored — need to store it):

```kotlin
class MyCollectionViewModel(private val repository: PokemonRepository) : ViewModel() {
    private val _allCaught: StateFlow<List<PokemonSummary>> = repository.getCaughtPokemon()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedGens = MutableStateFlow<Set<Generation>>(emptySet())
    val selectedGens: StateFlow<Set<Generation>> = _selectedGens

    private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes

    val caughtList: StateFlow<List<PokemonSummary>> =
        combine(_allCaught, _selectedGens, _selectedTypes) { list, gens, types ->
            var filtered = list
            if (gens.isNotEmpty()) filtered = filtered.filter { p -> gens.any { p.id in it.idRange } }
            if (types.isNotEmpty()) {
                filtered = filtered.filter { p ->
                    val cached = repository.getCachedTypes(p.id)
                    cached == null || cached.any { it in types }
                }
            }
            filtered.sortedBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleGeneration(gen: Generation) {
        _selectedGens.value = _selectedGens.value.toMutableSet().apply {
            if (!add(gen)) remove(gen)
        }
    }

    fun clearGenerations() { _selectedGens.value = emptySet() }

    fun toggleType(type: String) {
        _selectedTypes.value = _selectedTypes.value.toMutableSet().apply {
            if (!add(type)) remove(type)
        }
    }

    fun clearTypes() { _selectedTypes.value = emptySet() }

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { MyCollectionViewModel(repository) }
        }
    }
}
```

- [ ] **Step 2: Add type filter button to MyCollectionScreen**

Add `val selectedTypes by viewModel.selectedTypes.collectAsState()` and `var showTypeDialog by remember { mutableStateOf(false) }` after existing state.

Add `TypeFilterDialog` invocation alongside the existing `RegionFilterDialog`:

```kotlin
if (showTypeDialog) {
    TypeFilterDialog(
        selectedTypes = selectedTypes,
        onToggle = { viewModel.toggleType(it) },
        onClear = { viewModel.clearTypes() },
        onDismiss = { showTypeDialog = false }
    )
}
```

Replace the filter row with the same two-button layout used in FullListScreen (GEN | TYPE), without the sort button:

```kotlin
Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier = Modifier
        .fillMaxWidth()
        .offset(y = stripTop - 28.dp)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null)
            { showFilterDialog = true }
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.FilterList,
            contentDescription = "Region",
            tint = if (selectedGens.isNotEmpty()) CaughtGold else PokedexCream,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = if (selectedGens.isNotEmpty()) " GEN(${selectedGens.size})" else " GEN",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = if (selectedGens.isNotEmpty()) CaughtGold else PokedexCream
        )
    }
    Text(
        text = "|",
        fontFamily = PressStart2P,
        fontSize = 5.sp,
        color = PokedexCream.copy(alpha = 0.3f)
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null)
            { showTypeDialog = true }
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = if (selectedTypes.isNotEmpty()) "TYPE(${selectedTypes.size})" else "TYPE",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = if (selectedTypes.isNotEmpty()) CaughtGold else PokedexCream
        )
    }
}
```

Add missing imports to `MyCollectionScreen.kt`:
```kotlin
import com.madmaxlgndklr.pokedex.ui.common.TypeFilterDialog
```

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/mycollection/MyCollectionViewModel.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/mycollection/MyCollectionScreen.kt
git commit -m "feat: add type filter to My Dex screen"
```

---

### Task 7: Completion tracker on My Dex

The completion tracker shows how many Pokémon the user has caught per generation — displayed as a small stats bar below the sprite strip.

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/mycollection/MyCollectionViewModel.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/mycollection/MyCollectionScreen.kt`

- [ ] **Step 1: Add completion stats to MyCollectionViewModel**

Add `completionStats` derived state inside `MyCollectionViewModel`, after `caughtList`:

```kotlin
data class GenCompletion(val gen: Generation, val caught: Int, val total: Int)

val completionStats: StateFlow<List<GenCompletion>> =
    _allCaught.map { caught ->
        val caughtIds = caught.map { it.id }.toSet()
        Generation.entries.map { gen ->
            GenCompletion(
                gen = gen,
                caught = caughtIds.count { it in gen.idRange },
                total = gen.idRange.last - gen.idRange.first + 1
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

- [ ] **Step 2: Display completion stats below the sprite strip in MyCollectionScreen**

Add `val completionStats by viewModel.completionStats.collectAsState()` after existing state declarations.

Add the stats panel inside `BoxWithConstraints`, positioned below the strip:

```kotlin
if (completionStats.isNotEmpty()) {
    Column(
        modifier = Modifier
            .offset(x = sw * 0.04f, y = stripTop + stripHeight + 4.dp)
            .fillMaxWidth(0.92f)
            .background(PokedexDark.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        completionStats.forEach { stat ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.gen.label,
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = PokedexCream.copy(alpha = 0.7f),
                    modifier = Modifier.width(sw * 0.18f)
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(PokedexCream.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(stat.caught.toFloat() / stat.total)
                            .fillMaxHeight()
                            .background(
                                if (stat.caught == stat.total) CaughtGold else GlowBlue,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
                Text(
                    text = "${stat.caught}/${stat.total}",
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = if (stat.caught == stat.total) CaughtGold else PokedexCream.copy(alpha = 0.6f),
                    modifier = Modifier.width(sw * 0.18f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
```

Add missing imports to `MyCollectionScreen.kt`:
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import com.madmaxlgndklr.pokedex.ui.mycollection.GenCompletion
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
```

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run all unit tests**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/mycollection/MyCollectionViewModel.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/mycollection/MyCollectionScreen.kt
git commit -m "feat: per-generation completion tracker with progress bars on My Dex screen"
```
