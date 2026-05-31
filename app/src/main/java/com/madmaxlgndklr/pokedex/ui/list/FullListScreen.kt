package com.madmaxlgndklr.pokedex.ui.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.BottomNavBar
import com.madmaxlgndklr.pokedex.ui.common.PokemonImage
import com.madmaxlgndklr.pokedex.ui.common.DexSelection
import com.madmaxlgndklr.pokedex.ui.common.Generation
import com.madmaxlgndklr.pokedex.ui.common.NavDestination
import com.madmaxlgndklr.pokedex.ui.common.RegionFilterDialog
import com.madmaxlgndklr.pokedex.ui.common.TypeFilterDialog
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexMetal
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun FullListScreen(
    viewModel: FullListViewModel,
    onPokemonClick: (Int) -> Unit,
    onBack: () -> Unit,
    onNavigateMyCollection: () -> Unit,
    onNavigateTeam: () -> Unit = {},
    onNavigateSettings: () -> Unit,
    onClosePokedex: () -> Unit = {}
) {
    val uiState by viewModel.filteredState.collectAsState()
    val selectedGens by viewModel.selectedGens.collectAsState()
    val selectedTypes by viewModel.selectedTypes.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val caughtIds by viewModel.caughtIds.collectAsState()
    val nameQuery by viewModel.nameQuery.collectAsState()
    val selectedDex by viewModel.selectedDex.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var showDexDropdown by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    if (showFilterDialog) {
        RegionFilterDialog(
            selectedGens = selectedGens,
            onToggle = { viewModel.toggleGeneration(it) },
            onClear = { viewModel.clearGenerations() },
            onDismiss = { showFilterDialog = false }
        )
    }

    if (showTypeDialog) {
        TypeFilterDialog(
            selectedTypes = selectedTypes,
            onToggle = { viewModel.toggleType(it) },
            onClear = { viewModel.clearTypes() },
            onDismiss = { showTypeDialog = false }
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize().swipeBack(onBack)) {
        val sw = maxWidth
        val sh = maxHeight

        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        val stripTop = sh * 0.36f
        val stripHeight = sh * 0.32f

        // Filter/sort row above the sprite strip
        if (showSearch) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = stripTop - 28.dp)
                    .padding(horizontal = 12.dp)
            ) {
                BasicTextField(
                    value = nameQuery,
                    onValueChange = { viewModel.setNameQuery(it) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                    textStyle = TextStyle(
                        fontFamily = PressStart2P,
                        fontSize = 7.sp,
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(GlowBlue),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (nameQuery.isEmpty()) {
                                Text(
                                    text = "SEARCH NAME OR #",
                                    fontFamily = PressStart2P,
                                    fontSize = 6.sp,
                                    color = PokedexCream.copy(alpha = 0.35f)
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close search",
                    tint = PokedexCream.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            viewModel.setNameQuery("")
                            showSearch = false
                            keyboard?.hide()
                        }
                )
            }
        } else {
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
                Text(text = "|", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.3f))
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
                Text(text = "|", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.3f))
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
                Text(text = "|", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.3f))
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = if (nameQuery.isNotBlank()) CaughtGold else PokedexCream,
                    modifier = Modifier
                        .size(12.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null)
                        { showSearch = true }
                )
            }
        }

        when (val state = uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    color = GlowBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(22.dp)
                        .offset(x = sw * 0.5f - 11.dp, y = stripTop + stripHeight * 0.5f - 11.dp)
                )
            }
            is UiState.Error -> {
                Text(
                    text = "NO SIGNAL\n${state.message}",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = PokedexCream,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = stripTop + stripHeight * 0.3f)
                        .padding(horizontal = 32.dp)
                )
            }
            is UiState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(stripHeight)
                        .offset(y = stripTop)
                ) {
                    items(state.data, key = { it.id }) { pokemon ->
                        SpriteRowItem(
                            pokemon = pokemon,
                            isCaught = pokemon.id in caughtIds,
                            onToggleCaught = { viewModel.toggleCaught(pokemon.id, pokemon.name) },
                            onClick = { onPokemonClick(pokemon.id) }
                        )
                    }
                    if (selectedDex is DexSelection.Regional) {
                        item {
                            val allCaught = state.data.isNotEmpty() && state.data.all { it.id in caughtIds }
                            SelectAllItem(
                                allCaught = allCaught,
                                onClick = { viewModel.selectAllVisible() }
                            )
                        }
                    }
                }
            }
        }

        // Dex selector dropdown below sprite strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = stripTop + stripHeight + 8.dp)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PokedexDark.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .border(1.dp, GlowBlue.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showDexDropdown = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = selectedDex.label,
                    fontFamily = PressStart2P,
                    fontSize = 6.sp,
                    color = CaughtGold
                )
                Icon(
                    imageVector = if (showDexDropdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = CaughtGold,
                    modifier = Modifier.size(14.dp)
                )
            }
            DropdownMenu(
                expanded = showDexDropdown,
                onDismissRequest = { showDexDropdown = false },
                modifier = Modifier
                    .background(PokedexDark)
                    .border(1.dp, GlowBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            ) {
                val dexOptions: List<DexSelection> =
                    listOf(DexSelection.National) + Generation.entries.map { DexSelection.Regional(it) }
                dexOptions.forEach { dex ->
                    val isSelected = dex == selectedDex
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = dex.label,
                                fontFamily = PressStart2P,
                                fontSize = 5.sp,
                                color = if (isSelected) CaughtGold else PokedexCream
                            )
                        },
                        onClick = {
                            viewModel.setSelectedDex(dex)
                            showDexDropdown = false
                        },
                        colors = MenuDefaults.itemColors(textColor = PokedexCream)
                    )
                }
            }
        }

        // Red I/O power button — between the time and sound indicators on the arc
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = sw * 0.5f - 20.dp, y = sh * 0.14f - 20.dp)
                .size(40.dp)
                .background(PokedexDark.copy(alpha = 0.72f), CircleShape)
                .border(1.5.dp, Color(0xFFCC0000).copy(alpha = 0.80f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClosePokedex
                )
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = "Close Pokédex",
                tint = Color(0xFFFF2222),
                modifier = Modifier.size(22.dp)
            )
        }

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        BottomNavBar(
            current = NavDestination.FULL_LIST,
            onNavigateFullList = {},
            onNavigateMyCollection = onNavigateMyCollection,
            onNavigateTeam = onNavigateTeam,
            onNavigateSettings = onNavigateSettings,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun SelectAllItem(allCaught: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 80.dp, height = 140.dp)
            .border(1.dp, CaughtGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (allCaught) "✓" else "★",
                fontFamily = PressStart2P,
                fontSize = 18.sp,
                color = CaughtGold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (allCaught) "DESEL\nALL" else "SELECT\nALL",
                fontFamily = PressStart2P,
                fontSize = 5.sp,
                color = CaughtGold,
                textAlign = TextAlign.Center,
                lineHeight = 8.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun SpriteRowItem(
    pokemon: PokemonSummary,
    isCaught: Boolean,
    onToggleCaught: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PokemonImage(
            id = pokemon.id,
            name = pokemon.name,
            contentDescription = pokemon.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(96.dp)
        )
        Text(
            text = "#${pokemon.id.toString().padStart(3, '0')}",
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = PokedexCream.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
        Text(
            text = pokemon.name.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = PokedexCream,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Checkbox(
            checked = isCaught,
            onCheckedChange = { onToggleCaught() },
            colors = CheckboxDefaults.colors(
                checkedColor = CaughtGold,
                uncheckedColor = PokedexCream.copy(alpha = 0.5f),
                checkmarkColor = PokedexDark
            ),
            modifier = Modifier.size(20.dp)
        )
    }
}

// Used by MyCollectionScreen
@Composable
internal fun ListHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PokedexMetal)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "< BACK",
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = PokedexCream,
            modifier = Modifier.clickable(onClickLabel = "Back", onClick = onBack)
        )
        Text(
            text = title,
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = PokedexCream,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(end = 40.dp)
        )
    }
}
