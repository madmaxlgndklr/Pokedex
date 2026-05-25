package com.madmaxlgndklr.pokedex.ui.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import kotlinx.coroutines.delay

private enum class PokedexPhase { CLOSED, OPENING, OPEN, INTERACTIVE }

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPokemonClick: (Int) -> Unit,
    onNavigateFullList: () -> Unit,
    onNavigateMyCollection: () -> Unit
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }

    var phase by remember {
        mutableStateOf(
            if (viewModel.animationCompleted) PokedexPhase.INTERACTIVE else PokedexPhase.CLOSED
        )
    }

    LaunchedEffect(Unit) {
        if (!viewModel.animationCompleted) {
            delay(300)
            phase = PokedexPhase.OPENING
            delay(750)
            phase = PokedexPhase.OPEN
            delay(700)
            phase = PokedexPhase.INTERACTIVE
            viewModel.markAnimationCompleted()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { id -> onPokemonClick(id) }
    }

    val closedAlpha by animateFloatAsState(
        targetValue = if (phase == PokedexPhase.CLOSED) 1f else 0f,
        animationSpec = tween(600), label = "closed"
    )
    val openingAlpha by animateFloatAsState(
        targetValue = if (phase == PokedexPhase.OPENING) 1f else 0f,
        animationSpec = tween(600), label = "opening"
    )
    val openAlpha by animateFloatAsState(
        targetValue = if (phase == PokedexPhase.OPEN || phase == PokedexPhase.INTERACTIVE) 1f else 0f,
        animationSpec = tween(600), label = "open"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (phase == PokedexPhase.INTERACTIVE) 1f else 0f,
        animationSpec = tween(500), label = "content"
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sw = maxWidth
        val sh = maxHeight
        val isLoading = uiState is SearchUiState.Loading

        // Animation frames — stacked, crossfade via alpha
        Image(
            painter = painterResource(R.drawable.pdex_closed),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(closedAlpha)
        )
        Image(
            painter = painterResource(R.drawable.pdex_opening),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(openingAlpha)
        )
        Image(
            painter = painterResource(R.drawable.pdex_open),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(openAlpha)
        )

        // Search text — appears directly on the blue screen strip, no background
        BasicTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            enabled = phase == PokedexPhase.INTERACTIVE,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            textStyle = TextStyle(
                fontFamily = PressStart2P,
                fontSize = 12.sp,
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
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.35f),
                            textAlign = TextAlign.Center
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.48f)
                .padding(horizontal = 52.dp)
                .alpha(contentAlpha)
        )

        // Invisible tap area covering the circular search button in the image
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

        // Invisible tap area for power button — opens navigation menu
        Box(
            modifier = Modifier
                .size(56.dp)
                .offset(x = sw * 0.5f - 28.dp, y = sh * 0.945f - 28.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = phase == PokedexPhase.INTERACTIVE
                ) { menuOpen = !menuOpen }
        )

        // Loading spinner — lower blue strip area
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

        // Not found — lower blue strip area
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

        // Navigation menu overlay — tap scrim to dismiss
        if (menuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { menuOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MenuOption("SEARCH") { menuOpen = false }
                    Spacer(Modifier.height(32.dp))
                    MenuOption("FULL POKEDEX") {
                        menuOpen = false
                        onNavigateFullList()
                    }
                    Spacer(Modifier.height(32.dp))
                    MenuOption("MY POKEDEX") {
                        menuOpen = false
                        onNavigateMyCollection()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuOption(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = PressStart2P,
        fontSize = 10.sp,
        color = PokedexCream,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    )
}
