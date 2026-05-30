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
        HeldItem::class,
        TrainerRecord::class,
        WildRecord::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caughtPokemonDao(): CaughtPokemonDao
    abstract fun pokemonListCacheDao(): PokemonListCacheDao
    abstract fun pokemonDetailCacheDao(): PokemonDetailCacheDao
    abstract fun moveDao(): MoveDao
    abstract fun heldItemDao(): HeldItemDao
    abstract fun battleRecordDao(): BattleRecordDao

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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pokemon_list_cache_name ON pokemon_list_cache (name)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS trainer_records (" +
                    "trainerId TEXT PRIMARY KEY NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "region TEXT NOT NULL, " +
                    "trainerClass TEXT NOT NULL, " +
                    "typeSpecialty TEXT NOT NULL, " +
                    "wins INTEGER NOT NULL DEFAULT 0, " +
                    "losses INTEGER NOT NULL DEFAULT 0, " +
                    "firstDefeatedAt INTEGER, " +
                    "lastBattledAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS wild_records (" +
                    "pokemonId INTEGER PRIMARY KEY NOT NULL, " +
                    "pokemonName TEXT NOT NULL, " +
                    "wins INTEGER NOT NULL DEFAULT 0, " +
                    "losses INTEGER NOT NULL DEFAULT 0, " +
                    "lastBattledAt INTEGER NOT NULL)"
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()
                    .also { INSTANCE = it }
            }
    }
}
