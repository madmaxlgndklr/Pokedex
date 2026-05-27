package com.madmaxlgndklr.pokedex.ui.battle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

enum class BattleTab { CALC, BATTLE, MATCHUP }

@Composable
fun BattleHubScreen(
    calcVm: DamageCalcViewModel,
    battleVm: TurnBattleViewModel,
    matchupVm: MatchupViewModel,
    teamIds: List<Int>,
    openOnTab: BattleTab = BattleTab.CALC,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(openOnTab) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sw = maxWidth
        val sh = maxHeight
        val contentTop = sh * 0.36f

        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Back button
        IconButton(onClick = onBack, modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        // Title
        Text(
            text = "BATTLE HUB",
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = CaughtGold,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.22f)
                .padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Tab row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = contentTop - 28.dp)
                .padding(horizontal = 16.dp)
        ) {
            listOf(BattleTab.CALC to "CALC", BattleTab.BATTLE to "BATTLE", BattleTab.MATCHUP to "MATCHUP")
                .forEach { (tab, label) ->
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
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null)
                            { selectedTab = tab }
                    ) {
                        Text(
                            text = label,
                            fontFamily = PressStart2P,
                            fontSize = 5.sp,
                            color = if (isSelected) PokedexDark else PokedexCream
                        )
                    }
                }
        }

        // Content area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sh - contentTop - 8.dp)
                .offset(y = contentTop)
                .background(PokedexDark.copy(alpha = 0.55f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        ) {
            when (selectedTab) {
                BattleTab.CALC    -> DamageCalcScreen(viewModel = calcVm)
                BattleTab.BATTLE  -> TurnBattleScreen(viewModel = battleVm, teamIds = teamIds, onBack = onBack)
                BattleTab.MATCHUP -> MatchupScreen(viewModel = matchupVm, yourTeamIds = teamIds)
            }
        }
    }
}
