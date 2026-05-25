package com.madmaxlgndklr.pokedex.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.PokemonSpriteGrid
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDarkRed
import com.madmaxlgndklr.pokedex.ui.theme.PokedexGreen
import com.madmaxlgndklr.pokedex.ui.theme.PokedexMetal
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun FullListScreen(
    viewModel: FullListViewModel,
    onPokemonClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().background(PokedexDarkRed)) {
        ListHeader(title = "FULL POKEDEX", onBack = onBack)

        when (val state = uiState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PokedexGreen)
            }
            is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO SIGNAL\n${state.message}",
                    fontFamily = PressStart2P,
                    fontSize = 9.sp,
                    color = PokedexCream,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is UiState.Success -> PokemonSpriteGrid(
                pokemon = state.data,
                onPokemonClick = onPokemonClick
            )
        }
    }
}

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
                .padding(end = 40.dp)  // offset to visually center against BACK button
        )
    }
}
