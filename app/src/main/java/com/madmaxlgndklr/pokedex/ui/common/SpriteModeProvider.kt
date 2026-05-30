package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalSpriteMode = compositionLocalOf { "modern" }

@Composable
fun SpriteModeProvider(mode: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSpriteMode provides mode) {
        content()
    }
}
