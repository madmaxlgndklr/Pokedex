package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MoveDao {
    @Query("SELECT * FROM moves WHERE name = :name")
    suspend fun getByName(name: String): MoveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MoveEntity)
}
