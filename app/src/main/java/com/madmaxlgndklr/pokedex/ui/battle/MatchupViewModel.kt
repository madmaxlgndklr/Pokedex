package com.madmaxlgndklr.pokedex.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import com.madmaxlgndklr.pokedex.ui.common.typeWeaknesses
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TeamMatchup(
    val members: List<PokemonDetail> = emptyList(),
    val offensiveCoverage: Set<String> = emptySet(),  // types this team hits SE
    val defensiveWeaknesses: Map<String, Int> = emptyMap() // type → count of team members weak to it
)

class MatchupViewModel(private val repo: PokemonRepository) : ViewModel() {

    private val _yourTeam = MutableStateFlow(TeamMatchup())
    val yourTeam: StateFlow<TeamMatchup> = _yourTeam

    private val _opponentTeam = MutableStateFlow(TeamMatchup())
    val opponentTeam: StateFlow<TeamMatchup> = _opponentTeam

    private val _swapped = MutableStateFlow(false)
    val swapped: StateFlow<Boolean> = _swapped

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadYourTeam(ids: List<Int>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val details = ids.map { repo.getPokemonDetail(it) }
                _yourTeam.value = buildMatchup(details)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addOpponent(id: Int) {
        val current = _opponentTeam.value.members
        if (current.size >= 6 || current.any { it.id == id }) return
        viewModelScope.launch {
            try {
                val detail = repo.getPokemonDetail(id)
                val updated = current + detail
                _opponentTeam.value = buildMatchup(updated)
            } catch (_: Exception) {}
        }
    }

    fun removeOpponent(id: Int) {
        val updated = _opponentTeam.value.members.filter { it.id != id }
        _opponentTeam.value = buildMatchup(updated)
    }

    fun swap() { _swapped.value = !_swapped.value }

    private fun buildMatchup(members: List<PokemonDetail>): TeamMatchup {
        val allTypes = listOf(
            "normal","fire","water","electric","grass","ice","fighting","poison",
            "ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"
        )

        // Offensive coverage: types that at least one team member's type hits SE
        val coverage = mutableSetOf<String>()
        for (pokemon in members) {
            for (atkType in pokemon.types) {
                for (defType in allTypes) {
                    val eff = typeWeaknesses(listOf(defType))[atkType] ?: 1f
                    if (eff > 1f) coverage.add(defType)
                }
            }
        }

        // Defensive weaknesses: how many team members are weak to each type
        val weakMap = mutableMapOf<String, Int>()
        for (pokemon in members) {
            val weaknesses = typeWeaknesses(pokemon.types).filter { it.value > 1f }
            for (type in weaknesses.keys) {
                weakMap[type] = (weakMap[type] ?: 0) + 1
            }
        }
        val sortedWeak = weakMap.entries.sortedByDescending { it.value }
            .associate { it.key to it.value }

        return TeamMatchup(members, coverage, sortedWeak)
    }

    companion object {
        fun factory(repo: PokemonRepository) =
            viewModelFactory { initializer { MatchupViewModel(repo) } }
    }
}
