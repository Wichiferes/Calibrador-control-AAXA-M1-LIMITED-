package com.example.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class IrSender(private val context: Context, private val callback: Callback) {

    interface Callback {
        fun onError(error: String)
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbConnection: UsbDeviceConnection? = null
    private var usbEndpoint: UsbEndpoint? = null

    private val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { setupDevice(it) }
                    } else {
                        callback.onError("Permiso USB denegado. Conéctalo de nuevo y acepta el permiso.")
                    }
                    context.unregisterReceiver(this)
                }
            }
        }
    }

    suspend fun connect() {
        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) {
            callback.onError("Adaptador IR no conectado. Verifica la conexión OTG.")
            return
        }
        val device = deviceList.values.first()
        if (usbManager.hasPermission(device)) {
            setupDevice(device)
        } else {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION).apply { `package` = context.packageName }, flags
            )
            val intentFilter = IntentFilter(ACTION_USB_PERMISSION)
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                usbReceiver,
                intentFilter,
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    private fun setupDevice(device: UsbDevice) {
        val count = device.interfaceCount
        if (count == 0) {
            callback.onError("Dispositivo USB sin interfaces.")
            return
        }

        var selectedInterface: UsbInterface? = null
        var selectedEndpoint: UsbEndpoint? = null

        for (i in 0 until count) {
            val intf = device.getInterface(i)
            for (j in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(j)
                if (ep.direction == UsbConstants.USB_DIR_OUT) {
                    selectedInterface = intf
                    selectedEndpoint = ep
                    break
                }
            }
            if (selectedEndpoint != null) break
        }

        if (selectedInterface == null || selectedEndpoint == null) {
            callback.onError("No se encontró endpoint OUT.")
            return
        }

        val connection = usbManager.openDevice(device)
        if (connection == null) {
            callback.onError("No se pudo abrir el dispositivo.")
            return
        }

        if (!connection.claimInterface(selectedInterface, true)) {
            callback.onError("No se pudo reclamar la interfaz USB.")
            connection.close()
            return
        }

        usbConnection = connection
        usbEndpoint = selectedEndpoint
    }

    fun disconnect() {
        try {
            usbConnection?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        usbConnection = null
        usbEndpoint = null
    }

    fun isConnected() = usbConnection != null && usbEndpoint != null

    suspend fun sendCode(hexCode: Long) = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            // Try to connect once
            connect()
            delay(100)
            if (!isConnected()) {
                callback.onError("Adaptador IR no conectado. Verifica la conexión OTG.")
                return@withContext
            }
        }
        val data = buildIrPayload(hexCode)
        sendData(data)
    }

    suspend fun sendRepeat() = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext
        val times = intArrayOf(9000, 2250, 562) // Actually 562.5
        val byteList = mutableListOf<Byte>()
        byteList.add(0x00) // Protocol header
        for (t in times) {
            byteList.add((t and 0xFF).toByte())
            byteList.add(((t shr 8) and 0xFF).toByte())
        }
        val payload = byteList.toByteArray()
        sendData(payload)
    }

    private suspend fun sendData(data: ByteArray) {
        val conn = usbConnection ?: return
        val ep = usbEndpoint ?: return
        
        var result = conn.bulkTransfer(ep, data, data.size, 1000)

        if (result == -1) {
            delay(50) // retry after 50ms
            result = conn.bulkTransfer(ep, data, data.size, 1000)
            if (result == -1) {
                callback.onError("Error al enviar señal. Reintentando...")
            }
        }
    }

    private fun buildIrPayload(hexCode: Long): ByteArray {
        val timings = mutableListOf<Int>()
        // Leader
        timings.add(9000)
        timings.add(4500)
        
        // 32 bits (LSB first for each byte, wait, standard NEC sends LSB of the entire 32-bit value?
        // NEC typically sends: address (8 bits, LSB first), ~address, command, ~command.
        // It's equivalent to taking the 32-bit hexCode and extracting bit by bit from LSB to MSB if constructed appropriately.
        // But the prompt says: "each of the 32 bits is encoded LSB-first".
        // Wait, the prompt says:
        // Byte 1: Address, Byte 2: ~Address, Byte 3: Command, Byte 4: ~Command.
        // If we just iterate 32 times: `val bit = (hexCode shr i) and 1L` from bit 0 to 31, it reads the 32-bit value LSB-first.
        // Let's assume buildNecCode placed Address in MSB or LSB.
        // My buildNecCode does: ((address shl 24) or (notAddress shl 16) or (cmd shl 8) or notCmd).
        // If we read bit 0 first, we are reading notCmd first! That's reversed.
        // Standard NEC: address is sent FIRST.
        // If address is sent first, and address is in bits 24-31, we need to read from MSB byte down to LSB byte, and within each byte, read LSB first.
        // Let's do that explicitly.
        
        val bytes = arrayOf(
            (hexCode shr 24) and 0xFF,
            (hexCode shr 16) and 0xFF,
            (hexCode shr 8)  and 0xFF,
            (hexCode)        and 0xFF
        )
        
        for (b in bytes) {
            for (i in 0..7) {
                val bit = (b shr i) and 1L
                if (bit == 1L) {
                    timings.add(562) // Actually 562.5
                    timings.add(1687) // Actually 1687.5
                } else {
                    timings.add(562)
                    // Wait, gap for 0 is 562.5
                    timings.add(562)
                }
            }
        }
        
        // Trailing burst
        timings.add(562)

        val byteList = mutableListOf<Byte>()
        byteList.add(0x00) // header for raw mode
        for (t in timings) {
            byteList.add((t and 0xFF).toByte())
            byteList.add(((t shr 8) and 0xFF).toByte())
        }
        return byteList.toByteArray()
    }
}
