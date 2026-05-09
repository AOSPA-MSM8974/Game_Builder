package com.idlekingdom.engine

import com.idlekingdom.model.FlappyState
import com.idlekingdom.model.Pipe
import kotlin.math.abs
import kotlin.random.Random

class FlappyEngine {

    // smoother physics
    private val gravity = 0.0018f
    private val flapForce = -0.026f
    private val maxFallSpeed = 0.020f
    private val liftDamping = 0.92f

    fun update(state: FlappyState): FlappyState {

        if (state.gameOver) return state

        // smooth velocity
        var velocity = (state.velocity * liftDamping) + gravity

        // cap fall speed
        if (velocity > maxFallSpeed) {
            velocity = maxFallSpeed
        }

        // move bird
        val birdY = state.birdY + velocity

        // move pipes
        val movedPipes = state.pipes
            .map {
                it.copy(x = it.x - 0.005f)
            }
            .filter {
                it.x > -0.25f
            }

        // spawn pipes
        val pipes = if (Random.nextFloat() < 0.015f) {

            movedPipes + Pipe(
                x = 1.2f,
                gapY = Random.nextFloat() * 0.45f + 0.15f
            )

        } else {
            movedPipes
        }

        var dead = false

        // world bounds
        if (birdY < 0f || birdY > 1f) {
            dead = true
        }

        // collision detection
        pipes.forEach { pipe ->

            val birdX = 0.12f
            val pipeX = pipe.x

            // smaller hitbox
            val touchingX = abs(pipeX - birdX) < 0.045f

            // larger gap
            val gapTop = pipe.gapY
            val gapBottom = pipe.gapY + 0.32f

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

        val goldEarned =
            (state.score * state.kingdom.goldMultiplier).toLong()

        return state.copy(
            kingdom = state.kingdom.copy(
                gold = state.kingdom.gold + goldEarned
            )
        )
    }
}
