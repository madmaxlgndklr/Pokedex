package com.madmaxlgndklr.pokedex.ui.common

enum class Generation(val label: String, val idRange: IntRange, val pokedexName: String) {
    KANTO("KANTO",   1..151,    "kanto"),
    JOHTO("JOHTO",   152..251,  "original-johto"),
    HOENN("HOENN",   252..386,  "hoenn"),
    SINNOH("SINNOH", 387..493,  "original-sinnoh"),
    UNOVA("UNOVA",   494..649,  "original-unova"),
    KALOS("KALOS",   650..721,  "kalos-central"),
    ALOLA("ALOLA",   722..809,  "original-alola"),
    GALAR("GALAR",   810..905,  "galar"),
    PALDEA("PALDEA", 906..1025, "paldea")
}
