package com.madmaxlgndklr.pokedex.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.local.HeldItem
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.data.repository.PokemonRepository
import com.madmaxlgndklr.pokedex.model.Move
import com.madmaxlgndklr.pokedex.model.PokemonDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalcSlot(
    val detail: PokemonDetail? = null,
    val level: Int = 50,
    val statConfig: StatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 }),
    val nature: Nature = Natures.HARDY,
    val heldItem: HeldItem? = null
)

data class CalcUiState(
    val gen: Int = 5,
    val attacker: CalcSlot = CalcSlot(),
    val defender: CalcSlot = CalcSlot(),
    val selectedMove: Move? = null,
    val result: DamageResult? = null,
    val isLoadingAttacker: Boolean = false,
    val isLoadingDefender: Boolean = false,
    val isLoadingMove: Boolean = false,
    val error: String? = null
)

class DamageCalcViewModel(
    private val repo: PokemonRepository,
    private val settingsRepo: SettingsRepository,
    preloadAttackerId: Int? = null
) : ViewModel() {

    private val _state = MutableStateFlow(CalcUiState())
    val state: StateFlow<CalcUiState> = _state

    val selectedGen: StateFlow<Int> = settingsRepo.selectedGen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    init {
        viewModelScope.launch {
            settingsRepo.selectedGen.collect { gen -> _state.value = _state.value.copy(gen = gen) }
        }
        if (preloadAttackerId != null) loadAttacker(preloadAttackerId)
    }

    fun setGen(gen: Int) {
        viewModelScope.launch {
            settingsRepo.setGen(gen)
            val defaultConfig = if (gen <= 2)
                StatConfig.Gen12Config(IntArray(5) { 15 }, IntArray(5) { 0 })
            else
                StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, IntArray(6) { 0 })
            _state.value = _state.value.copy(
                attacker = _state.value.attacker.copy(statConfig = defaultConfig, nature = Natures.HARDY),
                defender = _state.value.defender.copy(statConfig = defaultConfig, nature = Natures.HARDY)
            )
        }
    }

    fun loadAttacker(id: Int) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoadingAttacker = true, error = null)
        try {
            val detail = repo.getPokemonDetail(id)
            _state.value = _state.value.copy(
                attacker = _state.value.attacker.copy(detail = detail),
                isLoadingAttacker = false
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoadingAttacker = false, error = e.message)
        }
    }

    fun loadDefender(id: Int) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoadingDefender = true, error = null)
        try {
            val detail = repo.getPokemonDetail(id)
            _state.value = _state.value.copy(
                defender = _state.value.defender.copy(detail = detail),
                isLoadingDefender = false
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoadingDefender = false, error = e.message)
        }
    }

    fun loadMove(name: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoadingMove = true, error = null)
        try {
            val move = repo.getMove(name)
            _state.value = _state.value.copy(selectedMove = move, isLoadingMove = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoadingMove = false, error = e.message)
        }
    }

    fun updateAttacker(slot: CalcSlot) { _state.value = _state.value.copy(attacker = slot, result = null) }
    fun updateDefender(slot: CalcSlot) { _state.value = _state.value.copy(defender = slot, result = null) }

    fun calculate() {
        val s = _state.value
        val attackerDetail = s.attacker.detail ?: return
        val defenderDetail = s.defender.detail ?: return
        val move = s.selectedMove ?: return
        if (move.power == null) {
            _state.value = s.copy(result = DamageResult(0, 0, 0, "—"))
            return
        }
        val isPhysical = when {
            s.gen >= 4 -> move.category == "physical"
            s.gen == 1 -> true
            else -> DamageEngine.isPhysicalGen23(move.type)
        }
        val atkStatName = if (isPhysical) "attack" else "special-attack"
        val defStatName = if (isPhysical) "defense" else "special-defense"
        val atkBase = attackerDetail.stats.firstOrNull { it.name == atkStatName }?.value ?: 50
        val defBase = defenderDetail.stats.firstOrNull { it.name == defStatName }?.value ?: 50
        val attackStatIndex = if (isPhysical) 1 else 3
        val defenseStatIndex = if (isPhysical) 2 else 4

        val params = DamageParams(
            gen = s.gen,
            level = s.attacker.level,
            attackBaseStat = atkBase,
            defenseBaseStat = defBase,
            attackStatIndex = attackStatIndex,
            defenseStatIndex = defenseStatIndex,
            attackerStatConfig = s.attacker.statConfig,
            attackerNature = s.attacker.nature,
            defenderStatConfig = s.defender.statConfig,
            defenderNature = s.defender.nature,
            heldItem = s.attacker.heldItem,
            basePower = move.power,
            moveType = move.type,
            moveCategory = move.category,
            attackerTypes = attackerDetail.types,
            defenderTypes = defenderDetail.types
        )
        _state.value = s.copy(result = DamageEngine.calculate(params))
    }

    fun isEvSumValid(slot: CalcSlot): Boolean {
        val cfg = slot.statConfig
        return if (cfg is StatConfig.Gen3PlusConfig) StatFormulas.isEvSumValid(cfg.evs) else true
    }

    companion object {
        fun factory(repo: PokemonRepository, settingsRepo: SettingsRepository, preloadId: Int? = null) =
            viewModelFactory { initializer { DamageCalcViewModel(repo, settingsRepo, preloadId) } }
    }
}
