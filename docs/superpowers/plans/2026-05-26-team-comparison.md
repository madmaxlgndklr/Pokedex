# Team Builder & Comparison Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Team Builder screen (pick up to 6 Pokémon, see combined type coverage) and a Comparison screen (view two Pokémon's stats side by side), both accessible from the detail screen.

**Architecture:** Team state is persisted in `SettingsRepository` as a JSON-encoded `List<Int>` (Pokémon IDs). `TeamViewModel` reads/writes the team and derives type coverage using `typeWeaknesses()` from Plan 1. The Comparison screen is reached from the detail screen via a COMPARE button; the second Pokémon is chosen via a search field on the Comparison screen. Both screens follow the existing `BoxWithConstraints` + `pdex_open_v2` background pattern.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore Preferences, Gson, Room detail cache, existing `typeWeaknesses()` and `typeColor()`, PressStart2P font, existing navigation routes pattern in `AppNavigation.kt`

**Dependency:** Plan 1 (`detail-enhancements`) must be completed first — this plan uses `typeWeaknesses()` from `TypeWeakness.kt` and `shinySpriteUrl()` from `RetrofitClient`.

---

## File Structure

| File | Change |
|------|--------|
| `data/local/SettingsRepository.kt` | Add team storage (JSON list of Int) |
| `ui/team/TeamViewModel.kt` | NEW — team state, add/remove, coverage derivation |
| `ui/team/TeamScreen.kt` | NEW — display team of 6, type coverage chart |
| `ui/compare/CompareViewModel.kt` | NEW — loads two Pokémon details |
| `ui/compare/CompareScreen.kt` | NEW — side-by-side stat comparison |
| `ui/detail/DetailScreen.kt` | Add TEAM and COMPARE buttons |
| `ui/navigation/AppNavigation.kt` | Add `team` and `compare/{id1}` routes |
| `ui/common/BottomNavBar.kt` | Add TEAM destination |

---

### Task 1: Team storage in SettingsRepository

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/data/local/SettingsRepository.kt`

- [ ] **Step 1: Add team preference key and methods**

Read `SettingsRepository.kt` first, then add after the last existing preference:

```kotlin
private val TEAM_KEY = stringPreferencesKey("team")

val team: Flow<List<Int>> = dataStore.data.map { prefs ->
    prefs[TEAM_KEY]
        ?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?: emptyList()
}

