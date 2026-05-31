package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

@Entity(tableName = "trainer_records")
data class TrainerRecord(
    @PrimaryKey val trainerId: String,
    val name: String,
    val title: String,
    val region: String,
    val trainerClass: String,
    val typeSpecialty: String,
    val wins: Int = 0,
    val losses: Int = 0,
    val firstDefeatedAt: Long? = null,
    val lastBattledAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wild_records")
data class WildRecord(
    @PrimaryKey val pokemonId: Int,
    val pokemonName: String,
    val wins: Int = 0,
    val losses: Int = 0,
    val lastBattledAt: Long = System.currentTimeMillis()
)

@Dao
interface BattleRecordDao {
    @Query("SELECT * FROM trainer_records ORDER BY lastBattledAt DESC")
    suspend fun getAllTrainerRecords(): List<TrainerRecord>

    @Query("SELECT * FROM trainer_records WHERE trainerId = :id")
    suspend fun getTrainerRecord(id: String): TrainerRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrainerRecord(record: TrainerRecord)

    @Query("SELECT * FROM wild_records ORDER BY (wins + losses) DESC")
    suspend fun getAllWildRecords(): List<WildRecord>

    @Query("SELECT * FROM wild_records WHERE pokemonId = :id")
    suspend fun getWildRecord(id: Int): WildRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWildRecord(record: WildRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTrainerRecords(records: List<TrainerRecord>)

    @Query("DELETE FROM trainer_records")
    suspend fun deleteAllTrainerRecords()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWildRecords(records: List<WildRecord>)

    @Query("DELETE FROM wild_records")
    suspend fun deleteAllWildRecords()

    @Transaction
    suspend fun replaceAllTrainerRecords(records: List<TrainerRecord>) {
        deleteAllTrainerRecords()
        if (records.isNotEmpty()) insertAllTrainerRecords(records)
    }

    @Transaction
    suspend fun replaceAllWildRecords(records: List<WildRecord>) {
        deleteAllWildRecords()
        if (records.isNotEmpty()) insertAllWildRecords(records)
    }
}
