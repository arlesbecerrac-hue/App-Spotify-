package com.example.engine

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import com.example.data.MusicRepository
import com.example.data.PlaybackEvent
import com.example.data.TrackCopy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioCaptureEngine(
    private val context: Context,
    private val repository: MusicRepository
) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L
    private var amplitudeJob: Job? = null
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

    private val _capturedTrackMetadata = MutableStateFlow<PlaybackEvent?>(null)
    val capturedTrackMetadata: StateFlow<PlaybackEvent?> = _capturedTrackMetadata.asStateFlow()

    fun startRecording(currentEvent: PlaybackEvent?): Boolean {
        if (_isRecording.value) return false
        _capturedTrackMetadata.value = currentEvent

        val artist = currentEvent?.artist?.ifBlank { "Grabacion" } ?: "Grabacion"
        val title = currentEvent?.title?.ifBlank { "Pista_${System.currentTimeMillis()}" } ?: "Pista_${System.currentTimeMillis()}"
        val album = currentEvent?.album?.ifBlank { "Capturas En Vivo" } ?: "Capturas En Vivo"

        val sanitizedArtist = sanitize(artist)
        val sanitizedAlbum = sanitize(album)
        val sanitizedTitle = sanitize(title)

        val musicDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "ARLSIC/$sanitizedArtist/$sanitizedAlbum")
        if (!musicDir.exists()) {
            musicDir.mkdirs()
        }

        val outputFile = File(musicDir, "$sanitizedArtist - $sanitizedTitle.m4a")
        currentOutputFile = outputFile

        try {
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(256000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationSeconds.value = 0

            // Start timer and amplitude sampler
            amplitudeJob?.cancel()
            amplitudeJob = engineScope.launch {
                while (_isRecording.value) {
                    val elapsed = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                    _recordingDurationSeconds.value = elapsed
                    try {
                        val maxAmp = recorder?.maxAmplitude ?: 0
                        _currentAmplitude.value = (maxAmp / 327.67).toInt().coerceIn(0, 100)
                    } catch (_: Exception) {}
                    delay(100)
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _isRecording.value = false
            currentOutputFile?.delete()
            return false
        }
    }

    suspend fun stopRecordingAndSave(): Result<TrackCopy> = withContext(Dispatchers.IO) {
        if (!_isRecording.value || recorder == null) {
            return@withContext Result.failure(Exception("No hay grabación activa"))
        }

        amplitudeJob?.cancel()
        amplitudeJob = null

        val durationMs = System.currentTimeMillis() - recordingStartTime

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
            _isRecording.value = false
            _currentAmplitude.value = 0
            _recordingDurationSeconds.value = 0
        }

        val file = currentOutputFile
        if (file == null || !file.exists() || file.length() == 0L) {
            return@withContext Result.failure(Exception("El archivo de audio no se grabó correctamente"))
        }

        val meta = _capturedTrackMetadata.value
        val title = meta?.title?.ifBlank { "Pista Grabada" } ?: "Pista Grabada"
        val artist = meta?.artist?.ifBlank { "Artista Desconocido" } ?: "Artista Desconocido"
        val album = meta?.album?.ifBlank { "Captura de Audio" } ?: "Captura de Audio"
        val sourceApp = meta?.appName ?: "Grabación Directa"

        val trackCopy = TrackCopy(
            title = title,
            artist = artist,
            album = album,
            sourceApp = sourceApp,
            sourcePackage = meta?.packageName ?: "",
            filePath = file.absolutePath,
            fileUri = file.toURI().toString(),
            durationMs = durationMs,
            fileSizeBytes = file.length(),
            format = "m4a",
            bitrateKbps = 256,
            savedTimestamp = System.currentTimeMillis(),
            albumArtUri = meta?.albumArtUri,
            category = "Grabación en Directo"
        )

        val insertedId = repository.insertTrackCopy(trackCopy)
        if (meta != null && meta.id > 0) {
            repository.markEventSaved(meta.id, insertedId)
        }

        Result.success(trackCopy.copy(id = insertedId))
    }

    fun cancelRecording() {
        if (!_isRecording.value) return
        amplitudeJob?.cancel()
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        recorder = null
        _isRecording.value = false
        _currentAmplitude.value = 0
        _recordingDurationSeconds.value = 0
        currentOutputFile?.delete()
        currentOutputFile = null
    }

    private fun sanitize(input: String): String {
        return input.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "Audio" }
    }
}
