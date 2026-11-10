package com.psychovolume.app.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Punto único para arrancar/detener la cápsula flotante y gestionar su permiso. */
object FloatingController {

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun overlayPermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    /** Debe llamarse con la app en primer plano (restricciones de servicios en primer plano). */
    fun start(context: Context) {
        if (!canDrawOverlays(context)) return
        ContextCompat.startForegroundService(context, Intent(context, FloatingCapsuleService::class.java))
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, FloatingCapsuleService::class.java))
    }
}
