package com.afilishop.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afilishop.app.data.AfiliShopRepository
import com.afilishop.app.ui.AfiliShopApp
import com.afilishop.app.ui.AfiliShopViewModel
import com.afilishop.app.ui.screens.NativeSplashScreen
import com.afilishop.app.ui.theme.AfiliShopTheme

class MainActivity : ComponentActivity() {
    private val repository by lazy { AfiliShopRepository(this) }
    private var incomingIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingIntent = intent
        setContent {
            AfiliShopTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                if (showSplash) {
                    NativeSplashScreen(onDone = { showSplash = false })
                } else {
                    val factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return AfiliShopViewModel(repository) as T
                        }
                    }
                    val viewModel: AfiliShopViewModel = viewModel(factory = factory)
                    AfiliShopApp(viewModel = viewModel, initialIntent = incomingIntent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntent = intent
    }
}
