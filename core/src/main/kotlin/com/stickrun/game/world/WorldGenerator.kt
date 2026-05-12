package com.stickrun.game.world

import com.stickrun.game.entities.*
import kotlin.random.Random

class WorldGenerator {
    private val rng = Random(System.currentTimeMillis())

    fun generateChunk(startX: Float, difficulty: Float): List<Platform> {
        val platforms = mutableListOf<Platform>()
        val minGap = lerp(420f, 180f, difficulty)
        val maxGap = lerp(700f, 320f, difficulty)
        var x = startX + 600f

        repeat(6) {
            val gap       = rng.nextFloat() * (maxGap - minGap) + minGap
            val platW     = rng.nextFloat() * 160f + 160f
            val platY     = lerp(200f, 260f, rng.nextFloat()) // elevation
            val plat      = Platform(x, platY, platW)

            // Crates on the platform
            val numCrates = when {
                difficulty > 0.6f -> rng.nextInt(1, 4)
                difficulty > 0.3f -> rng.nextInt(1, 3)
                else              -> if (rng.nextFloat() < 0.7f) 1 else 0
            }
            if (numCrates > 0) {
                val stack  = if (difficulty > 0.5f && rng.nextFloat() < 0.35f) 2 else 1
                val crateX = x + rng.nextFloat() * (platW - numCrates * 64f).coerceAtLeast(0f)
                for (i in 0 until numCrates) {
                    plat.crates.add(Crate(crateX + i * 64f, plat.surfaceY, stack))
                }
            }

            // Saw on platform
            if (difficulty > 0.4f && rng.nextFloat() < 0.25f && plat.crates.isEmpty()) {
                plat.saw = Saw(x + platW / 2f, plat.surfaceY)
            }

            platforms.add(plat)
            x += platW + gap
        }
        return platforms
    }

    // Ground-level crates (no platform — sitting directly on ground like the original)
    fun generateGroundCrates(startX: Float, difficulty: Float): List<Crate> {
        val crates = mutableListOf<Crate>()
        val minGap = lerp(380f, 160f, difficulty)
        val maxGap = lerp(640f, 280f, difficulty)
        var x = startX + 400f

        repeat(5) {
            val gap   = rng.nextFloat() * (maxGap - minGap) + minGap
            val stack = when {
                difficulty > 0.65f -> rng.nextInt(1, 4)
                difficulty > 0.35f -> rng.nextInt(1, 3)
                else               -> 1
            }
            crates.add(Crate(x, Player.GROUND_Y, stack))
            x += 64f + gap
        }
        return crates
    }

    fun generateCoins(platforms: List<Platform>, groundCrates: List<Crate>): List<Coin> {
        val coins = mutableListOf<Coin>()
        // Coins above platform crates
        for (p in platforms) {
            if (p.crates.isNotEmpty()) {
                val topCrate = p.crates.first()
                val topY = topCrate.y + topCrate.stack * Crate.SIZE + 28f
                for (i in 0..2) coins.add(Coin(topCrate.x + 32f + i * 36f, topY))
            } else {
                // Coins floating on empty platform
                for (i in 0..3) coins.add(Coin(p.x + 30f + i * 44f, p.surfaceY + 36f))
            }
        }
        // Coins above ground crates
        for (c in groundCrates) {
            val topY = c.y + c.stack * Crate.SIZE + 30f
            for (i in 0..1) coins.add(Coin(c.x + 16f + i * 36f, topY))
        }
        return coins
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}
