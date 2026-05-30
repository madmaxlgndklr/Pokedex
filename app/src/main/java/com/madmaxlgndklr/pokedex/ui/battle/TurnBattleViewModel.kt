package com.madmaxlgndklr.pokedex.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.repository.BattleRecordRepository
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SlotOverride(
    val level: Int? = null,
    val statConfig: StatConfig? = null,
    val nature: Nature? = null,
    val heldItem: HeldItem? = null,
    val selectedMoveNames: List<String>? = null
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

    private var _recordRepo: BattleRecordRepository? = null
    private var activeWildPokemonId: Int? = null
    private var activeWildPokemonName: String? = null

    fun setRecordRepository(repo: BattleRecordRepository) {
        _recordRepo = repo
    }

    private val _selectedSetupSlot = MutableStateFlow(0)
    val selectedSetupSlot: StateFlow<Int> = _selectedSetupSlot

    private val _slotDetails = MutableStateFlow<Map<Int, PokemonDetail>>(emptyMap())
    val slotDetails: StateFlow<Map<Int, PokemonDetail>> = _slotDetails

    private var _teamIds: List<Int> = emptyList()

    init {
        viewModelScope.launch {
            _setup.filterNotNull().debounce(800L).collect { setup ->
                settingsRepo.saveBattleConfig(setup.toDto().toJson())
            }
        }
        viewModelScope.launch {
            _battleState.collect { state ->
                val repo = _recordRepo ?: return@collect
                when (state) {
                    is BattleState.Won  -> viewModelScope.launch { recordResult(repo, won = true) }
                    is BattleState.Lost -> viewModelScope.launch { recordResult(repo, won = false) }
                    else -> {}
                }
            }
        }
    }

    private suspend fun recordResult(repo: BattleRecordRepository, won: Boolean) {
        val trainer = _battleTrainer.value
        if (trainer != null) {
            repo.recordTrainerBattle(trainer.trainer, won)
        } else {
            val id = activeWildPokemonId ?: return
            val name = activeWildPokemonName ?: return
            repo.recordWildBattle(id, name, won)
        }
    }

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

    fun setSlotOverride(pokemonId: Int, override: SlotOverride?) {
        val s = _setup.value ?: return
        val newOverrides = s.teamOverrides.toMutableMap()
        if (override == null) newOverrides.remove(pokemonId)
        else newOverrides[pokemonId] = override
        _setup.value = s.copy(teamOverrides = newOverrides)
    }

    fun selectSetupSlot(idx: Int, teamIds: List<Int>) {
        _selectedSetupSlot.value = idx
        if (_slotDetails.value.containsKey(idx)) return
        if (idx >= teamIds.size) return
        viewModelScope.launch {
            try {
                val detail = repo.getPokemonDetail(teamIds[idx])
                _slotDetails.value = _slotDetails.value + (idx to detail)
            } catch (e: Exception) { }
        }
    }

    fun toggleSlotMove(slot: Int, moveName: String) {
        if (slot == 0) { toggleSetupMove(moveName); return }
        val s = _setup.value ?: return
        val pokemonId = _teamIds.getOrNull(slot) ?: return
        val ov = s.teamOverrides[pokemonId]
        val detail = _slotDetails.value[slot] ?: return
        val level = ov?.level ?: s.level
        val autoMoves = learnableMoves(detail, level).filter { it.available }.take(4).map { it.name }
        val current = ov?.selectedMoveNames ?: autoMoves
        val newSelected = when {
            moveName in current -> current - moveName
            current.size < 4 -> current + moveName
            else -> current
        }
        val newOv = (ov ?: SlotOverride()).copy(selectedMoveNames = newSelected)
        setSlotOverride(pokemonId, newOv)
    }

    fun setSlotLevel(slot: Int, level: Int) {
        if (slot == 0) { setSetupLevel(level); return }
        val s = _setup.value ?: return
        val pokemonId = _teamIds.getOrNull(slot) ?: return
        val clamped = level.coerceIn(1, 100)
        val detail = _slotDetails.value[slot] ?: return
        val ov = s.teamOverrides[pokemonId]
        val availableNames = learnableMoves(detail, clamped).filter { it.available }.map { it.name }.toSet()
        val filteredMoves = ov?.selectedMoveNames?.filter { it in availableNames }
        val newOv = (ov ?: SlotOverride()).copy(level = clamped, selectedMoveNames = filteredMoves)
        val effective = if (newOv.level == s.level && newOv.nature == null && newOv.selectedMoveNames == null && newOv.heldItem == null && newOv.statConfig == null) null else newOv
        setSlotOverride(pokemonId, effective)
    }

    fun setSlotNature(slot: Int, nature: Nature) {
        if (slot == 0) { setNature(nature); return }
        val s = _setup.value ?: return
        val pokemonId = _teamIds.getOrNull(slot) ?: return
        val ov = s.teamOverrides[pokemonId]
        val newOv = (ov ?: SlotOverride()).copy(nature = nature)
        setSlotOverride(pokemonId, newOv)
    }

    fun setSlotStatConfig(slot: Int, config: StatConfig) {
        if (slot == 0) { setStatConfig(config); return }
        val s = _setup.value ?: return
        val pokemonId = _teamIds.getOrNull(slot) ?: return
        val ov = s.teamOverrides[pokemonId]
        val newOv = (ov ?: SlotOverride()).copy(statConfig = config)
        setSlotOverride(pokemonId, newOv)
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
        _teamIds = teamIds
        if (_setup.value != null || _isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val detail = repo.getPokemonDetail(teamIds[pickIndex])
                val savedDto = settingsRepo.loadBattleConfigJson()?.toBattleConfigDto()
                _setup.value = if (savedDto != null) {
                    val savedLevel = savedDto.level.coerceIn(1, 100)
                    val available = learnableMoves(detail, savedLevel).filter { it.available }.map { it.name }.toSet()
                    val validMoves = savedDto.moves.filter { it in available }
                        .ifEmpty { learnableMoves(detail, savedLevel).filter { it.available }.take(4).map { it.name } }
                    val nature = Natures.ALL.find { it.name.equals(savedDto.nature, ignoreCase = true) } ?: Natures.HARDY
                    val slots = savedDto.slots
                        .mapKeys { it.key.toIntOrNull() ?: -1 }
                        .filter { it.key > 0 }
                        .mapValues { (_, v) ->
                            SlotOverride(
                                level = v.level,
                                nature = v.nature?.let { n -> Natures.ALL.find { it.name.equals(n, ignoreCase = true) } },
                                selectedMoveNames = v.moves,
                                statConfig = v.statConfig?.toStatConfig(),
                                heldItem = v.heldItem?.toHeldItem()
                            )
                        }
                    BattleSetup(
                        playerDetail = detail,
                        level = savedLevel,
                        selectedMoveNames = validMoves,
                        statConfig = savedDto.statConfig.toStatConfig(),
                        nature = nature,
                        heldItem = savedDto.heldItem?.toHeldItem(),
                        teamOverrides = slots
                    )
                } else {
                    val defaults = learnableMoves(detail, 50).filter { it.available }.take(4).map { it.name }
                    BattleSetup(detail, 50, defaults)
                }
                _slotDetails.value = mapOf(0 to detail)
                _selectedSetupSlot.value = 0
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
                    val ov = s.teamOverrides[pokemonId]
                    val level = ov?.level ?: s.level
                    val statConfig = ov?.statConfig ?: s.statConfig
                    val nature = ov?.nature ?: s.nature
                    val heldItem = ov?.heldItem ?: s.heldItem
                    val moves = when {
                        idx == 0 -> resolveMoves(s.selectedMoveNames)
                        ov?.selectedMoveNames != null -> resolveMoves(ov.selectedMoveNames)
                        else -> resolveMoves(learnableMoves(detail, level).filter { it.available }.take(4).map { it.name })
                    }
                    BattleEngine.buildBattlePokemon(detail, level, moves, statConfig, nature, heldItem)
                }
                val bt = _battleTrainer.value
                activeWildPokemonId = null
                activeWildPokemonName = null
                val opponentTeam = if (bt != null) {
                    bt.trainer.rosters[bt.rosterIndex].team.mapNotNull { tp ->
                        try {
                            val detail = repo.getPokemonDetail(tp.pokemonId)
                            BattleEngine.buildBattlePokemon(detail, tp.level, resolveMoves(tp.moves))
                        } catch (e: Exception) { null }
                    }.ifEmpty { return@launch }
                } else {
                    val opponentDetail = repo.getPokemonDetail(repo.getPokemonList().random().id)
                    activeWildPokemonId = opponentDetail.id
                    activeWildPokemonName = opponentDetail.name
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
        _selectedSetupSlot.value = 0
        _slotDetails.value = emptyMap()
        activeWildPokemonId = null
        activeWildPokemonName = null
    }

    fun loadTrainerSetup(trainer: Trainer, rosterIndex: Int, teamIds: List<Int>) {
        _teamIds = teamIds
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
                    val ov = s.teamOverrides[pokemonId]
                    val level = ov?.level ?: s.level
                    val statConfig = ov?.statConfig ?: s.statConfig
                    val nature = ov?.nature ?: s.nature
                    val heldItem = ov?.heldItem ?: s.heldItem
                    val moves = when {
                        idx == 0 -> resolveMoves(s.selectedMoveNames)
                        ov?.selectedMoveNames != null -> resolveMoves(ov.selectedMoveNames)
                        else -> resolveMoves(learnableMoves(detail, level).filter { it.available }.take(4).map { it.name })
                    }
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
