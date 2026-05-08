package com.idlekingdom

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GameScreen(vm: GameViewModel) {

    val state by vm.state.collectAsState()

    val goldAnim by animateFloatAsState(
        targetValue = state.gold.toFloat(),
        label = "gold"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("💰 Gold: ${goldAnim.toInt()}")
        Text("⚡ /sec: ${state.goldPerSecond}")
        Text("🏰 Level: ${state.level}")

        Text("👹 Enemy HP: ${state.enemyHp}")

        Button(onClick = { vm.attackBot() }) {
            Text("⚔️ Raid Bot City")
        }

        Button(onClick = { vm.upgrade() }) {
            Text("⬆ Upgrade (${state.upgradeCost})")
        }
    }
}