suspend fun setTeam(ids: List<Int>) {
    dataStore.edit { prefs ->
        prefs[TEAM_KEY] = ids.take(6).joinToString(",")
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/data/local/SettingsRepository.kt
git commit -m "feat: add team storage (up to 6 Pokémon IDs) to SettingsRepository"
```

---

### Task 2: TeamViewModel

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/team/TeamViewModel.kt`

- [ ] **Step 1: Create `TeamViewModel.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.common.typeWeaknesses
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TeamEntry(val id: Int, val detail: PokemonDetail?)

class TeamViewModel(
    private val repository: PokemonRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val teamIds: StateFlow<List<Int>> = settingsRepo.team
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamEntries: StateFlow<List<TeamEntry>> = teamIds
        .map { ids ->
            ids.map { id ->
                val detail = try { repository.getPokemonDetail(id) } catch (_: Exception) { null }
                TeamEntry(id, detail)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamCoverage: StateFlow<Map<String, Float>> = teamEntries
        .map { entries ->
            val allTypes = entries.mapNotNull { it.detail }.flatMap { it.types }.distinct()
            if (allTypes.isEmpty()) emptyMap()
            else typeWeaknesses(allTypes)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addToTeam(id: Int) {
        val current = teamIds.value
        if (id in current || current.size >= 6) return
        viewModelScope.launch { settingsRepo.setTeam(current + id) }
    }

    fun removeFromTeam(id: Int) {
        viewModelScope.launch { settingsRepo.setTeam(teamIds.value.filter { it != id }) }
    }

    fun isOnTeam(id: Int): Boolean = id in teamIds.value

    companion object {
        fun factory(repository: PokemonRepository, settingsRepo: SettingsRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TeamViewModel(repository, settingsRepo) }
            }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/team/TeamViewModel.kt
git commit -m "feat: add TeamViewModel with team persistence and type coverage derivation"
```

---

### Task 3: TeamScreen

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/team/TeamScreen.kt`

The team screen shows the 6 slots as sprite cards at the top (in the blue strip area), then a combined type weakness/coverage chart below.

- [ ] **Step 1: Create `TeamScreen.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.team

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.ui.common.BottomNavBar
import com.madmaxlgndklr.pokedex.ui.common.NavDestination
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

@Composable
fun TeamScreen(
    viewModel: TeamViewModel,
    onBack: () -> Unit,
    onPokemonClick: (Int) -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateFullList: () -> Unit,
    onNavigateMyCollection: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val teamEntries by viewModel.teamEntries.collectAsState()
    val coverage by viewModel.teamCoverage.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize().swipeBack(onBack)) {
        val sw = maxWidth
        val sh = maxHeight
        val stripTop = sh * 0.36f
        val stripHeight = sh * 0.24f

        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(onClick = onBack, modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        Text(
            text = "MY TEAM",
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = CaughtGold,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.22f)
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )

        // Team slots row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(stripHeight)
                .offset(y = stripTop)
                .padding(horizontal = 8.dp)
        ) {
            for (slot in 0..5) {
                val entry = teamEntries.getOrNull(slot)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(PokedexDark.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .then(
                            if (entry != null) Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPokemonClick(entry.id) } else Modifier
                        )
                ) {
                    if (entry != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = RetrofitClient.spriteUrl(entry.id),
                                contentDescription = entry.detail?.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "#${entry.id.toString().padStart(3, '0')}",
                                fontFamily = PressStart2P,
                                fontSize = 4.sp,
                                color = PokedexCream.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            // Long-press or tap X to remove — use a small X button
                            Text(
                                text = "✕",
                                fontFamily = PressStart2P,
                                fontSize = 6.sp,
                                color = PokedexRed.copy(alpha = 0.7f),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.removeFromTeam(entry.id) }
                            )
                        }
                    } else {
                        Text(
                            text = "${slot + 1}",
                            fontFamily = PressStart2P,
                            fontSize = 8.sp,
                            color = PokedexCream.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Coverage panel
        Column(
            modifier = Modifier
                .offset(x = sw * 0.04f, y = stripTop + stripHeight + 8.dp)
                .fillMaxWidth(0.92f)
                .height(sh * 0.28f)
                .background(PokedexDark.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "TEAM COVERAGE",
                fontFamily = PressStart2P,
                fontSize = 6.sp,
                color = CaughtGold
            )
            Spacer(Modifier.height(6.dp))
            if (coverage.isEmpty()) {
                Text(
                    text = "ADD POKEMON TO SEE COVERAGE",
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = PokedexCream.copy(alpha = 0.4f),
                    lineHeight = 9.sp
                )
            } else {
                listOf(4f, 2f, 0.5f, 0.25f, 0f).forEach { mult ->
                    val entries = coverage.entries.filter { it.value == mult }
                    if (entries.isEmpty()) return@forEach
                    val label = when (mult) {
                        4f    -> "WEAK 4×"
                        2f    -> "WEAK 2×"
                        0.5f  -> "RESIST ½×"
                        0.25f -> "RESIST ¼×"
                        else  -> "IMMUNE 0×"
                    }
                    Text(
                        text = label,
                        fontFamily = PressStart2P,
                        fontSize = 5.sp,
                        color = when (mult) {
                            4f, 2f -> PokedexRed
                            0f     -> GlowBlue
                            else   -> PokedexCream.copy(alpha = 0.5f)
                        }
                    )
                    Spacer(Modifier.height(3.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        entries.forEach { (type, _) ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .background(typeColor(type), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 5.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = type.uppercase().take(3),
                                    fontFamily = PressStart2P,
                                    fontSize = 4.sp,
                                    color = PokedexCream
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                }
            }
        }

        BottomNavBar(
            current = NavDestination.TEAM,
            onNavigateSearch = onNavigateSearch,
            onNavigateFullList = onNavigateFullList,
            onNavigateMyCollection = onNavigateMyCollection,
            onNavigateSettings = onNavigateSettings,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/team/TeamScreen.kt
git commit -m "feat: add TeamScreen with 6 slots and combined type coverage chart"
```

---

### Task 4: CompareViewModel and CompareScreen

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/compare/CompareViewModel.kt`
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/compare/CompareScreen.kt`

The compare screen is reached via `compare/{id1}`. It loads id1's detail immediately and shows a search field to pick the second Pokémon. Once both are loaded, stats are shown side by side.

- [ ] **Step 1: Create `CompareViewModel.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CompareViewModel(
    private val repository: PokemonRepository,
    private val firstId: Int
) : ViewModel() {

    private val _firstState = MutableStateFlow<UiState<PokemonDetail>>(UiState.Loading)
    val firstState: StateFlow<UiState<PokemonDetail>> = _firstState

    private val _secondState = MutableStateFlow<UiState<PokemonDetail>?>(null)
    val secondState: StateFlow<UiState<PokemonDetail>?> = _secondState

    val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _firstState.value = try {
                UiState.Success(repository.getPokemonDetail(firstId))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Failed")
            }
        }
    }

    fun searchSecond() {
        val q = searchQuery.value.trim()
        if (q.isEmpty()) return
        _secondState.value = UiState.Loading
        viewModelScope.launch {
            _secondState.value = try {
                UiState.Success(repository.searchPokemon(q))
            } catch (e: Exception) {
                UiState.Error("Not found")
            }
        }
    }

    companion object {
        fun factory(repository: PokemonRepository, firstId: Int): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { CompareViewModel(repository, firstId) }
            }
    }
}
```

- [ ] **Step 2: Create `CompareScreen.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.compare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.model.PokemonStat
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun CompareScreen(
    viewModel: CompareViewModel,
    onBack: () -> Unit
) {
    val firstState by viewModel.firstState.collectAsState()
    val secondState by viewModel.secondState.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize().swipeBack(onBack)) {
        val sw = maxWidth
        val sh = maxHeight

        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(onClick = onBack, modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        Text(
            text = "COMPARE",
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = CaughtGold,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.22f),
            textAlign = TextAlign.Center
        )

        // Two side-by-side panels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.30f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left — first Pokémon (fixed)
            Box(modifier = Modifier.weight(1f)) {
                when (val s = firstState) {
                    is UiState.Loading -> CircularProgressIndicator(
                        color = GlowBlue,
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                    is UiState.Success -> PokemonCompareCard(
                        detail = s.data,
                        panelWidth = (sw - 24.dp) / 2,
                        highlightColor = GlowBlue
                    )
                    is UiState.Error -> Text(
                        text = "ERROR",
                        fontFamily = PressStart2P,
                        fontSize = 7.sp,
                        color = PokedexRed
                    )
                }
            }

            // Right — second Pokémon (searched)
            Box(modifier = Modifier.weight(1f)) {
                when (val s = secondState) {
                    null -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(PokedexDark.copy(alpha = 0.55f),
                                androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "SEARCH TO\nCOMPARE",
                            fontFamily = PressStart2P,
                            fontSize = 6.sp,
                            color = PokedexCream.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { viewModel.searchQuery.value = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.searchSecond() }),
                            textStyle = TextStyle(
                                fontFamily = PressStart2P,
                                fontSize = 8.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(GlowBlue),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "NAME OR #",
                                            fontFamily = PressStart2P,
                                            fontSize = 6.sp,
                                            color = Color.White.copy(alpha = 0.3f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                    is UiState.Loading -> CircularProgressIndicator(
                        color = GlowBlue,
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                    is UiState.Success -> PokemonCompareCard(
                        detail = s.data,
                        panelWidth = (sw - 24.dp) / 2,
                        highlightColor = CaughtGold
                    )
                    is UiState.Error -> Text(
                        text = "NOT FOUND",
                        fontFamily = PressStart2P,
                        fontSize = 6.sp,
                        color = PokedexRed
                    )
                }
            }
        }
    }
}

@Composable
private fun PokemonCompareCard(
    detail: PokemonDetail,
    panelWidth: Dp,
    highlightColor: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(PokedexDark.copy(alpha = 0.55f),
                androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        AsyncImage(
            model = RetrofitClient.spriteUrl(detail.id),
            contentDescription = detail.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(panelWidth * 0.55f)
        )
        Text(
            text = "#${detail.id.toString().padStart(3, '0')}",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = PokedexCream.copy(alpha = 0.5f)
        )
        Text(
            text = detail.name.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 6.sp,
            color = highlightColor,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        detail.stats.forEach { stat ->
            CompareStatRow(stat = stat, width = panelWidth - 12.dp, accentColor = highlightColor)
        }
    }
}

@Composable
private fun CompareStatRow(stat: PokemonStat, width: Dp, accentColor: androidx.compose.ui.graphics.Color) {
    val label = when (stat.name) {
        "hp" -> "HP"; "attack" -> "ATK"; "defense" -> "DEF"
        "special-attack" -> "SpA"; "special-defense" -> "SpD"; "speed" -> "SPD"
        else -> stat.name.uppercase().take(3)
    }
    Column(modifier = Modifier.width(width)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontFamily = PressStart2P,
                fontSize = 5.sp,
                color = PokedexCream,
                modifier = Modifier.width(width * 0.45f)
            )
            Text(
                text = stat.value.toString().padStart(3),
                fontFamily = PressStart2P,
                fontSize = 5.sp,
                color = PokedexCream
            )
        }
        Spacer(Modifier.height(1.dp))
        Box(
            Modifier.fillMaxWidth().height(4.dp)
                .background(PokedexCream.copy(alpha = 0.15f))
        ) {
            Box(
                Modifier.fillMaxWidth(stat.value / 255f).height(4.dp)
                    .background(accentColor)
            )
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/compare/CompareViewModel.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/compare/CompareScreen.kt
git commit -m "feat: add CompareViewModel and CompareScreen for side-by-side stat comparison"
```

---

### Task 5: Wire Team and Compare into navigation + add TEAM and COMPARE buttons to detail screen

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/BottomNavBar.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/detail/DetailScreen.kt`

- [ ] **Step 1: Read BottomNavBar.kt**

Read `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/BottomNavBar.kt` to see the current `NavDestination` enum and button layout.

- [ ] **Step 2: Add TEAM to `NavDestination` and BottomNavBar**

In `BottomNavBar.kt`, add `TEAM` to the `NavDestination` enum and add a TEAM button to the `BottomNavBar` composable. The TEAM button uses `Icons.Filled.Group` (or `Icons.Filled.People` — use whichever is available in `androidx.compose.material.icons`).

Locate the `NavDestination` enum (it likely looks like):
```kotlin
enum class NavDestination { SEARCH, FULL_LIST, MY_COLLECTION, SETTINGS }
```

Replace with:
```kotlin
enum class NavDestination { SEARCH, FULL_LIST, MY_COLLECTION, TEAM, SETTINGS }
```

Add a TEAM `NavButton` to the `BottomNavBar` Row, matching the existing button style. Add an `onNavigateTeam: () -> Unit` parameter to `BottomNavBar`.

- [ ] **Step 3: Add routes and composables in AppNavigation**

Add to the `Routes` object:
```kotlin
const val TEAM = "team"
const val COMPARE = "compare/{firstId}"
fun compare(id: Int) = "compare/$id"
```

Add team composable inside `NavHost`:
```kotlin
composable(Routes.TEAM) {
    val vm: TeamViewModel = viewModel(
        factory = TeamViewModel.factory(repo, settingsRepo)
    )
    TeamScreen(
        viewModel = vm,
        onBack = { navController.popBackStack() },
        onPokemonClick = { id -> navController.navigate(Routes.detail(id)) },
        onNavigateSearch = { navController.navigate(Routes.SEARCH) { popUpTo(Routes.TEAM) { inclusive = true } } },
        onNavigateFullList = { navController.navigate(Routes.FULL_LIST) { popUpTo(Routes.TEAM) { inclusive = true } } },
        onNavigateMyCollection = { navController.navigate(Routes.MY_COLLECTION) { popUpTo(Routes.TEAM) { inclusive = true } } },
        onNavigateSettings = { navController.navigate(Routes.SETTINGS) }
    )
}
```

Add compare composable inside `NavHost`:
```kotlin
composable(
    route = Routes.COMPARE,
    arguments = listOf(navArgument("firstId") { type = NavType.IntType })
) { backStackEntry ->
    val firstId = backStackEntry.arguments?.getInt("firstId") ?: return@composable
    val vm: CompareViewModel = viewModel(
        factory = CompareViewModel.factory(repo, firstId)
    )
    CompareScreen(
        viewModel = vm,
        onBack = { navController.popBackStack() }
    )
}
```

Update every `BottomNavBar` call in `AppNavigation` to pass `onNavigateTeam = { navController.navigate(Routes.TEAM) }`.

Add `TeamViewModel`, `TeamScreen`, `CompareViewModel`, `CompareScreen` imports to `AppNavigation.kt`.

- [ ] **Step 4: Add TEAM and COMPARE buttons to DetailScreen**

In `DetailScreen.kt`, add `onNavigateTeam: () -> Unit` and `onCompare: () -> Unit` parameters to `DetailScreen`.

Update `DetailScreen` call in AppNavigation:
```kotlin
DetailScreen(
    viewModel = vm,
    onBack = { navController.popBackStack() },
    onNavigatePrev = { ... },  // existing
    onNavigateNext = { ... },  // existing
    onLoadingChanged = { ... }, // existing
    onEvolutionClick = { id -> navController.navigate(Routes.detail(id)) },
    onNavigateTeam = { navController.navigate(Routes.TEAM) },
    onCompare = { id -> navController.navigate(Routes.compare(id)) }
)
```

In `DetailScreen`, update the signature and pass through to `DetailContent`:
```kotlin
@Composable
fun DetailScreen(
    viewModel: PokemonDetailViewModel,
    onBack: () -> Unit,
    onNavigatePrev: () -> Unit,
    onNavigateNext: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onEvolutionClick: (Int) -> Unit,
    onNavigateTeam: () -> Unit,
    onCompare: (Int) -> Unit
)
```

In `DetailContent`, add `onNavigateTeam`, `onCompare`, `teamViewModel` (needs access to check isOnTeam). Actually, to avoid threading TeamViewModel through many layers, pass `isOnTeam: Boolean` and `onToggleTeam: () -> Unit` as simple lambdas from AppNavigation using a shared TeamViewModel.

Simpler approach: in AppNavigation, create a single `teamVm` at the NavHost level:

```kotlin
val teamVm: TeamViewModel = viewModel(factory = TeamViewModel.factory(repo, settingsRepo))
```

Then in the detail composable:
```kotlin
val isOnTeam by teamVm.teamIds.collectAsState()
DetailScreen(
    ...
    isOnTeam = vm.pokemonId in isOnTeam.value,  // Note: pokemonId is private — expose it
    onToggleTeam = {
        if (pokemonId in isOnTeam.value) teamVm.removeFromTeam(pokemonId)
        else teamVm.addToTeam(pokemonId)
    },
    onCompare = { id -> navController.navigate(Routes.compare(id)) }
)
```

Note: `PokemonDetailViewModel.pokemonId` is currently private. Add a public getter:
```kotlin
val pokemonId: Int get() = pokemonIdInternal
```
Or rename the constructor parameter to expose it:
```kotlin
class PokemonDetailViewModel(
    private val repository: PokemonRepository,
    val pokemonId: Int  // was private
) : ViewModel()
```

In `DetailContent`, add two action buttons in the top-right area (next to the caught star):

```kotlin
// Team button
IconButton(
    onClick = onToggleTeam,
    modifier = Modifier.offset(x = sw - 76.dp, y = 2.dp).size(36.dp)
) {
    Icon(
        imageVector = if (isOnTeam) Icons.Filled.Group else Icons.Outlined.Group,
        contentDescription = if (isOnTeam) "Remove from team" else "Add to team",
        tint = if (isOnTeam) CaughtGold else PokedexCream,
        modifier = Modifier.size(24.dp)
    )
}

// Compare button
IconButton(
    onClick = { onCompare(detail.id) },
    modifier = Modifier.offset(x = sw - 114.dp, y = 2.dp).size(36.dp)
) {
    Icon(
        imageVector = Icons.Filled.CompareArrows,
        contentDescription = "Compare",
        tint = PokedexCream,
        modifier = Modifier.size(24.dp)
    )
}
```

Add imports: `Icons.Filled.Group`, `Icons.Outlined.Group`, `Icons.Filled.CompareArrows` (all in `androidx.compose.material.icons`).

- [ ] **Step 5: Build to verify**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`. If `Icons.Filled.Group` is unavailable, use `Icons.Filled.People` or `Icons.Filled.Star` with a different tint to distinguish from the caught icon.

- [ ] **Step 6: Run full test suite**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/navigation/AppNavigation.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/BottomNavBar.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/detail/DetailScreen.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/detail/PokemonDetailViewModel.kt
git commit -m "feat: wire Team and Compare screens into navigation, add TEAM and COMPARE buttons to detail"
```
