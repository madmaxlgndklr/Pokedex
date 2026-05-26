package com.madmaxlgndklr.pokedex.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val completed: Int, val total: Int) : SyncState()
    data class Done(val cached: Int, val total: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val pokemonRepo: PokemonRepository
) : ViewModel() {

    val musicOnLaunch: StateFlow<Boolean> = settingsRepo.musicOnLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    fun setMusicOnLaunch(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setMusicOnLaunch(enabled) }
    }

    fun syncAll() {
        if (_syncState.value is SyncState.Syncing) return
        viewModelScope.launch {
            try {
                pokemonRepo.syncAll { completed, total ->
                    _syncState.value = SyncState.Syncing(completed, total)
                }
                val (cached, listSize) = pokemonRepo.getCachedCount()
                _syncState.value = SyncState.Done(cached, listSize)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    companion object {
        fun factory(
            settingsRepo: SettingsRepository,
            pokemonRepo: PokemonRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(settingsRepo, pokemonRepo) }
        }
    }
}
