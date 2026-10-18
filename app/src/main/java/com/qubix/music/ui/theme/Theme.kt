package com.qubix.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Qubix brand colors — own identity, deep space violet. */
val QubixViolet = Color(0xFF8B7CFF)
val QubixVioletSoft = Color(0xFFB9AEFF)
val QubixBackground = Color(0xFF0B0B14)
val QubixSurface = Color(0xFF14141F)
val QubixOnSurface = Color(0xFFF2F0FF)

private val QubixDarkColors = darkColorScheme(
    primary = QubixViolet,
    onPrimary = Color.White,
    secondary = QubixVioletSoft,
    background = QubixBackground,
    onBackground = QubixOnSurface,
    surface = QubixSurface,
    onSurface = QubixOnSurface,
    surfaceVariant = Color(0xFF1E1E2C),
    onSurfaceVariant = Color(0xFFB9B9D0),
)

@Composable
fun QubixTheme(content: @Composable () -> Unit) {
    // Qubix Music is dark-first, always.
    MaterialTheme(
        colorScheme = QubixDarkColors,
        content = content,
    )
}
