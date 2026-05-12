package com.stickrun.game.world

import com.stickrun.game.entities.Coin
import com.stickrun.game.entities.Obstacle
import com.stickrun.game.entities.Player
import kotlin.random.Random

class WorldGenerator {

    private var rng = Random(System.currentTimeMillis())

    /**
     * Generate one chunk of obstacles + coins.
     * [difficulty] 0f..1f — controls gap shrinkage and obstacle complexity.
     */
    fun generateChunk(startX: Float, difficulty: Float): Pair<List<Obstacle>, List<Coin>> {
        val obstacles = mutableListOf<Obstacle>()
        val coins     = mutableListOf<Coin>()

        val minGap = lerp(420f, 200f, difficulty)
        val maxGap = lerp(700f, 350f, difficulty)

        var x = startX + 600f   // breathing room at chunk start

        repeat(10) {
            val gap = rng.nextFloat() * (maxGap - minGap) + minGap

            when {
                // Double crate cluster (only after some distance)
                difficulty > 0.25f && rng.nextFloat() < 0.22f -> {
                    val s1 = rng.nextInt(1, 3)
                    val s2 = rng.nextInt(1, 3)
                    val innerGap = rng.nextFloat() * 60f + 50f
                    obstacles.add(Obstacle(x, Obstacle.Type.CRATE, s1))
                    obstacles.add(Obstacle(x + Obstacle.CRATE_W + innerGap, Obstacle.Type.CRATE, s2))
                    // Coins arcing over
                    val clW = Obstacle.CRATE_W + innerGap + Obstacle.CRATE_W
                    val coinY = Player.GROUND_Y + maxOf(s1, s2) * Obstacle.CRATE_H + 36f
                    for (c in 0..3) coins.add(Coin(x + clW * 0.15f + c * clW * 0.22f, coinY))
                    x += clW + gap
                }

                // Glass pane (must jump over OR slide under if gap below it — future)
                difficulty > 0.15f && rng.nextFloat() < 0.20f -> {
                    obstacles.add(Obstacle(x, Obstacle.Type.GLASS))
                    // Coin right before it as a warning beacon
                    coins.add(Coin(x - 80f, Player.GROUND_Y + Coin.R * 3f))
                    x += Obstacle.GLASS_W + gap
                }

                // Saw blade
                difficulty > 0.40f && rng.nextFloat() < 0.18f -> {
                    val sawX = x + Obstacle.SAW_R
                    obstacles.add(Obstacle(sawX, Obstacle.Type.SAW))
                    x += Obstacle.SAW_R * 2f + gap
                }

                // Default: single or double-stacked crate
                else -> {
                    val stacks = when {
                        difficulty > 0.6f -> rng.nextInt(1, 4)
                        difficulty > 0.3f -> rng.nextInt(1, 3)
                        else              -> 1
                    }
                    obstacles.add(Obstacle(x, Obstacle.Type.CRATE, stacks))
                    // Coins above crate
                    val coinY = Player.GROUND_Y + stacks * Obstacle.CRATE_H + 36f
                    val num   = rng.nextInt(2, 5)
                    for (c in 0 until num) {
                        coins.add(Coin(x - 20f + (Obstacle.CRATE_W + 40f) / num * c, coinY))
                    }
                    x += Obstacle.CRATE_W + gap
                }
            }
        }

        return Pair(obstacles, coins)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}
