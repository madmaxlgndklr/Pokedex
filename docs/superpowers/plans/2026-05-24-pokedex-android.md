# Pokédex Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a retro-styled Android Pokédex app in Kotlin with Jetpack Compose that browses all Pokémon from a local PokeAPI instance (`http://10.0.2.2:89/api/v2/`), supports name/type search, shows full detail screens (stats, moves, evolution, flavor text), and tracks caught Pokémon in a local Room database.

**Architecture:** MVVM — each ViewModel exposes `UiState<T>` via `StateFlow`, screens observe state reactively, a single `PokemonRepository` mediates between Retrofit (remote) and Room (local). No DI framework — a custom `Application` class holds the singleton repository; ViewModels receive it via `ViewModelProvider.Factory`.

**Tech Stack:** Kotlin · Jetpack Compose · Navigation Compose · Retrofit + Gson · Room + KSP · Coil · Press Start 2P TTF · Coroutines + Flow · JUnit4 + kotlinx-coroutines-test

---

## File Map

All new files live under `app/src/main/java/com/madmaxlgndklr/pokedex/`.

| File | Responsibility |
|---|---|
| `PokedexApplication.kt` | Singleton `AppDatabase` + `PokemonRepository` |
| `model/PokemonSummary.kt` | `PokemonSummary(id, name)` domain model |
| `model/PokemonDetail.kt` | `PokemonDetail`, `PokemonStat`, `PokemonMove`, `EvolutionStage`, `EvolutionNode` |
| `ui/common/UiState.kt` | `sealed class UiState<T>` |
| `data/remote/dto/` | Gson-mapped DTOs for list, detail, species, evolution-chain responses |
| `data/remote/PokeApiService.kt` | Retrofit interface — 4 endpoints |
| `data/remote/RetrofitClient.kt` | Retrofit singleton with OkHttp |
| `data/local/CaughtPokemonEntity.kt` | Room `@Entity` |
| `data/local/CaughtPokemonDao.kt` | Room `@Dao` — insert, delete, getAll, isCaught |
| `data/local/AppDatabase.kt` | Room `@Database` singleton |
| `data/repository/PokemonRepository.kt` | Wraps API + DAO; all business logic lives here |
| `ui/theme/Color.kt` | Retro palette + all 18 type badge colors (replaces scaffold file) |
| `ui/theme/Type.kt` | Press Start 2P font + `Typography` (replaces scaffold file) |
| `ui/theme/Theme.kt` | `PokedexTheme` composable (replaces scaffold file) |
| `ui/common/TypeBadge.kt` | Type chip composable |
| `ui/common/StatBar.kt` | Labeled pixel-style stat bar composable |
| `ui/common/PokemonCard.kt` | Grid card: sprite + number + name + badges |
| `ui/common/EvolutionChain.kt` | Stage-based horizontal evolution row |
| `ui/list/PokemonListViewModel.kt` | Loads full list; exposes `UiState<List<PokemonSummary>>` |
| `ui/list/ListScreen.kt` | 2-column grid screen |
| `ui/search/SearchViewModel.kt` | Filters in-memory list by name + types |
| `ui/search/SearchScreen.kt` | Search input + type chips + filtered grid |
| `ui/detail/PokemonDetailViewModel.kt` | Loads detail + species + evolution in parallel |
| `ui/detail/DetailScreen.kt` | Full detail layout |
| `ui/mycollection/MyCollectionViewModel.kt` | Observes Room `Flow`; exposes caught list |
| `ui/mycollection/MyCollectionScreen.kt` | Caught-only grid with empty state |
| `ui/navigation/AppNavigation.kt` | `ModalNavigationDrawer` + `NavHost` |
| `MainActivity.kt` | Replaced: sets content to `PokedexTheme { AppNavigation() }` |
| `app/src/main/res/font/press_start_2p.ttf` | Pixel font file (downloaded in Task 6) |
| `app/src/main/res/values/strings.xml` | App name string |

Test files mirror source layout under `app/src/test/java/com/madmaxlgndklr/pokedex/`.

---

## Task 1: Dependencies, Manifest & Application class

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root)
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/PokedexApplication.kt`

- [ ] **Step 1: Add versions to `gradle/libs.versions.toml`**

Replace the `[versions]` block:

```toml
[versions]
agp = "9.2.1"
coreKtx = "1.10.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
lifecycleRuntimeKtx = "2.6.1"
activityCompose = "1.8.0"
kotlin = "2.2.10"
composeBom = "2026.02.01"
ksp = "2.2.10-2.0.2"
room = "2.7.0"
navigationCompose = "2.8.0"
coroutines = "1.9.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
coil = "2.7.0"
lifecycleViewmodelCompose = "2.8.0"
archCore = "2.2.0"
```

Add to `[libraries]`:

```toml
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
arch-core-testing = { group = "androidx.arch.core", name = "core-testing", version.ref = "archCore" }
```

Add to `[plugins]`:

```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Add KSP plugin to root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 3: Replace `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.madmaxlgndklr.pokedex"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.madmaxlgndklr.pokedex"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.arch.core.testing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.room.testing)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

- [ ] **Step 4: Update `AndroidManifest.xml`**

Add `usesCleartextTraffic` and `android:name` to `<application>`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:name=".PokedexApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Pokédex"
        android:usesCleartextTraffic="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.Pokédex">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 5: Create `PokedexApplication.kt`**

```kotlin
package com.madmaxlgndklr.pokedex

import android.app.Application
import com.madmaxlgndklr.pokedex.data.local.AppDatabase
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository

class PokedexApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy {
        PokemonRepository(RetrofitClient.api, database.caughtPokemonDao())
    }
}
```

- [ ] **Step 6: Sync gradle and verify build**

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL` with no errors. If KSP version doesn't resolve, check https://github.com/google/ksp/releases for the version matching your Kotlin version and update `libs.versions.toml`.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts \
    app/src/main/AndroidManifest.xml \
    app/src/main/java/com/madmaxlgndklr/pokedex/PokedexApplication.kt
