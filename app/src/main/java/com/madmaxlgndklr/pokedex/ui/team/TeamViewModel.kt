package com.madmaxlgndklr.pokedex.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.remote.SyncRepository
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.common.typeWeaknesses
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TeamEntry(val id: Int, val detail: PokemonDetail?)

class TeamViewModel(
    private val repository: PokemonRepository,
    private val settingsRepo: SettingsRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val teamIds: StateFlow<List<Int>> = settingsRepo.team
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamEntries: StateFlow<List<TeamEntry>> = teamIds
        .map { ids ->
            ids.map { id ->
                val detail = try { repository.getPokemonDetail(id) } catch (_: Exception) { null }
                TeamEntry(id, detail)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamCoverage: StateFlow<Map<String, Float>> = teamEntries
        .map { entries ->
            val allTypes = entries.mapNotNull { it.detail }.flatMap { it.types }.distinct()
            if (allTypes.isEmpty()) emptyMap()
            else typeWeaknesses(allTypes)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addToTeam(id: Int) {
        val current = teamIds.value
        if (id in current || current.size >= 6) return
        val newTeam = current + id
        viewModelScope.launch {
            settingsRepo.setTeam(newTeam)
            syncRepository.pushTeam(newTeam)
        }
    }

    fun removeFromTeam(id: Int) {
        val filtered = teamIds.value.filter { it != id }
        viewModelScope.launch {
            settingsRepo.setTeam(filtered)
            syncRepository.pushTeam(filtered)
        }
    }

    fun isOnTeam(id: Int): Boolean = id in teamIds.value

    companion object {
        fun factory(repository: PokemonRepository, settingsRepo: SettingsRepository, syncRepository: SyncRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TeamViewModel(repository, settingsRepo, syncRepository) }
            }
    }
}
