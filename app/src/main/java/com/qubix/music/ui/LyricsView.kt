package com.qubix.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qubix.music.data.LyricLine
import com.qubix.music.data.LyricsRepository
import com.qubix.music.data.Song
import com.qubix.music.player.PlayerManager
import com.qubix.music.ui.theme.QubixVioletSoft

/**
 * Apple Music-style synced lyrics: current line highlighted, auto-scrolls,
 * tap a line to seek.
 */
@Composable
fun LyricsView(
    player: PlayerManager,
    song: Song,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    var lines by remember(song.videoId) { mutableStateOf<List<LyricLine>?>(null) }

    LaunchedEffect(song.videoId) {
        lines = null
        lines = LyricsRepository.getSyncedLyrics(song.artist, song.title)
    }

    val listState = rememberLazyListState()
    val loaded = lines
    if (loaded == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Memuat lirik…",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
            )
        }
        return
    }
    if (loaded.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Lirik tidak ditemukan",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
            )
        }
        return
    }

    val currentIndex = remember(positionMs, loaded) {
        loaded.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }

    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(
            index = currentIndex.coerceIn(0, loaded.size - 1),
            scrollOffset = -220,
        )
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 120.dp, horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(loaded, key = { _, l -> l.timeMs }) { index, line ->
            val active = index == currentIndex
            Text(
                text = line.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { player.seekTo(line.timeMs) }
                    .padding(vertical = 2.dp),
                textAlign = TextAlign.Start,
                fontSize = if (active) 22.sp else 18.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                lineHeight = if (active) 30.sp else 26.sp,
                color = if (active) Color.White else Color.White.copy(alpha = 0.38f),
            )
        }
    }
}
