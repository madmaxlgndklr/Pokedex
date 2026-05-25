package com.madmaxlgndklr.pokedex.ui.mycollection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.common.PokemonSpriteGrid
import com.madmaxlgndklr.pokedex.ui.list.ListHeader
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDarkRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun MyCollectionScreen(
    viewModel: MyCollectionViewModel,
    onPokemonClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val caughtList by viewModel.caughtList.collectAsState()

    Column(Modifier.fillMaxSize().background(PokedexDarkRed)) {
        ListHeader(title = "MY POKEDEX", onBack = onBack)

        if (caughtList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO POKEMON\nCAUGHT YET",
                    fontFamily = PressStart2P,
                    fontSize = 10.sp,
                    color = PokedexCream,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            PokemonSpriteGrid(
                pokemon = caughtList,
                onPokemonClick = onPokemonClick
            )
        }
    }
}
