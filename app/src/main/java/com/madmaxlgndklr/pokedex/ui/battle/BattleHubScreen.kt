package com.madmaxlgndklr.pokedex.ui.battle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark

enum class BattleTab { CALC, BATTLE, MATCHUP, TRAINERS, RECORD }

@Composable
fun BattleHubScreen(
    calcVm: DamageCalcViewModel,
    battleVm: TurnBattleViewModel,
    matchupVm: MatchupViewModel,
    trainerVm: TrainerSelectViewModel,
    recordVm: RecordViewModel,
    teamIds: List<Int>,
    openOnTab: BattleTab = BattleTab.CALC,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(openOnTab) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == BattleTab.RECORD) recordVm.refresh()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sh = maxHeight
        val contentTop = sh * 0.36f

        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(onClick = onBack, modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        Text(
            text = "BATTLE HUB",
            fontFamily = com.madmaxlgndklr.pokedex.ui.theme.PressStart2P,
            fontSize = 8.sp,
            color = CaughtGold,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.22f)
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = contentTop - 28.dp)
                .padding(horizontal = 16.dp)
        ) {
            listOf(
                BattleTab.CALC     to "CALC",
                BattleTab.BATTLE   to "WILD",
                BattleTab.TRAINERS to "TRAIN",
                BattleTab.MATCHUP  to "MATCH",
                BattleTab.RECORD   to "LOG"
            ).forEach { (tab, label) ->
                val isSelected = selectedTab == tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                        .background(
                            if (isSelected) GlowBlue else PokedexDark.copy(alpha = 0.55f),
                            RoundedCornerShape(3.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) GlowBlue else GlowBlue.copy(alpha = 0.25f),
                            RoundedCornerShape(3.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedTab = tab }
                ) {
                    Text(
                        text = label,
                        fontFamily = com.madmaxlgndklr.pokedex.ui.theme.PressStart2P,
                        fontSize = 4.sp,
                        color = if (isSelected) PokedexDark else PokedexCream
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sh - contentTop - 8.dp)
                .offset(y = contentTop)
                .background(PokedexDark.copy(alpha = 0.55f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        ) {
            when (selectedTab) {
                BattleTab.CALC     -> DamageCalcScreen(viewModel = calcVm)
                BattleTab.BATTLE   -> TurnBattleScreen(viewModel = battleVm, teamIds = teamIds, onBack = onBack)
                BattleTab.MATCHUP  -> MatchupScreen(viewModel = matchupVm, yourTeamIds = teamIds)
                BattleTab.RECORD   -> RecordScreen(viewModel = recordVm)
                BattleTab.TRAINERS -> TrainerSelectScreen(
                    viewModel = trainerVm,
                    onQuickBattle = { trainer ->
                        val rosterIdx = trainerVm.sheetRosterIndex.value
                        trainerVm.closeSheet()
                        battleVm.startTrainerBattle(trainer, rosterIdx, teamIds)
                        selectedTab = BattleTab.BATTLE
                    },
                    onConfigure = { trainer ->
                        val rosterIdx = trainerVm.sheetRosterIndex.value
                        trainerVm.closeSheet()
                        battleVm.loadTrainerSetup(trainer, rosterIdx, teamIds)
                        selectedTab = BattleTab.BATTLE
                    }
                )
            }
        }
    }
}
