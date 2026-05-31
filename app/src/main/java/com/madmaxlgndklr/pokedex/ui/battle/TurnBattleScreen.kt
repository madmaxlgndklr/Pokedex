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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.ui.common.CryPlayer
import com.madmaxlgndklr.pokedex.ui.common.PokemonImage
import com.madmaxlgndklr.pokedex.ui.common.TypeBadge
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
    val battleTrainer by viewModel.battleTrainer.collectAsState()

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
            battleState is BattleState.PendingSwitch -> {
                val pending = battleState as BattleState.PendingSwitch
                PendingSwitchView(
                    state = pending,
                    onSwitch = { idx -> viewModel.confirmSwitch(idx) }
                )
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
                    onSwitch = { idx -> viewModel.submitSwitch(idx) },
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
                    battleTrainer = battleTrainer,
                    teamIds = teamIds,
                    viewModel = viewModel,
                    onFight = { viewModel.startBattleFromSetup(teamIds) }
                )
            }
            else -> {
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
private fun PendingSwitchView(
    state: BattleState.PendingSwitch,
    onSwitch: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            "CHOOSE YOUR NEXT POKÉMON",
            fontFamily = PressStart2P, fontSize = 7.sp, color = CaughtGold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        state.log.takeLast(4).forEach { line ->
            Text(line, fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, lineHeight = 9.sp)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(state.playerTeam) { idx, poke ->
                val isCurrent = idx == state.playerActiveIndex
                val isFainted = poke.currentHp <= 0
                val isAvailable = !isCurrent && !isFainted
                val hpFraction = (poke.currentHp.toFloat() / poke.maxHp).coerceIn(0f, 1f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            PokedexDark.copy(alpha = if (isAvailable) 0.6f else 0.2f),
                            RoundedCornerShape(6.dp)
                        )
                        .then(
                            if (isAvailable) Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSwitch(idx) } else Modifier
                        )
                        .padding(8.dp)
                ) {
                    PokemonImage(
                        id = poke.detail.id,
                        name = poke.detail.name,
                        contentDescription = poke.detail.name,
                        modifier = Modifier.size(40.dp).then(
                            if (isFainted) Modifier.alpha(0.4f) else Modifier
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            poke.detail.name.uppercase(),
                            fontFamily = PressStart2P, fontSize = 6.sp,
                            color = if (isAvailable) PokedexCream else PokedexCream.copy(alpha = 0.3f)
                        )
                        Box(
                            Modifier.fillMaxWidth().height(4.dp)
                                .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        ) {
                            val barColor = when {
                                isFainted -> PokedexRed.copy(alpha = 0.3f)
                                hpFraction > 0.5f -> Color(0xFF44DD44)
                                hpFraction > 0.2f -> CaughtGold
                                else -> PokedexRed
                            }
                            Box(
                                Modifier.fillMaxWidth(hpFraction).height(4.dp)
                                    .background(barColor, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                    if (isFainted) {
                        Text("FAINT", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexRed.copy(alpha = 0.6f))
                    }
                    if (isCurrent) {
                        Text("OUT", fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleSetupView(
    setup: BattleSetup,
    battleTrainer: SelectedTrainer?,
    teamIds: List<Int>,
    viewModel: TurnBattleViewModel,
    onFight: () -> Unit
) {
    val selectedSetupSlot by viewModel.selectedSetupSlot.collectAsState()
    val slotDetails by viewModel.slotDetails.collectAsState()
    val gen by viewModel.selectedGen.collectAsState()
    val heldItems by viewModel.heldItems.collectAsState()
    val syncError by viewModel.heldItemSyncError.collectAsState()
    val canStart by viewModel.canStartBattle.collectAsState()

    val activePokemonId = teamIds.getOrNull(selectedSetupSlot)
    val activeOv = if (selectedSetupSlot == 0 || activePokemonId == null) null else setup.teamOverrides[activePokemonId]
    val activeDetail = slotDetails[selectedSetupSlot] ?: setup.playerDetail
    val activeLevel = activeOv?.level ?: setup.level
    val activeNature = activeOv?.nature ?: setup.nature
    val activeStatConfig = activeOv?.statConfig ?: setup.statConfig
    val activeMoves = learnableMoves(activeDetail, activeLevel)
    val activeSelected: List<String> = when {
        selectedSetupSlot == 0 -> setup.selectedMoveNames
        activeOv?.selectedMoveNames != null -> activeOv.selectedMoveNames
        else -> activeMoves.filter { it.available }.take(4).map { it.name }
    }
    val selectedSet = activeSelected.toSet()
    val slot0HasMoves = setup.selectedMoveNames.isNotEmpty()

    var showStatConfig by remember { mutableStateOf(false) }
    var showNaturePicker by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSetupSlot) {
        showStatConfig = false
        showNaturePicker = false
        showItems = false
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Active slot Pokémon + level row
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    PokemonImage(
                        id = activeDetail.id,
                        name = activeDetail.name,
                        contentDescription = activeDetail.name,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        activeDetail.name.uppercase(),
                        fontFamily = PressStart2P, fontSize = 7.sp, color = PokedexCream,
                        modifier = Modifier.weight(1f)
                    )
                    LevelPicker(level = activeLevel, onLevelChange = { viewModel.setSlotLevel(selectedSetupSlot, it) })
                }
            }

            // Opponent slot
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text("VS.", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.4f))
                    Spacer(Modifier.width(8.dp))
                    if (battleTrainer != null) {
                        Text(
                            battleTrainer.trainer.name.uppercase(),
                            fontFamily = PressStart2P, fontSize = 7.sp, color = PokedexCream,
                            modifier = Modifier.weight(1f)
                        )
                        TypeBadge(type = battleTrainer.trainer.typeSpecialty)
                    } else {
                        Text(
                            "RANDOM OPPONENT",
                            fontFamily = PressStart2P, fontSize = 7.sp,
                            color = PokedexCream.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Team slot strip — tap to switch which Pokémon you're configuring
            if (teamIds.size > 1) {
                item {
                    Text(
                        "TEAM",
                        fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(teamIds, key = { _, id -> id }) { idx, _ ->
                            val isActive = selectedSetupSlot == idx
                            val hasOverride = setup.teamOverrides.containsKey(idx)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        when {
                                            isActive -> GlowBlue.copy(alpha = 0.3f)
                                            hasOverride -> CaughtGold.copy(alpha = 0.2f)
                                            else -> PokedexDark.copy(alpha = 0.4f)
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isActive -> GlowBlue
                                            hasOverride -> CaughtGold
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { viewModel.selectSetupSlot(idx, teamIds) }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    "${idx + 1}",
                                    fontFamily = PressStart2P, fontSize = 7.sp,
                                    color = if (isActive) GlowBlue else PokedexCream.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    if (selectedSetupSlot != 0 && setup.teamOverrides.containsKey(selectedSetupSlot)) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "RESET SLOT ${selectedSetupSlot + 1}",
                            fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexRed.copy(alpha = 0.7f),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.setSlotOverride(selectedSetupSlot, null) }
                        )
                    }
                }
            }

            // Gen toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isGen12 = gen <= 2
                    listOf("GEN I–II" to true, "GEN III+" to false).forEach { (label, isGen12Option) ->
                        val selected = isGen12 == isGen12Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) CaughtGold else PokedexDark.copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val newGen = if (isGen12Option) 1 else 5
                                    viewModel.setGen(newGen)
                                    val defaultConfig = if (isGen12Option)
                                        StatConfig.Gen12Config(IntArray(5) { 15 }, IntArray(5) { 0 })
                                    else
                                        StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 })
                                    viewModel.setStatConfig(defaultConfig)
                                    viewModel.setNature(Natures.HARDY)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label, fontFamily = PressStart2P, fontSize = 6.sp,
                                color = if (selected) PokedexDark else PokedexCream
                            )
                        }
                    }
                }
            }

            // Stats section (collapsible) — shows active slot's config
            item {
                Text(
                    text = if (showStatConfig) "STATS ▲" else "STATS ▼",
                    fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showStatConfig = !showStatConfig }
                )
            }
            if (showStatConfig) {
                item {
                    val isEvSumValid = if (activeStatConfig is StatConfig.Gen3PlusConfig)
                        StatFormulas.isEvSumValid(activeStatConfig.evs) else true
                    StatConfigSection(
                        gen = gen,
                        statConfig = activeStatConfig,
                        label = if (gen <= 2) "DVs / Stat Exp" else "IVs / EVs",
                        onConfigChange = { viewModel.setSlotStatConfig(selectedSetupSlot, it) },
                        isEvSumValid = isEvSumValid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Nature picker (collapsible, Gen 3+) — shows active slot's nature
            if (gen >= 3) {
                item {
                    Text(
                        text = if (showNaturePicker) "NATURE ▲" else "NATURE: ${activeNature.name.uppercase()} ▼",
                        fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showNaturePicker = !showNaturePicker }
                    )
                }
                if (showNaturePicker) {
                    item {
                        NaturePicker(
                            selectedNature = activeNature,
                            onNatureSelected = { viewModel.setSlotNature(selectedSetupSlot, it); showNaturePicker = false }
                        )
                    }
                }
            }

            // Held items (collapsible)
            item {
                if (syncError) {
                    Text(
                        "ITEMS UNAVAILABLE — SYNC IN SETTINGS",
                        fontFamily = PressStart2P, fontSize = 5.sp,
                        color = PokedexCream.copy(alpha = 0.5f)
                    )
                } else {
                    Text(
                        text = if (showItems) "ITEM ▲" else "ITEM: ${setup.heldItem?.displayName ?: "NONE"} ▼",
                        fontFamily = PressStart2P, fontSize = 5.sp, color = GlowBlue,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showItems = !showItems }
                    )
                    if (showItems) {
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                val noneSelected = setup.heldItem == null
                                ItemCard("NONE", "No item", noneSelected) { viewModel.setHeldItem(null) }
                            }
                            items(heldItems) { heldItem ->
                                val selected = setup.heldItem?.id == heldItem.id
                                ItemCard(heldItem.displayName, heldItem.effectSummary, selected) { viewModel.setHeldItem(heldItem) }
                            }
                        }
                    }
                }
            }

            // Moves header — shows active slot's move count
            item {
                Text(
                    "MOVES  ${selectedSet.size}/4",
                    fontFamily = PressStart2P, fontSize = 5.sp, color = CaughtGold
                )
            }

            // Move list for active slot
            items(activeMoves) { move ->
                MoveRow(
                    move = move,
                    selected = move.name in selectedSet,
                    selectionFull = selectedSet.size >= 4 && move.name !in selectedSet,
                    onToggle = { if (move.available) viewModel.toggleSlotMove(selectedSetupSlot, move.name) }
                )
            }
        }

        // Pinned START BATTLE button — enabled when slot 0 has at least one move selected
        Text(
            text = "START BATTLE",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = if (canStart && slot0HasMoves) CaughtGold else PokedexCream.copy(alpha = 0.3f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .background(
                    if (canStart && slot0HasMoves) PokedexRed else PokedexDark.copy(alpha = 0.4f),
                    RoundedCornerShape(4.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = canStart && slot0HasMoves
                ) {
                    if (canStart && slot0HasMoves) onFight()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun StatConfigSection(
    gen: Int,
    statConfig: StatConfig,
    label: String,
    onConfigChange: (StatConfig) -> Unit,
    isEvSumValid: Boolean,
    modifier: Modifier = Modifier
) {
    val statNames12 = listOf("HP", "ATK", "DEF", "SPE", "SPC")
    val statNames3  = listOf("HP", "ATK", "DEF", "SPATK", "SPDEF", "SPE")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontFamily = PressStart2P, fontSize = 6.sp, color = CaughtGold)

        if (gen <= 2) {
            val cfg = statConfig as? StatConfig.Gen12Config
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
                            onConfigChange(cfg.copy(dvs = newDvs))
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
            val cfg = statConfig as? StatConfig.Gen3PlusConfig
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
                            onConfigChange(cfg.copy(ivs = newIvs))
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
                            val newCfg = cfg.copy(evs = newEvs)
                            if (StatFormulas.isEvSumValid(newEvs)) onConfigChange(newCfg)
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

@Composable
private fun ItemCard(name: String, summary: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .background(
                if (selected) CaughtGold.copy(alpha = 0.2f) else PokedexDark.copy(alpha = 0.4f),
                RoundedCornerShape(6.dp)
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) CaughtGold else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(name, fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream, maxLines = 1)
        Text(summary, fontFamily = PressStart2P, fontSize = 4.sp,
            color = PokedexCream.copy(alpha = 0.6f), maxLines = 2, lineHeight = 6.sp)
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
    onSwitch: (Int) -> Unit,
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

        // Team strip — tap a slot to switch voluntarily
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(state.playerTeam) { idx, poke ->
                val isActive = idx == state.playerActiveIndex
                val isFainted = poke.currentHp <= 0
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            when {
                                isActive -> GlowBlue.copy(alpha = 0.3f)
                                isFainted -> PokedexDark.copy(alpha = 0.2f)
                                else -> PokedexDark.copy(alpha = 0.5f)
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            if (isActive) GlowBlue else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .then(
                            if (!isActive && !isFainted)
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSwitch(idx) }
                            else Modifier
                        )
                ) {
                    PokemonImage(
                        id = poke.detail.id,
                        name = poke.detail.name,
                        contentDescription = poke.detail.name,
                        modifier = Modifier.size(28.dp).then(
                            if (isFainted) Modifier.alpha(0.3f) else Modifier
                        )
                    )
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
        PokemonImage(
            id = pokemon.detail.id,
            name = pokemon.detail.name,
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
