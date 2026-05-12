package com.stickrun.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle

class Player(startX: Float, startY: Float) {

    var x        = startX
    var y        = startY
    var velY     = 0f
    var onGround = false
    var alive    = true
    var jumpCount = 0
    var animTimer = 0f
    var totalJumps = 0

    // Customization — defaults match the video (black body, top hat, green shoes)
    var bodyColor  = Color(0.05f, 0.05f, 0.05f, 1f)
    var shoeColor  = Color(0.15f, 0.75f, 0.15f, 1f)
    var hatType    = HatType.TOP_HAT
    var hatColor   = Color(0.05f, 0.05f, 0.05f, 1f)

    private var legAngle   = 0f
    private var deathTimer = 0f

    val bounds = Rectangle(startX, startY, W, H)

    companion object {
        const val W         = 36f
        const val H         = 80f
        const val GROUND_Y  = 96f     // sits on top of ground strip
        const val JUMP_VEL  = 980f
        const val GRAVITY   = -2200f
        const val MAX_FALL  = -1600f
        const val MAX_JUMPS = 2
    }

    enum class HatType { NONE, CAP, TOP_HAT }

    fun update(delta: Float, speed: Float) {
        if (!alive) { deathTimer += delta; return }

        animTimer += delta
        x += speed * delta

        velY = (velY + GRAVITY * delta).coerceAtLeast(MAX_FALL)
        y   += velY * delta

        // Hard floor
        if (y <= GROUND_Y) {
            y = GROUND_Y; velY = 0f; onGround = true; jumpCount = 0
        }

        legAngle = when {
            onGround  -> MathUtils.sin(animTimer * 13f) * 32f
            velY > 0f -> -28f
            else      -> 24f
        }

        onGround = false   // reset; collision sets it back
        bounds.set(x, y, W, H)
    }

    fun jump(): Boolean {
        if (!alive || jumpCount >= MAX_JUMPS) return false
        velY = JUMP_VEL
        jumpCount++
        totalJumps++
        onGround = false
        return true
    }

    fun landOn(surfaceY: Float) {
        if (velY <= 0f) {
            y = surfaceY; velY = 0f; onGround = true; jumpCount = 0
            bounds.set(x, y, W, H)
        }
    }

    val deathAlpha get() = if (!alive) (1f - deathTimer * 1.8f).coerceAtLeast(0f) else 1f

    // ── Draw ──────────────────────────────────────────────────────────────

    fun draw(sr: ShapeRenderer) {
        val a = deathAlpha
        if (a <= 0f) return

        val cx   = x + W / 2f
        val cy   = y
        val tB   = cy + H * 0.38f
        val tT   = cy + H * 0.70f
        val hCy  = cy + H * 0.84f
        val hR   = H  * 0.13f
        val legL = 32f
        val armL = 26f
        val la   = legAngle
        val aa   = -la * 0.38f

        val bc = Color(bodyColor.r, bodyColor.g, bodyColor.b, a)
        val sc = Color(0.88f, 0.72f, 0.55f, a)  // skin
        val gc = Color(shoeColor.r, shoeColor.g, shoeColor.b, a)

        // ── Legs ──────────────────────────────────────────────────────
        sr.color = bc
        val lx1 = cx + MathUtils.cosDeg(254f + la) * legL
        val ly1 = tB + MathUtils.sinDeg(254f + la) * legL
        val lx2 = cx + MathUtils.cosDeg(286f - la) * legL
        val ly2 = tB + MathUtils.sinDeg(286f - la) * legL
        sr.rectLine(cx, tB, lx1, ly1, 6f)
        sr.rectLine(cx, tB, lx2, ly2, 6f)

        // Green shoes
        sr.color = gc
        sr.ellipse(lx1 - 8f, ly1 - 5f, 18f, 10f, 8)
        sr.ellipse(lx2 - 8f, ly2 - 5f, 18f, 10f, 8)

        // ── Torso ─────────────────────────────────────────────────────
        sr.color = bc
        sr.rectLine(cx, tB, cx, tT, 7f)

        // ── Arms ──────────────────────────────────────────────────────
        val aY = tT - (tT - tB) * 0.15f
        val ax1 = cx + MathUtils.cosDeg(205f + aa) * armL
        val ay1 = aY + MathUtils.sinDeg(205f + aa) * armL
        val ax2 = cx + MathUtils.cosDeg(-25f + aa) * armL
        val ay2 = aY + MathUtils.sinDeg(-25f + aa) * armL
        sr.rectLine(cx, aY, ax1, ay1, 5f)
        sr.rectLine(cx, aY, ax2, ay2, 5f)

        // Briefcase on one hand
        sr.color = Color(0.55f, 0.35f, 0.10f, a)
        sr.rect(ax2 - 8f, ay2 - 6f, 14f, 10f)
        sr.color = Color(0.40f, 0.25f, 0.06f, a)
        sr.rect(ax2 - 5f, ay2 - 4f, 8f, 2f)  // handle

        // ── Head ──────────────────────────────────────────────────────
        sr.color = sc
        sr.circle(cx, hCy, hR, 14)

        // Eye
        sr.color = bc
        sr.circle(cx + hR * 0.35f, hCy + hR * 0.06f, 2.5f, 6)

        // ── Hat ───────────────────────────────────────────────────────
        val hc = Color(hatColor.r, hatColor.g, hatColor.b, a)
        sr.color = hc
        when (hatType) {
            HatType.TOP_HAT -> {
                sr.rect(cx - hR*0.95f, hCy + hR*0.52f, hR*1.90f, hR*0.26f)  // brim
                sr.rect(cx - hR*0.55f, hCy + hR*0.72f, hR*1.10f, hR*1.30f)  // crown
            }
            HatType.CAP -> {
                sr.rect(cx - hR*0.30f, hCy + hR*0.50f, hR*1.65f, hR*0.24f)
                sr.rect(cx - hR*0.72f, hCy + hR*0.68f, hR*1.44f, hR*0.82f)
            }
            HatType.NONE -> Unit
        }
    }
}
