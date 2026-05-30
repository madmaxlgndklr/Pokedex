package com.madmaxlgndklr.pokedex.battle

import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.data.local.HeldItemDao
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemAttributeResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemDetailResponse
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemResultDto
import com.madmaxlgndklr.pokedex.data.remote.dto.ItemNameDto
import com.madmaxlgndklr.pokedex.data.remote.dto.NamedDto
import com.madmaxlgndklr.pokedex.data.repository.HeldItemRepository
import com.madmaxlgndklr.pokedex.repository.FakePokeApiService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun fakeAttribute(vararg names: String) = ItemAttributeResponse(
    items = names.map { ItemResultDto(it, "https://pokeapi.co/api/v2/item/$it/") }
)

private fun fakeDetail(name: String, id: Int) = ItemDetailResponse(
    id = id,
    name = name,
    names = listOf(ItemNameDto(name.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }, NamedDto("en")))
)

open class FakeHeldItemApiService : FakePokeApiService() {
    override suspend fun getItemAttribute(name: String): ItemAttributeResponse = when (name) {
        "holdable-active"  -> fakeAttribute("choice-band", "life-orb")
        "holdable-passive" -> fakeAttribute("leftovers", "choice-band")  // choice-band in both
        else               -> ItemAttributeResponse(emptyList())
    }

    override suspend fun getItem(name: String) = when (name) {
        "choice-band" -> fakeDetail("choice-band", 1)
        "life-orb"    -> fakeDetail("life-orb", 247)
        "leftovers"   -> fakeDetail("leftovers", 234)
        else          -> throw IllegalArgumentException("Unknown item: $name")
    }
}

class FakeHeldItemDao : HeldItemDao {
    val stored = mutableListOf<HeldItem>()

    override suspend fun getAll() = stored.toList()

    override suspend fun upsertAll(items: List<HeldItem>) {
        items.forEach { new ->
            stored.removeAll { it.id == new.id }
            stored.add(new)
        }
    }

    override suspend fun deleteAll() {
        stored.clear()
    }
}

class HeldItemRepositoryTest {

    @Test
    fun `syncAll fetches from both attributes and deduplicates`() = runTest {
        val dao = FakeHeldItemDao()
        val repo = HeldItemRepository(FakeHeldItemApiService(), dao)
        repo.syncAll()
        val names = dao.stored.map { it.name }
        assertEquals(3, names.size)
        assertTrue("choice-band" in names)
        assertTrue("life-orb" in names)
        assertTrue("leftovers" in names)
    }

    @Test
    fun `syncAll upserts items into DAO`() = runTest {
        val dao = FakeHeldItemDao()
        val repo = HeldItemRepository(FakeHeldItemApiService(), dao)
        repo.syncAll()
        assertTrue(dao.stored.isNotEmpty())
    }

    @Test
    fun `syncAll clears old data before inserting`() = runTest {
        val dao = FakeHeldItemDao()
        dao.stored.add(HeldItem(999, "stale-item", "Stale", "Old data"))
        val repo = HeldItemRepository(FakeHeldItemApiService(), dao)
        repo.syncAll()
        assertTrue("stale item should be gone", dao.stored.none { it.id == 999 })
    }

    @Test
    fun `syncAll throws when API fails`() = runTest {
        val failingApi = object : FakeHeldItemApiService() {
            override suspend fun getItemAttribute(name: String): ItemAttributeResponse {
                throw RuntimeException("Network error")
            }
        }
        val dao = FakeHeldItemDao()
        val repo = HeldItemRepository(failingApi, dao)
        var threw = false
        try { repo.syncAll() } catch (_: Exception) { threw = true }
        assertTrue(threw)
    }
}
