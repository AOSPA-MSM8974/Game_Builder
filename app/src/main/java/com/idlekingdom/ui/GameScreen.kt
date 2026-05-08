package com.idlekingdom.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.idlekingdom.engine.FlappyEngine
import com.idlekingdom.model.*
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    engine: FlappyEngine,
    kingdom: KingdomState,
    onExit: (FlappyState) -> Unit
) {

    var state by remember { mutableStateOf(FlappyState(kingdom = kingdom)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            state = engine.update(state)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    state = engine.flap(state)
                }
            }
    ) {

        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        state.pipes.forEach { pipe ->
            Image(
                painter = painterResource(R.drawable.pipe),
                contentDescription = null,
                modifier = Modifier.offset(x = (pipe.x * 1000).dp)
            )
        }

        Image(
            painter = painterResource(R.drawable.bird),
            contentDescription = null,
            modifier = Modifier
                .offset(x = 120.dp, y = (state.birdY * 600).dp)
                .size(48.dp)
        )

        Column(Modifier.padding(16.dp)) {
            Text("💰 ${state.kingdom.gold}", color = Color(0xFFFFD700))
            Text("⭐ ${state.score}", color = Color.White)
        }

        if (state.gameOver) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {

                Text("💀 GAME OVER", color = Color.Red)

                Button(onClick = {
                    onExit(engine.reward(state))
                }) {
                    Text("Return")
                }
            }
        }
    }
}
