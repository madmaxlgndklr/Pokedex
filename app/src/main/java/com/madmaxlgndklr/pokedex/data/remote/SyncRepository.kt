package com.madmaxlgndklr.pokedex.data.remote

import com.madmaxlgndklr.pokedex.data.local.AppDatabase
import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonEntity
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.local.TrainerRecord
import com.madmaxlgndklr.pokedex.data.local.WildRecord
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ── Remote DTOs ──────────────────────────────────────────────────────────────

@Serializable
data class RemoteCaughtRow(
    @SerialName("pokemon_id") val pokemonId: Int,
    @SerialName("caught_at") val caughtAt: Long
)

@Serializable
data class RemoteTeamRow(
    @SerialName("team_json") val teamJson: List<Int>,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class RemoteTrainerRow(
    @SerialName("trainer_id") val trainerId: String,
    val name: String,
    val title: String,
    val region: String,
    @SerialName("trainer_class") val trainerClass: String,
    @SerialName("type_specialty") val typeSpecialty: String,
    val wins: Int,
    val losses: Int,
    @SerialName("first_defeated_at") val firstDefeatedAt: Long?,
    @SerialName("last_battled_at") val lastBattledAt: Long
)

@Serializable
data class RemoteWildRow(
    @SerialName("pokemon_id") val pokemonId: Int,
    @SerialName("pokemon_name") val pokemonName: String,
    val wins: Int,
    val losses: Int,
    @SerialName("last_battled_at") val lastBattledAt: Long
)

@Serializable
data class RemoteBattleConfigRow(
    @SerialName("config_json") val configJson: JsonObject,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class RemoteSettingsRow(
    val generation: Int,
    @SerialName("music_on_launch") val musicOnLaunch: Boolean,
    @SerialName("trainer_name") val trainerName: String,
    @SerialName("updated_at") val updatedAt: Long
)

// ── Pure merge helpers ────────────────────────────────────────────────────────

object MergeUtils {
    data class LocalSettings(
        val generation: Int,
        val musicOnLaunch: Boolean,
        val trainerName: String,
        val updatedAt: Long
    )
    data class RemoteSettings(
        val generation: Int,
        val musicOnLaunch: Boolean,
        val trainerName: String,
        val updatedAt: Long
    )

    fun mergeCaughtPokemon(local: List<Int>, remote: List<Int>): List<Int> =
        (local + remote).distinct()

    fun mergeTeam(local: List<Int>, localUpdatedAt: Long, remote: List<Int>?, remoteUpdatedAt: Long?): List<Int> {
        if (remote == null || remoteUpdatedAt == null) return local
        return if (remoteUpdatedAt > localUpdatedAt) remote else local
    }

    fun mergeTrainerRecords(local: List<TrainerRecord>, remote: List<RemoteTrainerRow>): List<TrainerRecord> {
        val map = mutableMapOf<String, TrainerRecord>()
        for (r in local) map[r.trainerId] = r.copy()
        for (r in remote) {
            val existing = map[r.trainerId]
            if (existing != null) {
                val localFirst = existing.firstDefeatedAt
                val remoteFirst = r.firstDefeatedAt
                map[r.trainerId] = existing.copy(
                    wins = maxOf(existing.wins, r.wins),
                    losses = maxOf(existing.losses, r.losses),
                    firstDefeatedAt = when {
                        localFirst != null && remoteFirst != null -> minOf(localFirst, remoteFirst)
                        else -> localFirst ?: remoteFirst
                    },
                    lastBattledAt = maxOf(existing.lastBattledAt, r.lastBattledAt)
                )
            } else {
                map[r.trainerId] = TrainerRecord(
                    trainerId = r.trainerId, name = r.name, title = r.title,
                    region = r.region, trainerClass = r.trainerClass, typeSpecialty = r.typeSpecialty,
                    wins = r.wins, losses = r.losses,
                    firstDefeatedAt = r.firstDefeatedAt,
                    lastBattledAt = r.lastBattledAt
                )
            }
        }
        return map.values.toList()
    }

    fun mergeWildRecords(local: List<WildRecord>, remote: List<RemoteWildRow>): List<WildRecord> {
        val map = mutableMapOf<Int, WildRecord>()
        for (r in local) map[r.pokemonId] = r.copy()
        for (r in remote) {
            val existing = map[r.pokemonId]
            if (existing != null) {
                map[r.pokemonId] = existing.copy(
                    wins = maxOf(existing.wins, r.wins),
                    losses = maxOf(existing.losses, r.losses),
                    lastBattledAt = maxOf(existing.lastBattledAt, r.lastBattledAt)
                )
            } else {
                map[r.pokemonId] = WildRecord(r.pokemonId, r.pokemonName, r.wins, r.losses, r.lastBattledAt)
            }
        }
        return map.values.toList()
    }

    fun mergeBattleConfig(localJson: String, localUpdatedAt: Long, remoteJson: String?, remoteUpdatedAt: Long?): String {
        if (remoteJson == null || remoteUpdatedAt == null) return localJson
        return if (remoteUpdatedAt > localUpdatedAt) remoteJson else localJson
    }

    fun mergeSettings(local: LocalSettings, remote: RemoteSettings?): LocalSettings {
        if (remote == null) return local
        return if (remote.updatedAt > local.updatedAt)
            LocalSettings(remote.generation, remote.musicOnLaunch, remote.trainerName, remote.updatedAt)
        else local
    }
}

// ── SyncRepository ────────────────────────────────────────────────────────────

class SyncRepository(
    private val db: AppDatabase,
    private val settingsRepo: SettingsRepository,
    private val scope: CoroutineScope
) {
    private val supabase = SupabaseModule.client
    private val pg = supabase.postgrest
    private val auth = supabase.auth

    private fun userId(): String? = auth.currentUserOrNull()?.id

    suspend fun syncOnOpen() {
        val uid = userId() ?: return
        try {
            val remote = pullAll(uid)
            writeLocal(remote)
            pushDiff(uid, remote)
        } catch (_: Exception) { }
    }

    private suspend fun pullAll(userId: String): RemoteState {
        val caught = pg.from("caught_pokemon")
            .select(Columns.list("pokemon_id", "caught_at")) { filter { eq("user_id", userId) } }
            .decodeList<RemoteCaughtRow>()

        val team = pg.from("team")
            .select(Columns.list("team_json", "updated_at")) { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<RemoteTeamRow>()

        val trainers = pg.from("trainer_records")
            .select { filter { eq("user_id", userId) } }
            .decodeList<RemoteTrainerRow>()

        val wild = pg.from("wild_records")
            .select { filter { eq("user_id", userId) } }
            .decodeList<RemoteWildRow>()

        val config = pg.from("battle_config")
            .select(Columns.list("config_json", "updated_at")) { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<RemoteBattleConfigRow>()

        val settings = pg.from("settings")
            .select(Columns.list("generation", "music_on_launch", "trainer_name", "updated_at")) { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<RemoteSettingsRow>()

        return RemoteState(caught, team, trainers, wild, config, settings)
    }

    data class RemoteState(
        val caught: List<RemoteCaughtRow>,
        val team: RemoteTeamRow?,
        val trainers: List<RemoteTrainerRow>,
        val wild: List<RemoteWildRow>,
        val battleConfig: RemoteBattleConfigRow?,
        val settings: RemoteSettingsRow?
    )

    private suspend fun writeLocal(remote: RemoteState) {
        val battleDao = db.battleRecordDao()
        val caughtDao = db.caughtPokemonDao()

        // caught_pokemon — stored in Room via CaughtPokemonEntity(id, name, caughtAt)
        val localCaughtIds = caughtDao.getAllIds()
        // Insert remote-only entries (name unknown at sync time; use empty string as placeholder)
        val localSet = localCaughtIds.toSet()
        val toInsert = remote.caught
            .filter { it.pokemonId !in localSet }
            .map { CaughtPokemonEntity(id = it.pokemonId, name = "", caughtAt = it.caughtAt) }
        if (toInsert.isNotEmpty()) {
            caughtDao.insertAll(toInsert)
        }

        // team
        val localTeamIds = settingsRepo.team.first()
        val localTeamUpdatedAt = settingsRepo.teamUpdatedAt.first()
        val mergedTeam = MergeUtils.mergeTeam(localTeamIds, localTeamUpdatedAt, remote.team?.teamJson, remote.team?.updatedAt)
        if (mergedTeam != localTeamIds) {
            settingsRepo.setTeam(mergedTeam)
        }

        // trainer_records
        val localTrainers = battleDao.getAllTrainerRecords()
        val mergedTrainers = MergeUtils.mergeTrainerRecords(localTrainers, remote.trainers)
        battleDao.replaceAllTrainerRecords(mergedTrainers)

        // wild_records
        val localWild = battleDao.getAllWildRecords()
        val mergedWild = MergeUtils.mergeWildRecords(localWild, remote.wild)
        battleDao.replaceAllWildRecords(mergedWild)

        // battle_config
        val localConfigJson = settingsRepo.loadBattleConfigJson() ?: "{}"
        val localConfigUpdatedAt = settingsRepo.battleConfigUpdatedAt.first()
        val mergedConfig = MergeUtils.mergeBattleConfig(
            localConfigJson, localConfigUpdatedAt,
            remote.battleConfig?.configJson?.toString(), remote.battleConfig?.updatedAt
        )
        if (mergedConfig != localConfigJson) {
            settingsRepo.saveBattleConfig(mergedConfig)
        }

        // settings
        val localGen = settingsRepo.selectedGen.first()
        val localMusic = settingsRepo.musicOnLaunch.first()
        val localTrainerName = settingsRepo.trainerName.first()
        val localSettingsUpdatedAt = settingsRepo.settingsUpdatedAt.first()
        val localSettings = MergeUtils.LocalSettings(localGen, localMusic, localTrainerName, localSettingsUpdatedAt)
        val remoteSettings = remote.settings?.let {
            MergeUtils.RemoteSettings(it.generation, it.musicOnLaunch, it.trainerName, it.updatedAt)
        }
        val merged = MergeUtils.mergeSettings(localSettings, remoteSettings)
        if (merged != localSettings) {
            settingsRepo.setGen(merged.generation)
            settingsRepo.setMusicOnLaunch(merged.musicOnLaunch)
            settingsRepo.setTrainerName(merged.trainerName)
        }
    }

    private suspend fun pushDiff(userId: String, remote: RemoteState) {
        val battleDao = db.battleRecordDao()
        val caughtDao = db.caughtPokemonDao()

        // caught_pokemon
        val remoteCaughtIds = remote.caught.map { it.pokemonId }.toSet()
        val localCaughtIds = caughtDao.getAllIds()
        val missing = localCaughtIds.filter { it !in remoteCaughtIds }
        if (missing.isNotEmpty()) {
            val now = System.currentTimeMillis()
            pg.from("caught_pokemon").upsert(missing.map { mapOf("user_id" to userId, "pokemon_id" to it, "caught_at" to now) })
        }

        // trainer_records
        val trainers = battleDao.getAllTrainerRecords()
        if (trainers.isNotEmpty()) {
            pg.from("trainer_records").upsert(trainers.map { t ->
                mapOf(
                    "user_id" to userId, "trainer_id" to t.trainerId,
                    "name" to t.name, "title" to t.title, "region" to t.region,
                    "trainer_class" to t.trainerClass, "type_specialty" to t.typeSpecialty,
                    "wins" to t.wins, "losses" to t.losses,
                    "first_defeated_at" to t.firstDefeatedAt,
                    "last_battled_at" to t.lastBattledAt
                )
            })
        }

        // wild_records
        val wildRecords = battleDao.getAllWildRecords()
        if (wildRecords.isNotEmpty()) {
            pg.from("wild_records").upsert(wildRecords.map { w ->
                mapOf(
                    "user_id" to userId, "pokemon_id" to w.pokemonId,
                    "pokemon_name" to w.pokemonName,
                    "wins" to w.wins, "losses" to w.losses,
                    "last_battled_at" to w.lastBattledAt
                )
            })
        }
    }

    fun pushCaughtToggle(pokemonId: Int, isCaught: Boolean) {
        val uid = userId() ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                if (isCaught) {
                    pg.from("caught_pokemon").upsert(mapOf("user_id" to uid, "pokemon_id" to pokemonId, "caught_at" to System.currentTimeMillis()))
                } else {
                    pg.from("caught_pokemon").delete { filter { eq("user_id", uid); eq("pokemon_id", pokemonId) } }
                }
            }
        }
    }

    fun pushTeam(teamIds: List<Int>) {
        val uid = userId() ?: return
        val now = System.currentTimeMillis()
        scope.launch(Dispatchers.IO) {
            runCatching { pg.from("team").upsert(mapOf("user_id" to uid, "team_json" to teamIds, "updated_at" to now)) }
        }
    }

    fun pushTrainerRecord(record: TrainerRecord) {
        val uid = userId() ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                pg.from("trainer_records").upsert(mapOf(
                    "user_id" to uid, "trainer_id" to record.trainerId,
                    "name" to record.name, "title" to record.title, "region" to record.region,
                    "trainer_class" to record.trainerClass, "type_specialty" to record.typeSpecialty,
                    "wins" to record.wins, "losses" to record.losses,
                    "first_defeated_at" to record.firstDefeatedAt,
                    "last_battled_at" to record.lastBattledAt
                ))
            }
        }
    }

    fun pushWildRecord(record: WildRecord) {
        val uid = userId() ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                pg.from("wild_records").upsert(mapOf(
                    "user_id" to uid, "pokemon_id" to record.pokemonId,
                    "pokemon_name" to record.pokemonName,
                    "wins" to record.wins, "losses" to record.losses,
                    "last_battled_at" to record.lastBattledAt
                ))
            }
        }
    }

    fun pushBattleConfig(configJson: String, updatedAt: Long) {
        val uid = userId() ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                pg.from("battle_config").upsert(mapOf(
                    "user_id" to uid,
                    "config_json" to kotlinx.serialization.json.Json.parseToJsonElement(configJson),
                    "updated_at" to updatedAt
                ))
            }
        }
    }

    fun pushSettings(generation: Int, musicOnLaunch: Boolean, trainerName: String, updatedAt: Long) {
        val uid = userId() ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                pg.from("settings").upsert(mapOf(
                    "user_id" to uid,
                    "generation" to generation,
                    "music_on_launch" to musicOnLaunch,
                    "trainer_name" to trainerName,
                    "updated_at" to updatedAt
                ))
            }
        }
    }
}
