package com.madmaxlgndklr.pokedex.ui.mycollection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.BottomNavBar
import com.madmaxlgndklr.pokedex.ui.common.DexSelection
import com.madmaxlgndklr.pokedex.ui.common.Generation
import com.madmaxlgndklr.pokedex.ui.common.NavDestination
import com.madmaxlgndklr.pokedex.ui.common.RegionFilterDialog
import com.madmaxlgndklr.pokedex.ui.common.TypeFilterDialog
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun MyCollectionScreen(
    viewModel: MyCollectionViewModel,
    onPokemonClick: (Int) -> Unit,
    onBack: () -> Unit,
    onNavigateFullList: () -> Unit,
    onNavigateTeam: () -> Unit = {},
    onNavigateSettings: () -> Unit,
    onClosePokedex: () -> Unit = {}
) {
    val caughtList by viewModel.caughtList.collectAsState()
    val selectedGens by viewModel.selectedGens.collectAsState()
    val selectedTypes by viewModel.selectedTypes.collectAsState()
    val nameQuery by viewModel.nameQuery.collectAsState()
    val selectedDex by viewModel.selectedDex.collectAsState()
    val selectedDexStats by viewModel.selectedDexStats.collectAsState()
    val selectedDexDescription by viewModel.selectedDexDescription.collectAsState()

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

        // Filter row above the sprite strip
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
                                    text = "SEARCH CAUGHT",
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

        val hasFilter = selectedGens.isNotEmpty() || selectedTypes.isNotEmpty()
        if (caughtList.isEmpty()) {
            Text(
                text = if (hasFilter) "NONE MATCH\nFILTERS" else "NO POKEMON\nCAUGHT YET",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier
                    .offset(y = stripTop + stripHeight * 0.25f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stripHeight)
                    .offset(y = stripTop)
            ) {
                items(caughtList, key = { it.id }) { pokemon ->
                    SpriteRowItem(pokemon, onClick = { onPokemonClick(pokemon.id) })
                }
            }
        }

        // Dex selector + info panel below the sprite strip
        val statsTop = stripTop + stripHeight + 8.dp
        val statsBottom = sh - 76.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(statsBottom - statsTop)
                .offset(y = statsTop)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Dropdown trigger
            Box {
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
                                viewModel.selectedDex.value = dex
                                showDexDropdown = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = PokedexCream
                            )
                        )
                    }
                }
            }

            // Info panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PokedexDark.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                    .border(1.dp, GlowBlue.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!selectedDexDescription.isNullOrBlank()) {
                    Text(
                        text = selectedDexDescription!!,
                        fontFamily = PressStart2P,
                        fontSize = 5.sp,
                        color = PokedexCream.copy(alpha = 0.8f),
                        lineHeight = 9.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                }

                val (caught, total) = selectedDexStats
                val fraction = if (total == 0) 0f else caught.toFloat() / total
                val isComplete = caught == total && total > 0

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DEX",
                        fontFamily = PressStart2P,
                        fontSize = 5.sp,
                        color = PokedexCream.copy(alpha = 0.6f),
                        modifier = Modifier.width(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(6.dp)
                                .background(
                                    if (isComplete) CaughtGold else GlowBlue.copy(alpha = 0.85f),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                    Text(
                        text = "$caught/$total",
                        fontFamily = PressStart2P,
                        fontSize = 5.sp,
                        color = if (isComplete) CaughtGold else PokedexCream.copy(alpha = 0.7f),
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.End
                    )
                }

                if (total > 0) {
                    val pct = (fraction * 100).toInt()
                    Text(
                        text = "$pct% COMPLETE",
                        fontFamily = PressStart2P,
                        fontSize = 5.sp,
                        color = if (isComplete) CaughtGold else GlowBlue.copy(alpha = 0.9f)
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
            current = NavDestination.MY_COLLECTION,
            onNavigateFullList = onNavigateFullList,
            onNavigateMyCollection = {},
            onNavigateTeam = onNavigateTeam,
            onNavigateSettings = onNavigateSettings,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun SpriteRowItem(pokemon: PokemonSummary, onClick: () -> Unit) {
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
        AsyncImage(
            model = RetrofitClient.spriteUrl(pokemon.id),
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
    }
}
