package com.madmaxlgndklr.pokedex.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepo: SettingsRepository) : ViewModel() {
    val musicOnLaunch: StateFlow<Boolean> = settingsRepo.musicOnLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setMusicOnLaunch(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setMusicOnLaunch(enabled) }
    }

    companion object {
        fun factory(settingsRepo: SettingsRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(settingsRepo) }
        }
    }
}
