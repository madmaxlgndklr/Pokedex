package com.madmaxlgndklr.pokedex.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.ui.common.BottomNavBar
import com.madmaxlgndklr.pokedex.ui.common.NavDestination
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexGreen
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onBack: () -> Unit,
    onNavigateFullList: () -> Unit,
    onNavigateMyCollection: () -> Unit,
    onNavigateProfile: () -> Unit = {}
) {
    val musicOnLaunch by viewModel.musicOnLaunch.collectAsState()
    val spriteMode by viewModel.spriteMode.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncOptions by remember { mutableStateOf(SyncOptions()) }
    val context = LocalContext.current

    if (showSyncDialog) {
        val allChecked = syncOptions.syncData && syncOptions.syncMoves && syncOptions.syncCries && syncOptions.syncItems
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            containerColor = PokedexDark,
            title = {
                Text(
                    text = "SYNC OPTIONS",
                    fontFamily = PressStart2P,
                    fontSize = 8.sp,
                    color = CaughtGold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Downloading may use mobile data. Select what to sync.",
                        fontFamily = PressStart2P,
                        fontSize = 6.sp,
                        color = PokedexCream.copy(alpha = 0.7f),
                        lineHeight = 11.sp
                    )
                    SyncOptionRow(
                        label = "SELECT ALL",
                        checked = allChecked,
                        onCheckedChange = { checked ->
                            syncOptions = SyncOptions(checked, checked, checked, checked)
                        }
                    )
                    HorizontalDivider(color = PokedexCream.copy(alpha = 0.2f))
                    SyncOptionRow(
                        label = "POKEMON DATA  ~25 MB",
                        checked = syncOptions.syncData,
                        onCheckedChange = { syncOptions = syncOptions.copy(syncData = it) }
                    )
                    SyncOptionRow(
                        label = "TEAM MOVES",
                        checked = syncOptions.syncMoves,
                        onCheckedChange = { syncOptions = syncOptions.copy(syncMoves = it) }
                    )
                    SyncOptionRow(
                        label = "BATTLE CRIES  ~30 MB",
                        checked = syncOptions.syncCries,
                        onCheckedChange = { syncOptions = syncOptions.copy(syncCries = it) }
                    )
                    SyncOptionRow(
                        label = "HELD ITEMS",
                        checked = syncOptions.syncItems,
                        onCheckedChange = { syncOptions = syncOptions.copy(syncItems = it) }
                    )
                }
            },
            confirmButton = {
                Text(
                    text = "START SYNC",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = GlowBlue,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showSyncDialog = false
                            viewModel.syncWithOptions(syncOptions)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            },
            dismissButton = {
                Text(
                    text = "CANCEL",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = PokedexCream.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showSyncDialog = false }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        )
    }
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        }.getOrDefault("1.0")
    }

    BoxWithConstraints(Modifier.fillMaxSize().swipeBack(onBack)) {
        val sw = maxWidth
        val sh = maxHeight

        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        // Settings panel inside the blue strip
        Column(
            modifier = Modifier
                .offset(x = sw * 0.04f, y = sh * 0.37f)
                .fillMaxWidth(0.92f)
                .height(sh * 0.50f)
                .background(PokedexDark.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "SETTINGS",
                fontFamily = PressStart2P,
                fontSize = 9.sp,
                color = CaughtGold
            )

            HorizontalDivider(color = PokedexCream.copy(alpha = 0.25f))

            // Sprite mode row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPRITE MODE",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = PokedexCream
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("modern" to "3D GIF", "retro" to "GB", "ds" to "DS").forEach { (mode, label) ->
                        val selected = spriteMode == mode
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selected) CaughtGold else PokedexDark,
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .clickable { viewModel.setSpriteMode(mode) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = PressStart2P,
                                fontSize = 5.sp,
                                color = if (selected) PokedexDark else PokedexCream.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = PokedexCream.copy(alpha = 0.25f))

            // Music on launch row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MUSIC ON LAUNCH",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = PokedexCream
                )
                Switch(
                    checked = musicOnLaunch,
                    onCheckedChange = { viewModel.setMusicOnLaunch(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PokedexDark,
                        checkedTrackColor = PokedexGreen,
                        uncheckedThumbColor = PokedexCream.copy(alpha = 0.6f),
                        uncheckedTrackColor = PokedexDark.copy(alpha = 0.6f)
                    )
                )
            }

            HorizontalDivider(color = PokedexCream.copy(alpha = 0.25f))

            // Mute this session row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MUTE THIS SESSION",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = PokedexCream
                )
                Switch(
                    checked = isMuted,
                    onCheckedChange = { onToggleMute() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PokedexDark,
                        checkedTrackColor = PokedexGreen,
                        uncheckedThumbColor = PokedexCream.copy(alpha = 0.6f),
                        uncheckedTrackColor = PokedexDark.copy(alpha = 0.6f)
                    )
                )
            }

            HorizontalDivider(color = PokedexCream.copy(alpha = 0.25f))

            // Version
            Text(
                text = "VERSION $version",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = PokedexCream.copy(alpha = 0.7f)
            )

            HorizontalDivider(color = PokedexCream.copy(alpha = 0.25f))

            // Trainer Profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNavigateProfile() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRAINER PROFILE",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = PokedexCream
                )
                Text(
                    text = "▶",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = GlowBlue
                )
            }

            HorizontalDivider(color = PokedexCream.copy(alpha = 0.25f))

            // Offline sync
            when (val state = syncState) {
                is SyncState.Idle -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                syncOptions = SyncOptions()   // reset to all-checked
                                showSyncDialog = true
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYNC FOR OFFLINE",
                            fontFamily = PressStart2P,
                            fontSize = 7.sp,
                            color = PokedexCream
                        )
                        Text(
                            text = "SYNC",
                            fontFamily = PressStart2P,
                            fontSize = 7.sp,
                            color = GlowBlue
                        )
                    }
                }
                is SyncState.Syncing -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SYNCING ${state.phase}...",
                                fontFamily = PressStart2P,
                                fontSize = 7.sp,
                                color = GlowBlue
                            )
                            Text(
                                text = "${state.completed}/${state.total}",
                                fontFamily = PressStart2P,
                                fontSize = 7.sp,
                                color = PokedexCream.copy(alpha = 0.7f)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { if (state.total > 0) state.completed.toFloat() / state.total else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = GlowBlue,
                            trackColor = PokedexCream.copy(alpha = 0.15f)
                        )
                    }
                }
                is SyncState.Done -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.resetSyncState() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYNC COMPLETE",
                            fontFamily = PressStart2P,
                            fontSize = 7.sp,
                            color = PokedexGreen
                        )
                        Text(
                            text = "${state.cached}/${state.total}",
                            fontFamily = PressStart2P,
                            fontSize = 7.sp,
                            color = PokedexCream.copy(alpha = 0.6f)
                        )
                    }
                }
                is SyncState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.resetSyncState()
                                syncOptions = SyncOptions()
                                showSyncDialog = true
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYNC FAILED",
                            fontFamily = PressStart2P,
                            fontSize = 7.sp,
                            color = PokedexCream.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "RETRY",
                            fontFamily = PressStart2P,
                            fontSize = 7.sp,
                            color = GlowBlue
                        )
                    }
                }
            }

            HorizontalDivider(color = PokedexCream.copy(alpha = 0.25f))

            // Official site link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pokemon.com"))
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OFFICIAL WEBSITE",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = PokedexCream
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open",
                    tint = PokedexCream,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        BottomNavBar(
            current = NavDestination.SETTINGS,
            onNavigateFullList = onNavigateFullList,
            onNavigateMyCollection = onNavigateMyCollection,
            onNavigateSettings = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun SyncOptionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = PressStart2P,
            fontSize = 6.sp,
            color = PokedexCream,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PokedexDark,
                checkedTrackColor = PokedexGreen,
                uncheckedThumbColor = PokedexCream.copy(alpha = 0.6f),
                uncheckedTrackColor = PokedexDark.copy(alpha = 0.6f)
            )
        )
    }
}
