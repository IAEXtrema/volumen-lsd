package com.psychovolume.app

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psychovolume.app.overlay.FloatingController
import com.psychovolume.app.ui.MainScreen
import com.psychovolume.app.ui.MainViewModel
import com.psychovolume.app.ui.SettingsScreen
import com.psychovolume.app.ui.theme.PsychoTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Los botones físicos ajustan el volumen multimedia mientras la app está visible
        volumeControlStream = AudioManager.STREAM_MUSIC

        setContent {
            PsychoTheme {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                val fullscreen by vm.fullscreen.collectAsStateWithLifecycle()
                val settings by vm.settings.collectAsStateWithLifecycle()
                val loaded by vm.settingsLoaded.collectAsStateWithLifecycle()
                var resumeTick by remember { mutableIntStateOf(0) }

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }

                // Pantalla completa inmersiva en horizontal
                DisposableEffect(fullscreen) {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    if (fullscreen) {
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        controller.show(WindowInsetsCompat.Type.systemBars())
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                    onDispose { }
                }

                // Arranca/detiene la cápsula flotante según el ajuste y el permiso (app en primer plano)
                LaunchedEffect(loaded, settings.floating, resumeTick) {
                    if (!loaded) return@LaunchedEffect
                    if (settings.floating && FloatingController.canDrawOverlays(this@MainActivity)) {
                        FloatingController.start(this@MainActivity)
                    } else if (!settings.floating) {
                        FloatingController.stop(this@MainActivity)
                    }
                }

                AnimatedContent(
                    targetState = showSettings && !fullscreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen",
                ) { settingsVisible ->
                    if (settingsVisible) {
                        SettingsScreen(vm, onBack = { showSettings = false })
                    } else {
                        MainScreen(vm, onOpenSettings = { showSettings = true })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }
}
