package com.example.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IrCodeRepository
import com.example.data.IrStatus
import com.example.data.dataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IrCodeRepository(application.dataStore)
    
    val fullStatusFlow: StateFlow<IrStatus?> = repository.fullStatusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun updateCommand(key: Preferences.Key<Int>, newCommandByte: Int) {
        viewModelScope.launch {
            repository.updateCommand(key, newCommandByte)
        }
    }

    fun restartCalibration(navigateCalibration: () -> Unit) {
        viewModelScope.launch {
            repository.clearCalibration()
            navigateCalibration()
        }
    }
}
