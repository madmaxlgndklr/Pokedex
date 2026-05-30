package com.madmaxlgndklr.pokedex.ui.move

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.model.Move
import com.madmaxlgndklr.pokedex.ui.common.PokemonCard
import com.madmaxlgndklr.pokedex.ui.common.TypeBadge
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

private val CategoryPhysical = Color(0xFFF08030)
private val CategorySpecial  = Color(0xFF6890F0)
private val CategoryStatus   = Color(0xFFA8A878)

@Composable
fun MoveDetailScreen(
    viewModel: MoveDetailViewModel,
    onBack: () -> Unit,
    onPokemonClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        when (val state = uiState) {
            is UiState.Loading -> CircularProgressIndicator(
                color = GlowBlue,
                modifier = Modifier.align(Alignment.Center)
            )
            is UiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                Text(
                    text = "NO SIGNAL\n\n${state.message}",
                    fontFamily = PressStart2P,
                    fontSize = 10.sp,
                    color = PokedexCream,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::load,
                    colors = ButtonDefaults.buttonColors(containerColor = PokedexRed)
                ) {
                    Text("RETRY", fontFamily = PressStart2P, fontSize = 8.sp, color = PokedexCream)
                }
            }
            is UiState.Success -> MoveDetailContent(
                move = state.data,
                onBack = onBack,
                onPokemonClick = onPokemonClick
            )
        }
    }
}

@Composable
private fun MoveDetailContent(
    move: Move,
    onBack: () -> Unit,
    onPokemonClick: (Int) -> Unit
) {
    val truncatedCount = move.totalLearnersCount - move.learnedBy.size

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.offset(x = (-8).dp).size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
                }

                Spacer(Modifier.height(4.dp))

                // Move name
                Text(
                    text = move.name.uppercase().replace("-", " "),
                    fontFamily = PressStart2P,
                    fontSize = 12.sp,
                    color = PokedexCream,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(10.dp))

                // Type badge + category chip
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeBadge(move.type)
                    CategoryChip(move.category)
                }

                Spacer(Modifier.height(12.dp))

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatBox("PWR", move.power?.toString() ?: "—", Modifier.weight(1f))
                    StatBox("ACC", move.accuracy?.let { "$it%" } ?: "—", Modifier.weight(1f))
                    StatBox("PP", move.pp.toString(), Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                // Effect text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PokedexDark.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = move.effectText,
                        fontFamily = PressStart2P,
                        fontSize = 7.sp,
                        color = PokedexCream,
                        lineHeight = 13.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Section header
                Text(
                    text = "LEARNED BY",
                    fontFamily = PressStart2P,
                    fontSize = 8.sp,
                    color = GlowBlue
                )

                Spacer(Modifier.height(8.dp))
            }
        }

        items(move.learnedBy) { summary ->
            PokemonCard(
                id = summary.id,
                name = summary.name,
                types = emptyList(),
                onClick = { onPokemonClick(summary.id) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (truncatedCount > 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "+ $truncatedCount more Pokémon",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = PokedexCream.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(category: String) {
    val color = when (category.lowercase()) {
        "physical" -> CategoryPhysical
        "special"  -> CategorySpecial
        else       -> CategoryStatus
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = category.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream
        )
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(PokedexDark.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = label,
            fontFamily = PressStart2P,
            fontSize = 6.sp,
            color = GlowBlue
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = PressStart2P,
            fontSize = 9.sp,
            color = PokedexCream,
            textAlign = TextAlign.Center
        )
    }
}
