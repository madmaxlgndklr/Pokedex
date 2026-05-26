package com.madmaxlgndklr.pokedex.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    object NotFound : SearchUiState()
}

class SearchViewModel(private val repository: PokemonRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    private val _navigationEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<Int> = _navigationEvent

    private val _cacheIsEmpty = MutableStateFlow(false)
    val cacheIsEmpty: StateFlow<Boolean> = _cacheIsEmpty

    var animationCompleted: Boolean = false
        private set

    var bootPromptAcknowledged: Boolean = false
        private set

    init {
        viewModelScope.launch {
            val (detailCount, _) = repository.getCachedCount()
            _cacheIsEmpty.value = detailCount == 0
        }
    }

    fun markAnimationCompleted() { animationCompleted = true }

    fun acknowledgeBootPrompt() { bootPromptAcknowledged = true }

    fun onQueryChange(query: String) {
        _query.value = query
        if (_uiState.value !is SearchUiState.Idle) _uiState.value = SearchUiState.Idle
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty() || _uiState.value is SearchUiState.Loading) return
        _uiState.value = SearchUiState.Loading
        viewModelScope.launch {
            try {
                val detail = repository.searchPokemon(q)
                _query.value = ""
                _uiState.value = SearchUiState.Idle
                _navigationEvent.emit(detail.id)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.NotFound
            }
        }
    }

    companion object {
        fun factory(repository: PokemonRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SearchViewModel(repository) }
        }
    }
}
