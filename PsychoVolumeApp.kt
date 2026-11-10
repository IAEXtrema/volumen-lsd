package com.psychovolume.app

import android.app.Application
import com.psychovolume.app.data.SettingsRepository
import com.psychovolume.app.data.VolumeRepository

class PsychoVolumeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializa los singletons con el contexto de aplicación (sin fugas de Activity)
        VolumeRepository.get(this)
        SettingsRepository.get(this)
    }
}
