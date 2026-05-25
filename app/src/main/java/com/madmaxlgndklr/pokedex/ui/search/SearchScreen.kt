package com.madmaxlgndklr.pokedex.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDarkRed
import com.madmaxlgndklr.pokedex.ui.theme.PokedexGreen
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPokemonClick: (Int) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { id -> onPokemonClick(id) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexDark)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        Text(
            text = "POKEDEX",
            fontFamily = PressStart2P,
            fontSize = 18.sp,
            color = PokedexRed
        )

        Spacer(Modifier.height(64.dp))

        BasicTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            textStyle = TextStyle(
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream
            ),
            cursorBrush = SolidColor(PokedexCream),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(PokedexDarkRed)
                        .padding(12.dp)
                ) {
                    if (query.isEmpty()) {
                        Text(
                            "NAME OR NUMBER...",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = PokedexCream.copy(alpha = 0.4f)
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (uiState is SearchUiState.Loading) PokedexDarkRed else PokedexRed)
                .clickable(enabled = uiState !is SearchUiState.Loading) { viewModel.search() }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SEARCH",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream
            )
        }

        Spacer(Modifier.height(32.dp))

        when (uiState) {
            is SearchUiState.Loading -> CircularProgressIndicator(color = PokedexGreen)
            is SearchUiState.NotFound -> Text(
                text = "NOT FOUND",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexRed
            )
            else -> {}
        }
    }
}
