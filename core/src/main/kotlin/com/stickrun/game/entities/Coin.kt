package com.stickrun.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.stickrun.game.Assets

class Coin(var cx: Float, var cy: Float) {

    var collected    = false
    var collectTimer = 0f
    private var bobTimer    = 0f
    private var frameTimer  = 0f
    private var frameIdx    = 0

    val bounds = Rectangle(cx - R, cy - R, R * 2f, R * 2f)

    companion object {
        const val R          = 28f
        const val FRAME_TIME = 0.07f   // seconds per animation frame
    }

    fun update(delta: Float) {
        bobTimer   += delta
        frameTimer += delta
        if (frameTimer >= FRAME_TIME) {
            frameIdx   = (frameIdx + 1) % 8
            frameTimer = 0f
        }
        if (collected) collectTimer += delta * 2.5f
        bounds.setCenter(cx, cy + kotlin.math.sin(bobTimer * 3.2).toFloat() * 6f)
    }

    val done get() = collected && collectTimer >= 1f

    fun draw(batch: SpriteBatch) {
        if (done) return
        val alpha = if (collected) (1f - collectTimer).coerceAtLeast(0f) else 1f
        val scale = if (collected) 1f + collectTimer * 0.8f else 1f
        val drawY = cy + kotlin.math.sin(bobTimer * 3.2).toFloat() * 6f

        batch.setColor(1f, 1f, 1f, alpha)
        val frame = Assets.coinFrames[frameIdx]
        val size  = R * 2f * scale
        batch.draw(frame, cx - size/2f, drawY - size/2f, size, size)
        batch.setColor(1f, 1f, 1f, 1f)
    }
}
