package com.stickrun.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Vector2

class Coin(x: Float, y: Float) {
    val position = Vector2(x, y)
    val bounds = Circle(x + RADIUS, y + RADIUS, RADIUS)
    var collected = false
    var animTimer = 0f
    var collectAnim = 0f // 0 = not collected, > 0 = collecting

    companion object {
        const val RADIUS = 10f
    }

    fun update(delta: Float) {
        animTimer += delta
        if (collectAnim > 0f) {
            collectAnim += delta * 3f
        }
    }

    fun collect() {
        if (!collected) {
            collected = true
            collectAnim = 0.01f
        }
    }

    fun isFinished() = collected && collectAnim > 1f

    fun draw(sr: ShapeRenderer) {
        if (collected && collectAnim <= 0f) return

        val alpha = if (collected) (1f - collectAnim).coerceAtLeast(0f) else 1f
        val scale = if (collected) 1f + collectAnim * 0.5f else 1f + Math.sin((animTimer * 4.0)).toFloat() * 0.08f
        val r = RADIUS * scale

        val bobY = position.y + Math.sin((animTimer * 3.0)).toFloat() * 3f

        // Outer glow
        sr.color = Color(1f, 0.85f, 0.1f, alpha * 0.3f)
        sr.circle(position.x + RADIUS, bobY + RADIUS, r + 5f, 16)

        // Main coin
        sr.color = Color(1f, 0.82f, 0f, alpha)
        sr.circle(position.x + RADIUS, bobY + RADIUS, r, 16)

        // Shine
        sr.color = Color(1f, 1f, 0.7f, alpha * 0.6f)
        sr.circle(position.x + RADIUS - r * 0.2f, bobY + RADIUS + r * 0.2f, r * 0.35f, 10)
    }
}
