package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.TrackCopy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class MusicPlayerController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<TrackCopy?>(null)
    val currentTrack: StateFlow<TrackCopy?> = _currentTrack.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _currentArtist = MutableStateFlow("")
    val currentArtist: StateFlow<String> = _currentArtist.asStateFlow()

    private val _currentProgressMs = MutableStateFlow(0)
    val currentProgressMs: StateFlow<Int> = _currentProgressMs.asStateFlow()

    private val _currentDurationMs = MutableStateFlow(0)
    val currentDurationMs: StateFlow<Int> = _currentDurationMs.asStateFlow()

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun playTrack(track: TrackCopy) {
        _currentTrack.value = track
        _currentTitle.value = track.title
        _currentArtist.value = track.artist
        playAudioPath(track.filePath)
    }

    fun playPreview(title: String, artist: String, filePathOrUri: String) {
        _currentTrack.value = null
        _currentTitle.value = title
        _currentArtist.value = artist
        playAudioPath(filePathOrUri)
    }

    private fun playAudioPath(path: String) {
        stop()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (path.startsWith("content://") || path.startsWith("file://")) {
                    setDataSource(context, Uri.parse(path))
                } else {
                    val file = File(path)
                    if (file.exists()) {
                        setDataSource(path)
                    } else {
                        // Virtual stream simulation for cache demo
                        return
                    }
                }

                isLooping = _isLooping.value
                prepare()
                start()
            }

            mediaPlayer = player
            _isPlaying.value = true
            _currentDurationMs.value = player.duration.coerceAtLeast(0)

            player.setOnCompletionListener {
                if (!_isLooping.value) {
                    _isPlaying.value = false
                    _currentProgressMs.value = 0
                }
            }

            startProgressTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.start()
            _isPlaying.value = true
            startProgressTracker()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _currentProgressMs.value = positionMs
    }

    fun toggleLoop() {
        val newLoop = !_isLooping.value
        _isLooping.value = newLoop
        mediaPlayer?.isLooping = newLoop
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(speed) ?: return
        } catch (_: Exception) {}
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentProgressMs.value = 0
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            _currentProgressMs.value = player.currentPosition
                        }
                    }
                } catch (_: Exception) {}
                delay(250)
            }
        }
    }
}
