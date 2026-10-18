package com.qubix.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.qubix.music.player.PlayerManager
import com.qubix.music.ui.theme.QubixViolet
import kotlin.math.abs

/** Full-screen now playing: artwork, seekbar, controls, synced lyrics. */
@Composable
fun NowPlayingScreen(
    player: PlayerManager,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song by player.currentSong.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val isLoading by player.isLoading.collectAsStateWithLifecycle()
    val positionMs by player.positionMs.collectAsStateWithLifecycle()
    val durationMs by player.durationMs.collectAsStateWithLifecycle()
    val current = song ?: return

    var showLyrics by remember(current.videoId) { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    BackHandler(onBack = onCollapse)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1A2E), Color(0xFF0B0B14)),
                ),
            )
            .dismissOnSwipeDown(onCollapse),
    ) {
        // Blurred artwork backdrop (with dark scrim so it degrades gracefully)
        AsyncImage(
            model = current.artworkUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(90.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.35f,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Tutup",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "SEDANG DIPUTAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.55f),
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            if (showLyrics) {
                LyricsView(
                    player = player,
                    song = current,
                    positionMs = positionMs,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.height(24.dp))
                if (current.artworkUrl != null) {
                    AsyncImage(
                        model = current.artworkUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(28.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
            }

            Text(
                text = current.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = current.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
            Slider(
                value = if (scrubbing) scrubValue else progress,
                onValueChange = {
                    scrubbing = true
                    scrubValue = it
                },
                onValueChangeFinished = {
                    player.seekTo((scrubValue * durationMs).toLong())
                    scrubbing = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = QubixViolet,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    formatMs(if (scrubbing) (scrubValue * durationMs).toLong() else positionMs),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f),
                )
                Text(
                    formatMs(durationMs),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { player.previous() },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Sebelumnya",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.width(20.dp))
                IconButton(
                    onClick = { player.togglePlayPause() },
                    modifier = Modifier.size(76.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = Color.White,
                            strokeWidth = 4.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp),
                        )
                    }
                }
                Spacer(Modifier.width(20.dp))
                IconButton(
                    onClick = { player.next() },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Berikutnya",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            IconButton(onClick = { showLyrics = !showLyrics }) {
                Icon(
                    Icons.Rounded.Lyrics,
                    contentDescription = "Lirik",
                    tint = if (showLyrics) QubixViolet else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Swipe down past the threshold collapses the player. */
private fun Modifier.dismissOnSwipeDown(onCollapse: () -> Unit): Modifier =
    pointerInput(onCollapse) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val slop = awaitTouchSlopOrCancellation(down.id) { _, _ -> } ?: return@awaitEachGesture
            val start = slop.position - down.position
            if (!(start.y > 0f && abs(start.y) > abs(start.x))) return@awaitEachGesture
            var totalY = start.y
            drag(slop.id) { change ->
                totalY += change.positionChange().y
                change.consume()
            }
            val thresholdPx = with(this@dismissOnSwipeDown) { 110.dp.toPx() }
            if (totalY > thresholdPx) onCollapse()
        }
    }
