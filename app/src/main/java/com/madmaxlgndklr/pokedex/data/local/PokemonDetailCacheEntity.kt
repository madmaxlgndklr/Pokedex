package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon_detail_cache")
data class PokemonDetailCacheEntity(
    @PrimaryKey val id: Int,
    val detailJson: String
)
