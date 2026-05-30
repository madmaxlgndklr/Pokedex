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
