package com.stickrun.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

class Obstacle(
    x: Float,
    val type: Type,
    val stackCount: Int = 1  // for CRATE type: how many stacked
) {
    enum class Type { CRATE, GLASS, SAW }

    val bounds: Rectangle

    // Saw rotation
    private var sawAngle = 0f

    companion object {
        const val CRATE_W  = 64f
        const val CRATE_H  = 64f
        const val GLASS_W  = 18f
        const val GLASS_H  = 120f
        const val SAW_R    = 40f
    }

    init {
        val groundY = Player.GROUND_Y
        bounds = when (type) {
            Type.CRATE -> Rectangle(x, groundY, CRATE_W, CRATE_H * stackCount)
            Type.GLASS -> Rectangle(x, groundY, GLASS_W, GLASS_H)
            Type.SAW   -> Rectangle(x - SAW_R, groundY, SAW_R * 2f, SAW_R * 2f)
        }
    }

    fun update(delta: Float) {
        if (type == Type.SAW) sawAngle += delta * 280f
    }

    fun draw(sr: ShapeRenderer) {
        when (type) {
            Type.CRATE -> drawCrates(sr)
            Type.GLASS -> drawGlass(sr)
            Type.SAW   -> drawSaw(sr)
        }
    }

    private fun drawCrates(sr: ShapeRenderer) {
        val x = bounds.x
        for (i in 0 until stackCount) {
            val cy = Player.GROUND_Y + i * CRATE_H
            val w  = CRATE_W
            val h  = CRATE_H

            // Shadow
            sr.color = Color(0.15f, 0.07f, 0.01f, 0.40f)
            sr.rect(x + 5f, cy - 4f, w, h)

            // Main face
            sr.color = Color(0.50f, 0.25f, 0.06f, 1f)
            sr.rect(x, cy, w, h)

            // Top highlight
            sr.color = Color(0.68f, 0.38f, 0.12f, 1f)
            sr.rect(x, cy + h - 8f, w, 8f)

            // Left highlight
            sr.color = Color(0.60f, 0.30f, 0.09f, 1f)
            sr.rect(x, cy, 8f, h)

            // Bottom dark
            sr.color = Color(0.26f, 0.11f, 0.02f, 1f)
            sr.rect(x, cy, w, 5f)

            // Cross brace lines
            sr.color = Color(0.34f, 0.16f, 0.03f, 0.7f)
            sr.rectLine(x + 8f, cy + 8f, x + w - 8f, cy + h - 8f, 3f)
            sr.rectLine(x + w - 8f, cy + 8f, x + 8f, cy + h - 8f, 3f)

            // Outline
            sr.color = Color(0.22f, 0.09f, 0.01f, 1f)
            sr.rectLine(x,     cy,     x + w, cy,     3f)
            sr.rectLine(x + w, cy,     x + w, cy + h, 3f)
            sr.rectLine(x + w, cy + h, x,     cy + h, 3f)
            sr.rectLine(x,     cy + h, x,     cy,     3f)
        }
    }

    private fun drawGlass(sr: ShapeRenderer) {
        val x = bounds.x
        val y = bounds.y
        val w = GLASS_W
        val h = GLASS_H

        // Glass body — semi-transparent cyan tint
        sr.color = Color(0.55f, 0.88f, 0.96f, 0.30f)
        sr.rect(x, y, w, h)

        // Bright edge highlights
        sr.color = Color(0.80f, 0.96f, 1.00f, 0.75f)
        sr.rect(x, y, 3f, h)              // left edge
        sr.rect(x + w - 3f, y, 3f, h)    // right edge
        sr.rect(x, y + h - 3f, w, 3f)    // top edge

        // Inner glint
        sr.color = Color(1f, 1f, 1f, 0.22f)
        sr.rect(x + 4f, y + h * 0.15f, 4f, h * 0.55f)

        // Bottom dark grounding line
        sr.color = Color(0.30f, 0.60f, 0.72f, 0.8f)
        sr.rect(x, y, w, 4f)
    }

    private fun drawSaw(sr: ShapeRenderer) {
        val cx = bounds.x + SAW_R
        val cy = bounds.y + SAW_R
        val r  = SAW_R

        // Outer metal ring
        sr.color = Color(0.72f, 0.72f, 0.72f, 1f)
        sr.circle(cx, cy, r, 32)

        // Teeth — 8 triangular spikes
        sr.color = Color(0.88f, 0.88f, 0.88f, 1f)
        for (i in 0 until 8) {
            val a  = sawAngle + i * 45f
            val tx = cx + com.badlogic.gdx.math.MathUtils.cosDeg(a) * (r + 14f)
            val ty = cy + com.badlogic.gdx.math.MathUtils.sinDeg(a) * (r + 14f)
            val l1x = cx + com.badlogic.gdx.math.MathUtils.cosDeg(a - 14f) * r
            val l1y = cy + com.badlogic.gdx.math.MathUtils.sinDeg(a - 14f) * r
            val l2x = cx + com.badlogic.gdx.math.MathUtils.cosDeg(a + 14f) * r
            val l2y = cy + com.badlogic.gdx.math.MathUtils.sinDeg(a + 14f) * r
            sr.triangle(l1x, l1y, tx, ty, l2x, l2y)
        }

        // Inner dark hub
        sr.color = Color(0.38f, 0.38f, 0.38f, 1f)
        sr.circle(cx, cy, r * 0.55f, 24)

        // Centre bolt
        sr.color = Color(0.82f, 0.82f, 0.82f, 1f)
        sr.circle(cx, cy, r * 0.18f, 12)

        // Danger red glow under blade
        sr.color = Color(0.9f, 0.15f, 0.05f, 0.18f)
        sr.circle(cx, cy, r + 18f, 24)
    }
}
