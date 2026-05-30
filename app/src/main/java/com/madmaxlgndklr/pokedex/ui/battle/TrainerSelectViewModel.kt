package com.madmaxlgndklr.pokedex.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.trainer.TrainerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TrainerSelectViewModel(private val repo: TrainerRepository) : ViewModel() {

    private val _trainers = MutableStateFlow<List<Trainer>>(emptyList())
    val trainers: StateFlow<List<Trainer>> = _trainers

    private val _expandedRegions = MutableStateFlow<Set<String>>(emptySet())
    val expandedRegions: StateFlow<Set<String>> = _expandedRegions

    private val _sheetTrainer = MutableStateFlow<Trainer?>(null)
    val sheetTrainer: StateFlow<Trainer?> = _sheetTrainer

    private val _sheetRosterIndex = MutableStateFlow(0)
    val sheetRosterIndex: StateFlow<Int> = _sheetRosterIndex

    init {
        _trainers.value = repo.getAll()
    }

    fun toggleRegion(region: String) {
        val current = _expandedRegions.value
        _expandedRegions.value = if (region in current) current - region else current + region
    }

    fun openSheet(trainer: Trainer) {
        _sheetTrainer.value = trainer
        _sheetRosterIndex.value = 0
    }

    fun closeSheet() {
        _sheetTrainer.value = null
        _sheetRosterIndex.value = 0
    }

    fun setRosterIndex(index: Int) {
        _sheetRosterIndex.value = index
    }

    companion object {
        fun factory(repo: TrainerRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { TrainerSelectViewModel(repo) } }
    }
}
