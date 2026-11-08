package com.equilibrio.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equilibrio.app.ui.AppNavHost
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.Routes
import com.equilibrio.core.ui.glass.LiquidBackground
import com.equilibrio.core.ui.theme.AccessibilitySettings
import com.equilibrio.core.ui.theme.EquilibrioTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute.value = intent?.getStringExtra(Routes.EXTRA_ROUTE)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val risk by viewModel.risk.collectAsStateWithLifecycle()
            val loaded by viewModel.loaded.collectAsStateWithLifecycle()
            val pending by pendingRoute.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) { viewModel.notifyRecentCrashIfAny() }

            LaunchedEffect(settings.secureScreen) {
                if (settings.secureScreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            EquilibrioTheme(
                darkTheme = settings.darkTheme,
                accessibility = AccessibilitySettings(
                    reduceMotion = settings.reduceMotion,
                    reduceTransparency = settings.reduceTransparency,
                    highContrast = settings.highContrast,
                    fontScale = settings.fontScale
                ),
                riskScore = risk?.score ?: 0
            ) {
                if (!loaded) {
                    LoadingScreen()
                } else {
                    val startRoute = remember {
                        if (settings.onboardingDone) Routes.HOME else Routes.ONBOARDING
                    }
                    AppNavHost(
                        viewModel = viewModel,
                        startRoute = startRoute,
                        pendingRoute = pending,
                        onPendingConsumed = { pendingRoute.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = intent.getStringExtra(Routes.EXTRA_ROUTE)
    }
}

@androidx.compose.runtime.Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize()) {
        LiquidBackground()
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = "Abrindo o seu espaço seguro…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
