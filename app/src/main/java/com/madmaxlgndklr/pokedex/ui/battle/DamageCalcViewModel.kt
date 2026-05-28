package com.madmaxlgndklr.pokedex.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.local.SettingsRepository
import com.madmaxlgndklr.pokedex.ui.battle.Natures
import com.madmaxlgndklr.pokedex.ui.battle.StatConfig
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
    val attackEvs: Int = 0,
    val spAttackEvs: Int = 0,
    val defenseEvs: Int = 0,
    val spDefenseEvs: Int = 0,
    val natureMultiplier: Float = 1f  // 0.9 / 1.0 / 1.1
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
        viewModelScope.launch { settingsRepo.setGen(gen) }
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
        val atkEvs = if (isPhysical) s.attacker.attackEvs else s.attacker.spAttackEvs
        val defEvs = if (isPhysical) s.defender.defenseEvs else s.defender.spDefenseEvs

        val params = DamageParams(
            gen = s.gen,
            level = s.attacker.level,
            attackBaseStat = atkBase,
            defenseBaseStat = defBase,
            attackStatIndex = if (isPhysical) 1 else 3,
            defenseStatIndex = if (isPhysical) 2 else 4,
            attackerStatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, intArrayOf(atkEvs, atkEvs, atkEvs, atkEvs, atkEvs, atkEvs)),
            attackerNature = Natures.HARDY,
            defenderStatConfig = StatConfig.Gen3PlusConfig(IntArray(6) { 31 }, intArrayOf(defEvs, defEvs, defEvs, defEvs, defEvs, defEvs)),
            defenderNature = Natures.HARDY,
            basePower = move.power,
            moveType = move.type,
            moveCategory = move.category,
            attackerTypes = attackerDetail.types,
            defenderTypes = defenderDetail.types
        )
        _state.value = s.copy(result = DamageEngine.calculate(params))
    }

    companion object {
        fun factory(repo: PokemonRepository, settingsRepo: SettingsRepository, preloadId: Int? = null) =
            viewModelFactory { initializer { DamageCalcViewModel(repo, settingsRepo, preloadId) } }
    }
}
