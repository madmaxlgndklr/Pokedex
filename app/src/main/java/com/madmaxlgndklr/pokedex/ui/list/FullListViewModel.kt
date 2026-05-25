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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FullListViewModel(private val repository: PokemonRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<PokemonSummary>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<PokemonSummary>>> = _uiState

    val caughtIds: StateFlow<Set<Int>> = repository.getCaughtPokemon()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        viewModelScope.launch {
            _uiState.value = try {
                UiState.Success(repository.getPokemonList())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    fun toggleCaught(id: Int, name: String) {
        viewModelScope.launch {
            repository.setCaught(id, name, id !in caughtIds.value)
        }
    }

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { FullListViewModel(repository) }
        }
    }
}
