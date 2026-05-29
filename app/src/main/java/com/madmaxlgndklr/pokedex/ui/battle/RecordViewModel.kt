package com.madmaxlgndklr.pokedex.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.pokedex.data.local.TrainerRecord
import com.madmaxlgndklr.pokedex.data.local.WildRecord
import com.madmaxlgndklr.pokedex.data.repository.BattleRecordRepository
import com.madmaxlgndklr.pokedex.data.trainer.TrainerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecordViewModel(
    private val recordRepo: BattleRecordRepository,
    private val trainerRepo: TrainerRepository
) : ViewModel() {

    private val _trainerRecords = MutableStateFlow<List<TrainerRecord>>(emptyList())
    val trainerRecords: StateFlow<List<TrainerRecord>> = _trainerRecords

    private val _wildRecords = MutableStateFlow<List<WildRecord>>(emptyList())
    val wildRecords: StateFlow<List<WildRecord>> = _wildRecords

    private val _selectedTrainer = MutableStateFlow<Trainer?>(null)
    val selectedTrainer: StateFlow<Trainer?> = _selectedTrainer

    private val _selectedRecord = MutableStateFlow<TrainerRecord?>(null)
    val selectedRecord: StateFlow<TrainerRecord?> = _selectedRecord

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _trainerRecords.value = recordRepo.getAllTrainerRecords()
            _wildRecords.value = recordRepo.getAllWildRecords()
        }
    }

    fun selectTrainer(record: TrainerRecord) {
        _selectedRecord.value = record
        _selectedTrainer.value = trainerRepo.getById(record.trainerId)
    }

    fun clearSelection() {
        _selectedRecord.value = null
        _selectedTrainer.value = null
    }

    companion object {
        fun factory(recordRepo: BattleRecordRepository, trainerRepo: TrainerRepository) =
            viewModelFactory { initializer { RecordViewModel(recordRepo, trainerRepo) } }
    }
}
