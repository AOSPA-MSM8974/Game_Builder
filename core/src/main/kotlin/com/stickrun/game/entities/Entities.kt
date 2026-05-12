package com.stickrun.game.entities

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.stickrun.game.Assets

// ── Crate ─────────────────────────────────────────────────────────────────
class Crate(val x: Float, val y: Float, val stack: Int = 1) {
    companion object { const val SIZE = 64f }
    val bounds = Rectangle(x, y, SIZE, SIZE * stack)
    fun draw(batch: SpriteBatch) {
        for (i in 0 until stack) batch.draw(Assets.crate, x, y + i * SIZE, SIZE, SIZE)
    }
}

// ── Saw ───────────────────────────────────────────────────────────────────
class Saw(val cx: Float, val baseY: Float) {
    companion object { const val R = 32f }
    private var angle = MathUtils.random(0f, 360f)
    val bounds = Rectangle(cx - R, baseY, R * 2f, R * 2f)
    fun update(delta: Float) { angle = (angle + delta * 280f) % 360f }
    fun draw(batch: SpriteBatch) {
        batch.draw(Assets.saw, cx - R, baseY, R, R, R * 2f, R * 2f, 1f, 1f, angle,
            0, 0, Assets.saw.width, Assets.saw.height, false, false)
    }
}

// ── Coin ──────────────────────────────────────────────────────────────────
class Coin(val x: Float, val y: Float) {
    companion object { const val R = 20f; const val FRAME_TIME = 0.08f }
    var collected = false; var collectTimer = 0f
    private var bobTimer = 0f; private var frameTimer = 0f; private var frameIdx = 0
    val bounds = Rectangle(x - R, y - R, R * 2f, R * 2f)
    fun update(delta: Float) {
        bobTimer += delta; frameTimer += delta
        if (frameTimer >= FRAME_TIME) { frameIdx = (frameIdx + 1) % 8; frameTimer = 0f }
        if (collected) collectTimer += delta * 2.8f
    }
    val done get() = collected && collectTimer >= 1f
    fun draw(batch: SpriteBatch) {
        if (done) return
        val alpha = if (collected) (1f - collectTimer).coerceAtLeast(0f) else 1f
        val scale = if (collected) 1f + collectTimer * 0.6f else 1f
        val bobY  = y + MathUtils.sin(bobTimer * 3.5f) * 4f
        val size  = R * 2f * scale
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(Assets.coinFrames[frameIdx], x - size / 2f, bobY - size / 2f, size, size)
        batch.setColor(1f, 1f, 1f, 1f)
    }
}

// ── Platform ──────────────────────────────────────────────────────────────
class Platform(val x: Float, val platformY: Float, val width: Float) {
    companion object { const val H = 28f; const val POLE_W = 14f }
    val surfaceY get() = platformY + H
    val bounds = Rectangle(x, platformY, width, H)
    val crates  = mutableListOf<Crate>()
    var saw: Saw? = null

    fun update(delta: Float) { saw?.update(delta) }

    fun draw(batch: SpriteBatch) {
        val poleH = (platformY - Player.GROUND_Y).coerceAtLeast(0f)
        if (poleH > 0f) {
            val px = x + width / 2f - POLE_W / 2f
            batch.draw(Assets.pole, px, Player.GROUND_Y, POLE_W, poleH,
                0, 0, Assets.pole.width, Assets.pole.height, false, false)
        }
        batch.draw(Assets.platform, x, platformY, width, H,
            0, 0, Assets.platform.width, Assets.platform.height, false, false)
        crates.forEach { it.draw(batch) }
        saw?.draw(batch)
    }
}
