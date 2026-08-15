package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.MusicPlayerController
import com.example.ui.theme.*

@Composable
fun MiniPlayer(
    playerController: MusicPlayerController,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying by playerController.isPlaying.collectAsState()
    val title by playerController.currentTitle.collectAsState()
    val artist by playerController.currentArtist.collectAsState()
    val progressMs by playerController.currentProgressMs.collectAsState()
    val durationMs by playerController.currentDurationMs.collectAsState()

    if (title.isBlank()) return

    val progressFraction = if (durationMs > 0) (progressMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, LightCardBorder, RoundedCornerShape(18.dp))
            .clickable { onExpandClick() }
            .testTag("mini_player_surface"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            // Linear progress indicator
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = SpotifyGreen,
                trackColor = SpotifyGreenSubtle
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpotifyGreenSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_vinyl_art),
                        contentDescription = "Vinyl Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist.ifBlank { "Reproduciendo pista" },
                        style = MaterialTheme.typography.bodySmall,
                        color = SpotifyGreenDark,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledIconButton(
                    onClick = { playerController.togglePlayPause() },
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("mini_player_play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = SpotifyGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { playerController.stop() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("mini_player_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = LightTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayerBottomSheet(
    playerController: MusicPlayerController,
    onDismiss: () -> Unit
) {
    val isPlaying by playerController.isPlaying.collectAsState()
    val title by playerController.currentTitle.collectAsState()
    val artist by playerController.currentArtist.collectAsState()
    val progressMs by playerController.currentProgressMs.collectAsState()
    val durationMs by playerController.currentDurationMs.collectAsState()
    val isLooping by playerController.isLooping.collectAsState()
    val speed by playerController.playbackSpeed.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Smooth spinning vinyl animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album Art Container with Vinyl Disc
            Surface(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .border(2.dp, SpotifyGreenLight, CircleShape),
                shadowElevation = 10.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_vinyl_art),
                    contentDescription = "Vinyl Record",
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(if (isPlaying) rotation else 0f),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = SpotifyGreenDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated waveform
            WaveformVisualizer(
                isAnimating = isPlaying,
                amplitude = if (isPlaying) 85 else 15,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Seek slider
            val sliderValue = if (durationMs > 0) progressMs.toFloat() else 0f
            Slider(
                value = sliderValue,
                onValueChange = { playerController.seekTo(it.toInt()) },
                valueRange = 0f..(durationMs.coerceAtLeast(1).toFloat()),
                colors = SliderDefaults.colors(
                    thumbColor = SpotifyGreen,
                    activeTrackColor = SpotifyGreen,
                    inactiveTrackColor = SpotifyGreenSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_seek_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(progressMs.toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = LightTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDuration(durationMs.toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = LightTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Loop toggle
                IconButton(
                    onClick = { playerController.toggleLoop() },
                    modifier = Modifier.testTag("player_loop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repetir",
                        tint = if (isLooping) SpotifyGreenDark else LightTextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Play / Pause main button with high elevation
                FilledIconButton(
                    onClick = { playerController.togglePlayPause() },
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("player_expanded_play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = SpotifyGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Speed toggle
                TextButton(
                    onClick = {
                        val nextSpeed = when (speed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 0.75f
                            else -> 1.0f
                        }
                        playerController.setSpeed(nextSpeed)
                    },
                    modifier = Modifier.testTag("player_speed_button")
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (speed != 1.0f) SpotifyGreenSubtle else Color.Transparent
                    ) {
                        Text(
                            text = "${speed}x",
                            fontWeight = FontWeight.Black,
                            color = if (speed != 1.0f) SpotifyGreenDark else LightTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

