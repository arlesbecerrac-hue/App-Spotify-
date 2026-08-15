package com.example.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MusicGrabberApplication
import com.example.data.ScannedCacheAudio
import com.example.data.TrackCopy
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CacheScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MusicGrabberApplication
    private val scanner = app.cacheScanner
    private val extractor = app.extractorEngine
    private val player = app.playerController

    val isScanning: StateFlow<Boolean> = scanner.isScanning
    val scanProgressText: StateFlow<String> = scanner.scanProgressText
    val scannedCount: StateFlow<Int> = scanner.scannedCount

    private val _selectedFilter = MutableStateFlow("Todos")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isBatchExtracting = MutableStateFlow(false)
    val isBatchExtracting: StateFlow<Boolean> = _isBatchExtracting.asStateFlow()

    val filteredScannedList: StateFlow<List<ScannedCacheAudio>> = combine(
        scanner.scannedResults,
        _selectedFilter,
        _searchQuery
    ) { list, filter, query ->
        list.filter { item ->
            val matchesFilter = when (filter) {
                "Todos" -> true
                "Spotify" -> item.probableSource.contains("Spotify", ignoreCase = true)
                "YouTube Music" -> item.probableSource.contains("YouTube", ignoreCase = true)
                "Descargas" -> item.probableSource.contains("Descargas", ignoreCase = true) || item.probableSource.contains("Download", ignoreCase = true)
                "Caché Sistema" -> item.probableSource.contains("Caché", ignoreCase = true) || item.probableSource.contains("Sistema", ignoreCase = true)
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                    item.probableTitle.contains(query, ignoreCase = true) ||
                    item.probableArtist.contains(query, ignoreCase = true) ||
                    item.fileName.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startScan(customUri: Uri? = null) {
        viewModelScope.launch {
            scanner.performDeepScan(customUri)
        }
    }

    fun previewCacheAudio(item: ScannedCacheAudio) {
        player.playPreview(item.probableTitle, item.probableArtist, item.fileAbsolutePath)
    }

    fun extractSingleItem(
        item: ScannedCacheAudio,
        customTitle: String? = null,
        customArtist: String? = null,
        customAlbum: String? = null,
        onSuccess: (TrackCopy) -> Unit
    ) {
        viewModelScope.launch {
            val result = extractor.extractAndOrganizeCacheItem(item, customTitle, customArtist, customAlbum)
            result.onSuccess { track ->
                Toast.makeText(getApplication(), "¡Pista guardada y organizada como ${track.artist} - ${track.title}!", Toast.LENGTH_SHORT).show()
                onSuccess(track)
            }.onFailure { e ->
                Toast.makeText(getApplication(), "Error al extraer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun extractAllFiltered(onComplete: (Int) -> Unit) {
        val currentList = filteredScannedList.value
        if (currentList.isEmpty()) return
        viewModelScope.launch {
            _isBatchExtracting.value = true
            val count = extractor.extractAllBatch(currentList)
            _isBatchExtracting.value = false
            Toast.makeText(getApplication(), "¡Se extrajeron y organizaron $count pistas con éxito!", Toast.LENGTH_LONG).show()
            onComplete(count)
        }
    }
}
