package com.madmaxlgndklr.pokedex.ui.mycollection

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.BottomNavBar
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
    onNavigateSearch: () -> Unit,
    onNavigateFullList: () -> Unit,
    onNavigateTeam: () -> Unit = {},
    onNavigateSettings: () -> Unit
) {
    val caughtList by viewModel.caughtList.collectAsState()
    val selectedGens by viewModel.selectedGens.collectAsState()
    val selectedTypes by viewModel.selectedTypes.collectAsState()
    val completionStats by viewModel.completionStats.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }

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

        // Per-gen completion bars below the sprite strip
        if (completionStats.isNotEmpty()) {
            val statsTop = stripTop + stripHeight + 8.dp
            val statsBottom = sh - 76.dp
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statsBottom - statsTop)
                    .offset(y = statsTop)
            ) {
                items(completionStats) { stat ->
                    GenProgressRow(stat)
                }
            }
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
            onNavigateSearch = onNavigateSearch,
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
private fun GenProgressRow(stat: GenCompletion) {
    val fraction = if (stat.total == 0) 0f else stat.caught.toFloat() / stat.total
    val isComplete = stat.caught == stat.total
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stat.gen.label,
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = if (isComplete) CaughtGold else PokedexCream.copy(alpha = 0.7f),
            modifier = Modifier.width(44.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(5.dp)
                    .background(
                        if (isComplete) CaughtGold else GlowBlue.copy(alpha = 0.8f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
        Text(
            text = "${stat.caught}/${stat.total}",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = if (isComplete) CaughtGold else PokedexCream.copy(alpha = 0.55f),
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
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
