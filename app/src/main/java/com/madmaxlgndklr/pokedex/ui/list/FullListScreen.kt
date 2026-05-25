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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.BottomNavBar
import com.madmaxlgndklr.pokedex.ui.common.NavDestination
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
    onNavigateSearch: () -> Unit,
    onNavigateMyCollection: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val caughtIds by viewModel.caughtIds.collectAsState()

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
            current = NavDestination.FULL_LIST,
            onNavigateSearch = onNavigateSearch,
            onNavigateFullList = {},
            onNavigateMyCollection = onNavigateMyCollection,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
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
