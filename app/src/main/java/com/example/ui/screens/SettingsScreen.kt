package com.example.ui.screens

import android.hardware.usb.UsbManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import com.example.data.IrCodeRepository
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.UsbIrViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    usbIrViewModel: UsbIrViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateRemote: () -> Unit,
    onNavigateCalibration: () -> Unit
) {
    val usbConnected by usbIrViewModel.usbConnected.collectAsState()
    val fullStatus by settingsViewModel.fullStatusFlow.collectAsState()
    val context = LocalContext.current
    val usbManager = context.getSystemService(android.content.Context.USB_SERVICE) as UsbManager

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateRemote) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A3E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Control") },
                    label = { Text("Control") },
                    selected = false,
                    onClick = { onNavigateRemote() }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    selected = true,
                    onClick = { }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D2B)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Estado del Adaptador USB IR", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A3E)), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(if (usbConnected) "Adaptador: Conectado ✅" else "Adaptador: No detectado ❌", color = Color.White)
                        if (usbConnected) {
                            val deviceList = usbManager.deviceList
                            if (deviceList.isNotEmpty()) {
                                val dev = deviceList.values.first()
                                Text("USB: ${dev.productName ?: dev.deviceName} (Vendor: ${dev.vendorId})", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { usbIrViewModel.releaseUsb() }) {
                            Text("Reconectar")
                        }
                    }
                }
            }

            item {
                Text("Códigos IR calibrados", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
            }

            fullStatus?.let { status ->
                val buttons = listOf(
                    Triple("Power", status.cmdPower, IrCodeRepository.KEY_IR_CMD_POWER),
                    Triple("Mute", status.cmdMute, IrCodeRepository.KEY_IR_CMD_MUTE),
                    Triple("Play/Pause", status.cmdPlay, IrCodeRepository.KEY_IR_CMD_PLAY),
                    Triple("RW", status.cmdRw, IrCodeRepository.KEY_IR_CMD_RW),
                    Triple("FF", status.cmdFf, IrCodeRepository.KEY_IR_CMD_FF),
                    Triple("Up", status.cmdUp, IrCodeRepository.KEY_IR_CMD_UP),
                    Triple("Down", status.cmdDown, IrCodeRepository.KEY_IR_CMD_DOWN),
                    Triple("Left", status.cmdLeft, IrCodeRepository.KEY_IR_CMD_LEFT),
                    Triple("Right", status.cmdRight, IrCodeRepository.KEY_IR_CMD_RIGHT),
                    Triple("OK", status.cmdOk, IrCodeRepository.KEY_IR_CMD_OK),
                    Triple("Menú", status.cmdSettings, IrCodeRepository.KEY_IR_CMD_SETTINGS),
                    Triple("Atrás", status.cmdBack, IrCodeRepository.KEY_IR_CMD_BACK),
                    Triple("Vol+", status.cmdVolUp, IrCodeRepository.KEY_IR_CMD_VOLUP),
                    Triple("Home", status.cmdHome, IrCodeRepository.KEY_IR_CMD_HOME)
                )

                items(buttons.size) { i ->
                    val (name, cmd, key) = buttons[i]
                    val hexCode = IrCodeRepository.buildNecCode(status.address, cmd)
                    
                    var showEditDialog by remember { mutableStateOf(false) }
                    var editValue by remember { mutableStateOf(cmd.toString(16).uppercase()) }
                    var editError by remember { mutableStateOf(false) }

                    if (showEditDialog) {
                        AlertDialog(
                            onDismissRequest = { showEditDialog = false },
                            title = { Text("Reasignar código — $name") },
                            text = {
                                Column {
                                    Text("Ingresa el nuevo byte de comando en hexadecimal (ejemplo: 0x45 o simplemente 45):")
                                    TextField(
                                        value = editValue,
                                        onValueChange = { editValue = it; editError = false },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                        isError = editError
                                    )
                                    if (editError) {
                                        Text("Valor inválido — usa formato hexadecimal (00 a FF).", color = Color.Red, fontSize = 12.sp)
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val cleaned = editValue.removePrefix("0x").trim()
                                    val parsed = cleaned.toIntOrNull(16)
                                    if (parsed != null && parsed in 0..255) {
                                        settingsViewModel.updateCommand(key, parsed)
                                        showEditDialog = false
                                    } else {
                                        editError = true
                                    }
                                }) { Text("Guardar") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") }
                            }
                        )
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A3E)), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Addr: 0x${status.address.toString(16).uppercase().padStart(2, '0')} | Cmd: 0x${cmd.toString(16).uppercase().padStart(2, '0')}", color = Color.Gray, fontSize = 12.sp)
                                Text("32-bit: 0x${hexCode.toString(16).uppercase().padStart(8, '0')}", color = Color.Gray, fontSize = 12.sp)
                                Button(onClick = { usbIrViewModel.sendCode(hexCode) }, modifier = Modifier.height(32.dp).padding(top = 4.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                    Text("🧪 Probar", fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = { editValue = cmd.toString(16).uppercase(); showEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Text("Variante calibrada", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A3E)), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (fullStatus != null && fullStatus!!.calibrationVariant >= 0) {
                            Text("Variante: #${fullStatus!!.calibrationVariant + 1}", color = Color.White)
                            Text("Address Byte: 0x${fullStatus!!.address.toString(16).uppercase().padStart(2, '0')}", color = Color.Gray)
                            // We don't have the family name stored, so we just show variant index
                        } else {
                            Text("No calibrado aún.", color = Color.Gray)
                        }
                    }
                }
            }

            item {
                Text("Recalibrar", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                var showRecalibrateConfirm by remember { mutableStateOf(false) }
                if (showRecalibrateConfirm) {
                    AlertDialog(
                        onDismissRequest = { showRecalibrateConfirm = false },
                        title = { Text("Reiniciar calibración") },
                        text = { Text("¿Estás seguro? Esto borrará tus códigos guardados y tendrás que calibrar de nuevo.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showRecalibrateConfirm = false
                                settingsViewModel.restartCalibration(onNavigateCalibration)
                            }) { Text("Sí, reiniciar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRecalibrateConfirm = false }) { Text("Cancelar") }
                        }
                    )
                }
                Button(onClick = { showRecalibrateConfirm = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("🔄 Reiniciar calibración")
                }
            }

            item {
                Text("Información del Proyector", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A3E)), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val specs = listOf(
                            "Modelo" to "AAXA M1 Limited (MP100-3)",
                            "Tecnología" to "LCoS (Liquid Crystal on Silicon)",
                            "Resolución nativa" to "800×600 (SVGA)",
                            "Luminosidad" to "75 lúmenes ANSI",
                            "Contraste" to "1000:1 (Full On/Off)",
                            "Vida útil LED" to "15,000 horas",
                            "Sensor IR" to "Panel posterior — 940 nm / 38 kHz",
                            "Protocolo IR" to "NEC Pulse Distance Modulation, 32 bits",
                            "Batería del control original" to "CR2025 o CR2032",
                            "Número de botones" to "14 (según especificación)",
                            "Part numbers de control" to "KP40001, RTKP40001, M1AAXA, RTM1AAXA, AAXA-RTM1AAXA"
                        )
                        specs.forEach { (label, value) ->
                            Text("$label: $value", color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
