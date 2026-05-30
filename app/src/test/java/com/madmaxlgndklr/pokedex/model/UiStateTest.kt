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
