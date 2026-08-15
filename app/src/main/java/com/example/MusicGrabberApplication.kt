package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.MusicRepository
import com.example.engine.AudioCaptureEngine
import com.example.engine.MusicExtractorEngine
import com.example.engine.MusicPlayerController
import com.example.engine.StorageCacheScanner

class MusicGrabberApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { MusicRepository(this, database.trackCopyDao(), database.playbackEventDao()) }
    val cacheScanner by lazy { StorageCacheScanner(this) }
    val extractorEngine by lazy { MusicExtractorEngine(this, repository) }
    val audioCaptureEngine by lazy { AudioCaptureEngine(this, repository) }
    val playerController by lazy { MusicPlayerController(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
