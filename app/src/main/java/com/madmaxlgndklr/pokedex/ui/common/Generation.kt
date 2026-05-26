package com.madmaxlgndklr.pokedex.ui.common

enum class Generation(val label: String, val idRange: IntRange) {
    KANTO("KANTO",   1..151),
    JOHTO("JOHTO",   152..251),
    HOENN("HOENN",   252..386),
    SINNOH("SINNOH", 387..493),
    UNOVA("UNOVA",   494..649),
    KALOS("KALOS",   650..721),
    ALOLA("ALOLA",   722..809),
    GALAR("GALAR",   810..905),
    PALDEA("PALDEA", 906..1025)
}
