package com.qubix.music

import android.app.Application
import com.qubix.music.data.YouTubeRepository
import com.qubix.music.player.PlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QubixApp : Application() {

    lateinit var playerManager: PlayerManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Warm up NewPipeExtractor off the main thread
        appScope.launch(Dispatchers.IO) { YouTubeRepository.init() }
        playerManager = PlayerManager(this)
    }
}
