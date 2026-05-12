package com.stickrun.game.world

import com.stickrun.game.entities.Coin
import com.stickrun.game.entities.Obstacle
import com.stickrun.game.entities.Player
import kotlin.random.Random

class WorldGenerator {
    private var rng = Random(System.currentTimeMillis())

    fun generateChunk(startX: Float, difficulty: Float): Pair<List<Obstacle>, List<Coin>> {
        val obstacles = mutableListOf<Obstacle>()
        val coins     = mutableListOf<Coin>()

        // Gap between obstacles (shrinks with difficulty)
        val minGap = lerp(500f, 240f, difficulty)
        val maxGap = lerp(800f, 400f, difficulty)

        var x = startX + 800f   // large breathing room at start of each chunk

        repeat(8) {
            val gap  = rng.nextFloat() * (maxGap - minGap) + minGap
            val roll = rng.nextFloat()

            when {
                // Saw blade — only after difficulty 0.5
                difficulty > 0.50f && roll < 0.15f -> {
                    val sawX = x + Obstacle.SAW_R
                    obstacles.add(Obstacle(sawX, Obstacle.Type.SAW))
                    coins.add(Coin(sawX, Player.GROUND_Y + Obstacle.SAW_R * 2.5f))
                    x += Obstacle.SAW_R * 2f + gap
                }

                // Glass pane — only after difficulty 0.2
                difficulty > 0.20f && roll < 0.30f -> {
                    obstacles.add(Obstacle(x, Obstacle.Type.GLASS))
                    // Coin before it as visual cue
                    coins.add(Coin(x - 120f, Player.GROUND_Y + Coin.R * 3f))
                    x += Obstacle.GLASS_W + gap
                }

                // Double crate cluster — only after difficulty 0.3
                difficulty > 0.30f && roll < 0.50f -> {
                    val s1    = rng.nextInt(1, if (difficulty > 0.6f) 4 else 3)
                    val s2    = rng.nextInt(1, if (difficulty > 0.6f) 3 else 2)
                    val inner = rng.nextFloat() * 80f + 70f
                    obstacles.add(Obstacle(x, Obstacle.Type.CRATE, s1))
                    obstacles.add(Obstacle(x + Obstacle.CRATE_SIZE + inner, Obstacle.Type.CRATE, s2))
                    val clusterW = Obstacle.CRATE_SIZE + inner + Obstacle.CRATE_SIZE
                    val coinH    = Player.GROUND_Y + maxOf(s1, s2) * Obstacle.CRATE_SIZE + 44f
                    for (c in 0..3) coins.add(Coin(x + clusterW * 0.15f + c * clusterW * 0.24f, coinH))
                    x += clusterW + gap
                }

                // Default: single crate (1–2 stacked)
                else -> {
                    val stacks = if (difficulty > 0.4f) rng.nextInt(1, 3) else 1
                    obstacles.add(Obstacle(x, Obstacle.Type.CRATE, stacks))
                    val coinH = Player.GROUND_Y + stacks * Obstacle.CRATE_SIZE + 44f
                    val num   = rng.nextInt(2, 5)
                    for (c in 0 until num) {
                        coins.add(Coin(x - 24f + (Obstacle.CRATE_SIZE + 48f) / num * c, coinH))
                    }
                    x += Obstacle.CRATE_SIZE + gap
                }
            }
        }

        return Pair(obstacles, coins)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}
