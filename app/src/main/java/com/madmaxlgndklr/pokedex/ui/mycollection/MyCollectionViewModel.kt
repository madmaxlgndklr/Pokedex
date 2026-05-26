package com.madmaxlgndklr.pokedex.ui.mycollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.Generation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MyCollectionViewModel(repository: PokemonRepository) : ViewModel() {
    private val _allCaught: StateFlow<List<PokemonSummary>> = repository.getCaughtPokemon()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedGens = MutableStateFlow<Set<Generation>>(emptySet())
    val selectedGens: StateFlow<Set<Generation>> = _selectedGens

    val caughtList: StateFlow<List<PokemonSummary>> =
        combine(_allCaught, _selectedGens) { list, gens ->
            val filtered = if (gens.isEmpty()) list
                           else list.filter { p -> gens.any { p.id in it.idRange } }
            filtered.sortedBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleGeneration(gen: Generation) {
        _selectedGens.value = _selectedGens.value.toMutableSet().apply {
            if (!add(gen)) remove(gen)
        }
    }

    fun clearGenerations() {
        _selectedGens.value = emptySet()
    }

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { MyCollectionViewModel(repository) }
        }
    }
}
