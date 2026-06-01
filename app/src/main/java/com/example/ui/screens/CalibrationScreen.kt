package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IrCodeRepository
import com.example.viewmodel.CalibrationViewModel
import com.example.viewmodel.UsbIrViewModel

@Composable
fun CalibrationScreen(
    calibrationViewModel: CalibrationViewModel,
    usbIrViewModel: UsbIrViewModel,
    onNavigateNext: () -> Unit
) {
    val currentIndex by calibrationViewModel.currentIndex.collectAsState()
    val candidates = calibrationViewModel.candidates

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

    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentIndex) {
        if (currentIndex >= candidates.size) {
            showDialog = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = Modifier.background(Brush.verticalGradient(currentGradient))
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Calibración IR",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 40.dp, bottom = 16.dp)
            )

            Text(
                text = "Vamos a encontrar el código infrarrojo de tu proyector AAXA M1. Conecta tu adaptador OTG IR y apunta el extremo emisor hacia la PARTE TRASERA del proyector — el sensor fotodiodo está ubicado en el panel posterior, NO en el frente. Luego presiona 'Enviar señal'. Si el proyector reacciona (se enciende, apaga, parpadea o emite un sonido), presiona '¡Funcionó!'.",
                color = Color.LightGray,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (currentIndex < candidates.size) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF332A15)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Probando candidato ${currentIndex + 1} de 20",
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { currentIndex / 20f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFB300)
                        )
                    }
                }

                val candidate = candidates[currentIndex]
                val hexCode = IrCodeRepository.buildNecCode(candidate.address, candidate.command)
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Candidato #${currentIndex + 1} — Familia: ${candidate.family}", color = Color.Black)
                        Text("Address: 0x${candidate.address.toString(16).uppercase().padStart(2, '0')}  /  Command: 0x${candidate.command.toString(16).uppercase().padStart(2, '0')}", color = Color.Gray)
                        Text("Código 32-bit: 0x${hexCode.toString(16).uppercase().padStart(8, '0')}", fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                }

                Button(
                    onClick = {
                        usbIrViewModel.sendCode(hexCode)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF06B6D4))), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📡 Enviar señal", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        calibrationViewModel.saveSuccess(onNavigateNext)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF065F46), Color(0xFF10B981))), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✅ ¡Funcionó!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(onClick = { calibrationViewModel.nextCandidate() }) {
                    Text("Siguiente →", color = Color(0xFF06B6D4), fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = { calibrationViewModel.useDefault(onNavigateNext) }) {
                Text("Saltar y usar código por defecto", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Ningún código funcionó") },
            text = { Text("Es posible que el adaptador OTG no esté enviando señal, o que el proyector necesite apuntar con más precisión a su sensor trasero. Puedes reiniciar la calibración o usar el código por defecto.") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    calibrationViewModel.resetCalibration()
                }) {
                    Text("Reiniciar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    calibrationViewModel.useDefault(onNavigateNext)
                }) {
                    Text("Usar código por defecto")
                }
            }
        )
    }

    val error = usbIrViewModel.lastError.collectAsState()
    LaunchedEffect(error.value) {
        error.value?.let {
            snackbarHostState.showSnackbar(it)
            usbIrViewModel.clearError()
        }
    }
}
