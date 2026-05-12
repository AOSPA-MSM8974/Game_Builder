package com.stickrun.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.stickrun.game.Assets
import com.stickrun.game.StickRunGame
import com.stickrun.game.entities.*
import com.stickrun.game.world.WorldGenerator

class GameScreen(private val game: StickRunGame) : Screen {

    companion object {
        const val VW = 1920f
        const val VH = 1080f
        // Ground strip height — matches the grey diagonal strip in the video
        const val GROUND_STRIP_H = 96f
    }

    private val worldCam = OrthographicCamera(VW, VH)
    private val uiCam    = OrthographicCamera(VW, VH)

    val player   = Player(240f, Player.GROUND_Y)
    private val gen   = WorldGenerator()

    private val platforms    = mutableListOf<Platform>()
    private val groundCrates = mutableListOf<Crate>()
    private val coins        = mutableListOf<Coin>()

    private var score      = 0
    private var xp         = 0
    private var combo      = 0
    private var comboTimer = 0f
    private var jumps      = 0
    private var distance   = 0f
    private var speed      = 480f
    private var difficulty = 0f

    private var gameOver  = false
    private var goTimer   = 0f
    private var nextChunk = 0f

    // Floating popup texts
    private data class Popup(var wx: Float, var wy: Float, var life: Float, val text: String, val color: Color)
    private val popups = mutableListOf<Popup>()

    private val hitRect = Rectangle()
    private val font    = BitmapFont()
    private val layout  = GlyphLayout()

    fun applyCustomization(bodyColor: Color, hatType: Player.HatType, hatColor: Color, shoeColor: Color) {
        player.bodyColor = bodyColor; player.hatType = hatType
        player.hatColor  = hatColor; player.shoeColor = shoeColor
    }

    init {
        worldCam.position.set(VW / 2f, VH / 2f, 0f); worldCam.update()
        uiCam.position.set(VW / 2f, VH / 2f, 0f);    uiCam.update()
        font.data.setScale(2f)
        spawnChunk(200f)
        spawnChunk(nextChunk)
    }

    private fun spawnChunk(startX: Float) {
        val plats  = gen.generateChunk(startX, difficulty)
        val gcrats = gen.generateGroundCrates(startX, difficulty)
        val cns    = gen.generateCoins(plats, gcrats)
        platforms.addAll(plats)
        groundCrates.addAll(gcrats)
        coins.addAll(cns)
        nextChunk = startX + 5000f
    }

    // ── Input ─────────────────────────────────────────────────────────────

    private fun handleInput() {
        val doJump = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                     Gdx.input.isKeyJustPressed(Input.Keys.UP)    ||
                     Gdx.input.isKeyJustPressed(Input.Keys.W)     ||
                     Gdx.input.justTouched()
        if (doJump && player.jump()) jumps++
    }

    // ── Update ────────────────────────────────────────────────────────────

