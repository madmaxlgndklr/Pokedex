package com.madmaxlgndklr.pokedex.ui.navigation

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.madmaxlgndklr.pokedex.R

enum class PokedexState { OPEN, CLOSING, CLOSED, OPENING }

@OptIn(UnstableApi::class)
@Composable
fun PokedexAnimOverlay(
    state: PokedexState,
    onClosingComplete: () -> Unit,
    onOpeningComplete: () -> Unit,
    onPowerButtonTap: () -> Unit
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }

    // Track which direction the animation is going so the playback-end listener
    // knows which callback to fire, even after recompositions.
    var isClosingVideo by remember { mutableStateOf(true) }
    val currentIsClosingVideo by rememberUpdatedState(isClosingVideo)

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (currentIsClosingVideo) onClosingComplete() else onOpeningComplete()
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(state) {
        when (state) {
            PokedexState.CLOSING -> {
                isClosingVideo = true
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(
                    MediaItem.fromUri(
                        Uri.parse("android.resource://${context.packageName}/${R.raw.mp1}")
                    )
                )
                player.prepare()
                player.play()
            }
            PokedexState.CLOSED -> {
                // Freeze on the last frame of the closing animation.
                player.pause()
            }
            PokedexState.OPENING -> {
                isClosingVideo = false
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(
                    MediaItem.fromUri(
                        Uri.parse("android.resource://${context.packageName}/${R.raw.pdexanim}")
                    )
                )
                player.prepare()
                player.play()
            }
            PokedexState.OPEN -> {}
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sw = maxWidth
        val sh = maxHeight

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (state == PokedexState.CLOSED) {
            // Green power button hotspot over the frozen closed frame.
            // Nudge y until it aligns with the physical green LED in the frame.
            Box(
                modifier = Modifier
                    .offset(x = sw * 0.5f - 32.dp, y = sh * 0.78f - 32.dp)
                    .size(64.dp)
                    .background(Color(0xFF00FF44).copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, Color(0xFF00FF44).copy(alpha = 0.35f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPowerButtonTap
                    )
            )
        }
    }
}
