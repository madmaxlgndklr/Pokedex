package com.madmaxlgndklr.pokedex.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.madmaxlgndklr.pokedex.data.local.TrainerRecord
import com.madmaxlgndklr.pokedex.data.local.WildRecord
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.ui.common.TypeBadge
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CLASS_ORDER = listOf("CHAMPION", "ELITE_FOUR", "GYM_LEADER", "RIVAL")

private fun classLabel(cls: String) = when (cls) {
    "ELITE_FOUR" -> "ELITE FOUR"
    "GYM_LEADER" -> "GYM LEADER"
    else -> cls
}

private fun classColor(cls: String) = when (cls) {
    "CHAMPION"   -> CaughtGold
    "ELITE_FOUR" -> Color(0xFFCC88FF)
    "RIVAL"      -> Color(0xFFFF9944)
    else         -> GlowBlue
}

private fun formatDate(ts: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ts))

@Composable
fun RecordScreen(viewModel: RecordViewModel) {
    val trainerRecords by viewModel.trainerRecords.collectAsState()
    val wildRecords by viewModel.wildRecords.collectAsState()
    val selectedRecord by viewModel.selectedRecord.collectAsState()
    val selectedTrainer by viewModel.selectedTrainer.collectAsState()

    var showTrainers by remember { mutableStateOf(true) }
    var showWild by remember { mutableStateOf(true) }

    val totalWins = trainerRecords.sumOf { it.wins } + wildRecords.sumOf { it.wins }
    val totalLosses = trainerRecords.sumOf { it.losses } + wildRecords.sumOf { it.losses }
    val totalBattles = totalWins + totalLosses

    Box(Modifier.fillMaxSize()) {
        if (totalBattles == 0) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "NO BATTLES YET",
                    fontFamily = PressStart2P, fontSize = 7.sp,
                    color = PokedexCream.copy(alpha = 0.35f)
                )
                Text(
                    "HEAD TO WILD OR TRAIN",
                    fontFamily = PressStart2P, fontSize = 5.sp,
                    color = PokedexCream.copy(alpha = 0.2f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Summary header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatChip("TOTAL", "$totalBattles", PokedexCream)
                        StatChip("WIN", "$totalWins", Color(0xFF44DD44))
                        StatChip("LOSS", "$totalLosses", PokedexRed)
                    }
                }

                // Trainer records
                item {
                    RecordSectionHeader(
                        label = "TRAINERS (${trainerRecords.size})",
                        expanded = showTrainers,
                        onToggle = { showTrainers = !showTrainers }
                    )
                }

                if (showTrainers) {
                    if (trainerRecords.isEmpty()) {
                        item {
                            Text(
                                "NO TRAINER BATTLES YET",
                                fontFamily = PressStart2P, fontSize = 5.sp,
                                color = PokedexCream.copy(alpha = 0.3f),
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    } else {
                        val byClass = trainerRecords.groupBy { it.trainerClass }
                        CLASS_ORDER.forEach { cls ->
                            val group = byClass[cls] ?: return@forEach
                            item {
                                Text(
                                    classLabel(cls),
                                    fontFamily = PressStart2P, fontSize = 5.sp,
                                    color = classColor(cls),
                                    modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp)
                                )
                            }
                            items(group, key = { it.trainerId }) { record ->
                                TrainerRecordRow(record) { viewModel.selectTrainer(record) }
                            }
                        }
                        // Any classes not in our ordered list
                        (trainerRecords.map { it.trainerClass }.toSet() - CLASS_ORDER.toSet()).forEach { cls ->
                            val group = byClass[cls] ?: return@forEach
                            item {
                                Text(
                                    classLabel(cls),
                                    fontFamily = PressStart2P, fontSize = 5.sp,
                                    color = PokedexCream.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp)
                                )
                            }
                            items(group, key = { it.trainerId }) { record ->
                                TrainerRecordRow(record) { viewModel.selectTrainer(record) }
                            }
                        }
                    }
                }

                // Wild records
                item {
                    RecordSectionHeader(
                        label = "WILD (${wildRecords.size})",
                        expanded = showWild,
                        onToggle = { showWild = !showWild }
                    )
                }

                if (showWild) {
                    if (wildRecords.isEmpty()) {
                        item {
                            Text(
                                "NO WILD BATTLES YET",
                                fontFamily = PressStart2P, fontSize = 5.sp,
                                color = PokedexCream.copy(alpha = 0.3f),
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    } else {
                        items(wildRecords, key = { it.pokemonId }) { record ->
                            WildRecordRow(record)
                        }
                    }
                }
            }
        }

        // Trainer detail overlay
        if (selectedRecord != null) {
            TrainerDetailOverlay(
                record = selectedRecord!!,
                trainer = selectedTrainer,
                onDismiss = { viewModel.clearSelection() }
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = PressStart2P, fontSize = 10.sp, color = color)
        Text(label, fontFamily = PressStart2P, fontSize = 4.sp, color = PokedexCream.copy(alpha = 0.5f))
    }
}

