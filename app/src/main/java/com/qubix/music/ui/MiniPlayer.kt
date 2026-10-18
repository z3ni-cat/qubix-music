package com.qubix.music.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.qubix.music.data.Song
import com.qubix.music.player.PlayerManager
import com.qubix.music.ui.theme.QubixViolet
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlin.math.abs

/**
 * Floating capsule mini player — frosted glass pill with artwork, title,
 * transport controls, swipe gestures (up = expand, left/right = next/prev).
 */
@Composable
fun MiniPlayer(
    player: PlayerManager,
    hazeState: HazeState,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song by player.currentSong.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val isLoading by player.isLoading.collectAsStateWithLifecycle()
    val positionMs by player.positionMs.collectAsStateWithLifecycle()
    val durationMs by player.durationMs.collectAsStateWithLifecycle()
    val current = song ?: return

    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "capsuleProgress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .hazeChild(
                state = hazeState,
                shape = RoundedCornerShape(32.dp),
            )
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onExpand)
            .capsuleGestures(
                onExpand = onExpand,
                onNext = { player.next() },
                onPrevious = { player.previous() },
            ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = current.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White,
                    )
                    Text(
                        text = current.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 3.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(12.dp))
                } else {
                    IconButton(onClick = { player.togglePlayPause() }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                IconButton(onClick = { player.next() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Berikutnya",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            // Slim progress hairline
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 28.dp),
                color = QubixViolet,
                trackColor = Color.White.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

/**
 * One gesture detector for the capsule: swipe up expands, horizontal swipes
 * skip tracks. A tap is left to [clickable].
 */
private fun Modifier.capsuleGestures(
    onExpand: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
): Modifier = pointerInput(onExpand, onNext, onPrevious) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var expandUp = false
        val slopChange = awaitTouchSlopOrCancellation(down.id) { change, _ ->
            val d = change.position - down.position
            expandUp = d.y < 0f && abs(d.y) > abs(d.x) * 1.2f
        } ?: return@awaitEachGesture // lifted before slop: it's a tap
        if (expandUp) {
            drag(slopChange.id) { change ->
                if (change.positionChange() != Offset.Zero) change.consume()
            }
            onExpand()
        } else {
            var totalX = slopChange.position.x - down.position.x
            val finished = drag(slopChange.id) { change ->
                totalX += change.positionChange().x
                change.consume()
            }
            if (finished) {
                val thresholdPx = with(this@pointerInput) { 72.dp.toPx() }
                when {
                    totalX <= -thresholdPx -> onNext()
                    totalX >= thresholdPx -> onPrevious()
                }
            }
        }
    }
}
