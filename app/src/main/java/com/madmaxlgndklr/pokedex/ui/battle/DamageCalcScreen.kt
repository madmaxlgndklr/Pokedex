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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
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

private val GENS = (1..9).map { it }

@Composable
fun DamageCalcScreen(viewModel: DamageCalcViewModel) {
    val state by viewModel.state.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Gen selector
        Text("GENERATION", fontFamily = PressStart2P, fontSize = 6.sp, color = GlowBlue)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(GENS) { gen ->
                val selected = gen == state.gen
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(
                            if (selected) GlowBlue else PokedexDark.copy(alpha = 0.5f),
                            RoundedCornerShape(4.dp)
                        )
                        .border(1.dp, GlowBlue.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.setGen(gen) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "G$gen",
                        fontFamily = PressStart2P,
                        fontSize = 6.sp,
                        color = if (selected) PokedexDark else PokedexCream
                    )
                }
            }
        }

        // Attacker
        SlotPanel(
            label = "ATTACKER",
            gen = state.gen,
            slot = state.attacker,
            isLoading = state.isLoadingAttacker,
            onLoadId = { viewModel.loadAttacker(it) },
            onUpdate = { viewModel.updateAttacker(it) },
            isEvSumValid = viewModel.isEvSumValid(state.attacker),
            keyboard = keyboard
        )

        // Move selector
        Text("MOVE", fontFamily = PressStart2P, fontSize = 6.sp, color = GlowBlue)
        MoveSearchField(
            currentMove = state.selectedMove?.name,
            isLoading = state.isLoadingMove,
            onMoveName = { viewModel.loadMove(it) }
        )

        // Defender
        SlotPanel(
            label = "DEFENDER",
            gen = state.gen,
            slot = state.defender,
            isLoading = state.isLoadingDefender,
            onLoadId = { viewModel.loadDefender(it) },
            onUpdate = { viewModel.updateDefender(it) },
            isEvSumValid = viewModel.isEvSumValid(state.defender),
            keyboard = keyboard
        )

        // Calculate button
        Button(
            onClick = { keyboard?.hide(); viewModel.calculate() },
            enabled = state.attacker.detail != null && state.defender.detail != null && state.selectedMove != null,
            colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CALCULATE", fontFamily = PressStart2P, fontSize = 7.sp, color = PokedexDark)
        }

        // Result
        state.result?.let { result ->
            ResultPanel(result)
        }

        state.error?.let { err ->
            Text(
                text = "ERROR: $err",
                fontFamily = PressStart2P,
                fontSize = 6.sp,
                color = PokedexRed,
                lineHeight = 10.sp
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SlotPanel(
    label: String,
    gen: Int,
    slot: CalcSlot,
    isLoading: Boolean,
    onLoadId: (Int) -> Unit,
    onUpdate: (CalcSlot) -> Unit,
    isEvSumValid: Boolean,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    var showStatConfig by remember { mutableStateOf(false) }
    var showNaturePicker by remember { mutableStateOf(false) }
    var idQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, fontFamily = PressStart2P, fontSize = 6.sp, color = CaughtGold)

        // Pokémon search
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicTextField(
                value = idQuery,
                onValueChange = { idQuery = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onSearch = {
                    idQuery.toIntOrNull()?.let { onLoadId(it) }
                    keyboard?.hide()
                }),
                textStyle = TextStyle(fontFamily = PressStart2P, fontSize = 7.sp, color = Color.White),
                cursorBrush = SolidColor(GlowBlue),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (idQuery.isEmpty()) Text(
                            "ENTER DEX #",
                            fontFamily = PressStart2P, fontSize = 6.sp,
                            color = PokedexCream.copy(alpha = 0.35f)
                        )
                        inner()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .background(PokedexDark.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .border(1.dp, GlowBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
            if (isLoading) CircularProgressIndicator(color = GlowBlue, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        }

        slot.detail?.let { detail ->
            PokemonSlotDisplay(detail)

            // Level
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LVL", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.6f), modifier = Modifier.width(28.dp))
                var lvlText by remember(slot.level) { mutableStateOf(slot.level.toString()) }
                BasicTextField(
                    value = lvlText,
                    onValueChange = { s ->
                        lvlText = s
                        s.toIntOrNull()?.coerceIn(1, 100)?.let { onUpdate(slot.copy(level = it)) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(fontFamily = PressStart2P, fontSize = 7.sp, color = Color.White),
                    cursorBrush = SolidColor(GlowBlue),
                    modifier = Modifier
                        .width(48.dp)
                        .background(PokedexDark.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .border(1.dp, GlowBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            // Stat config toggle
            Text(
                text = if (showStatConfig) "STATS ▲" else "STATS ▼",
                fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showStatConfig = !showStatConfig }
            )
            if (showStatConfig) {
                StatConfigSection(
                    gen = gen,
                    slot = slot,
                    label = if (gen <= 2) "DVs / Stat Exp" else "IVs / EVs",
                    onSlotChange = onUpdate,
                    isEvSumValid = isEvSumValid,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Nature picker (Gen 3+)
            if (gen >= 3) {
                Text(
                    text = if (showNaturePicker) "NATURE ▲" else "NATURE: ${slot.nature.name.uppercase()}",
                    fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showNaturePicker = !showNaturePicker }
                )
                if (showNaturePicker) {
                    NaturePicker(
                        selectedNature = slot.nature,
                        onNatureSelected = { onUpdate(slot.copy(nature = it)); showNaturePicker = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun PokemonSlotDisplay(detail: PokemonDetail) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AsyncImage(
            model = RetrofitClient.spriteUrl(detail.id),
            contentDescription = detail.name,
            modifier = Modifier.size(48.dp)
        )
        Column {
            Text(
                text = detail.name.uppercase(),
                fontFamily = PressStart2P, fontSize = 7.sp, color = PokedexCream
            )
            Text(
                text = detail.types.joinToString(" / ") { it.uppercase() },
                fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue
            )
        }
    }
}

@Composable
private fun MoveSearchField(currentMove: String?, isLoading: Boolean, onMoveName: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotBlank()) { onMoveName(query.trim().lowercase().replace(" ", "-")); keyboard?.hide() }
            }),
            textStyle = TextStyle(fontFamily = PressStart2P, fontSize = 7.sp, color = Color.White),
            cursorBrush = SolidColor(GlowBlue),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(
                        currentMove?.uppercase()?.replace("-", " ") ?: "MOVE NAME",
                        fontFamily = PressStart2P, fontSize = 6.sp,
                        color = if (currentMove != null) CaughtGold else PokedexCream.copy(alpha = 0.35f)
                    )
                    inner()
                }
            },
            modifier = Modifier
                .weight(1f)
                .background(PokedexDark.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .border(1.dp, GlowBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        )
        if (isLoading) CircularProgressIndicator(color = GlowBlue, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ResultPanel(result: DamageResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PokedexDark.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .border(1.dp, CaughtGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("RESULT", fontFamily = PressStart2P, fontSize = 6.sp, color = CaughtGold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ResultStat("MIN", result.min.toString())
            ResultStat("AVG", result.average.toString())
            ResultStat("MAX", result.max.toString())
            ResultStat("EFF", result.effectivenessLabel)
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue)
        Text(value, fontFamily = PressStart2P, fontSize = 8.sp, color = PokedexCream)
    }
}

@Composable
private fun StatConfigSection(
    gen: Int,
    slot: CalcSlot,
    label: String,
    onSlotChange: (CalcSlot) -> Unit,
    isEvSumValid: Boolean,
    modifier: Modifier = Modifier
) {
    val statNames12 = listOf("HP", "ATK", "DEF", "SPE", "SPC")
    val statNames3  = listOf("HP", "ATK", "DEF", "SPATK", "SPDEF", "SPE")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontFamily = PressStart2P, fontSize = 6.sp, color = CaughtGold)

        if (gen <= 2) {
            val cfg = slot.statConfig as? StatConfig.Gen12Config
                ?: StatConfig.Gen12Config(IntArray(5) { 15 }, IntArray(5) { 0 })
            statNames12.forEachIndexed { i, name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, modifier = Modifier.width(36.dp))
                    Text("DV", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.6f))
                    Slider(
                        value = cfg.dvs.getOrElse(i) { 15 }.toFloat(),
                        onValueChange = { v ->
                            val newDvs = cfg.dvs.copyOf().also { it[i] = v.toInt() }
                            onSlotChange(slot.copy(statConfig = cfg.copy(dvs = newDvs)))
                        },
                        valueRange = 0f..15f,
                        steps = 14,
                        modifier = Modifier.weight(1f),
                        colors = sliderColors()
                    )
                    Text("${cfg.dvs.getOrElse(i){15}}", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, modifier = Modifier.width(20.dp))
                }
            }
        } else {
            val cfg = slot.statConfig as? StatConfig.Gen3PlusConfig
                ?: StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 })
            val evSum = cfg.evs.sum()
            statNames3.forEachIndexed { i, name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, modifier = Modifier.width(40.dp))
                    Slider(
                        value = cfg.ivs.getOrElse(i) { 31 }.toFloat(),
                        onValueChange = { v ->
                            val newIvs = cfg.ivs.copyOf().also { it[i] = v.toInt() }
                            onSlotChange(slot.copy(statConfig = cfg.copy(ivs = newIvs)))
                        },
                        valueRange = 0f..31f,
                        steps = 30,
                        modifier = Modifier.weight(1f),
                        colors = sliderColors()
                    )
                    Slider(
                        value = cfg.evs.getOrElse(i) { 0 }.toFloat(),
                        onValueChange = { v ->
                            val newEvs = cfg.evs.copyOf().also { it[i] = v.toInt() }
                            val newSlot = slot.copy(statConfig = cfg.copy(evs = newEvs))
                            if (StatFormulas.isEvSumValid(newEvs)) onSlotChange(newSlot)
                        },
                        valueRange = 0f..252f,
                        steps = 62,
                        modifier = Modifier.weight(1f),
                        colors = sliderColors()
                    )
                }
            }
            Text(
                text = "$evSum/510 EVs",
                fontFamily = PressStart2P,
                fontSize = 5.sp,
                color = if (isEvSumValid) PokedexCream.copy(alpha = 0.6f) else Color.Red
            )
        }
    }
}

@Composable
private fun NaturePicker(
    selectedNature: Nature,
    onNatureSelected: (Nature) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = Natures.ALL.chunked(5)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        grouped.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { nature ->
                    val selected = nature == selectedNature
                    Text(
                        text = nature.name.uppercase().take(4),
                        fontFamily = PressStart2P,
                        fontSize = 4.sp,
                        color = if (selected) CaughtGold else PokedexCream.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNatureSelected(nature) }
                            .padding(4.dp)
                    )
                }
            }
        }
        if (selectedNature.boostedStat != null) {
            val statNames = listOf("HP", "Atk", "Def", "SpAtk", "SpDef", "Spe")
            val b = statNames.getOrElse(selectedNature.boostedStat) { "?" }
            val d = statNames.getOrElse(selectedNature.droppedStat ?: 0) { "?" }
            Text("↑ $b  ↓ $d", fontFamily = PressStart2P, fontSize = 5.sp, color = CaughtGold)
        } else {
            Text("—", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun sliderColors() = androidx.compose.material3.SliderDefaults.colors(
    thumbColor = CaughtGold,
    activeTrackColor = CaughtGold,
    inactiveTrackColor = PokedexCream.copy(alpha = 0.2f)
)