git commit -m "feat: add all dependencies, manifest cleartext, Application class"
```

---

## Task 2: Domain Models & UiState

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/model/PokemonSummary.kt`
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/model/PokemonDetail.kt`
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/UiState.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/model/UiStateTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/madmaxlgndklr/pokedex/model/UiStateTest.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.model

import com.madmaxlgndklr.pokedex.ui.common.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateTest {
    @Test
    fun `Loading state is singleton`() {
        val state: UiState<String> = UiState.Loading
        assertTrue(state is UiState.Loading)
    }

    @Test
    fun `Success state holds data`() {
        val state = UiState.Success("hello")
        assertEquals("hello", state.data)
    }

    @Test
    fun `Error state holds message`() {
        val state = UiState.Error("network failure")
        assertEquals("network failure", state.message)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:test --tests "*.UiStateTest"
```

Expected: FAIL — `UiState` not defined.

- [ ] **Step 3: Create `UiState.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.common

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

- [ ] **Step 4: Create `PokemonSummary.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.model

data class PokemonSummary(
    val id: Int,
    val name: String
)
```

- [ ] **Step 5: Create `PokemonDetail.kt`**

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
    val flavorText: String
)
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
./gradlew :app:test --tests "*.UiStateTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/model/ \
    app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/UiState.kt \
    app/src/test/java/com/madmaxlgndklr/pokedex/model/
git commit -m "feat: add domain models and UiState"
```

---

## Task 3: Remote DTOs & PokeApiService

**Files:**
- Create: `data/remote/dto/PokemonListResponse.kt`
- Create: `data/remote/dto/PokemonDetailResponse.kt`
- Create: `data/remote/dto/PokemonSpeciesResponse.kt`
- Create: `data/remote/dto/EvolutionChainResponse.kt`
- Create: `data/remote/PokeApiService.kt`
- Create: `data/remote/RetrofitClient.kt`

No unit tests for DTOs — they are pure data containers; the repository tests in Task 5 exercise them end-to-end.

- [ ] **Step 1: Create `dto/PokemonListResponse.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.data.remote.dto

data class PokemonListResponse(val results: List<PokemonResultDto>)

data class PokemonResultDto(val name: String, val url: String) {
    fun extractId(): Int = url.trimEnd('/').substringAfterLast('/').toInt()
}
```

- [ ] **Step 2: Create `dto/PokemonDetailResponse.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PokemonDetailResponse(
    val id: Int,
    val name: String,
    val sprites: SpritesDto,
    val types: List<PokemonTypeSlotDto>,
    val stats: List<PokemonStatDto>,
    val moves: List<PokemonMoveSlotDto>
)

data class SpritesDto(@SerializedName("front_default") val frontDefault: String?)

data class PokemonTypeSlotDto(val type: NamedDto)

data class PokemonStatDto(
    @SerializedName("base_stat") val baseStat: Int,
    val stat: NamedDto
)

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

- [ ] **Step 3: Create `dto/PokemonSpeciesResponse.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PokemonSpeciesResponse(
    @SerializedName("flavor_text_entries") val flavorTextEntries: List<FlavorTextEntryDto>,
    @SerializedName("evolution_chain") val evolutionChain: EvolutionChainRefDto
)

data class FlavorTextEntryDto(
    @SerializedName("flavor_text") val flavorText: String,
    val language: NamedDto
)

data class EvolutionChainRefDto(val url: String) {
    fun extractId(): Int = url.trimEnd('/').substringAfterLast('/').toInt()
}
```

- [ ] **Step 4: Create `dto/EvolutionChainResponse.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EvolutionChainResponse(val chain: ChainLinkDto)

data class ChainLinkDto(
    val species: NamedDto,
    @SerializedName("evolves_to") val evolvesTo: List<ChainLinkDto>
)
```

- [ ] **Step 5: Create `PokeApiService.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.data.remote

import com.madmaxlgndklr.pokedex.data.remote.dto.EvolutionChainResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonDetailResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonListResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.PokemonSpeciesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokeApiService {
    @GET("pokemon/")
    suspend fun getPokemonList(
        @Query("limit") limit: Int = 1500,
        @Query("offset") offset: Int = 0
    ): PokemonListResponse

    @GET("pokemon/{id}/")
    suspend fun getPokemonDetail(@Path("id") id: Int): PokemonDetailResponse

    @GET("pokemon-species/{id}/")
    suspend fun getPokemonSpecies(@Path("id") id: Int): PokemonSpeciesResponse

    @GET("evolution-chain/{id}/")
    suspend fun getEvolutionChain(@Path("id") id: Int): EvolutionChainResponse
}
```

- [ ] **Step 6: Create `RetrofitClient.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:89/api/v2/"

    val api: PokeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PokeApiService::class.java)
    }
}
```

- [ ] **Step 7: Verify build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/data/remote/
git commit -m "feat: add remote DTOs, PokeApiService, RetrofitClient"
```

---

## Task 4: Room Local Data Layer

**Files:**
- Create: `data/local/CaughtPokemonEntity.kt`
- Create: `data/local/CaughtPokemonDao.kt`
- Create: `data/local/AppDatabase.kt`
- Create: `app/src/androidTest/java/com/madmaxlgndklr/pokedex/local/CaughtPokemonDaoTest.kt`

- [ ] **Step 1: Write the failing DAO test**

