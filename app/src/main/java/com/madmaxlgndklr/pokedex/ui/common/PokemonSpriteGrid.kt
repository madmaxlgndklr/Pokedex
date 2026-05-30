package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.common.PokemonImage
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun PokemonSpriteGrid(
    pokemon: List<PokemonSummary>,
    onPokemonClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(pokemon, key = { it.id }) { summary ->
            SpriteGridItem(
                id = summary.id,
                name = summary.name,
                onClick = { onPokemonClick(summary.id) }
            )
        }
    }
}

@Composable
private fun SpriteGridItem(id: Int, name: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PokedexDark)
            .clickable(onClickLabel = name, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PokemonImage(
            id = id,
            name = name,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = "#${id.toString().padStart(3, '0')}",
            fontFamily = PressStart2P,
            fontSize = 6.sp,
            color = PokedexCream.copy(alpha = 0.55f)
        )
        Text(
            text = name.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 6.sp,
            color = PokedexCream,
            maxLines = 1
        )
    }
}
