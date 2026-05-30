package com.madmaxlgndklr.pokedex.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CompareViewModel(
    private val repository: PokemonRepository,
    private val firstId: Int
) : ViewModel() {

    private val _firstState = MutableStateFlow<UiState<PokemonDetail>>(UiState.Loading)
    val firstState: StateFlow<UiState<PokemonDetail>> = _firstState

    private val _secondState = MutableStateFlow<UiState<PokemonDetail>?>(null)
    val secondState: StateFlow<UiState<PokemonDetail>?> = _secondState

    val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _firstState.value = try {
                UiState.Success(repository.getPokemonDetail(firstId))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Failed")
            }
        }
    }

    fun searchSecond() {
        val q = searchQuery.value.trim()
        if (q.isEmpty()) return
        _secondState.value = UiState.Loading
        viewModelScope.launch {
            _secondState.value = try {
                UiState.Success(repository.searchPokemon(q))
            } catch (e: Exception) {
                UiState.Error("Not found")
            }
        }
    }

    companion object {
        fun factory(repository: PokemonRepository, firstId: Int): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { CompareViewModel(repository, firstId) }
            }
    }
}
