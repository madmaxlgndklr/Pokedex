package com.madmaxlgndklr.pokedex.ui.theme

import androidx.compose.ui.graphics.Color

val PokedexRed      = Color(0xFFCC0000)
val PokedexDarkRed  = Color(0xFF8B0000)
val PokedexGreen    = Color(0xFF88CC00)
val PokedexDark     = Color(0xFF1A1A1A)
val PokedexCream    = Color(0xFFF5F5DC)
val CaughtGold      = Color(0xFFFFD700)
val PokedexLightBlue = Color(0xFFB8DCE8)
val PokedexMetal    = Color(0xFF1C1C1C)
val PokedexMetalRim = Color(0xFF505050)
val GlowBlue        = Color(0xFF00C8FF)

val TypeNormal    = Color(0xFFA8A878)
val TypeFire      = Color(0xFFF08030)
val TypeWater     = Color(0xFF6890F0)
val TypeElectric  = Color(0xFFF8D030)
val TypeGrass     = Color(0xFF78C850)
val TypeIce       = Color(0xFF98D8D8)
val TypeFighting  = Color(0xFFC03028)
val TypePoison    = Color(0xFFA040A0)
val TypeGround    = Color(0xFFE0C068)
val TypeFlying    = Color(0xFFA890F0)
val TypePsychic   = Color(0xFFF85888)
val TypeBug       = Color(0xFFA8B820)
val TypeRock      = Color(0xFFB8A038)
val TypeGhost     = Color(0xFF705898)
val TypeDragon    = Color(0xFF7038F8)
val TypeDark      = Color(0xFF705848)
val TypeSteel     = Color(0xFFB8B8D0)
val TypeFairy     = Color(0xFFEE99AC)

fun typeColor(type: String): Color = when (type.lowercase()) {
    "normal"   -> TypeNormal
    "fire"     -> TypeFire
    "water"    -> TypeWater
    "electric" -> TypeElectric
    "grass"    -> TypeGrass
    "ice"      -> TypeIce
    "fighting" -> TypeFighting
    "poison"   -> TypePoison
    "ground"   -> TypeGround
    "flying"   -> TypeFlying
    "psychic"  -> TypePsychic
    "bug"      -> TypeBug
    "rock"     -> TypeRock
    "ghost"    -> TypeGhost
    "dragon"   -> TypeDragon
    "dark"     -> TypeDark
    "steel"    -> TypeSteel
    "fairy"    -> TypeFairy
    else       -> TypeNormal
}