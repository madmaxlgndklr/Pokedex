package com.madmaxlgndklr.pokedex.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SlotOverride(
    val level: Int? = null,
    val statConfig: StatConfig? = null,
    val nature: Nature? = null,
    val heldItem: HeldItem? = null
)

data class BattleSetup(
    val playerDetail: PokemonDetail,
    val level: Int,
    val selectedMoveNames: List<String>,
    val statConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
    val nature: Nature = Natures.HARDY,
    val heldItem: HeldItem? = null,
    val teamOverrides: Map<Int, SlotOverride> = emptyMap()
)

data class LearnableMove(
    val name: String,
    val requiredLevel: Int?,  // null = TM/egg/tutor (no level restriction)
    val available: Boolean
)

data class SelectedTrainer(val trainer: Trainer, val rosterIndex: Int)

fun learnableMoves(detail: PokemonDetail, level: Int): List<LearnableMove> {
    val tmSet = (detail.tmMoves ?: emptyList()).toSet()
    val byName = mutableMapOf<String, LearnableMove>()
    // Level-up moves: gate by level; keep lowest level if duplicates present
    for (pm in detail.moves) {
        val existing = byName[pm.name]
        if (existing == null || pm.levelLearnedAt < (existing.requiredLevel ?: Int.MAX_VALUE)) {
            byName[pm.name] = LearnableMove(pm.name, pm.levelLearnedAt, pm.levelLearnedAt <= level)
        }
    }
    // TM/egg/tutor: always available, override any level-up entry
    for (name in tmSet) {
        byName[name] = LearnableMove(name, null, true)
    }
    return byName.values.sortedWith(
        compareBy<LearnableMove> { it.requiredLevel != null }  // TMs (false) before level-up (true)
            .thenBy { it.requiredLevel ?: 0 }
            .thenBy { it.name }
    )
}

