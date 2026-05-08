package com.idlekingdom

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8860B),
    secondary = Color(0xFF6A0DAD),
    background = Color(0xFF0B0B10),
    surface = Color(0xFF141420),
    onBackground = Color(0xFFE6E6E6)
)

@Composable
fun IdleKingdomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
