package com.madmaxlgndklr.pokedex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R

val PressStart2P = FontFamily(Font(R.font.press_start_2p))

val PokedexTypography = Typography(
    displayLarge  = TextStyle(fontFamily = PressStart2P, fontSize = 18.sp),
    titleLarge    = TextStyle(fontFamily = PressStart2P, fontSize = 14.sp),
    titleMedium   = TextStyle(fontFamily = PressStart2P, fontSize = 11.sp),
    bodyMedium    = TextStyle(fontFamily = PressStart2P, fontSize = 8.sp),
    labelSmall    = TextStyle(fontFamily = PressStart2P, fontSize = 7.sp),
)