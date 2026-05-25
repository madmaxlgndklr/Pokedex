package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caught_pokemon")
data class CaughtPokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val caughtAt: Long = System.currentTimeMillis()
)
