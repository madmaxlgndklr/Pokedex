package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon_list_cache")
data class PokemonListCacheEntity(
    @PrimaryKey val id: Int,
    val name: String
)
