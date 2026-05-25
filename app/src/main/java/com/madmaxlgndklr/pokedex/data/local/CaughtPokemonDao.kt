package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaughtPokemonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CaughtPokemonEntity)

    @Delete
    suspend fun delete(entity: CaughtPokemonEntity)

    @Query("SELECT * FROM caught_pokemon ORDER BY caughtAt DESC")
    fun getAll(): Flow<List<CaughtPokemonEntity>>

    @Query("SELECT COUNT(*) > 0 FROM caught_pokemon WHERE id = :id")
    fun isCaught(id: Int): Flow<Boolean>
}
