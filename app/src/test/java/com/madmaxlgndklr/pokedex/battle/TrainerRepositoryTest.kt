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
