package com.idlekingdom.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlekingdom.R
import com.idlekingdom.model.KingdomState

@Composable
fun MenuScreen(
    kingdom: KingdomState,
    onPlay: () -> Unit,
    onUpgrade: () -> Unit
) {

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "🏰 FLAPPY KINGDOM",
                color = Color(0xFFFFD700),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .background(
                        Color(0xAA111111),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(20.dp)
            ) {
                Text(
                    text = "Gold: ${kingdom.gold}",
                    color = Color.White,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(onClick = onPlay) {
                Text("⚔ Enter Battle")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = onUpgrade) {
                Text("⬆ Upgrade Kingdom")
            }
        }
    }
}