class TurnBattleViewModel(
    private val repo: PokemonRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _battleState = MutableStateFlow<BattleState?>(null)
    val battleState: StateFlow<BattleState?> = _battleState

    private val _setup = MutableStateFlow<BattleSetup?>(null)
    val setup: StateFlow<BattleSetup?> = _setup

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val selectedGen: StateFlow<Int> = settingsRepo.selectedGen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    private val _heldItems = MutableStateFlow<List<HeldItem>>(emptyList())
    val heldItems: StateFlow<List<HeldItem>> = _heldItems

    private val _heldItemSyncError = MutableStateFlow(false)
    val heldItemSyncError: StateFlow<Boolean> = _heldItemSyncError

    private val _battleTrainer = MutableStateFlow<SelectedTrainer?>(null)
    val battleTrainer: StateFlow<SelectedTrainer?> = _battleTrainer

    val canStartBattle: StateFlow<Boolean> = _setup
        .map { setup ->
            val cfg = setup?.statConfig
            if (cfg is StatConfig.Gen3PlusConfig) StatFormulas.isEvSumValid(cfg.evs) else true
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setStatConfig(config: StatConfig) {
        _setup.value = _setup.value?.copy(statConfig = config)
    }

    fun setNature(nature: Nature) {
        _setup.value = _setup.value?.copy(nature = nature)
    }

    fun setHeldItem(item: HeldItem?) {
        _setup.value = _setup.value?.copy(heldItem = item)
    }

    fun setSlotOverride(slotIndex: Int, override: SlotOverride?) {
        val s = _setup.value ?: return
        val newOverrides = s.teamOverrides.toMutableMap()
        if (override == null) newOverrides.remove(slotIndex)
        else newOverrides[slotIndex] = override
        _setup.value = s.copy(teamOverrides = newOverrides)
    }

    fun setGen(gen: Int) {
        viewModelScope.launch {
            settingsRepo.setGen(gen)
        }
    }

    fun loadHeldItems(heldItemRepo: com.madmaxlgndklr.pokedex.data.repository.HeldItemRepository) {
        viewModelScope.launch {
            try {
                _heldItems.value = heldItemRepo.getAll()
            } catch (e: Exception) {
                _heldItemSyncError.value = true
            }
        }
    }

    fun loadSetup(teamIds: List<Int>, pickIndex: Int = 0) {
        if (_setup.value != null || _isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val detail = repo.getPokemonDetail(teamIds[pickIndex])
                val level = 50
                val defaults = learnableMoves(detail, level).filter { it.available }.take(4).map { it.name }
                _setup.value = BattleSetup(detail, level, defaults)
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSetupLevel(level: Int) {
        val s = _setup.value ?: return
        val clamped = level.coerceIn(1, 100)
        val availableNames = learnableMoves(s.playerDetail, clamped).filter { it.available }.map { it.name }.toSet()
        _setup.value = s.copy(
            level = clamped,
            selectedMoveNames = s.selectedMoveNames.filter { it in availableNames }
        )
    }

    fun toggleSetupMove(moveName: String) {
        val s = _setup.value ?: return
        val selected = s.selectedMoveNames
        _setup.value = s.copy(
            selectedMoveNames = when {
                moveName in selected -> selected - moveName
                selected.size < 4 -> selected + moveName
                else -> selected
            }
        )
    }

    fun startBattleFromSetup(teamIds: List<Int>) {
        val s = _setup.value ?: return
        if (s.selectedMoveNames.isEmpty()) return
        if (teamIds.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val gen = settingsRepo.selectedGen.first()
                val playerTeam = teamIds.mapIndexed { idx, pokemonId ->
                    val detail = if (idx == 0) s.playerDetail else {
                        try { repo.getPokemonDetail(pokemonId) } catch (e: Exception) { s.playerDetail }
                    }
                    val ov = s.teamOverrides[idx]
                    val level = ov?.level ?: s.level
                    val statConfig = ov?.statConfig ?: s.statConfig
                    val nature = ov?.nature ?: s.nature
                    val heldItem = ov?.heldItem ?: s.heldItem
                    val moves = if (idx == 0) resolveMoves(s.selectedMoveNames)
                        else resolveMoves(learnableMoves(detail, level).filter { it.available }.take(4).map { it.name })
                    BattleEngine.buildBattlePokemon(detail, level, moves, statConfig, nature, heldItem)
                }
                val bt = _battleTrainer.value
                val opponentTeam = if (bt != null) {
                    bt.trainer.rosters[bt.rosterIndex].team.mapNotNull { tp ->
                        try {
                            val detail = repo.getPokemonDetail(tp.pokemonId)
                            BattleEngine.buildBattlePokemon(detail, tp.level, resolveMoves(tp.moves))
                        } catch (e: Exception) { null }
                    }.ifEmpty { return@launch }
                } else {
                    val opponentDetail = repo.getPokemonDetail(repo.getPokemonList().random().id)
                    val opponentMoves = resolveMoves(opponentDetail.moves.take(4).map { it.name })
                    listOf(BattleEngine.buildBattlePokemon(opponentDetail, s.level, opponentMoves))
                }
                _battleState.value = BattleEngine.startBattle(playerTeam, opponentTeam, gen)
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetToSetup() {
        _battleState.value = null
        _battleTrainer.value = null
    }

    fun loadTrainerSetup(trainer: Trainer, rosterIndex: Int, teamIds: List<Int>) {
        _battleTrainer.value = SelectedTrainer(trainer, rosterIndex)
        _setup.value = null  // allow loadSetup to re-run
        loadSetup(teamIds)
    }

    fun startTrainerBattle(trainer: Trainer, rosterIndex: Int, teamIds: List<Int>) {
        if (teamIds.isEmpty()) return
        val s = _setup.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val gen = settingsRepo.selectedGen.first()
                val playerTeam = teamIds.mapIndexed { idx, pokemonId ->
                    val detail = if (idx == 0) s.playerDetail else {
                        try { repo.getPokemonDetail(pokemonId) } catch (e: Exception) { s.playerDetail }
                    }
                    val ov = s.teamOverrides[idx]
                    val level = ov?.level ?: s.level
                    val statConfig = ov?.statConfig ?: s.statConfig
                    val nature = ov?.nature ?: s.nature
                    val heldItem = ov?.heldItem ?: s.heldItem
                    val moves = if (idx == 0) resolveMoves(s.selectedMoveNames)
                        else resolveMoves(learnableMoves(detail, level).filter { it.available }.take(4).map { it.name })
                    BattleEngine.buildBattlePokemon(detail, level, moves, statConfig, nature, heldItem)
                }
                val opponentTeam = trainer.rosters[rosterIndex].team.mapNotNull { tp ->
                    try {
                        val detail = repo.getPokemonDetail(tp.pokemonId)
                        val moves = resolveMoves(tp.moves)
                        BattleEngine.buildBattlePokemon(detail, tp.level, moves)
                    } catch (e: Exception) { null }
                }
                if (opponentTeam.isEmpty()) return@launch
                _battleState.value = BattleEngine.startBattle(playerTeam, opponentTeam, gen)
            } catch (e: Exception) {
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
            val playerAction = TurnAction.UseMove(playerMove)
            val aiAction = BattleEngine.aiPickAction(ongoing.opponent, ongoing.player, ongoing.opponentTeam, gen)
            _battleState.value = BattleEngine.resolveTurn(playerAction, aiAction, ongoing, gen)
        }
    }

    fun submitSwitch(targetIndex: Int) {
        val ongoing = _battleState.value as? BattleState.Ongoing ?: return
        viewModelScope.launch {
            val gen = settingsRepo.selectedGen.first()
            val playerAction = TurnAction.SwitchTo(targetIndex)
            val aiAction = BattleEngine.aiPickAction(ongoing.opponent, ongoing.player, ongoing.opponentTeam, gen)
            _battleState.value = BattleEngine.resolveTurn(playerAction, aiAction, ongoing, gen)
        }
    }

    fun confirmSwitch(newIndex: Int) {
        val pending = _battleState.value as? BattleState.PendingSwitch ?: return
        _battleState.value = BattleEngine.confirmSwitch(newIndex, pending)
    }

    fun forfeit() {
        val ongoing = _battleState.value as? BattleState.Ongoing ?: return
        _battleState.value = BattleState.Lost(ongoing.log + listOf("You forfeited."))
    }

    private suspend fun resolveMoves(moveNames: List<String>): List<BattleMove> {
        val moves = moveNames.map { name ->
            try {
                val m = repo.getMove(name)
                BattleMove(name = m.name, type = m.type, category = m.category,
                    power = m.power, maxPp = m.pp, currentPp = m.pp)
            } catch (e: Exception) {
                BattleMove(name, "normal", "physical", 50, 20, 20)
            }
        }
        return moves.ifEmpty { listOf(BattleMove("struggle", "normal", "physical", 50, 1, 1)) }
    }

    companion object {
        fun factory(repo: PokemonRepository, settingsRepo: SettingsRepository) =
            viewModelFactory { initializer { TurnBattleViewModel(repo, settingsRepo) } }
    }
}
