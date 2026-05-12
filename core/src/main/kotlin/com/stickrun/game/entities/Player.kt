package com.stickrun.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle

class Player(startX: Float, startY: Float) {

    // Position & physics
    var x        = startX
    var y        = startY
    var velY     = 0f
    var onGround = false
    var alive    = true

    // State
    var jumping      = false
    var sliding      = false
    var jumpCount    = 0
    var animTimer    = 0f
    private var slideTimer = 0f
    private var legAngle   = 0f
    private var deathTimer = 0f

    // Cosmetics
    var bodyColor = Color(0.08f, 0.08f, 0.08f, 1f)
    var hatType   = HatType.NONE
    var hatColor  = Color(0.75f, 0.12f, 0.12f, 1f)

    val bounds = Rectangle(startX, startY, W, H)

    companion object {
        const val W          = 44f
        const val H          = 88f
        const val SLIDE_W    = 72f
        const val SLIDE_H    = 44f
        const val GROUND_Y   = 140f
        const val JUMP_VEL   = 1050f
        const val GRAVITY    = -2400f
        const val MAX_FALL   = -1800f
        const val MAX_JUMPS  = 2
        const val SLIDE_DUR  = 0.5f
    }

    enum class HatType { NONE, CAP, TOP_HAT, BEANIE }

    // ── Update ────────────────────────────────────────────────────────────

    fun update(delta: Float, speed: Float) {
        if (!alive) { deathTimer += delta; return }

        animTimer += delta

        // Horizontal auto-run
        x += speed * delta

        // Slide countdown
        if (sliding) {
            slideTimer -= delta
            if (slideTimer <= 0f) { sliding = false; slideTimer = 0f }
        }

        // Gravity
        velY = (velY + GRAVITY * delta).coerceAtLeast(MAX_FALL)
        y   += velY * delta

        // Hard ground — cannot go below
        if (y <= GROUND_Y) {
            y         = GROUND_Y
            velY      = 0f
            onGround  = true
            jumpCount = 0
            jumping   = false
        }

        // Animate legs
        legAngle = when {
            sliding   -> 0f
            onGround  -> MathUtils.sin(animTimer * 14f) * 34f
            velY > 0f -> -30f
            else      -> 26f
        }

        // Reset each frame — platform collision sets it back
        onGround = false
        syncBounds()
    }

    fun jump() {
        if (!alive || sliding) return
        if (jumpCount < MAX_JUMPS) {
            velY      = JUMP_VEL
            jumpCount++
            onGround  = false
            jumping   = true
        }
    }

    fun slide() {
        if (!alive || !onGround || sliding) return
        sliding    = true
        slideTimer = SLIDE_DUR
        syncBounds()
    }

    fun landOn(surfaceY: Float) {
        if (velY <= 0f) {
            y         = surfaceY
            velY      = 0f
            onGround  = true
            jumpCount = 0
            jumping   = false
            syncBounds()
        }
    }

    private fun syncBounds() {
        if (sliding) bounds.set(x, y, SLIDE_W, SLIDE_H)
        else         bounds.set(x, y, W, H)
    }

    // ── Draw ──────────────────────────────────────────────────────────────

    fun draw(sr: ShapeRenderer) {
        val alpha = if (!alive) (1f - (deathTimer * 2f).coerceAtMost(1f)) else 1f
        if (alpha <= 0f) return

        if (sliding) drawSlide(sr, alpha)
        else         drawStand(sr, alpha)
    }

    private fun drawStand(sr: ShapeRenderer, alpha: Float) {
        val cx   = x + W / 2f
        val cy   = y
        val tB   = cy + H * 0.40f
        val tT   = cy + H * 0.72f
        val hCy  = cy + H * 0.87f
        val hR   = H * 0.135f
        val legL = 36f
        val armL = 30f
        val la   = legAngle
        val aa   = -la * 0.40f

        val bc = Color(bodyColor.r, bodyColor.g, bodyColor.b, alpha)
        val sc = Color(0.91f, 0.76f, 0.60f, alpha)

        // Legs
        sr.color = bc
        sr.rectLine(cx, tB, cx + MathUtils.cosDeg(252f + la)*legL, tB + MathUtils.sinDeg(252f + la)*legL, 7f)
        sr.rectLine(cx, tB, cx + MathUtils.cosDeg(288f - la)*legL, tB + MathUtils.sinDeg(288f - la)*legL, 7f)

        // Torso
        sr.rectLine(cx, tB, cx, tT, 8f)

        // Arms
        val aY = tT - (tT - tB) * 0.18f
        sr.rectLine(cx, aY, cx + MathUtils.cosDeg(205f + aa)*armL, aY + MathUtils.sinDeg(205f + aa)*armL, 6f)
        sr.rectLine(cx, aY, cx + MathUtils.cosDeg(-25f + aa)*armL, aY + MathUtils.sinDeg(-25f + aa)*armL, 6f)

        // Head
        sr.color = sc
        sr.circle(cx, hCy, hR, 16)

        // Eye
        sr.color = bc
        sr.circle(cx + hR * 0.38f, hCy + hR * 0.08f, 3f, 8)

        drawHat(sr, cx, hCy, hR, alpha)
    }

    private fun drawSlide(sr: ShapeRenderer, alpha: Float) {
        val bc = Color(bodyColor.r, bodyColor.g, bodyColor.b, alpha)
        val sc = Color(0.91f, 0.76f, 0.60f, alpha)
        val hR = SLIDE_H * 0.26f
        val cy = y + SLIDE_H * 0.52f

        // Body
        sr.color = bc
        sr.rectLine(x + 10f, cy, x + SLIDE_W - hR * 1.6f, cy, 10f)

        // Trailing legs
        sr.rectLine(x + 14f, cy, x - 10f, y + 5f,  6f)
        sr.rectLine(x + 14f, cy, x - 4f,  y + 20f, 6f)

        // Head at front
        sr.color = sc
        sr.circle(x + SLIDE_W - hR, cy + hR * 0.28f, hR, 14)
        sr.color = bc
        sr.circle(x + SLIDE_W - hR + hR*0.38f, cy + hR*0.36f, 2.5f, 8)
    }

    private fun drawHat(sr: ShapeRenderer, cx: Float, hCy: Float, r: Float, alpha: Float) {
        val hc = Color(hatColor.r, hatColor.g, hatColor.b, alpha)
        sr.color = hc
        when (hatType) {
            HatType.CAP -> {
                sr.rect(cx - r*0.28f, hCy + r*0.50f, r*1.72f, r*0.28f)
                sr.rect(cx - r*0.72f, hCy + r*0.72f, r*1.44f, r*0.90f)
            }
            HatType.TOP_HAT -> {
                sr.rect(cx - r*0.90f, hCy + r*0.54f, r*1.80f, r*0.28f)
                sr.rect(cx - r*0.52f, hCy + r*0.76f, r*1.04f, r*1.44f)
            }
            HatType.BEANIE -> {
                sr.circle(cx, hCy + r*0.44f, r*0.96f, 14)
                sr.color = Color(hc.r*0.72f, hc.g*0.72f, hc.b*0.72f, alpha)
                sr.rect(cx - r, hCy + r*0.12f, r*2f, r*0.44f)
            }
            HatType.NONE -> Unit
        }
    }
}
