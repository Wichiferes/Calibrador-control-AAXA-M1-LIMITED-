package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IrCodeRepository
import com.example.data.IrStatus
import com.example.ui.components.DPadComponent
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.UsbIrViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemoteScreen(
    usbIrViewModel: UsbIrViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateSettings: () -> Unit,
    onNavigateRemote: () -> Unit
) {
    val usbConnected by usbIrViewModel.usbConnected.collectAsState()
    val fullStatus by settingsViewModel.fullStatusFlow.collectAsState()

    val infiniteTransition = rememberInfiniteTransition()
    val gradientProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val color1 = Color(0xFF0D0D2B)
    val color2 = Color(0xFF1A1A3E)
    val color3 = Color(0xFF0D1F3C)
    
    val currentGradient = listOf(
        androidx.compose.ui.graphics.lerp(color1, color2, gradientProgress),
        androidx.compose.ui.graphics.lerp(color2, color3, gradientProgress)
    )

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val context = LocalContext.current
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun hapticFeedback() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        }
    }

    fun sendCommand(commandByte: Int?) {
        if (commandByte == null || fullStatus == null) return
        hapticFeedback()
        val hex = IrCodeRepository.buildNecCode(fullStatus!!.address, commandByte)
        usbIrViewModel.sendCode(hex)
    }

    fun sendRepeatStart(commandByte: Int?) {
        if (commandByte == null || fullStatus == null) return
        hapticFeedback()
        val hex = IrCodeRepository.buildNecCode(fullStatus!!.address, commandByte)
        usbIrViewModel.sendRepeatStart(hex)
    }

    fun sendRepeatStop() {
        usbIrViewModel.sendRepeatStop()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Control") },
                    label = { Text("Control") },
                    selected = true,
                    onClick = { onNavigateRemote() }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    selected = false,
                    onClick = { onNavigateSettings() }
                )
            }
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(Brush.verticalGradient(currentGradient))
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // TOP HEADER CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A3E)),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AAXA M1 Control", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        val dateFmt = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"))
                        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        Text(dateFmt.format(Date(currentTime)).replaceFirstChar { it.uppercase() }, color = Color.LightGray, fontSize = 12.sp)
                        Text(timeFmt.format(Date(currentTime)), color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(12.dp).background(if (usbConnected) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape))
                        Text(if (usbConnected) "IR OK" else "Sin IR", color = if (usbConnected) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ROW 1
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    var showPowerDialog by remember { mutableStateOf(false) }
                    
                    if (showPowerDialog) {
                        AlertDialog(
                            onDismissRequest = { showPowerDialog = false },
                            title = { Text("Encender / Apagar proyector") },
                            text = { Text("¿Enviar señal de encendido/apagado al AAXA M1?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showPowerDialog = false
                                    sendCommand(fullStatus?.cmdPower)
                                }) { Text("Enviar señal") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPowerDialog = false }) { Text("Cancelar") }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            // Note: renderEffect blur is complex in older APIs, let's use simple shadow or ignore graphic layer issue for now.
                            // To be safe we will just add a colored background to simulate glow if needed, or stick to simple UI.
                            .background(Brush.radialGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.5f), Color.Transparent)), CircleShape)
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { showPowerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF991B1B), Color(0xFFEF4444))), CircleShape), contentAlignment = Alignment.Center) {
                                Text("⏻", color = Color.White, fontSize = 28.sp)
                            }
                        }
                    }

                    Button(
                        onClick = { sendCommand(fullStatus?.cmdMute) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier.height(56.dp).width(120.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF374151), Color(0xFF6B7280))), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                            Text("🔇 Mute", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                // ROW 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TransportButton("⏮ RW") { sendCommand(fullStatus?.cmdRw) }
                    TransportButton("▶⏸ Play/Pausa") { sendCommand(fullStatus?.cmdPlay) }
                    TransportButton("FF ⏭") { sendCommand(fullStatus?.cmdFf) }
                }

                // ROW 3
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TallButton("⚙ Menú") { sendCommand(fullStatus?.cmdSettings) }
                    DPadComponent(
                        onUpPressed = { sendCommand(fullStatus?.cmdUp) },
                        onDownPressed = { sendCommand(fullStatus?.cmdDown) },
                        onLeftPressed = { sendCommand(fullStatus?.cmdLeft) },
                        onRightPressed = { sendCommand(fullStatus?.cmdRight) },
                        onOkPressed = { sendCommand(fullStatus?.cmdOk) },
                        onPressStart = { zone ->
                            when (zone) {
                                0 -> sendRepeatStart(fullStatus?.cmdUp)
                                1 -> sendRepeatStart(fullStatus?.cmdRight)
                                2 -> sendRepeatStart(fullStatus?.cmdDown)
                                3 -> sendRepeatStart(fullStatus?.cmdLeft)
                            }
                        },
                        onPressEnd = { sendRepeatStop() }
                    )
                    TallButton("↩ Atrás") { sendCommand(fullStatus?.cmdBack) }
                }

                // ROW 4
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.weight(1f).height(52.dp).pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                sendRepeatStart(fullStatus?.cmdVolUp)
                                tryAwaitRelease()
                                sendRepeatStop()
                            },
                            onTap = { sendCommand(fullStatus?.cmdVolUp) }
                        )
                    }.background(Color(0xFF1E1E3F), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("🔊 Vol +", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TransportButton("🏠 Home", modifier = Modifier.weight(1f)) { sendCommand(fullStatus?.cmdHome) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f).height(52.dp).alpha(0.4f).background(Color.Gray, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("—", color = Color.White)
                    }
                }
            }

            // INFO STRIP
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Text(
                    text = "⚠ Apunta el adaptador OTG hacia la PARTE TRASERA del proyector. El sensor IR del AAXA M1 está en el panel posterior, no en el frente.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun TransportButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E3F)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun TallButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(80.dp).width(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E3F)),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = Color.White, fontSize = 12.sp)
        }
    }
}
