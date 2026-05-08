package com.idlekingdom.engine

import com.idlekingdom.model.*
import kotlin.random.Random

class FlappyEngine {

    fun update(state: FlappyState): FlappyState {

        if (state.gameOver) return state

        val gravity = state.kingdom.gravity
        val newVelocity = state.velocity + gravity
        val newY = state.birdY + newVelocity

        val pipes = state.pipes
            .map { it.copy(x = it.x - 0.01f) }
            .filter { it.x > -0.2f }

        val spawn = if (Random.nextFloat() < 0.02f) {
            pipes + Pipe(1.2f, Random.nextFloat())
        } else pipes

        val dead = newY < 0f || newY > 1f

        return state.copy(
            birdY = newY,
            velocity = newVelocity,
            pipes = spawn,
            score = state.score + 1,
            gameOver = dead
        )
    }

    fun flap(state: FlappyState): FlappyState {
        if (state.gameOver) return state
        return state.copy(velocity = -0.04f)
    }

    fun reward(state: FlappyState): FlappyState {
        val gold = (state.score * state.kingdom.goldMultiplier).toLong()

        return state.copy(
            kingdom = state.kingdom.copy(
                gold = state.kingdom.gold + gold
            )
        )
    }
}