@Composable
private fun RecordSectionHeader(label: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontFamily = PressStart2P, fontSize = 6.sp,
            color = CaughtGold,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (expanded) "▲" else "▼",
            fontFamily = PressStart2P, fontSize = 6.sp,
            color = CaughtGold
        )
    }
}

@Composable
private fun TrainerRecordRow(record: TrainerRecord, onClick: () -> Unit) {
    val defeated = record.wins > 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(PokedexDark.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .border(
                1.dp,
                if (defeated) classColor(record.trainerClass).copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Class color accent bar
        Box(
            Modifier
                .width(3.dp)
                .height(36.dp)
                .background(classColor(record.trainerClass), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(record.name.uppercase(), fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexCream)
            Text(
                "${record.title}  •  ${record.region}",
                fontFamily = PressStart2P, fontSize = 4.sp,
                color = PokedexCream.copy(alpha = 0.5f)
            )
        }
        TypeBadge(type = record.typeSpecialty)
        Spacer(Modifier.width(8.dp))
        WLBadge(wins = record.wins, losses = record.losses)
        if (defeated) {
            Spacer(Modifier.width(6.dp))
            Text("▶", fontFamily = PressStart2P, fontSize = 5.sp, color = classColor(record.trainerClass).copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun WildRecordRow(record: WildRecord) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(PokedexDark.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        AsyncImage(
            model = RetrofitClient.spriteUrl(record.pokemonId),
            contentDescription = record.pokemonName,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            record.pokemonName.uppercase(),
            fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexCream,
            modifier = Modifier.weight(1f)
        )
        WLBadge(wins = record.wins, losses = record.losses)
    }
}

@Composable
private fun WLBadge(wins: Int, losses: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${wins}W",
            fontFamily = PressStart2P, fontSize = 5.sp,
            color = Color(0xFF44DD44)
        )
        Text(
            "${losses}L",
            fontFamily = PressStart2P, fontSize = 5.sp,
            color = PokedexRed
        )
    }
}

@Composable
private fun TrainerDetailOverlay(
    record: TrainerRecord,
    trainer: Trainer?,
    onDismiss: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PokedexDark.copy(alpha = 0.97f))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Back button row
            item {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
                }
            }

            // Trainer header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .border(1.dp, classColor(record.trainerClass).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            classLabel(record.trainerClass),
                            fontFamily = PressStart2P, fontSize = 5.sp,
                            color = classColor(record.trainerClass)
                        )
                        TypeBadge(type = record.typeSpecialty)
                    }
                    Text(record.name.uppercase(), fontFamily = PressStart2P, fontSize = 10.sp, color = PokedexCream)
                    Text(
                        "${record.title}  •  ${record.region}",
                        fontFamily = PressStart2P, fontSize = 5.sp,
                        color = PokedexCream.copy(alpha = 0.55f)
                    )
                }
            }

            // Battle record stats
            item {
                val total = record.wins + record.losses
                val winPct = if (total > 0) (record.wins * 100 / total) else 0
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("RECORD", fontFamily = PressStart2P, fontSize = 5.sp, color = CaughtGold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatChip("BATTLES", "$total", PokedexCream)
                        StatChip("WINS", "${record.wins}", Color(0xFF44DD44))
                        StatChip("LOSSES", "${record.losses}", PokedexRed)
                        StatChip("WIN%", "$winPct%", if (winPct >= 50) Color(0xFF44DD44) else PokedexRed)
                    }
                    if (record.firstDefeatedAt != null) {
                        Text(
                            "FIRST DEFEATED: ${formatDate(record.firstDefeatedAt)}",
                            fontFamily = PressStart2P, fontSize = 4.sp,
                            color = classColor(record.trainerClass).copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            "NOT YET DEFEATED",
                            fontFamily = PressStart2P, fontSize = 4.sp,
                            color = PokedexRed.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        "LAST BATTLED: ${formatDate(record.lastBattledAt)}",
                        fontFamily = PressStart2P, fontSize = 4.sp,
                        color = PokedexCream.copy(alpha = 0.4f)
                    )
                }
            }

            // Trainer rosters (from live trainer data if available)
            if (trainer != null) {
                trainer.rosters.forEachIndexed { rIdx, roster ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (trainer.rosters.size > 1) "ROSTER ${rIdx + 1}: ${roster.label.uppercase()}" else "ROSTER",
                                fontFamily = PressStart2P, fontSize = 5.sp, color = CaughtGold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(roster.team) { tp ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        AsyncImage(
                                            model = RetrofitClient.spriteUrl(tp.pokemonId),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            "LV.${tp.level}",
                                            fontFamily = PressStart2P, fontSize = 4.sp,
                                            color = PokedexCream.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
