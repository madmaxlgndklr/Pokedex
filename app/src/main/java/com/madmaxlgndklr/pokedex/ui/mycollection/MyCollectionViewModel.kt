package com.madmaxlgndklr.pokedex.ui.mycollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.DexSelection
import com.madmaxlgndklr.pokedex.ui.common.Generation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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

    private val _nameQuery = MutableStateFlow("")
    val nameQuery: StateFlow<String> = _nameQuery

    val selectedDex = MutableStateFlow<DexSelection>(DexSelection.National)

    val caughtList: StateFlow<List<PokemonSummary>> =
        combine(_allCaught, _selectedGens, _selectedTypes, _nameQuery, selectedDex) { list, gens, types, query, dex ->
            var filtered = when (dex) {
                is DexSelection.Regional -> list.filter { p -> p.id in dex.gen.idRange }
                is DexSelection.National -> list
            }
            if (gens.isNotEmpty()) filtered = filtered.filter { p -> gens.any { p.id in it.idRange } }
            if (types.isNotEmpty()) {
                val cachedTypesMap = filtered.associate { p -> p.id to repository.getCachedTypes(p.id) }
                filtered = filtered.filter { p ->
                    val cached = cachedTypesMap[p.id]
                    cached == null || cached.any { it in types }
                }
            }
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                filtered = filtered.filter { p ->
                    p.name.contains(q, ignoreCase = true) || p.id.toString() == q
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

    val selectedDexStats: StateFlow<Pair<Int, Int>> =
        combine(_allCaught, selectedDex) { caught, dex ->
            val caughtIds = caught.map { it.id }.toSet()
            when (dex) {
                is DexSelection.National -> caughtIds.count { it in 1..1025 } to 1025
                is DexSelection.Regional -> caughtIds.count { it in dex.gen.idRange } to dex.gen.idRange.count()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDexDescription: StateFlow<String?> = selectedDex
        .flatMapLatest { dex ->
            flow { emit(repository.getDexDescription(dex.pokedexName)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    fun setNameQuery(q: String) { _nameQuery.value = q }

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { MyCollectionViewModel(repository) }
        }
    }
}