`app/src/androidTest/java/com/madmaxlgndklr/pokedex/local/CaughtPokemonDaoTest.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.madmaxlgndklr.pokedex.data.local.AppDatabase
import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaughtPokemonDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun insertAndGetAll() = runTest {
        val entity = CaughtPokemonEntity(id = 1, name = "bulbasaur")
        db.caughtPokemonDao().insert(entity)
        val all = db.caughtPokemonDao().getAll().first()
        assertEquals(1, all.size)
        assertEquals("bulbasaur", all[0].name)
    }

    @Test
    fun deleteRemovesEntry() = runTest {
        val entity = CaughtPokemonEntity(id = 1, name = "bulbasaur")
        db.caughtPokemonDao().insert(entity)
        db.caughtPokemonDao().delete(entity)
        val all = db.caughtPokemonDao().getAll().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun isCaughtReturnsTrueWhenPresent() = runTest {
        db.caughtPokemonDao().insert(CaughtPokemonEntity(id = 25, name = "pikachu"))
        assertTrue(db.caughtPokemonDao().isCaught(25).first())
    }

    @Test
    fun isCaughtReturnsFalseWhenAbsent() = runTest {
        assertFalse(db.caughtPokemonDao().isCaught(25).first())
    }
}
```

- [ ] **Step 2: Create `CaughtPokemonEntity.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caught_pokemon")
data class CaughtPokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val caughtAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: Create `CaughtPokemonDao.kt`**

```kotlin
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
```

- [ ] **Step 4: Create `AppDatabase.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CaughtPokemonEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caughtPokemonDao(): CaughtPokemonDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pokedex.db"
                ).build().also { INSTANCE = it }
            }
    }
}
```

- [ ] **Step 5: Run the DAO tests on the emulator**

```bash
./gradlew :app:connectedAndroidTest --tests "*.CaughtPokemonDaoTest"
```

Expected: 4 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/data/local/ \
    app/src/androidTest/java/com/madmaxlgndklr/pokedex/local/
git commit -m "feat: add Room entity, DAO, and database"
```

---

## Task 5: PokemonRepository

**Files:**
- Create: `data/repository/PokemonRepository.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/repository/PokemonRepositoryTest.kt`

- [ ] **Step 1: Write the failing repository tests**

`app/src/test/java/com/madmaxlgndklr/pokedex/repository/PokemonRepositoryTest.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.repository

import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonDao
import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonEntity
import com.madmaxlgndklr.pokedex.data.remote.PokeApiService
import com.madmaxlgndklr.pokedex.data.remote.dto.*
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonRepositoryTest {
    private val fakeApi = FakePokeApiService()
    private val fakeDao = FakeCaughtPokemonDao()
    private val repo = PokemonRepository(fakeApi, fakeDao)

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
    fun `getPokemonDetail returns level-up moves only sorted by level`() = runTest {
        val detail = repo.getPokemonDetail(1)
        assertTrue(detail.moves.all { it.levelLearnedAt > 0 || it.levelLearnedAt == 1 })
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
        assertEquals(1, fakeDao.all.first().size)
    }

    @Test
    fun `setCaught false removes entity`() = runTest {
        repo.setCaught(id = 1, name = "bulbasaur", caught = true)
        repo.setCaught(id = 1, name = "bulbasaur", caught = false)
        assertTrue(fakeDao.all.first().isEmpty())
    }
}

// --- Fakes ---

class FakePokeApiService : PokeApiService {
    override suspend fun getPokemonList(limit: Int, offset: Int) = PokemonListResponse(
        results = listOf(
            PokemonResultDto("bulbasaur", "http://10.0.2.2:89/api/v2/pokemon/1/"),
            PokemonResultDto("ivysaur",   "http://10.0.2.2:89/api/v2/pokemon/2/")
        )
    )

    override suspend fun getPokemonDetail(id: Int) = PokemonDetailResponse(
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
        )
    )

    override suspend fun getPokemonSpecies(id: Int) = PokemonSpeciesResponse(
        flavorTextEntries = listOf(
            FlavorTextEntryDto("A strange seed was planted.", NamedDto("en")),
            FlavorTextEntryDto("Une graine bizarre a été plantée.", NamedDto("fr"))
        ),
        evolutionChain = EvolutionChainRefDto("http://10.0.2.2:89/api/v2/evolution-chain/1/")
    )

    override suspend fun getEvolutionChain(id: Int) = EvolutionChainResponse(
        chain = ChainLinkDto(
            species = NamedDto("bulbasaur", "http://10.0.2.2:89/api/v2/pokemon-species/1/"),
            evolvesTo = listOf(
                ChainLinkDto(
                    species = NamedDto("ivysaur", "http://10.0.2.2:89/api/v2/pokemon-species/2/"),
                    evolvesTo = emptyList()
                )
            )
        )
    )
}

class FakeCaughtPokemonDao : CaughtPokemonDao {
    private val _all = MutableStateFlow<List<CaughtPokemonEntity>>(emptyList())
    val all: Flow<List<CaughtPokemonEntity>> = _all

    override suspend fun insert(entity: CaughtPokemonEntity) {
        _all.value = _all.value.filter { it.id != entity.id } + entity
    }

    override suspend fun delete(entity: CaughtPokemonEntity) {
        _all.value = _all.value.filter { it.id != entity.id }
    }

    override fun getAll(): Flow<List<CaughtPokemonEntity>> = _all

    override fun isCaught(id: Int): Flow<Boolean> =
        MutableStateFlow(_all.value.any { it.id == id })
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:test --tests "*.PokemonRepositoryTest"
```

Expected: FAIL — `PokemonRepository` not defined.

