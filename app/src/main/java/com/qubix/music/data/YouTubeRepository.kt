package com.qubix.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoType
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * YouTube Music search + audio stream resolution via NewPipeExtractor.
 * No API key required.
 */
object YouTubeRepository {

    private val initialized = AtomicBoolean(false)

    fun init() {
        if (initialized.compareAndSet(false, true)) {
            NewPipe.init(SimpleDownloader())
        }
    }

    suspend fun search(query: String, maxResults: Int = 25): List<Song> =
        withContext(Dispatchers.IO) {
            init()
            val extractor = ServiceList.YouTube.getSearchExtractor(query)
            extractor.fetchPage()
            extractor.initialPage.items
                .filter { it.infoType == InfoType.STREAM && it is StreamInfoItem }
                .map { it as StreamInfoItem }
                .take(maxResults)
                .mapNotNull { item ->
                    try {
                        val thumb = item.thumbnails.maxByOrNull { t -> t.height * t.width }
                        Song(
                            videoId = extractVideoId(item.url),
                            title = item.name,
                            artist = item.uploaderName?.takeIf { it.isNotBlank() } ?: "Unknown artist",
                            artworkUrl = thumb?.url,
                            videoUrl = item.url,
                            durationSec = item.duration,
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
        }

    /** Resolve the best direct audio stream URL for a video. */
    suspend fun resolveAudioUrl(videoUrl: String): String? =
        withContext(Dispatchers.IO) {
            init()
            val extractor = ServiceList.YouTube.getStreamExtractor(videoUrl)
            extractor.fetchPage()
            val candidates: List<AudioStream> = extractor.audioStreams
                .filter { !it.content.isNullOrBlank() }
            // Prefer highest bitrate; prefer non-opus/webm when tied is unnecessary —
            // ExoPlayer handles all, so just take max bitrate.
            candidates.maxByOrNull { it.averageBitrate }
                ?.content
        }

    private fun extractVideoId(url: String): String {
        // https://www.youtube.com/watch?v=XXXX
        val v = Regex("[?&]v=([^&]+)").find(url)?.groupValues?.get(1)
        if (v != null) return v
        // https://youtu.be/XXXX
        val short = Regex("youtu\\.be/([^?&]+)").find(url)?.groupValues?.get(1)
        return short ?: url.hashCode().toString()
    }

    private class SimpleDownloader : Downloader() {
        override fun execute(request: Request): Response {
            val conn = (URL(request.url()).openConnection() as HttpURLConnection).apply {
                requestMethod = request.httpMethod()
                connectTimeout = 20_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                request.headers().forEach { (name, values) ->
                    values.forEach { addRequestProperty(name, it) }
                }
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36",
                )
                request.dataToSend()?.let { data ->
                    doOutput = true
                    outputStream.use { it.write(data) }
                }
            }
            val code = conn.responseCode
            val body = try {
                conn.inputStream.bufferedReader().readText()
            } catch (_: IOException) {
                try {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (_: IOException) {
                    ""
                }
            }
            val headers = conn.headerFields
                .filterKeys { it != null }
                .mapValues { it.value ?: emptyList() }
            return Response(code, conn.responseMessage ?: "", headers, body, conn.url.toString())
        }
    }
}
