package com.idlekingdom.model

data class FlappyState(
    val birdY: Float = 0.5f,
    val velocity: Float = 0f,
    val pipes: List<Pipe> = listOf(),
    val score: Int = 0,
    val gameOver: Boolean = false,
    val kingdom: KingdomState = KingdomState()
)

data class Pipe(
    val x: Float,
    val gapY: Float
)