- [ ] **Step 3: Create `PokemonRepository.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.data.repository

import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonDao
import com.madmaxlgndklr.pokedex.data.local.CaughtPokemonEntity
import com.madmaxlgndklr.pokedex.data.remote.PokeApiService
import com.madmaxlgndklr.pokedex.data.remote.dto.ChainLinkDto
import com.madmaxlgndklr.pokedex.model.EvolutionNode
import com.madmaxlgndklr.pokedex.model.EvolutionStage
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.model.PokemonMove
import com.madmaxlgndklr.pokedex.model.PokemonStat
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PokemonRepository(
    private val api: PokeApiService,
    private val dao: CaughtPokemonDao
) {
    suspend fun getPokemonList(): List<PokemonSummary> =
        api.getPokemonList().results.map { dto ->
            PokemonSummary(id = dto.extractId(), name = dto.name)
        }

    suspend fun getPokemonDetail(id: Int): PokemonDetail = coroutineScope {
        val detailDeferred = async { api.getPokemonDetail(id) }
        val speciesDeferred = async { api.getPokemonSpecies(id) }

        val detail = detailDeferred.await()
        val species = speciesDeferred.await()
        val evoChainId = species.evolutionChain.extractId()
        val evoChain = api.getEvolutionChain(evoChainId)

        val levelUpMoves = detail.moves
            .flatMap { slot ->
                slot.versionGroupDetails
                    .filter { it.moveLearnMethod.name == "level-up" }
                    .map { PokemonMove(slot.move.name, it.levelLearnedAt) }
            }
            .distinctBy { it.name }
            .sortedBy { it.levelLearnedAt }

        val flavorText = species.flavorTextEntries
            .firstOrNull { it.language.name == "en" }
            ?.flavorText
            ?.replace("\n", " ")
            ?.replace("", " ")
            ?: ""

        PokemonDetail(
            id = detail.id,
            name = detail.name,
            spriteUrl = detail.sprites.frontDefault ?: "",
            types = detail.types.map { it.type.name },
            stats = detail.stats.map { PokemonStat(it.stat.name, it.baseStat) },
            moves = levelUpMoves,
            evolutionChain = parseEvolutionChain(evoChain.chain),
            flavorText = flavorText
        )
    }

    fun getCaughtPokemon(): Flow<List<PokemonSummary>> =
        dao.getAll().map { entities ->
            entities.map { PokemonSummary(it.id, it.name) }
        }

    fun isCaught(id: Int): Flow<Boolean> = dao.isCaught(id)

    suspend fun setCaught(id: Int, name: String, caught: Boolean) {
        if (caught) dao.insert(CaughtPokemonEntity(id, name))
        else dao.delete(CaughtPokemonEntity(id, name))
    }

    private fun parseEvolutionChain(link: ChainLinkDto): List<EvolutionStage> {
        val stages = mutableListOf<EvolutionStage>()
        var current = listOf(link)
        while (current.isNotEmpty()) {
            stages.add(EvolutionStage(current.map { node ->
                EvolutionNode(
                    id = node.species.url.trimEnd('/').substringAfterLast('/').toInt(),
                    name = node.species.name
                )
            }))
            current = current.flatMap { it.evolvesTo }
        }
        return stages
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:test --tests "*.PokemonRepositoryTest"
```

Expected: 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/data/repository/ \
    app/src/test/java/com/madmaxlgndklr/pokedex/repository/
git commit -m "feat: add PokemonRepository with unit tests"
```

---

## Task 6: PokedexTheme (Colors, Font, Theme)

**Files:**
- Create: `app/src/main/res/font/press_start_2p.ttf` (downloaded)
- Modify: `ui/theme/Color.kt`
- Modify: `ui/theme/Type.kt`
- Modify: `ui/theme/Theme.kt`

- [ ] **Step 1: Download Press Start 2P font**

```bash
curl -L "https://fonts.google.com/download?family=Press+Start+2P" -o /tmp/PressStart2P.zip
unzip /tmp/PressStart2P.zip -d /tmp/PressStart2P/
mkdir -p app/src/main/res/font/
cp /tmp/PressStart2P/PressStart2P-Regular.ttf app/src/main/res/font/press_start_2p.ttf
```

Verify: `ls app/src/main/res/font/press_start_2p.ttf` should exist.

- [ ] **Step 2: Replace `ui/theme/Color.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.theme

import androidx.compose.ui.graphics.Color

val PokedexRed    = Color(0xFFCC0000)
val PokedexDarkRed = Color(0xFF8B0000)
val PokedexGreen  = Color(0xFF88CC00)
val PokedexDark   = Color(0xFF1A1A1A)
val PokedexCream  = Color(0xFFF5F5DC)
val CaughtGold    = Color(0xFFFFD700)

val TypeNormal    = Color(0xFFA8A878)
val TypeFire      = Color(0xFFF08030)
val TypeWater     = Color(0xFF6890F0)
val TypeElectric  = Color(0xFFF8D030)
val TypeGrass     = Color(0xFF78C850)
val TypeIce       = Color(0xFF98D8D8)
val TypeFighting  = Color(0xFFC03028)
val TypePoison    = Color(0xFFA040A0)
val TypeGround    = Color(0xFFE0C068)
val TypeFlying    = Color(0xFFA890F0)
val TypePsychic   = Color(0xFFF85888)
val TypeBug       = Color(0xFFA8B820)
val TypeRock      = Color(0xFFB8A038)
val TypeGhost     = Color(0xFF705898)
val TypeDragon    = Color(0xFF7038F8)
val TypeDark      = Color(0xFF705848)
val TypeSteel     = Color(0xFFB8B8D0)
val TypeFairy     = Color(0xFFEE99AC)

fun typeColor(type: String): Color = when (type.lowercase()) {
    "normal"   -> TypeNormal
    "fire"     -> TypeFire
    "water"    -> TypeWater
    "electric" -> TypeElectric
    "grass"    -> TypeGrass
    "ice"      -> TypeIce
    "fighting" -> TypeFighting
    "poison"   -> TypePoison
    "ground"   -> TypeGround
    "flying"   -> TypeFlying
    "psychic"  -> TypePsychic
    "bug"      -> TypeBug
    "rock"     -> TypeRock
    "ghost"    -> TypeGhost
    "dragon"   -> TypeDragon
    "dark"     -> TypeDark
    "steel"    -> TypeSteel
    "fairy"    -> TypeFairy
    else       -> TypeNormal
}
```

- [ ] **Step 3: Replace `ui/theme/Type.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R

