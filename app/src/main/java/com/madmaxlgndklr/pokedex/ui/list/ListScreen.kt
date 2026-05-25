package com.madmaxlgndklr.pokedex.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.PokemonCard
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexGreen
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun ListScreen(
    viewModel: PokemonListViewModel,
    onPokemonClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexGreen)
    ) {
        when (val state = uiState) {
            is UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                text = "NO SIGNAL\n\n${state.message}",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
            is UiState.Success -> PokemonGrid(
                pokemon = state.data,
                onPokemonClick = onPokemonClick
            )
        }
    }
}

@Composable
private fun PokemonGrid(
    pokemon: List<PokemonSummary>,
    onPokemonClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pokemon, key = { it.id }) { summary ->
            PokemonCard(
                id = summary.id,
                name = summary.name,
                types = emptyList(),
                onClick = { onPokemonClick(summary.id) }
            )
        }
    }
}
