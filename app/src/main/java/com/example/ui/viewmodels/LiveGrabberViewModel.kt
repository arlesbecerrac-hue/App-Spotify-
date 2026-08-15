package com.example.ui.viewmodels

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MusicGrabberApplication
import com.example.data.PlaybackEvent
import com.example.data.ScannedCacheAudio
import com.example.data.TrackCopy
import com.example.service.LivePlaybackTracker
import com.example.service.MusicNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LiveGrabberViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MusicGrabberApplication
    private val repository = app.repository
    private val audioCaptureEngine = app.audioCaptureEngine
    private val extractorEngine = app.extractorEngine

    val currentPlayingTrack: StateFlow<PlaybackEvent?> = LivePlaybackTracker.currentTrack
    val isServiceActive: StateFlow<Boolean> = LivePlaybackTracker.isServiceActive
    val lastDetectedSource: StateFlow<String> = LivePlaybackTracker.lastDetectedSource

    val isRecording: StateFlow<Boolean> = audioCaptureEngine.isRecording
    val recordingDuration: StateFlow<Int> = audioCaptureEngine.recordingDurationSeconds
    val currentAmplitude: StateFlow<Int> = audioCaptureEngine.currentAmplitude

    val recentEvents: StateFlow<List<PlaybackEvent>> = repository.recentPlaybackEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun isNotificationAccessGranted(): Boolean {
        return MusicNotificationListenerService.isNotificationListenerEnabled(getApplication())
    }

    fun openNotificationSettings() {
        MusicNotificationListenerService.openNotificationListenerSettings(getApplication())
    }

    fun startAudioCapture() {
        val current = currentPlayingTrack.value
        val started = audioCaptureEngine.startRecording(current)
        if (started) {
            _statusMessage.value = "Grabando audio en alta definición..."
        } else {
            _statusMessage.value = "No se pudo iniciar la grabación"
        }
    }

    fun stopAudioCaptureAndSave() {
        viewModelScope.launch {
            _isExtracting.value = true
            val result = audioCaptureEngine.stopRecordingAndSave()
            _isExtracting.value = false
            result.onSuccess { track ->
                _statusMessage.value = "Copia guardada: ${track.artist} - ${track.title}"
                Toast.makeText(getApplication(), "¡Canción guardada y organizada con éxito!", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                _statusMessage.value = "Error al guardar: ${e.message}"
            }
        }
    }

    fun cancelAudioCapture() {
        audioCaptureEngine.cancelRecording()
        _statusMessage.value = "Grabación cancelada"
    }

    fun quickGrabCurrentTrack(event: PlaybackEvent) {
        viewModelScope.launch {
            _isExtracting.value = true
            val simulatedCache = ScannedCacheAudio(
                id = "live_${event.id}_${System.currentTimeMillis()}",
                fileAbsolutePath = "",
                fileName = "${event.title}.m4a",
                sizeBytes = 4200000L,
                lastModified = System.currentTimeMillis(),
                detectedFormat = "m4a",
                estimatedDurationMs = if (event.durationMs > 0) event.durationMs else 210000L,
                probableTitle = event.title,
                probableArtist = event.artist,
                probableSource = event.appName,
                isRecognizedAudioHeader = true
            )

            val result = extractorEngine.extractAndOrganizeCacheItem(
                cacheItem = simulatedCache,
                customTitle = event.title,
                customArtist = event.artist,
                customAlbum = event.album.ifBlank { "Copia de ${event.appName}" }
            )
            _isExtracting.value = false

            result.onSuccess { track ->
                if (event.id > 0) {
                    repository.markEventSaved(event.id, track.id)
                }
                _statusMessage.value = "Copia organizada: ${track.title}"
                Toast.makeText(getApplication(), "Copia creada en Music/OrganizedMusic/${track.artist}", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearPlaybackHistory()
            _statusMessage.value = "Historial de reproducción limpiado"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
