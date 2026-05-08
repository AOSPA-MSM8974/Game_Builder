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

    val animatedGold by animateFloatAsState(
        targetValue = state.gold.toFloat(),
        label = "gold"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "💰 Gold: ${animatedGold.toInt()}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text("⚡ Gold/sec: ${state.goldPerSecond}")
        Text("👹 Enemy HP: ${state.enemyHp}")

        Box(
            modifier = Modifier
                .size(160.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text("👹")
        }

        Button(onClick = { vm.attackEnemy() }) {
            Text("⚔️ Attack")
        }

        Button(onClick = { vm.buyUpgrade() }) {
            Text("⬆ Upgrade (${state.upgradeCost})")
        }
    }
}
