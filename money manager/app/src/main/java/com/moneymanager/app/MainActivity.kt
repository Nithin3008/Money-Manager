package com.moneymanager.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.moneymanager.app.model.ScreenTab
import com.moneymanager.app.ui.MoneyManagerApp
import com.moneymanager.app.ui.theme.MoneyManagerTheme
import com.moneymanager.app.viewmodel.MoneyViewModel

class MainActivity : ComponentActivity() {
    private val moneyViewModel: MoneyViewModel by viewModels()

    companion object {
        /** Set by home-screen widgets; routes the app to the matching tab on open. */
        const val EXTRA_OPEN_TAB = "mm_open_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestSmsPermissions()
        routeWidgetIntent(intent)
        setContent {
            val state by moneyViewModel.uiState.collectAsState()
            MoneyManagerTheme(
                themeMode = state.themeMode,
                uiAccent = state.uiAccent,
                uiSurface = state.uiSurface,
                customAccentHex = state.customAccentHex
            ) {
                MoneyManagerApp(viewModel = moneyViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeWidgetIntent(intent)
    }

    private fun routeWidgetIntent(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_OPEN_TAB) == "dashboard") {
            moneyViewModel.selectTab(ScreenTab.Dashboard)
        }
    }

    private fun requestSmsPermissions() {
        val needsReadSms = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) != PackageManager.PERMISSION_GRANTED
        if (!needsReadSms) return
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
            ),
            42
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (
            requestCode == 42 &&
            grantResults.isNotEmpty() &&
            grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        ) {
            moneyViewModel.scanForNewMessages()
        }
    }
}
