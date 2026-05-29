package com.madmaxlgndklr.pokedex.data.trainer

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.madmaxlgndklr.pokedex.ui.battle.Trainer
import com.madmaxlgndklr.pokedex.ui.battle.TrainerClass
import com.madmaxlgndklr.pokedex.ui.battle.TrainerPokemon
import com.madmaxlgndklr.pokedex.ui.battle.TrainerRoster

private data class TrainerJson(
    val id: String,
    val name: String,
    val title: String,
    val region: String,
    @SerializedName("trainerClass") val trainerClass: String,
    val typeSpecialty: String,
    val rosters: List<RosterJson>
)

private data class RosterJson(
    val label: String,
    val team: List<PokemonJson>
)

private data class PokemonJson(
    val pokemonId: Int,
    val level: Int,
    val moves: List<String>
)

private data class TrainersFile(
    val regions: List<RegionJson>
)

private data class RegionJson(
    val name: String,
    val trainers: List<TrainerJson>
)

class TrainerRepository(private val jsonProvider: () -> String) {

    constructor(context: Context) : this({
        context.assets.open("trainers.json").bufferedReader().readText()
    })

    // Secondary constructor for tests — accepts raw JSON string directly
    constructor(json: String) : this({ json })

    private val gson = Gson()
    private var cache: List<Trainer>? = null

    fun getAll(): List<Trainer> {
        cache?.let { return it }
        val loaded = try {
            val file = gson.fromJson(jsonProvider(), TrainersFile::class.java)
            file.regions.flatMap { region ->
                region.trainers.map { t ->
                    Trainer(
                        id           = t.id,
                        name         = t.name,
                        title        = t.title,
                        region       = region.name,
                        trainerClass = runCatching { TrainerClass.valueOf(t.trainerClass) }
                                           .getOrDefault(TrainerClass.GYM_LEADER),
                        typeSpecialty = t.typeSpecialty,
                        rosters      = t.rosters.map { r ->
                            TrainerRoster(
                                label = r.label,
                                team  = r.team.map { p ->
                                    TrainerPokemon(p.pokemonId, p.level, p.moves)
                                }
                            )
                        }
                    )
                }
            }
        } catch (e: Exception) { emptyList() }
        cache = loaded
        return loaded
    }

    fun getByRegion(region: String): List<Trainer> =
        getAll().filter { it.region == region }

    fun getById(id: String): Trainer? =
        getAll().firstOrNull { it.id == id }
}
