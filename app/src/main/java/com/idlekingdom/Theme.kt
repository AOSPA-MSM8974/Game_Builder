package com.idlekingdom

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8860B),
    secondary = Color(0xFF6A0DAD),
    background = Color(0xFF0B0B10)
)

@Composable
fun IdleKingdomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
