package com.madmaxlgndklr.pokedex.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.model.EvolutionNode
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.model.PokemonStat
import com.madmaxlgndklr.pokedex.ui.common.CryPlayer
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.common.swipeNavigation
import com.madmaxlgndklr.pokedex.ui.common.typeWeaknesses
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

@Composable
fun DetailScreen(
    viewModel: PokemonDetailViewModel,
    onBack: () -> Unit,
    onNavigatePrev: () -> Unit,
    onNavigateNext: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onEvolutionClick: (Int) -> Unit,
    isOnTeam: Boolean = false,
    onToggleTeam: () -> Unit = {},
    onCompare: (Int) -> Unit = {},
    onMoveClick: (String) -> Unit = {},
    onBattle: (Int) -> Unit = {}
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
                isOnTeam = isOnTeam,
                onBack = onBack,
                onNavigatePrev = onNavigatePrev,
                onNavigateNext = onNavigateNext,
                onToggleCaught = viewModel::toggleCaught,
                onToggleTeam = onToggleTeam,
                onCompare = onCompare,
                onEvolutionClick = onEvolutionClick,
                onMoveClick = onMoveClick,
                onBattle = onBattle
            )
        }
    }
}

private enum class LeftPanel { DEX_ENTRY, ABILITIES, MOVES }

