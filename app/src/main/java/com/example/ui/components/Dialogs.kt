package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ScannedCacheAudio
import com.example.data.TrackCopy

@Composable
fun EditMetadataDialog(
    track: TrackCopy,
    onDismiss: () -> Unit,
    onConfirm: (title: String, artist: String, album: String) -> Unit
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Información de la Pista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la Canción") },
                    leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_title_input")
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista / Intérprete") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_artist_input")
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Álbum / Colección") },
                    leadingIcon = { Icon(Icons.Default.Album, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_album_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, artist, album) },
                modifier = Modifier.testTag("edit_save_button")
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("edit_cancel_button")
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ExtractOptionsDialog(
    scannedItem: ScannedCacheAudio,
    onDismiss: () -> Unit,
    onExtract: (title: String, artist: String, album: String) -> Unit
) {
    var title by remember { mutableStateOf(scannedItem.probableTitle) }
    var artist by remember { mutableStateOf(scannedItem.probableArtist) }
    var album by remember { mutableStateOf(scannedItem.probableSource) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extraer y Organizar Caché") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "El archivo se copiará y organizará automáticamente con etiquetas ID3 y estructura limpia en tu biblioteca.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre de la Canción") },
                    leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("extract_title_input")
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("extract_artist_input")
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Álbum o Carpeta") },
                    leadingIcon = { Icon(Icons.Default.Album, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("extract_album_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onExtract(title, artist, album) },
                modifier = Modifier.testTag("extract_confirm_button")
            ) {
                Text("Extraer Copia")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("extract_cancel_button")
            ) {
                Text("Cancelar")
            }
        }
    )
}
