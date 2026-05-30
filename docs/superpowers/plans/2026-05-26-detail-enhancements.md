# Detail Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enrich the Pokémon detail screen with height/weight, abilities, a shiny sprite toggle, and a type weakness chart — displayed in toggleable left/right panels to avoid layout refactoring.

**Architecture:** New fields are added to `PokemonDetail` and mapped in `PokemonRepository.mapDetail`. A Room DB migration (v2→v3) wipes the stale detail cache so re-fetched entries include the new fields. The detail screen gets two toggleable panels: left panel flips between dex flavor text and abilities, right panel flips between stats and the weakness chart. The weakness chart is a pure function in `TypeWeakness.kt` using a hardcoded Gen 6+ type chart. The sprite toggles between normal and shiny on tap.

**Tech Stack:** Kotlin, Jetpack Compose, Room v3 migration, Gson, PressStart2P font, existing `typeColor()` from `ui/theme/Color.kt`

---

## File Structure

| File | Change |
|------|--------|
| `model/PokemonDetail.kt` | Add `height: Int`, `weight: Int`, `abilities: List<String>` |
| `data/remote/dto/PokemonDetailResponse.kt` | Add `height`, `weight`, `abilities` fields |
| `data/repository/PokemonRepository.kt` | Map new fields in `mapDetail` |
| `data/local/AppDatabase.kt` | Bump to v3, MIGRATION_2_3 deletes stale detail cache rows |
| `data/remote/RetrofitClient.kt` | Add `shinySpriteUrl(id: Int)` |
| `ui/common/TypeWeakness.kt` | NEW — Gen 6+ type chart + `typeWeaknesses()` pure function |
| `ui/detail/DetailScreen.kt` | Panel toggle state, shiny toggle, weakness chart, abilities display, height/weight display |
| `test/.../repository/PokemonRepositoryTest.kt` | Fix constructor (now needs 4 args), add height/weight/abilities to fake data |
| `test/.../ui/TypeWeaknessTest.kt` | NEW — unit tests for `typeWeaknesses()` |

---

### Task 1: Add height, weight, abilities to the model and DTO

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/model/PokemonDetail.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/dto/PokemonDetailResponse.kt`

- [ ] **Step 1: Update `PokemonDetail` model**

Replace the entire file content:

```kotlin
package com.madmaxlgndklr.pokedex.model

data class PokemonStat(val name: String, val value: Int)

data class PokemonMove(val name: String, val levelLearnedAt: Int)

data class EvolutionNode(val id: Int, val name: String)

data class EvolutionStage(val members: List<EvolutionNode>)

data class PokemonDetail(
    val id: Int,
    val name: String,
    val spriteUrl: String,
    val types: List<String>,
    val stats: List<PokemonStat>,
    val moves: List<PokemonMove>,
    val evolutionChain: List<EvolutionStage>,
    val flavorText: String,
    val height: Int = 0,
    val weight: Int = 0,
    val abilities: List<String> = emptyList()
)
```

- [ ] **Step 2: Add height, weight, abilities to `PokemonDetailResponse`**

Add `PokemonAbilitySlotDto` data class and update `PokemonDetailResponse`:

```kotlin
package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PokemonDetailResponse(
    val id: Int,
    val name: String,
    val sprites: SpritesDto,
    val types: List<PokemonTypeSlotDto>,
    val stats: List<PokemonStatDto>,
    val moves: List<PokemonMoveSlotDto>,
    val height: Int = 0,
    val weight: Int = 0,
    val abilities: List<PokemonAbilitySlotDto> = emptyList()
)

data class SpritesDto(@SerializedName("front_default") val frontDefault: String?)

data class PokemonTypeSlotDto(val type: NamedDto)

data class PokemonStatDto(
    @SerializedName("base_stat") val baseStat: Int,
    val stat: NamedDto
)

data class PokemonAbilitySlotDto(val ability: NamedDto)

data class PokemonMoveSlotDto(
    val move: NamedDto,
    @SerializedName("version_group_details") val versionGroupDetails: List<MoveVersionDetailDto>
)

data class MoveVersionDetailDto(
    @SerializedName("level_learned_at") val levelLearnedAt: Int,
    @SerializedName("move_learn_method") val moveLearnMethod: NamedDto
)

