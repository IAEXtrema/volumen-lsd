package com.psychovolume.app.ui

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.psychovolume.app.data.SettingsRepository
import com.psychovolume.app.data.VisualSettings
import com.psychovolume.app.data.VolumeRepository
import com.psychovolume.app.data.VolumeState
import com.psychovolume.app.engine.RenderParams
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class DemoMode { Off, Manual, Auto }

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val volumeRepo = VolumeRepository.get(app)
    private val settingsRepo = SettingsRepository.get(app)

    val volume: StateFlow<VolumeState> = volumeRepo.state

    val settings: StateFlow<VisualSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, VisualSettings())

    /** true cuando DataStore ya entregó los ajustes reales (evita actuar sobre los valores por defecto). */
    val settingsLoaded: StateFlow<Boolean> =
        settingsRepo.settings.map { true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _systemReduceMotion = MutableStateFlow(readSystemReduceMotion())

    val renderParams: StateFlow<RenderParams> =
        combine(settings, _systemReduceMotion) { s, sys -> RenderParams.from(s, sys) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, RenderParams())

    private val _demo = MutableStateFlow(DemoMode.Off)
    val demo: StateFlow<DemoMode> = _demo.asStateFlow()

    private val _demoLevel = MutableStateFlow(0f)

    /** Valor mientras el usuario arrastra el slider (continuo, aunque el volumen tenga pasos). */
    private val _drag = MutableStateFlow<Float?>(null)

    private val _fullscreen = MutableStateFlow(false)
    val fullscreen: StateFlow<Boolean> = _fullscreen.asStateFlow()

    private val _controlsVisible = MutableStateFlow(true)
    val controlsVisible: StateFlow<Boolean> = _controlsVisible.asStateFlow()

    /** Nivel que se muestra (y que alimenta a la animación). */
    val displayLevel: StateFlow<Float> =
        combine(volume, _demo, _demoLevel, _drag) { v, demo, demoLevel, drag ->
            when {
                demo != DemoMode.Off -> demoLevel
                drag != null -> drag
                else -> v.fraction
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, volumeRepo.state.value.fraction)

    private var autoJob: Job? = null

    init {
        volumeRepo.acquire()
    }

    fun onResume() {
        volumeRepo.refresh()
        _systemReduceMotion.value = readSystemReduceMotion()
    }

    fun onSliderChange(f: Float) {
        when (_demo.value) {
            DemoMode.Off -> {
                _drag.value = f
                volumeRepo.setFraction(f)
            }
            DemoMode.Manual -> _demoLevel.value = f
            DemoMode.Auto -> {
                setDemo(DemoMode.Manual)
                _demoLevel.value = f
            }
        }
    }

    fun onSliderFinished() {
        _drag.value = null
    }

    fun stepVolume(up: Boolean) = volumeRepo.step(up)

    fun setDemo(mode: DemoMode) {
        autoJob?.cancel()
        autoJob = null
        if (mode != DemoMode.Off && _demo.value == DemoMode.Off) {
            _demoLevel.value = volume.value.fraction
        }
        _demo.value = mode
        if (mode == DemoMode.Auto) {
            autoJob = viewModelScope.launch {
                // Recorrido triangular 0 -> 1 -> 0 en ~26 s; el motor lo suaviza a 60 FPS.
                var t = (_demoLevel.value * 13f)
                while (isActive) {
                    delay(33)
                    t += 0.033f
                    val period = 26f
                    val p = (t % period) / period
                    _demoLevel.value = 1f - abs(1f - 2f * p)
                }
            }
        }
    }

    fun setFullscreen(on: Boolean) {
        _fullscreen.value = on
        _controlsVisible.value = true
    }

    fun toggleControls() {
        _controlsVisible.value = !_controlsVisible.value
    }

    fun updateSettings(transform: (VisualSettings) -> VisualSettings) {
        viewModelScope.launch { settingsRepo.update(transform) }
    }

    private fun readSystemReduceMotion(): Boolean = runCatching {
        Settings.Global.getFloat(
            getApplication<Application>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }.getOrDefault(false)

    override fun onCleared() {
        volumeRepo.release()
        super.onCleared()
    }
}