val PressStart2P = FontFamily(Font(R.font.press_start_2p))

val Typography = Typography(
    displayLarge  = TextStyle(fontFamily = PressStart2P, fontSize = 18.sp),
    titleLarge    = TextStyle(fontFamily = PressStart2P, fontSize = 14.sp),
    titleMedium   = TextStyle(fontFamily = PressStart2P, fontSize = 11.sp),
    bodyMedium    = TextStyle(fontFamily = PressStart2P, fontSize = 8.sp),
    labelSmall    = TextStyle(fontFamily = PressStart2P, fontSize = 7.sp),
)
```

- [ ] **Step 4: Replace `ui/theme/Theme.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PokedexColorScheme = darkColorScheme(
    primary          = PokedexRed,
    onPrimary        = PokedexCream,
    primaryContainer = PokedexDarkRed,
    background       = PokedexDark,
    onBackground     = PokedexCream,
    surface          = PokedexDark,
    onSurface        = PokedexCream,
    secondary        = PokedexGreen,
    onSecondary      = PokedexDark
)

@Composable
fun PokedexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PokedexColorScheme,
        typography  = Typography,
        content     = content
    )
}
```

- [ ] **Step 5: Build to verify**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/font/ app/src/main/java/com/madmaxlgndklr/pokedex/ui/theme/
git commit -m "feat: add retro PokedexTheme with Press Start 2P font and type colors"
```

---

## Task 7: TypeBadge & StatBar Composables

**Files:**
- Create: `ui/common/TypeBadge.kt`
- Create: `ui/common/StatBar.kt`

No automated tests — these are purely visual; verify with `@Preview`.

- [ ] **Step 1: Create `TypeBadge.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

@Composable
fun TypeBadge(type: String, modifier: Modifier = Modifier) {
    Text(
        text = type.uppercase(),
        fontFamily = PressStart2P,
        fontSize = 7.sp,
        color = PokedexCream,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(typeColor(type))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Preview
@Composable
private fun TypeBadgePreview() {
    TypeBadge("fire")
}
```

- [ ] **Step 2: Create `StatBar.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun StatBar(label: String, value: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label.uppercase().take(7).padEnd(7),
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream,
            modifier = Modifier.width(60.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value.toString().padStart(3),
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream,
            modifier = Modifier.width(28.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .background(PokedexCream.copy(alpha = 0.2f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value / 255f)
                    .height(10.dp)
                    .background(PokedexRed)
            )
        }
    }
}

@Preview
@Composable
private fun StatBarPreview() {
    StatBar("HP", 78)
}
```

- [ ] **Step 3: Build to verify**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/TypeBadge.kt \
    app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/StatBar.kt