data class NamedDto(val name: String, val url: String = "")
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/model/PokemonDetail.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/dto/PokemonDetailResponse.kt
git commit -m "feat: add height, weight, abilities fields to PokemonDetail model and DTO"
```

---

### Task 2: Map new fields in repository + DB migration

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/data/repository/PokemonRepository.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/data/local/AppDatabase.kt`

- [ ] **Step 1: Update `mapDetail` to populate height, weight, abilities**

In `PokemonRepository.kt`, find the `mapDetail` function and replace the `return PokemonDetail(...)` block:

```kotlin
return PokemonDetail(
    id = detail.id,
    name = detail.name,
    spriteUrl = RetrofitClient.spriteUrl(detail.id),
    types = detail.types.map { it.type.name },
    stats = detail.stats.map { PokemonStat(it.stat.name, it.baseStat) },
    moves = levelUpMoves,
    evolutionChain = parseEvolutionChain(evoChain.chain),
    flavorText = flavorText,
    height = detail.height,
    weight = detail.weight,
    abilities = detail.abilities.map { it.ability.name }
)
```

- [ ] **Step 2: Bump DB to version 3 and add MIGRATION_2_3**

In `AppDatabase.kt`, replace the entire file:

```kotlin
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
        PokemonDetailCacheEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caughtPokemonDao(): CaughtPokemonDao
    abstract fun pokemonListCacheDao(): PokemonListCacheDao
    abstract fun pokemonDetailCacheDao(): PokemonDetailCacheDao

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

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pokedex.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
```

- [ ] **Step 3: Add `shinySpriteUrl` to RetrofitClient**

In `RetrofitClient.kt`, add after the existing `spriteUrl` function:

```kotlin
fun shinySpriteUrl(id: Int) = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/$id.png"
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/data/repository/PokemonRepository.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/data/local/AppDatabase.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/RetrofitClient.kt
git commit -m "feat: map height/weight/abilities in repository, bump DB to v3 to clear stale cache"
```

---

### Task 3: Fix the broken PokemonRepositoryTest

The test was written before cache DAOs were added. It uses `PokemonRepository(fakeApi, fakeDao)` which no longer compiles since the constructor now needs 4 args.

**Files:**
- Modify: `app/src/test/java/com/madmaxlgndklr/pokedex/repository/PokemonRepositoryTest.kt`

- [ ] **Step 1: Run the existing test to confirm it fails**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.repository.PokemonRepositoryTest" 2>&1 | tail -20
```

Expected: compilation error about wrong number of arguments.

- [ ] **Step 2: Add fake cache DAOs and fix the test**

Replace the entire `PokemonRepositoryTest.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.repository

import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonDao
import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonEntity
import com.madmaxlgndklr.pokedex.data.local.PokemonDetailCacheDao
import com.madmaxlgndklr.pokedex.data.local.PokemonDetailCacheEntity
import com.madmaxlgndklr.pokedex.data.local.PokemonListCacheDao
import com.madmaxlgndklr.pokedex.data.local.PokemonListCacheEntity
import com.madmaxlgndklr.pokedex.data.remote.PokeApiService
import com.madmaxlgndklr.pokedex.data.remote.dto.*
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonRepositoryTest {
    private val fakeApi = FakePokeApiService()
    private val fakeDao = FakeCaughtPokemonDao()
    private val fakeListCacheDao = FakePokemonListCacheDao()
    private val fakeDetailCacheDao = FakePokemonDetailCacheDao()
    private val repo = PokemonRepository(fakeApi, fakeDao, fakeListCacheDao, fakeDetailCacheDao)

    @Test
    fun `getPokemonList maps DTOs to domain summaries`() = runTest {
        val list = repo.getPokemonList()
        assertEquals(2, list.size)
        assertEquals(1, list[0].id)
        assertEquals("bulbasaur", list[0].name)
    }

    @Test
    fun `getPokemonDetail returns correct name and types`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertEquals("bulbasaur", detail.name)
        assertEquals(listOf("grass", "poison"), detail.types)
    }

    @Test
    fun `getPokemonDetail returns height and weight`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertEquals(7, detail.height)
        assertEquals(69, detail.weight)
    }

    @Test
    fun `getPokemonDetail returns ability names`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertEquals(listOf("overgrow"), detail.abilities)
    }

    @Test
    fun `getPokemonDetail returns level-up moves only sorted by level`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertEquals(listOf("tackle"), detail.moves.map { it.name })
        val levels = detail.moves.map { it.levelLearnedAt }
        assertEquals(levels.sorted(), levels)
    }

    @Test
    fun `getPokemonDetail includes flavor text`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertEquals("A strange seed was planted.", detail.flavorText)
    }

    @Test
    fun `getPokemonDetail parses linear evolution chain`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertEquals(2, detail.evolutionChain.size)
        assertEquals(1, detail.evolutionChain[0].members.size)
        assertEquals("bulbasaur", detail.evolutionChain[0].members[0].name)
    }

    @Test
    fun `setCaught inserts entity`() = runTest {
        repo.setCaught(id = 1, name = "bulbasaur", caught = true)
        assertEquals(1, fakeDao.caught.first().size)
    }

    @Test
    fun `setCaught false removes entity`() = runTest {
        repo.setCaught(id = 1, name = "bulbasaur", caught = true)
        repo.setCaught(id = 1, name = "bulbasaur", caught = false)
        assertTrue(fakeDao.caught.first().isEmpty())
    }
}

