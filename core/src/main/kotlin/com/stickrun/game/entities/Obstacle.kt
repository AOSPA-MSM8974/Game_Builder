package com.stickrun.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.stickrun.game.Assets

class Obstacle(x: Float, val type: Type, val stackCount: Int = 1) {

    enum class Type { CRATE, GLASS, SAW }

    val bounds: Rectangle
    private var sawAngle = MathUtils.random(0f, 360f)

    companion object {
        const val CRATE_SIZE = 96f
        const val GLASS_W    = 24f
        const val GLASS_H    = 160f
        const val SAW_R      = 64f
    }

    init {
        val gy = Player.GROUND_Y
        bounds = when (type) {
            Type.CRATE -> Rectangle(x, gy, CRATE_SIZE, CRATE_SIZE * stackCount)
            Type.GLASS -> Rectangle(x, gy, GLASS_W, GLASS_H)
            Type.SAW   -> Rectangle(x - SAW_R, gy - SAW_R * 0.3f, SAW_R * 2f, SAW_R * 2f)
        }
    }

    fun update(delta: Float) {
        if (type == Type.SAW) sawAngle = (sawAngle + delta * 300f) % 360f
    }

    fun draw(batch: SpriteBatch) {
        when (type) {
            Type.CRATE -> {
                for (i in 0 until stackCount) {
                    val cy = Player.GROUND_Y + i * CRATE_SIZE
                    batch.draw(Assets.crate,
                        bounds.x, cy,
                        CRATE_SIZE, CRATE_SIZE)
                }
            }
            Type.GLASS -> {
                batch.draw(Assets.glass,
                    bounds.x, bounds.y,
                    GLASS_W, GLASS_H)
            }
            Type.SAW -> {
                val cx = bounds.x + SAW_R
                val cy = bounds.y + SAW_R
                batch.draw(
                    Assets.saw,
                    cx - SAW_R, cy - SAW_R,  // position
                    SAW_R, SAW_R,             // origin
                    SAW_R * 2f, SAW_R * 2f,  // size
                    1f, 1f,                   // scale
                    sawAngle,                 // rotation
                    0, 0,                     // srcX, srcY
                    Assets.saw.width, Assets.saw.height, // srcW, srcH
                    false, false              // flipX, flipY
                )
            }
        }
    }

    // Fallback shape draw (if textures not loaded)
    fun drawShape(sr: ShapeRenderer) {
        when (type) {
            Type.CRATE -> {
                for (i in 0 until stackCount) {
                    val cy = Player.GROUND_Y + i * CRATE_SIZE
                    sr.color = Color(0.52f, 0.26f, 0.07f, 1f)
                    sr.rect(bounds.x, cy, CRATE_SIZE, CRATE_SIZE)
                }
            }
            Type.GLASS -> {
                sr.color = Color(0.55f, 0.88f, 0.96f, 0.40f)
                sr.rect(bounds.x, bounds.y, GLASS_W, GLASS_H)
            }
            Type.SAW -> {
                sr.color = Color(0.75f, 0.75f, 0.75f, 1f)
                sr.circle(bounds.x + SAW_R, bounds.y + SAW_R, SAW_R, 24)
            }
        }
    }
}
