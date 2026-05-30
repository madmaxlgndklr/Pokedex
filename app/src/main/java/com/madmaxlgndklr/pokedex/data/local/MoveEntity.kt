package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "moves")
data class MoveEntity(
    @PrimaryKey val name: String,
    val moveJson: String
)
