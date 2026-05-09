package com.idlekingdom.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlekingdom.R
import com.idlekingdom.engine.FlappyEngine
import com.idlekingdom.model.FlappyState
import com.idlekingdom.model.KingdomState
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    engine: FlappyEngine,
    kingdom: KingdomState,
    onExit: (FlappyState) -> Unit
) {

    var state by remember {
        mutableStateOf(
            FlappyState(kingdom = kingdom)
        )
    }

    var frame by remember { mutableStateOf(0) }

    val birdRotation by animateFloatAsState(
        targetValue = state.velocity * 900f,
        label = "birdRotation"
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            state = engine.update(state)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            frame = (frame + 1) % 3
        }
    }

    val birdRes = when(frame) {
        0 -> R.drawable.bird_frame_1
        1 -> R.drawable.bird_frame_2
        else -> R.drawable.bird_frame_3
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    state = engine.flap(state)
                }
            }
    ) {

        // BACKGROUND
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        // PIPES
        state.pipes.forEach { pipe ->

            // TOP PIPE
            Image(
                painter = painterResource(R.drawable.pipe),
                contentDescription = null,
                modifier = Modifier
                    .offset(
                        x = (pipe.x * 1000).dp,
                        y = (-320).dp
                    )
                    .height(350.dp)
                    .width(90.dp)
            )

            // BOTTOM PIPE
            Image(
                painter = painterResource(R.drawable.pipe),
                contentDescription = null,
                modifier = Modifier
                    .offset(
                        x = (pipe.x * 1000).dp,
                        y = ((pipe.gapY * 500) + 420).dp
                    )
                    .height(350.dp)
                    .width(90.dp)
            )
        }

        // BIRD
        Image(
            painter = painterResource(id = birdRes),
            contentDescription = null,
            modifier = Modifier
                .offset(
                    x = 120.dp,
                    y = (state.birdY * 700).dp
                )
                .rotate(birdRotation)
                .size(58.dp)
        )

        // HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Box(
                    modifier = Modifier
                        .background(
                            Color(0xAA111111),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "💰 ${state.kingdom.gold}",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            Color(0xAA111111),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "⭐ ${state.score}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }

        // GAME OVER
        if (state.gameOver) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "💀 GAME OVER",
                    color = Color.Red,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Score: ${state.score}",
                    color = Color.White,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = {
                    onExit(engine.reward(state))
                }) {
                    Text("Return To Kingdom")
                }
            }
        }
    }
}
