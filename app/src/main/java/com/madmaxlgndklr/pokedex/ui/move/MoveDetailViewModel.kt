package com.madmaxlgndklr.pokedex.ui.move

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.Move
import com.madmaxlgndklr.pokedex.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MoveDetailViewModel(
    private val repo: PokemonRepository,
    private val moveName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Move>>(UiState.Loading)
    val uiState: StateFlow<UiState<Move>> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                _uiState.value = UiState.Success(repo.getMove(moveName))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        fun factory(repo: PokemonRepository, moveName: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MoveDetailViewModel(repo, moveName) as T
            }
    }
}
