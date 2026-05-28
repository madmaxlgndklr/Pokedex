package com.madmaxlgndklr.pokedex.data.repository

import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.data.local.HeldItemDao
import com.madmaxlgndklr.pokedex.data.remote.PokeApiService
import com.madmaxlgndklr.pokedex.ui.battle.HeldItemEffect

class HeldItemRepository(
    private val api: PokeApiService,
    private val dao: HeldItemDao
) {
    suspend fun getAll(): List<HeldItem> = dao.getAll()

    suspend fun syncAll() {
        val active = api.getItemAttribute("holdable-active").items
        val passive = api.getItemAttribute("holdable-passive").items
        val allNames = (active + passive).map { it.name }.distinct()
        val items = allNames.mapNotNull { name ->
            try {
                val detail = api.getItem(name)
                val displayName = detail.names
                    .firstOrNull { it.language.name == "en" }?.name
                    ?: name.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
                val effect = HeldItemEffect.from(name)
                val summary = when (effect) {
                    is HeldItemEffect.StatMultiplier      -> "Boosts a stat by ×${effect.factor}"
                    is HeldItemEffect.DamageMultiplier    -> "Boosts damage by ×${effect.factor}"
                    is HeldItemEffect.TypeMultiplier      -> "Boosts matching move type"
                    is HeldItemEffect.SuperEffectiveBoost -> "Boosts super-effective moves"
                    HeldItemEffect.None                   -> "No battle effect"
                }
                HeldItem(id = detail.id, name = name, displayName = displayName, effectSummary = summary)
            } catch (_: Exception) { null }
        }
        dao.deleteAll()
        dao.upsertAll(items)
    }
}
