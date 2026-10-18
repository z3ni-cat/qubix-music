package com.qubix.music.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.qubix.music.data.Song
import com.qubix.music.data.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the MediaController connection and exposes playback state as flows.
 * Queue = last search results; index-based next/previous.
 */
class PlayerManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    var queue: List<Song> = emptyList()
    var queueIndex: Int = -1

    init {
        connect()
        scope.launch {
            while (isActive) {
                val c = controller
                if (c != null) {
                    _isPlaying.value = c.isPlaying
                    if (c.isPlaying || c.playbackState != androidx.media3.common.Player.STATE_IDLE) {
                        _positionMs.value = c.currentPosition.coerceAtLeast(0L)
                        val d = c.duration
                        _durationMs.value = if (d > 0) d else 0L
                    }
                }
                delay(500)
            }
        }
    }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, QubixPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture?.addListener(
            { controller = controllerFuture?.get() },
            MoreExecutors.directExecutor(),
        )
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty() || startIndex !in songs.indices) return
        queue = songs
        queueIndex = startIndex
        playSong(songs[startIndex])
    }

    fun playSong(song: Song) {
        val c = controller ?: return
        _isLoading.value = true
        scope.launch(Dispatchers.IO) {
            try {
                val streamUrl = YouTubeRepository.resolveAudioUrl(song.videoUrl)
                if (streamUrl.isNullOrBlank()) {
                    // Skip to next on failure
                    launch(Dispatchers.Main) {
                        _isLoading.value = false
                        next(auto = true)
                    }
                    return@launch
                }
                val item = androidx.media3.common.MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .build(),
                    )
                    .build()
                launch(Dispatchers.Main) {
                    _currentSong.value = song
                    c.setMediaItem(item)
                    c.prepare()
                    c.play()
                    _isLoading.value = false
                }
            } catch (_: Exception) {
                launch(Dispatchers.Main) {
                    _isLoading.value = false
                    next(auto = true)
                }
            }
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next(auto: Boolean = false) {
        if (queue.isEmpty()) return
        val nextIndex = queueIndex + 1
        if (nextIndex in queue.indices) {
            queueIndex = nextIndex
            playSong(queue[nextIndex])
        } else if (!auto) {
            // wrap to start on manual next at end
            queueIndex = 0
            playSong(queue[0])
        }
    }

    fun previous() {
        val c = controller
        // Restart song if played more than 3 seconds, else previous track
        if ((c?.currentPosition ?: 0L) > 3000L) {
            c?.seekTo(0L)
            return
        }
        if (queue.isEmpty()) return
        val prevIndex = queueIndex - 1
        if (prevIndex in queue.indices) {
            queueIndex = prevIndex
            playSong(queue[prevIndex])
        } else {
            c?.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun release() {
        controllerFuture?.let { androidx.media3.session.MediaController.releaseFuture(it) }
    }
}
