package com.example.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IrCodeRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_IR_ADDRESS = intPreferencesKey("ir_address")
        val KEY_IR_CMD_POWER = intPreferencesKey("ir_cmd_power")
        val KEY_IR_CMD_MUTE = intPreferencesKey("ir_cmd_mute")
        val KEY_IR_CMD_PLAY = intPreferencesKey("ir_cmd_play")
        val KEY_IR_CMD_RW = intPreferencesKey("ir_cmd_rw")
        val KEY_IR_CMD_FF = intPreferencesKey("ir_cmd_ff")
        val KEY_IR_CMD_UP = intPreferencesKey("ir_cmd_up")
        val KEY_IR_CMD_DOWN = intPreferencesKey("ir_cmd_down")
        val KEY_IR_CMD_LEFT = intPreferencesKey("ir_cmd_left")
        val KEY_IR_CMD_RIGHT = intPreferencesKey("ir_cmd_right")
        val KEY_IR_CMD_OK = intPreferencesKey("ir_cmd_ok")
        val KEY_IR_CMD_SETTINGS = intPreferencesKey("ir_cmd_settings")
        val KEY_IR_CMD_BACK = intPreferencesKey("ir_cmd_back")
        val KEY_IR_CMD_VOLUP = intPreferencesKey("ir_cmd_volup")
        val KEY_IR_CMD_HOME = intPreferencesKey("ir_cmd_home")
        val KEY_CALIBRATION_DONE = booleanPreferencesKey("calibration_done")
        val KEY_CALIBRATION_VARIANT = intPreferencesKey("calibration_variant")

        fun buildNecCode(address: Int, command: Int): Long {
            val notAddress = address.inv() and 0xFF
            val notCommand = command.inv() and 0xFF
            return ((address.toLong() shl 24)
                    or (notAddress.toLong() shl 16)
                    or (command.toLong() shl 8)
                    or notCommand.toLong())
        }
    }

    val calibrationDoneFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_CALIBRATION_DONE] ?: false
    }

    val fullStatusFlow: Flow<IrStatus> = dataStore.data.map { p ->
        IrStatus(
            address = p[KEY_IR_ADDRESS] ?: 0,
            cmdPower = p[KEY_IR_CMD_POWER] ?: 0,
            cmdMute = p[KEY_IR_CMD_MUTE] ?: 0,
            cmdPlay = p[KEY_IR_CMD_PLAY] ?: 0,
            cmdRw = p[KEY_IR_CMD_RW] ?: 0,
            cmdFf = p[KEY_IR_CMD_FF] ?: 0,
            cmdUp = p[KEY_IR_CMD_UP] ?: 0,
            cmdDown = p[KEY_IR_CMD_DOWN] ?: 0,
            cmdLeft = p[KEY_IR_CMD_LEFT] ?: 0,
            cmdRight = p[KEY_IR_CMD_RIGHT] ?: 0,
            cmdOk = p[KEY_IR_CMD_OK] ?: 0,
            cmdSettings = p[KEY_IR_CMD_SETTINGS] ?: 0,
            cmdBack = p[KEY_IR_CMD_BACK] ?: 0,
            cmdVolUp = p[KEY_IR_CMD_VOLUP] ?: 0,
            cmdHome = p[KEY_IR_CMD_HOME] ?: 0,
            calibrationVariant = p[KEY_CALIBRATION_VARIANT] ?: -1
        )
    }

    suspend fun saveCalibration(variantIndex: Int, address: Int, powerCmd: Int) {
        dataStore.edit { p ->
            p[KEY_CALIBRATION_VARIANT] = variantIndex
            p[KEY_IR_ADDRESS] = address
            p[KEY_IR_CMD_POWER] = powerCmd
            p[KEY_IR_CMD_MUTE] = (powerCmd + 0x09) and 0xFF
            p[KEY_IR_CMD_PLAY] = (powerCmd + 0x0C) and 0xFF
            p[KEY_IR_CMD_RW] = (powerCmd + 0x15) and 0xFF
            p[KEY_IR_CMD_FF] = (powerCmd + 0x16) and 0xFF
            p[KEY_IR_CMD_UP] = (powerCmd + 0x01) and 0xFF
            p[KEY_IR_CMD_DOWN] = (powerCmd + 0x02) and 0xFF
            p[KEY_IR_CMD_LEFT] = (powerCmd + 0x04) and 0xFF
            p[KEY_IR_CMD_RIGHT] = (powerCmd + 0x03) and 0xFF
            p[KEY_IR_CMD_OK] = (powerCmd + 0x07) and 0xFF
            p[KEY_IR_CMD_SETTINGS] = (powerCmd + 0x1A) and 0xFF
            p[KEY_IR_CMD_BACK] = (powerCmd + 0x1B) and 0xFF
            p[KEY_IR_CMD_VOLUP] = (powerCmd + 0x0A) and 0xFF
            p[KEY_IR_CMD_HOME] = (powerCmd + 0x17) and 0xFF
            p[KEY_CALIBRATION_DONE] = true
        }
    }

    suspend fun updateCommand(key: Preferences.Key<Int>, newCommand: Int) {
        dataStore.edit { p ->
            p[key] = newCommand
        }
    }

    suspend fun clearCalibration() {
        dataStore.edit { p ->
            p[KEY_CALIBRATION_DONE] = false
        }
    }
}

data class IrStatus(
    val address: Int,
    val cmdPower: Int,
    val cmdMute: Int,
    val cmdPlay: Int,
    val cmdRw: Int,
    val cmdFf: Int,
    val cmdUp: Int,
    val cmdDown: Int,
    val cmdLeft: Int,
    val cmdRight: Int,
    val cmdOk: Int,
    val cmdSettings: Int,
    val cmdBack: Int,
    val cmdVolUp: Int,
    val cmdHome: Int,
    val calibrationVariant: Int
)
