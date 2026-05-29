package com.madmaxlgndklr.pokedex.ui.team

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.ui.common.BottomNavBar
import com.madmaxlgndklr.pokedex.ui.common.NavDestination
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

@Composable
fun TeamScreen(
    viewModel: TeamViewModel,
    onBack: () -> Unit,
    onPokemonClick: (Int) -> Unit,
    onNavigateFullList: () -> Unit,
    onNavigateMyCollection: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateBattle: () -> Unit = {},
    onClosePokedex: () -> Unit = {}
) {
    val teamEntries by viewModel.teamEntries.collectAsState()
    val coverage by viewModel.teamCoverage.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize().swipeBack(onBack)) {
        val sw = maxWidth
        val sh = maxHeight
        val stripTop = sh * 0.36f
        val stripHeight = sh * 0.24f

        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Red I/O power button — between the time and sound indicators on the arc
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = sw * 0.5f - 20.dp, y = sh * 0.14f - 20.dp)
                .size(40.dp)
                .background(PokedexDark.copy(alpha = 0.72f), CircleShape)
                .border(1.5.dp, Color(0xFFCC0000).copy(alpha = 0.80f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClosePokedex
                )
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = "Close Pokédex",
                tint = Color(0xFFFF2222),
                modifier = Modifier.size(22.dp)
            )
        }

        IconButton(onClick = onBack, modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        Text(
            text = "MY TEAM",
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = CaughtGold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.19f)
                .padding(horizontal = 16.dp)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = sh * 0.245f)
                .background(GlowBlue.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                .border(1.5.dp, GlowBlue, RoundedCornerShape(4.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNavigateBattle
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("BATTLE", fontFamily = PressStart2P, fontSize = 6.sp, color = GlowBlue)
        }

        // Team slots row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(stripHeight)
                .offset(y = stripTop)
                .padding(horizontal = 8.dp)
        ) {
            for (slot in 0..5) {
                val entry = teamEntries.getOrNull(slot)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(PokedexDark.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .then(
                            if (entry != null) Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPokemonClick(entry.id) } else Modifier
                        )
                ) {
                    if (entry != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = RetrofitClient.spriteUrl(entry.id),
                                contentDescription = entry.detail?.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "#${entry.id.toString().padStart(3, '0')}",
                                fontFamily = PressStart2P,
                                fontSize = 4.sp,
                                color = PokedexCream.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "✕",
                                fontFamily = PressStart2P,
                                fontSize = 6.sp,
                                color = PokedexRed.copy(alpha = 0.7f),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.removeFromTeam(entry.id) }
                            )
                        }
                    } else {
                        Text(
                            text = "${slot + 1}",
                            fontFamily = PressStart2P,
                            fontSize = 8.sp,
                            color = PokedexCream.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Coverage panel
        Column(
            modifier = Modifier
                .offset(x = sw * 0.04f, y = stripTop + stripHeight + 8.dp)
                .fillMaxWidth(0.92f)
                .height(sh * 0.28f)
                .background(PokedexDark.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "TEAM COVERAGE",
                fontFamily = PressStart2P,
                fontSize = 6.sp,
                color = CaughtGold
            )
            Spacer(Modifier.height(6.dp))
            if (coverage.isEmpty()) {
                Text(
                    text = "ADD POKEMON TO SEE COVERAGE",
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = PokedexCream.copy(alpha = 0.4f),
                    lineHeight = 9.sp
                )
            } else {
                listOf(4f, 2f, 0.5f, 0.25f, 0f).forEach { mult ->
                    val entries = coverage.entries.filter { it.value == mult }
                    if (entries.isEmpty()) return@forEach
                    val label = when (mult) {
                        4f    -> "WEAK 4×"
                        2f    -> "WEAK 2×"
                        0.5f  -> "RESIST ½×"
                        0.25f -> "RESIST ¼×"
                        else  -> "IMMUNE 0×"
                    }
                    Text(
                        text = label,
                        fontFamily = PressStart2P,
                        fontSize = 5.sp,
                        color = when (mult) {
                            4f, 2f -> PokedexRed
                            0f     -> GlowBlue
                            else   -> PokedexCream.copy(alpha = 0.5f)
                        }
                    )
                    Spacer(Modifier.height(3.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        entries.forEach { (type, _) ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .background(typeColor(type), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 5.dp, vertical = 3.dp)
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
                    Spacer(Modifier.height(5.dp))
                }
            }
        }

        BottomNavBar(
            current = NavDestination.TEAM,
            onNavigateFullList = onNavigateFullList,
            onNavigateMyCollection = onNavigateMyCollection,
            onNavigateTeam = {},
            onNavigateSettings = onNavigateSettings,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}