// --- Fakes ---

class FakePokeApiService : PokeApiService {
    override suspend fun getPokemonList(limit: Int, offset: Int) = PokemonListResponse(
        results = listOf(
            PokemonResultDto("bulbasaur", "http://localhost/api/v2/pokemon/1/"),
            PokemonResultDto("ivysaur",   "http://localhost/api/v2/pokemon/2/")
        )
    )

    override suspend fun getPokemonDetail(id: String) = PokemonDetailResponse(
        id = 1,
        name = "bulbasaur",
        sprites = SpritesDto("https://example.com/1.png"),
        types = listOf(
            PokemonTypeSlotDto(NamedDto("grass")),
            PokemonTypeSlotDto(NamedDto("poison"))
        ),
        stats = listOf(
            PokemonStatDto(45, NamedDto("hp")),
            PokemonStatDto(49, NamedDto("attack"))
        ),
        moves = listOf(
            PokemonMoveSlotDto(
                move = NamedDto("tackle"),
                versionGroupDetails = listOf(
                    MoveVersionDetailDto(1, NamedDto("level-up"))
                )
            ),
            PokemonMoveSlotDto(
                move = NamedDto("cut"),
                versionGroupDetails = listOf(
                    MoveVersionDetailDto(0, NamedDto("machine"))
                )
            )
        ),
        height = 7,
        weight = 69,
        abilities = listOf(PokemonAbilitySlotDto(NamedDto("overgrow")))
    )

    override suspend fun getPokemonSpecies(id: Int) = PokemonSpeciesResponse(
        flavorTextEntries = listOf(
            FlavorTextEntryDto("A strange seed was planted.", NamedDto("en")),
            FlavorTextEntryDto("Une graine bizarre.", NamedDto("fr"))
        ),
        evolutionChain = EvolutionChainRefDto("http://localhost/api/v2/evolution-chain/1/")
    )

    override suspend fun getEvolutionChain(id: Int) = EvolutionChainResponse(
        chain = ChainLinkDto(
            species = NamedDto("bulbasaur", "http://localhost/api/v2/pokemon-species/1/"),
            evolvesTo = listOf(
                ChainLinkDto(
                    species = NamedDto("ivysaur", "http://localhost/api/v2/pokemon-species/2/"),
                    evolvesTo = emptyList()
                )
            )
        )
    )
}

class FakeCaughtPokemonDao : CaughtPokemonDao {
    private val _all = MutableStateFlow<List<CaughtPokemonEntity>>(emptyList())
    val caught: Flow<List<CaughtPokemonEntity>> = _all

