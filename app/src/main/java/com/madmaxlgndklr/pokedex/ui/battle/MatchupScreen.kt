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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

@Composable
fun MatchupScreen(
    viewModel: MatchupViewModel,
    yourTeamIds: List<Int>
) {
    val yourTeam by viewModel.yourTeam.collectAsState()
    val opponentTeam by viewModel.opponentTeam.collectAsState()
    val swapped by viewModel.swapped.collectAsState()

    LaunchedEffect(yourTeamIds) {
        if (yourTeam.members.isEmpty()) viewModel.loadYourTeam(yourTeamIds)
    }

    val left = if (swapped) opponentTeam else yourTeam
    val right = if (swapped) yourTeam else opponentTeam
    val leftLabel = if (swapped) "OPPONENT" else "YOUR TEAM"
    val rightLabel = if (swapped) "YOUR TEAM" else "OPPONENT"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TeamColumn(
                label = leftLabel,
                team = left,
                canAddMembers = swapped,
                onAdd = { viewModel.addOpponent(it) },
                onRemove = if (swapped) ({ viewModel.removeOpponent(it) }) else null,
                modifier = Modifier.weight(1f)
            )
            TeamColumn(
                label = rightLabel,
                team = right,
                canAddMembers = !swapped,
                onAdd = { viewModel.addOpponent(it) },
                onRemove = if (!swapped) ({ viewModel.removeOpponent(it) }) else null,
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = { viewModel.swap() },
            colors = ButtonDefaults.buttonColors(containerColor = PokedexDark),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .border(1.dp, GlowBlue, RoundedCornerShape(4.dp))
        ) {
            Text("⇄ SWAP", fontFamily = PressStart2P, fontSize = 7.sp, color = GlowBlue)
        }

        // Coverage panels
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CoveragePanel(label = leftLabel, team = left, modifier = Modifier.weight(1f))
            CoveragePanel(label = rightLabel, team = right, modifier = Modifier.weight(1f))
        }

        // Weakness panels
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            WeaknessPanel(label = leftLabel, team = left, modifier = Modifier.weight(1f))
            WeaknessPanel(label = rightLabel, team = right, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TeamColumn(
    label: String,
    team: TeamMatchup,
    canAddMembers: Boolean,
    onAdd: (Int) -> Unit,
    onRemove: ((Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, fontFamily = PressStart2P, fontSize = 5.sp, color = CaughtGold)

        team.members.forEach { pokemon ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = RetrofitClient.spriteUrl(pokemon.id),
                    contentDescription = pokemon.name,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = pokemon.name.uppercase(),
                    fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                if (onRemove != null) {
                    Text(
                        "✕",
                        fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexRed.copy(alpha = 0.7f),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null
                        ) { onRemove(pokemon.id) }
                    )
                }
            }
        }

        if (canAddMembers && team.members.size < 6) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onSearch = {
                    query.toIntOrNull()?.let { onAdd(it); query = "" }
                    keyboard?.hide()
                }),
                textStyle = TextStyle(fontFamily = PressStart2P, fontSize = 6.sp, color = Color.White),
                cursorBrush = SolidColor(GlowBlue),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(
                            "+ DEX #",
                            fontFamily = PressStart2P, fontSize = 5.sp,
                            color = GlowBlue.copy(alpha = 0.5f)
                        )
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    .border(1.dp, GlowBlue.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CoveragePanel(label: String, team: TeamMatchup, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PokedexDark.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("COVERAGE", fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue)
        if (team.offensiveCoverage.isEmpty()) {
            Text("—", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.3f))
        } else {
            TypeTagFlow(team.offensiveCoverage.toList())
        }
    }
}

@Composable
private fun WeaknessPanel(label: String, team: TeamMatchup, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PokedexDark.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("WEAKNESSES", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexRed)
        if (team.defensiveWeaknesses.isEmpty()) {
            Text("—", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.3f))
        } else {
            team.defensiveWeaknesses.entries.take(10).forEach { (type, count) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(typeColor(type), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(type.uppercase().take(3), fontFamily = PressStart2P, fontSize = 4.sp, color = PokedexCream)
                    }
                    Text(
                        "×$count",
                        fontFamily = PressStart2P, fontSize = 5.sp,
                        color = if (count >= 3) PokedexRed else PokedexCream.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeTagFlow(types: List<String>) {
    // Simple wrapping row using manual chunking
    val rows = types.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEach { rowTypes ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                rowTypes.forEach { type ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(typeColor(type), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(type.uppercase().take(3), fontFamily = PressStart2P, fontSize = 4.sp, color = PokedexCream)
                    }
                }
            }
        }
    }
}
