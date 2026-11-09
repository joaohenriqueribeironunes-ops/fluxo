package com.lambda.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.ui.AccessibilitySettings
import com.lambda.app.ui.AppPreferences
import com.lambda.app.ui.BehaviorScreen
import com.lambda.app.ui.BehaviorViewModel
import com.lambda.app.ui.CommitmentsScreen
import com.lambda.app.ui.CommitmentsViewModel
import com.lambda.app.ui.SettingsScreen
import com.lambda.app.ui.StudyScreen
import com.lambda.app.ui.StudyViewModel
import com.lambda.app.util.TtsHelper
import com.lambda.coreui.design.AuroraBackdrop
import com.lambda.coreui.design.DockItem
import com.lambda.coreui.design.GlassDock
import com.lambda.coreui.design.LocalGlassStyle
import com.lambda.coreui.theme.LambdaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openVoice = intent?.getBooleanExtra("open_voice", false) == true
        setContent {
            val a11y by prefs.settings.collectAsStateWithLifecycle(initialValue = AccessibilitySettings())
            LambdaTheme(
                lowStimulus = a11y.lowStimulus,
                highContrast = a11y.highContrast,
                reducedMotion = a11y.reducedMotion,
                fontScale = a11y.fontScale,
            ) {
                AppShell(a11y = a11y, prefs = prefs, startTab = if (openVoice) 1 else 0)
            }
        }
    }
}

@Composable
private fun AppShell(a11y: AccessibilitySettings, prefs: AppPreferences, startTab: Int) {
    var tab by rememberSaveable { mutableIntStateOf(startTab) }
    val style = LocalGlassStyle.current
    val appContext = LocalContext.current.applicationContext
    val tts = remember { TtsHelper(appContext) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }
    val speak: (String) -> Unit = { text -> tts.speak(text, enabled = !a11y.noUnexpectedSounds) }

    val dock = remember {
        listOf(
            DockItem("Tradutor", Icons.Rounded.Translate),
            DockItem("Estudos", Icons.Rounded.MenuBook),
            DockItem("Agenda", Icons.Rounded.EventNote),
            DockItem("Ajustes", Icons.Rounded.Settings),
        )
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackdrop(Modifier.fillMaxSize(), style)
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            Crossfade(
                targetState = tab,
                animationSpec = tween(if (style.reducedMotion) 0 else 260),
                label = "tab",
            ) { current ->
                when (current) {
                    0 -> {
                        val vm: BehaviorViewModel = hiltViewModel()
                        BehaviorScreen(vm, onSpeak = speak)
                    }
                    1 -> {
                        val vm: StudyViewModel = hiltViewModel()
                        StudyScreen(vm)
                    }
                    2 -> {
                        val vm: CommitmentsViewModel = hiltViewModel()
                        CommitmentsScreen(vm)
                    }
                    else -> SettingsScreen(a11y = a11y, prefs = prefs)
                }
            }
        }
        GlassDock(
            items = dock,
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
