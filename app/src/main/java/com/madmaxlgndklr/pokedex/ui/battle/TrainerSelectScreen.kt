package com.madmaxlgndklr.pokedex.ui.battle

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.common.PokemonImage
import com.madmaxlgndklr.pokedex.ui.common.TypeBadge
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerSelectScreen(
    viewModel: TrainerSelectViewModel,
    onQuickBattle: (Trainer) -> Unit,
    onConfigure: (Trainer) -> Unit
) {
    val trainers by viewModel.trainers.collectAsState()
    val expandedRegions by viewModel.expandedRegions.collectAsState()
    val sheetTrainer by viewModel.sheetTrainer.collectAsState()
    val sheetRosterIndex by viewModel.sheetRosterIndex.collectAsState()

    val groupedByRegion = remember(trainers) { trainers.groupBy { it.region } }

    if (trainers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "TRAINERS UNAVAILABLE",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = PokedexCream.copy(alpha = 0.5f)
            )
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        groupedByRegion.forEach { (region, regionTrainers) ->
            item(key = "header-$region") {
                RegionHeader(
                    region = region,
                    expanded = region in expandedRegions,
                    onClick = { viewModel.toggleRegion(region) }
                )
            }
            if (region in expandedRegions) {
                items(regionTrainers, key = { it.id }) { trainer ->
                    TrainerCard(trainer = trainer, onClick = { viewModel.openSheet(trainer) })
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    val current = sheetTrainer
    if (current != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSheet() },
            sheetState = sheetState,
            containerColor = PokedexDark
        ) {
            TrainerBottomSheet(
                trainer = current,
                rosterIndex = sheetRosterIndex,
                onRosterIndexChange = { viewModel.setRosterIndex(it) },
                onQuickBattle = { onQuickBattle(current) },
                onConfigure = { onConfigure(current) }
            )
        }
    }
}

@Composable
private fun RegionHeader(region: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = region.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (expanded) "▲" else "▼",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = GlowBlue
        )
    }
}

@Composable
private fun TrainerCard(trainer: Trainer, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PokedexDark.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(56.dp)
                .background(typeColor(trainer.typeSpecialty), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${trainer.name} · ${trainer.title}",
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = PokedexCream
                )
                Spacer(Modifier.width(6.dp))
                TypeBadge(type = trainer.typeSpecialty)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                trainer.rosters[0].team.forEach { tp ->
                    PokemonImage(
                        id = tp.pokemonId,
                        name = "",
                        contentDescription = "",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun TrainerBottomSheet(
    trainer: Trainer,
    rosterIndex: Int,
    onRosterIndexChange: (Int) -> Unit,
    onQuickBattle: () -> Unit,
    onConfigure: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = trainer.name.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 9.sp,
            color = PokedexCream
        )
        Text(
            text = "${trainer.title} · ${trainer.region}",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = PokedexCream.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(Modifier.height(12.dp))

        if (trainer.rosters.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trainer.rosters.forEachIndexed { idx, roster ->
                    val selected = idx == rosterIndex
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(
                                if (selected) GlowBlue else PokedexDark.copy(alpha = 0.4f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onRosterIndexChange(idx) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = roster.label,
                            fontFamily = PressStart2P,
                            fontSize = 4.sp,
                            color = if (selected) PokedexDark else PokedexCream
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        val roster = trainer.rosters[rosterIndex]
        roster.team.forEach { tp ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                PokemonImage(
                    id = tp.pokemonId,
                    name = "",
                    contentDescription = "",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "#${tp.pokemonId}",
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = PokedexCream,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Lv.${tp.level}",
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = GlowBlue
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onQuickBattle,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PokedexRed)
        ) {
            Text("QUICK BATTLE", fontFamily = PressStart2P, fontSize = 6.sp, color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onConfigure,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowBlue),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GlowBlue)
        ) {
            Text("CONFIGURE", fontFamily = PressStart2P, fontSize = 6.sp, color = GlowBlue)
        }

        Spacer(Modifier.height(24.dp))
    }
}
