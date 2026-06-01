package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.IrCodeRepository
import com.example.data.dataStore
import com.example.navigation.MainNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CalibrationViewModel
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.UsbIrViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val usbIrViewModel: UsbIrViewModel = viewModel()
                val calibrationViewModel: CalibrationViewModel = viewModel()
                val settingsViewModel: SettingsViewModel = viewModel()
                
                val context = LocalContext.current
                val isCalibrationDoneFlow = remember { 
                    context.dataStore.data.map { prefs -> prefs[IrCodeRepository.KEY_CALIBRATION_DONE] ?: false } 
                }
                val isCalibrationDone by isCalibrationDoneFlow.collectAsState(initial = null)

                if (isCalibrationDone != null) {
                    MainNavigation(
                        isCalibrationDone = isCalibrationDone!!,
                        calibrationViewModel = calibrationViewModel,
                        usbIrViewModel = usbIrViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