git commit -m "feat: add TypeBadge and StatBar composables"
```

---

## Task 8: PokemonCard & EvolutionChain Composables

**Files:**
- Create: `ui/common/PokemonCard.kt`
- Create: `ui/common/EvolutionChain.kt`

- [ ] **Step 1: Create `PokemonCard.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDarkRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun PokemonCard(
    id: Int,
    name: String,
    types: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PokedexDarkRed)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = spriteUrl,
            contentDescription = "$name sprite",
            modifier = Modifier.size(80.dp)
        )
        Text(
            text = "#${id.toString().padStart(3, '0')}",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = name.replaceFirstChar { it.uppercase() },
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            types.forEach { type -> TypeBadge(type) }
        }
    }
}
```

- [ ] **Step 2: Create `EvolutionChain.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.model.EvolutionStage
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun EvolutionChain(
    stages: List<EvolutionStage>,
    onPokemonClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        stages.forEachIndexed { index, stage ->
            if (index > 0) {
                Text("→", fontFamily = PressStart2P, fontSize = 12.sp, color = PokedexCream)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stage.members.forEach { node ->
                    val spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${node.id}.png"
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onPokemonClick(node.id) }
                    ) {
                        AsyncImage(
                            model = spriteUrl,
                            contentDescription = "${node.name} sprite",
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = node.name.replaceFirstChar { it.uppercase() },
                            fontFamily = PressStart2P,
                            fontSize = 6.sp,
                            color = PokedexCream,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Build to verify**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/PokemonCard.kt \
    app/src/main/java/com/madmaxlgndklr/pokedex/ui/common/EvolutionChain.kt
git commit -m "feat: add PokemonCard and EvolutionChain composables"
```

---

## Task 9: List Screen

**Files:**
- Create: `ui/list/PokemonListViewModel.kt`
- Create: `ui/list/ListScreen.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/list/PokemonListViewModelTest.kt`

- [ ] **Step 1: Write the failing ViewModel test**

```kotlin
package com.madmaxlgndklr.pokedex.list

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.list.PokemonListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonListViewModelTest {
    @get:Rule val instantTaskRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PokemonListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repo = PokemonRepository(FakePokeApiService(), FakeCaughtPokemonDao())
        viewModel = PokemonListViewModel(repo)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Loading`() {
        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    @Test
    fun `after load state is Success with list`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success<List<PokemonSummary>>).data
        assertEquals(2, list.size)
        assertEquals("bulbasaur", list[0].name)
    }
}
```

- [ ] **Step 2: Run to verify fail**

```bash
./gradlew :app:test --tests "*.PokemonListViewModelTest"
```

Expected: FAIL.

- [ ] **Step 3: Create `PokemonListViewModel.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PokemonListViewModel(private val repository: PokemonRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<PokemonSummary>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<PokemonSummary>>> = _uiState

    init {
        loadList()
    }

    fun loadList() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = try {
                UiState.Success(repository.getPokemonList())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { PokemonListViewModel(repository) }
        }
    }
}
```

- [ ] **Step 4: Create `ListScreen.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.PokemonCard
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexGreen
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun ListScreen(
    viewModel: PokemonListViewModel,
    onPokemonClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexGreen)
    ) {
        when (val state = uiState) {
            is UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                text = "NO SIGNAL\n\n${state.message}",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
            is UiState.Success -> PokemonGrid(
                pokemon = state.data,
                onPokemonClick = onPokemonClick
            )
        }
    }
}

@Composable
private fun PokemonGrid(
    pokemon: List<PokemonSummary>,
    onPokemonClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pokemon, key = { it.id }) { summary ->
            PokemonCard(
                id = summary.id,
                name = summary.name,
                types = emptyList(),
                onClick = { onPokemonClick(summary.id) }
            )
        }
    }
}
```

> **Note:** Types are not available from the list endpoint — only from the detail endpoint. The list screen shows cards without type badges; badges appear on the detail screen. This is intentional and avoids 1500 extra API calls on load.

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :app:test --tests "*.PokemonListViewModelTest"
```

Expected: 2 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/list/ \
    app/src/test/java/com/madmaxlgndklr/pokedex/list/
git commit -m "feat: add List screen and PokemonListViewModel"
```

---

## Task 10: Search Screen

**Files:**
- Create: `ui/search/SearchViewModel.kt`
- Create: `ui/search/SearchScreen.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/search/SearchViewModelTest.kt`

- [ ] **Step 1: Write the failing ViewModel test**

```kotlin
package com.madmaxlgndklr.pokedex.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.ui.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule val instantTaskRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repo = PokemonRepository(FakePokeApiService(), FakeCaughtPokemonDao())
        viewModel = SearchViewModel(repo)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `empty query returns full list after load`() = runTest {
        advanceUntilIdle()
        assertEquals(2, viewModel.filteredList.value.size)
    }

    @Test
    fun `query filters by name`() = runTest {
        advanceUntilIdle()
        viewModel.onQueryChange("ivy")
        assertEquals(1, viewModel.filteredList.value.size)
        assertEquals("ivysaur", viewModel.filteredList.value[0].name)
    }

    @Test
    fun `clearing query restores full list`() = runTest {
        advanceUntilIdle()
        viewModel.onQueryChange("ivy")
        viewModel.onQueryChange("")
        assertEquals(2, viewModel.filteredList.value.size)
    }
}
```

- [ ] **Step 2: Run to verify fail**

```bash
./gradlew :app:test --tests "*.SearchViewModelTest"
```

Expected: FAIL.

- [ ] **Step 3: Create `SearchViewModel.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: PokemonRepository) : ViewModel() {
    private val allPokemon = mutableListOf<PokemonSummary>()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes

    private val _filteredList = MutableStateFlow<List<PokemonSummary>>(emptyList())
    val filteredList: StateFlow<List<PokemonSummary>> = _filteredList

    init {
        viewModelScope.launch {
            try {
                allPokemon.addAll(repository.getPokemonList())
                applyFilter()
            } catch (_: Exception) {}
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
        applyFilter()
    }

    fun onTypeToggle(type: String) {
        _selectedTypes.value = _selectedTypes.value.toMutableSet().apply {
            if (contains(type)) remove(type) else add(type)
        }
        applyFilter()
    }

    private fun applyFilter() {
        val q = _query.value.lowercase()
        _filteredList.value = allPokemon.filter { pokemon ->
            (q.isEmpty() || pokemon.name.contains(q))
        }
    }

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SearchViewModel(repository) }
        }
    }
}
```

> **Note:** Type filtering is not applied in the list view (types aren't available without individual detail calls). The `selectedTypes` state is exposed for the UI chip row but filtering by type is left for a follow-up that can cache detail data.

- [ ] **Step 4: Create `SearchScreen.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.common.PokemonCard
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDarkRed
import com.madmaxlgndklr.pokedex.ui.theme.PokedexGreen
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPokemonClick: (Int) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val filteredList by viewModel.filteredList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexGreen)
    ) {
        BasicTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream
            ),
            cursorBrush = SolidColor(PokedexCream),
            decorationBox = { inner ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(PokedexDarkRed)
                        .padding(12.dp)
                ) {
                    if (query.isEmpty()) {
                        Text(
                            "SEARCH...",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = PokedexCream.copy(alpha = 0.5f)
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList, key = { it.id }) { summary ->
                PokemonCard(
                    id = summary.id,
                    name = summary.name,
                    types = emptyList(),
                    onClick = { onPokemonClick(summary.id) }
                )
            }
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :app:test --tests "*.SearchViewModelTest"
```

Expected: 3 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/search/ \
    app/src/test/java/com/madmaxlgndklr/pokedex/search/
git commit -m "feat: add Search screen and SearchViewModel"
```

---

## Task 11: Detail Screen

**Files:**
- Create: `ui/detail/PokemonDetailViewModel.kt`
- Create: `ui/detail/DetailScreen.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/detail/PokemonDetailViewModelTest.kt`

- [ ] **Step 1: Write the failing ViewModel test**

```kotlin
package com.madmaxlgndklr.pokedex.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.detail.PokemonDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonDetailViewModelTest {
    @get:Rule val instantTaskRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PokemonDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repo = PokemonRepository(FakePokeApiService(), FakeCaughtPokemonDao())
        viewModel = PokemonDetailViewModel(repo, pokemonId = 1)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Loading`() {
        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    @Test
    fun `after load returns correct detail`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val detail = (state as UiState.Success<PokemonDetail>).data
        assertEquals("bulbasaur", detail.name)
        assertEquals(listOf("grass", "poison"), detail.types)
        assertEquals("A strange seed was planted.", detail.flavorText)
    }

    @Test
    fun `toggling caught inserts into dao`() = runTest {
        advanceUntilIdle()
        viewModel.toggleCaught()
        advanceUntilIdle()
        assertTrue(viewModel.isCaught.value)
    }
}
```

- [ ] **Step 2: Run to verify fail**

```bash
./gradlew :app:test --tests "*.PokemonDetailViewModelTest"
```

Expected: FAIL.

- [ ] **Step 3: Create `PokemonDetailViewModel.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PokemonDetailViewModel(
    private val repository: PokemonRepository,
    private val pokemonId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<PokemonDetail>>(UiState.Loading)
    val uiState: StateFlow<UiState<PokemonDetail>> = _uiState

    val isCaught: StateFlow<Boolean> = repository.isCaught(pokemonId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var currentDetail: PokemonDetail? = null

    init {
        viewModelScope.launch {
            _uiState.value = try {
                val detail = repository.getPokemonDetail(pokemonId)
                currentDetail = detail
                UiState.Success(detail)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Failed to load Pokémon")
            }
        }
    }

    fun toggleCaught() {
        val detail = currentDetail ?: return
        viewModelScope.launch {
            repository.setCaught(
                id = detail.id,
                name = detail.name,
                caught = !isCaught.value
            )
        }
    }

    companion object {
        fun factory(repository: PokemonRepository, pokemonId: Int): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { PokemonDetailViewModel(repository, pokemonId) }
            }
    }
}
```

- [ ] **Step 4: Create `DetailScreen.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.common.EvolutionChain
import com.madmaxlgndklr.pokedex.ui.common.StatBar
import com.madmaxlgndklr.pokedex.ui.common.TypeBadge
import com.madmaxlgndklr.pokedex.ui.common.UiState
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun DetailScreen(
    viewModel: PokemonDetailViewModel,
    onBack: () -> Unit,
    onEvolutionClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isCaught by viewModel.isCaught.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexDark)
    ) {
        when (val state = uiState) {
            is UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                text = "NO SIGNAL\n\n${state.message}",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
            is UiState.Success -> DetailContent(
                detail = state.data,
                isCaught = isCaught,
                onBack = onBack,
                onToggleCaught = viewModel::toggleCaught,
                onEvolutionClick = onEvolutionClick
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: PokemonDetail,
    isCaught: Boolean,
    onBack: () -> Unit,
    onToggleCaught: () -> Unit,
    onEvolutionClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PokedexRed)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
            }
            Text(
                text = "#${detail.id.toString().padStart(3, '0')} ${detail.name.uppercase()}",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleCaught) {
                Icon(
                    if (isCaught) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isCaught) "Uncatch" else "Catch",
                    tint = if (isCaught) CaughtGold else PokedexCream,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Sprite
        AsyncImage(
            model = detail.spriteUrl,
            contentDescription = "${detail.name} sprite",
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
        )

        // Types
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        ) {
            detail.types.forEach { TypeBadge(it) }
        }

        SectionDivider("STATS")
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            detail.stats.forEach { stat ->
                StatBar(label = stat.name, value = stat.value)
            }
        }

        SectionDivider("MOVES")
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            detail.moves.take(20).forEach { move ->
                Text(
                    text = "LV${move.levelLearnedAt.toString().padStart(3)} ${move.name.replace('-', ' ').uppercase()}",
                    fontFamily = PressStart2P,
                    fontSize = 7.sp,
                    color = PokedexCream
                )
            }
        }

        if (detail.evolutionChain.size > 1) {
            SectionDivider("EVOLUTION")
            EvolutionChain(
                stages = detail.evolutionChain,
                onPokemonClick = onEvolutionClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        SectionDivider("POKÉDEX ENTRY")
        Text(
            text = detail.flavorText,
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = PokedexCream,
            lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionDivider(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f), color = PokedexCream.copy(alpha = 0.3f))
        Text(
            text = "  $label  ",
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = PokedexCream.copy(alpha = 0.7f)
        )
        HorizontalDivider(Modifier.weight(1f), color = PokedexCream.copy(alpha = 0.3f))
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :app:test --tests "*.PokemonDetailViewModelTest"
```

Expected: 3 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/detail/ \
    app/src/test/java/com/madmaxlgndklr/pokedex/detail/
git commit -m "feat: add Detail screen and PokemonDetailViewModel"
```

---

## Task 12: My Collection Screen

**Files:**
- Create: `ui/mycollection/MyCollectionViewModel.kt`
- Create: `ui/mycollection/MyCollectionScreen.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/mycollection/MyCollectionViewModelTest.kt`

- [ ] **Step 1: Write the failing ViewModel test**

```kotlin
package com.madmaxlgndklr.pokedex.mycollection

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import com.madmaxlgndklr.pokedex.ui.mycollection.MyCollectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyCollectionViewModelTest {
    @get:Rule val instantTaskRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()
    private val fakeDao = FakeCaughtPokemonDao()
    private lateinit var viewModel: MyCollectionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repo = PokemonRepository(FakePokeApiService(), fakeDao)
        viewModel = MyCollectionViewModel(repo)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `initially empty`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.caughtList.value.isEmpty())
    }

    @Test
    fun `reflects newly caught pokemon`() = runTest {
        advanceUntilIdle()
        val repo = PokemonRepository(FakePokeApiService(), fakeDao)
        repo.setCaught(1, "bulbasaur", true)
        advanceUntilIdle()
        assertEquals(1, viewModel.caughtList.value.size)
        assertEquals("bulbasaur", viewModel.caughtList.value[0].name)
    }
}
```

- [ ] **Step 2: Run to verify fail**

```bash
./gradlew :app:test --tests "*.MyCollectionViewModelTest"
```

Expected: FAIL.

- [ ] **Step 3: Create `MyCollectionViewModel.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.mycollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MyCollectionViewModel(repository: PokemonRepository) : ViewModel() {
    val caughtList: StateFlow<List<PokemonSummary>> = repository.getCaughtPokemon()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { MyCollectionViewModel(repository) }
        }
    }
}
```

- [ ] **Step 4: Create `MyCollectionScreen.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.mycollection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.common.PokemonCard
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexGreen
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun MyCollectionScreen(
    viewModel: MyCollectionViewModel,
    onPokemonClick: (Int) -> Unit
) {
    val caughtList by viewModel.caughtList.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexGreen)
    ) {
        if (caughtList.isEmpty()) {
            Text(
                text = "NO POKÉMON\nCAUGHT YET",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = PokedexCream,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(caughtList, key = { it.id }) { summary ->
                    PokemonCard(
                        id = summary.id,
                        name = summary.name,
                        types = emptyList(),
                        onClick = { onPokemonClick(summary.id) }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :app:test --tests "*.MyCollectionViewModelTest"
```

Expected: 2 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/mycollection/ \
    app/src/test/java/com/madmaxlgndklr/pokedex/mycollection/
git commit -m "feat: add My Collection screen and ViewModel"
```

---

## Task 13: Navigation & MainActivity

**Files:**
- Create: `ui/navigation/AppNavigation.kt`
- Modify: `MainActivity.kt`

- [ ] **Step 1: Create `AppNavigation.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.madmaxlgndklr.pokedex.PokedexApplication
import com.madmaxlgndklr.pokedex.ui.detail.DetailScreen
import com.madmaxlgndklr.pokedex.ui.detail.PokemonDetailViewModel
import com.madmaxlgndklr.pokedex.ui.list.ListScreen
import com.madmaxlgndklr.pokedex.ui.list.PokemonListViewModel
import com.madmaxlgndklr.pokedex.ui.mycollection.MyCollectionScreen
import com.madmaxlgndklr.pokedex.ui.mycollection.MyCollectionViewModel
import com.madmaxlgndklr.pokedex.ui.search.SearchScreen
import com.madmaxlgndklr.pokedex.ui.search.SearchViewModel
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDarkRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import kotlinx.coroutines.launch

private object Routes {
    const val LIST = "list"
    const val SEARCH = "search"
    const val MY_COLLECTION = "my_collection"
    const val DETAIL = "detail/{pokemonId}"
    fun detail(id: Int) = "detail/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val repo = (LocalContext.current.applicationContext as PokedexApplication).repository

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(PokedexDarkRed)
                    .padding(24.dp)
            ) {
                Spacer(Modifier.height(48.dp))
                DrawerItem("● POKÉDEX") {
                    navController.navigate(Routes.LIST) { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
                Spacer(Modifier.height(24.dp))
                DrawerItem("◌ SEARCH") {
                    navController.navigate(Routes.SEARCH) { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
                Spacer(Modifier.height(24.dp))
                DrawerItem("◌ MY POKÉDEX") {
                    navController.navigate(Routes.MY_COLLECTION) { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.LIST,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.LIST) {
                val vm: PokemonListViewModel = viewModel(factory = PokemonListViewModel.factory(repo))
                ListScreen(vm) { id -> navController.navigate(Routes.detail(id)) }
            }
            composable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(factory = SearchViewModel.factory(repo))
                SearchScreen(vm) { id -> navController.navigate(Routes.detail(id)) }
            }
            composable(Routes.MY_COLLECTION) {
                val vm: MyCollectionViewModel = viewModel(factory = MyCollectionViewModel.factory(repo))
                MyCollectionScreen(vm) { id -> navController.navigate(Routes.detail(id)) }
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("pokemonId") { type = NavType.IntType })
            ) { backStackEntry ->
                val pokemonId = backStackEntry.arguments!!.getInt("pokemonId")
                val vm: PokemonDetailViewModel = viewModel(
                    key = "detail_$pokemonId",
                    factory = PokemonDetailViewModel.factory(repo, pokemonId)
                )
                DetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onEvolutionClick = { id -> navController.navigate(Routes.detail(id)) }
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = PressStart2P,
        fontSize = 10.sp,
        color = PokedexCream,
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

- [ ] **Step 2: Replace `MainActivity.kt`**

```kotlin
package com.madmaxlgndklr.pokedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.madmaxlgndklr.pokedex.ui.navigation.AppNavigation
import com.madmaxlgndklr.pokedex.ui.theme.PokedexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PokedexTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
```

- [ ] **Step 3: Run the full test suite**

```bash
./gradlew :app:test
```

Expected: All unit tests PASS.

- [ ] **Step 4: Build and install on emulator**

```bash
./gradlew :app:installDebug
```

Expected: App installs. Launch it and verify:
- List screen loads with Pokémon grid
- Tapping a card navigates to detail screen with sprite, stats, moves, evolution, and flavor text
- ★ button toggles gold/white and updates My Pokédex drawer section
- Drawer opens from left edge swipe or hamburger icon
- Search screen filters the list as you type

- [ ] **Step 5: Final commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/navigation/ \
    app/src/main/java/com/madmaxlgndklr/pokedex/MainActivity.kt
git commit -m "feat: add navigation drawer, NavHost, and wire MainActivity"
```

- [ ] **Step 6: Push branch**

```bash
git push origin feature/pokedex-app
```

---

## Self-Review Notes

- All spec sections are covered: browse list ✓, search ✓, caught tracker ✓, detail (sprite/types/stats/moves/evolution/flavor) ✓, retro theme ✓, drawer nav ✓
- `FakePokeApiService` and `FakeCaughtPokemonDao` are defined once in `PokemonRepositoryTest.kt` and reused by ViewModel tests via import — no duplication
- `typeColor()` function defined in `Color.kt` and referenced consistently in `TypeBadge.kt`
- `EvolutionStage`/`EvolutionNode` defined in `PokemonDetail.kt` and used consistently in `PokemonRepository`, `EvolutionChain` composable, and `DetailScreen`
- `UiState` sealed class imported by all 4 ViewModels and their tests
- Type filtering in `SearchViewModel` is stubbed (tracks selected types but doesn't filter — types unavailable without per-Pokémon detail calls). This is called out inline in Task 10 step 3.
