package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PokemonListCacheDao {
    @Query("SELECT * FROM pokemon_list_cache ORDER BY id ASC")
    suspend fun getAll(): List<PokemonListCacheEntity>

    @Query("SELECT * FROM pokemon_list_cache WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): PokemonListCacheEntity?

    @Query("SELECT COUNT(*) FROM pokemon_list_cache")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PokemonListCacheEntity>)
}
