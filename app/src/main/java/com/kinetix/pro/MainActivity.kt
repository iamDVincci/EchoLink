package com.kinetix.pro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kinetix.pro.core.ui.theme.KinetixTheme
import com.kinetix.pro.core.ui.theme.ObsidianCanvas
import com.kinetix.pro.navigation.KinetixNavHost
import com.kinetix.pro.navigation.Screen

class MainActivity : ComponentActivity() {

    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            KinetixTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianCanvas
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(sharedUrl) {
                        if (sharedUrl != null) {
                            navController.navigate(Screen.Download.route) {
                                launchSingleTop = true
                            }
                        }
                    }

                    KinetixNavHost(
                        navController = navController,
                        startDestination = Screen.Download.route,
                        sharedUrl = sharedUrl,
                        onSharedUrlConsumed = { sharedUrl = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                sharedUrl = sharedText.trim()
            }
        }
    }
}
