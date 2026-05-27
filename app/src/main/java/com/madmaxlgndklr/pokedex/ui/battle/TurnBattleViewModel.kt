package com.madmaxlgndklr.pokedex.ui.battle

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TurnBattleViewModel(
    private val repo: PokemonRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _battleState = MutableStateFlow<BattleState?>(null)
    val battleState: StateFlow<BattleState?> = _battleState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val selectedGen: StateFlow<Int> = settingsRepo.selectedGen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    fun startBattle(playerTeamIds: List<Int>, playerPickIndex: Int = 0) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allPokemon = repo.getPokemonList()
                val opponentSummary = allPokemon.random()
                val playerDetail = repo.getPokemonDetail(playerTeamIds[playerPickIndex])
                val opponentDetail = repo.getPokemonDetail(opponentSummary.id)
                val gen = settingsRepo.selectedGen.first()
                _battleState.value = BattleEngine.startBattle(playerDetail, opponentDetail, gen)
            } catch (e: Exception) {
                // leave state null — UI shows error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitMove(moveIndex: Int) {
        val ongoing = _battleState.value as? BattleState.Ongoing ?: return
        viewModelScope.launch {
            val gen = settingsRepo.selectedGen.first()
            val playerMove = ongoing.player.moves.getOrNull(moveIndex) ?: return@launch
            val aiMove = BattleEngine.aiPickMove(ongoing.opponent)
            val playerAction = MoveAction(ongoing.player, playerMove, ongoing.opponent)
            val aiAction = MoveAction(ongoing.opponent, aiMove, ongoing.player)
            _battleState.value = BattleEngine.resolveTurn(playerAction, aiAction, ongoing, gen)
        }
    }

    fun forfeit() {
        val ongoing = _battleState.value as? BattleState.Ongoing ?: return
        _battleState.value = BattleState.Lost(ongoing.log + listOf("You forfeited."))
    }

    fun rematch(playerTeamIds: List<Int>, playerPickIndex: Int = 0) {
        _battleState.value = null
        startBattle(playerTeamIds, playerPickIndex)
    }

    companion object {
        fun factory(repo: PokemonRepository, settingsRepo: SettingsRepository) =
            viewModelFactory { initializer { TurnBattleViewModel(repo, settingsRepo) } }
    }
}
