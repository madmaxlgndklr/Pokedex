package com.madmaxlgndklr.pokedex.data.repository

import com.madmaxlgndklr.pokedex.data.local.BattleRecordDao
import com.madmaxlgndklr.pokedex.data.local.TrainerRecord
import com.madmaxlgndklr.pokedex.data.local.WildRecord
import com.madmaxlgndklr.pokedex.ui.battle.Trainer

class BattleRecordRepository(private val dao: BattleRecordDao) {

    suspend fun getAllTrainerRecords(): List<TrainerRecord> = dao.getAllTrainerRecords()
    suspend fun getAllWildRecords(): List<WildRecord> = dao.getAllWildRecords()

    suspend fun recordTrainerBattle(trainer: Trainer, won: Boolean) {
        val existing = dao.getTrainerRecord(trainer.id)
        val now = System.currentTimeMillis()
        val updated = if (existing != null) {
            existing.copy(
                wins = if (won) existing.wins + 1 else existing.wins,
                losses = if (!won) existing.losses + 1 else existing.losses,
                firstDefeatedAt = if (won && existing.firstDefeatedAt == null) now else existing.firstDefeatedAt,
                lastBattledAt = now
            )
        } else {
            TrainerRecord(
                trainerId = trainer.id,
                name = trainer.name,
                title = trainer.title,
                region = trainer.region,
                trainerClass = trainer.trainerClass.name,
                typeSpecialty = trainer.typeSpecialty,
                wins = if (won) 1 else 0,
                losses = if (!won) 1 else 0,
                firstDefeatedAt = if (won) now else null,
                lastBattledAt = now
            )
        }
        dao.upsertTrainerRecord(updated)
    }

    suspend fun recordWildBattle(pokemonId: Int, pokemonName: String, won: Boolean) {
        val existing = dao.getWildRecord(pokemonId)
        val now = System.currentTimeMillis()
        val updated = if (existing != null) {
            existing.copy(
                wins = if (won) existing.wins + 1 else existing.wins,
                losses = if (!won) existing.losses + 1 else existing.losses,
                lastBattledAt = now
            )
        } else {
            WildRecord(
                pokemonId = pokemonId,
                pokemonName = pokemonName,
                wins = if (won) 1 else 0,
                losses = if (!won) 1 else 0,
                lastBattledAt = now
            )
        }
        dao.upsertWildRecord(updated)
    }
}
