package com.idlekingdom.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.idlekingdom.model.KingdomState

@Composable
fun MenuScreen(
    kingdom: KingdomState,
    onPlay: () -> Unit,
    onUpgrade: () -> Unit
) {

    Column(Modifier.padding(16.dp)) {

        Text("🏰 Flappy Kingdom")

        Spacer(Modifier.height(20.dp))

        Text("💰 Gold: ${kingdom.gold}")

        Button(onClick = onPlay) {
            Text("▶ Play")
        }

        Button(onClick = onUpgrade) {
            Text("⬆ Upgrade")
        }
    }
}
