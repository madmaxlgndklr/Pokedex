package com.madmaxlgndklr.pokedex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CaughtPokemonEntity::class,
        PokemonListCacheEntity::class,
        PokemonDetailCacheEntity::class,
        MoveEntity::class,
        HeldItem::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caughtPokemonDao(): CaughtPokemonDao
    abstract fun pokemonListCacheDao(): PokemonListCacheDao
    abstract fun pokemonDetailCacheDao(): PokemonDetailCacheDao
    abstract fun moveDao(): MoveDao
    abstract fun heldItemDao(): HeldItemDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pokemon_list_cache " +
                    "(id INTEGER PRIMARY KEY NOT NULL, name TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pokemon_detail_cache " +
                    "(id INTEGER PRIMARY KEY NOT NULL, detailJson TEXT NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM pokemon_detail_cache")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS moves " +
                    "(name TEXT PRIMARY KEY NOT NULL, moveJson TEXT NOT NULL)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS held_items " +
                    "(id INTEGER PRIMARY KEY NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "displayName TEXT NOT NULL, " +
                    "effectSummary TEXT NOT NULL)"
                )
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pokedex.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
                    .also { INSTANCE = it }
            }
    }
}