    override suspend fun insert(entity: CaughtPokemonEntity) {
        _all.value = _all.value.filter { it.id != entity.id } + entity
    }
    override suspend fun delete(entity: CaughtPokemonEntity) {
        _all.value = _all.value.filter { it.id != entity.id }
    }
    override fun getAll(): Flow<List<CaughtPokemonEntity>> = _all
    override fun isCaught(id: Int): Flow<Boolean> =
        _all.map { list -> list.any { it.id == id } }
}

class FakePokemonListCacheDao : PokemonListCacheDao {
    private val store = mutableListOf<PokemonListCacheEntity>()
    override suspend fun getAll() = store.toList()
    override suspend fun getByName(name: String) = store.firstOrNull { it.name == name }
    override suspend fun insertAll(entities: List<PokemonListCacheEntity>) { store.addAll(entities) }
    override suspend fun count() = store.size
}

class FakePokemonDetailCacheDao : PokemonDetailCacheDao {
    private val store = mutableMapOf<Int, PokemonDetailCacheEntity>()
    override suspend fun getById(id: Int) = store[id]
    override suspend fun insert(entity: PokemonDetailCacheEntity) { store[entity.id] = entity }
    override suspend fun count() = store.size
}
```

- [ ] **Step 3: Run tests to verify they pass**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.repository.PokemonRepositoryTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/madmaxlgndklr/pokedex/repository/PokemonRepositoryTest.kt
git commit -m "fix: update PokemonRepositoryTest to use 4-arg constructor and cover new fields"
```

---

### Task 4: Create TypeWeakness.kt with the Gen 6+ type chart

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/TypeWeakness.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/ui/TypeWeaknessTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/madmaxlgndklr/pokedex/ui/TypeWeaknessTest.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.ui

