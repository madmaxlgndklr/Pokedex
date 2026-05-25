package com.madmaxlgndklr.pokedex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PokedexColorScheme = darkColorScheme(
    primary          = PokedexRed,
    onPrimary        = PokedexCream,
    primaryContainer = PokedexDarkRed,
    background       = PokedexDark,
    onBackground     = PokedexCream,
    surface          = PokedexDark,
    onSurface        = PokedexCream,
    secondary        = PokedexGreen,
    onSecondary      = PokedexDark
)

@Composable
fun PokedexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PokedexColorScheme,
        typography  = PokedexTypography,
        content     = content
    )
}