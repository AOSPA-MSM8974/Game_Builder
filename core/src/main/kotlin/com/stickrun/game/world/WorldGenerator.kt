package com.stickrun.game.world

import com.stickrun.game.entities.Coin
import com.stickrun.game.entities.Platform
import kotlin.random.Random

class WorldGenerator {

    fun generateLevel(): Pair<List<Platform>, List<Coin>> {
        val platforms = mutableListOf<Platform>()
        val coins = mutableListOf<Coin>()

        // Ground floor sections
        platforms.add(Platform(0f, 60f, 400f, 20f))
        platforms.add(Platform(450f, 60f, 300f, 20f))
        platforms.add(Platform(800f, 60f, 350f, 20f))
        platforms.add(Platform(1200f, 60f, 400f, 20f))
        platforms.add(Platform(1650f, 60f, 500f, 20f))
        platforms.add(Platform(2200f, 60f, 600f, 20f))
        platforms.add(Platform(2900f, 60f, 400f, 20f))
        platforms.add(Platform(3400f, 60f, 500f, 20f))
        platforms.add(Platform(3950f, 60f, 600f, 20f))

        // Elevated platforms - varied heights
        val elevatedData = listOf(
            floatArrayOf(120f, 180f, 140f),
            floatArrayOf(320f, 240f, 120f),
            floatArrayOf(500f, 180f, 150f),
            floatArrayOf(700f, 280f, 100f),
            floatArrayOf(900f, 200f, 160f),
            floatArrayOf(1050f, 320f, 110f),
            floatArrayOf(1250f, 200f, 140f),
            floatArrayOf(1420f, 300f, 120f),
            floatArrayOf(1650f, 180f, 130f),
            floatArrayOf(1850f, 260f, 150f),
            floatArrayOf(2050f, 340f, 110f),
            floatArrayOf(2200f, 200f, 160f),
            floatArrayOf(2420f, 300f, 130f),
            floatArrayOf(2620f, 180f, 140f),
            floatArrayOf(2800f, 400f, 100f),
            floatArrayOf(2950f, 250f, 120f),
            floatArrayOf(3100f, 180f, 150f),
            floatArrayOf(3300f, 320f, 110f),
            floatArrayOf(3500f, 200f, 140f),
            floatArrayOf(3700f, 280f, 130f),
            floatArrayOf(3900f, 180f, 160f),
            floatArrayOf(4100f, 350f, 120f),
            floatArrayOf(4300f, 240f, 150f),
            // High platforms
            floatArrayOf(600f, 420f, 100f),
            floatArrayOf(1300f, 460f, 90f),
            floatArrayOf(2100f, 500f, 80f),
            floatArrayOf(3200f, 480f, 90f)
        )

        val rng = Random(42)
        elevatedData.forEachIndexed { i, data ->
            val type = when {
                i % 7 == 0 -> Platform.PlatformType.HIGHLIGHT
                i % 3 == 0 -> Platform.PlatformType.DARK
                else -> Platform.PlatformType.NORMAL
            }
            platforms.add(Platform(data[0], data[1], data[2], 18f, type))

            // Coins on platforms
            val numCoins = rng.nextInt(1, 5)
            for (c in 0 until numCoins) {
                val cx = data[0] + 15f + (data[2] - 30f) / numCoins * c
                val cy = data[1] + 30f
                coins.add(Coin(cx, cy))
            }
        }

        // Ground coins
        listOf(80f, 200f, 460f, 600f, 820f, 1000f, 1260f, 1700f, 1900f, 2250f).forEach { cx ->
            coins.add(Coin(cx, 100f))
        }

        return Pair(platforms, coins)
    }

    // Procedurally extend world as player advances
    fun generateChunk(startX: Float, rng: Random = Random(startX.toInt())): Pair<List<Platform>, List<Coin>> {
        val platforms = mutableListOf<Platform>()
        val coins = mutableListOf<Coin>()
        val chunkWidth = 800f

        // Ground
        if (rng.nextFloat() > 0.3f) {
            platforms.add(Platform(startX, 60f, chunkWidth * 0.6f, 20f))
        }

        // Elevated platforms
        var x = startX + 60f
        while (x < startX + chunkWidth - 100f) {
            val w = rng.nextFloat() * 100f + 80f
            val h = rng.nextFloat() * 300f + 120f
            val type = Platform.PlatformType.values()[rng.nextInt(3)]
            platforms.add(Platform(x, h, w, 18f, type))

            val numCoins = rng.nextInt(1, 5)
            for (c in 0 until numCoins) {
                coins.add(Coin(x + 15f + (w - 30f) / numCoins * c, h + 30f))
            }

            x += w + rng.nextFloat() * 80f + 60f
        }

        return Pair(platforms, coins)
    }
}
