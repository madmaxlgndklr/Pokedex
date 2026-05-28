package com.madmaxlgndklr.pokedex.ui.battle

sealed class HeldItemEffect {
    data class StatMultiplier(val statIndex: Int, val factor: Float) : HeldItemEffect()
    data class DamageMultiplier(val factor: Float) : HeldItemEffect()
    data class TypeMultiplier(val factor: Float) : HeldItemEffect()
    data class SuperEffectiveBoost(val factor: Float) : HeldItemEffect()
    object None : HeldItemEffect()

    companion object {
        private val TYPE_ITEMS = mapOf(
            "flame-plate"    to "fire",     "splash-plate"  to "water",
            "zap-plate"      to "electric", "meadow-plate"  to "grass",
            "icicle-plate"   to "ice",      "fist-plate"    to "fighting",
            "toxic-plate"    to "poison",   "earth-plate"   to "ground",
            "sky-plate"      to "flying",   "mind-plate"    to "psychic",
            "insect-plate"   to "bug",      "stone-plate"   to "rock",
            "spooky-plate"   to "ghost",    "draco-plate"   to "dragon",
            "dread-plate"    to "dark",     "iron-plate"    to "steel",
            "pixie-plate"    to "fairy",
            "charcoal"       to "fire",     "mystic-water"  to "water",
            "magnet"         to "electric", "miracle-seed"  to "grass",
            "never-melt-ice" to "ice",      "black-belt"    to "fighting",
            "poison-barb"    to "poison",   "soft-sand"     to "ground",
            "sharp-beak"     to "flying",   "twisted-spoon" to "psychic",
            "silver-powder"  to "bug",      "hard-stone"    to "rock",
            "spell-tag"      to "ghost",    "dragon-fang"   to "dragon",
            "black-glasses"  to "dark",     "metal-coat"    to "steel",
            "fairy-feather"  to "fairy"
        )

        fun from(name: String): HeldItemEffect = when {
            name == "choice-band"  -> StatMultiplier(statIndex = 1, factor = 1.5f)
            name == "choice-specs" -> StatMultiplier(statIndex = 3, factor = 1.5f)
            name == "life-orb"     -> DamageMultiplier(factor = 1.3f)
            name == "expert-belt"  -> SuperEffectiveBoost(factor = 1.2f)
            name in TYPE_ITEMS     -> TypeMultiplier(factor = 1.2f)
            else                   -> None
        }

        fun typeFor(name: String): String? = TYPE_ITEMS[name]
    }
}