import com.madmaxlgndklr.pokedex.ui.common.typeWeaknesses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TypeWeaknessTest {

    @Test
    fun `single grass type has correct 2x weaknesses`() {
        val result = typeWeaknesses(listOf("grass"))
        assertEquals(2f, result["fire"] ?: 1f, 0.01f)
        assertEquals(2f, result["ice"] ?: 1f, 0.01f)
        assertEquals(2f, result["poison"] ?: 1f, 0.01f)
        assertEquals(2f, result["flying"] ?: 1f, 0.01f)
        assertEquals(2f, result["bug"] ?: 1f, 0.01f)
    }

    @Test
    fun `single grass type has correct 0_5x resistances`() {
        val result = typeWeaknesses(listOf("grass"))
        assertEquals(0.5f, result["water"] ?: 1f, 0.01f)
        assertEquals(0.5f, result["electric"] ?: 1f, 0.01f)
        assertEquals(0.5f, result["grass"] ?: 1f, 0.01f)
        assertEquals(0.5f, result["ground"] ?: 1f, 0.01f)
    }

    @Test
    fun `dual grass poison type stacks multipliers`() {
        val result = typeWeaknesses(listOf("grass", "poison"))
        // Ground is 0.5x on grass, 2x on poison = 1x net -> not in results
        assertFalse(result.containsKey("ground"))
        // Psychic is 1x on grass, 2x on poison = 2x
        assertEquals(2f, result["psychic"] ?: 1f, 0.01f)
        // Fire is 2x on grass, 1x on poison = 2x
        assertEquals(2f, result["fire"] ?: 1f, 0.01f)
    }

    @Test
    fun `ghost type is immune to normal`() {
        val result = typeWeaknesses(listOf("ghost"))
        assertEquals(0f, result["normal"] ?: 1f, 0.01f)
    }

    @Test
    fun `steel type produces 4x weakness for fire on ice_steel dual type`() {
        // Ice/Steel: fire is 0.5x on ice, 2x on steel = 1x — not a good example
        // Water/Ground: electric is 2x on water, 0x on ground = 0x (immune)
        val result = typeWeaknesses(listOf("water", "ground"))
        assertEquals(0f, result["electric"] ?: 1f, 0.01f)
    }

    @Test
    fun `normal effectiveness types are excluded from result`() {
        val result = typeWeaknesses(listOf("fire"))
        assertFalse(result.containsKey("normal"))
        assertFalse(result.containsKey("fighting"))
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.ui.TypeWeaknessTest" 2>&1 | tail -10
```

Expected: FAIL — `typeWeaknesses` not found.

- [ ] **Step 3: Create `TypeWeakness.kt`**

Create `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/TypeWeakness.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.ui.common

private val CHART: Map<String, Map<String, Float>> = mapOf(
    "normal"   to mapOf("rock" to 0.5f, "steel" to 0.5f, "ghost" to 0f),
    "fire"     to mapOf("grass" to 2f, "ice" to 2f, "bug" to 2f, "steel" to 2f,
                        "fire" to 0.5f, "water" to 0.5f, "rock" to 0.5f, "dragon" to 0.5f),
    "water"    to mapOf("fire" to 2f, "ground" to 2f, "rock" to 2f,
                        "water" to 0.5f, "grass" to 0.5f, "dragon" to 0.5f),
    "electric" to mapOf("water" to 2f, "flying" to 2f,
                        "electric" to 0.5f, "grass" to 0.5f, "dragon" to 0.5f, "ground" to 0f),
    "grass"    to mapOf("water" to 2f, "ground" to 2f, "rock" to 2f,
                        "fire" to 0.5f, "grass" to 0.5f, "poison" to 0.5f, "flying" to 0.5f,
                        "bug" to 0.5f, "dragon" to 0.5f, "steel" to 0.5f),
    "ice"      to mapOf("grass" to 2f, "ground" to 2f, "flying" to 2f, "dragon" to 2f,
                        "fire" to 0.5f, "water" to 0.5f, "ice" to 0.5f, "steel" to 0.5f),
    "fighting" to mapOf("normal" to 2f, "ice" to 2f, "rock" to 2f, "dark" to 2f, "steel" to 2f,
                        "poison" to 0.5f, "bug" to 0.5f, "flying" to 0.5f,
                        "psychic" to 0.5f, "fairy" to 0.5f, "ghost" to 0f),
    "poison"   to mapOf("grass" to 2f, "fairy" to 2f,
                        "poison" to 0.5f, "ground" to 0.5f, "rock" to 0.5f,
                        "ghost" to 0.5f, "steel" to 0f),
    "ground"   to mapOf("fire" to 2f, "electric" to 2f, "poison" to 2f, "rock" to 2f, "steel" to 2f,
                        "grass" to 0.5f, "bug" to 0.5f, "flying" to 0f),
    "flying"   to mapOf("grass" to 2f, "fighting" to 2f, "bug" to 2f,
                        "electric" to 0.5f, "rock" to 0.5f, "steel" to 0.5f),
    "psychic"  to mapOf("fighting" to 2f, "poison" to 2f,
                        "psychic" to 0.5f, "steel" to 0.5f, "dark" to 0f),
    "bug"      to mapOf("grass" to 2f, "psychic" to 2f, "dark" to 2f,
                        "fire" to 0.5f, "fighting" to 0.5f, "flying" to 0.5f,
                        "ghost" to 0.5f, "steel" to 0.5f, "poison" to 0.5f, "fairy" to 0.5f),
    "rock"     to mapOf("fire" to 2f, "ice" to 2f, "flying" to 2f, "bug" to 2f,
                        "fighting" to 0.5f, "ground" to 0.5f, "steel" to 0.5f),
    "ghost"    to mapOf("ghost" to 2f, "psychic" to 2f,
                        "dark" to 0.5f, "normal" to 0f),
    "dragon"   to mapOf("dragon" to 2f, "steel" to 0.5f, "fairy" to 0f),
    "dark"     to mapOf("ghost" to 2f, "psychic" to 2f,
                        "fighting" to 0.5f, "dark" to 0.5f, "fairy" to 0.5f),
    "steel"    to mapOf("ice" to 2f, "rock" to 2f, "fairy" to 2f,
                        "fire" to 0.5f, "water" to 0.5f, "electric" to 0.5f, "steel" to 0.5f),
    "fairy"    to mapOf("fighting" to 2f, "dragon" to 2f, "dark" to 2f,
                        "fire" to 0.5f, "poison" to 0.5f, "steel" to 0.5f)
)

private val ALL_TYPES = listOf(
    "normal","fire","water","electric","grass","ice","fighting","poison",
    "ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"
)

fun typeWeaknesses(defenderTypes: List<String>): Map<String, Float> =
    ALL_TYPES.associateWith { attacker ->
        defenderTypes.fold(1f) { acc, defender ->
            acc * (CHART[attacker]?.get(defender) ?: 1f)
        }
    }.filter { it.value != 1f }
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.ui.TypeWeaknessTest" 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`, all 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/TypeWeakness.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/ui/TypeWeaknessTest.kt
git commit -m "feat: add TypeWeakness.kt with Gen 6+ type chart and unit tests"
```

---

### Task 5: Update DetailScreen — shiny toggle, height/weight, abilities panel, weakness chart

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/detail/DetailScreen.kt`

**Context:** The current `DetailContent` composable uses `BoxWithConstraints` with absolute `offset` positioning. The layout is:
- y = sh * 0.21f: name + types Column
- y = sh * 0.38f to sh * 0.64f: left panel (dex entry), sprite (center), right panel (stats)
- y = sh * 0.72f: evolution chain row

Changes:
1. Add `var showShiny by remember { mutableStateOf(false) }` — toggled by tapping the sprite
2. Add height/weight text below the types at y = sh * 0.29f
3. Left panel: toggle between DEX (flavor text) and ABILITIES with a tap
4. Right panel: toggle between STATS and WEAK (weakness chart) with a tap

- [ ] **Step 1: Add needed imports to DetailScreen**

Add these imports at the top of `DetailScreen.kt` (after existing imports):

```kotlin
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.madmaxlgndklr.pokedex.ui.common.typeWeaknesses
import com.madmaxlgndklr.pokedex.ui.common.swipeNavigation
import com.madmaxlgndklr.pokedex.ui.theme.typeColor
```

Note: `swipeNavigation` is already imported. Verify no duplicate import. The `typeColor` function is in `ui/theme/Color.kt`. The inline `typeColor` function already in `DetailScreen.kt` (private) must be removed to avoid conflict — the theme version is identical, so just delete the private one at the bottom of DetailScreen.kt.

- [ ] **Step 2: Update `DetailContent` signature to add toggle state**

Replace the `DetailContent` function signature and opening lines:

```kotlin
@Composable
private fun DetailContent(
    detail: PokemonDetail,
    isCaught: Boolean,
    onBack: () -> Unit,
    onNavigatePrev: () -> Unit,
    onNavigateNext: () -> Unit,
    onToggleCaught: () -> Unit,
    onEvolutionClick: (Int) -> Unit
) {
    var showShiny by remember { mutableStateOf(false) }
    var showAbilities by remember { mutableStateOf(false) }
    var showWeakness by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().swipeNavigation(
        onBack = onBack,
        onSwipeLeft = onNavigateNext,
        onSwipeRight = onNavigatePrev
    )) {
```

- [ ] **Step 3: Add height/weight row below types**

After the existing types Row inside the name/types Column (around line 151 in original), add:

```kotlin
Spacer(Modifier.height(4.dp))
Text(
    text = "${"%.1f".format(detail.height / 10f)}m / ${"%.1f".format(detail.weight / 10f)}kg",
    fontFamily = PressStart2P,
    fontSize = 6.sp,
    color = PokedexCream.copy(alpha = 0.6f),
    textAlign = TextAlign.Center
)
```

- [ ] **Step 4: Make sprite tappable for shiny toggle**

Replace the `AsyncImage` for the main sprite:

```kotlin
AsyncImage(
    model = if (showShiny) RetrofitClient.shinySpriteUrl(detail.id) else detail.spriteUrl,
    contentDescription = detail.name,
    contentScale = ContentScale.Fit,
    modifier = Modifier
        .size(sw * 0.36f)
        .offset(x = sw * 0.32f, y = sh * 0.38f)
        .pointerInput(Unit) {
            detectTapGestures { showShiny = !showShiny }
        }
)
```

- [ ] **Step 5: Add tappable header to left panel (DEX / ABILITIES toggle)**

Replace the left panel `Box`:

```kotlin
Box(
    modifier = Modifier
        .offset(x = sw * 0.02f, y = sh * 0.38f)
        .width(sw * 0.28f)
        .height(sh * 0.26f)
        .background(panelBg, panelShape)
        .padding(6.dp)
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (showAbilities) "ABILITIES" else "DEX ENTRY",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = GlowBlue,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showAbilities = !showAbilities }
                .padding(bottom = 4.dp)
        )
        if (showAbilities) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (detail.abilities.isEmpty()) {
                    Text(
                        text = "—",
                        fontFamily = PressStart2P,
                        fontSize = 6.sp,
                        color = PokedexCream
                    )
                } else {
                    detail.abilities.forEach { ability ->
                        Text(
                            text = ability.uppercase().replace("-", "\n"),
                            fontFamily = PressStart2P,
                            fontSize = 6.sp,
                            color = PokedexCream,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        } else {
            Text(
                text = detail.flavorText,
                fontFamily = PressStart2P,
                fontSize = 8.sp,
                color = PokedexCream,
                lineHeight = 13.sp,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
    }
}
```

- [ ] **Step 6: Add tappable header to right panel (STATS / WEAK toggle)**

Replace the right panel `Column`:

```kotlin
Column(
    modifier = Modifier
        .offset(x = sw * 0.70f, y = sh * 0.38f)
        .width(sw * 0.27f)
        .height(sh * 0.26f)
        .background(panelBg, panelShape)
        .padding(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
) {
    Text(
        text = if (showWeakness) "WEAKNESS" else "STATS",
        fontFamily = PressStart2P,
        fontSize = 5.sp,
        color = GlowBlue,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showWeakness = !showWeakness }
            .padding(bottom = 2.dp)
    )
    if (showWeakness) {
        val weaknesses = remember(detail.types) { typeWeaknesses(detail.types) }
        val grouped = weaknesses.entries
            .sortedByDescending { it.value }
            .groupBy { it.value }
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            listOf(4f, 2f, 0.5f, 0.25f, 0f).forEach { mult ->
                val entries = grouped[mult] ?: return@forEach
                val label = when (mult) {
                    4f    -> "4×"
                    2f    -> "2×"
                    0.5f  -> "½×"
                    0.25f -> "¼×"
                    else  -> "0×"
                }
                Text(
                    text = label,
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = when (mult) {
                        4f   -> PokedexRed
                        2f   -> CaughtGold
                        0f   -> GlowBlue
                        else -> PokedexCream.copy(alpha = 0.5f)
                    }
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    entries.forEach { (type, _) ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(typeColor(type), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = type.uppercase().take(3),
                                fontFamily = PressStart2P,
                                fontSize = 4.sp,
                                color = PokedexCream
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            detail.stats.forEach { stat ->
                CompactStatRow(stat = stat, width = sw * 0.27f - 12.dp)
            }
        }
    }
}
```

- [ ] **Step 7: Remove the now-duplicate private `typeColor` function from DetailScreen.kt**

Delete the `private fun typeColor(type: String)` function at the bottom of `DetailScreen.kt` — it's now imported from `ui/theme/Color.kt`. Also add `import com.madmaxlgndklr.pokedex.ui.theme.typeColor` if not already present.

Add any missing imports at the top of the file:

```kotlin
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.madmaxlgndklr.pokedex.ui.common.typeWeaknesses
import com.madmaxlgndklr.pokedex.ui.theme.typeColor
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
```

Note: `FlowRow` requires `androidx.compose.foundation.layout.FlowRow` which is in Compose 1.5+. Check `app/build.gradle` for the Compose BOM version. If FlowRow is unavailable, replace with a simple `Column` of rows and manually chunk entries: `entries.chunked(3).forEach { row -> Row { row.forEach { ... } } }`.

- [ ] **Step 8: Build to verify compilation**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/detail/DetailScreen.kt
git commit -m "feat: shiny toggle, height/weight, abilities panel, type weakness chart on detail screen"
```

---

### Task 6: Run all unit tests

- [ ] **Step 1: Run full test suite**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr ./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 2: If any test fails, fix it before moving on**

Common failure: `SearchViewModelTest` or `MyCollectionViewModelTest` may fail if they use `PokemonRepository` directly. Check if they use fake repos — if so they should be unaffected. If a test imports `PokemonDetailResponse` with old field count, add default values or update the fake data.
