package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PokemonDetailCacheDao {
    @Query("SELECT * FROM pokemon_detail_cache WHERE id = :id")
    suspend fun getById(id: Int): PokemonDetailCacheEntity?

    @Query("SELECT COUNT(*) FROM pokemon_detail_cache")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PokemonDetailCacheEntity)
}
