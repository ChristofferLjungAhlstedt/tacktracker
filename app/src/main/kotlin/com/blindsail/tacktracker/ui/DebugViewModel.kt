package com.blindsail.tacktracker.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blindsail.tacktracker.sensors.CompassSource
import com.blindsail.tacktracker.sensors.HeadingFusion
import com.blindsail.tacktracker.sensors.HeadingState
import com.blindsail.tacktracker.sensors.LocationSource
import com.blindsail.tacktracker.settings.MountOrientation
import com.blindsail.tacktracker.settings.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DebugUiState(
    val heading: HeadingState? = null,
    val mounting: MountOrientation = MountOrientation.FLAT_TOP_TO_BOW,
    val gpsError: Boolean = false,
    val compassMissing: Boolean = false,
)

/** Debug screen state. Runs on the main thread, so HeadingFusion needs no locking. */
class DebugViewModel(app: Application) : AndroidViewModel(app) {
    private val mounting = MutableStateFlow(MountOrientation.FLAT_TOP_TO_BOW)
    private val fusion = HeadingFusion(HeadingFusion.Config.from(Settings()))
    private val compassSource = CompassSource(app) { mounting.value }
    private val locationSource = LocationSource(app)

    private val _ui = MutableStateFlow(DebugUiState())
    val ui: StateFlow<DebugUiState> = _ui.asStateFlow()

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        _ui.update { it.copy(gpsError = false, compassMissing = !compassSource.isAvailable) }
        job = viewModelScope.launch {
            launch {
                compassSource.samples()
                    .catch { _ui.update { s -> s.copy(compassMissing = true) } }
                    .collect { fusion.onCompass(it) }
            }
            launch {
                locationSource.fixes()
                    .catch { _ui.update { s -> s.copy(gpsError = true) } }
                    .collect { fusion.onGps(it) }
            }
            // Publish at 4 Hz: the compass delivers ~50 Hz, far too fast for a screen reader.
            while (isActive) {
                delay(250)
                val state = fusion.current(SystemClock.elapsedRealtime())
                _ui.update { it.copy(heading = state) }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun restart() {
        stop()
        start()
    }

    fun setMounting(m: MountOrientation) {
        mounting.value = m
        _ui.update { it.copy(mounting = m) }
    }
}
