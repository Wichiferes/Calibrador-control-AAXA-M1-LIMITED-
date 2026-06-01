package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IrCodeRepository
import com.example.data.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalibrationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IrCodeRepository(application.dataStore)
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    data class Candidate(
        val family: String,
        val address: Int,
        val command: Int
    )

    val candidates = listOf(
        Candidate("Generic-A", 0x00, 0x45),
        Candidate("Generic-A", 0x00, 0x40),
        Candidate("Generic-A", 0x00, 0x08),
        Candidate("Generic-A", 0x00, 0x18),
        Candidate("Generic-A", 0x00, 0x46),
        Candidate("Generic-A", 0x00, 0x38),
        Candidate("Actions Semiconductor", 0x04, 0x45),
        Candidate("Actions Semiconductor", 0x04, 0x08),
        Candidate("Actions Semiconductor", 0x04, 0x18),
        Candidate("Actions Semiconductor", 0x04, 0x12),
        Candidate("Sunplus", 0x40, 0x12),
        Candidate("Sunplus", 0x40, 0x14),
        Candidate("Sunplus/Generic-A", 0x40, 0x45),
        Candidate("Sunplus", 0x40, 0x00),
        Candidate("Mediatek MT8222", 0x80, 0x12),
        Candidate("Mediatek/Generic-A", 0x80, 0x45),
        Candidate("Mediatek", 0x80, 0x2D),
        Candidate("NEC Extended", 0xFF, 0x00),
        Candidate("NEC Extended", 0xFF, 0x18),
        Candidate("Generic-A/Aiptek", 0x00, 0x10)
    )

    fun nextCandidate() {
        if (_currentIndex.value < candidates.size) {
            _currentIndex.value += 1
        }
    }

    fun resetCalibration() {
        _currentIndex.value = 0
    }

    fun saveSuccess(navigateNext: () -> Unit) {
        viewModelScope.launch {
            if (_currentIndex.value < candidates.size) {
                val candidate = candidates[_currentIndex.value]
                repository.saveCalibration(_currentIndex.value, candidate.address, candidate.command)
            } else {
                val defaultCandidate = candidates[0]
                repository.saveCalibration(0, defaultCandidate.address, defaultCandidate.command)
            }
            navigateNext()
        }
    }
    
    fun useDefault(navigateNext: () -> Unit) {
        viewModelScope.launch {
            val defaultCandidate = candidates[0]
            repository.saveCalibration(0, defaultCandidate.address, defaultCandidate.command)
            navigateNext()
        }
    }
}
