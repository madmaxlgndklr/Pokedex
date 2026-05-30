package com.madmaxlgndklr.pokedex.ui.compare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.common.PokemonImage
import com.madmaxlgndklr.pokedex.model.PokemonStat
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun CompareScreen(
    viewModel: CompareViewModel,
    onBack: () -> Unit
) {
    val firstState by viewModel.firstState.collectAsState()
    val secondState by viewModel.secondState.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize().swipeBack(onBack)) {
        val sw = maxWidth
        val sh = maxHeight

        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(onClick = onBack, modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        Text(
            text = "COMPARE",
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = CaughtGold,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.22f),
            textAlign = TextAlign.Center
        )

        // Two side-by-side panels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.30f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left — first Pokémon (fixed)
            Box(modifier = Modifier.weight(1f)) {
                when (val s = firstState) {
                    is UiState.Loading -> CircularProgressIndicator(
                        color = GlowBlue,
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                    is UiState.Success -> PokemonCompareCard(
                        detail = s.data,
                        panelWidth = (sw - 24.dp) / 2,
                        highlightColor = GlowBlue
                    )
                    is UiState.Error -> Text(
                        text = "ERROR",
                        fontFamily = PressStart2P,
                        fontSize = 7.sp,
                        color = PokedexRed
                    )
                }
            }

            // Right — second Pokémon (searched)
            Box(modifier = Modifier.weight(1f)) {
                when (val s = secondState) {
                    null -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(PokedexDark.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "SEARCH TO\nCOMPARE",
                            fontFamily = PressStart2P,
                            fontSize = 6.sp,
                            color = PokedexCream.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { viewModel.searchQuery.value = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.searchSecond() }),
                            textStyle = TextStyle(
                                fontFamily = PressStart2P,
                                fontSize = 8.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(GlowBlue),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "NAME OR #",
                                            fontFamily = PressStart2P,
                                            fontSize = 6.sp,
                                            color = Color.White.copy(alpha = 0.3f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                    is UiState.Loading -> CircularProgressIndicator(
                        color = GlowBlue,
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                    is UiState.Success -> PokemonCompareCard(
                        detail = s.data,
                        panelWidth = (sw - 24.dp) / 2,
                        highlightColor = CaughtGold
                    )
                    is UiState.Error -> Text(
                        text = "NOT FOUND",
                        fontFamily = PressStart2P,
                        fontSize = 6.sp,
                        color = PokedexRed
                    )
                }
            }
        }
    }
}

@Composable
private fun PokemonCompareCard(
    detail: PokemonDetail,
    panelWidth: Dp,
    highlightColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(PokedexDark.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        PokemonImage(
            id = detail.id,
            name = detail.name,
            contentDescription = detail.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(panelWidth * 0.55f)
        )
        Text(
            text = "#${detail.id.toString().padStart(3, '0')}",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = PokedexCream.copy(alpha = 0.5f)
        )
        Text(
            text = detail.name.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 6.sp,
            color = highlightColor,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        detail.stats.forEach { stat ->
            CompareStatRow(stat = stat, width = panelWidth - 12.dp, accentColor = highlightColor)
        }
    }
}

@Composable
private fun CompareStatRow(stat: PokemonStat, width: Dp, accentColor: Color) {
    val label = when (stat.name) {
        "hp" -> "HP"; "attack" -> "ATK"; "defense" -> "DEF"
        "special-attack" -> "SpA"; "special-defense" -> "SpD"; "speed" -> "SPD"
        else -> stat.name.uppercase().take(3)
    }
    Column(modifier = Modifier.width(width)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontFamily = PressStart2P,
                fontSize = 5.sp,
                color = PokedexCream,
                modifier = Modifier.width(width * 0.45f)
            )
            Text(
                text = stat.value.toString().padStart(3),
                fontFamily = PressStart2P,
                fontSize = 5.sp,
                color = PokedexCream
            )
        }
        Spacer(Modifier.height(1.dp))
        Box(
            Modifier.fillMaxWidth().height(4.dp)
                .background(PokedexCream.copy(alpha = 0.15f))
        ) {
            Box(
                Modifier.fillMaxWidth(stat.value / 255f).height(4.dp)
                    .background(accentColor)
            )
        }
    }
}