    private fun update(dt: Float) {
        speed      = (speed + 14f * dt).coerceAtMost(1100f)
        difficulty = ((player.x - 240f) / 28000f).coerceIn(0f, 1f)

        player.update(dt, speed)
        distance = (player.x - 240f).coerceAtLeast(0f)

        // ── Combo timer ───────────────────────────────────────────────
        comboTimer -= dt
        if (comboTimer < 0f) { comboTimer = 0f; combo = 0 }

        // ── Platform collision: land on top ───────────────────────────
        for (p in platforms) {
            p.update(dt)
            val pb = p.bounds
            // Player feet just above platform top
            if (player.bounds.overlaps(pb) && player.velY <= 0f &&
                player.y >= pb.y + pb.height - 12f) {
                player.landOn(p.surfaceY)
                // XP for box jump
                gainXP(2, "BOX JUMP\n+2 XP", player.x, player.y + Player.H + 20f)
            }
            // Crate collision — kill
            for (c in p.crates) {
                hitRect.set(player.bounds.x + 5f, player.bounds.y + 5f,
                    player.bounds.width - 10f, player.bounds.height - 8f)
                if (c.bounds.overlaps(hitRect)) { triggerDeath(); return }
            }
            // Saw collision — kill
            p.saw?.let { saw ->
                hitRect.set(player.bounds.x + 5f, player.bounds.y + 5f,
                    player.bounds.width - 10f, player.bounds.height - 8f)
                if (saw.bounds.overlaps(hitRect)) { triggerDeath(); return }
            }
        }

        // ── Ground crate collision — kill ─────────────────────────────
        for (c in groundCrates) {
            hitRect.set(player.bounds.x + 5f, player.bounds.y + 5f,
                player.bounds.width - 10f, player.bounds.height - 8f)
            if (c.bounds.overlaps(hitRect)) { triggerDeath(); return }
        }

        // ── Coin collection ───────────────────────────────────────────
        val pcx = player.bounds.x + player.bounds.width  / 2f
        val pcy = player.bounds.y + player.bounds.height / 2f
        val ci  = coins.iterator()
        while (ci.hasNext()) {
            val c = ci.next()
            c.update(dt)
            if (!c.collected) {
                val dx = pcx - c.x; val dy = pcy - c.y
                if (dx*dx + dy*dy < (player.bounds.width*0.5f + Coin.R) * (player.bounds.width*0.5f + Coin.R)) {
                    c.collected = true
                    score += 5; xp += 1
                    combo++; comboTimer = 3f
                    val comboStr = if (combo > 1) "Combo ${combo}x\n+ ${combo*2} XP" else "+5"
                    gainXP(combo * 2, comboStr, c.x, c.y + 30f)
                }
            }
            if (c.done) ci.remove()
        }

        // ── Popups ────────────────────────────────────────────────────
        popups.forEach { it.wy += dt * 80f; it.life -= dt * 1.0f }
        popups.removeAll { it.life <= 0f }

        // ── Camera ───────────────────────────────────────────────────
        val targetX = player.x + VW * 0.20f
        worldCam.position.x = MathUtils.lerp(worldCam.position.x, targetX, 0.13f)
        worldCam.position.y = VH / 2f
        worldCam.update()

        // ── Spawn + cull ──────────────────────────────────────────────
        if (player.x + 3000f > nextChunk) spawnChunk(nextChunk)
        val cullX = worldCam.position.x - VW
        platforms.removeAll    { it.x + it.width < cullX }
        groundCrates.removeAll { it.x + Crate.SIZE < cullX }
        coins.removeAll        { it.done || it.x < cullX }

        score = (distance / 6f).toInt() + xp * 10
    }

    private fun gainXP(amount: Int, text: String, wx: Float, wy: Float) {
        xp += amount
        popups.add(Popup(wx, wy, 1.4f, text, Color(1f, 0.22f, 0.22f, 1f)))
    }

    private fun triggerDeath() {
        player.alive = false
        gameOver     = true
    }

    // ── Render ────────────────────────────────────────────────────────────

    override fun render(delta: Float) {
        val dt = delta.coerceIn(0.001f, 0.032f)
        handleInput()
        if (!gameOver) update(dt)
        else {
            goTimer += dt
            if (goTimer > 1.5f && Gdx.input.justTouched()) {
                game.setScreen(GameScreen(game)); return
            }
        }
        draw()
    }

    // ── Draw ──────────────────────────────────────────────────────────────

