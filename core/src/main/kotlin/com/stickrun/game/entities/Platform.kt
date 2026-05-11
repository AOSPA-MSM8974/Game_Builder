package com.stickrun.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

class Platform(x: Float, y: Float, width: Float, height: Float = 20f, val type: PlatformType = PlatformType.NORMAL) {
    val bounds = Rectangle(x, y, width, height)

    enum class PlatformType { NORMAL, DARK, HIGHLIGHT }

    fun draw(sr: ShapeRenderer) {
        val x = bounds.x
        val y = bounds.y
        val w = bounds.width
        val h = bounds.height

        when (type) {
            PlatformType.NORMAL -> {
                // Shadow
                sr.color = Color(0.25f, 0.12f, 0.04f, 0.5f)
                sr.rect(x + 3, y - 4, w, h)
                // Main box
                sr.color = Color(0.55f, 0.28f, 0.08f, 1f)
                sr.rect(x, y, w, h)
                // Top highlight
                sr.color = Color(0.72f, 0.42f, 0.15f, 1f)
                sr.rect(x, y + h - 4f, w, 4f)
                // Left highlight
                sr.color = Color(0.65f, 0.35f, 0.10f, 1f)
                sr.rect(x, y, 4f, h)
            }
            PlatformType.DARK -> {
                sr.color = Color(0.18f, 0.08f, 0.02f, 0.5f)
                sr.rect(x + 3, y - 4, w, h)
                sr.color = Color(0.3f, 0.15f, 0.05f, 1f)
                sr.rect(x, y, w, h)
                sr.color = Color(0.45f, 0.22f, 0.08f, 1f)
                sr.rect(x, y + h - 4f, w, 4f)
            }
            PlatformType.HIGHLIGHT -> {
                sr.color = Color(0.3f, 0.15f, 0.02f, 0.4f)
                sr.rect(x + 3, y - 4, w, h)
                sr.color = Color(0.75f, 0.48f, 0.18f, 1f)
                sr.rect(x, y, w, h)
                sr.color = Color(0.95f, 0.72f, 0.35f, 1f)
                sr.rect(x, y + h - 4f, w, 4f)
            }
        }
    }
}
