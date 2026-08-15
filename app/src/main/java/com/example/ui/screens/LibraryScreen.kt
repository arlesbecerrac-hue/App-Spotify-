package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.TrackCopy
import com.example.ui.components.EditMetadataDialog
import com.example.ui.components.formatDuration
import com.example.ui.theme.*
import com.example.ui.viewmodels.LibraryTab
import com.example.ui.viewmodels.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val displayedTracks by viewModel.displayedTracks.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val totalCount by viewModel.totalCopiesCount.collectAsState()
    val totalStorageUsed by viewModel.totalStorageUsed.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var trackToEdit by remember { mutableStateOf<TrackCopy?>(null) }
    var trackForOptions by remember { mutableStateOf<TrackCopy?>(null) }

    if (trackToEdit != null) {
        EditMetadataDialog(
            track = trackToEdit!!,
            onDismiss = { trackToEdit = null },
            onConfirm = { title, artist, album ->
                viewModel.updateTrackMetadata(trackToEdit!!, title, artist, album)
                trackToEdit = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Library Stats Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = totalCount.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = SpotifyGreenDark
                        )
                        Text(
                            text = "Canciones",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LightTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(LightCardBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatFileSize(totalStorageUsed ?: 0L),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Espacio HQ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LightTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(LightCardBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = artists.size.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = SpotifyGreenDark
                        )
                        Text(
                            text = "Artistas",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LightTextSecondary
                        )
                    }
                }
            }
        }

        // Tab Selector Row
        item {
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SpotifyGreenDark,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("library_tab_row")
            ) {
                Tab(
                    selected = currentTab == LibraryTab.ALL,
                    onClick = { viewModel.setTab(LibraryTab.ALL) },
                    text = { Text("Canciones", fontSize = 12.sp, fontWeight = if (currentTab == LibraryTab.ALL) FontWeight.Black else FontWeight.Normal) }
                )
                Tab(
                    selected = currentTab == LibraryTab.ARTISTS,
                    onClick = { viewModel.setTab(LibraryTab.ARTISTS) },
                    text = { Text("Artistas", fontSize = 12.sp, fontWeight = if (currentTab == LibraryTab.ARTISTS) FontWeight.Black else FontWeight.Normal) }
                )
                Tab(
                    selected = currentTab == LibraryTab.ALBUMS,
                    onClick = { viewModel.setTab(LibraryTab.ALBUMS) },
                    text = { Text("Álbumes", fontSize = 12.sp, fontWeight = if (currentTab == LibraryTab.ALBUMS) FontWeight.Black else FontWeight.Normal) }
                )
                Tab(
                    selected = currentTab == LibraryTab.FAVORITES,
                    onClick = { viewModel.setTab(LibraryTab.FAVORITES) },
                    text = { Text("Favoritos", fontSize = 12.sp, fontWeight = if (currentTab == LibraryTab.FAVORITES) FontWeight.Black else FontWeight.Normal) }
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar por título, artista o álbum...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SpotifyGreenDark) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = LightCardBorder,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth().testTag("library_search_input")
            )
        }

        // Subcategory Breadcrumb when filtering artist/album
        if (currentTab == LibraryTab.ARTISTS && selectedArtist != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.selectArtist(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = SpotifyGreenDark)
                    }
                    Text(
                        text = "Artista: $selectedArtist",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyGreenDark
                    )
                }
            }
        } else if (currentTab == LibraryTab.ALBUMS && selectedAlbum != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.selectAlbum(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = SpotifyGreenDark)
                    }
                    Text(
                        text = "Álbum: $selectedAlbum",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyGreenDark
                    )
                }
            }
        }

        // Render content based on active tab
        if (currentTab == LibraryTab.ARTISTS && selectedArtist == null) {
            if (artists.isEmpty()) {
                item { EmptyLibraryState() }
            } else {
                items(artists) { artist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectArtist(artist) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SpotifyGreenSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SpotifyGreenDark)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else if (currentTab == LibraryTab.ALBUMS && selectedAlbum == null) {
            if (albums.isEmpty()) {
                item { EmptyLibraryState() }
            } else {
                items(albums) { album ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectAlbum(album) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SpotifyGreenSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Album, contentDescription = null, tint = SpotifyGreenDark)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = album,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            if (displayedTracks.isEmpty()) {
                item { EmptyLibraryState() }
            } else {
                items(displayedTracks, key = { it.id }) { track ->
                    TrackCopyCard(
                        track = track,
                        onPlay = { viewModel.playTrack(track) },
                        onToggleFavorite = { viewModel.toggleFavorite(track) },
                        onShare = { viewModel.shareTrack(track) },
                        onExport = { viewModel.exportTrackToPublicMusic(track) },
                        onEdit = { trackToEdit = track },
                        onDelete = { viewModel.deleteTrack(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackCopyCard(
    track: TrackCopy,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPlay() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SpotifyGreenSubtle),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_vinyl_art),
                    contentDescription = "Vinyl Record",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyGreenDark,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = SpotifyGreenSubtle,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${track.format.uppercase()} ${track.bitrateKbps}k",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SpotifyGreenDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = formatFileSize(track.fileSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "• ${formatDuration(track.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("track_fav_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorito",
                    tint = if (track.isFavorite) AccentAmber else LightTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("track_menu_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = LightTextSecondary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Reproducir", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SpotifyGreenDark) },
                        onClick = {
                            showMenu = false
                            onPlay()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Compartir Audio") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = SpotifyGreenDark) },
                        onClick = {
                            showMenu = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Exportar a Music/ pública") },
                        leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null, tint = SpotifyGreenDark) },
                        onClick = {
                            showMenu = false
                            onExport()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Editar Metadatos") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = SpotifyGreenDark) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    HorizontalDivider(color = LightCardBorder)
                    DropdownMenuItem(
                        text = { Text("Eliminar Pista", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = SpotifyGreenDark,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Tu biblioteca está vacía",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Usa el Extractor de Caché o el Detector en Vivo para guardar y organizar tus primeras pistas en alta calidad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