@Composable
private fun DetailContent(
    detail: PokemonDetail,
    isCaught: Boolean,
    isOnTeam: Boolean,
    onBack: () -> Unit,
    onNavigatePrev: () -> Unit,
    onNavigateNext: () -> Unit,
    onToggleCaught: () -> Unit,
    onToggleTeam: () -> Unit,
    onCompare: (Int) -> Unit,
    onEvolutionClick: (Int) -> Unit,
    onMoveClick: (String) -> Unit,
    onBattle: (Int) -> Unit = {}
) {
    var showShiny by remember { mutableStateOf(false) }
    var leftPanel by remember { mutableStateOf(LeftPanel.DEX_ENTRY) }
    var showWeakness by remember { mutableStateOf(false) }
    var showCryButton by remember { mutableStateOf(false) }
    LaunchedEffect(detail.id) {
        CryPlayer.play(detail.name)
    }
    LaunchedEffect(detail.name) {
        showCryButton = CryPlayer.isCryAvailable(detail.name)
    }

    BoxWithConstraints(Modifier.fillMaxSize().swipeNavigation(
        onBack = onBack,
        onSwipeLeft = onNavigateNext,
        onSwipeRight = onNavigatePrev
    )) {
        val sw = maxWidth
        val sh = maxHeight
        val panelShape = RoundedCornerShape(6.dp)
        val panelBg = PokedexDark.copy(alpha = 0.55f)

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

        // Battle button
        IconButton(
            onClick = { onBattle(detail.id) },
            modifier = Modifier.offset(x = sw - 152.dp, y = 2.dp).size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SportsKabaddi,
                contentDescription = "Battle",
                tint = PokedexCream,
                modifier = Modifier.size(22.dp)
            )
        }

        // Compare button
        IconButton(
            onClick = { onCompare(detail.id) },
            modifier = Modifier.offset(x = sw - 114.dp, y = 2.dp).size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                contentDescription = "Compare",
                tint = PokedexCream,
                modifier = Modifier.size(22.dp)
            )
        }

        // Team toggle
        IconButton(
            onClick = onToggleTeam,
            modifier = Modifier.offset(x = sw - 76.dp, y = 2.dp).size(36.dp)
        ) {
            Icon(
                imageVector = if (isOnTeam) Icons.Filled.Group else Icons.Outlined.Group,
                contentDescription = if (isOnTeam) "Remove from team" else "Add to team",
                tint = if (isOnTeam) CaughtGold else PokedexCream,
                modifier = Modifier.size(22.dp)
            )
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

        // Name + types + height/weight
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
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${"%.1f".format(detail.height / 10f)}m / ${"%.1f".format(detail.weight / 10f)}kg",
                fontFamily = PressStart2P,
                fontSize = 6.sp,
                color = PokedexCream.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }

        // Sprite — tappable to toggle shiny
        AsyncImage(
            model = if (showShiny) RetrofitClient.shinySpriteUrl(detail.id) else detail.spriteUrl,
            contentDescription = detail.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(sw * 0.36f)
                .offset(x = sw * 0.32f, y = sh * 0.38f)
                .pointerInput(Unit) {
                    detectTapGestures { showShiny = !showShiny }
                }
        )

        if (showCryButton) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = sw * 0.32f, y = sh * 0.575f)
                    .width(sw * 0.36f)
                    .background(PokedexDark.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .border(1.dp, GlowBlue.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { CryPlayer.play(detail.name) }
                    .padding(horizontal = 4.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "♪ BATTLE CRY",
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = GlowBlue,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Left panel — DEX ENTRY / ABILITIES / MOVES cycle
        Box(
            modifier = Modifier
                .offset(x = sw * 0.02f, y = sh * 0.38f)
                .width(sw * 0.28f)
                .height(sh * 0.26f)
                .background(panelBg, panelShape)
                .padding(6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val panelLabel = when (leftPanel) {
                    LeftPanel.DEX_ENTRY -> "DEX ENTRY"
                    LeftPanel.ABILITIES -> "ABILITIES"
                    LeftPanel.MOVES     -> "MOVES"
                }
                Text(
                    text = panelLabel,
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = GlowBlue,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            leftPanel = when (leftPanel) {
                                LeftPanel.DEX_ENTRY -> LeftPanel.ABILITIES
                                LeftPanel.ABILITIES -> LeftPanel.MOVES
                                LeftPanel.MOVES     -> LeftPanel.DEX_ENTRY
                            }
                        }
                        .padding(bottom = 4.dp)
                )
                when (leftPanel) {
                    LeftPanel.DEX_ENTRY -> Text(
                        text = detail.flavorText,
                        fontFamily = PressStart2P,
                        fontSize = 8.sp,
                        color = PokedexCream,
                        lineHeight = 13.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                    LeftPanel.ABILITIES -> Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        if (detail.abilities.isEmpty()) {
                            Text("—", fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexCream)
                        } else {
                            detail.abilities.forEach { ability ->
                                Text(
                                    text = ability.uppercase().replace("-", "\n"),
                                    fontFamily = PressStart2P,
                                    fontSize = 6.sp,
                                    color = PokedexCream,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }
                    LeftPanel.MOVES -> Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        if (detail.moves.isEmpty()) {
                            Text("—", fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexCream)
                        } else {
                            detail.moves.forEach { move ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onMoveClick(move.name) }
                                        .padding(vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${move.levelLearnedAt.toString().padStart(2)}",
                                        fontFamily = PressStart2P,
                                        fontSize = 5.sp,
                                        color = PokedexCream.copy(alpha = 0.5f),
                                        modifier = Modifier.width(sw * 0.06f)
                                    )
                                    Text(
                                        text = move.name.uppercase().replace("-", " "),
                                        fontFamily = PressStart2P,
                                        fontSize = 5.sp,
                                        color = GlowBlue,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right panel — STATS / WEAKNESS toggle
        Column(
            modifier = Modifier
                .offset(x = sw * 0.70f, y = sh * 0.38f)
                .width(sw * 0.27f)
                .height(sh * 0.26f)
                .background(panelBg, panelShape)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (showWeakness) "WEAKNESS" else "STATS",
                fontFamily = PressStart2P,
                fontSize = 5.sp,
                color = GlowBlue,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showWeakness = !showWeakness }
                    .padding(bottom = 2.dp)
            )
            if (showWeakness) {
                val weaknesses = remember(detail.types) { typeWeaknesses(detail.types) }
                val grouped = weaknesses.entries
                    .sortedByDescending { it.value }
                    .groupBy { it.value }
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    listOf(4f, 2f, 0.5f, 0.25f, 0f).forEach { mult ->
                        val entries = grouped[mult] ?: return@forEach
                        val label = when (mult) {
                            4f    -> "4×"
                            2f    -> "2×"
                            0.5f  -> "½×"
                            0.25f -> "¼×"
                            else  -> "0×"
                        }
                        Text(
                            text = label,
                            fontFamily = PressStart2P,
                            fontSize = 5.sp,
                            color = when (mult) {
                                4f   -> PokedexRed
                                2f   -> CaughtGold
                                0f   -> GlowBlue
                                else -> PokedexCream.copy(alpha = 0.5f)
                            }
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            entries.forEach { (type, _) ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .background(typeColor(type), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 3.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = type.uppercase().take(3),
                                        fontFamily = PressStart2P,
                                        fontSize = 4.sp,
                                        color = PokedexCream
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    detail.stats.forEach { stat ->
                        CompactStatRow(stat = stat, width = sw * 0.27f - 12.dp)
                    }
                }
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
            .background(typeColor(type), RoundedCornerShape(4.dp))
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
            .background(PokedexDark.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
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
