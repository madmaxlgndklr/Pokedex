package com.madmaxlgndklr.pokedex.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.model.EvolutionNode
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.model.PokemonStat
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.common.swipeNavigation
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun DetailScreen(
    viewModel: PokemonDetailViewModel,
    onBack: () -> Unit,
    onNavigatePrev: () -> Unit,
    onNavigateNext: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onEvolutionClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isCaught by viewModel.isCaught.collectAsState()

    LaunchedEffect(uiState) { onLoadingChanged(uiState is UiState.Loading) }

    Box(Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is UiState.Loading -> CircularProgressIndicator(
                color = GlowBlue,
                modifier = Modifier.align(Alignment.Center)
            )
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
                onNavigatePrev = onNavigatePrev,
                onNavigateNext = onNavigateNext,
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
    onNavigatePrev: () -> Unit,
    onNavigateNext: () -> Unit,
    onToggleCaught: () -> Unit,
    onEvolutionClick: (Int) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().swipeNavigation(
        onBack = onBack,
        onSwipeLeft = onNavigateNext,
        onSwipeRight = onNavigatePrev
    )) {
        val sw = maxWidth
        val sh = maxHeight
        val panelShape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
        val panelBg = PokedexDark.copy(alpha = 0.55f)

        // Same background as other screens
        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Back button — top left
        IconButton(
            onClick = onBack,
            modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        // Caught toggle — top right
        IconButton(
            onClick = onToggleCaught,
            modifier = Modifier.offset(x = sw - 38.dp, y = 2.dp).size(36.dp)
        ) {
            Icon(
                imageVector = if (isCaught) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = if (isCaught) "Uncatch" else "Catch",
                tint = if (isCaught) CaughtGold else PokedexCream,
                modifier = Modifier.size(24.dp)
            )
        }

        // Name + types — above sprite, moved higher
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.21f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "#${detail.id.toString().padStart(3, '0')} ${detail.name.uppercase()}",
                fontFamily = PressStart2P,
                fontSize = 8.sp,
                color = PokedexCream,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                detail.types.forEach { type -> TypeChip(type) }
            }
        }

        // Sprite — center of screen
        AsyncImage(
            model = detail.spriteUrl,
            contentDescription = detail.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(sw * 0.36f)
                .offset(x = sw * 0.32f, y = sh * 0.38f)
        )

        // Dex entry — left of sprite, scrollable, dark panel
        Box(
            modifier = Modifier
                .offset(x = sw * 0.02f, y = sh * 0.38f)
                .width(sw * 0.28f)
                .height(sh * 0.26f)
                .background(panelBg, panelShape)
                .padding(6.dp)
        ) {
            Text(
                text = detail.flavorText,
                fontFamily = PressStart2P,
                fontSize = 8.sp,
                color = PokedexCream,
                lineHeight = 13.sp,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }

        // Stats — right of sprite, dark panel
        Column(
            modifier = Modifier
                .offset(x = sw * 0.70f, y = sh * 0.38f)
                .width(sw * 0.27f)
                .background(panelBg, panelShape)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            detail.stats.forEach { stat ->
                CompactStatRow(stat = stat, width = sw * 0.27f - 12.dp)
            }
        }

        // Evolution stages — below sprite
        val stages = detail.evolutionChain
        Row(
            horizontalArrangement = Arrangement.spacedBy(sw * 0.02f),
            modifier = Modifier
                .offset(x = sw * 0.03f, y = sh * 0.72f)
                .width(sw * 0.94f)
        ) {
            for (i in 0..2) {
                val node = stages.getOrNull(i)?.members?.firstOrNull()
                EvoStageBox(
                    label = "STAGE ${i + 1}",
                    node = node,
                    onEvolutionClick = onEvolutionClick,
                    modifier = Modifier.weight(1f).height(sh * 0.18f)
                )
            }
        }
    }
}

@Composable
private fun TypeChip(type: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(typeColor(type), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = type.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = PokedexCream
        )
    }
}

@Composable
private fun CompactStatRow(stat: PokemonStat, width: androidx.compose.ui.unit.Dp) {
    val label = abbreviateStat(stat.name)
    Column(modifier = Modifier.width(width)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontFamily = PressStart2P,
                fontSize = 6.sp,
                color = PokedexCream,
                modifier = Modifier.width(width * 0.45f)
            )
            Text(
                text = stat.value.toString().padStart(3),
                fontFamily = PressStart2P,
                fontSize = 6.sp,
                color = PokedexCream
            )
        }
        Spacer(Modifier.height(1.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(PokedexCream.copy(alpha = 0.2f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(stat.value / 255f)
                    .height(5.dp)
                    .background(PokedexRed)
            )
        }
    }
}

@Composable
private fun EvoStageBox(
    label: String,
    node: EvolutionNode?,
    onEvolutionClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .background(PokedexDark.copy(alpha = 0.45f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .then(
                if (node != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onEvolutionClick(node.id) } else Modifier
            )
            .padding(4.dp)
    ) {
        Text(
            text = label,
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = PokedexCream.copy(alpha = 0.7f)
        )
        if (node != null) {
            AsyncImage(
                model = RetrofitClient.spriteUrl(node.id),
                contentDescription = node.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(52.dp)
            )
            Text(
                text = node.name.uppercase(),
                fontFamily = PressStart2P,
                fontSize = 5.sp,
                color = PokedexCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun abbreviateStat(name: String) = when (name) {
    "hp" -> "HP"
    "attack" -> "ATK"
    "defense" -> "DEF"
    "special-attack" -> "Sp.A"
    "special-defense" -> "Sp.D"
    "speed" -> "SPD"
    else -> name.uppercase().take(4)
}

private fun typeColor(type: String) = when (type.lowercase()) {
    "fire" -> androidx.compose.ui.graphics.Color(0xFFD62828)
    "water" -> androidx.compose.ui.graphics.Color(0xFF2196F3)
    "grass" -> androidx.compose.ui.graphics.Color(0xFF388E3C)
    "electric" -> androidx.compose.ui.graphics.Color(0xFFFBC02D)
    "psychic" -> androidx.compose.ui.graphics.Color(0xFFAB47BC)
    "ice" -> androidx.compose.ui.graphics.Color(0xFF26C6DA)
    "dragon" -> androidx.compose.ui.graphics.Color(0xFF1565C0)
    "dark" -> androidx.compose.ui.graphics.Color(0xFF37474F)
    "fairy" -> androidx.compose.ui.graphics.Color(0xFFEC407A)
    "fighting" -> androidx.compose.ui.graphics.Color(0xFFBF360C)
    "poison" -> androidx.compose.ui.graphics.Color(0xFF7B1FA2)
    "ground" -> androidx.compose.ui.graphics.Color(0xFF8D6E63)
    "flying" -> androidx.compose.ui.graphics.Color(0xFF5C6BC0)
    "bug" -> androidx.compose.ui.graphics.Color(0xFF7CB342)
    "rock" -> androidx.compose.ui.graphics.Color(0xFF78909C)
    "ghost" -> androidx.compose.ui.graphics.Color(0xFF4527A0)
    "steel" -> androidx.compose.ui.graphics.Color(0xFF546E7A)
    "normal" -> androidx.compose.ui.graphics.Color(0xFF78909C)
    else -> androidx.compose.ui.graphics.Color(0xFF607D8B)
}
