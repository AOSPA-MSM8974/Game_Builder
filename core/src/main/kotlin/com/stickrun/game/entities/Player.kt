package com.stickrun.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle

class Player(startX: Float, startY: Float) {

    var x = startX
    var y = startY
    var velY = 0f
    var onGround = false
    var sliding = false
    var jumpCount = 0
    var animTimer = 0f
    var alive = true

    private var slideTimer = 0f
    private var legAngle = 0f

    // Cosmetics
    var bodyColor: Color = Color(0.08f, 0.08f, 0.08f, 1f)
    var hatType: HatType = HatType.NONE
    var hatColor: Color = Color(0.8f, 0.15f, 0.15f, 1f)

    val bounds = Rectangle(startX, startY, W, H)

    companion object {
        const val W           = 48f
        const val H           = 96f
        const val SLIDE_W     = 80f
        const val SLIDE_H     = 48f
        const val GROUND_Y    = 130f
        const val JUMP_VEL    = 1100f
        const val GRAVITY     = -2600f
        const val MAX_FALL    = -1900f
        const val MAX_JUMPS   = 2
        const val SLIDE_TIME  = 0.55f
    }

    enum class HatType { NONE, CAP, TOP_HAT, BEANIE }

    fun update(delta: Float, speed: Float) {
        animTimer += delta
        x += speed * delta

        // Slide countdown
        if (sliding) {
            slideTimer -= delta
            if (slideTimer <= 0f) { sliding = false; slideTimer = 0f }
        }

        // Gravity
        velY = (velY + GRAVITY * delta).coerceAtLeast(MAX_FALL)
        y   += velY * delta

        // Hard floor — cannot fall through
        if (y <= GROUND_Y) {
            y      = GROUND_Y
            velY   = 0f
            onGround = true
            jumpCount = 0
        }

        // Leg swing
        legAngle = when {
            sliding    -> 0f
            onGround   -> MathUtils.sin(animTimer * 14f) * 34f
            velY > 0f  -> -28f
            else       -> 24f
        }

        // Reset for next frame; collision will flip it back if needed
        onGround = false
        syncBounds()
    }

    fun jump() {
        if (sliding || !alive) return
        if (jumpCount < MAX_JUMPS) {
            velY = JUMP_VEL
            jumpCount++
            onGround = false
        }
    }

    fun slide() {
        if (!alive || !onGround || sliding) return
        sliding   = true
        slideTimer = SLIDE_TIME
        syncBounds()
    }

    fun landOn(surfaceY: Float) {
        if (velY <= 0f) {
            y         = surfaceY
            velY      = 0f
            onGround  = true
            jumpCount = 0
            syncBounds()
        }
    }

    private fun syncBounds() {
        if (sliding) bounds.set(x, y, SLIDE_W, SLIDE_H)
        else         bounds.set(x, y, W, H)
    }

    // ── Drawing ─────────────────────────────────────────────────────────────

    fun draw(sr: ShapeRenderer) {
        if (sliding) drawSlide(sr) else drawStand(sr)
    }

    private fun drawStand(sr: ShapeRenderer) {
        val cx    = x + W / 2f
        val cy    = y
        val tB    = cy + H * 0.40f   // torso bottom
        val tT    = cy + H * 0.72f   // torso top
        val hCy   = cy + H * 0.87f   // head centre
        val hR    = H * 0.135f       // head radius
        val legL  = 40f
        val armL  = 34f
        val la    = legAngle
        val aa    = -la * 0.42f

        // ── Legs
        sr.color = bodyColor
        sr.rectLine(cx, tB, cx + MathUtils.cosDeg(252f + la) * legL, tB + MathUtils.sinDeg(252f + la) * legL, 7f)
        sr.rectLine(cx, tB, cx + MathUtils.cosDeg(288f - la) * legL, tB + MathUtils.sinDeg(288f - la) * legL, 7f)

        // ── Torso
        sr.rectLine(cx, tB, cx, tT, 8f)

        // ── Arms
        val aY = tT - (tT - tB) * 0.18f
        sr.rectLine(cx, aY, cx + MathUtils.cosDeg(202f + aa) * armL, aY + MathUtils.sinDeg(202f + aa) * armL, 6f)
        sr.rectLine(cx, aY, cx + MathUtils.cosDeg(-22f + aa) * armL, aY + MathUtils.sinDeg(-22f + aa) * armL, 6f)

        // ── Head
        sr.color = Color(0.91f, 0.76f, 0.60f, 1f)
        sr.circle(cx, hCy, hR, 18)

        // ── Eye
        sr.color = bodyColor
        sr.circle(cx + hR * 0.38f, hCy + hR * 0.08f, 3.5f, 8)

        drawHat(sr, cx, hCy, hR)
    }

    private fun drawSlide(sr: ShapeRenderer) {
        val cx  = x + SLIDE_W * 0.5f
        val cy  = y + SLIDE_H * 0.5f
        val hR  = SLIDE_H * 0.26f

        // Low body
        sr.color = bodyColor
        sr.rectLine(x + 10f, cy, x + SLIDE_W - hR * 1.4f, cy, 10f)

        // Head at front
        sr.color = Color(0.91f, 0.76f, 0.60f, 1f)
        sr.circle(x + SLIDE_W - hR, cy + hR * 0.3f, hR, 14)
        sr.color = bodyColor
        sr.circle(x + SLIDE_W - hR + hR * 0.38f, cy + hR * 0.38f, 3f, 8)

        // Trailing legs
        sr.color = bodyColor
        sr.rectLine(x + 14f, cy, x - 10f, y + 4f, 6f)
        sr.rectLine(x + 14f, cy, x - 6f,  y + 18f, 6f)
    }

    private fun drawHat(sr: ShapeRenderer, cx: Float, hCy: Float, r: Float) {
        sr.color = hatColor
        when (hatType) {
            HatType.CAP -> {
                sr.rect(cx - r * 0.28f, hCy + r * 0.5f,  r * 1.7f, r * 0.28f)  // brim
                sr.rect(cx - r * 0.72f, hCy + r * 0.72f, r * 1.44f, r * 0.88f) // dome
            }
            HatType.TOP_HAT -> {
                sr.rect(cx - r * 0.88f, hCy + r * 0.55f, r * 1.76f, r * 0.28f) // brim
                sr.rect(cx - r * 0.52f, hCy + r * 0.76f, r * 1.04f, r * 1.4f)  // tall crown
            }
            HatType.BEANIE -> {
                sr.circle(cx, hCy + r * 0.44f, r * 0.96f, 14)
                sr.color = hatColor.cpy().mul(0.72f)
                sr.rect(cx - r, hCy + r * 0.14f, r * 2f, r * 0.44f)
            }
            HatType.NONE -> Unit
        }
    }
}
