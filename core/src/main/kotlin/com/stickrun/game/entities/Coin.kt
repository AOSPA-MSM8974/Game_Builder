package com.stickrun.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

class Coin(var cx: Float, var cy: Float) {

    var collected    = false
    var collectTimer = 0f
    private var bobTimer = 0f

    val bounds = Rectangle(cx - R, cy - R, R * 2f, R * 2f)

    companion object { const val R = 18f }

    fun update(delta: Float) {
        bobTimer += delta
        if (collected) collectTimer += delta * 3f
        cy = (cy - R) + R + Math.sin(bobTimer * 3.2).toFloat() * 5f  // gentle bob
        bounds.setCenter(cx, cy)
    }

    val done get() = collected && collectTimer >= 1f

    fun draw(sr: ShapeRenderer) {
        if (done) return
        val alpha = if (collected) (1f - collectTimer).coerceAtLeast(0f) else 1f
        val scale = if (collected) 1f + collectTimer * 0.7f else 1f
        val r     = R * scale

        // Glow
        sr.color = Color(1f, 0.85f, 0.10f, alpha * 0.22f)
        sr.circle(cx, cy, r + 10f, 14)

        // Body
        sr.color = Color(1f, 0.80f, 0.00f, alpha)
        sr.circle(cx, cy, r, 16)

        // Shine
        sr.color = Color(1f, 1f, 0.70f, alpha * 0.55f)
        sr.circle(cx - r * 0.22f, cy + r * 0.22f, r * 0.30f, 8)
    }
}
