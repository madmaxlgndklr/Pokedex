package com.madmaxlgndklr.pokedex.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.common.EvolutionChain
import com.madmaxlgndklr.pokedex.ui.common.StatBar
import com.madmaxlgndklr.pokedex.ui.common.TypeBadge
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun DetailScreen(
    viewModel: PokemonDetailViewModel,
    onBack: () -> Unit,
    onEvolutionClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isCaught by viewModel.isCaught.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexDark)
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
            is UiState.Success -> DetailContent(
                detail = state.data,
                isCaught = isCaught,
                onBack = onBack,
                onToggleCaught = viewModel::toggleCaught,
                onEvolutionClick = onEvolutionClick
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: PokemonDetail,
    isCaught: Boolean,
    onBack: () -> Unit,
    onToggleCaught: () -> Unit,
    onEvolutionClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PokedexRed)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
            }
            Text(
                text = "#${detail.id.toString().padStart(3, '0')} ${detail.name.uppercase()}",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleCaught) {
                Icon(
                    if (isCaught) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isCaught) "Uncatch" else "Catch",
                    tint = if (isCaught) CaughtGold else PokedexCream,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        AsyncImage(
            model = detail.spriteUrl,
            contentDescription = "${detail.name} sprite",
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        ) {
            detail.types.forEach { TypeBadge(it) }
        }

        SectionDivider("STATS")
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            detail.stats.forEach { stat ->
                StatBar(label = stat.name, value = stat.value)
            }
        }

        SectionDivider("MOVES")
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            detail.moves.take(20).forEach { move ->
                Text(
                    text = "LV${move.levelLearnedAt.toString().padStart(3)} ${move.name.replace('-', ' ').uppercase()}",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = PokedexCream
                )
            }
        }

        if (detail.evolutionChain.size > 1) {
            SectionDivider("EVOLUTION")
            EvolutionChain(
                stages = detail.evolutionChain,
                onPokemonClick = onEvolutionClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        SectionDivider("POKÉDEX ENTRY")
        Text(
            text = detail.flavorText,
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = PokedexCream,
            lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionDivider(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f), color = PokedexCream.copy(alpha = 0.3f))
        Text(
            text = "  $label  ",
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = PokedexCream.copy(alpha = 0.7f)
        )
        HorizontalDivider(Modifier.weight(1f), color = PokedexCream.copy(alpha = 0.3f))
    }
}
