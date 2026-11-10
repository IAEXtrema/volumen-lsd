package com.psychovolume.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.psychovolume.app.MainActivity
import com.psychovolume.app.R
import com.psychovolume.app.data.SettingsRepository
import com.psychovolume.app.data.VisualSettings
import com.psychovolume.app.data.VolumeRepository
import com.psychovolume.app.engine.RenderParams
import com.psychovolume.app.ui.theme.PsychoTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que dibuja una cápsula flotante (TYPE_APPLICATION_OVERLAY) con Compose.
 * Implementa LifecycleOwner (LifecycleService) y SavedStateRegistryOwner, necesarios para que
 * un ComposeView funcione fuera de una Activity.
 *
 * Limitación de Android: esta ventana NO sustituye al panel de volumen del sistema; aparece junto a él.
 */
class FloatingCapsuleService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 48
        y = 360
    }

    private lateinit var volumeRepo: VolumeRepository
    private lateinit var settings: StateFlow<VisualSettings>

    override fun onCreate() {
        // Debe restaurarse antes de que el ciclo de vida pase a CREATED
        savedStateController.performRestore(null)
        super.onCreate()
        running = true

        windowManager = getSystemService(WindowManager::class.java)
        volumeRepo = VolumeRepository.get(this)
        volumeRepo.acquire()
        settings = SettingsRepository.get(this).settings
            .stateIn(lifecycleScope, SharingStarted.Eagerly, VisualSettings())

        startAsForeground()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        addOverlay()

        // Si el usuario desactiva la cápsula desde ajustes, el servicio se detiene solo
        lifecycleScope.launch {
            SettingsRepository.get(this@FloatingCapsuleService).settings
                .map { it.floating }
                .collect { enabled -> if (!enabled) stopSelf() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            lifecycleScope.launch {
                SettingsRepository.get(this@FloatingCapsuleService).update { it.copy(floating = false) }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_floating), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.channel_floating_desc)
                setShowBadge(false)
            }
        )
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingCapsuleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_capsule)
            .setContentTitle(getString(R.string.notif_floating_title))
            .setContentText(getString(R.string.notif_floating_text))
            .setContentIntent(open)
            .addAction(0, getString(R.string.notif_stop), stop)
            .setOngoing(true)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun addOverlay() {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingCapsuleService)
            setViewTreeSavedStateRegistryOwner(this@FloatingCapsuleService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val volume by volumeRepo.state.collectAsStateWithLifecycle()
                val s by settings.collectAsStateWithLifecycle()
                val sysReduce = readSystemReduceMotion()
                PsychoTheme {
                    FloatingCapsuleContent(
                        level = volume.fraction,
                        params = RenderParams.from(s, sysReduce),
                        opacity = s.opacity,
                        onDrag = this@FloatingCapsuleService::moveBy,
                        onVolume = volumeRepo::setFraction,
                        onOpenApp = this@FloatingCapsuleService::openApp,
                        onClose = {
                            lifecycleScope.launch {
                                SettingsRepository.get(this@FloatingCapsuleService).update { it.copy(floating = false) }
                            }
                        },
                    )
                }
            }
        }
        composeView = view
        windowManager.addView(view, params)
    }

    private fun readSystemReduceMotion(): Boolean = runCatching {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)

    private fun moveBy(dx: Float, dy: Float) {
        val view = composeView ?: return
        val bounds = windowManager.currentWindowMetrics.bounds
        params.x = (params.x + dx.toInt()).coerceIn(0, (bounds.width() - view.width).coerceAtLeast(0))
        params.y = (params.y + dy.toInt()).coerceIn(0, (bounds.height() - view.height).coerceAtLeast(0))
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun openApp() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    override fun onDestroy() {
        running = false
        composeView?.let { runCatching { windowManager.removeView(it) } }
        composeView = null
        volumeRepo.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "floating_capsule"
        private const val NOTIFICATION_ID = 7
        const val ACTION_STOP = "com.psychovolume.app.STOP_FLOATING"

        @Volatile
        var running: Boolean = false
            private set
    }
}
