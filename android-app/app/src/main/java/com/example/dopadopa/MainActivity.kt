package com.example.dopadopa

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.dopadopa.state.AppState
import com.example.dopadopa.ui.screens.DashboardScreen
import com.example.dopadopa.ui.screens.LoginScreen

/**
 * index.html のログイン画面 / アプリ本体の出し分け相当（iOS 版 ContentView.swift / DopaDopaApp.swift）。
 * 実際の画面ロック計測は [com.example.dopadopa.tracker.ScreenTrackingService]
 * （[DopaDopaApplication] から起動）が Activity のライフサイクルとは独立して行う。
 */
class MainActivity : ComponentActivity() {

    private val appState: AppState by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            DopaDopaRoot(appState = appState)
        }
    }
}

@Composable
private fun DopaDopaRoot(appState: AppState) {
    val uiState by appState.uiState.collectAsState()
    Crossfade(targetState = uiState.username != null, label = "login-dashboard") { isLoggedIn ->
        if (isLoggedIn) {
            DashboardScreen(appState = appState)
        } else {
            LoginScreen(onLogin = { username -> appState.login(username) })
        }
    }
}
