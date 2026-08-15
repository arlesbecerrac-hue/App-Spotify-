package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.MusicGrabberApplication
import com.example.data.PlaybackEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MusicNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastLoggedKey: String = ""
    private var lastLoggedTime: Long = 0L

    override fun onListenerConnected() {
        super.onListenerConnected()
        LivePlaybackTracker.setServiceActive(true)
        scanActiveMediaNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        LivePlaybackTracker.setServiceActive(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        processNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        val current = LivePlaybackTracker.currentTrack.value
        if (current != null && current.packageName == sbn.packageName) {
            // Check if there are other media notifications
            scanActiveMediaNotifications()
        }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val packageName = sbn.packageName ?: return

        // Exclude our own app
        if (packageName == applicationContext.packageName) return

        val extras: Bundle = notification.extras ?: return

        val isMedia = notification.category == Notification.CATEGORY_TRANSPORT ||
                extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                isKnownMusicApp(packageName)

        if (!isMedia) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""

        if (title.isBlank() && text.isBlank()) return

        // Discard generic status messages like "Downloading..."
        if (title.contains("downloading", ignoreCase = true) || text.contains("downloading", ignoreCase = true)) return

        val artist = if (text.isNotBlank()) text else subText
        val appName = getFriendlyAppName(packageName)

        var albumArtPath: String? = null
        try {
            val largeIcon = extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)
            if (largeIcon != null) {
                albumArtPath = LivePlaybackTracker.saveBitmapToCache(
                    applicationContext,
                    largeIcon,
                    "art_${System.currentTimeMillis()}"
                )
            }
        } catch (_: Exception) {}

        val eventKey = "$packageName:$title:$artist"
        val now = System.currentTimeMillis()

        val event = PlaybackEvent(
            title = if (title.isNotBlank()) title else "Pista en reproducción",
            artist = if (artist.isNotBlank()) artist else appName,
            album = subText,
            appName = appName,
            packageName = packageName,
            timestamp = now,
            albumArtUri = albumArtPath,
            isCurrentlyPlaying = true
        )

        LivePlaybackTracker.updateCurrentTrack(event)

        // Log to database if new track (debounce duplicate notification ticks)
        if (eventKey != lastLoggedKey || (now - lastLoggedTime > 20000)) {
            lastLoggedKey = eventKey
            lastLoggedTime = now
            serviceScope.launch {
                try {
                    val app = applicationContext as? MusicGrabberApplication
                    app?.repository?.logPlaybackEvent(event)
                } catch (e: Exception) {
                    Log.e("MusicListener", "Error logging playback event", e)
                }
            }
        }
    }

    private fun scanActiveMediaNotifications() {
        try {
            val active = activeNotifications ?: return
            for (sbn in active) {
                if (isKnownMusicApp(sbn.packageName) || sbn.notification.category == Notification.CATEGORY_TRANSPORT) {
                    processNotification(sbn)
                    return
                }
            }
            // If none found
            LivePlaybackTracker.updateCurrentTrack(null)
        } catch (_: Exception) {}
    }

    private fun isKnownMusicApp(pkg: String): Boolean {
        val known = setOf(
            "com.spotify.music",
            "com.spotify.lite",
            "com.google.android.apps.youtube.music",
            "com.google.android.youtube",
            "com.soundcloud.android",
            "com.apple.android.music",
            "deezer.android.app",
            "com.aspiro.tidal",
            "com.amazon.mp3",
            "com.maxmpz.audioplayer",
            "com.musixmatch.android.lyrify",
            "org.videolan.vlc",
            "com.vanced.android.apps.youtube.music",
            "app.revanced.android.apps.youtube.music"
        )
        return known.contains(pkg) || pkg.contains("music", ignoreCase = true) || pkg.contains("audio", ignoreCase = true) || pkg.contains("player", ignoreCase = true)
    }

    private fun getFriendlyAppName(pkg: String): String {
        return when {
            pkg.contains("spotify") -> "Spotify"
            pkg.contains("youtube.music") -> "YouTube Music"
            pkg.contains("youtube") -> "YouTube"
            pkg.contains("soundcloud") -> "SoundCloud"
            pkg.contains("apple") -> "Apple Music"
            pkg.contains("deezer") -> "Deezer"
            pkg.contains("tidal") -> "Tidal"
            pkg.contains("amazon") -> "Amazon Music"
            pkg.contains("vlc") -> "VLC Player"
            else -> {
                try {
                    val pm = packageManager
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    "Reproductor"
                }
            }
        }
    }

    companion object {
        fun isNotificationListenerEnabled(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(packageName)
        }

        fun openNotificationListenerSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
