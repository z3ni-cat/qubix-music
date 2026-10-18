package com.qubix.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class LrcLibResponse(
    val syncedLyrics: String?,
    val plainLyrics: String?,
)

data class LyricLine(val timeMs: Long, val text: String)

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("artist_name") artist: String,
        @Query("track_name") track: String,
    ): LrcLibResponse
}

/** Synced lyrics from LRCLIB (free, no auth). */
object LyricsRepository {

    private val api: LrcLibApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrcLibApi::class.java)
    }

    suspend fun getSyncedLyrics(artist: String, track: String): List<LyricLine> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.getLyrics(artist, track)
                val synced = res.syncedLyrics
                if (!synced.isNullOrBlank()) {
                    parseLrc(synced).takeIf { it.isNotEmpty() } ?: emptyList()
                } else {
                    // Fallback: plain lyrics as untimed lines
                    res.plainLyrics
                        ?.lines()
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.mapIndexed { i, t -> LyricLine(i * 4000L, t) }
                        ?: emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    private val LRC_LINE = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\](.*)""")

    fun parseLrc(lrc: String): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        for (raw in lrc.lines()) {
            val m = LRC_LINE.find(raw.trim()) ?: continue
            val min = m.groupValues[1].toLongOrNull() ?: continue
            val sec = m.groupValues[2].toLongOrNull() ?: continue
            val fracRaw = m.groupValues[3]
            val fracMs = when (fracRaw.length) {
                0 -> 0L
                1 -> fracRaw.toLong() * 100L
                2 -> fracRaw.toLong() * 10L
                else -> fracRaw.take(3).toLong()
            }
            val text = m.groupValues[4].trim()
            if (text.isEmpty()) continue
            out += LyricLine(min * 60_000 + sec * 1_000 + fracMs, text)
        }
        return out.sortedBy { it.timeMs }
    }
}
