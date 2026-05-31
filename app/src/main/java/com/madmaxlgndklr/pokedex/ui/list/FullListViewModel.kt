package com.madmaxlgndklr.pokedex.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import com.madmaxlgndklr.pokedex.ui.common.DexSelection
import com.madmaxlgndklr.pokedex.ui.common.Generation
import com.madmaxlgndklr.pokedex.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { NUMBER, NAME }

class FullListViewModel(private val repository: PokemonRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<PokemonSummary>>>(UiState.Loading)

    private val _selectedGens = MutableStateFlow<Set<Generation>>(emptySet())
    val selectedGens: StateFlow<Set<Generation>> = _selectedGens

    private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes

    private val _sortOrder = MutableStateFlow(SortOrder.NUMBER)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _nameQuery = MutableStateFlow("")
    val nameQuery: StateFlow<String> = _nameQuery

    val selectedDex = MutableStateFlow<DexSelection>(DexSelection.National)

    private val _filteredBase: StateFlow<UiState<List<PokemonSummary>>> =
        combine(_uiState, _selectedGens, _selectedTypes, _sortOrder, _nameQuery) { state, gens, types, sort, query ->
            when (state) {
                is UiState.Success -> {
                    var list = state.data
                    if (gens.isNotEmpty()) list = list.filter { p -> gens.any { p.id in it.idRange } }
                    if (types.isNotEmpty()) {
                        val cachedTypesMap = list.associate { p -> p.id to repository.getCachedTypes(p.id) }
                        list = list.filter { p ->
                            val cached = cachedTypesMap[p.id]
                            cached == null || cached.any { it in types }
                        }
                    }
                    if (query.isNotBlank()) {
                        val q = query.trim().lowercase()
                        list = list.filter { p ->
                            p.name.contains(q, ignoreCase = true) || p.id.toString() == q
                        }
                    }
                    list = when (sort) {
                        SortOrder.NUMBER -> list.sortedBy { it.id }
                        SortOrder.NAME   -> list.sortedBy { it.name }
                    }
                    UiState.Success(list)
                }
                else -> state
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val filteredState: StateFlow<UiState<List<PokemonSummary>>> =
        combine(_filteredBase, selectedDex) { state, dex ->
            when (state) {
                is UiState.Success -> {
                    val list = when (dex) {
                        is DexSelection.National -> state.data
                        is DexSelection.Regional -> state.data.filter { it.id in dex.gen.idRange }
                    }
                    UiState.Success(list)
                }
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

    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }

    fun setNameQuery(q: String) { _nameQuery.value = q }

    fun setSelectedDex(dex: DexSelection) { selectedDex.value = dex }

    fun toggleCaught(id: Int, name: String) {
        viewModelScope.launch {
            repository.setCaught(id, name, id !in caughtIds.value)
        }
    }

    fun selectAllVisible() {
        val state = filteredState.value
        if (state !is UiState.Success) return
        val visible = state.data
        val caught = caughtIds.value
        val allCaught = visible.isNotEmpty() && visible.all { it.id in caught }
        viewModelScope.launch {
            if (allCaught) {
                visible.forEach { repository.setCaught(it.id, it.name, false) }
            } else {
                visible.filter { it.id !in caught }.forEach { repository.setCaught(it.id, it.name, true) }
            }
        }
    }

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { FullListViewModel(repository) }
        }
    }
}
