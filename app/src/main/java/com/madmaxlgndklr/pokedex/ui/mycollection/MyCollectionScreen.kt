package com.madmaxlgndklr.pokedex.ui.mycollection

import androidx.compose.foundation.Image
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
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun MyCollectionScreen(
    viewModel: MyCollectionViewModel,
    onPokemonClick: (Int) -> Unit,
    onBack: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateFullList: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val caughtList by viewModel.caughtList.collectAsState()
    val selectedGens by viewModel.selectedGens.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }

    if (showFilterDialog) {
        RegionFilterDialog(
            selectedGens = selectedGens,
            onToggle = { viewModel.toggleGeneration(it) },
            onClear = { viewModel.clearGenerations() },
            onDismiss = { showFilterDialog = false }
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

        // Filter button above the sprite strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = stripTop - 28.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showFilterDialog = true }
        ) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = "Filter",
                tint = if (selectedGens.isNotEmpty()) CaughtGold else PokedexCream,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (selectedGens.isNotEmpty())
                    " FILTER BY REGION (${selectedGens.size})"
                else
                    " FILTER BY REGION",
                fontFamily = PressStart2P,
                fontSize = 6.sp,
                color = if (selectedGens.isNotEmpty()) CaughtGold else PokedexCream
            )
        }

        if (caughtList.isEmpty()) {
            Text(
                text = if (selectedGens.isNotEmpty()) "NONE IN\nTHIS REGION" else "NO POKEMON\nCAUGHT YET",
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
