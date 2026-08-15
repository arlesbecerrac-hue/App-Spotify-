package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ExpandedPlayerBottomSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.CacheScannerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LiveScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodels.CacheScannerViewModel
import com.example.ui.viewmodels.LibraryViewModel
import com.example.ui.viewmodels.LiveGrabberViewModel

class MainActivity : ComponentActivity() {

    private val liveViewModel: LiveGrabberViewModel by viewModels()
    private val scannerViewModel: CacheScannerViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MusicGrabberApplication
        val playerController = app.playerController

        setContent {
            MyApplicationTheme(darkTheme = false) {
                var selectedNavIndex by remember { mutableIntStateOf(0) }
                var showExpandedPlayer by remember { mutableStateOf(false) }

                val isServiceActive by liveViewModel.isServiceActive.collectAsState()
                val currentTrack by liveViewModel.currentPlayingTrack.collectAsState()
                val isNotificationGranted = liveViewModel.isNotificationAccessGranted()

                val snackbarHostState = remember { SnackbarHostState() }
                val statusMessage by liveViewModel.statusMessage.collectAsState()

                LaunchedEffect(statusMessage) {
                    statusMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        liveViewModel.clearStatusMessage()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(SpotifyGreen, SpotifyGreenLight)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "ARLSIC",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SpotifyGreenSubtle
                                            ) {
                                                Text(
                                                    text = "PRO",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = SpotifyGreenDark,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Music Extractor & Studio",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LightTextSecondary
                                        )
                                    }
                                }

                                // Status Indicator
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isNotificationGranted) SpotifyGreenSubtle else Color(0xFFFEF3C7),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.clickable {
                                        if (!isNotificationGranted) {
                                            liveViewModel.openNotificationSettings()
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isNotificationGranted) SpotifyGreen else AccentAmber)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isNotificationGranted) "Live Activo" else "Permiso",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNotificationGranted) SpotifyGreenDark else AccentAmber
                                        )
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        ) {
                            // Persistent Mini Player
                            MiniPlayer(
                                playerController = playerController,
                                onExpandClick = { showExpandedPlayer = true }
                            )

                            // Bottom Navigation Bar
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = selectedNavIndex == 0,
                                    onClick = { selectedNavIndex = 0 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Radio,
                                            contentDescription = "En Vivo"
                                        )
                                    },
                                    label = { Text("En Vivo", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SpotifyGreenDark,
                                        selectedTextColor = SpotifyGreenDark,
                                        indicatorColor = SpotifyGreenSubtle,
                                        unselectedIconColor = LightTextSecondary,
                                        unselectedTextColor = LightTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_live")
                                )

                                NavigationBarItem(
                                    selected = selectedNavIndex == 1,
                                    onClick = { selectedNavIndex = 1 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.ManageSearch,
                                            contentDescription = "Extractor"
                                        )
                                    },
                                    label = { Text("Extractor", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SpotifyGreenDark,
                                        selectedTextColor = SpotifyGreenDark,
                                        indicatorColor = SpotifyGreenSubtle,
                                        unselectedIconColor = LightTextSecondary,
                                        unselectedTextColor = LightTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_scanner")
                                )

                                NavigationBarItem(
                                    selected = selectedNavIndex == 2,
                                    onClick = { selectedNavIndex = 2 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.LibraryMusic,
                                            contentDescription = "Biblioteca"
                                        )
                                    },
                                    label = { Text("Biblioteca", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SpotifyGreenDark,
                                        selectedTextColor = SpotifyGreenDark,
                                        indicatorColor = SpotifyGreenSubtle,
                                        unselectedIconColor = LightTextSecondary,
                                        unselectedTextColor = LightTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_library")
                                )

                                NavigationBarItem(
                                    selected = selectedNavIndex == 3,
                                    onClick = { selectedNavIndex = 3 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Ajustes"
                                        )
                                    },
                                    label = { Text("Ajustes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SpotifyGreenDark,
                                        selectedTextColor = SpotifyGreenDark,
                                        indicatorColor = SpotifyGreenSubtle,
                                        unselectedIconColor = LightTextSecondary,
                                        unselectedTextColor = LightTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_settings")
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedNavIndex) {
                            0 -> LiveScreen(viewModel = liveViewModel)
                            1 -> CacheScannerScreen(viewModel = scannerViewModel)
                            2 -> LibraryScreen(viewModel = libraryViewModel)
                            3 -> SettingsScreen(onClearHistory = { liveViewModel.clearHistory() })
                        }
                    }
                }

                if (showExpandedPlayer) {
                    ExpandedPlayerBottomSheet(
                        playerController = playerController,
                        onDismiss = { showExpandedPlayer = false }
                    )
                }
            }
        }
    }
}
