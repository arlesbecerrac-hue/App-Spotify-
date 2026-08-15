package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_copies")
data class TrackCopy(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "Desconocido",
    val sourceApp: String, // e.g. "Spotify", "YouTube Music", "Caché Scanner", "Grabación Directa"
    val sourcePackage: String = "",
    val filePath: String,
    val fileUri: String = "",
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val format: String = "m4a", // "mp3", "m4a", "aac", "ogg", "wav"
    val bitrateKbps: Int = 192,
    val savedTimestamp: Long = System.currentTimeMillis(),
    val albumArtUri: String? = null,
    val category: String = "Música",
    val isFavorite: Boolean = false
)

@Entity(tableName = "playback_events")
data class PlaybackEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "",
    val appName: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val albumArtUri: String? = null,
    val isCurrentlyPlaying: Boolean = false,
    val hasSavedCopy: Boolean = false,
    val savedCopyId: Long? = null
)

data class ScannedCacheAudio(
    val id: String,
    val fileAbsolutePath: String,
    val fileName: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val detectedFormat: String,
    val estimatedDurationMs: Long = 0L,
    val probableTitle: String,
    val probableArtist: String,
    val probableSource: String,
    val isRecognizedAudioHeader: Boolean = true
)
