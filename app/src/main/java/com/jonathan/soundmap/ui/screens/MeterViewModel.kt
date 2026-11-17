package com.jonathan.soundmap.ui.screens

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonathan.soundmap.data.repository.NoiseSpotRepository
import com.jonathan.soundmap.data.sensor.AudioMeterManager
import com.jonathan.soundmap.data.sensor.Coordinates
import com.jonathan.soundmap.data.sensor.LocationTracker
import com.jonathan.soundmap.domain.acoustics.NoiseMath
import com.jonathan.soundmap.domain.acoustics.NoiseSampler
import com.jonathan.soundmap.domain.model.NoiseSpot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeterUiState(
    val currentDb: Double = 0.0,
    val isListening: Boolean = false,
    val isSampling: Boolean = false,
    val samplingProgress: Float = 0f,
    /** Promedio de la última medición de 10 s, pendiente de guardar. */
    val pendingAverageDb: Double? = null,
    val location: Coordinates? = null,
    val isLocating: Boolean = false,
    val locationFailed: Boolean = false,
    val error: String? = null,
    /** Mensaje breve para un Snackbar (por ejemplo, "Lugar guardado"). */
    val message: String? = null,
)

@HiltViewModel
class MeterViewModel @Inject constructor(
    private val audioMeter: AudioMeterManager,
    private val locationTracker: LocationTracker,
    private val repository: NoiseSpotRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MeterUiState())
    val state: StateFlow<MeterUiState> = _state.asStateFlow()

    private val sampler = NoiseSampler()
    private var listenJob: Job? = null
    private var hasLocationPermission = false

    /** Abre el micrófono. Llamar solo con el permiso RECORD_AUDIO concedido. */
    @Suppress("MissingPermission")
    fun startListening() {
        if (listenJob?.isActive == true) return
        _state.update { it.copy(isListening = true, error = null) }
        listenJob = viewModelScope.launch {
            audioMeter.decibels()
                .catch { e -> _state.update { it.copy(isListening = false, error = e.message ?: "Error del micrófono") } }
                .collect { db -> onReading(db) }
        }
    }

    /** Libera el micrófono (al salir de la pantalla o pasar la app a segundo plano). */
    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        if (sampler.isRunning) sampler.cancel()
        _state.update { it.copy(isListening = false, isSampling = false, samplingProgress = 0f) }
    }

    fun setLocationPermission(granted: Boolean) {
        hasLocationPermission = granted
    }

    /** Empieza la medición estable de 10 segundos. */
    fun startSampling() {
        if (!_state.value.isListening || sampler.isRunning) return
        sampler.start(SystemClock.elapsedRealtime())
        _state.update { it.copy(isSampling = true, samplingProgress = 0f, pendingAverageDb = null) }
    }

    private fun onReading(db: Double) {
        val now = SystemClock.elapsedRealtime()
        _state.update { it.copy(currentDb = NoiseMath.smooth(it.currentDb, db)) }
        if (!sampler.isRunning) return
        val progress = sampler.add(db, now)
        if (sampler.isComplete(now)) {
            val average = sampler.finish()
            _state.update { it.copy(isSampling = false, samplingProgress = 1f, pendingAverageDb = average) }
            if (average != null) requestLocation()
        } else {
            _state.update { it.copy(samplingProgress = progress) }
        }
    }

    @Suppress("MissingPermission")
    fun requestLocation() {
        if (!hasLocationPermission) {
            _state.update { it.copy(location = null, isLocating = false, locationFailed = true) }
            return
        }
        _state.update { it.copy(isLocating = true, locationFailed = false) }
        viewModelScope.launch {
            val coords = locationTracker.currentLocation()
            _state.update { it.copy(location = coords, isLocating = false, locationFailed = coords == null) }
        }
    }

    fun saveSpot(name: String) {
        val s = _state.value
        val avg = s.pendingAverageDb ?: return
        val coords = s.location ?: return
        viewModelScope.launch {
            repository.save(
                NoiseSpot(
                    name = name.trim().ifBlank { "Lugar sin nombre" },
                    averageDb = avg,
                    timestampMillis = System.currentTimeMillis(),
                    latitude = coords.latitude,
                    longitude = coords.longitude,
                ),
            )
            _state.update { it.copy(pendingAverageDb = null, samplingProgress = 0f, message = "Lugar guardado en el mapa") }
        }
    }

    fun discardMeasurement() {
        _state.update { it.copy(pendingAverageDb = null, samplingProgress = 0f) }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    override fun onCleared() {
        stopListening()
    }
}
