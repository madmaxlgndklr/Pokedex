package com.madmaxlgndklr.pokedex.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: PokemonRepository) : ViewModel() {
    private val allPokemon = mutableListOf<PokemonSummary>()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes

    private val _filteredList = MutableStateFlow<List<PokemonSummary>>(emptyList())
    val filteredList: StateFlow<List<PokemonSummary>> = _filteredList

    init {
        viewModelScope.launch {
            try {
                allPokemon.addAll(repository.getPokemonList())
                applyFilter()
            } catch (_: Exception) {}
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
        applyFilter()
    }

    fun onTypeToggle(type: String) {
        _selectedTypes.value = _selectedTypes.value.toMutableSet().apply {
            if (contains(type)) remove(type) else add(type)
        }
        applyFilter()
    }

    private fun applyFilter() {
        val q = _query.value.lowercase()
        _filteredList.value = allPokemon.filter { pokemon ->
            (q.isEmpty() || pokemon.name.contains(q))
        }
    }

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SearchViewModel(repository) }
        }
    }
}
