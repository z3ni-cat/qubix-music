package com.qubix.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qubix.music.ui.HomeScreen
import com.qubix.music.ui.MiniPlayer
import com.qubix.music.ui.NowPlayingScreen
import com.qubix.music.ui.theme.QubixTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val player = (application as QubixApp).playerManager
        setContent {
            QubixTheme {
                val hazeState = remember { HazeState() }
                var expanded by remember { mutableStateOf(false) }
                val song by player.currentSong.collectAsStateWithLifecycle()

                Box(Modifier.fillMaxSize()) {
                    // Content behind the capsule feeds the frosted-glass blur.
                    HomeScreen(
                        player = player,
                        modifier = Modifier.haze(state = hazeState),
                    )

                    if (song != null && !expanded) {
                        MiniPlayer(
                            player = player,
                            hazeState = hazeState,
                            onExpand = { expanded = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp),
                        )
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        NowPlayingScreen(
                            player = player,
                            onCollapse = { expanded = false },
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        (application as? QubixApp)?.playerManager?.release()
        super.onDestroy()
    }
}
