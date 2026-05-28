package com.madmaxlgndklr.pokedex.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.ui.common.CryPlayer
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

@Composable
fun TurnBattleScreen(
    viewModel: TurnBattleViewModel,
    teamIds: List<Int>,
    onBack: () -> Unit
) {
    val setup by viewModel.setup.collectAsState()
    val battleState by viewModel.battleState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(teamIds) {
        if (setup == null && battleState == null && !isLoading && teamIds.isNotEmpty()) {
            viewModel.loadSetup(teamIds)
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(color = GlowBlue, modifier = Modifier.align(Alignment.Center))
            }
            battleState is BattleState.Ongoing -> {
                val ongoing = battleState as BattleState.Ongoing
                val started = remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!started.value) {
                        started.value = true
                        CryPlayer.play(ongoing.opponent.detail.name)
                    }
                }
                OngoingBattleView(
                    state = ongoing,
                    onMove = { idx ->
                        CryPlayer.play(ongoing.player.detail.name)
                        viewModel.submitMove(idx)
                    },
                    onForfeit = { viewModel.forfeit() }
                )
            }
            battleState != null -> {
                val won = battleState is BattleState.Won
                val log = if (won) (battleState as BattleState.Won).log
                          else (battleState as BattleState.Lost).log
                BattleEndView(
                    won = won,
                    log = log,
                    onRematch = { viewModel.resetToSetup() },
                    onBack = onBack
                )
            }
            setup != null -> {
                val s = setup!!
                BattleSetupView(
                    setup = s,
                    moves = learnableMoves(s.playerDetail, s.level),
                    onLevelChange = { viewModel.setSetupLevel(it) },
                    onToggleMove = { viewModel.toggleSetupMove(it) },
                    onFight = { viewModel.startBattleFromSetup() }
                )
            }
            else -> {
                // No team loaded — shouldn't normally reach here
                Text(
                    "NO TEAM",
                    fontFamily = PressStart2P, fontSize = 8.sp, color = PokedexCream.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun BattleSetupView(
    setup: BattleSetup,
    moves: List<LearnableMove>,
    onLevelChange: (Int) -> Unit,
    onToggleMove: (String) -> Unit,
    onFight: () -> Unit
) {
    val selectedSet = setup.selectedMoveNames.toSet()
    val canFight = selectedSet.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pokemon + level row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            AsyncImage(
                model = RetrofitClient.spriteUrl(setup.playerDetail.id),
                contentDescription = setup.playerDetail.name,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                setup.playerDetail.name.uppercase(),
                fontFamily = PressStart2P, fontSize = 7.sp, color = PokedexCream,
                modifier = Modifier.weight(1f)
            )
            // Level picker
            LevelPicker(level = setup.level, onLevelChange = onLevelChange)
        }

        // Move count header
        Text(
            "MOVES  ${selectedSet.size}/4",
            fontFamily = PressStart2P, fontSize = 5.sp, color = CaughtGold
        )

        // Move list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(moves) { move ->
                MoveRow(
                    move = move,
                    selected = move.name in selectedSet,
                    selectionFull = selectedSet.size >= 4 && move.name !in selectedSet,
                    onToggle = { if (move.available) onToggleMove(move.name) }
                )
            }
        }

        // Fight button
        Button(
            onClick = onFight,
            enabled = canFight,
            colors = ButtonDefaults.buttonColors(
                containerColor = PokedexRed,
                disabledContainerColor = PokedexDark.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "FIGHT!",
                fontFamily = PressStart2P, fontSize = 9.sp,
                color = if (canFight) PokedexCream else PokedexCream.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun LevelPicker(level: Int, onLevelChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "LV",
            fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.5f)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .background(PokedexDark.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                .border(1.dp, GlowBlue.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    onLevelChange(level - 1)
                }
        ) {
            Text("−", fontFamily = PressStart2P, fontSize = 8.sp, color = GlowBlue)
        }
        Text(
            level.toString().padStart(3, '0'),
            fontFamily = PressStart2P, fontSize = 7.sp, color = CaughtGold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .background(PokedexDark.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                .border(1.dp, GlowBlue.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    onLevelChange(level + 1)
                }
        ) {
            Text("+", fontFamily = PressStart2P, fontSize = 8.sp, color = GlowBlue)
        }
    }
}

@Composable
private fun MoveRow(
    move: LearnableMove,
    selected: Boolean,
    selectionFull: Boolean,
    onToggle: () -> Unit
) {
    val dimmed = !move.available
    val textAlpha = when {
        dimmed -> 0.3f
        selectionFull -> 0.5f
        else -> 1f
    }
    val bgColor = when {
        selected -> GlowBlue.copy(alpha = 0.2f)
        dimmed -> PokedexDark.copy(alpha = 0.2f)
        else -> PokedexDark.copy(alpha = 0.45f)
    }
    val borderColor = when {
        selected -> GlowBlue
        dimmed -> PokedexCream.copy(alpha = 0.1f)
        else -> PokedexCream.copy(alpha = 0.15f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .then(
                if (move.available && !selectionFull || selected)
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle)
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Selection indicator
        Text(
            if (selected) "✓" else " ",
            fontFamily = PressStart2P, fontSize = 6.sp,
            color = GlowBlue,
            modifier = Modifier.width(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        // Move name
        Text(
            move.name.uppercase().replace("-", " "),
            fontFamily = PressStart2P, fontSize = 5.sp,
            color = PokedexCream.copy(alpha = textAlpha),
            modifier = Modifier.weight(1f)
        )
        // Level badge or TM badge
        val badgeLabel = if (move.requiredLevel == null) "TM" else "LV${move.requiredLevel}"
        val badgeColor = when {
            move.requiredLevel == null -> GlowBlue.copy(alpha = if (dimmed) 0.2f else 0.7f)
            dimmed -> PokedexRed.copy(alpha = 0.4f)
            else -> PokedexCream.copy(alpha = 0.3f)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(badgeColor, RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(badgeLabel, fontFamily = PressStart2P, fontSize = 4.sp, color = PokedexCream.copy(alpha = textAlpha))
        }
    }
}

@Composable
private fun OngoingBattleView(
    state: BattleState.Ongoing,
    onMove: (Int) -> Unit,
    onForfeit: () -> Unit
) {
    val logState = rememberLazyListState()
    LaunchedEffect(state.log.size) {
        if (state.log.isNotEmpty()) logState.animateScrollToItem(state.log.size - 1)
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CombatantRow(state.opponent, isFront = true)
        CombatantRow(state.player, isFront = false)

        LazyColumn(
            state = logState,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(PokedexDark.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                .padding(6.dp)
        ) {
            items(state.log.takeLast(20)) { line ->
                Text(line, fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, lineHeight = 9.sp)
            }
        }

        // Move buttons
        val moves = state.player.moves
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0..1) {
                        val idx = row * 2 + col
                        val move = moves.getOrNull(idx)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (move != null && move.currentPp > 0) PokedexDark.copy(alpha = 0.65f)
                                    else PokedexDark.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(1.dp, GlowBlue.copy(alpha = if (move != null) 0.4f else 0.15f), RoundedCornerShape(4.dp))
                                .then(
                                    if (move != null && move.currentPp > 0)
                                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onMove(idx) }
                                    else Modifier
                                )
                        ) {
                            if (move != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Type badge
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .background(typeColor(move.type), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(move.type.uppercase().take(3), fontFamily = PressStart2P, fontSize = 3.sp, color = PokedexCream)
                                    }
                                    Text(
                                        move.name.uppercase().replace("-", " "),
                                        fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream,
                                        textAlign = TextAlign.Center, maxLines = 1
                                    )
                                    Text(
                                        "PP ${move.currentPp}/${move.maxPp}",
                                        fontFamily = PressStart2P, fontSize = 4.sp, color = PokedexCream.copy(alpha = 0.5f)
                                    )
                                }
                            } else {
                                Text("—", fontFamily = PressStart2P, fontSize = 7.sp, color = PokedexCream.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
        }

        Text(
            "FORFEIT",
            fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexRed.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.End)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onForfeit)
        )
    }
}

@Composable
private fun CombatantRow(pokemon: BattlePokemon, isFront: Boolean) {
    val hpFraction = pokemon.currentHp.toFloat() / pokemon.maxHp.toFloat()
    val barColor = when {
        hpFraction > 0.5f -> Color(0xFF44DD44)
        hpFraction > 0.2f -> CaughtGold
        else -> PokedexRed
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        AsyncImage(
            model = RetrofitClient.spriteUrl(pokemon.detail.id),
            contentDescription = pokemon.detail.name,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(pokemon.detail.name.uppercase(), fontFamily = PressStart2P, fontSize = 7.sp, color = PokedexCream)
            Text("Lv.${pokemon.level}", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.5f))
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(hpFraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(barColor, RoundedCornerShape(3.dp))
                )
            }
            Text(
                "${pokemon.currentHp}/${pokemon.maxHp}",
                fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun BattleEndView(
    won: Boolean,
    log: List<String>,
    onRematch: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = if (won) "VICTORY!" else "DEFEATED...",
            fontFamily = PressStart2P,
            fontSize = 14.sp,
            color = if (won) CaughtGold else PokedexRed
        )
        log.takeLast(4).forEach { line ->
            Text(line, fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexCream, lineHeight = 10.sp)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onRematch,
            colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("REMATCH", fontFamily = PressStart2P, fontSize = 8.sp, color = PokedexDark)
        }
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = PokedexDark),
            modifier = Modifier.fillMaxWidth().border(1.dp, GlowBlue, RoundedCornerShape(4.dp))
        ) {
            Text("BACK", fontFamily = PressStart2P, fontSize = 8.sp, color = PokedexCream)
        }
    }
}
