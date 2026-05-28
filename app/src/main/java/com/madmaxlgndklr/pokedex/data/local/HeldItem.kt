package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "held_items")
data class HeldItem(
    @PrimaryKey val id: Int,
    val name: String,
    val displayName: String,
    val effectSummary: String
)

@Dao
interface HeldItemDao {
    @Query("SELECT * FROM held_items ORDER BY displayName ASC")
    suspend fun getAll(): List<HeldItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HeldItem>)

    @Query("DELETE FROM held_items")
    suspend fun deleteAll()
}
