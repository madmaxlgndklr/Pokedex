package com.madmaxlgndklr.pokedex.sync

import com.madmaxlgndklr.pokedex.data.remote.MergeUtils
import com.madmaxlgndklr.pokedex.data.remote.RemoteTrainerRow
import com.madmaxlgndklr.pokedex.data.remote.RemoteWildRow
import com.madmaxlgndklr.pokedex.data.local.TrainerRecord
import com.madmaxlgndklr.pokedex.data.local.WildRecord
import org.junit.Test
import org.junit.Assert.*

class SyncMergeTest {

    @Test
    fun `mergeCaughtPokemon unions local and remote`() {
        val local = listOf(1, 2, 3)
        val remote = listOf(3, 4, 5)
        val result = MergeUtils.mergeCaughtPokemon(local, remote)
        assertEquals(setOf(1, 2, 3, 4, 5), result.toSet())
    }

    @Test
    fun `mergeTeam picks remote when newer`() {
        val result = MergeUtils.mergeTeam(listOf(1, 2), 100L, listOf(3, 4), 200L)
        assertEquals(listOf(3, 4), result)
    }

    @Test
    fun `mergeTeam picks local when newer`() {
        val result = MergeUtils.mergeTeam(listOf(1, 2), 300L, listOf(3, 4), 200L)
        assertEquals(listOf(1, 2), result)
    }

    @Test
    fun `mergeTrainerRecords uses max wins and losses`() {
        val local = listOf(TrainerRecord("t1", "Brock", "Gym Leader", "Kanto", "GymLeader", "Rock", wins = 5, losses = 2, lastBattledAt = 100))
        val remote = listOf(RemoteTrainerRow("t1", "Brock", "Gym Leader", "Kanto", "GymLeader", "Rock", wins = 3, losses = 4, firstDefeatedAt = 50L, lastBattledAt = 200))
        val result = MergeUtils.mergeTrainerRecords(local, remote)
        assertEquals(1, result.size)
        assertEquals(5, result[0].wins)
        assertEquals(4, result[0].losses)
        assertEquals(200L, result[0].lastBattledAt)
    }

    @Test
    fun `mergeWildRecords uses max wins and losses`() {
        val local = listOf(WildRecord(25, "pikachu", wins = 10, losses = 1, lastBattledAt = 100))
        val remote = listOf(RemoteWildRow(25, "pikachu", wins = 8, losses = 3, lastBattledAt = 200))
        val result = MergeUtils.mergeWildRecords(local, remote)
        assertEquals(1, result.size)
        assertEquals(10, result[0].wins)
        assertEquals(3, result[0].losses)
    }

    @Test
    fun `mergeSettings picks remote when newer`() {
        val local = MergeUtils.LocalSettings(5, false, "ASH", 100L)
        val remote = MergeUtils.RemoteSettings(3, true, "MISTY", 200L)
        val result = MergeUtils.mergeSettings(local, remote)
        assertEquals("MISTY", result.trainerName)
        assertEquals(3, result.generation)
    }

    @Test
    fun `mergeSettings picks local when newer`() {
        val local = MergeUtils.LocalSettings(5, false, "ASH", 300L)
        val remote = MergeUtils.RemoteSettings(3, true, "MISTY", 200L)
        val result = MergeUtils.mergeSettings(local, remote)
        assertEquals("ASH", result.trainerName)
    }
}
