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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun TurnBattleScreen(
    viewModel: TurnBattleViewModel,
    teamIds: List<Int>,
    onBack: () -> Unit
) {
    val battleState by viewModel.battleState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        if (battleState == null && !isLoading) viewModel.startBattle(teamIds)
    }

    Box(Modifier.fillMaxSize()) {
        when {
            isLoading || battleState == null -> {
                CircularProgressIndicator(
                    color = GlowBlue,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            battleState is BattleState.Ongoing -> {
                OngoingBattleView(
                    state = battleState as BattleState.Ongoing,
                    onMove = { viewModel.submitMove(it) },
                    onForfeit = { viewModel.forfeit() }
                )
            }
            else -> {
                val won = battleState is BattleState.Won
                val log = if (won) (battleState as BattleState.Won).log
                           else (battleState as BattleState.Lost).log
                BattleEndView(
                    won = won,
                    log = log,
                    onRematch = { viewModel.rematch(teamIds) },
                    onBack = onBack
                )
            }
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
        // Opponent
        CombatantRow(state.opponent, isFront = true)

        // Player
        CombatantRow(state.player, isFront = false)

        // Battle log
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

        // Forfeit
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
