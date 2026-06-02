package com.madmaxlgndklr.pokedex.ui.common

import android.net.Uri
import androidx.media3.common.MediaItem

object RbyPlaylist {
    private const val BASE = "https://madmaxlgndklrpokeapi.com/assets/rby_music"

    private const val INTRO = "02. Opening Movie - Stereo (Red, Green & Blue Version).mp3"
    private const val TITLE = "03 Title Screen.mp3"

    private val POOL = listOf(
        "04 Pallet Town.mp3",
        "05 Professor Oak.mp3",
        "06. Hurry Along.mp3",
        "07. Pokémon Lab.mp3",
        "08. Pokémon Obtained!.mp3",
        "09. Rival Appears!.mp3",
        "10. Battle! (Trainer Battle).mp3",
        "11. Level Up!.mp3",
        "12. Victory! (Trainer Battle).mp3",
        "13 Route 1.mp3",
        "14 Battle! (Wild Pokémon).mp3",
        "15 Victory! (Wild Pokémon).mp3",
        "16. Item Obtained!.mp3",
        "17 Viridian City.mp3",
        "18 Pokémon Center.mp3",
        "19. Pokémon Healed.mp3",
        "20. Pokémon Caught!.mp3",
        "21. Traded Pokémon Received!.mp3",
        "22 Viridian Forest.mp3",
        "23. A Trainer Appears (Boy Version).mp3",
        "24 Jigglypuff's Song.mp3",
        "25. Professor Oak's Evaluation!.mp3",
        "26 Evolution.mp3",
        "27 Pokémon Gym.mp3",
        "28. Battle! (Gym Leader).mp3",
        "29. Victory! (Gym Leader).mp3",
        "30 Route 3.mp3",
        "31. A Trainer Appears (Girl Version).mp3",
        "32 Mt. Moon.mp3",
        "33. A Trainer Appears (Bad Guy Version).mp3",
        "34 Cerulean City.mp3",
        "35 Route 24 - Welcome to the World of Pokémon!.mp3",
        "36 Vermilion City.mp3",
        "37. S.S. Anne.mp3",
        "38. Bicycle.mp3",
        "39 Route 11.mp3",
        "40 Lavender Town.mp3",
        "41 Celadon City.mp3",
        "42 Rocket Game Corner.mp3",
        "43 Rocket Hideout.mp3",
        "44 Sylph Co..mp3",
        "45 Pokémon Tower.mp3",
        "46 Poké Flute.mp3",
        "47 Surf.mp3",
        "48 Cinnabar Island.mp3",
        "49 Pokémon Mansion.mp3",
        "50 Victory Road.mp3",
        "51 Final Battle! (Rival).mp3",
        "52 Hall of Fame.mp3",
        "53. Ending Theme.mp3",
        "54. Unused Song.mp3",
        "55. Opening Movie (Yellow Version).mp3",
        "56 Printer Menu.mp3",
        "57. A Trainer Appears (Rocket Duo Version).mp3",
        "58 Pikachu's Beach.mp3"
    )

    fun buildItems(): List<MediaItem> {
        val ordered = listOf(INTRO, TITLE) + POOL.shuffled()
        return ordered.map { filename ->
            MediaItem.fromUri(Uri.parse("$BASE/${Uri.encode(filename)}"))
        }
    }
}
