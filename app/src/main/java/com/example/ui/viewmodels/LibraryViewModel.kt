package com.example.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MusicGrabberApplication
import com.example.data.TrackCopy
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MusicGrabberApplication
    private val repository = app.repository
    private val player = app.playerController

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentTab = MutableStateFlow(LibraryTab.ALL)
    val currentTab: StateFlow<LibraryTab> = _currentTab.asStateFlow()

    private val _selectedArtist = MutableStateFlow<String?>(null)
    val selectedArtist: StateFlow<String?> = _selectedArtist.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    val allTracks: StateFlow<List<TrackCopy>> = repository.allCopies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<TrackCopy>> = repository.favoriteCopies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<String>> = repository.allArtists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<String>> = repository.allAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCopiesCount: StateFlow<Int> = repository.totalCopiesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStorageUsed: StateFlow<Long?> = repository.totalStorageUsed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val tabFilterFlow = combine(_currentTab, _searchQuery, _selectedArtist, _selectedAlbum) { tab, query, artist, album ->
        FilterParams(tab, query, artist, album)
    }

    val displayedTracks: StateFlow<List<TrackCopy>> = combine(
        allTracks,
        favoriteTracks,
        tabFilterFlow
    ) { all, favs, params ->
        val baseList = when (params.tab) {
            LibraryTab.ALL -> all
            LibraryTab.FAVORITES -> favs
            LibraryTab.ARTISTS -> if (params.artist != null) all.filter { it.artist.equals(params.artist, ignoreCase = true) } else all
            LibraryTab.ALBUMS -> if (params.album != null) all.filter { it.album.equals(params.album, ignoreCase = true) } else all
        }

        if (params.query.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.title.contains(params.query, ignoreCase = true) ||
                it.artist.contains(params.query, ignoreCase = true) ||
                it.album.contains(params.query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: LibraryTab) {
        _currentTab.value = tab
        if (tab != LibraryTab.ARTISTS) _selectedArtist.value = null
        if (tab != LibraryTab.ALBUMS) _selectedAlbum.value = null
    }

    fun selectArtist(artist: String?) {
        _selectedArtist.value = artist
    }

    fun selectAlbum(album: String?) {
        _selectedAlbum.value = album
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playTrack(track: TrackCopy) {
        player.playTrack(track)
    }

    fun toggleFavorite(track: TrackCopy) {
        viewModelScope.launch {
            repository.toggleFavorite(track)
        }
    }

    fun deleteTrack(track: TrackCopy) {
        viewModelScope.launch {
            repository.deleteTrackCopy(track)
            Toast.makeText(getApplication(), "Pista eliminada", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateTrackMetadata(track: TrackCopy, newTitle: String, newArtist: String, newAlbum: String) {
        viewModelScope.launch {
            val updated = track.copy(
                title = newTitle.trim().ifEmpty { track.title },
                artist = newArtist.trim().ifEmpty { track.artist },
                album = newAlbum.trim().ifEmpty { track.album }
            )
            repository.updateTrackCopy(updated)
            Toast.makeText(getApplication(), "Información actualizada", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportTrackToPublicMusic(track: TrackCopy) {
        viewModelScope.launch {
            val result = repository.exportTrackToPublicMusicFolder(track)
            result.onSuccess { path ->
                Toast.makeText(getApplication(), "Exportado a: $path", Toast.LENGTH_LONG).show()
            }.onFailure { e ->
                Toast.makeText(getApplication(), "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareTrack(track: TrackCopy) {
        try {
            val file = File(track.filePath)
            if (!file.exists()) {
                Toast.makeText(getApplication(), "El archivo no existe", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(
                getApplication(),
                "${getApplication<Application>().packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, "${track.artist} - ${track.title}")
                putExtra(Intent.EXTRA_TEXT, "Escucha esta pista: ${track.artist} - ${track.title}")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(Intent.createChooser(shareIntent, "Compartir audio").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Toast.makeText(getApplication(), "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

enum class LibraryTab {
    ALL,
    ARTISTS,
    ALBUMS,
    FAVORITES
}

data class FilterParams(
    val tab: LibraryTab,
    val query: String,
    val artist: String?,
    val album: String?
)
