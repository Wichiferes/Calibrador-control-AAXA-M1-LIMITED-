package com.example.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IrSender
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UsbIrViewModel(application: Application) : AndroidViewModel(application) {

    private val _usbConnected = MutableStateFlow(false)
    val usbConnected: StateFlow<Boolean> = _usbConnected.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val irSender: IrSender

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    checkUsbConnection()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    irSender.disconnect()
                    _usbConnected.value = false
                }
            }
        }
    }

    init {
        val irCallback = object : IrSender.Callback {
            override fun onError(error: String) {
                _lastError.value = error
            }
        }
        irSender = IrSender(application, irCallback)

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            application,
            usbReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )

        checkUsbConnection()
    }

    private fun checkUsbConnection() {
        viewModelScope.launch {
            irSender.connect()
            _usbConnected.value = irSender.isConnected()
        }
    }

    fun clearError() {
        _lastError.value = null
    }

    fun sendCode(hexCode: Long) {
        viewModelScope.launch {
            irSender.sendCode(hexCode)
        }
    }

    private var repeatJob: Job? = null
    private var isRepeating = false

    fun sendRepeatStart(hexCode: Long) {
        if (isRepeating) return
        isRepeating = true
        repeatJob = viewModelScope.launch {
            // Send the first command
            irSender.sendCode(hexCode)
            delay(108) // wait before first repeat
            while (isActive && isRepeating) {
                irSender.sendRepeat()
                delay(108)
            }
        }
    }

    fun sendRepeatStop() {
        isRepeating = false
        repeatJob?.cancel()
        repeatJob = null
    }

    fun releaseUsb() {
        irSender.disconnect()
        checkUsbConnection()
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(usbReceiver)
        irSender.disconnect()
    }
}
