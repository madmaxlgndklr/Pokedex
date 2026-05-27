package com.madmaxlgndklr.pokedex.ui.search

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.ui.common.BottomNavBar
import com.madmaxlgndklr.pokedex.ui.common.NavDestination
import com.madmaxlgndklr.pokedex.ui.common.swipeBack
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import kotlinx.coroutines.delay

private enum class PokedexPhase { VIDEO, INTERACTIVE }

@Composable
private fun BootDiagnosticOverlay(
    sw: androidx.compose.ui.unit.Dp,
    sh: androidx.compose.ui.unit.Dp,
    alpha: Float,
    onSyncNow: () -> Unit,
    onSkip: () -> Unit
) {
    val lines = remember {
        listOf(
            "> POKEDEX DATA CHECK",
            "> SCANNING LOCAL CACHE...",
            "> ENTRIES FOUND: 0",
            "> OFFLINE MODE: UNAVAILABLE",
            "",
            "> SYNC RECOMMENDED",
            "> (~80 MB REQUIRED)"
        )
    }
    var visibleLines by remember { mutableStateOf(0) }
    var showButtons by remember { mutableStateOf(false) }
    val buttonsAlpha by animateFloatAsState(
        targetValue = if (showButtons) 1f else 0f,
        animationSpec = tween(400),
        label = "diagButtons"
    )

    LaunchedEffect(Unit) {
        lines.forEachIndexed { i, _ ->
            delay(if (i == 0) 0L else 300L)
            visibleLines = i + 1
        }
        delay(300)
        showButtons = true
    }

    Column(
        modifier = Modifier
            .offset(x = sw * 0.04f, y = sh * 0.36f)
            .fillMaxWidth(0.92f)
            .height(sh * 0.27f)
            .alpha(alpha)
            .background(PokedexDark.copy(alpha = 0.90f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.take(visibleLines).forEach { line ->
            Text(
                text = line,
                fontFamily = PressStart2P,
                fontSize = 6.sp,
                color = when {
                    line.contains("ENTRIES FOUND: 0") || line.contains("UNAVAILABLE") -> PokedexRed
                    else -> GlowBlue
                },
                lineHeight = 10.sp
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().alpha(buttonsAlpha),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "[ SKIP ]",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = PokedexCream.copy(alpha = 0.45f),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSkip() }
            )
            Text(
                text = "[ SYNC NOW ]",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = GlowBlue,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSyncNow() }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPokemonClick: (Int) -> Unit,
    onBack: () -> Unit,
    onNavigateFullList: () -> Unit,
    onNavigateMyCollection: () -> Unit,
    onNavigateTeam: () -> Unit = {},
    onNavigateSettings: () -> Unit,
    onSyncNow: () -> Unit = {},
    onAnimationStarted: () -> Unit = {},
    onAnimationEnded: () -> Unit = {}
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val cacheIsEmpty by viewModel.cacheIsEmpty.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var bootPromptDismissed by remember { mutableStateOf(viewModel.bootPromptAcknowledged) }
    var bootPromptVisible by remember { mutableStateOf(false) }
    val bootPromptAlpha by animateFloatAsState(
        targetValue = if (bootPromptVisible && !bootPromptDismissed) 1f else 0f,
        animationSpec = tween(500),
        label = "bootPrompt"
    )

    var phase by remember {
        mutableStateOf(
            if (viewModel.animationCompleted) PokedexPhase.INTERACTIVE else PokedexPhase.VIDEO
        )
    }

    val exoPlayer = remember {
        if (!viewModel.animationCompleted) {
            ExoPlayer.Builder(context).build().apply {
                val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.pdexanim}")
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
        } else null
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer?.release() }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    phase = PokedexPhase.INTERACTIVE
                    viewModel.markAnimationCompleted()
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose { exoPlayer?.removeListener(listener) }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { id -> onPokemonClick(id) }
    }

    LaunchedEffect(phase) {
        when (phase) {
            PokedexPhase.VIDEO -> onAnimationStarted()
            PokedexPhase.INTERACTIVE -> onAnimationEnded()
        }
    }

    LaunchedEffect(phase, cacheIsEmpty) {
        if (phase == PokedexPhase.INTERACTIVE && cacheIsEmpty && !viewModel.bootPromptAcknowledged) {
            delay(700)
            bootPromptVisible = true
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (phase == PokedexPhase.INTERACTIVE) 1f else 0f,
        animationSpec = tween(500), label = "content"
    )

    BoxWithConstraints(Modifier.fillMaxSize().swipeBack(onBack)) {
        val sw = maxWidth
        val sh = maxHeight
        val isLoading = uiState is SearchUiState.Loading

        if (phase == PokedexPhase.VIDEO) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(contentAlpha)
        )

        // Back button — exits app from home screen
        IconButton(
            onClick = onBack,
            modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp).alpha(contentAlpha)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        BasicTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            enabled = phase == PokedexPhase.INTERACTIVE,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            textStyle = TextStyle(
                fontFamily = PressStart2P,
                fontSize = 28.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(GlowBlue),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (query.isEmpty()) {
                        Text(
                            text = "ENTER NAME OR #",
                            fontFamily = PressStart2P,
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.35f),
                            textAlign = TextAlign.Center
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.44f)
                .padding(horizontal = 24.dp)
                .alpha(contentAlpha)
                .focusRequester(focusRequester)
        )

        // Broad tap zone covering the whole blue screen strip — brings up keyboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sh * 0.28f)
                .offset(y = sh * 0.35f)
                .alpha(contentAlpha)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = phase == PokedexPhase.INTERACTIVE
                ) { focusRequester.requestFocus() }
        )

        Box(
            modifier = Modifier
                .size(116.dp)
                .offset(x = sw * 0.5f - 58.dp, y = sh * 0.515f - 58.dp)
                .alpha(contentAlpha)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = phase == PokedexPhase.INTERACTIVE && !isLoading
                ) { viewModel.search() }
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = GlowBlue,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(22.dp)
                    .offset(x = sw * 0.5f - 11.dp, y = sh * 0.655f - 11.dp)
                    .alpha(contentAlpha)
            )
        }

        if (uiState is SearchUiState.NotFound) {
            Text(
                text = "NOT FOUND",
                fontFamily = PressStart2P,
                fontSize = 8.sp,
                color = PokedexRed,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = sh * 0.655f)
                    .padding(horizontal = 52.dp)
                    .alpha(contentAlpha)
            )
        }

        BottomNavBar(
            current = NavDestination.SEARCH,
            onNavigateSearch = {},
            onNavigateFullList = onNavigateFullList,
            onNavigateMyCollection = onNavigateMyCollection,
            onNavigateTeam = onNavigateTeam,
            onNavigateSettings = onNavigateSettings,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .alpha(contentAlpha)
        )

        if (phase == PokedexPhase.INTERACTIVE && query.isEmpty() && searchHistory.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = sh * 0.67f)
                    .padding(horizontal = 24.dp)
                    .alpha(contentAlpha),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                searchHistory.take(5).forEach { item ->
                    Text(
                        text = "▸ ${item.uppercase()}",
                        fontFamily = PressStart2P,
                        fontSize = 6.sp,
                        color = PokedexCream.copy(alpha = 0.55f),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.onQueryChange(item)
                            viewModel.search()
                        }
                    )
                }
            }
        }

        if (phase == PokedexPhase.INTERACTIVE && uiState !is SearchUiState.Loading) {
            Text(
                text = "? SURPRISE ME",
                fontFamily = PressStart2P,
                fontSize = 6.sp,
                color = GlowBlue.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp)
                    .alpha(contentAlpha)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.randomPokemon() }
            )
        }

        if (cacheIsEmpty && !bootPromptDismissed) {
            BootDiagnosticOverlay(
                sw = sw,
                sh = sh,
                alpha = bootPromptAlpha,
                onSyncNow = {
                    viewModel.acknowledgeBootPrompt()
                    bootPromptDismissed = true
                    onSyncNow()
                },
                onSkip = {
                    viewModel.acknowledgeBootPrompt()
                    bootPromptDismissed = true
                }
            )
        }
    }
}
