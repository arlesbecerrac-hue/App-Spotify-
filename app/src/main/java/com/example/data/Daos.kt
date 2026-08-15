package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackCopyDao {
    @Query("SELECT * FROM track_copies ORDER BY savedTimestamp DESC")
    fun getAllCopies(): Flow<List<TrackCopy>>

    @Query("SELECT * FROM track_copies WHERE isFavorite = 1 ORDER BY savedTimestamp DESC")
    fun getFavoriteCopies(): Flow<List<TrackCopy>>

    @Query("SELECT * FROM track_copies WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY savedTimestamp DESC")
    fun searchCopies(query: String): Flow<List<TrackCopy>>

    @Query("SELECT * FROM track_copies WHERE id = :id LIMIT 1")
    suspend fun getCopyById(id: Long): TrackCopy?

    @Query("SELECT DISTINCT artist FROM track_copies WHERE artist != '' ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT DISTINCT album FROM track_copies WHERE album != '' ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<String>>

    @Query("SELECT * FROM track_copies WHERE artist = :artist ORDER BY title ASC")
    fun getCopiesByArtist(artist: String): Flow<List<TrackCopy>>

    @Query("SELECT * FROM track_copies WHERE album = :album ORDER BY title ASC")
    fun getCopiesByAlbum(album: String): Flow<List<TrackCopy>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCopy(track: TrackCopy): Long

    @Update
    suspend fun updateCopy(track: TrackCopy)

    @Delete
    suspend fun deleteCopy(track: TrackCopy)

    @Query("DELETE FROM track_copies WHERE id = :id")
    suspend fun deleteCopyById(id: Long)

    @Query("SELECT COUNT(*) FROM track_copies")
    fun getCopyCount(): Flow<Int>

    @Query("SELECT SUM(fileSizeBytes) FROM track_copies")
    fun getTotalStorageUsed(): Flow<Long?>
}

@Dao
interface PlaybackEventDao {
    @Query("SELECT * FROM playback_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): Flow<List<PlaybackEvent>>

    @Query("SELECT * FROM playback_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<PlaybackEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PlaybackEvent): Long

    @Query("UPDATE playback_events SET hasSavedCopy = 1, savedCopyId = :savedCopyId WHERE id = :eventId")
    suspend fun markEventAsSaved(eventId: Long, savedCopyId: Long)

    @Query("DELETE FROM playback_events")
    suspend fun clearAllEvents()

    @Query("DELETE FROM playback_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)
}
