package com.example.navigation

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.RemoteScreen
import com.example.ui.screens.SettingsScreen
import com.example.viewmodel.CalibrationViewModel
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.UsbIrViewModel

@Composable
fun MainNavigation(
    isCalibrationDone: Boolean,
    calibrationViewModel: CalibrationViewModel,
    usbIrViewModel: UsbIrViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val startDestination = if (isCalibrationDone) "remote" else "calibration"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = EaseInOutCubic)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300, easing = EaseInOutCubic)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = EaseInOutCubic)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = EaseInOutCubic)
            )
        }
    ) {
        composable("calibration") {
            CalibrationScreen(
                calibrationViewModel = calibrationViewModel,
                usbIrViewModel = usbIrViewModel,
                onNavigateNext = {
                    navController.navigate("remote") {
                        popUpTo("calibration") { inclusive = true }
                    }
                }
            )
        }
        composable("remote") {
            RemoteScreen(
                usbIrViewModel = usbIrViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateSettings = { navController.navigate("settings") },
                onNavigateRemote = { /* already here */ }
            )
        }
        composable("settings") {
            SettingsScreen(
                usbIrViewModel = usbIrViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateRemote = { navController.popBackStack() },
                onNavigateCalibration = {
                    navController.navigate("calibration") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
