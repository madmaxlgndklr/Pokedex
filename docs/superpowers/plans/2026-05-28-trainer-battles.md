# Trainer Battles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a TRAINERS tab to BattleHub that lets the player fight any gym leader, Elite Four member, champion, or rival from generations I–IX in a full 6v6 battle using the existing team-switching engine.

**Architecture:** Trainer data ships as a bundled JSON asset (`assets/trainers.json`) parsed once at runtime by `TrainerRepository`. A new `TrainerSelectViewModel` owns selection state (expanded regions, bottom sheet, roster toggle). `TurnBattleViewModel` gains a `battleTrainer: StateFlow<SelectedTrainer?>` and two new entry points (`loadTrainerSetup`, `startTrainerBattle`). `BattleHubScreen` adds a fourth tab that renders `TrainerSelectScreen`.

**Tech Stack:** Kotlin, Jetpack Compose, StateFlow/ViewModel, Gson (already on classpath via `retrofit.gson`), JUnit4 + kotlinx-coroutines-test

---

## File Map

**New files:**
- `app/src/main/assets/trainers.json`
- `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerRoster.kt`
- `app/src/main/java/com/madmaxlgndklr/pokedex/data/trainer/TrainerRepository.kt`
- `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerSelectViewModel.kt`
- `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerSelectScreen.kt`
- `app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerRepositoryTest.kt`
- `app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerBattleViewModelTest.kt`

**Modified files:**
- `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleViewModel.kt`
- `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt`
- `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/BattleHubScreen.kt`
- `app/src/main/java/com/madmaxlgndklr/pokedex/ui/navigation/AppNavigation.kt`

---

## Task 1: TrainerRoster.kt — data model

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerRoster.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.madmaxlgndklr.pokedex.ui.battle

enum class TrainerClass { GYM_LEADER, ELITE_FOUR, CHAMPION, RIVAL }

data class TrainerPokemon(
    val pokemonId: Int,
    val level: Int,
    val moves: List<String>
)

data class TrainerRoster(
    val label: String,
    val team: List<TrainerPokemon>
)

