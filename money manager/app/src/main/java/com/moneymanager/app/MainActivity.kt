package com.moneymanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneymanager.app.ui.MoneyManagerApp
import com.moneymanager.app.ui.theme.MoneyManagerTheme
import com.moneymanager.app.viewmodel.MoneyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestSmsPermissions()
        setContent {
            val viewModel: MoneyViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            MoneyManagerTheme(themeMode = state.themeMode) {
                MoneyManagerApp(viewModel = viewModel)
            }
        }
    }

    private fun requestSmsPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.RECEIVE_SMS
            ),
            42
        )
    }
}
