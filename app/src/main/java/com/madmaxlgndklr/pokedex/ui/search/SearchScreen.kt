package com.madmaxlgndklr.pokedex.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.common.PokemonCard
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDarkRed
import com.madmaxlgndklr.pokedex.ui.theme.PokedexGreen
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPokemonClick: (Int) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val filteredList by viewModel.filteredList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexGreen)
    ) {
        BasicTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream
            ),
            cursorBrush = SolidColor(PokedexCream),
            decorationBox = { inner ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(PokedexDarkRed)
                        .padding(12.dp)
                ) {
                    if (query.isEmpty()) {
                        Text(
                            "SEARCH...",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = PokedexCream.copy(alpha = 0.5f)
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList, key = { it.id }) { summary ->
                PokemonCard(
                    id = summary.id,
                    name = summary.name,
                    types = emptyList(),
                    onClick = { onPokemonClick(summary.id) }
                )
            }
        }
    }
}
