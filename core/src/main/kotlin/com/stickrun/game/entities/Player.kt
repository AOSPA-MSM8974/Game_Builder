package com.stickrun.game.entities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

class Player(startX: Float, startY: Float) {

    val position = Vector2(startX, startY)
    val velocity = Vector2(0f, 0f)
    val bounds = Rectangle(startX, startY, WIDTH, HEIGHT)

    var isOnGround = false
    var isRunning = false
    var facingRight = true
    var animTimer = 0f
    var jumpCount = 0

    // Customization
    var bodyColor = Color(0.15f, 0.15f, 0.15f, 1f)
    var hatType = HatType.NONE
    var hatColor = Color(0.8f, 0.2f, 0.2f, 1f)

    // Run animation leg angle
    private var legAngle = 0f

    companion object {
        const val WIDTH = 24f
        const val HEIGHT = 48f
        const val MOVE_SPEED = 220f
        const val JUMP_FORCE = 480f
        const val GRAVITY = -900f
        const val MAX_FALL_SPEED = -700f
        const val MAX_JUMP_COUNT = 2
    }

    enum class HatType { NONE, CAP, TOP_HAT, BEANIE }

    fun update(delta: Float) {
        animTimer += delta

        // Horizontal movement
        isRunning = false
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            velocity.x = -MOVE_SPEED
            facingRight = false
            isRunning = true
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            velocity.x = MOVE_SPEED
            facingRight = true
            isRunning = true
        } else {
            velocity.x *= 0.7f
            if (Math.abs(velocity.x) < 5f) velocity.x = 0f
        }

        // Gravity
        velocity.y += GRAVITY * delta
        velocity.y = velocity.y.coerceAtLeast(MAX_FALL_SPEED)

        // Update position
        position.x += velocity.x * delta
        position.y += velocity.y * delta

        // Keep in world bounds
        if (position.x < 0f) position.x = 0f

        // Update bounds
        bounds.setPosition(position.x, position.y)

        // Animate legs
        if (isRunning && isOnGround) {
            legAngle = Math.sin((animTimer * 12.0)).toFloat() * 35f
        } else if (!isOnGround) {
            legAngle = if (velocity.y > 0) -30f else 20f
        } else {
            legAngle *= 0.8f
        }

        isOnGround = false // reset, collision will set it
    }

    fun jump() {
        if (jumpCount < MAX_JUMP_COUNT) {
            velocity.y = JUMP_FORCE
            jumpCount++
            isOnGround = false
        }
    }

    fun land() {
        if (velocity.y < 0) {
            velocity.y = 0f
            isOnGround = true
            jumpCount = 0
        }
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        val cx = position.x + WIDTH / 2f
        val cy = position.y

        shapeRenderer.color = bodyColor

        // Body (torso)
        val torsoBot = cy + HEIGHT * 0.42f
        val torsoTop = cy + HEIGHT * 0.75f
        shapeRenderer.rectLine(cx, torsoBot, cx, torsoTop, 4f)

        // Head
        val headCy = cy + HEIGHT * 0.87f
        val headR = HEIGHT * 0.13f
        shapeRenderer.color = Color(0.92f, 0.78f, 0.65f, 1f)
        shapeRenderer.circle(cx, headCy, headR, 16)

        // Eyes
        shapeRenderer.color = bodyColor
        val eyeOffX = if (facingRight) headR * 0.35f else -headR * 0.35f
        shapeRenderer.circle(cx + eyeOffX, headCy + headR * 0.1f, 2f, 8)

        // Arms
        shapeRenderer.color = bodyColor
        val armY = torsoTop - (torsoTop - torsoBot) * 0.2f
        val armAngle = if (isRunning) -legAngle * 0.5f else 0f
        // Left arm
        val lax = cx + Math.cos(Math.toRadians((200.0 + armAngle))).toFloat() * 18f
        val lay = armY + Math.sin(Math.toRadians((200.0 + armAngle))).toFloat() * 18f
        shapeRenderer.rectLine(cx, armY, lax, lay, 3f)
        // Right arm
        val rax = cx + Math.cos(Math.toRadians((-20.0 + armAngle))).toFloat() * 18f
        val ray = armY + Math.sin(Math.toRadians((-20.0 + armAngle))).toFloat() * 18f
        shapeRenderer.rectLine(cx, armY, rax, ray, 3f)

        // Legs
        val legTop = torsoBot
        // Left leg
        val llx = cx + Math.cos(Math.toRadians((240.0 + legAngle))).toFloat() * 22f
        val lly = legTop + Math.sin(Math.toRadians((240.0 + legAngle))).toFloat() * 22f
        shapeRenderer.rectLine(cx, legTop, llx, lly, 3.5f)
        // Right leg
        val rlx = cx + Math.cos(Math.toRadians((300.0 - legAngle))).toFloat() * 22f
        val rly = legTop + Math.sin(Math.toRadians((300.0 - legAngle))).toFloat() * 22f
        shapeRenderer.rectLine(cx, legTop, rlx, rly, 3.5f)

        // Hat
        drawHat(shapeRenderer, cx, headCy, headR)
    }

    private fun drawHat(sr: ShapeRenderer, cx: Float, headCy: Float, headR: Float) {
        sr.color = hatColor
        when (hatType) {
            HatType.CAP -> {
                // Brim
                val brimX = if (facingRight) cx - headR * 0.3f else cx - headR * 1.0f
                sr.rect(brimX, headCy + headR * 0.5f, headR * 1.8f, headR * 0.3f)
                // Cap
                sr.rect(cx - headR * 0.8f, headCy + headR * 0.7f, headR * 1.6f, headR * 0.9f)
            }
            HatType.TOP_HAT -> {
                sr.rect(cx - headR * 0.9f, headCy + headR * 0.6f, headR * 1.8f, headR * 0.3f)
                sr.rect(cx - headR * 0.6f, headCy + headR * 0.8f, headR * 1.2f, headR * 1.4f)
            }
            HatType.BEANIE -> {
                sr.color = hatColor
                sr.circle(cx, headCy + headR * 0.4f, headR * 1.0f, 16)
                sr.color = hatColor.cpy().mul(0.8f)
                sr.rect(cx - headR, headCy + headR * 0.2f, headR * 2f, headR * 0.5f)
            }
            HatType.NONE -> {}
        }
    }
}
