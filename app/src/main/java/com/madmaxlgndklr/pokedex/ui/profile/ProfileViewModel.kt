package com.madmaxlgndklr.pokedex.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.remote.AuthRepository
import com.madmaxlgndklr.pokedex.data.remote.SyncRepository
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val settingsRepo: SettingsRepository,
    private val authRepo: AuthRepository,
    private val syncRepo: SyncRepository
) : ViewModel() {

    val trainerName: StateFlow<String> = settingsRepo.trainerName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _currentUser = MutableStateFlow<UserInfo?>(authRepo.currentUser())
    val currentUser: StateFlow<UserInfo?> = _currentUser

    val isAnonymous: Boolean get() = authRepo.isAnonymous()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun saveTrainerName(name: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            settingsRepo.setTrainerName(name)
            val uid = authRepo.currentUserId() ?: return@launch
            val gen = settingsRepo.selectedGen.first()
            val music = settingsRepo.musicOnLaunch.first()
            val spriteMode = settingsRepo.spriteMode.first()
            syncRepo.pushSettings(gen, music, name.take(16), spriteMode, now)
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { authRepo.signInWithEmail(email, password) }
                .onSuccess {
                    _currentUser.value = authRepo.currentUser()
                    syncRepo.syncOnOpen()
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { authRepo.signUpWithEmail(email, password) }
                .onSuccess { _currentUser.value = authRepo.currentUser() }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { authRepo.signOut() }
            runCatching { authRepo.signInAnonymously() }
            _currentUser.value = authRepo.currentUser()
        }
    }

    fun onGoogleSignInResult() {
        viewModelScope.launch {
            _currentUser.value = authRepo.currentUser()
            syncRepo.syncOnOpen()
        }
    }

    fun clearError() { _error.value = null }

    companion object {
        fun factory(
            settingsRepo: SettingsRepository,
            authRepo: AuthRepository,
            syncRepo: SyncRepository
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>) =
                ProfileViewModel(settingsRepo, authRepo, syncRepo) as T
        }
    }
}
