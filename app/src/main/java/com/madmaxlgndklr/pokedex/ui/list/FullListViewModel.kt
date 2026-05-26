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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Generation(val label: String, val idRange: IntRange) {
    KANTO("KANTO",  1..151),
    JOHTO("JOHTO",  152..251),
    HOENN("HOENN",  252..386),
    SINNOH("SINNOH", 387..493),
    UNOVA("UNOVA",  494..649),
    KALOS("KALOS",  650..721),
    ALOLA("ALOLA",  722..809),
    GALAR("GALAR",  810..905),
    PALDEA("PALDEA", 906..1025)
}

class FullListViewModel(private val repository: PokemonRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<PokemonSummary>>>(UiState.Loading)

    private val _selectedGen = MutableStateFlow<Generation?>(null)
    val selectedGen: StateFlow<Generation?> = _selectedGen

    val filteredState: StateFlow<UiState<List<PokemonSummary>>> =
        combine(_uiState, _selectedGen) { state, gen ->
            when {
                state is UiState.Success && gen != null ->
                    UiState.Success(state.data.filter { it.id in gen.idRange })
                else -> state
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

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

    fun selectGeneration(gen: Generation?) {
        _selectedGen.value = if (_selectedGen.value == gen) null else gen
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