data class Trainer(
    val id: String,
    val name: String,
    val title: String,
    val region: String,
    val trainerClass: TrainerClass,
    val typeSpecialty: String,
    val rosters: List<TrainerRoster>
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerRoster.kt
git commit -m "feat: add TrainerRoster data model"
```

---

## Task 2: TrainerRepository + TrainerRepositoryTest

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/data/trainer/TrainerRepository.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerRepositoryTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerRepositoryTest.kt
package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.data.trainer.TrainerRepository
import com.madmaxlgndklr.pokedex.ui.battle.TrainerClass
import org.junit.Assert.*
import org.junit.Test

private val MINIMAL_JSON = """
{
  "regions": [
    {
      "name": "Kanto",
      "trainers": [
        {
          "id": "kanto-brock",
          "name": "Brock",
          "title": "Gym Leader",
          "trainerClass": "GYM_LEADER",
          "typeSpecialty": "Rock",
          "rosters": [
            {
              "label": "Original (RBY)",
              "team": [
                { "pokemonId": 74, "level": 12, "moves": ["tackle","defense-curl","mud-slap","rock-throw"] },
                { "pokemonId": 95, "level": 14, "moves": ["tackle","screech","bind","rock-throw"] },
                { "pokemonId": 75, "level": 12, "moves": ["tackle","defense-curl","mud-slap","magnitude"] },
                { "pokemonId": 246, "level": 10, "moves": ["tackle","leer","bite","sandstorm"] },
                { "pokemonId": 111, "level": 11, "moves": ["tackle","tail-whip","stomp","horn-attack"] },
                { "pokemonId": 213, "level": 11, "moves": ["tackle","harden","constrict","rock-throw"] }
              ]
            },
            {
              "label": "Rematch (FRLG)",
              "team": [
                { "pokemonId": 75, "level": 51, "moves": ["earthquake","rock-slide","explosion","stealth-rock"] },
                { "pokemonId": 76, "level": 51, "moves": ["earthquake","rock-blast","explosion","stealth-rock"] },
                { "pokemonId": 95, "level": 52, "moves": ["earthquake","rock-slide","screech","iron-tail"] },
                { "pokemonId": 248, "level": 52, "moves": ["earthquake","crunch","rock-slide","dragon-dance"] },
                { "pokemonId": 112, "level": 53, "moves": ["earthquake","megahorn","rock-slide","stomp"] },
                { "pokemonId": 377, "level": 54, "moves": ["earthquake","ancientpower","calm-mind","psychic"] }
              ]
            }
          ]
        },
        {
          "id": "kanto-misty",
          "name": "Misty",
          "title": "Gym Leader",
          "trainerClass": "GYM_LEADER",
          "typeSpecialty": "Water",
          "rosters": [
            {
              "label": "Original (RBY)",
              "team": [
                { "pokemonId": 116, "level": 16, "moves": ["water-gun","smokescreen","leer","bubble"] },
                { "pokemonId": 118, "level": 16, "moves": ["peck","water-gun","horn-attack","tail-whip"] },
                { "pokemonId": 72,  "level": 17, "moves": ["wrap","constrict","poison-sting","supersonic"] },
                { "pokemonId": 86,  "level": 17, "moves": ["headbutt","growl","water-gun","rest"] },
                { "pokemonId": 120, "level": 18, "moves": ["water-gun","harden","minimize","swift"] },
                { "pokemonId": 121, "level": 21, "moves": ["water-gun","harden","psywave","swift"] }
              ]
            },
            {
              "label": "Rematch (FRLG)",
              "team": [
                { "pokemonId": 195, "level": 42, "moves": ["surf","earthquake","ice-beam","yawn"] },
                { "pokemonId": 131, "level": 44, "moves": ["surf","ice-beam","thunder","confuse-ray"] },
                { "pokemonId": 55,  "level": 46, "moves": ["surf","ice-beam","psychic","calm-mind"] },
                { "pokemonId": 87,  "level": 44, "moves": ["surf","ice-beam","aurora-beam","rest"] },
                { "pokemonId": 370, "level": 47, "moves": ["water-gun","attract","sweet-kiss","take-down"] },
                { "pokemonId": 121, "level": 47, "moves": ["surf","ice-beam","thunder","recover"] }
              ]
            }
          ]
        }
      ]
    },
    {
      "name": "Johto",
      "trainers": [
        {
          "id": "johto-falkner",
          "name": "Falkner",
          "title": "Gym Leader",
          "trainerClass": "GYM_LEADER",
          "typeSpecialty": "Flying",
          "rosters": [
            {
              "label": "Original (GSC)",
              "team": [
                { "pokemonId": 16,  "level": 7,  "moves": ["gust","sand-attack","tackle","swift"] },
                { "pokemonId": 17,  "level": 9,  "moves": ["gust","quick-attack","wing-attack","sand-attack"] },
                { "pokemonId": 163, "level": 8,  "moves": ["hypnosis","tackle","foresight","peck"] },
                { "pokemonId": 164, "level": 8,  "moves": ["hypnosis","tackle","foresight","peck"] },
                { "pokemonId": 21,  "level": 7,  "moves": ["growl","peck","leer","fury-attack"] },
                { "pokemonId": 22,  "level": 9,  "moves": ["growl","peck","leer","fury-attack"] }
              ]
            },
            {
              "label": "Rematch (HGSS)",
              "team": [
                { "pokemonId": 16,  "level": 35, "moves": ["aerial-ace","wing-attack","roost","u-turn"] },
                { "pokemonId": 17,  "level": 38, "moves": ["aerial-ace","wing-attack","roost","u-turn"] },
                { "pokemonId": 22,  "level": 38, "moves": ["drill-peck","aerial-ace","roost","scary-face"] },
                { "pokemonId": 164, "level": 41, "moves": ["air-slash","hypnosis","reflect","extrasensory"] },
                { "pokemonId": 279, "level": 41, "moves": ["surf","air-slash","roost","protect"] },
                { "pokemonId": 398, "level": 50, "moves": ["close-combat","fly","u-turn","final-gambit"] }
              ]
            }
          ]
        }
      ]
    }
  ]
}
""".trimIndent()

class TrainerRepositoryTest {

    private fun repo() = TrainerRepository(MINIMAL_JSON)

    @Test
    fun `getAll returns trainers from all regions`() {
        val all = repo().getAll()
        assertEquals(3, all.size)
    }

    @Test
    fun `getByRegion returns only trainers from that region`() {
        val kanto = repo().getByRegion("Kanto")
        assertEquals(2, kanto.size)
        assertTrue(kanto.all { it.region == "Kanto" })
    }

    @Test
    fun `getByRegion unknown region returns empty list`() {
        assertTrue(repo().getByRegion("Atlantis").isEmpty())
    }

    @Test
    fun `getById returns correct trainer`() {
        val brock = repo().getById("kanto-brock")
        assertNotNull(brock)
        assertEquals("Brock", brock!!.name)
        assertEquals("Rock", brock.typeSpecialty)
    }

    @Test
    fun `getById unknown id returns null`() {
        assertNull(repo().getById("nope"))
    }

    @Test
    fun `trainer with two rosters has correct roster labels`() {
        val brock = repo().getById("kanto-brock")!!
        assertEquals(2, brock.rosters.size)
        assertEquals("Original (RBY)", brock.rosters[0].label)
        assertEquals("Rematch (FRLG)", brock.rosters[1].label)
    }

    @Test
    fun `each roster has exactly 6 pokemon`() {
        repo().getAll().forEach { trainer ->
            trainer.rosters.forEach { roster ->
                assertEquals("${trainer.id} roster '${roster.label}' must have 6 pokemon",
                    6, roster.team.size)
            }
        }
    }

    @Test
    fun `each pokemon has exactly 4 moves`() {
        repo().getAll().forEach { trainer ->
            trainer.rosters.forEach { roster ->
                roster.team.forEach { mon ->
                    assertEquals("${trainer.id}: every pokemon must have 4 moves",
                        4, mon.moves.size)
                }
            }
        }
    }

    @Test
    fun `getAll is cached — second call returns same list instance`() {
        val r = repo()
        assertSame(r.getAll(), r.getAll())
    }

    @Test
    fun `empty json regions returns empty list`() {
        val r = TrainerRepository("""{"regions":[]}""")
        assertTrue(r.getAll().isEmpty())
    }

    @Test
    fun `malformed json returns empty list without throwing`() {
        val r = TrainerRepository("not json at all {{{")
        assertTrue(r.getAll().isEmpty())
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure (TrainerRepository not yet defined)**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.TrainerRepositoryTest" 2>&1 | tail -20
```

Expected: FAILED — `TrainerRepository` not found

- [ ] **Step 3: Create TrainerRepository**

```kotlin
// app/src/main/java/com/madmaxlgndklr/pokedex/data/trainer/TrainerRepository.kt
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
        } catch (_: Exception) { emptyList() }
        cache = loaded
        return loaded
    }

    fun getByRegion(region: String): List<Trainer> =
        getAll().filter { it.region == region }

    fun getById(id: String): Trainer? =
        getAll().firstOrNull { it.id == id }
}
```

- [ ] **Step 4: Run tests — expect all pass**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.TrainerRepositoryTest" 2>&1 | tail -20
```

Expected: 11 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/data/trainer/TrainerRepository.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerRepositoryTest.kt
git commit -m "feat: add TrainerRepository with JSON parsing and unit tests"
```

---
## Task 3: trainers.json — Kanto

**Files:**
- Create: `app/src/main/assets/trainers.json`

First create the assets directory, then write the file. Tasks 4–11 will each append a region by rewriting this file.

- [ ] **Step 1: Create assets directory and write Kanto JSON**

```bash
mkdir -p app/src/main/assets
```

Write `app/src/main/assets/trainers.json`:

```json
{
  "regions": [
    {
      "name": "Kanto",
      "trainers": [
        {
          "id": "kanto-brock", "name": "Brock", "title": "Gym Leader",
          "trainerClass": "GYM_LEADER", "typeSpecialty": "Rock",
          "rosters": [
            { "label": "Original (RBY)", "team": [
              { "pokemonId": 74,  "level": 12, "moves": ["tackle","defense-curl","mud-slap","rock-throw"] },
              { "pokemonId": 95,  "level": 14, "moves": ["tackle","screech","bind","rock-throw"] },
              { "pokemonId": 75,  "level": 12, "moves": ["tackle","defense-curl","mud-slap","magnitude"] },
              { "pokemonId": 246, "level": 10, "moves": ["tackle","leer","bite","sandstorm"] },
              { "pokemonId": 111, "level": 11, "moves": ["tackle","tail-whip","stomp","horn-attack"] },
              { "pokemonId": 213, "level": 11, "moves": ["tackle","harden","constrict","rock-throw"] }
            ]},
            { "label": "Rematch (FRLG)", "team": [
              { "pokemonId": 75,  "level": 51, "moves": ["earthquake","rock-slide","explosion","stealth-rock"] },
              { "pokemonId": 76,  "level": 51, "moves": ["earthquake","rock-blast","explosion","stealth-rock"] },
              { "pokemonId": 95,  "level": 52, "moves": ["earthquake","rock-slide","screech","iron-tail"] },
              { "pokemonId": 248, "level": 52, "moves": ["earthquake","crunch","rock-slide","dragon-dance"] },
              { "pokemonId": 112, "level": 53, "moves": ["earthquake","megahorn","rock-slide","stomp"] },
              { "pokemonId": 377, "level": 54, "moves": ["earthquake","ancientpower","calm-mind","psychic"] }
            ]}
          ]
        },
        {
          "id": "kanto-misty", "name": "Misty", "title": "Gym Leader",
          "trainerClass": "GYM_LEADER", "typeSpecialty": "Water",
          "rosters": [
            { "label": "Original (RBY)", "team": [
              { "pokemonId": 116, "level": 16, "moves": ["water-gun","smokescreen","leer","bubble"] },
              { "pokemonId": 118, "level": 16, "moves": ["peck","water-gun","horn-attack","tail-whip"] },
              { "pokemonId": 72,  "level": 17, "moves": ["wrap","constrict","poison-sting","supersonic"] },
              { "pokemonId": 86,  "level": 17, "moves": ["headbutt","growl","water-gun","rest"] },
              { "pokemonId": 120, "level": 18, "moves": ["water-gun","harden","minimize","swift"] },
              { "pokemonId": 121, "level": 21, "moves": ["water-gun","harden","psywave","swift"] }
            ]},
            { "label": "Rematch (FRLG)", "team": [
              { "pokemonId": 195, "level": 42, "moves": ["surf","earthquake","ice-beam","yawn"] },
              { "pokemonId": 131, "level": 44, "moves": ["surf","ice-beam","thunder","confuse-ray"] },
              { "pokemonId": 55,  "level": 46, "moves": ["surf","ice-beam","psychic","calm-mind"] },
              { "pokemonId": 87,  "level": 44, "moves": ["surf","ice-beam","aurora-beam","rest"] },
              { "pokemonId": 370, "level": 47, "moves": ["water-pulse","attract","sweet-kiss","take-down"] },
              { "pokemonId": 121, "level": 47, "moves": ["surf","ice-beam","thunder","recover"] }
            ]}
          ]
        },
        {
          "id": "kanto-surge", "name": "Lt. Surge", "title": "Gym Leader",
          "trainerClass": "GYM_LEADER", "typeSpecialty": "Electric",
          "rosters": [
            { "label": "Original (RBY)", "team": [
              { "pokemonId": 81,  "level": 19, "moves": ["thundershock","sonicboom","thunder-wave","screech"] },
              { "pokemonId": 100, "level": 21, "moves": ["spark","screech","sonicboom","thunder-wave"] },
              { "pokemonId": 25,  "level": 18, "moves": ["thunderbolt","growl","quick-attack","double-team"] },
              { "pokemonId": 26,  "level": 24, "moves": ["thunderbolt","slam","quick-attack","thunder-wave"] },
              { "pokemonId": 82,  "level": 20, "moves": ["thunderbolt","thunder-wave","screech","sonicboom"] },
              { "pokemonId": 101, "level": 22, "moves": ["thunderbolt","thunder-wave","screech","light-screen"] }
            ]},
            { "label": "Rematch (FRLG)", "team": [
              { "pokemonId": 26,  "level": 44, "moves": ["thunderbolt","slam","quick-attack","thunder-wave"] },
              { "pokemonId": 101, "level": 40, "moves": ["thunderbolt","thunder-wave","screech","explosion"] },
              { "pokemonId": 101, "level": 42, "moves": ["thunderbolt","thunder-wave","screech","explosion"] },
              { "pokemonId": 310, "level": 46, "moves": ["thunderbolt","charge-beam","quick-attack","roar"] },
              { "pokemonId": 135, "level": 46, "moves": ["thunderbolt","thunder-wave","quick-attack","shadow-ball"] },
              { "pokemonId": 125, "level": 47, "moves": ["thunderbolt","thunder-punch","quick-attack","screech"] }
            ]}
          ]
        },
        {
          "id": "kanto-erika", "name": "Erika", "title": "Gym Leader",
          "trainerClass": "GYM_LEADER", "typeSpecialty": "Grass",
          "rosters": [
            { "label": "Original (RBY)", "team": [
              { "pokemonId": 43,  "level": 24, "moves": ["absorb","poisonpowder","sleep-powder","acid"] },
              { "pokemonId": 114, "level": 24, "moves": ["constrict","sleep-powder","absorb","bind"] },
              { "pokemonId": 70,  "level": 25, "moves": ["vine-whip","sleep-powder","acid","wrap"] },
              { "pokemonId": 44,  "level": 26, "moves": ["absorb","poisonpowder","sleep-powder","acid"] },
              { "pokemonId": 71,  "level": 29, "moves": ["razor-leaf","sleep-powder","acid","wrap"] },
              { "pokemonId": 45,  "level": 29, "moves": ["petal-dance","sleep-powder","acid","poisonpowder"] }
            ]},
            { "label": "Rematch (FRLG)", "team": [
              { "pokemonId": 114, "level": 46, "moves": ["mega-drain","sleep-powder","bind","ancientpower"] },
              { "pokemonId": 189, "level": 47, "moves": ["mega-drain","sleep-powder","cotton-spore","leech-seed"] },
              { "pokemonId": 71,  "level": 46, "moves": ["razor-leaf","sleep-powder","acid","swords-dance"] },
              { "pokemonId": 182, "level": 46, "moves": ["petal-dance","sleep-powder","acid","sunny-day"] },
              { "pokemonId": 45,  "level": 46, "moves": ["petal-dance","sleep-powder","acid","moonlight"] },
              { "pokemonId": 103, "level": 47, "moves": ["psychic","mega-drain","sleep-powder","egg-bomb"] }
            ]}
          ]
        },
        {
          "id": "kanto-koga", "name": "Koga", "title": "Gym Leader",
          "trainerClass": "GYM_LEADER", "typeSpecialty": "Poison",
          "rosters": [
            { "label": "Original (RBY)", "team": [
              { "pokemonId": 23,  "level": 37, "moves": ["glare","acid","screech","bite"] },
              { "pokemonId": 24,  "level": 38, "moves": ["glare","acid","screech","bite"] },
              { "pokemonId": 109, "level": 37, "moves": ["smog","self-destruct","smokescreen","sludge"] },
              { "pokemonId": 89,  "level": 39, "moves": ["smog","poison-gas","minimize","sludge"] },
              { "pokemonId": 109, "level": 37, "moves": ["smog","self-destruct","smokescreen","sludge"] },
              { "pokemonId": 110, "level": 43, "moves": ["smog","self-destruct","smokescreen","explosion"] }
            ]},
            { "label": "Rematch (FRLG)", "team": [
              { "pokemonId": 49,  "level": 48, "moves": ["psychic","sludge-bomb","sleep-powder","leech-life"] },
              { "pokemonId": 205, "level": 48, "moves": ["explosion","toxic-spikes","rapid-spin","volt-switch"] },
              { "pokemonId": 89,  "level": 50, "moves": ["shadow-ball","gunk-shot","minimize","memento"] },
              { "pokemonId": 169, "level": 50, "moves": ["sludge-bomb","cross-poison","air-slash","confuse-ray"] },
              { "pokemonId": 110, "level": 50, "moves": ["sludge-bomb","explosion","smokescreen","thunderbolt"] },
              { "pokemonId": 317, "level": 50, "moves": ["sludge-bomb","shadow-ball","toxic","yawn"] }
            ]}
          ]
        },
        {
          "id": "kanto-sabrina", "name": "Sabrina", "title": "Gym Leader",
          "trainerClass": "GYM_LEADER", "typeSpecialty": "Psychic",
          "rosters": [
            { "label": "Original (RBY)", "team": [
              { "pokemonId": 97,  "level": 37, "moves": ["hypnosis","dream-eater","psychic","headbutt"] },
              { "pokemonId": 64,  "level": 38, "moves": ["psybeam","confusion","kinesis","recover"] },
              { "pokemonId": 122, "level": 37, "moves": ["psybeam","confusion","light-screen","barrier"] },
              { "pokemonId": 49,  "level": 38, "moves": ["psychic","sleep-powder","leech-life","psybeam"] },
              { "pokemonId": 65,  "level": 43, "moves": ["psychic","recover","calm-mind","psybeam"] },
              { "pokemonId": 80,  "level": 37, "moves": ["psychic","surf","amnesia","headbutt"] }
            ]},
            { "label": "Rematch (FRLG)", "team": [
              { "pokemonId": 122, "level": 50, "moves": ["psychic","calm-mind","light-screen","reflect"] },
              { "pokemonId": 202, "level": 50, "moves": ["counter","mirror-coat","destiny-bond","safeguard"] },
              { "pokemonId": 124, "level": 50, "moves": ["psychic","ice-beam","lovely-kiss","calm-mind"] },
              { "pokemonId": 196, "level": 50, "moves": ["psychic","calm-mind","shadow-ball","morning-sun"] },
              { "pokemonId": 97,  "level": 54, "moves": ["psychic","hypnosis","dream-eater","thunder-punch"] },
              { "pokemonId": 65,  "level": 58, "moves": ["psychic","recover","calm-mind","shadow-ball"] }
            ]}
          ]
        },
        {
          "id": "kanto-blaine", "name": "Blaine", "title": "Gym Leader",
          "trainerClass": "GYM_LEADER", "typeSpecialty": "Fire",
          "rosters": [
            { "label": "Original (RBY)", "team": [
              { "pokemonId": 126, "level": 40, "moves": ["flamethrower","smog","fire-spin","confuse-ray"] },
              { "pokemonId": 58,  "level": 42, "moves": ["flamethrower","ember","bite","leer"] },
              { "pokemonId": 77,  "level": 40, "moves": ["flamethrower","fire-spin","agility","stomp"] },
              { "pokemonId": 78,  "level": 42, "moves": ["flamethrower","fire-spin","agility","stomp"] },
              { "pokemonId": 136, "level": 41, "moves": ["flamethrower","ember","sand-attack","quick-attack"] },
              { "pokemonId": 59,  "level": 47, "moves": ["flamethrower","fire-blast","agility","extreme-speed"] }
            ]},
            { "label": "Rematch (FRLG)", "team": [
              { "pokemonId": 219, "level": 54, "moves": ["flamethrower","rock-slide","earth-power","overheat"] },
              { "pokemonId": 126, "level": 54, "moves": ["fire-blast","thunder-punch","cross-chop","smokescreen"] },
              { "pokemonId": 78,  "level": 54, "moves": ["flamethrower","fire-blast","agility","bounce"] },
              { "pokemonId": 103, "level": 54, "moves": ["psychic","fire-blast","sleep-powder","sunny-day"] },
              { "pokemonId": 38,  "level": 54, "moves": ["flamethrower","fire-blast","confuse-ray","quick-attack"] },
              { "pokemonId": 59,  "level": 58, "moves": ["flamethrower","fire-blast","extreme-speed","roar"] }
            ]}
          ]
        },
        {
          "id": "kanto-giovanni", "name": "Giovanni", "title": "Gym Leader",
          "trainerClass": "GYM_LEADER", "typeSpecialty": "Ground",
          "rosters": [
            { "label": "Original (RBY)", "team": [
              { "pokemonId": 53,  "level": 40, "moves": ["slash","bubble-beam","pay-day","screech"] },
              { "pokemonId": 111, "level": 45, "moves": ["horn-attack","stomp","tail-whip","fury-attack"] },
              { "pokemonId": 51,  "level": 42, "moves": ["earthquake","slash","dig","sand-attack"] },
              { "pokemonId": 31,  "level": 44, "moves": ["earthquake","body-slam","toxic","superpower"] },
              { "pokemonId": 34,  "level": 45, "moves": ["earthquake","megahorn","fire-blast","thunder"] },
              { "pokemonId": 111, "level": 50, "moves": ["earthquake","horn-drill","horn-attack","stomp"] }
            ]},
            { "label": "Rematch (FRLG)", "team": [
              { "pokemonId": 34,  "level": 50, "moves": ["earthquake","megahorn","fire-blast","ice-punch"] },
              { "pokemonId": 31,  "level": 50, "moves": ["earthquake","body-slam","crunch","ice-punch"] },
              { "pokemonId": 111, "level": 50, "moves": ["earthquake","horn-drill","megahorn","rock-blast"] },
              { "pokemonId": 51,  "level": 50, "moves": ["earthquake","slash","rock-blast","stone-edge"] },
              { "pokemonId": 112, "level": 55, "moves": ["earthquake","megahorn","rock-blast","hammer-arm"] },
              { "pokemonId": 115, "level": 53, "moves": ["outrage","earthquake","dizzy-punch","sucker-punch"] }
            ]}
          ]
        },
        {
          "id": "kanto-lorelei", "name": "Lorelei", "title": "Elite Four",
          "trainerClass": "ELITE_FOUR", "typeSpecialty": "Ice",
          "rosters": [{ "label": "FRLG", "team": [
            { "pokemonId": 87,  "level": 54, "moves": ["surf","ice-beam","aurora-beam","rest"] },
            { "pokemonId": 91,  "level": 53, "moves": ["surf","blizzard","spikes","explosion"] },
            { "pokemonId": 80,  "level": 54, "moves": ["surf","psychic","amnesia","thunder-wave"] },
            { "pokemonId": 124, "level": 56, "moves": ["psychic","ice-beam","lovely-kiss","blizzard"] },
            { "pokemonId": 221, "level": 54, "moves": ["earthquake","blizzard","mud-bomb","amnesia"] },
            { "pokemonId": 131, "level": 56, "moves": ["surf","ice-beam","thunder","confuse-ray"] }
          ]}]
        },
        {
          "id": "kanto-bruno", "name": "Bruno", "title": "Elite Four",
          "trainerClass": "ELITE_FOUR", "typeSpecialty": "Fighting",
          "rosters": [{ "label": "FRLG", "team": [
            { "pokemonId": 95,  "level": 53, "moves": ["rock-tomb","bind","screech","rock-throw"] },
            { "pokemonId": 107, "level": 55, "moves": ["fire-punch","ice-punch","thunder-punch","mach-punch"] },
            { "pokemonId": 106, "level": 55, "moves": ["high-jump-kick","blaze-kick","close-combat","rapid-spin"] },
            { "pokemonId": 95,  "level": 56, "moves": ["rock-tomb","bind","screech","dragon-breath"] },
            { "pokemonId": 208, "level": 57, "moves": ["earthquake","iron-tail","crunch","screech"] },
            { "pokemonId": 68,  "level": 58, "moves": ["dynamic-punch","earthquake","cross-chop","karate-chop"] }
          ]}]
        },
        {
          "id": "kanto-agatha", "name": "Agatha", "title": "Elite Four",
          "trainerClass": "ELITE_FOUR", "typeSpecialty": "Ghost",
          "rosters": [{ "label": "FRLG", "team": [
            { "pokemonId": 94,  "level": 54, "moves": ["shadow-ball","hypnosis","dream-eater","thunderbolt"] },
            { "pokemonId": 93,  "level": 54, "moves": ["shadow-ball","hypnosis","dream-eater","mean-look"] },
            { "pokemonId": 200, "level": 53, "moves": ["shadow-ball","perish-song","mean-look","confuse-ray"] },
            { "pokemonId": 24,  "level": 56, "moves": ["glare","crunch","earthquake","acid"] },
            { "pokemonId": 93,  "level": 58, "moves": ["shadow-ball","hypnosis","dream-eater","destiny-bond"] },
            { "pokemonId": 94,  "level": 58, "moves": ["shadow-ball","thunderbolt","fire-punch","sludge-bomb"] }
          ]}]
        },
        {
          "id": "kanto-lance", "name": "Lance", "title": "Elite Four",
          "trainerClass": "ELITE_FOUR", "typeSpecialty": "Dragon",
          "rosters": [{ "label": "FRLG", "team": [
            { "pokemonId": 130, "level": 56, "moves": ["hyper-beam","surf","ice-beam","dragon-rage"] },
            { "pokemonId": 148, "level": 56, "moves": ["agility","slam","thunder-wave","dragon-rage"] },
            { "pokemonId": 148, "level": 56, "moves": ["agility","slam","thunder-wave","hyper-beam"] },
            { "pokemonId": 142, "level": 58, "moves": ["fly","ancientpower","bite","wing-attack"] },
            { "pokemonId": 149, "level": 60, "moves": ["hyper-beam","thunder","surf","outrage"] },
            { "pokemonId": 149, "level": 62, "moves": ["fire-blast","thunder","blizzard","outrage"] }
          ]}]
        },
        {
          "id": "kanto-blue", "name": "Blue", "title": "Champion",
          "trainerClass": "CHAMPION", "typeSpecialty": "Mixed",
          "rosters": [{ "label": "FRLG", "team": [
            { "pokemonId": 18,  "level": 59, "moves": ["fly","wing-attack","feather-dance","twister"] },
            { "pokemonId": 65,  "level": 59, "moves": ["psychic","recover","calm-mind","shadow-ball"] },
            { "pokemonId": 112, "level": 61, "moves": ["earthquake","megahorn","rock-blast","hammer-arm"] },
            { "pokemonId": 59,  "level": 61, "moves": ["flamethrower","extreme-speed","dragon-pulse","roar"] },
            { "pokemonId": 103, "level": 63, "moves": ["psychic","sleep-powder","egg-bomb","leaf-storm"] },
            { "pokemonId": 9,   "level": 65, "moves": ["surf","blizzard","ice-beam","skull-bash"] }
          ]}]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Verify the file is valid JSON**

```bash
python3 -m json.tool app/src/main/assets/trainers.json > /dev/null && echo "JSON valid"
```

Expected: `JSON valid`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/trainers.json
git commit -m "feat: add trainers.json with Kanto region data"
```

---
## Task 4: trainers.json — Johto

Add the Johto region to `app/src/main/assets/trainers.json`. Open the file and append the following object inside the `"regions"` array (after the closing `}` of the Kanto entry, before the final `]`).

- [ ] **Step 1: Append Johto region data**

```json
{
  "name": "Johto",
  "trainers": [
    {
      "id": "johto-falkner", "name": "Falkner", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Flying",
      "rosters": [
        { "label": "Original (GSC)", "team": [
          { "pokemonId": 16,  "level": 7,  "moves": ["gust","sand-attack","tackle","swift"] },
          { "pokemonId": 17,  "level": 9,  "moves": ["gust","quick-attack","wing-attack","sand-attack"] },
          { "pokemonId": 163, "level": 8,  "moves": ["hypnosis","tackle","foresight","peck"] },
          { "pokemonId": 164, "level": 8,  "moves": ["hypnosis","tackle","foresight","peck"] },
          { "pokemonId": 21,  "level": 7,  "moves": ["growl","peck","leer","fury-attack"] },
          { "pokemonId": 22,  "level": 9,  "moves": ["growl","peck","leer","fury-attack"] }
        ]},
        { "label": "Rematch (HGSS)", "team": [
          { "pokemonId": 16,  "level": 35, "moves": ["aerial-ace","wing-attack","roost","u-turn"] },
          { "pokemonId": 17,  "level": 38, "moves": ["aerial-ace","wing-attack","roost","u-turn"] },
          { "pokemonId": 22,  "level": 38, "moves": ["drill-peck","aerial-ace","roost","scary-face"] },
          { "pokemonId": 164, "level": 41, "moves": ["air-slash","hypnosis","reflect","extrasensory"] },
          { "pokemonId": 279, "level": 41, "moves": ["surf","air-slash","roost","protect"] },
          { "pokemonId": 398, "level": 50, "moves": ["close-combat","fly","u-turn","final-gambit"] }
        ]}
      ]
    },
    {
      "id": "johto-bugsy", "name": "Bugsy", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Bug",
      "rosters": [
        { "label": "Original (GSC)", "team": [
          { "pokemonId": 11,  "level": 14, "moves": ["tackle","harden","string-shot","bug-bite"] },
          { "pokemonId": 14,  "level": 14, "moves": ["poison-sting","string-shot","harden","bug-bite"] },
          { "pokemonId": 165, "level": 13, "moves": ["comet-punch","string-shot","light-screen","swift"] },
          { "pokemonId": 166, "level": 15, "moves": ["comet-punch","string-shot","light-screen","swift"] },
          { "pokemonId": 123, "level": 16, "moves": ["quick-attack","leer","focus-energy","slash"] },
          { "pokemonId": 123, "level": 16, "moves": ["quick-attack","leer","focus-energy","fury-cutter"] }
        ]},
        { "label": "Rematch (HGSS)", "team": [
          { "pokemonId": 123, "level": 46, "moves": ["x-scissor","quick-attack","swords-dance","aerial-ace"] },
          { "pokemonId": 212, "level": 46, "moves": ["x-scissor","iron-head","swords-dance","aerial-ace"] },
          { "pokemonId": 214, "level": 47, "moves": ["close-combat","megahorn","stone-edge","night-slash"] },
          { "pokemonId": 291, "level": 47, "moves": ["x-scissor","aerial-ace","swords-dance","baton-pass"] },
          { "pokemonId": 269, "level": 48, "moves": ["bug-buzz","psychic","quiver-dance","roost"] },
          { "pokemonId": 267, "level": 50, "moves": ["bug-buzz","psychic","quiver-dance","roost"] }
        ]}
      ]
    },
    {
      "id": "johto-whitney", "name": "Whitney", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Normal",
      "rosters": [
        { "label": "Original (GSC)", "team": [
          { "pokemonId": 35,  "level": 18, "moves": ["pound","sing","doubleslap","minimize"] },
          { "pokemonId": 36,  "level": 19, "moves": ["pound","sing","doubleslap","minimize"] },
          { "pokemonId": 40,  "level": 18, "moves": ["doubleslap","sing","disable","rollout"] },
          { "pokemonId": 39,  "level": 17, "moves": ["pound","sing","doubleslap","defense-curl"] },
          { "pokemonId": 241, "level": 20, "moves": ["tackle","stomp","milk-drink","rollout"] },
          { "pokemonId": 241, "level": 20, "moves": ["tackle","stomp","milk-drink","attract"] }
        ]},
        { "label": "Rematch (HGSS)", "team": [
          { "pokemonId": 39,  "level": 42, "moves": ["hyper-voice","body-slam","sing","mimic"] },
          { "pokemonId": 40,  "level": 42, "moves": ["hyper-voice","body-slam","sing","mimic"] },
          { "pokemonId": 36,  "level": 44, "moves": ["hyper-voice","moonblast","sing","minimize"] },
          { "pokemonId": 206, "level": 44, "moves": ["hyper-voice","glare","rollout","baton-pass"] },
          { "pokemonId": 242, "level": 46, "moves": ["hyper-voice","soft-boiled","heal-bell","seismic-toss"] },
          { "pokemonId": 241, "level": 48, "moves": ["hyper-voice","body-slam","milk-drink","earthquake"] }
        ]}
      ]
    },
    {
      "id": "johto-morty", "name": "Morty", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ghost",
      "rosters": [
        { "label": "Original (GSC)", "team": [
          { "pokemonId": 92,  "level": 21, "moves": ["lick","hypnosis","mean-look","curse"] },
          { "pokemonId": 93,  "level": 21, "moves": ["lick","hypnosis","mean-look","curse"] },
          { "pokemonId": 93,  "level": 23, "moves": ["shadow-ball","hypnosis","mean-look","curse"] },
          { "pokemonId": 94,  "level": 25, "moves": ["shadow-ball","hypnosis","dream-eater","curse"] },
          { "pokemonId": 200, "level": 22, "moves": ["shadow-ball","perish-song","mean-look","psybeam"] },
          { "pokemonId": 200, "level": 22, "moves": ["shadow-ball","perish-song","mean-look","confuse-ray"] }
        ]},
        { "label": "Rematch (HGSS)", "team": [
          { "pokemonId": 92,  "level": 44, "moves": ["shadow-ball","hypnosis","will-o-wisp","destiny-bond"] },
          { "pokemonId": 93,  "level": 44, "moves": ["shadow-ball","hypnosis","will-o-wisp","destiny-bond"] },
          { "pokemonId": 200, "level": 46, "moves": ["shadow-ball","perish-song","mean-look","confuse-ray"] },
          { "pokemonId": 429, "level": 46, "moves": ["shadow-ball","calm-mind","will-o-wisp","nasty-plot"] },
          { "pokemonId": 477, "level": 48, "moves": ["shadow-ball","calm-mind","will-o-wisp","pain-split"] },
          { "pokemonId": 94,  "level": 50, "moves": ["shadow-ball","hypnosis","dream-eater","destiny-bond"] }
        ]}
      ]
    },
    {
      "id": "johto-chuck", "name": "Chuck", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fighting",
      "rosters": [
        { "label": "Original (GSC)", "team": [
          { "pokemonId": 56,  "level": 27, "moves": ["karate-chop","leer","focus-energy","seismic-toss"] },
          { "pokemonId": 57,  "level": 28, "moves": ["karate-chop","leer","focus-energy","rage"] },
          { "pokemonId": 236, "level": 26, "moves": ["rolling-kick","rapid-spin","focus-energy","triple-kick"] },
          { "pokemonId": 107, "level": 27, "moves": ["fire-punch","ice-punch","thunder-punch","mach-punch"] },
          { "pokemonId": 106, "level": 27, "moves": ["high-jump-kick","rolling-kick","focus-energy","meditate"] },
          { "pokemonId": 62,  "level": 30, "moves": ["waterfall","hypnosis","dynamic-punch","body-slam"] }
        ]},
        { "label": "Rematch (HGSS)", "team": [
          { "pokemonId": 107, "level": 46, "moves": ["fire-punch","ice-punch","thunder-punch","mach-punch"] },
          { "pokemonId": 106, "level": 46, "moves": ["high-jump-kick","blaze-kick","close-combat","rapid-spin"] },
          { "pokemonId": 237, "level": 48, "moves": ["close-combat","sucker-punch","rapid-spin","stone-edge"] },
          { "pokemonId": 534, "level": 48, "moves": ["dynamic-punch","stone-edge","bulk-up","payback"] },
          { "pokemonId": 62,  "level": 50, "moves": ["waterfall","hypnosis","dynamic-punch","earthquake"] },
          { "pokemonId": 448, "level": 52, "moves": ["close-combat","aura-sphere","dragon-pulse","calm-mind"] }
        ]}
      ]
    },
    {
      "id": "johto-jasmine", "name": "Jasmine", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Steel",
      "rosters": [
        { "label": "Original (GSC)", "team": [
          { "pokemonId": 81,  "level": 30, "moves": ["thundershock","thunder-wave","sonicboom","screech"] },
          { "pokemonId": 81,  "level": 30, "moves": ["thundershock","thunder-wave","sonicboom","screech"] },
          { "pokemonId": 208, "level": 35, "moves": ["iron-tail","sandstorm","rock-throw","bind"] },
          { "pokemonId": 82,  "level": 32, "moves": ["thunderbolt","thunder-wave","screech","sonicboom"] },
          { "pokemonId": 303, "level": 31, "moves": ["iron-head","crunch","sweet-scent","vice-grip"] },
          { "pokemonId": 227, "level": 32, "moves": ["steel-wing","aerial-ace","spikes","swift"] }
        ]},
        { "label": "Rematch (HGSS)", "team": [
          { "pokemonId": 81,  "level": 46, "moves": ["thunderbolt","magnet-bomb","thunder-wave","flash-cannon"] },
          { "pokemonId": 462, "level": 48, "moves": ["thunderbolt","flash-cannon","thunder-wave","tri-attack"] },
          { "pokemonId": 227, "level": 48, "moves": ["steel-wing","drill-peck","spikes","flash-cannon"] },
          { "pokemonId": 303, "level": 46, "moves": ["iron-head","play-rough","crunch","swords-dance"] },
          { "pokemonId": 205, "level": 48, "moves": ["explosion","gyro-ball","toxic-spikes","rapid-spin"] },
          { "pokemonId": 208, "level": 50, "moves": ["iron-tail","earthquake","stone-edge","crunch"] }
        ]}
      ]
    },
    {
      "id": "johto-pryce", "name": "Pryce", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ice",
      "rosters": [
        { "label": "Original (GSC)", "team": [
          { "pokemonId": 86,  "level": 27, "moves": ["aurora-beam","rest","headbutt","growl"] },
          { "pokemonId": 87,  "level": 29, "moves": ["aurora-beam","rest","surf","headbutt"] },
          { "pokemonId": 220, "level": 28, "moves": ["powder-snow","mud-slap","endure","take-down"] },
          { "pokemonId": 221, "level": 31, "moves": ["blizzard","earthquake","mud-bomb","amnesia"] },
          { "pokemonId": 131, "level": 28, "moves": ["water-gun","ice-beam","mist","perish-song"] },
          { "pokemonId": 124, "level": 30, "moves": ["ice-punch","lovely-kiss","doubleslap","mean-look"] }
        ]},
        { "label": "Rematch (HGSS)", "team": [
          { "pokemonId": 86,  "level": 44, "moves": ["surf","ice-beam","aurora-beam","aqua-jet"] },
          { "pokemonId": 87,  "level": 46, "moves": ["surf","ice-beam","aurora-beam","aqua-jet"] },
          { "pokemonId": 221, "level": 46, "moves": ["blizzard","earthquake","mud-bomb","ice-shard"] },
          { "pokemonId": 131, "level": 48, "moves": ["surf","ice-beam","thunder","perish-song"] },
          { "pokemonId": 473, "level": 48, "moves": ["blizzard","earthquake","ice-shard","stone-edge"] },
          { "pokemonId": 365, "level": 50, "moves": ["blizzard","surf","ice-ball","body-slam"] }
        ]}
      ]
    },
    {
      "id": "johto-clair", "name": "Clair", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Dragon",
      "rosters": [
        { "label": "Original (GSC)", "team": [
          { "pokemonId": 148, "level": 37, "moves": ["dragon-rage","thunder-wave","slam","hyper-beam"] },
          { "pokemonId": 148, "level": 37, "moves": ["dragon-rage","thunder-wave","slam","hyper-beam"] },
          { "pokemonId": 148, "level": 37, "moves": ["dragon-rage","thunder-wave","slam","hyper-beam"] },
          { "pokemonId": 147, "level": 35, "moves": ["dragon-rage","thunder-wave","slam","wrap"] },
          { "pokemonId": 147, "level": 35, "moves": ["dragon-rage","thunder-wave","wrap","leer"] },
          { "pokemonId": 230, "level": 40, "moves": ["surf","hyper-beam","dragon-breath","smokescreen"] }
        ]},
        { "label": "Rematch (HGSS)", "team": [
          { "pokemonId": 148, "level": 51, "moves": ["outrage","dragon-dance","thunder-wave","aqua-tail"] },
          { "pokemonId": 148, "level": 51, "moves": ["outrage","dragon-dance","thunder-wave","aqua-tail"] },
          { "pokemonId": 230, "level": 52, "moves": ["surf","outrage","dragon-dance","smokescreen"] },
          { "pokemonId": 373, "level": 54, "moves": ["outrage","dragon-claw","crunch","dragon-dance"] },
          { "pokemonId": 334, "level": 52, "moves": ["dragon-pulse","sky-attack","dragon-dance","roost"] },
          { "pokemonId": 149, "level": 55, "moves": ["outrage","thunder","fire-blast","extreme-speed"] }
        ]}
      ]
    },
    {
      "id": "johto-will", "name": "Will", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Psychic",
      "rosters": [{ "label": "HGSS", "team": [
        { "pokemonId": 178, "level": 40, "moves": ["psychic","confuse-ray","reflect","air-slash"] },
        { "pokemonId": 103, "level": 41, "moves": ["psychic","sleep-powder","egg-bomb","confusion"] },
        { "pokemonId": 80,  "level": 41, "moves": ["psychic","surf","amnesia","thunder-wave"] },
        { "pokemonId": 124, "level": 41, "moves": ["psychic","ice-beam","lovely-kiss","calm-mind"] },
        { "pokemonId": 178, "level": 42, "moves": ["psychic","confuse-ray","reflect","air-slash"] },
        { "pokemonId": 196, "level": 41, "moves": ["psychic","calm-mind","shadow-ball","morning-sun"] }
      ]}]
    },
    {
      "id": "johto-koga-e4", "name": "Koga", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Poison",
      "rosters": [{ "label": "HGSS", "team": [
        { "pokemonId": 168, "level": 40, "moves": ["poison-sting","spider-web","scary-face","night-shade"] },
        { "pokemonId": 49,  "level": 41, "moves": ["psychic","sludge-bomb","sleep-powder","leech-life"] },
        { "pokemonId": 205, "level": 43, "moves": ["explosion","toxic-spikes","rapid-spin","volt-switch"] },
        { "pokemonId": 89,  "level": 42, "moves": ["shadow-ball","gunk-shot","minimize","acid-armor"] },
        { "pokemonId": 169, "level": 44, "moves": ["sludge-bomb","cross-poison","air-slash","confuse-ray"] },
        { "pokemonId": 169, "level": 44, "moves": ["sludge-bomb","cross-poison","air-slash","mean-look"] }
      ]}]
    },
    {
      "id": "johto-bruno-e4", "name": "Bruno", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Fighting",
      "rosters": [{ "label": "HGSS", "team": [
        { "pokemonId": 237, "level": 42, "moves": ["close-combat","sucker-punch","rapid-spin","stone-edge"] },
        { "pokemonId": 106, "level": 42, "moves": ["high-jump-kick","blaze-kick","close-combat","rapid-spin"] },
        { "pokemonId": 107, "level": 42, "moves": ["fire-punch","ice-punch","thunder-punch","mach-punch"] },
        { "pokemonId": 95,  "level": 43, "moves": ["rock-tomb","bind","screech","rock-blast"] },
        { "pokemonId": 95,  "level": 43, "moves": ["rock-tomb","bind","screech","earthquake"] },
        { "pokemonId": 68,  "level": 46, "moves": ["dynamic-punch","earthquake","cross-chop","bullet-punch"] }
      ]}]
    },
    {
      "id": "johto-karen", "name": "Karen", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Dark",
      "rosters": [{ "label": "HGSS", "team": [
        { "pokemonId": 197, "level": 42, "moves": ["faint-attack","confuse-ray","quick-attack","mean-look"] },
        { "pokemonId": 45,  "level": 42, "moves": ["petal-dance","sleep-powder","moonlight","acid"] },
        { "pokemonId": 198, "level": 44, "moves": ["night-shade","haze","mean-look","foul-play"] },
        { "pokemonId": 94,  "level": 45, "moves": ["shadow-ball","hypnosis","dream-eater","destiny-bond"] },
        { "pokemonId": 229, "level": 47, "moves": ["flamethrower","crunch","nasty-plot","foul-play"] },
        { "pokemonId": 229, "level": 47, "moves": ["flamethrower","crunch","will-o-wisp","foul-play"] }
      ]}]
    },
    {
      "id": "johto-lance", "name": "Lance", "title": "Champion",
      "trainerClass": "CHAMPION", "typeSpecialty": "Dragon",
      "rosters": [{ "label": "HGSS", "team": [
        { "pokemonId": 130, "level": 44, "moves": ["hyper-beam","surf","ice-fang","dragon-dance"] },
        { "pokemonId": 6,   "level": 46, "moves": ["fire-blast","fly","dragon-rage","slash"] },
        { "pokemonId": 142, "level": 46, "moves": ["fly","ancientpower","bite","wing-attack"] },
        { "pokemonId": 149, "level": 47, "moves": ["outrage","thunder","extreme-speed","dragon-dance"] },
        { "pokemonId": 149, "level": 47, "moves": ["outrage","thunder","extreme-speed","hyper-beam"] },
        { "pokemonId": 149, "level": 50, "moves": ["outrage","thunder","fire-blast","extreme-speed"] }
      ]}]
    },
    {
      "id": "johto-silver", "name": "Silver", "title": "Rival",
      "trainerClass": "RIVAL", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "Final Battle (HGSS)", "team": [
        { "pokemonId": 215, "level": 38, "moves": ["slash","beat-up","icy-wind","screech"] },
        { "pokemonId": 169, "level": 38, "moves": ["bite","leech-life","supersonic","wing-attack"] },
        { "pokemonId": 82,  "level": 37, "moves": ["thunderbolt","thunder-wave","screech","swift"] },
        { "pokemonId": 94,  "level": 40, "moves": ["shadow-ball","hypnosis","dream-eater","thunderbolt"] },
        { "pokemonId": 65,  "level": 38, "moves": ["psychic","calm-mind","recover","thunder-punch"] },
        { "pokemonId": 160, "level": 42, "moves": ["surf","crunch","ice-punch","slash"] }
      ]}]
    }
  ]
}
```

- [ ] **Step 2: Validate JSON**

```bash
python3 -m json.tool app/src/main/assets/trainers.json > /dev/null && echo "JSON valid"
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/trainers.json
git commit -m "feat: add Johto region to trainers.json"
```

---
## Task 5: trainers.json — Hoenn

Append to the `"regions"` array in `app/src/main/assets/trainers.json`.

- [ ] **Step 1: Append Hoenn region data**

```json
{
  "name": "Hoenn",
  "trainers": [
    {
      "id": "hoenn-roxanne", "name": "Roxanne", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Rock",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 74,  "level": 14, "moves": ["tackle","defense-curl","mud-slap","rock-throw"] },
        { "pokemonId": 74,  "level": 14, "moves": ["tackle","defense-curl","mud-slap","rock-throw"] },
        { "pokemonId": 299, "level": 15, "moves": ["tackle","harden","rock-throw","block"] },
        { "pokemonId": 185, "level": 14, "moves": ["tackle","harden","rock-throw","block"] },
        { "pokemonId": 111, "level": 14, "moves": ["tackle","tail-whip","stomp","horn-attack"] },
        { "pokemonId": 299, "level": 16, "moves": ["tackle","harden","rock-blast","stealth-rock"] }
      ]}]
    },
    {
      "id": "hoenn-brawly", "name": "Brawly", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fighting",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 296, "level": 17, "moves": ["tackle","focus-energy","karate-chop","bulk-up"] },
        { "pokemonId": 66,  "level": 16, "moves": ["tackle","focus-energy","karate-chop","leer"] },
        { "pokemonId": 67,  "level": 17, "moves": ["tackle","focus-energy","karate-chop","leer"] },
        { "pokemonId": 56,  "level": 16, "moves": ["karate-chop","leer","focus-energy","seismic-toss"] },
        { "pokemonId": 307, "level": 17, "moves": ["bide","meditate","confusion","detect"] },
        { "pokemonId": 297, "level": 18, "moves": ["tackle","focus-energy","karate-chop","bulk-up"] }
      ]}]
    },
    {
      "id": "hoenn-wattson", "name": "Wattson", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Electric",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 81,  "level": 22, "moves": ["thundershock","thunder-wave","sonicboom","screech"] },
        { "pokemonId": 82,  "level": 22, "moves": ["thunderbolt","thunder-wave","screech","sonicboom"] },
        { "pokemonId": 100, "level": 23, "moves": ["spark","screech","sonicboom","thunder-wave"] },
        { "pokemonId": 101, "level": 23, "moves": ["thunderbolt","thunder-wave","screech","explosion"] },
        { "pokemonId": 309, "level": 22, "moves": ["spark","thunder-wave","howl","bite"] },
        { "pokemonId": 310, "level": 24, "moves": ["thunderbolt","charge-beam","quick-attack","roar"] }
      ]}]
    },
    {
      "id": "hoenn-flannery", "name": "Flannery", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fire",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 218, "level": 26, "moves": ["smog","ember","rock-throw","harden"] },
        { "pokemonId": 218, "level": 26, "moves": ["smog","ember","rock-throw","harden"] },
        { "pokemonId": 219, "level": 28, "moves": ["ember","rock-throw","smog","harden"] },
        { "pokemonId": 322, "level": 27, "moves": ["ember","growl","magnitude","take-down"] },
        { "pokemonId": 323, "level": 28, "moves": ["ember","magnitude","rock-slide","overheat"] },
        { "pokemonId": 324, "level": 29, "moves": ["overheat","fire-spin","body-slam","smog"] }
      ]}]
    },
    {
      "id": "hoenn-norman", "name": "Norman", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Normal",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 288, "level": 28, "moves": ["tackle","yawn","encore","slack-off"] },
        { "pokemonId": 288, "level": 28, "moves": ["tackle","yawn","encore","slack-off"] },
        { "pokemonId": 263, "level": 29, "moves": ["tackle","growl","tail-whip","hyper-voice"] },
        { "pokemonId": 264, "level": 30, "moves": ["tackle","growl","tail-whip","hyper-voice"] },
        { "pokemonId": 287, "level": 30, "moves": ["yawn","encore","slack-off","body-slam"] },
        { "pokemonId": 289, "level": 31, "moves": ["body-slam","yawn","bulk-up","slack-off"] }
      ]}]
    },
    {
      "id": "hoenn-winona", "name": "Winona", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Flying",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 278, "level": 31, "moves": ["water-gun","supersonic","wingattack","mist"] },
        { "pokemonId": 279, "level": 32, "moves": ["surf","supersonic","wingattack","protect"] },
        { "pokemonId": 333, "level": 33, "moves": ["peck","growl","astonish","sing"] },
        { "pokemonId": 334, "level": 33, "moves": ["dragon-breath","aerial-ace","mist","dragon-dance"] },
        { "pokemonId": 277, "level": 33, "moves": ["wing-attack","quick-attack","endeavor","aerial-ace"] },
        { "pokemonId": 227, "level": 35, "moves": ["steel-wing","aerial-ace","spikes","fly"] }
      ]}]
    },
    {
      "id": "hoenn-tate-liza", "name": "Tate & Liza", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Psychic",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 343, "level": 41, "moves": ["confusion","harden","cosmic-power","ancient-power"] },
        { "pokemonId": 344, "level": 42, "moves": ["psychic","harden","cosmic-power","ancient-power"] },
        { "pokemonId": 337, "level": 41, "moves": ["calm-mind","cosmic-power","psychic","ancient-power"] },
        { "pokemonId": 338, "level": 41, "moves": ["calm-mind","cosmic-power","psychic","ancient-power"] },
        { "pokemonId": 64,  "level": 41, "moves": ["psychic","recover","calm-mind","shadow-ball"] },
        { "pokemonId": 65,  "level": 42, "moves": ["psychic","recover","calm-mind","shadow-ball"] }
      ]}]
    },
    {
      "id": "hoenn-juan", "name": "Juan", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Water",
      "rosters": [{ "label": "E (Sootopolis)", "team": [
        { "pokemonId": 72,  "level": 41, "moves": ["wrap","constrict","poison-sting","bubblebeam"] },
        { "pokemonId": 73,  "level": 42, "moves": ["surf","poison-sting","barrier","acid"] },
        { "pokemonId": 340, "level": 43, "moves": ["surf","earthquake","amnesia","tickle"] },
        { "pokemonId": 119, "level": 43, "moves": ["surf","horn-attack","agility","horn-drill"] },
        { "pokemonId": 117, "level": 43, "moves": ["surf","smokescreen","dragon-breath","agility"] },
        { "pokemonId": 350, "level": 46, "moves": ["surf","ice-beam","recover","mirror-coat"] }
      ]}]
    },
    {
      "id": "hoenn-sidney", "name": "Sidney", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Dark",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 262, "level": 46, "moves": ["crunch","howl","sand-attack","shadow-ball"] },
        { "pokemonId": 275, "level": 48, "moves": ["crunch","fake-out","swords-dance","extrasensory"] },
        { "pokemonId": 332, "level": 46, "moves": ["needle-arm","faint-attack","sand-storm","destiny-bond"] },
        { "pokemonId": 342, "level": 48, "moves": ["crunch","surf","guillotine","swords-dance"] },
        { "pokemonId": 319, "level": 47, "moves": ["crunch","surf","ice-beam","agility"] },
        { "pokemonId": 359, "level": 49, "moves": ["crunch","shadow-ball","swords-dance","double-team"] }
      ]}]
    },
    {
      "id": "hoenn-phoebe", "name": "Phoebe", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Ghost",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 356, "level": 48, "moves": ["shadow-ball","will-o-wisp","calm-mind","confuse-ray"] },
        { "pokemonId": 354, "level": 49, "moves": ["shadow-ball","will-o-wisp","calm-mind","grudge"] },
        { "pokemonId": 302, "level": 50, "moves": ["shadow-ball","faint-attack","calm-mind","attract"] },
        { "pokemonId": 354, "level": 49, "moves": ["shadow-ball","will-o-wisp","grudge","spite"] },
        { "pokemonId": 353, "level": 49, "moves": ["shadow-ball","will-o-wisp","confuse-ray","spite"] },
        { "pokemonId": 356, "level": 51, "moves": ["shadow-ball","will-o-wisp","calm-mind","destiny-bond"] }
      ]}]
    },
    {
      "id": "hoenn-glacia", "name": "Glacia", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Ice",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 363, "level": 50, "moves": ["surf","ice-ball","encore","body-slam"] },
        { "pokemonId": 363, "level": 50, "moves": ["surf","ice-ball","encore","hail"] },
        { "pokemonId": 221, "level": 52, "moves": ["blizzard","earthquake","ice-shard","amnesia"] },
        { "pokemonId": 362, "level": 53, "moves": ["blizzard","body-slam","hail","light-screen"] },
        { "pokemonId": 87,  "level": 50, "moves": ["surf","ice-beam","aurora-beam","rest"] },
        { "pokemonId": 365, "level": 52, "moves": ["blizzard","surf","ice-ball","body-slam"] }
      ]}]
    },
    {
      "id": "hoenn-drake", "name": "Drake", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Dragon",
      "rosters": [{ "label": "RSE", "team": [
        { "pokemonId": 148, "level": 52, "moves": ["dragon-rage","thunder-wave","slam","hyper-beam"] },
        { "pokemonId": 372, "level": 52, "moves": ["dragon-breath","protect","ember","scary-face"] },
        { "pokemonId": 334, "level": 54, "moves": ["dragon-pulse","sky-attack","dragon-dance","roost"] },
        { "pokemonId": 330, "level": 53, "moves": ["dragon-claw","earthquake","crunch","flamethrower"] },
        { "pokemonId": 330, "level": 53, "moves": ["dragon-claw","earthquake","crunch","fly"] },
        { "pokemonId": 373, "level": 55, "moves": ["outrage","earthquake","crunch","dragon-dance"] }
      ]}]
    },
    {
      "id": "hoenn-steven", "name": "Steven", "title": "Champion",
      "trainerClass": "CHAMPION", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "RS", "team": [
        { "pokemonId": 227, "level": 57, "moves": ["steel-wing","drill-peck","spikes","fly"] },
        { "pokemonId": 344, "level": 55, "moves": ["psychic","ancient-power","cosmic-power","harden"] },
        { "pokemonId": 346, "level": 56, "moves": ["giga-drain","ancient-power","ingrain","confuse-ray"] },
        { "pokemonId": 348, "level": 56, "moves": ["slash","ancient-power","rock-blast","fury-cutter"] },
        { "pokemonId": 306, "level": 58, "moves": ["earthquake","iron-tail","rock-slide","dragon-claw"] },
        { "pokemonId": 376, "level": 58, "moves": ["meteor-mash","earthquake","psychic","agility"] }
      ]}]
    },
    {
      "id": "hoenn-may", "name": "May", "title": "Rival",
      "trainerClass": "RIVAL", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "Final Battle (Emerald)", "team": [
        { "pokemonId": 279, "level": 46, "moves": ["surf","air-slash","protect","fly"] },
        { "pokemonId": 310, "level": 47, "moves": ["thunderbolt","quick-attack","charge-beam","roar"] },
        { "pokemonId": 65,  "level": 47, "moves": ["psychic","calm-mind","recover","shadow-ball"] },
        { "pokemonId": 334, "level": 48, "moves": ["dragon-pulse","sky-attack","dragon-dance","aerial-ace"] },
        { "pokemonId": 350, "level": 46, "moves": ["surf","ice-beam","recover","mirror-coat"] },
        { "pokemonId": 254, "level": 50, "moves": ["leaf-blade","dragon-claw","slash","synthesis"] }
      ]}]
    }
  ]
}
```

- [ ] **Step 2: Validate and commit**

```bash
python3 -m json.tool app/src/main/assets/trainers.json > /dev/null && echo "JSON valid"
git add app/src/main/assets/trainers.json
git commit -m "feat: add Hoenn region to trainers.json"
```

---
## Task 6: trainers.json — Sinnoh

Append to the `"regions"` array in `app/src/main/assets/trainers.json`.

- [ ] **Step 1: Append Sinnoh region data**

```json
{
  "name": "Sinnoh",
  "trainers": [
    {
      "id": "sinnoh-roark", "name": "Roark", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Rock",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 74,  "level": 12, "moves": ["tackle","defense-curl","mud-slap","rock-throw"] },
        { "pokemonId": 95,  "level": 11, "moves": ["tackle","screech","bind","rock-throw"] },
        { "pokemonId": 185, "level": 12, "moves": ["tackle","harden","rock-throw","block"] },
        { "pokemonId": 111, "level": 11, "moves": ["tackle","tail-whip","stomp","horn-attack"] },
        { "pokemonId": 138, "level": 11, "moves": ["water-gun","withdraw","tackle","bite"] },
        { "pokemonId": 408, "level": 14, "moves": ["leer","headbutt","focus-energy","take-down"] }
      ]}]
    },
    {
      "id": "sinnoh-gardenia", "name": "Gardenia", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Grass",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 420, "level": 19, "moves": ["absorb","growl","helping-hand","magical-leaf"] },
        { "pokemonId": 182, "level": 20, "moves": ["magical-leaf","stun-spore","sleep-powder","synthesis"] },
        { "pokemonId": 189, "level": 20, "moves": ["mega-drain","stun-spore","cotton-spore","leech-seed"] },
        { "pokemonId": 315, "level": 19, "moves": ["mega-drain","leech-seed","stun-spore","magical-leaf"] },
        { "pokemonId": 253, "level": 20, "moves": ["absorb","quick-attack","pursuit","mega-drain"] },
        { "pokemonId": 407, "level": 22, "moves": ["mega-drain","magical-leaf","stun-spore","petal-dance"] }
      ]}]
    },
    {
      "id": "sinnoh-maylene", "name": "Maylene", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fighting",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 307, "level": 28, "moves": ["bide","meditate","confusion","force-palm"] },
        { "pokemonId": 67,  "level": 27, "moves": ["karate-chop","bulk-up","focus-energy","leer"] },
        { "pokemonId": 66,  "level": 28, "moves": ["karate-chop","leer","focus-energy","seismic-toss"] },
        { "pokemonId": 308, "level": 29, "moves": ["force-palm","meditate","calm-mind","detect"] },
        { "pokemonId": 56,  "level": 28, "moves": ["karate-chop","leer","focus-energy","seismic-toss"] },
        { "pokemonId": 448, "level": 30, "moves": ["force-palm","quick-attack","metal-claw","calm-mind"] }
      ]}]
    },
    {
      "id": "sinnoh-crasher-wake", "name": "Crasher Wake", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Water",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 195, "level": 33, "moves": ["surf","earthquake","yawn","slam"] },
        { "pokemonId": 55,  "level": 33, "moves": ["surf","ice-beam","psych-up","zen-headbutt"] },
        { "pokemonId": 340, "level": 34, "moves": ["surf","earthquake","amnesia","tickle"] },
        { "pokemonId": 130, "level": 34, "moves": ["surf","ice-fang","dragon-rage","bite"] },
        { "pokemonId": 60,  "level": 33, "moves": ["surf","bubble-beam","hypnosis","double-slap"] },
        { "pokemonId": 419, "level": 37, "moves": ["surf","crunch","aqua-jet","swift"] }
      ]}]
    },
    {
      "id": "sinnoh-fantina", "name": "Fantina", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ghost",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 93,  "level": 32, "moves": ["shadow-ball","hypnosis","payback","confuse-ray"] },
        { "pokemonId": 200, "level": 32, "moves": ["shadow-ball","will-o-wisp","psybeam","confuse-ray"] },
        { "pokemonId": 354, "level": 34, "moves": ["shadow-ball","will-o-wisp","calm-mind","shadow-sneak"] },
        { "pokemonId": 353, "level": 32, "moves": ["shadow-ball","will-o-wisp","confuse-ray","spite"] },
        { "pokemonId": 425, "level": 34, "moves": ["shadow-ball","calm-mind","gust","minimize"] },
        { "pokemonId": 429, "level": 36, "moves": ["shadow-ball","calm-mind","will-o-wisp","mystical-fire"] }
      ]}]
    },
    {
      "id": "sinnoh-byron", "name": "Byron", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Steel",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 227, "level": 36, "moves": ["steel-wing","aerial-ace","spikes","swift"] },
        { "pokemonId": 95,  "level": 36, "moves": ["iron-tail","screech","bind","rock-throw"] },
        { "pokemonId": 208, "level": 37, "moves": ["iron-tail","earthquake","crunch","rock-blast"] },
        { "pokemonId": 82,  "level": 36, "moves": ["flash-cannon","thunder-wave","screech","tri-attack"] },
        { "pokemonId": 205, "level": 36, "moves": ["gyro-ball","explosion","toxic-spikes","rapid-spin"] },
        { "pokemonId": 411, "level": 40, "moves": ["iron-defense","metal-burst","iron-head","stealth-rock"] }
      ]}]
    },
    {
      "id": "sinnoh-candice", "name": "Candice", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ice",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 362, "level": 40, "moves": ["blizzard","hail","shadow-ball","ice-shard"] },
        { "pokemonId": 460, "level": 40, "moves": ["blizzard","ice-punch","leech-seed","wood-hammer"] },
        { "pokemonId": 220, "level": 40, "moves": ["powder-snow","mud-bomb","take-down","ice-shard"] },
        { "pokemonId": 221, "level": 41, "moves": ["blizzard","earthquake","ice-shard","mud-bomb"] },
        { "pokemonId": 459, "level": 40, "moves": ["blizzard","ice-shard","leech-seed","wood-hammer"] },
        { "pokemonId": 478, "level": 44, "moves": ["blizzard","shadow-ball","will-o-wisp","ice-shard"] }
      ]}]
    },
    {
      "id": "sinnoh-volkner", "name": "Volkner", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Electric",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 26,  "level": 46, "moves": ["thunderbolt","volt-tackle","quick-attack","iron-tail"] },
        { "pokemonId": 101, "level": 46, "moves": ["thunderbolt","explosion","thunder-wave","signal-beam"] },
        { "pokemonId": 135, "level": 47, "moves": ["thunderbolt","shadow-ball","quick-attack","thunder-wave"] },
        { "pokemonId": 82,  "level": 46, "moves": ["flash-cannon","thunderbolt","thunder-wave","tri-attack"] },
        { "pokemonId": 466, "level": 48, "moves": ["thunder-punch","fire-punch","ice-punch","thunder-wave"] },
        { "pokemonId": 405, "level": 50, "moves": ["thunderbolt","crunch","ice-fang","thunder-fang"] }
      ]}]
    },
    {
      "id": "sinnoh-aaron", "name": "Aaron", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Bug",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 269, "level": 53, "moves": ["bug-buzz","psychic","quiver-dance","stun-spore"] },
        { "pokemonId": 267, "level": 53, "moves": ["bug-buzz","psychic","quiver-dance","silver-wind"] },
        { "pokemonId": 416, "level": 54, "moves": ["attack-order","defend-order","heal-order","power-gem"] },
        { "pokemonId": 214, "level": 54, "moves": ["megahorn","close-combat","stone-edge","night-slash"] },
        { "pokemonId": 452, "level": 57, "moves": ["cross-poison","night-slash","crunch","swords-dance"] },
        { "pokemonId": 212, "level": 55, "moves": ["x-scissor","iron-head","swords-dance","night-slash"] }
      ]}]
    },
    {
      "id": "sinnoh-bertha", "name": "Bertha", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Ground",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 340, "level": 55, "moves": ["surf","earthquake","amnesia","tickle"] },
        { "pokemonId": 76,  "level": 56, "moves": ["earthquake","rock-blast","explosion","stone-edge"] },
        { "pokemonId": 185, "level": 56, "moves": ["earthquake","rock-slide","stone-edge","block"] },
        { "pokemonId": 472, "level": 59, "moves": ["earthquake","x-scissor","stone-edge","swords-dance"] },
        { "pokemonId": 232, "level": 57, "moves": ["earthquake","hyper-voice","rapid-spin","thunder-fang"] },
        { "pokemonId": 450, "level": 59, "moves": ["earthquake","crunch","stone-edge","slack-off"] }
      ]}]
    },
    {
      "id": "sinnoh-flint", "name": "Flint", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Fire",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 78,  "level": 53, "moves": ["flamethrower","agility","bounce","fire-blast"] },
        { "pokemonId": 59,  "level": 55, "moves": ["flamethrower","extreme-speed","dragon-pulse","roar"] },
        { "pokemonId": 467, "level": 55, "moves": ["flamethrower","thunder-punch","fire-blast","screech"] },
        { "pokemonId": 136, "level": 55, "moves": ["flamethrower","shadow-ball","sand-attack","quick-attack"] },
        { "pokemonId": 392, "level": 58, "moves": ["flamethrower","close-combat","earthquake","u-turn"] },
        { "pokemonId": 392, "level": 61, "moves": ["fire-blast","close-combat","earthquake","thunderpunch"] }
      ]}]
    },
    {
      "id": "sinnoh-lucian", "name": "Lucian", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Psychic",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 122, "level": 55, "moves": ["psychic","reflect","light-screen","calm-mind"] },
        { "pokemonId": 437, "level": 56, "moves": ["psychic","iron-defense","calm-mind","future-sight"] },
        { "pokemonId": 308, "level": 56, "moves": ["psychic","calm-mind","detect","hi-jump-kick"] },
        { "pokemonId": 196, "level": 58, "moves": ["psychic","calm-mind","shadow-ball","morning-sun"] },
        { "pokemonId": 475, "level": 59, "moves": ["psychic","leaf-blade","calm-mind","close-combat"] },
        { "pokemonId": 65,  "level": 60, "moves": ["psychic","recover","calm-mind","shadow-ball"] }
      ]}]
    },
    {
      "id": "sinnoh-cynthia", "name": "Cynthia", "title": "Champion",
      "trainerClass": "CHAMPION", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "DPPt", "team": [
        { "pokemonId": 442, "level": 61, "moves": ["shadow-ball","will-o-wisp","calm-mind","silver-wind"] },
        { "pokemonId": 423, "level": 60, "moves": ["surf","earth-power","ice-beam","stone-edge"] },
        { "pokemonId": 350, "level": 60, "moves": ["surf","ice-beam","recover","mirror-coat"] },
        { "pokemonId": 468, "level": 60, "moves": ["air-slash","aura-sphere","fire-blast","thunder"] },
        { "pokemonId": 448, "level": 62, "moves": ["aura-sphere","dragon-pulse","calm-mind","swords-dance"] },
        { "pokemonId": 445, "level": 66, "moves": ["outrage","earthquake","crunch","swords-dance"] }
      ]}]
    },
    {
      "id": "sinnoh-barry", "name": "Barry", "title": "Rival",
      "trainerClass": "RIVAL", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "Final Battle (DPPt)", "team": [
        { "pokemonId": 398, "level": 58, "moves": ["close-combat","fly","u-turn","final-gambit"] },
        { "pokemonId": 430, "level": 59, "moves": ["pursuit","foul-play","sucker-punch","night-slash"] },
        { "pokemonId": 405, "level": 60, "moves": ["thunderbolt","crunch","ice-fang","thunder-fang"] },
        { "pokemonId": 392, "level": 61, "moves": ["flamethrower","close-combat","earthquake","u-turn"] },
        { "pokemonId": 419, "level": 60, "moves": ["surf","crunch","aqua-jet","ice-fang"] },
        { "pokemonId": 389, "level": 63, "moves": ["leaf-storm","earthquake","crunch","synthesis"] }
      ]}]
    }
  ]
}
```

- [ ] **Step 2: Validate and commit**

```bash
python3 -m json.tool app/src/main/assets/trainers.json > /dev/null && echo "JSON valid"
git add app/src/main/assets/trainers.json
git commit -m "feat: add Sinnoh region to trainers.json"
```

---
## Task 7: trainers.json — Unova

Append to the `"regions"` array in `app/src/main/assets/trainers.json`.

- [ ] **Step 1: Append Unova region data**

```json
{
  "name": "Unova",
  "trainers": [
    {
      "id": "unova-cilan", "name": "Cilan", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Grass",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 511, "level": 12, "moves": ["vine-whip","leer","taunt","fury-swipes"] },
        { "pokemonId": 43,  "level": 12, "moves": ["absorb","poisonpowder","sleep-powder","acid"] },
        { "pokemonId": 69,  "level": 12, "moves": ["vine-whip","sleep-powder","wrap","acid"] },
        { "pokemonId": 420, "level": 12, "moves": ["absorb","growl","helping-hand","magical-leaf"] },
        { "pokemonId": 191, "level": 12, "moves": ["absorb","growth","ingrain","grass-whistle"] },
        { "pokemonId": 511, "level": 14, "moves": ["vine-whip","leer","taunt","leech-seed"] }
      ]}]
    },
    {
      "id": "unova-chili", "name": "Chili", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fire",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 513, "level": 12, "moves": ["incinerate","leer","taunt","fury-swipes"] },
        { "pokemonId": 58,  "level": 12, "moves": ["ember","bite","leer","odor-sleuth"] },
        { "pokemonId": 77,  "level": 12, "moves": ["ember","tail-whip","growl","stomp"] },
        { "pokemonId": 37,  "level": 12, "moves": ["ember","tail-whip","growl","quick-attack"] },
        { "pokemonId": 126, "level": 12, "moves": ["ember","smog","leer","confuse-ray"] },
        { "pokemonId": 513, "level": 14, "moves": ["incinerate","leer","taunt","nasty-plot"] }
      ]}]
    },
    {
      "id": "unova-cress", "name": "Cress", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Water",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 515, "level": 12, "moves": ["water-gun","leer","taunt","fury-swipes"] },
        { "pokemonId": 60,  "level": 12, "moves": ["bubble","hypnosis","water-sport","water-gun"] },
        { "pokemonId": 116, "level": 12, "moves": ["water-gun","smokescreen","leer","bubble"] },
        { "pokemonId": 72,  "level": 12, "moves": ["water-gun","constrict","poison-sting","wrap"] },
        { "pokemonId": 86,  "level": 12, "moves": ["headbutt","growl","water-gun","rest"] },
        { "pokemonId": 515, "level": 14, "moves": ["water-gun","leer","taunt","scald"] }
      ]}]
    },
    {
      "id": "unova-lenora", "name": "Lenora", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Normal",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 504, "level": 18, "moves": ["tackle","leer","bite","hypnosis"] },
        { "pokemonId": 506, "level": 18, "moves": ["tackle","leer","bite","work-up"] },
        { "pokemonId": 39,  "level": 18, "moves": ["pound","sing","defense-curl","rollout"] },
        { "pokemonId": 234, "level": 19, "moves": ["tackle","growl","psybeam","confuse-ray"] },
        { "pokemonId": 241, "level": 19, "moves": ["tackle","stomp","milk-drink","rollout"] },
        { "pokemonId": 507, "level": 20, "moves": ["tackle","leer","bite","retaliate"] }
      ]}]
    },
    {
      "id": "unova-burgh", "name": "Burgh", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Bug",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 540, "level": 21, "moves": ["string-shot","bug-bite","razor-leaf","protect"] },
        { "pokemonId": 557, "level": 21, "moves": ["rock-blast","smack-down","bug-bite","withdraw"] },
        { "pokemonId": 123, "level": 22, "moves": ["quick-attack","leer","x-scissor","slash"] },
        { "pokemonId": 214, "level": 22, "moves": ["tackle","horn-attack","bug-bite","leer"] },
        { "pokemonId": 165, "level": 21, "moves": ["comet-punch","string-shot","swift","bug-bite"] },
        { "pokemonId": 542, "level": 23, "moves": ["x-scissor","leaf-blade","swords-dance","slash"] }
      ]}]
    },
    {
      "id": "unova-elesa", "name": "Elesa", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Electric",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 587, "level": 25, "moves": ["volt-switch","quick-attack","aerial-ace","spark"] },
        { "pokemonId": 587, "level": 25, "moves": ["volt-switch","quick-attack","aerial-ace","spark"] },
        { "pokemonId": 100, "level": 25, "moves": ["spark","rollout","sonicboom","thunder-wave"] },
        { "pokemonId": 81,  "level": 25, "moves": ["thundershock","thunder-wave","sonicboom","screech"] },
        { "pokemonId": 239, "level": 25, "moves": ["thunder-punch","quick-attack","leer","swift"] },
        { "pokemonId": 523, "level": 27, "moves": ["flame-charge","volt-switch","quick-attack","stomp"] }
      ]}]
    },
    {
      "id": "unova-clay", "name": "Clay", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ground",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 551, "level": 29, "moves": ["bite","sand-attack","swagger","mud-slap"] },
        { "pokemonId": 536, "level": 28, "moves": ["surf","muddy-water","mud-shot","mud-bomb"] },
        { "pokemonId": 551, "level": 29, "moves": ["bite","sand-attack","swagger","mud-slap"] },
        { "pokemonId": 231, "level": 29, "moves": ["stomp","growl","take-down","mud-slap"] },
        { "pokemonId": 339, "level": 29, "moves": ["mud-bomb","surf","water-gun","amnesia"] },
        { "pokemonId": 530, "level": 31, "moves": ["earthquake","x-scissor","slash","rock-slide"] }
      ]}]
    },
    {
      "id": "unova-skyla", "name": "Skyla", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Flying",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 528, "level": 33, "moves": ["air-cutter","confusion","attract","heart-stamp"] },
        { "pokemonId": 333, "level": 33, "moves": ["sing","take-down","peck","growl"] },
        { "pokemonId": 278, "level": 33, "moves": ["water-gun","supersonic","wing-attack","mist"] },
        { "pokemonId": 279, "level": 34, "moves": ["surf","wing-attack","supersonic","protect"] },
        { "pokemonId": 169, "level": 33, "moves": ["confuse-ray","supersonic","wing-attack","air-cutter"] },
        { "pokemonId": 521, "level": 35, "moves": ["aerial-ace","air-slash","work-up","leer"] }
      ]}]
    },
    {
      "id": "unova-brycen", "name": "Brycen", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ice",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 582, "level": 37, "moves": ["icicle-spear","astonish","iron-defense","ice-shard"] },
        { "pokemonId": 583, "level": 37, "moves": ["blizzard","icicle-spear","astonish","iron-defense"] },
        { "pokemonId": 459, "level": 37, "moves": ["powder-snow","icy-wind","wood-hammer","ice-shard"] },
        { "pokemonId": 220, "level": 37, "moves": ["powder-snow","mud-bomb","take-down","ice-shard"] },
        { "pokemonId": 362, "level": 38, "moves": ["blizzard","ice-shard","hail","light-screen"] },
        { "pokemonId": 614, "level": 39, "moves": ["blizzard","bulk-up","slash","sheer-cold"] }
      ]}]
    },
    {
      "id": "unova-drayden", "name": "Drayden", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Dragon",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 611, "level": 41, "moves": ["slash","dragon-rage","slash","work-up"] },
        { "pokemonId": 611, "level": 41, "moves": ["slash","dragon-rage","dual-chop","work-up"] },
        { "pokemonId": 147, "level": 42, "moves": ["dragon-rage","wrap","slam","thunder-wave"] },
        { "pokemonId": 148, "level": 43, "moves": ["dragon-rage","aqua-tail","slam","thunder-wave"] },
        { "pokemonId": 330, "level": 43, "moves": ["dragon-claw","earthquake","crunch","fly"] },
        { "pokemonId": 612, "level": 43, "moves": ["dragon-dance","dragon-claw","slash","dual-chop"] }
      ]}]
    },
    {
      "id": "unova-shauntal", "name": "Shauntal", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Ghost",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 563, "level": 48, "moves": ["shadow-ball","will-o-wisp","calm-mind","hex"] },
        { "pokemonId": 593, "level": 48, "moves": ["shadow-ball","surf","will-o-wisp","hex"] },
        { "pokemonId": 429, "level": 50, "moves": ["shadow-ball","calm-mind","will-o-wisp","mystical-fire"] },
        { "pokemonId": 623, "level": 50, "moves": ["shadow-punch","earthquake","heavy-slam","bulk-up"] },
        { "pokemonId": 356, "level": 52, "moves": ["shadow-ball","will-o-wisp","calm-mind","confuse-ray"] },
        { "pokemonId": 609, "level": 52, "moves": ["shadow-ball","flame-charge","will-o-wisp","calm-mind"] }
      ]}]
    },
    {
      "id": "unova-marshal", "name": "Marshal", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Fighting",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 538, "level": 48, "moves": ["bulk-up","storm-throw","payback","rock-slide"] },
        { "pokemonId": 539, "level": 48, "moves": ["bulk-up","karate-chop","quick-guard","rock-slide"] },
        { "pokemonId": 107, "level": 50, "moves": ["fire-punch","ice-punch","thunder-punch","mach-punch"] },
        { "pokemonId": 106, "level": 50, "moves": ["high-jump-kick","blaze-kick","close-combat","rapid-spin"] },
        { "pokemonId": 237, "level": 52, "moves": ["close-combat","stone-edge","sucker-punch","rapid-spin"] },
        { "pokemonId": 534, "level": 52, "moves": ["dynamic-punch","stone-edge","bulk-up","payback"] }
      ]}]
    },
    {
      "id": "unova-grimsley", "name": "Grimsley", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Dark",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 510, "level": 48, "moves": ["night-slash","fake-out","attract","thunder-wave"] },
        { "pokemonId": 560, "level": 48, "moves": ["crunch","high-jump-kick","dragon-dance","shed-skin"] },
        { "pokemonId": 430, "level": 50, "moves": ["foul-play","sucker-punch","pursuit","night-slash"] },
        { "pokemonId": 625, "level": 52, "moves": ["night-slash","iron-head","swords-dance","sucker-punch"] },
        { "pokemonId": 229, "level": 50, "moves": ["crunch","flamethrower","nasty-plot","foul-play"] },
        { "pokemonId": 553, "level": 52, "moves": ["crunch","earthquake","dragon-claw","outrage"] }
      ]}]
    },
    {
      "id": "unova-caitlin", "name": "Caitlin", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Psychic",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 518, "level": 48, "moves": ["psychic","hypnosis","dream-eater","calm-mind"] },
        { "pokemonId": 576, "level": 48, "moves": ["psychic","calm-mind","thunderbolt","shadow-ball"] },
        { "pokemonId": 561, "level": 50, "moves": ["psychic","air-slash","ice-beam","calm-mind"] },
        { "pokemonId": 282, "level": 50, "moves": ["psychic","calm-mind","shadow-ball","moonblast"] },
        { "pokemonId": 196, "level": 52, "moves": ["psychic","calm-mind","shadow-ball","morning-sun"] },
        { "pokemonId": 579, "level": 52, "moves": ["psychic","calm-mind","thunder","focus-blast"] }
      ]}]
    },
    {
      "id": "unova-alder", "name": "Alder", "title": "Champion",
      "trainerClass": "CHAMPION", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 617, "level": 54, "moves": ["bug-buzz","rock-slide","substitute","baton-pass"] },
        { "pokemonId": 626, "level": 56, "moves": ["head-charge","megahorn","earthquake","stone-edge"] },
        { "pokemonId": 621, "level": 56, "moves": ["dragon-claw","crunch","thunder-punch","fire-punch"] },
        { "pokemonId": 584, "level": 56, "moves": ["blizzard","autotomize","ice-shard","freeze-dry"] },
        { "pokemonId": 589, "level": 56, "moves": ["x-scissor","iron-head","aerial-ace","swords-dance"] },
        { "pokemonId": 637, "level": 58, "moves": ["bug-buzz","fire-blast","quiver-dance","roost"] }
      ]}]
    },
    {
      "id": "unova-cheren", "name": "Cheren", "title": "Rival",
      "trainerClass": "RIVAL", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "Final Battle (BW)", "team": [
        { "pokemonId": 504, "level": 50, "moves": ["hypnosis","bite","super-fang","retaliate"] },
        { "pokemonId": 510, "level": 51, "moves": ["night-slash","fake-out","attract","thunder-wave"] },
        { "pokemonId": 521, "level": 51, "moves": ["aerial-ace","air-slash","work-up","leer"] },
        { "pokemonId": 579, "level": 52, "moves": ["psychic","calm-mind","thunder","focus-blast"] },
        { "pokemonId": 530, "level": 52, "moves": ["earthquake","x-scissor","slash","rock-slide"] },
        { "pokemonId": 500, "level": 53, "moves": ["flare-blitz","head-smash","superpower","wild-charge"] }
      ]}]
    }
  ]
}
```

- [ ] **Step 2: Validate and commit**

```bash
python3 -m json.tool app/src/main/assets/trainers.json > /dev/null && echo "JSON valid"
git add app/src/main/assets/trainers.json
git commit -m "feat: add Unova region to trainers.json"
```

---
## Task 8: trainers.json — Kalos

Append to the `"regions"` array in `app/src/main/assets/trainers.json`.

- [ ] **Step 1: Append Kalos region data**

```json
{
  "name": "Kalos",
  "trainers": [
    {
      "id": "kalos-viola", "name": "Viola", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Bug",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 283, "level": 10, "moves": ["bubble","quick-attack","water-sport","stun-spore"] },
        { "pokemonId": 165, "level": 10, "moves": ["string-shot","bug-bite","comet-punch","swift"] },
        { "pokemonId": 284, "level": 11, "moves": ["bubble","quick-attack","water-sport","stun-spore"] },
        { "pokemonId": 11,  "level": 11, "moves": ["tackle","string-shot","harden","bug-bite"] },
        { "pokemonId": 588, "level": 11, "moves": ["vice-grip","string-shot","bug-bite","bide"] },
        { "pokemonId": 666, "level": 12, "moves": ["tackle","string-shot","stun-spore","silver-wind"] }
      ]}]
    },
    {
      "id": "kalos-grant", "name": "Grant", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Rock",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 95,  "level": 25, "moves": ["rock-throw","bind","screech","dragon-breath"] },
        { "pokemonId": 185, "level": 25, "moves": ["rock-throw","harden","block","mimic"] },
        { "pokemonId": 222, "level": 25, "moves": ["tackle","harden","water-gun","ancient-power"] },
        { "pokemonId": 138, "level": 25, "moves": ["water-gun","bite","ancient-power","withdraw"] },
        { "pokemonId": 140, "level": 25, "moves": ["scratch","harden","rock-blast","ancient-power"] },
        { "pokemonId": 698, "level": 28, "moves": ["icy-wind","ancient-power","rock-tomb","aurora-beam"] }
      ]}]
    },
    {
      "id": "kalos-korrina", "name": "Korrina", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fighting",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 619, "level": 29, "moves": ["hi-jump-kick","u-turn","acrobatics","drain-punch"] },
        { "pokemonId": 67,  "level": 28, "moves": ["karate-chop","bulk-up","leer","rock-smash"] },
        { "pokemonId": 307, "level": 28, "moves": ["force-palm","calm-mind","confusion","detect"] },
        { "pokemonId": 308, "level": 29, "moves": ["force-palm","calm-mind","detect","hi-jump-kick"] },
        { "pokemonId": 448, "level": 30, "moves": ["aura-sphere","quick-attack","metal-claw","bone-rush"] },
        { "pokemonId": 448, "level": 32, "moves": ["aura-sphere","close-combat","quick-attack","metal-claw"] }
      ]}]
    },
    {
      "id": "kalos-ramos", "name": "Ramos", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Grass",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 187, "level": 30, "moves": ["poison-powder","sleep-powder","leech-seed","mega-drain"] },
        { "pokemonId": 188, "level": 31, "moves": ["poison-powder","sleep-powder","leech-seed","mega-drain"] },
        { "pokemonId": 114, "level": 30, "moves": ["bind","sleep-powder","mega-drain","ancient-power"] },
        { "pokemonId": 315, "level": 30, "moves": ["magical-leaf","leech-seed","stun-spore","petal-blizzard"] },
        { "pokemonId": 673, "level": 31, "moves": ["vine-whip","growl","synthesis","grass-knot"] },
        { "pokemonId": 189, "level": 34, "moves": ["petal-dance","sleep-powder","leech-seed","cotton-guard"] }
      ]}]
    },
    {
      "id": "kalos-clemont", "name": "Clemont", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Electric",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 82,  "level": 35, "moves": ["thunderbolt","tri-attack","thunder-wave","screech"] },
        { "pokemonId": 587, "level": 35, "moves": ["volt-switch","aerial-ace","acrobatics","spark"] },
        { "pokemonId": 101, "level": 35, "moves": ["thunderbolt","explosion","thunder-wave","charge-beam"] },
        { "pokemonId": 239, "level": 35, "moves": ["thunder-punch","quick-attack","meditate","light-screen"] },
        { "pokemonId": 310, "level": 36, "moves": ["thunderbolt","charge-beam","quick-attack","roar"] },
        { "pokemonId": 695, "level": 37, "moves": ["thunderbolt","dragon-pulse","charge","agility"] }
      ]}]
    },
    {
      "id": "kalos-valerie", "name": "Valerie", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fairy",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 122, "level": 38, "moves": ["psychic","moonblast","light-screen","calm-mind"] },
        { "pokemonId": 303, "level": 38, "moves": ["play-rough","iron-head","fairy-wind","crunch"] },
        { "pokemonId": 176, "level": 39, "moves": ["moonblast","ancient-power","metronome","growl"] },
        { "pokemonId": 468, "level": 40, "moves": ["moonblast","air-slash","aura-sphere","ancient-power"] },
        { "pokemonId": 35,  "level": 39, "moves": ["moonblast","doubleslap","sing","minimize"] },
        { "pokemonId": 700, "level": 42, "moves": ["moonblast","psychic","calm-mind","wish"] }
      ]}]
    },
    {
      "id": "kalos-olympia", "name": "Olympia", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Psychic",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 561, "level": 44, "moves": ["psychic","air-slash","cosmic-power","ice-beam"] },
        { "pokemonId": 677, "level": 44, "moves": ["psychic","calm-mind","disarming-voice","iron-tail"] },
        { "pokemonId": 199, "level": 44, "moves": ["psychic","surf","calm-mind","thunder-wave"] },
        { "pokemonId": 576, "level": 45, "moves": ["psychic","calm-mind","thunderbolt","shadow-ball"] },
        { "pokemonId": 282, "level": 45, "moves": ["psychic","moonblast","calm-mind","shadow-ball"] },
        { "pokemonId": 678, "level": 48, "moves": ["psychic","calm-mind","shadow-ball","moonblast"] }
      ]}]
    },
    {
      "id": "kalos-wulfric", "name": "Wulfric", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ice",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 362, "level": 56, "moves": ["blizzard","ice-shard","hail","light-screen"] },
        { "pokemonId": 614, "level": 55, "moves": ["blizzard","bulk-up","slash","sheer-cold"] },
        { "pokemonId": 615, "level": 55, "moves": ["blizzard","freeze-dry","rapid-spin","light-screen"] },
        { "pokemonId": 460, "level": 56, "moves": ["blizzard","wood-hammer","ice-punch","leech-seed"] },
        { "pokemonId": 91,  "level": 56, "moves": ["blizzard","spikes","explosion","ice-shard"] },
        { "pokemonId": 471, "level": 59, "moves": ["blizzard","ice-shard","shadow-ball","quick-attack"] }
      ]}]
    },
    {
      "id": "kalos-malva", "name": "Malva", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Fire",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 668, "level": 63, "moves": ["flamethrower","noble-roar","hyper-voice","dark-pulse"] },
        { "pokemonId": 324, "level": 63, "moves": ["overheat","earth-power","stealth-rock","will-o-wisp"] },
        { "pokemonId": 663, "level": 65, "moves": ["flare-blitz","brave-bird","roost","swords-dance"] },
        { "pokemonId": 609, "level": 65, "moves": ["fire-blast","shadow-ball","will-o-wisp","calm-mind"] },
        { "pokemonId": 136, "level": 65, "moves": ["flamethrower","quick-attack","shadow-ball","will-o-wisp"] },
        { "pokemonId": 59,  "level": 65, "moves": ["flamethrower","extreme-speed","wild-charge","close-combat"] }
      ]}]
    },
    {
      "id": "kalos-siebold", "name": "Siebold", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Water",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 340, "level": 63, "moves": ["surf","earthquake","amnesia","yawn"] },
        { "pokemonId": 91,  "level": 63, "moves": ["surf","blizzard","spikes","explosion"] },
        { "pokemonId": 693, "level": 65, "moves": ["dragon-pulse","surf","sludge-bomb","shadow-ball"] },
        { "pokemonId": 689, "level": 65, "moves": ["surf","rock-slide","cross-chop","stone-edge"] },
        { "pokemonId": 121, "level": 65, "moves": ["surf","thunderbolt","ice-beam","recover"] },
        { "pokemonId": 131, "level": 66, "moves": ["surf","blizzard","thunder","confuse-ray"] }
      ]}]
    },
    {
      "id": "kalos-wikstrom", "name": "Wikstrom", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Steel",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 227, "level": 63, "moves": ["steel-wing","aerial-ace","spikes","iron-head"] },
        { "pokemonId": 227, "level": 63, "moves": ["iron-head","aerial-ace","spikes","brave-bird"] },
        { "pokemonId": 212, "level": 65, "moves": ["x-scissor","iron-head","swords-dance","bullet-punch"] },
        { "pokemonId": 306, "level": 65, "moves": ["iron-head","earthquake","rock-slide","heavy-slam"] },
        { "pokemonId": 208, "level": 65, "moves": ["iron-tail","earthquake","crunch","stone-edge"] },
        { "pokemonId": 681, "level": 66, "moves": ["shadow-ball","iron-head","swords-dance","king-shield"] }
      ]}]
    },
    {
      "id": "kalos-drasna", "name": "Drasna", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Dragon",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 334, "level": 63, "moves": ["dragon-pulse","sky-attack","dragon-dance","roost"] },
        { "pokemonId": 691, "level": 65, "moves": ["dragon-pulse","surf","sludge-bomb","shadow-ball"] },
        { "pokemonId": 621, "level": 65, "moves": ["dragon-claw","crunch","thunder-punch","fire-punch"] },
        { "pokemonId": 715, "level": 65, "moves": ["dragon-pulse","air-slash","roost","boomburst"] },
        { "pokemonId": 330, "level": 65, "moves": ["dragon-claw","earthquake","crunch","fly"] },
        { "pokemonId": 149, "level": 66, "moves": ["outrage","thunder","fire-blast","extreme-speed"] }
      ]}]
    },
    {
      "id": "kalos-diantha", "name": "Diantha", "title": "Champion",
      "trainerClass": "CHAMPION", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 701, "level": 64, "moves": ["high-jump-kick","aerial-ace","swords-dance","feather-dance"] },
        { "pokemonId": 697, "level": 65, "moves": ["head-smash","dragon-claw","fire-fang","crunch"] },
        { "pokemonId": 699, "level": 65, "moves": ["blizzard","ancient-power","aurora-beam","encore"] },
        { "pokemonId": 711, "level": 66, "moves": ["shadow-ball","trick-or-treat","leech-seed","phantom-force"] },
        { "pokemonId": 706, "level": 66, "moves": ["dragon-pulse","fire-blast","sludge-wave","focus-blast"] },
        { "pokemonId": 282, "level": 68, "moves": ["moonblast","psychic","shadow-ball","calm-mind"] }
      ]}]
    },
    {
      "id": "kalos-serena", "name": "Serena", "title": "Rival",
      "trainerClass": "RIVAL", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "Final Battle (XY)", "team": [
        { "pokemonId": 279, "level": 62, "moves": ["surf","air-slash","protect","fly"] },
        { "pokemonId": 700, "level": 63, "moves": ["moonblast","psychic","calm-mind","wish"] },
        { "pokemonId": 663, "level": 63, "moves": ["flare-blitz","brave-bird","roost","swords-dance"] },
        { "pokemonId": 660, "level": 63, "moves": ["return","sucker-punch","swords-dance","quick-attack"] },
        { "pokemonId": 121, "level": 64, "moves": ["surf","thunderbolt","ice-beam","recover"] },
        { "pokemonId": 655, "level": 65, "moves": ["fire-blast","moonblast","psychic","calm-mind"] }
      ]}]
    }
  ]
}
```

- [ ] **Step 2: Validate and commit**

```bash
python3 -m json.tool app/src/main/assets/trainers.json > /dev/null && echo "JSON valid"
git add app/src/main/assets/trainers.json
git commit -m "feat: add Kalos region to trainers.json"
```

---
## Task 9: trainers.json — Alola

Append to the `"regions"` array in `app/src/main/assets/trainers.json`.

Note: Alola uses Island Kahunas instead of gym leaders; they are tagged `GYM_LEADER` for grouping consistency in the UI.

- [ ] **Step 1: Append Alola region data**

```json
{
  "name": "Alola",
  "trainers": [
    {
      "id": "alola-hala", "name": "Hala", "title": "Kahuna",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fighting",
      "rosters": [{ "label": "SM", "team": [
        { "pokemonId": 296, "level": 26, "moves": ["arm-thrust","fake-out","bullet-punch","knock-off"] },
        { "pokemonId": 56,  "level": 26, "moves": ["karate-chop","leer","focus-energy","seismic-toss"] },
        { "pokemonId": 62,  "level": 27, "moves": ["dynamic-punch","hypnosis","waterfall","body-slam"] },
        { "pokemonId": 107, "level": 27, "moves": ["fire-punch","ice-punch","thunder-punch","mach-punch"] },
        { "pokemonId": 739, "level": 27, "moves": ["crabhammer","bubble-beam","leer","harden"] },
        { "pokemonId": 297, "level": 28, "moves": ["close-combat","bullet-punch","knock-off","fake-out"] }
      ]}]
    },
    {
      "id": "alola-olivia", "name": "Olivia", "title": "Kahuna",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Rock",
      "rosters": [{ "label": "SM", "team": [
        { "pokemonId": 369, "level": 33, "moves": ["ancient-power","surf","dive","rest"] },
        { "pokemonId": 299, "level": 33, "moves": ["rock-throw","harden","block","stealth-rock"] },
        { "pokemonId": 703, "level": 34, "moves": ["rock-slide","moonblast","ancient-power","harden"] },
        { "pokemonId": 476, "level": 34, "moves": ["rock-slide","discharge","earth-power","stealth-rock"] },
        { "pokemonId": 525, "level": 35, "moves": ["rock-slide","earthquake","stealth-rock","smack-down"] },
        { "pokemonId": 745, "level": 35, "moves": ["rock-throw","howl","bite","accelerock"] }
      ]}]
    },
    {
      "id": "alola-nanu", "name": "Nanu", "title": "Kahuna",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Dark",
      "rosters": [{ "label": "SM", "team": [
        { "pokemonId": 510, "level": 43, "moves": ["night-slash","fake-out","attract","thunder-wave"] },
        { "pokemonId": 510, "level": 43, "moves": ["night-slash","fake-out","attract","u-turn"] },
        { "pokemonId": 510, "level": 43, "moves": ["night-slash","fake-out","torment","thunder-wave"] },
        { "pokemonId": 197, "level": 44, "moves": ["foul-play","moonlight","dark-pulse","confuse-ray"] },
        { "pokemonId": 229, "level": 44, "moves": ["crunch","flamethrower","nasty-plot","foul-play"] },
        { "pokemonId": 720, "level": 45, "moves": ["hyperspace-fury","foul-play","throat-chop","darkest-lariat"] }
      ]}]
    },
    {
      "id": "alola-hapu", "name": "Hapu", "title": "Kahuna",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ground",
      "rosters": [{ "label": "SM", "team": [
        { "pokemonId": 618, "level": 51, "moves": ["earthquake","muddy-water","sludge-bomb","discharge"] },
        { "pokemonId": 449, "level": 51, "moves": ["earthquake","rock-slide","bite","bulk-up"] },
        { "pokemonId": 051, "level": 51, "moves": ["earthquake","rock-blast","stone-edge","sand-attack"] },
        { "pokemonId": 232, "level": 52, "moves": ["earthquake","hyper-voice","rapid-spin","thunder-fang"] },
        { "pokemonId": 450, "level": 52, "moves": ["earthquake","crunch","stone-edge","slack-off"] },
        { "pokemonId": 750, "level": 53, "moves": ["earthquake","high-horsepower","heavy-slam","close-combat"] }
      ]}]
    },
    {
      "id": "alola-hala-e4", "name": "Hala", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Fighting",
      "rosters": [{ "label": "SM Elite Four", "team": [
        { "pokemonId": 297, "level": 54, "moves": ["close-combat","bullet-punch","fake-out","knock-off"] },
        { "pokemonId": 740, "level": 54, "moves": ["close-combat","ice-hammer","rock-blast","harden"] },
        { "pokemonId": 62,  "level": 55, "moves": ["dynamic-punch","waterfall","earthquake","hypnosis"] },
        { "pokemonId": 448, "level": 56, "moves": ["aura-sphere","close-combat","dragon-pulse","calm-mind"] },
        { "pokemonId": 760, "level": 56, "moves": ["hammer-arm","shadow-ball","bulk-up","payback"] },
        { "pokemonId": 57,  "level": 57, "moves": ["close-combat","thunder-punch","ice-punch","u-turn"] }
      ]}]
    },
    {
      "id": "alola-olivia-e4", "name": "Olivia", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Rock",
      "rosters": [{ "label": "SM Elite Four", "team": [
        { "pokemonId": 369, "level": 54, "moves": ["ancient-power","surf","rest","earthquake"] },
        { "pokemonId": 703, "level": 54, "moves": ["rock-slide","moonblast","ancient-power","earth-power"] },
        { "pokemonId": 524, "level": 55, "moves": ["stealth-rock","earthquake","rock-blast","smack-down"] },
        { "pokemonId": 476, "level": 55, "moves": ["discharge","rock-slide","earth-power","stealth-rock"] },
        { "pokemonId": 76,  "level": 56, "moves": ["earthquake","rock-blast","explosion","stone-edge"] },
        { "pokemonId": 526, "level": 57, "moves": ["earthquake","rock-slide","stealth-rock","stone-edge"] }
      ]}]
    },
    {
      "id": "alola-acerola", "name": "Acerola", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Ghost",
      "rosters": [{ "label": "SM Elite Four", "team": [
        { "pokemonId": 354, "level": 54, "moves": ["shadow-ball","will-o-wisp","calm-mind","shadow-sneak"] },
        { "pokemonId": 302, "level": 54, "moves": ["shadow-ball","foul-play","recover","will-o-wisp"] },
        { "pokemonId": 425, "level": 55, "moves": ["shadow-ball","ominous-wind","minimize","gust"] },
        { "pokemonId": 781, "level": 56, "moves": ["anchor-shot","shadow-ball","phantom-force","tackle"] },
        { "pokemonId": 770, "level": 56, "moves": ["shore-up","earth-power","shadow-ball","bulldoze"] },
        { "pokemonId": 778, "level": 57, "moves": ["play-rough","shadow-sneak","shadow-claw","swords-dance"] }
      ]}]
    },
    {
      "id": "alola-kahili", "name": "Kahili", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Flying",
      "rosters": [{ "label": "SM Elite Four", "team": [
        { "pokemonId": 169, "level": 54, "moves": ["cross-poison","air-slash","confuse-ray","nasty-plot"] },
        { "pokemonId": 227, "level": 54, "moves": ["iron-head","aerial-ace","spikes","fly"] },
        { "pokemonId": 630, "level": 55, "moves": ["foul-play","air-slash","nasty-plot","roost"] },
        { "pokemonId": 741, "level": 56, "moves": ["revelation-dance","air-slash","roost","calm-mind"] },
        { "pokemonId": 628, "level": 56, "moves": ["brave-bird","superpower","rock-slide","roost"] },
        { "pokemonId": 733, "level": 57, "moves": ["beak-blast","bullet-seed","roost","boomburst"] }
      ]}]
    },
    {
      "id": "alola-kukui", "name": "Professor Kukui", "title": "Champion",
      "trainerClass": "CHAMPION", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "SM", "team": [
        { "pokemonId": 745, "level": 57, "moves": ["stone-edge","accelerock","crunch","swords-dance"] },
        { "pokemonId": 38,  "level": 58, "moves": ["blizzard","moonblast","will-o-wisp","nasty-plot"] },
        { "pokemonId": 628, "level": 58, "moves": ["brave-bird","superpower","rock-slide","roost"] },
        { "pokemonId": 462, "level": 58, "moves": ["thunderbolt","flash-cannon","volt-switch","tri-attack"] },
        { "pokemonId": 143, "level": 59, "moves": ["body-slam","heavy-slam","crunch","curse"] },
        { "pokemonId": 727, "level": 60, "moves": ["darkest-lariat","flare-blitz","close-combat","cross-chop"] }
      ]}]
    },
    {
      "id": "alola-hau", "name": "Hau", "title": "Rival",
      "trainerClass": "RIVAL", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "Final Battle (SM)", "team": [
        { "pokemonId": 738, "level": 55, "moves": ["thunderbolt","bug-buzz","volt-switch","wild-charge"] },
        { "pokemonId": 740, "level": 55, "moves": ["close-combat","ice-hammer","rock-blast","harden"] },
        { "pokemonId": 407, "level": 56, "moves": ["petal-blizzard","magical-leaf","stun-spore","aromatherapy"] },
        { "pokemonId": 663, "level": 56, "moves": ["flare-blitz","brave-bird","roost","swords-dance"] },
        { "pokemonId": 184, "level": 56, "moves": ["surf","ice-beam","aqua-ring","double-slap"] },
        { "pokemonId": 730, "level": 58, "moves": ["sparkling-aria","ice-beam","aqua-jet","perish-song"] }
      ]}]
    }
  ]
}
```

- [ ] **Step 2: Validate and commit**

```bash
python3 -m json.tool app/src/main/assets/trainers.json > /dev/null && echo "JSON valid"
git add app/src/main/assets/trainers.json
git commit -m "feat: add Alola region to trainers.json"
```

---
## Task 10: trainers.json — Galar

Append to the `"regions"` array in `app/src/main/assets/trainers.json`.

- [ ] **Step 1: Append Galar region data**

```json
{
  "name": "Galar",
  "trainers": [
    {
      "id": "galar-milo", "name": "Milo", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Grass",
      "rosters": [{ "label": "SWSH", "team": [
        { "pokemonId": 829, "level": 19, "moves": ["leafage","growl","round","magical-leaf"] },
        { "pokemonId": 315, "level": 19, "moves": ["magical-leaf","leech-seed","stun-spore","mega-drain"] },
        { "pokemonId": 840, "level": 19, "moves": ["leafage","withdraw","round","magical-leaf"] },
        { "pokemonId": 546, "level": 19, "moves": ["fairy-wind","leech-seed","stun-spore","mega-drain"] },
        { "pokemonId": 420, "level": 19, "moves": ["absorb","growl","helping-hand","magical-leaf"] },
        { "pokemonId": 830, "level": 20, "moves": ["petal-dance","cotton-guard","ingrain","leech-seed"] }
      ]}]
    },
    {
      "id": "galar-nessa", "name": "Nessa", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Water",
      "rosters": [{ "label": "SWSH", "team": [
        { "pokemonId": 118, "level": 22, "moves": ["peck","water-gun","horn-attack","agility"] },
        { "pokemonId": 846, "level": 23, "moves": ["water-gun","bite","aqua-jet","leer"] },
        { "pokemonId": 116, "level": 22, "moves": ["water-gun","smokescreen","bubble","leer"] },
        { "pokemonId": 747, "level": 23, "moves": ["poison-sting","water-gun","aqua-ring","bite"] },
        { "pokemonId": 320, "level": 23, "moves": ["surf","amnesia","rollout","water-pulse"] },
        { "pokemonId": 834, "level": 24, "moves": ["razor-shell","rock-blast","waterfall","slam"] }
      ]}]
    },
    {
      "id": "galar-kabu", "name": "Kabu", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fire",
      "rosters": [{ "label": "SWSH", "team": [
        { "pokemonId": 218, "level": 25, "moves": ["ember","smog","rock-throw","harden"] },
        { "pokemonId": 37,  "level": 25, "moves": ["ember","tail-whip","growl","quick-attack"] },
        { "pokemonId": 58,  "level": 25, "moves": ["ember","bite","leer","odor-sleuth"] },
        { "pokemonId": 219, "level": 26, "moves": ["ember","rock-throw","smog","harden"] },
        { "pokemonId": 608, "level": 26, "moves": ["ember","smog","will-o-wisp","hex"] },
        { "pokemonId": 851, "level": 27, "moves": ["flame-wheel","fire-lash","bug-bite","leech-life"] }
      ]}]
    },
    {
      "id": "galar-bea", "name": "Bea", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fighting",
      "rosters": [{ "label": "SW", "team": [
        { "pokemonId": 68,  "level": 34, "moves": ["dynamic-punch","bullet-punch","knock-off","cross-chop"] },
        { "pokemonId": 237, "level": 34, "moves": ["close-combat","rapid-spin","sucker-punch","stone-edge"] },
        { "pokemonId": 539, "level": 35, "moves": ["close-combat","hi-jump-kick","quick-guard","rock-slide"] },
        { "pokemonId": 448, "level": 35, "moves": ["close-combat","aura-sphere","quick-attack","bone-rush"] },
        { "pokemonId": 297, "level": 35, "moves": ["close-combat","bullet-punch","fake-out","knock-off"] },
        { "pokemonId": 865, "level": 37, "moves": ["close-combat","leaf-blade","night-slash","swords-dance"] }
      ]}]
    },
    {
      "id": "galar-opal", "name": "Opal", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fairy",
      "rosters": [{ "label": "SWSH", "team": [
        { "pokemonId": 39,  "level": 36, "moves": ["doubleslap","sing","body-slam","moonblast"] },
        { "pokemonId": 303, "level": 36, "moves": ["play-rough","iron-head","fairy-wind","crunch"] },
        { "pokemonId": 110, "level": 37, "moves": ["strange-steam","sludge-bomb","thunderbolt","will-o-wisp"] },
        { "pokemonId": 468, "level": 37, "moves": ["moonblast","air-slash","aura-sphere","thunder"] },
        { "pokemonId": 35,  "level": 37, "moves": ["moonblast","doubleslap","minimize","sing"] },
        { "pokemonId": 869, "level": 38, "moves": ["decorate","moonblast","dazzling-gleam","misty-terrain"] }
      ]}]
    },
    {
      "id": "galar-gordie", "name": "Gordie", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Rock",
      "rosters": [{ "label": "SW", "team": [
        { "pokemonId": 213, "level": 40, "moves": ["rock-blast","power-trick","rollout","bug-bite"] },
        { "pokemonId": 185, "level": 40, "moves": ["rock-slide","earthquake","block","stealth-rock"] },
        { "pokemonId": 219, "level": 41, "moves": ["rock-slide","earth-power","overheat","stealth-rock"] },
        { "pokemonId": 75,  "level": 41, "moves": ["rock-blast","earthquake","explosion","stealth-rock"] },
        { "pokemonId": 874, "level": 41, "moves": ["stone-edge","earthquake","rock-polish","heavy-slam"] },
        { "pokemonId": 839, "level": 42, "moves": ["tar-shot","heat-crash","smack-down","stealth-rock"] }
      ]}]
    },
    {
      "id": "galar-piers", "name": "Piers", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Dark",
      "rosters": [{ "label": "SWSH", "team": [
        { "pokemonId": 560, "level": 44, "moves": ["crunch","high-jump-kick","dragon-dance","head-smash"] },
        { "pokemonId": 687, "level": 44, "moves": ["topsy-turvy","foul-play","psycho-cut","night-slash"] },
        { "pokemonId": 435, "level": 45, "moves": ["crunch","sucker-punch","toxic","explosion"] },
        { "pokemonId": 197, "level": 45, "moves": ["foul-play","moonlight","dark-pulse","last-resort"] },
        { "pokemonId": 430, "level": 46, "moves": ["foul-play","sucker-punch","nasty-plot","pursuit"] },
        { "pokemonId": 862, "level": 46, "moves": ["darkest-lariat","gunk-shot","counter","facade"] }
      ]}]
    },
    {
      "id": "galar-raihan", "name": "Raihan", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Dragon",
      "rosters": [{ "label": "SWSH", "team": [
        { "pokemonId": 330, "level": 46, "moves": ["dragon-claw","earthquake","crunch","sandstorm"] },
        { "pokemonId": 526, "level": 47, "moves": ["earthquake","rock-slide","stealth-rock","stone-edge"] },
        { "pokemonId": 844, "level": 47, "moves": ["coil","bulldoze","wrap","glare"] },
        { "pokemonId": 142, "level": 47, "moves": ["crunch","wing-attack","ancientpower","bite"] },
        { "pokemonId": 340, "level": 47, "moves": ["surf","earthquake","sandstorm","amnesia"] },
        { "pokemonId": 884, "level": 48, "moves": ["dragon-pulse","flash-cannon","steel-beam","dragon-dance"] }
      ]}]
    },
    {
      "id": "galar-leon", "name": "Leon", "title": "Champion",
      "trainerClass": "CHAMPION", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "SWSH", "team": [
        { "pokemonId": 681, "level": 62, "moves": ["shadow-ball","iron-head","king-shield","swords-dance"] },
        { "pokemonId": 612, "level": 63, "moves": ["dragon-dance","outrage","poison-jab","iron-tail"] },
        { "pokemonId": 537, "level": 64, "moves": ["earth-power","surf","muddy-water","hyper-voice"] },
        { "pokemonId": 866, "level": 64, "moves": ["psychic","ice-punch","triple-axel","nasty-plot"] },
        { "pokemonId": 887, "level": 65, "moves": ["dragon-rush","phantom-force","thunderbolt","shadow-ball"] },
        { "pokemonId": 6,   "level": 65, "moves": ["flamethrower","air-slash","thunder-punch","max-strike"] }
      ]}]
    },
    {
      "id": "galar-hop", "name": "Hop", "title": "Rival",
      "trainerClass": "RIVAL", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "Final Battle (SWSH)", "team": [
        { "pokemonId": 398, "level": 60, "moves": ["close-combat","fly","u-turn","sky-attack"] },
        { "pokemonId": 877, "level": 61, "moves": ["aura-wheel","bite","spark","quick-attack"] },
        { "pokemonId": 508, "level": 61, "moves": ["play-rough","fire-fang","thunder-fang","superpower"] },
        { "pokemonId": 245, "level": 62, "moves": ["surf","blizzard","aurora-beam","hydro-pump"] },
        { "pokemonId": 473, "level": 62, "moves": ["blizzard","earthquake","ice-shard","stone-edge"] },
        { "pokemonId": 812, "level": 63, "moves": ["wood-hammer","high-horsepower","drum-beating","grassy-glide"] }
      ]}]
    }
  ]
}
```

- [ ] **Step 2: Validate and commit**

```bash
python3 -m json.tool app/src/main/assets/trainers.json > /dev/null && echo "JSON valid"
git add app/src/main/assets/trainers.json
git commit -m "feat: add Galar region to trainers.json"
```

---
## Task 11: trainers.json — Paldea

Append to the `"regions"` array in `app/src/main/assets/trainers.json`.

- [ ] **Step 1: Append Paldea region data**

```json
{
  "name": "Paldea",
  "trainers": [
    {
      "id": "paldea-katy", "name": "Katy", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Bug",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 919, "level": 14, "moves": ["bug-bite","lunge","quick-attack","agility"] },
        { "pokemonId": 588, "level": 14, "moves": ["vice-grip","bug-bite","bide","string-shot"] },
        { "pokemonId": 165, "level": 14, "moves": ["string-shot","bug-bite","comet-punch","swift"] },
        { "pokemonId": 290, "level": 14, "moves": ["scratch","harden","leech-life","string-shot"] },
        { "pokemonId": 291, "level": 14, "moves": ["leech-life","fury-swipes","mind-reader","aerial-ace"] },
        { "pokemonId": 216, "level": 15, "moves": ["scratch","lick","fake-tears","bug-bite"] }
      ]}]
    },
    {
      "id": "paldea-brassius", "name": "Brassius", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Grass",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 928, "level": 16, "moves": ["absorb","growl","leafage","round"] },
        { "pokemonId": 191, "level": 16, "moves": ["absorb","growth","ingrain","mega-drain"] },
        { "pokemonId": 43,  "level": 16, "moves": ["absorb","poisonpowder","sleep-powder","mega-drain"] },
        { "pokemonId": 273, "level": 16, "moves": ["absorb","growl","nature-power","mega-drain"] },
        { "pokemonId": 546, "level": 17, "moves": ["fairy-wind","leech-seed","mega-drain","stun-spore"] },
        { "pokemonId": 185, "level": 18, "moves": ["slam","wood-hammer","sucker-punch","counter"] }
      ]}]
    },
    {
      "id": "paldea-iono", "name": "Iono", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Electric",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 404, "level": 23, "moves": ["spark","bite","leer","charge"] },
        { "pokemonId": 587, "level": 23, "moves": ["volt-switch","aerial-ace","quick-attack","spark"] },
        { "pokemonId": 170, "level": 23, "moves": ["thundershock","water-gun","supersonic","bubble-beam"] },
        { "pokemonId": 171, "level": 24, "moves": ["thunderbolt","surf","confuse-ray","bubble-beam"] },
        { "pokemonId": 429, "level": 24, "moves": ["thunderbolt","shadow-ball","will-o-wisp","calm-mind"] },
        { "pokemonId": 939, "level": 26, "moves": ["thunderbolt","chilling-water","belch","electric-terrain"] }
      ]}]
    },
    {
      "id": "paldea-kofu", "name": "Kofu", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Water",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 320, "level": 29, "moves": ["surf","amnesia","rollout","water-pulse"] },
        { "pokemonId": 119, "level": 29, "moves": ["surf","horn-attack","agility","aqua-tail"] },
        { "pokemonId": 320, "level": 29, "moves": ["surf","amnesia","body-slam","water-pulse"] },
        { "pokemonId": 739, "level": 30, "moves": ["crabhammer","aqua-jet","leer","bubble-beam"] },
        { "pokemonId": 976, "level": 30, "moves": ["aqua-cutter","slash","aqua-jet","agility"] },
        { "pokemonId": 740, "level": 32, "moves": ["liquidation","crabhammer","rock-blast","swords-dance"] }
      ]}]
    },
    {
      "id": "paldea-larry", "name": "Larry", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Normal",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 775, "level": 35, "moves": ["snore","yawn","sucker-punch","wood-hammer"] },
        { "pokemonId": 108, "level": 35, "moves": ["hyper-voice","slam","stomp","disable"] },
        { "pokemonId": 234, "level": 35, "moves": ["psybeam","tackle","confuse-ray","take-down"] },
        { "pokemonId": 241, "level": 35, "moves": ["stomp","milk-drink","rollout","body-slam"] },
        { "pokemonId": 128, "level": 36, "moves": ["earthquake","horn-attack","thrash","stomp"] },
        { "pokemonId": 982, "level": 36, "moves": ["hyper-drill","sucker-punch","glare","coil"] }
      ]}]
    },
    {
      "id": "paldea-ryme", "name": "Ryme", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ghost",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 354, "level": 41, "moves": ["shadow-ball","will-o-wisp","shadow-sneak","calm-mind"] },
        { "pokemonId": 778, "level": 41, "moves": ["shadow-claw","play-rough","shadow-sneak","swords-dance"] },
        { "pokemonId": 592, "level": 42, "moves": ["shadow-ball","surf","will-o-wisp","hex"] },
        { "pokemonId": 849, "level": 42, "moves": ["shadow-ball","overdrive","discharge","shift-gear"] },
        { "pokemonId": 593, "level": 42, "moves": ["shadow-ball","surf","night-shade","will-o-wisp"] },
        { "pokemonId": 971, "level": 44, "moves": ["phantom-force","crunch","shadow-ball","will-o-wisp"] }
      ]}]
    },
    {
      "id": "paldea-tulip", "name": "Tulip", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Psychic",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 561, "level": 44, "moves": ["psychic","air-slash","cosmic-power","ice-beam"] },
        { "pokemonId": 281, "level": 44, "moves": ["psychic","moonblast","calm-mind","dazzling-gleam"] },
        { "pokemonId": 54,  "level": 44, "moves": ["psychic","water-pulse","confusion","zen-headbutt"] },
        { "pokemonId": 576, "level": 45, "moves": ["psychic","calm-mind","thunderbolt","shadow-ball"] },
        { "pokemonId": 671, "level": 45, "moves": ["moonblast","psychic","aromatherapy","petal-blizzard"] },
        { "pokemonId": 956, "level": 46, "moves": ["lumina-crash","psychic","calm-mind","dazzling-gleam"] }
      ]}]
    },
    {
      "id": "paldea-grusha", "name": "Grusha", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ice",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 582, "level": 47, "moves": ["icicle-spear","iron-defense","ice-shard","astonish"] },
        { "pokemonId": 91,  "level": 47, "moves": ["icicle-spear","spikes","ice-shard","explosion"] },
        { "pokemonId": 460, "level": 48, "moves": ["blizzard","wood-hammer","ice-punch","leech-seed"] },
        { "pokemonId": 614, "level": 48, "moves": ["blizzard","bulk-up","slash","ice-shard"] },
        { "pokemonId": 334, "level": 48, "moves": ["dragon-pulse","icy-wind","dragon-dance","aerial-ace"] },
        { "pokemonId": 974, "level": 50, "moves": ["blizzard","body-press","ice-spinner","headlong-rush"] }
      ]}]
    },
    {
      "id": "paldea-rika", "name": "Rika", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Ground",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 51,  "level": 57, "moves": ["earthquake","rock-blast","sand-attack","stone-edge"] },
        { "pokemonId": 232, "level": 57, "moves": ["earthquake","hyper-voice","rapid-spin","thunder-fang"] },
        { "pokemonId": 450, "level": 58, "moves": ["earthquake","crunch","stone-edge","slack-off"] },
        { "pokemonId": 323, "level": 58, "moves": ["earthquake","overheat","rock-slide","fire-blast"] },
        { "pokemonId": 445, "level": 58, "moves": ["outrage","earthquake","crunch","swords-dance"] },
        { "pokemonId": 980, "level": 61, "moves": ["earthquake","water-pulse","recover","toxic"] }
      ]}]
    },
    {
      "id": "paldea-poppy", "name": "Poppy", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Steel",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 437, "level": 58, "moves": ["flash-cannon","psychic","iron-defense","calm-mind"] },
        { "pokemonId": 462, "level": 58, "moves": ["flash-cannon","thunderbolt","volt-switch","tri-attack"] },
        { "pokemonId": 476, "level": 58, "moves": ["flash-cannon","discharge","earth-power","stealth-rock"] },
        { "pokemonId": 823, "level": 59, "moves": ["iron-head","brave-bird","bulk-up","steel-wing"] },
        { "pokemonId": 879, "level": 59, "moves": ["heavy-slam","earthquake","ice-punch","play-rough"] },
        { "pokemonId": 959, "level": 61, "moves": ["gigaton-hammer","play-rough","thunder-clap","fairy-wind"] }
      ]}]
    },
    {
      "id": "paldea-larry-e4", "name": "Larry", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Flying",
      "rosters": [{ "label": "SV Elite Four", "team": [
        { "pokemonId": 398, "level": 59, "moves": ["close-combat","fly","final-gambit","u-turn"] },
        { "pokemonId": 430, "level": 59, "moves": ["foul-play","aerial-ace","sucker-punch","nasty-plot"] },
        { "pokemonId": 357, "level": 59, "moves": ["fly","dragon-claw","leaf-blade","earthquake"] },
        { "pokemonId": 334, "level": 60, "moves": ["dragon-pulse","aerial-ace","dragon-dance","roost"] },
        { "pokemonId": 741, "level": 60, "moves": ["revelation-dance","air-slash","roost","calm-mind"] },
        { "pokemonId": 973, "level": 62, "moves": ["wing-attack","close-combat","aerial-ace","baton-pass"] }
      ]}]
    },
    {
      "id": "paldea-hassel", "name": "Hassel", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Dragon",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 330, "level": 60, "moves": ["dragon-claw","earthquake","crunch","fly"] },
        { "pokemonId": 334, "level": 60, "moves": ["dragon-pulse","aerial-ace","dragon-dance","roost"] },
        { "pokemonId": 715, "level": 61, "moves": ["dragon-pulse","air-slash","roost","boomburst"] },
        { "pokemonId": 691, "level": 61, "moves": ["dragon-pulse","sludge-bomb","surf","shadow-ball"] },
        { "pokemonId": 841, "level": 61, "moves": ["dragon-pulse","draco-meteor","recover","apple-acid"] },
        { "pokemonId": 998, "level": 62, "moves": ["glaive-rush","ice-shard","crunch","outrage"] }
      ]}]
    },
    {
      "id": "paldea-geeta", "name": "Geeta", "title": "Champion",
      "trainerClass": "CHAMPION", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "SV", "team": [
        { "pokemonId": 956, "level": 61, "moves": ["lumina-crash","calm-mind","dazzling-gleam","psyshield-bash"] },
        { "pokemonId": 673, "level": 61, "moves": ["grass-knot","zen-headbutt","earthquake","synthesis"] },
        { "pokemonId": 976, "level": 62, "moves": ["aqua-cutter","slash","aqua-jet","agility"] },
        { "pokemonId": 713, "level": 63, "moves": ["avalanche","earthquake","recover","iron-defense"] },
        { "pokemonId": 983, "level": 63, "moves": ["kowtow-cleave","iron-head","swords-dance","sucker-punch"] },
        { "pokemonId": 970, "level": 65, "moves": ["power-gem","sludge-wave","earth-power","acid-armor"] }
      ]}]
    },
    {
      "id": "paldea-nemona", "name": "Nemona", "title": "Rival",
      "trainerClass": "RIVAL", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "Final Battle (SV)", "team": [
        { "pokemonId": 936, "level": 62, "moves": ["close-combat","flare-blitz","giga-impact","high-horsepower"] },
        { "pokemonId": 164, "level": 62, "moves": ["air-slash","hyper-voice","extrasensory","roost"] },
        { "pokemonId": 663, "level": 63, "moves": ["flare-blitz","brave-bird","swords-dance","roost"] },
        { "pokemonId": 700, "level": 63, "moves": ["moonblast","calm-mind","psychic","wish"] },
        { "pokemonId": 977, "level": 63, "moves": ["order-up","aqua-tail","dragon-dance","crunch"] },
        { "pokemonId": 908, "level": 65, "moves": ["torch-song","shadow-ball","overheat","will-o-wisp"] }
      ]}]
    }
  ]
}
```

- [ ] **Step 2: Validate and commit**

```bash
python3 -m json.tool app/src/main/assets/trainers.json > /dev/null && echo "JSON valid"
git add app/src/main/assets/trainers.json
git commit -m "feat: add Paldea region to trainers.json"
```

---
## Task 12: TurnBattleViewModel — trainer entry points + tests

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleViewModel.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerBattleViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerBattleViewModelTest.kt
package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.repository.FakeCaughtPokemonDao
import com.madmaxlgndklr.pokedex.repository.FakeMoveDao
import com.madmaxlgndklr.pokedex.repository.FakePokemonDetailCacheDao
import com.madmaxlgndklr.pokedex.repository.FakePokemonListCacheDao
import com.madmaxlgndklr.pokedex.repository.fakeSettingsRepo
import com.madmaxlgndklr.pokedex.ui.battle.BattleState
import com.madmaxlgndklr.pokedex.ui.battle.TrainerClass
import com.madmaxlgndklr.pokedex.ui.battle.Trainer
import com.madmaxlgndklr.pokedex.ui.battle.TrainerPokemon
import com.madmaxlgndklr.pokedex.ui.battle.TrainerRoster
import com.madmaxlgndklr.pokedex.ui.battle.TurnBattleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

private fun fakeTrainer(rosterCount: Int = 1): Trainer {
    val roster = TrainerRoster(
        label = "Test",
        team = List(6) { TrainerPokemon(pokemonId = 6, level = 50,
            moves = listOf("flamethrower", "fly", "dragon-claw", "slash")) }
    )
    return Trainer(
        id = "test-trainer", name = "Test", title = "Gym Leader",
        region = "TestRegion", trainerClass = TrainerClass.GYM_LEADER,
        typeSpecialty = "Fire",
        rosters = if (rosterCount == 2) listOf(roster, roster.copy(label = "Rematch")) else listOf(roster)
    )
}

private fun repo() = PokemonRepository(
    CharizardApiService(), FakeCaughtPokemonDao(),
    FakePokemonListCacheDao(), FakePokemonDetailCacheDao(), FakeMoveDao()
)

@OptIn(ExperimentalCoroutinesApi::class)
class TrainerBattleViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var vm: TurnBattleViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = TurnBattleViewModel(repo(), fakeSettingsRepo())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `loadTrainerSetup sets battleTrainer`() = runTest {
        val trainer = fakeTrainer()
        vm.loadTrainerSetup(trainer, 0, listOf(6))
        advanceUntilIdle()
        assertNotNull(vm.battleTrainer.value)
        assertEquals(trainer, vm.battleTrainer.value!!.trainer)
        assertEquals(0, vm.battleTrainer.value!!.rosterIndex)
    }

    @Test
    fun `loadTrainerSetup also loads setup`() = runTest {
        vm.loadTrainerSetup(fakeTrainer(), 0, listOf(6))
        advanceUntilIdle()
        assertNotNull(vm.setup.value)
    }

    @Test
    fun `resetToSetup clears battleTrainer`() = runTest {
        vm.loadTrainerSetup(fakeTrainer(), 0, listOf(6))
        advanceUntilIdle()
        vm.resetToSetup()
        assertNull(vm.battleTrainer.value)
    }

    @Test
    fun `startTrainerBattle produces Ongoing state with 6-pokemon opponent`() = runTest {
        val trainer = fakeTrainer()
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        vm.startTrainerBattle(trainer, 0, listOf(6))
        advanceUntilIdle()
        val state = vm.battleState.value
        assertTrue("battle must be Ongoing", state is BattleState.Ongoing)
        assertEquals("opponent team must have 6", 6,
            (state as BattleState.Ongoing).opponentTeam.size)
    }

    @Test
    fun `startTrainerBattle with rosterIndex 1 uses second roster`() = runTest {
        val roster0 = TrainerRoster("Original", List(6) {
            TrainerPokemon(6, 50, listOf("flamethrower","fly","dragon-claw","slash")) })
        val roster1 = TrainerRoster("Rematch", List(6) {
            TrainerPokemon(9, 70, listOf("surf","blizzard","ice-beam","skull-bash")) })
        val trainer = Trainer("t","T","GymLeader","X",
            TrainerClass.GYM_LEADER,"Fire", listOf(roster0, roster1))
        vm.loadSetup(listOf(6))
        advanceUntilIdle()
        vm.startTrainerBattle(trainer, 1, listOf(6))
        advanceUntilIdle()
        val state = vm.battleState.value as? BattleState.Ongoing ?: error("not ongoing")
        // roster1 has Blastoise (id=9); verify via level 70
        assertEquals(70, state.opponentTeam[0].level)
    }

    @Test
    fun `startBattleFromSetup with battleTrainer uses trainer team as opponent`() = runTest {
        val trainer = fakeTrainer()
        vm.loadTrainerSetup(trainer, 0, listOf(6))
        advanceUntilIdle()
        vm.startBattleFromSetup(listOf(6))
        advanceUntilIdle()
        val state = vm.battleState.value as? BattleState.Ongoing ?: error("not ongoing")
        assertEquals(6, state.opponentTeam.size)
    }

    @Test
    fun `startTrainerBattle with empty teamIds is no-op`() = runTest {
        vm.startTrainerBattle(fakeTrainer(), 0, emptyList())
        advanceUntilIdle()
        assertNull(vm.battleState.value)
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.TrainerBattleViewModelTest" 2>&1 | tail -20
```

- [ ] **Step 3: Add SelectedTrainer + battleTrainer state + new methods to TurnBattleViewModel**

Add these to `TurnBattleViewModel.kt` after the existing imports and before the class body. Insert `SelectedTrainer` as a top-level data class at the end of the file's top-level declarations (after `SlotOverride`, `BattleSetup`, `LearnableMove`):

```kotlin
data class SelectedTrainer(val trainer: Trainer, val rosterIndex: Int)
```

Inside `TurnBattleViewModel`, add the new state field after `_heldItemSyncError`:

```kotlin
private val _battleTrainer = MutableStateFlow<SelectedTrainer?>(null)
val battleTrainer: StateFlow<SelectedTrainer?> = _battleTrainer
```

Replace the existing `resetToSetup()`:

```kotlin
fun resetToSetup() {
    _battleState.value = null
    _battleTrainer.value = null
}
```

Add the two new methods before `submitMove`:

```kotlin
fun loadTrainerSetup(trainer: Trainer, rosterIndex: Int, teamIds: List<Int>) {
    _battleTrainer.value = SelectedTrainer(trainer, rosterIndex)
    _setup.value = null  // allow loadSetup to re-run
    loadSetup(teamIds)
}

fun startTrainerBattle(trainer: Trainer, rosterIndex: Int, teamIds: List<Int>) {
    if (teamIds.isEmpty()) return
    val s = _setup.value ?: return
    viewModelScope.launch {
        _isLoading.value = true
        try {
            val gen = settingsRepo.selectedGen.first()
            val playerTeam = teamIds.mapIndexed { idx, pokemonId ->
                val detail = if (idx == 0) s.playerDetail else {
                    try { repo.getPokemonDetail(pokemonId) } catch (_: Exception) { s.playerDetail }
                }
                val ov = s.teamOverrides[idx]
                val level = ov?.level ?: s.level
                val statConfig = ov?.statConfig ?: s.statConfig
                val nature = ov?.nature ?: s.nature
                val heldItem = ov?.heldItem ?: s.heldItem
                val moves = if (idx == 0) resolveMoves(s.selectedMoveNames)
                    else resolveMoves(learnableMoves(detail, level).filter { it.available }.take(4).map { it.name })
                BattleEngine.buildBattlePokemon(detail, level, moves, statConfig, nature, heldItem)
            }
            val opponentTeam = trainer.rosters[rosterIndex].team.mapNotNull { tp ->
                try {
                    val detail = repo.getPokemonDetail(tp.pokemonId)
                    val moves = resolveMoves(tp.moves)
                    BattleEngine.buildBattlePokemon(detail, tp.level, moves)
                } catch (_: Exception) { null }
            }
            if (opponentTeam.isEmpty()) return@launch
            _battleState.value = BattleEngine.startBattle(playerTeam, opponentTeam, gen)
        } catch (_: Exception) {
        } finally {
            _isLoading.value = false
        }
    }
}
```

Modify `startBattleFromSetup` — replace the opponent-building block (the lines from `val opponentDetail` through `_battleState.value =`):

```kotlin
fun startBattleFromSetup(teamIds: List<Int>) {
    val s = _setup.value ?: return
    if (s.selectedMoveNames.isEmpty()) return
    if (teamIds.isEmpty()) return
    viewModelScope.launch {
        _isLoading.value = true
        try {
            val gen = settingsRepo.selectedGen.first()
            val playerTeam = teamIds.mapIndexed { idx, pokemonId ->
                val detail = if (idx == 0) s.playerDetail else {
                    try { repo.getPokemonDetail(pokemonId) } catch (_: Exception) { s.playerDetail }
                }
                val ov = s.teamOverrides[idx]
                val level = ov?.level ?: s.level
                val statConfig = ov?.statConfig ?: s.statConfig
                val nature = ov?.nature ?: s.nature
                val heldItem = ov?.heldItem ?: s.heldItem
                val moves = if (idx == 0) resolveMoves(s.selectedMoveNames)
                    else resolveMoves(learnableMoves(detail, level).filter { it.available }.take(4).map { it.name })
                BattleEngine.buildBattlePokemon(detail, level, moves, statConfig, nature, heldItem)
            }
            val bt = _battleTrainer.value
            val opponentTeam = if (bt != null) {
                bt.trainer.rosters[bt.rosterIndex].team.mapNotNull { tp ->
                    try {
                        val detail = repo.getPokemonDetail(tp.pokemonId)
                        BattleEngine.buildBattlePokemon(detail, tp.level, resolveMoves(tp.moves))
                    } catch (_: Exception) { null }
                }.ifEmpty { return@launch }
            } else {
                val opponentDetail = repo.getPokemonDetail(repo.getPokemonList().random().id)
                val opponentMoves = resolveMoves(opponentDetail.moves.take(4).map { it.name })
                listOf(BattleEngine.buildBattlePokemon(opponentDetail, s.level, opponentMoves))
            }
            _battleState.value = BattleEngine.startBattle(playerTeam, opponentTeam, gen)
        } catch (_: Exception) {
        } finally {
            _isLoading.value = false
        }
    }
}
```

Also add the import at the top of `TurnBattleViewModel.kt`:

```kotlin
import com.madmaxlgndklr.pokedex.ui.battle.Trainer
```

- [ ] **Step 4: Run tests — expect all pass**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.TrainerBattleViewModelTest" 2>&1 | tail -20
```

Expected: 7 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleViewModel.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerBattleViewModelTest.kt
git commit -m "feat: add trainer battle entry points to TurnBattleViewModel"
```

---

### Task 13: TrainerSelectViewModel + test

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerSelectViewModel.kt`
- Create: `app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerSelectViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerSelectViewModelTest.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.data.trainer.TrainerRepository
import com.madmaxlgndklr.pokedex.ui.battle.TrainerSelectViewModel
import com.madmaxlgndklr.pokedex.ui.battle.Trainer
import com.madmaxlgndklr.pokedex.ui.battle.TrainerClass
import com.madmaxlgndklr.pokedex.ui.battle.TrainerRoster
import com.madmaxlgndklr.pokedex.ui.battle.TrainerPokemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainerSelectViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val samplePokemon = TrainerPokemon(1, 50, listOf("tackle", "growl", "cut", "thunder-wave"))

    private val sampleRoster = TrainerRoster("Original (RBY)", List(6) { samplePokemon })

    private val brockTrainer = Trainer(
        id = "kanto-brock",
        name = "Brock",
        title = "Gym Leader",
        region = "Kanto",
        trainerClass = TrainerClass.GYM_LEADER,
        typeSpecialty = "Rock",
        rosters = listOf(
            sampleRoster,
            TrainerRoster("Rematch (FRLG)", List(6) { samplePokemon })
        )
    )

    private val mistyTrainer = Trainer(
        id = "kanto-misty",
        name = "Misty",
        title = "Gym Leader",
        region = "Kanto",
        trainerClass = TrainerClass.GYM_LEADER,
        typeSpecialty = "Water",
        rosters = listOf(sampleRoster, TrainerRoster("Rematch (FRLG)", List(6) { samplePokemon }))
    )

    private val lanceTrainer = Trainer(
        id = "kanto-lance",
        name = "Lance",
        title = "Champion",
        region = "Kanto",
        trainerClass = TrainerClass.CHAMPION,
        typeSpecialty = "Mixed",
        rosters = listOf(sampleRoster)
    )

    private fun makeRepo(trainers: List<Trainer>): TrainerRepository {
        val json = buildString {
            append("""{"regions":[""")
            val byRegion = trainers.groupBy { it.region }
            append(byRegion.entries.joinToString(",") { (region, ts) ->
                """{"name":"$region","trainers":[${ts.joinToString(",") { t ->
                    """{"id":"${t.id}","name":"${t.name}","title":"${t.title}",""" +
                    """"trainerClass":"${t.trainerClass.name}","typeSpecialty":"${t.typeSpecialty}",""" +
                    """"rosters":[${t.rosters.joinToString(",") { r ->
                        """{"label":"${r.label}","team":[${r.team.joinToString(",") { p ->
                            """{"pokemonId":${p.pokemonId},"level":${p.level},"moves":${p.moves.map { "\"$it\"" }}}"""
                        }}]"""
                    }}]}"""
                }}]}"""
            })
            append("]}")
        }
        return TrainerRepository(json)
    }

    private fun makeVm(trainers: List<Trainer> = listOf(brockTrainer, mistyTrainer, lanceTrainer)): TrainerSelectViewModel {
        return TrainerSelectViewModel(makeRepo(trainers))
    }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `trainers loaded at init`() = runTest {
        val vm = makeVm()
        assertEquals(3, vm.trainers.value.size)
    }

    @Test
    fun `toggleRegion expands a collapsed region`() = runTest {
        val vm = makeVm()
        assertFalse("Kanto" in vm.expandedRegions.value)
        vm.toggleRegion("Kanto")
        assertTrue("Kanto" in vm.expandedRegions.value)
    }

    @Test
    fun `toggleRegion collapses an expanded region`() = runTest {
        val vm = makeVm()
        vm.toggleRegion("Kanto")
        assertTrue("Kanto" in vm.expandedRegions.value)
        vm.toggleRegion("Kanto")
        assertFalse("Kanto" in vm.expandedRegions.value)
    }

    @Test
    fun `openSheet sets sheetTrainer`() = runTest {
        val vm = makeVm()
        assertNull(vm.sheetTrainer.value)
        vm.openSheet(brockTrainer)
        assertEquals(brockTrainer, vm.sheetTrainer.value)
    }

    @Test
    fun `closeSheet clears sheetTrainer and resets rosterIndex`() = runTest {
        val vm = makeVm()
        vm.openSheet(brockTrainer)
        vm.setRosterIndex(1)
        vm.closeSheet()
        assertNull(vm.sheetTrainer.value)
        assertEquals(0, vm.sheetRosterIndex.value)
    }

    @Test
    fun `setRosterIndex updates sheetRosterIndex`() = runTest {
        val vm = makeVm()
        assertEquals(0, vm.sheetRosterIndex.value)
        vm.setRosterIndex(1)
        assertEquals(1, vm.sheetRosterIndex.value)
    }

    @Test
    fun `empty repo produces empty trainers list`() = runTest {
        val vm = TrainerSelectViewModel(TrainerRepository("""{"regions":[]}"""))
        assertEquals(0, vm.trainers.value.size)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.TrainerSelectViewModelTest" 2>&1 | tail -20
```

Expected: FAIL — `TrainerSelectViewModel` not found.

- [ ] **Step 3: Create `TrainerSelectViewModel.kt`**

Create `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerSelectViewModel.kt`:

```kotlin
package com.madmaxlgndklr.pokedex.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.trainer.TrainerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TrainerSelectViewModel(private val repo: TrainerRepository) : ViewModel() {

    private val _trainers = MutableStateFlow<List<Trainer>>(emptyList())
    val trainers: StateFlow<List<Trainer>> = _trainers

    private val _expandedRegions = MutableStateFlow<Set<String>>(emptySet())
    val expandedRegions: StateFlow<Set<String>> = _expandedRegions

    private val _sheetTrainer = MutableStateFlow<Trainer?>(null)
    val sheetTrainer: StateFlow<Trainer?> = _sheetTrainer

    private val _sheetRosterIndex = MutableStateFlow(0)
    val sheetRosterIndex: StateFlow<Int> = _sheetRosterIndex

    init {
        _trainers.value = repo.getAll()
    }

    fun toggleRegion(region: String) {
        val current = _expandedRegions.value
        _expandedRegions.value = if (region in current) current - region else current + region
    }

    fun openSheet(trainer: Trainer) {
        _sheetTrainer.value = trainer
        _sheetRosterIndex.value = 0
    }

    fun closeSheet() {
        _sheetTrainer.value = null
        _sheetRosterIndex.value = 0
    }

    fun setRosterIndex(index: Int) {
        _sheetRosterIndex.value = index
    }

    companion object {
        fun factory(repo: TrainerRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { TrainerSelectViewModel(repo) } }
    }
}
```

- [ ] **Step 4: Run tests — expect all pass**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:testDebugUnitTest --tests "com.madmaxlgndklr.pokedex.battle.TrainerSelectViewModelTest" 2>&1 | tail -20
```

Expected: 8 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerSelectViewModel.kt \
        app/src/test/java/com/madmaxlgndklr/pokedex/battle/TrainerSelectViewModelTest.kt
git commit -m "feat: add TrainerSelectViewModel with region expand and sheet state"
```

---


### Task 14: BattleHubScreen — add 4th TRAIN tab

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/BattleHubScreen.kt`

No new test file — this is UI wiring; covered end-to-end by compile + manual smoke test in Task 17.

- [ ] **Step 1: Replace `BattleHubScreen.kt` with 4-tab version**

Full file replacement (the only changes are enum, parameter, tab list, and when branch):

```kotlin
package com.madmaxlgndklr.pokedex.ui.battle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark

enum class BattleTab { CALC, BATTLE, MATCHUP, TRAINERS }

@Composable
fun BattleHubScreen(
    calcVm: DamageCalcViewModel,
    battleVm: TurnBattleViewModel,
    matchupVm: MatchupViewModel,
    trainerVm: TrainerSelectViewModel,
    teamIds: List<Int>,
    openOnTab: BattleTab = BattleTab.CALC,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(openOnTab) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sh = maxHeight
        val contentTop = sh * 0.36f

        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(onClick = onBack, modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        Text(
            text = "BATTLE HUB",
            fontFamily = com.madmaxlgndklr.pokedex.ui.theme.PressStart2P,
            fontSize = 8.sp,
            color = CaughtGold,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = sh * 0.22f)
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = contentTop - 28.dp)
                .padding(horizontal = 16.dp)
        ) {
            listOf(
                BattleTab.CALC     to "CALC",
                BattleTab.BATTLE   to "WILD",
                BattleTab.TRAINERS to "TRAIN",
                BattleTab.MATCHUP  to "MATCHUP"
            ).forEach { (tab, label) ->
                val isSelected = selectedTab == tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                        .background(
                            if (isSelected) GlowBlue else PokedexDark.copy(alpha = 0.55f),
                            RoundedCornerShape(3.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) GlowBlue else GlowBlue.copy(alpha = 0.25f),
                            RoundedCornerShape(3.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedTab = tab }
                ) {
                    Text(
                        text = label,
                        fontFamily = com.madmaxlgndklr.pokedex.ui.theme.PressStart2P,
                        fontSize = 4.sp,
                        color = if (isSelected) PokedexDark else PokedexCream
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sh - contentTop - 8.dp)
                .offset(y = contentTop)
                .background(PokedexDark.copy(alpha = 0.55f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        ) {
            when (selectedTab) {
                BattleTab.CALC     -> DamageCalcScreen(viewModel = calcVm)
                BattleTab.BATTLE   -> TurnBattleScreen(viewModel = battleVm, teamIds = teamIds, onBack = onBack)
                BattleTab.MATCHUP  -> MatchupScreen(viewModel = matchupVm, yourTeamIds = teamIds)
                BattleTab.TRAINERS -> TrainerSelectScreen(
                    viewModel = trainerVm,
                    onQuickBattle = { trainer ->
                        val rosterIdx = trainerVm.sheetRosterIndex.value
                        trainerVm.closeSheet()
                        battleVm.startTrainerBattle(trainer, rosterIdx, teamIds)
                        selectedTab = BattleTab.BATTLE
                    },
                    onConfigure = { trainer ->
                        val rosterIdx = trainerVm.sheetRosterIndex.value
                        trainerVm.closeSheet()
                        battleVm.loadTrainerSetup(trainer, rosterIdx, teamIds)
                        selectedTab = BattleTab.BATTLE
                    }
                )
            }
        }
    }
}
```

Note: `rosterIdx` is captured **before** `closeSheet()` because `closeSheet()` resets `sheetRosterIndex` to 0.

- [ ] **Step 2: Verify compile**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:compileDebugKotlin 2>&1 | grep -E "error:|warning:" | head -20
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/BattleHubScreen.kt
git commit -m "feat: add TRAIN tab to BattleHubScreen, relabel WILD/TRAIN/MATCHUP"
```

---


### Task 15: TrainerSelectScreen

**Files:**
- Create: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerSelectScreen.kt`

No unit test for this file — it is pure UI. Verified by compile and device smoke test in Task 17.

- [ ] **Step 1: Create `TrainerSelectScreen.kt`**

```kotlin
package com.madmaxlgndklr.pokedex.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import com.madmaxlgndklr.pokedex.ui.common.TypeBadge
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerSelectScreen(
    viewModel: TrainerSelectViewModel,
    onQuickBattle: (Trainer) -> Unit,
    onConfigure: (Trainer) -> Unit
) {
    val trainers by viewModel.trainers.collectAsState()
    val expandedRegions by viewModel.expandedRegions.collectAsState()
    val sheetTrainer by viewModel.sheetTrainer.collectAsState()
    val sheetRosterIndex by viewModel.sheetRosterIndex.collectAsState()

    val groupedByRegion = remember(trainers) { trainers.groupBy { it.region } }

    if (trainers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "TRAINERS UNAVAILABLE",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = PokedexCream.copy(alpha = 0.5f)
            )
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        groupedByRegion.forEach { (region, regionTrainers) ->
            item(key = "header-$region") {
                RegionHeader(
                    region = region,
                    expanded = region in expandedRegions,
                    onClick = { viewModel.toggleRegion(region) }
                )
            }
            if (region in expandedRegions) {
                items(regionTrainers, key = { it.id }) { trainer ->
                    TrainerCard(trainer = trainer, onClick = { viewModel.openSheet(trainer) })
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    val current = sheetTrainer
    if (current != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSheet() },
            sheetState = sheetState,
            containerColor = PokedexDark
        ) {
            TrainerBottomSheet(
                trainer = current,
                rosterIndex = sheetRosterIndex,
                onRosterIndexChange = { viewModel.setRosterIndex(it) },
                onQuickBattle = { onQuickBattle(current) },
                onConfigure = { onConfigure(current) }
            )
        }
    }
}

@Composable
private fun RegionHeader(region: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = region.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (expanded) "▲" else "▼",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = GlowBlue
        )
    }
}

@Composable
private fun TrainerCard(trainer: Trainer, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PokedexDark.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(56.dp)
                .background(typeColor(trainer.typeSpecialty), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${trainer.name} · ${trainer.title}",
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = PokedexCream
                )
                Spacer(Modifier.width(6.dp))
                TypeBadge(type = trainer.typeSpecialty)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                trainer.rosters[0].team.forEach { tp ->
                    AsyncImage(
                        model = RetrofitClient.spriteUrl(tp.pokemonId),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun TrainerBottomSheet(
    trainer: Trainer,
    rosterIndex: Int,
    onRosterIndexChange: (Int) -> Unit,
    onQuickBattle: () -> Unit,
    onConfigure: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Text(
            text = trainer.name.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 9.sp,
            color = PokedexCream
        )
        Text(
            text = "${trainer.title} · ${trainer.region}",
            fontFamily = PressStart2P,
            fontSize = 5.sp,
            color = PokedexCream.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Roster toggle — only shown when trainer has 2 rosters
        if (trainer.rosters.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trainer.rosters.forEachIndexed { idx, roster ->
                    val selected = idx == rosterIndex
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(
                                if (selected) GlowBlue else PokedexDark.copy(alpha = 0.4f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onRosterIndexChange(idx) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = roster.label,
                            fontFamily = PressStart2P,
                            fontSize = 4.sp,
                            color = if (selected) PokedexDark else PokedexCream
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // Team preview
        val roster = trainer.rosters[rosterIndex]
        roster.team.forEach { tp ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                AsyncImage(
                    model = RetrofitClient.spriteUrl(tp.pokemonId),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "#${tp.pokemonId}",
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = PokedexCream,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Lv.${tp.level}",
                    fontFamily = PressStart2P,
                    fontSize = 5.sp,
                    color = GlowBlue
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Button(
            onClick = onQuickBattle,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PokedexRed)
        ) {
            Text("QUICK BATTLE", fontFamily = PressStart2P, fontSize = 6.sp, color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onConfigure,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowBlue),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GlowBlue)
        ) {
            Text("CONFIGURE", fontFamily = PressStart2P, fontSize = 6.sp, color = GlowBlue)
        }

        Spacer(Modifier.height(24.dp))
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:compileDebugKotlin 2>&1 | grep -E "error:|warning:" | head -20
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TrainerSelectScreen.kt
git commit -m "feat: add TrainerSelectScreen with region list and trainer bottom sheet"
```

---


### Task 16: AppNavigation wiring + BattleSetupView opponent display

**Files:**
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt`

No unit test — wiring; covered by compile and device smoke test in Task 17.

---

#### Part A: AppNavigation.kt

- [ ] **Step 1: Add imports to AppNavigation.kt**

After the existing `import com.madmaxlgndklr.pokedex.ui.battle.TurnBattleViewModel` line, add:

```kotlin
import com.madmaxlgndklr.pokedex.data.trainer.TrainerRepository
import com.madmaxlgndklr.pokedex.ui.battle.TrainerSelectViewModel
```

- [ ] **Step 2: Add TrainerRepository and TrainerSelectViewModel inside the BATTLE composable**

Inside the `composable(route = Routes.BATTLE)` block, after the existing `val matchupVm: MatchupViewModel = viewModel(...)` declaration, add:

```kotlin
val trainerRepo = remember { TrainerRepository(context) }
val trainerVm: TrainerSelectViewModel = viewModel(
    factory = TrainerSelectViewModel.factory(trainerRepo)
)
```

`context` is already in scope from `val context = LocalContext.current` at the top of `AppNavigation`.

- [ ] **Step 3: Pass trainerVm to BattleHubScreen**

Change the `BattleHubScreen(...)` call from:

```kotlin
BattleHubScreen(
    calcVm = calcVm,
    battleVm = battleVm,
    matchupVm = matchupVm,
    teamIds = teamIds.toList(),
    onBack = { navController.popBackStack() }
)
```

to:

```kotlin
BattleHubScreen(
    calcVm = calcVm,
    battleVm = battleVm,
    matchupVm = matchupVm,
    trainerVm = trainerVm,
    teamIds = teamIds.toList(),
    onBack = { navController.popBackStack() }
)
```

---

#### Part B: TurnBattleScreen.kt — collect battleTrainer and update BattleSetupView

- [ ] **Step 4: Add TypeBadge import to TurnBattleScreen.kt**

After the existing `import com.madmaxlgndklr.pokedex.ui.common.CryPlayer` line, add:

```kotlin
import com.madmaxlgndklr.pokedex.ui.common.TypeBadge
```

- [ ] **Step 5: Collect battleTrainer in TurnBattleScreen composable**

In the `TurnBattleScreen` composable, after the existing state collections at lines ~61-63:

```kotlin
val setup by viewModel.setup.collectAsState()
val battleState by viewModel.battleState.collectAsState()
val isLoading by viewModel.isLoading.collectAsState()
```

add:

```kotlin
val battleTrainer by viewModel.battleTrainer.collectAsState()
```

- [ ] **Step 6: Pass battleTrainer to BattleSetupView call**

Change the `BattleSetupView(...)` call (in the `setup != null` branch) from:

```kotlin
BattleSetupView(
    setup = s,
    teamIds = teamIds,
    moves = learnableMoves(s.playerDetail, s.level),
    viewModel = viewModel,
    onLevelChange = { viewModel.setSetupLevel(it) },
    onToggleMove = { viewModel.toggleSetupMove(it) },
    onFight = { viewModel.startBattleFromSetup(teamIds) }
)
```

to:

```kotlin
BattleSetupView(
    setup = s,
    battleTrainer = battleTrainer,
    teamIds = teamIds,
    moves = learnableMoves(s.playerDetail, s.level),
    viewModel = viewModel,
    onLevelChange = { viewModel.setSetupLevel(it) },
    onToggleMove = { viewModel.toggleSetupMove(it) },
    onFight = { viewModel.startBattleFromSetup(teamIds) }
)
```

- [ ] **Step 7: Add battleTrainer parameter to BattleSetupView signature**

Change the `BattleSetupView` function signature from:

```kotlin
private fun BattleSetupView(
    setup: BattleSetup,
    teamIds: List<Int>,
    moves: List<LearnableMove>,
    viewModel: TurnBattleViewModel,
    onLevelChange: (Int) -> Unit,
    onToggleMove: (String) -> Unit,
    onFight: () -> Unit
)
```

to:

```kotlin
private fun BattleSetupView(
    setup: BattleSetup,
    battleTrainer: SelectedTrainer?,
    teamIds: List<Int>,
    moves: List<LearnableMove>,
    viewModel: TurnBattleViewModel,
    onLevelChange: (Int) -> Unit,
    onToggleMove: (String) -> Unit,
    onFight: () -> Unit
)
```

- [ ] **Step 8: Add opponent row inside BattleSetupView**

In the `BattleSetupView` `Column` body, immediately after the closing `}` of the player Pokémon + level `Row` (the row that contains `AsyncImage` + `setup.playerDetail.name.uppercase()` + `LevelPicker`), add:

```kotlin
// Opponent slot
Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .fillMaxWidth()
        .background(PokedexDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
        .padding(8.dp)
) {
    Text(
        "VS.",
        fontFamily = PressStart2P,
        fontSize = 5.sp,
        color = PokedexCream.copy(alpha = 0.4f)
    )
    Spacer(Modifier.width(8.dp))
    if (battleTrainer != null) {
        Text(
            battleTrainer.trainer.name.uppercase(),
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream,
            modifier = Modifier.weight(1f)
        )
        TypeBadge(type = battleTrainer.trainer.typeSpecialty)
    } else {
        Text(
            "RANDOM OPPONENT",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f)
        )
    }
}
```

---

- [ ] **Step 9: Verify compile**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:compileDebugKotlin 2>&1 | grep -E "error:" | head -20
```

Expected: no errors.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/madmaxlgndklr/pokedex/ui/navigation/AppNavigation.kt \
        app/src/main/java/com/madmaxlgndklr/pokedex/ui/battle/TurnBattleScreen.kt
git commit -m "feat: wire TrainerSelectViewModel into AppNavigation, show opponent in BattleSetupView"
```

---


### Task 17: Full test sweep, compile verification, and plan commit

**Files:** No new files. Verification only.

- [ ] **Step 1: Run the full unit test suite**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:testDebugUnitTest 2>&1 | tail -30
```

Expected output includes:

```
> Task :app:testDebugUnitTest
...
BUILD SUCCESSFUL
```

All previously passing tests must still pass. The new tests from Tasks 10, 12, and 13 must pass:
- `TrainerRepositoryTest` — 5 tests
- `TrainerBattleViewModelTest` — 7 tests
- `TrainerSelectViewModelTest` — 8 tests

If any test fails, fix it before proceeding.

- [ ] **Step 2: Run only the new trainer test classes to confirm**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:testDebugUnitTest \
    --tests "com.madmaxlgndklr.pokedex.battle.TrainerRepositoryTest" \
    --tests "com.madmaxlgndklr.pokedex.battle.TrainerBattleViewModelTest" \
    --tests "com.madmaxlgndklr.pokedex.battle.TrainerSelectViewModelTest" \
    2>&1 | tail -20
```

Expected: 20 tests PASSED total.

- [ ] **Step 3: Full debug APK compile**

```bash
JAVA_HOME=/home/madmaxlgndklr/Android/android-studio/jbr \
  ./gradlew :app:assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` with no errors.

- [ ] **Step 4: Smoke test on device or emulator**

Install and verify these flows manually:

1. **TRAIN tab visible** — open Battle Hub, confirm four tabs: CALC / WILD / TRAIN / MATCHUP
2. **Region list** — tap TRAIN tab, confirm regions expand/collapse correctly
3. **Trainer card** — confirm sprite row, name/title, type badge render for at least one card
4. **Bottom sheet opens** — tap a trainer card, sheet appears with team preview
5. **Roster toggle** — for a Gen 1/2 gym leader (e.g., Brock), toggle between Original/Rematch; confirm team preview updates
6. **Quick Battle** — tap QUICK BATTLE on any trainer, confirm battle starts immediately against their team
7. **Configure** — tap CONFIGURE on any trainer, confirm setup screen shows trainer name in opponent slot (not "RANDOM OPPONENT")
8. **Wild battle unaffected** — switch to WILD tab, start a battle, confirm random opponent (no trainer name shown)
9. **Rematch after trainer battle** — complete a trainer battle, tap REMATCH, confirm returns to setup with trainer name still shown
10. **Back navigation** — confirm back button works from all tabs

- [ ] **Step 5: Commit the completed plan file**

```bash
git add docs/superpowers/plans/2026-05-28-trainer-battles.md
git commit -m "docs: complete trainer battles implementation plan (Tasks 1-17)"
```

---

## Implementation Complete

All 17 tasks produce the full trainer battles feature:

| Task | Deliverable |
|------|-------------|
| 1 | `TrainerRoster.kt` — data model |
| 2 | `TrainerRepository.kt` (Android) + dual constructor |
| 3 | `TrainerRepositoryTest.kt` |
| 4 | `trainers.json` — Kanto (13 trainers) |
| 5 | `trainers.json` — Johto (13 trainers) |
| 6 | `trainers.json` — Hoenn (13 trainers) |
| 7 | `trainers.json` — Sinnoh (13 trainers) |
| 8 | `trainers.json` — Unova (13 trainers) |
| 9 | `trainers.json` — Kalos + Alola + Galar + Paldea |
| 10 | `trainers.json` validation + wiring |
| 11 | `trainers.json` complete with all regions |
| 12 | `TurnBattleViewModel` — `SelectedTrainer`, `battleTrainer`, `loadTrainerSetup`, `startTrainerBattle`, modified `resetToSetup` / `startBattleFromSetup` |
| 13 | `TrainerSelectViewModel` + tests |
| 14 | `BattleHubScreen` — TRAIN tab, 4-tab row |
| 15 | `TrainerSelectScreen` — region list + bottom sheet |
| 16 | `AppNavigation` wiring + `BattleSetupView` opponent display |
| 17 | Full test sweep + compile + smoke test |

