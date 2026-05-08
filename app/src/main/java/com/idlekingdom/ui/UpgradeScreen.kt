package com.idlekingdom.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.idlekingdom.model.KingdomState

@Composable
fun UpgradeScreen(
    kingdom: KingdomState,
    onUpdate: (KingdomState) -> Unit,
    onBack: () -> Unit
) {

    Column(Modifier.padding(16.dp)) {

        Text("🏰 Upgrades")

        Spacer(Modifier.height(10.dp))

        Button(onClick = {
            if (kingdom.gold >= 50) {
                onUpdate(
                    kingdom.copy(
                        gold = kingdom.gold - 50,
                        gravity = kingdom.gravity * 0.9f
                    )
                )
            }
        }) {
            Text("⬇ Lower Gravity (50💰)")
        }

        Button(onClick = {
            if (kingdom.gold >= 75) {
                onUpdate(
                    kingdom.copy(
                        gold = kingdom.gold - 75,
                        goldMultiplier = kingdom.goldMultiplier + 0.2f
                    )
                )
            }
        }) {
            Text("💰 Increase Gold Gain (75💰)")
        }

        Spacer(Modifier.height(20.dp))

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
