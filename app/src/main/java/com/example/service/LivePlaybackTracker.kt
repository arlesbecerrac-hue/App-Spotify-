package com.example.service

import android.content.Context
import android.graphics.Bitmap
import com.example.data.PlaybackEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

object LivePlaybackTracker {
    private val _currentTrack = MutableStateFlow<PlaybackEvent?>(null)
    val currentTrack: StateFlow<PlaybackEvent?> = _currentTrack.asStateFlow()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _lastDetectedSource = MutableStateFlow("Esperando reproductor...")
    val lastDetectedSource: StateFlow<String> = _lastDetectedSource.asStateFlow()

    fun updateCurrentTrack(track: PlaybackEvent?) {
        _currentTrack.value = track
        if (track != null) {
            _lastDetectedSource.value = "${track.appName}: ${track.title}"
        }
    }

    fun setServiceActive(active: Boolean) {
        _isServiceActive.value = active
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val artDir = File(context.cacheDir, "album_art")
            if (!artDir.exists()) artDir.mkdirs()
            val file = File(artDir, "$fileName.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
