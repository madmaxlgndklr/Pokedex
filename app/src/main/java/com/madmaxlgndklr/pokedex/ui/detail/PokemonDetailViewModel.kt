package com.madmaxlgndklr.pokedex.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PokemonDetailViewModel(
    private val repository: PokemonRepository,
    private val pokemonId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<PokemonDetail>>(UiState.Loading)
    val uiState: StateFlow<UiState<PokemonDetail>> = _uiState

    val isCaught: StateFlow<Boolean> = repository.isCaught(pokemonId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var currentDetail: PokemonDetail? = null

    init {
        viewModelScope.launch {
            _uiState.value = try {
                val detail = repository.getPokemonDetail(pokemonId)
                currentDetail = detail
                UiState.Success(detail)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Failed to load Pokémon")
            }
        }
    }

    fun toggleCaught() {
        val detail = currentDetail ?: return
        val wasCaught = isCaught.value
        viewModelScope.launch {
            repository.setCaught(id = detail.id, name = detail.name, caught = !wasCaught)
        }
    }

    companion object {
        fun factory(repository: PokemonRepository, pokemonId: Int): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { PokemonDetailViewModel(repository, pokemonId) }
            }
    }
}
