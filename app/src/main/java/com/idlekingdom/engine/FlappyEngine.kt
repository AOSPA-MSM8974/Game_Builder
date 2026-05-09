package com.idlekingdom.engine

import com.idlekingdom.model.FlappyState
import com.idlekingdom.model.Pipe
import kotlin.math.abs
import kotlin.random.Random

class FlappyEngine {

    private val gravity = 0.0028f
    private val flapForce = -0.035f
    private val maxFallSpeed = 0.028f

    fun update(state: FlappyState): FlappyState {

        if (state.gameOver) return state

        var velocity = state.velocity + gravity

        if (velocity > maxFallSpeed) {
            velocity = maxFallSpeed
        }

        val birdY = state.birdY + velocity

        val movedPipes = state.pipes
            .map {
                it.copy(x = it.x - 0.006f)
            }
            .filter {
                it.x > -0.25f
            }

        val pipes = if (Random.nextFloat() < 0.018f) {
            movedPipes + Pipe(
                x = 1.2f,
                gapY = Random.nextFloat() * 0.45f + 0.2f
            )
        } else {
            movedPipes
        }

        var dead = false

        if (birdY < 0f || birdY > 1f) {
            dead = true
        }

        // PIPE COLLISION
        pipes.forEach { pipe ->

            val pipeX = pipe.x
            val birdX = 0.12f

            val touchingX = abs(pipeX - birdX) < 0.08f

            val gapTop = pipe.gapY
            val gapBottom = pipe.gapY + 0.25f

            val insideGap = birdY in gapTop..gapBottom

            if (touchingX && !insideGap) {
                dead = true
            }
        }

        return state.copy(
            birdY = birdY,
            velocity = velocity,
            pipes = pipes,
            score = state.score + 1,
            gameOver = dead
        )
    }

    fun flap(state: FlappyState): FlappyState {
        if (state.gameOver) return state

        return state.copy(
            velocity = flapForce
        )
    }

    fun reward(state: FlappyState): FlappyState {

        val goldEarned = (state.score * state.kingdom.goldMultiplier).toLong()

        return state.copy(
            kingdom = state.kingdom.copy(
                gold = state.kingdom.gold + goldEarned
            )
        )
    }
}