    private fun draw() {
        // Clear to orange (the sky IS the background)
        Gdx.gl.glClearColor(0.96f, 0.62f, 0.10f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val camL = worldCam.position.x - VW / 2f
        val sr   = game.shape
        val bat  = game.batch

        // 1. World shapes: sky gradient + ground strip
        sr.projectionMatrix = worldCam.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        drawSky(camL)
        sr.end()

        // 2. World batch: ground tile, platforms, crates, coins
        bat.projectionMatrix = worldCam.combined
        bat.begin()
        drawGroundTile(bat, camL)
        platforms.forEach    { if (it.x < camL + VW + 100f && it.x + it.width > camL - 100f) it.draw(bat) }
        groundCrates.forEach { if (it.x < camL + VW + 100f && it.x + Crate.SIZE > camL - 100f) it.draw(bat) }
        coins.forEach        { if (it.x < camL + VW + 60f  && it.x > camL - 60f) it.draw(bat) }
        bat.end()

        // 3. Player (shape-drawn on top)
        sr.projectionMatrix = worldCam.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        player.draw(sr)
        sr.end()

        // 4. Popups (world space)
        bat.projectionMatrix = worldCam.combined
        bat.begin()
        font.data.setScale(1.9f)
        for (p in popups) {
            font.color = Color(p.color.r, p.color.g, p.color.b, p.life.coerceIn(0f,1f))
            font.draw(bat, p.text, p.wx, p.wy)
        }
        bat.end()

        // 5. UI (HUD bar + game over)
        sr.projectionMatrix = uiCam.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        drawHUDBg()
        if (gameOver) drawGameOverBg()
        sr.end()

        bat.projectionMatrix = uiCam.combined
        bat.begin()
        drawHUDText()
        if (gameOver) drawGameOverText()
        bat.end()
    }

    // ── Sky gradient ──────────────────────────────────────────────────────

    private fun drawSky(camL: Float) {
        val sr = game.shape; val w = VW + 4f
        // Lighter at top, darker orange at bottom — matches the video exactly
        sr.color = Color(0.99f, 0.78f, 0.22f, 1f); sr.rect(camL-2f, VH*0.65f, w, VH*0.35f)
        sr.color = Color(0.98f, 0.68f, 0.12f, 1f); sr.rect(camL-2f, VH*0.35f, w, VH*0.30f)
        sr.color = Color(0.96f, 0.58f, 0.08f, 1f); sr.rect(camL-2f, GROUND_STRIP_H, w, VH*0.35f)
    }

    // ── Ground tile ───────────────────────────────────────────────────────

    private fun drawGroundTile(bat: com.badlogic.gdx.graphics.g2d.SpriteBatch, camL: Float) {
        val tileW = 64f
        val tileH = GROUND_STRIP_H
        var tx    = camL - (camL % tileW)
        while (tx < camL + VW + tileW) {
            bat.draw(Assets.ground, tx, 0f, tileW, tileH,
                0, 0, Assets.ground.width, Assets.ground.height, false, false)
            tx += tileW
        }
    }

    // ── HUD — white bar at top matching the video ─────────────────────────

    private fun drawHUDBg() {
        val sr = game.shape
        sr.color = Color(1f, 1f, 1f, 0.92f)
        sr.rect(0f, VH - 80f, VW, 80f)
        sr.color = Color(0.82f, 0.82f, 0.82f, 1f)
        sr.rect(0f, VH - 83f, VW, 3f)
    }

    private fun drawHUDText() {
        font.data.setScale(2.4f)
        font.color = Color(0.1f, 0.1f, 0.1f, 1f)
        font.draw(game.batch, "Score: $score", 36f, VH - 24f)

        font.color = Color(1f, 0.55f, 0.05f, 1f)
        font.draw(game.batch, "XP: $xp", VW * 0.38f, VH - 24f)

        font.color = Color(0.1f, 0.1f, 0.1f, 1f)
        font.draw(game.batch, "Rank: 0", VW * 0.62f, VH - 24f)

        font.data.setScale(1.6f)
        font.color = Color(0.5f, 0.5f, 0.5f, 1f)
        font.draw(game.batch, "Press P to pause", VW - 440f, VH - 30f)
    }

    // ── Game Over ("YOU FAILED") ──────────────────────────────────────────

    private fun drawGameOverBg() {
        val sr = game.shape
        val a  = (goTimer * 2.5f).coerceAtMost(0.85f)
        sr.color = Color(0f, 0f, 0f, a * 0.35f)
        sr.rect(0f, 0f, VW, VH)
        if (goTimer > 0.3f) {
            val pa = ((goTimer - 0.3f) * 3f).coerceAtMost(1f)
            // White panel like in the video
            sr.color = Color(1f, 1f, 1f, pa * 0.95f)
            sr.rect(VW * 0.25f, VH * 0.20f, VW * 0.50f, VH * 0.58f)
            sr.color = Color(0.82f, 0.82f, 0.82f, pa)
            sr.rect(VW * 0.25f, VH * 0.76f, VW * 0.50f, 3f)
            sr.rect(VW * 0.25f, VH * 0.20f, VW * 0.50f, 3f)
            // Play Again button (green like the video)
            if (goTimer > 0.8f) {
                val ba = ((goTimer - 0.8f) * 2.5f).coerceAtMost(1f)
                sr.color = Color(0.18f, 0.68f, 0.18f, ba)
                sr.rect(VW * 0.53f, VH * 0.24f, VW * 0.18f, VH * 0.10f)
                sr.color = Color(0.12f, 0.50f, 0.12f, ba)
                sr.rect(VW * 0.53f, VH * 0.24f, VW * 0.18f, VH * 0.025f)
            }
        }
    }

    private fun drawGameOverText() {
        if (goTimer < 0.3f) return
        val a = ((goTimer - 0.3f) * 3f).coerceAtMost(1f)

        // "YOU FAILED" — red with dark outline, matches the video font style
        font.data.setScale(7.5f)
        font.color = Color(0.12f, 0.08f, 0.06f, a)
        layout.setText(font, "YOU FAILED")
        val tx = VW / 2f - layout.width / 2f
        font.draw(game.batch, "YOU FAILED", tx + 4f, VH * 0.74f - 4f) // shadow
        font.color = Color(0.90f, 0.08f, 0.08f, a)
        font.draw(game.batch, "YOU FAILED", tx, VH * 0.74f)

        // Stats
        font.data.setScale(2.6f)
        font.color = Color(0.12f, 0.12f, 0.12f, a)
        font.draw(game.batch, "Score:", VW * 0.35f, VH * 0.60f)
        font.draw(game.batch, "Jumps:", VW * 0.35f, VH * 0.52f)
        font.draw(game.batch, "XP:",    VW * 0.35f, VH * 0.44f)

        font.color = Color(0.10f, 0.10f, 0.10f, a)
        font.draw(game.batch, "$score",           VW * 0.58f, VH * 0.60f)
        font.draw(game.batch, "${player.totalJumps}", VW * 0.58f, VH * 0.52f)
        font.draw(game.batch, "$xp",              VW * 0.58f, VH * 0.44f)

        // Play Again button label
        if (goTimer > 0.8f) {
            val ba = ((goTimer - 0.8f) * 2.5f).coerceAtMost(1f)
            font.data.setScale(2.0f)
            font.color = Color(1f, 1f, 1f, ba)
            layout.setText(font, "Play Again")
            font.draw(game.batch, "Play Again",
                VW * 0.53f + VW * 0.18f / 2f - layout.width / 2f,
                VH * 0.24f + VH * 0.10f * 0.65f)
        }

        // Tap hint
        if (goTimer > 1.5f) {
            font.data.setScale(1.8f)
            font.color = Color(0.5f, 0.5f, 0.5f, MathUtils.sin(goTimer * 4f) * 0.45f + 0.55f)
            layout.setText(font, "Tap to play again")
            font.draw(game.batch, "Tap to play again", VW/2f - layout.width/2f, VH * 0.28f)
        }
    }

    override fun resize(width: Int, height: Int) {
        worldCam.setToOrtho(false, VW, VH); worldCam.position.set(VW/2f, VH/2f, 0f); worldCam.update()
        uiCam.setToOrtho(false, VW, VH);    uiCam.position.set(VW/2f, VH/2f, 0f);    uiCam.update()
    }

    override fun show()    {}
    override fun pause()   {}
    override fun resume()  {}
    override fun hide()    {}
    override fun dispose() { font.dispose() }
}
