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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class GenCompletion(val gen: Generation, val caught: Int, val total: Int)

class MyCollectionViewModel(private val repository: PokemonRepository) : ViewModel() {
    private val _allCaught: StateFlow<List<PokemonSummary>> = repository.getCaughtPokemon()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedGens = MutableStateFlow<Set<Generation>>(emptySet())
    val selectedGens: StateFlow<Set<Generation>> = _selectedGens

    private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes

    val caughtList: StateFlow<List<PokemonSummary>> =
        combine(_allCaught, _selectedGens, _selectedTypes) { list, gens, types ->
            var filtered = if (gens.isEmpty()) list else list.filter { p -> gens.any { p.id in it.idRange } }
            if (types.isNotEmpty()) {
                val cachedTypesMap = filtered.associate { p -> p.id to repository.getCachedTypes(p.id) }
                filtered = filtered.filter { p ->
                    val cached = cachedTypesMap[p.id]
                    cached == null || cached.any { it in types }
                }
            }
            filtered.sortedBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completionStats: StateFlow<List<GenCompletion>> = _allCaught.map { caught ->
        val caughtIds = caught.map { it.id }.toSet()
        Generation.entries.map { gen ->
            GenCompletion(gen, caughtIds.count { it in gen.idRange }, gen.idRange.count())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleGeneration(gen: Generation) {
        _selectedGens.value = _selectedGens.value.toMutableSet().apply {
            if (!add(gen)) remove(gen)
        }
    }

    fun clearGenerations() { _selectedGens.value = emptySet() }

    fun toggleType(type: String) {
        _selectedTypes.value = _selectedTypes.value.toMutableSet().apply {
            if (!add(type)) remove(type)
        }
    }

    fun clearTypes() { _selectedTypes.value = emptySet() }

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { MyCollectionViewModel(repository) }
        }
    }
}
