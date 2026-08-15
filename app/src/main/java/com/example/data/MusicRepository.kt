package com.example.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MusicRepository(
    private val context: Context,
    private val trackCopyDao: TrackCopyDao,
    private val playbackEventDao: PlaybackEventDao
) {
    val allCopies: Flow<List<TrackCopy>> = trackCopyDao.getAllCopies()
    val favoriteCopies: Flow<List<TrackCopy>> = trackCopyDao.getFavoriteCopies()
    val allArtists: Flow<List<String>> = trackCopyDao.getAllArtists()
    val allAlbums: Flow<List<String>> = trackCopyDao.getAllAlbums()
    val totalCopiesCount: Flow<Int> = trackCopyDao.getCopyCount()
    val totalStorageUsed: Flow<Long?> = trackCopyDao.getTotalStorageUsed()
    val recentPlaybackEvents: Flow<List<PlaybackEvent>> = playbackEventDao.getRecentEvents(40)

    fun searchCopies(query: String): Flow<List<TrackCopy>> = trackCopyDao.searchCopies(query)
    fun getCopiesByArtist(artist: String): Flow<List<TrackCopy>> = trackCopyDao.getCopiesByArtist(artist)
    fun getCopiesByAlbum(album: String): Flow<List<TrackCopy>> = trackCopyDao.getCopiesByAlbum(album)

    suspend fun insertTrackCopy(track: TrackCopy): Long = withContext(Dispatchers.IO) {
        val id = trackCopyDao.insertCopy(track)
        notifyMediaScanner(track.filePath)
        id
    }

    suspend fun updateTrackCopy(track: TrackCopy) = withContext(Dispatchers.IO) {
        trackCopyDao.updateCopy(track)
    }

    suspend fun deleteTrackCopy(track: TrackCopy) = withContext(Dispatchers.IO) {
        trackCopyDao.deleteCopy(track)
        try {
            val file = File(track.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    suspend fun deleteTrackById(id: Long) = withContext(Dispatchers.IO) {
        val track = trackCopyDao.getCopyById(id)
        if (track != null) {
            deleteTrackCopy(track)
        }
    }

    suspend fun toggleFavorite(track: TrackCopy) = withContext(Dispatchers.IO) {
        trackCopyDao.updateCopy(track.copy(isFavorite = !track.isFavorite))
    }

    suspend fun logPlaybackEvent(event: PlaybackEvent): Long = withContext(Dispatchers.IO) {
        playbackEventDao.insertEvent(event)
    }

    suspend fun markEventSaved(eventId: Long, savedId: Long) = withContext(Dispatchers.IO) {
        playbackEventDao.markEventAsSaved(eventId, savedId)
    }

    suspend fun clearPlaybackHistory() = withContext(Dispatchers.IO) {
        playbackEventDao.clearAllEvents()
    }

    suspend fun exportTrackToPublicMusicFolder(track: TrackCopy): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(track.filePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("El archivo fuente no existe"))
            }

            val sanitizedArtist = sanitizeFileName(if (track.artist.isNotBlank()) track.artist else "Desconocido")
            val sanitizedAlbum = sanitizeFileName(if (track.album.isNotBlank()) track.album else "Varios")
            val sanitizedTitle = sanitizeFileName(if (track.title.isNotBlank()) track.title else "Pista_${System.currentTimeMillis()}")
            val extension = sourceFile.extension.ifBlank { track.format }

            val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val appMusicDir = File(publicMusicDir, "ARLSIC/$sanitizedArtist/$sanitizedAlbum")
            if (!appMusicDir.exists()) {
                appMusicDir.mkdirs()
            }

            val targetFile = File(appMusicDir, "$sanitizedArtist - $sanitizedTitle.$extension")
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            notifyMediaScanner(targetFile.absolutePath)
            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    private fun notifyMediaScanner(path: String) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(path),
                arrayOf("audio/*", "audio/mp4", "audio/mpeg", "audio/aac", "audio/ogg")
            ) { _, _ -> }
        } catch (_: Exception) {}
    }
}
