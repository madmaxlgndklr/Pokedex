package com.madmaxlgndklr.pokedex.ui.common

sealed class DexSelection(val label: String, val pokedexName: String) {
    object National : DexSelection("NATIONAL DEX", "national")
    data class Regional(val gen: Generation) : DexSelection(gen.label, gen.pokedexName)
}
