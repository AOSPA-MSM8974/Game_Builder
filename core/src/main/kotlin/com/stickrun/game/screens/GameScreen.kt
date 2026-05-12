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
import com.stickrun.game.StickRunGame
import com.stickrun.game.entities.Coin
import com.stickrun.game.entities.Obstacle
import com.stickrun.game.entities.Player
import com.stickrun.game.world.WorldGenerator

class GameScreen(private val game: StickRunGame) : Screen {

    // ── Virtual resolution ─────────────────────────────────────────────────
    companion object {
        const val VW = 1920f
        const val VH = 1080f
    }

    private val worldCam = OrthographicCamera(VW, VH)
    private val uiCam    = OrthographicCamera(VW, VH)

    // ── Entities ───────────────────────────────────────────────────────────
    val player     = Player(200f, Player.GROUND_Y)
    private val gen      = WorldGenerator()
    private val obstacles = mutableListOf<Obstacle>()
    private val coins     = mutableListOf<Coin>()

    // ── State ──────────────────────────────────────────────────────────────
    private var score       = 0
    private var coinCount   = 0
    private var distance    = 0f
    private var runSpeed    = 480f          // pixels/sec, increases over time
    private var difficulty  = 0f
    private var gameOver    = false
    private var goTimer     = 0f            // game-over animation timer
    private var nextChunkX  = 0f

    // Floating texts
    private data class FloatText(var x: Float, var y: Float, var life: Float, val msg: String)
    private val floatTexts = mutableListOf<FloatText>()

    // Touch zones (screen coords, updated in handleInput)
    private val jumpZone  = Rectangle()
    private val slideZone = Rectangle()

    // Background parallax layers
    private data class BgRect(val wx: Float, val wy: Float, val w: Float, val h: Float)
    private val bgBuildings = ArrayList<BgRect>(40)
    private val bgTrees     = ArrayList<BgRect>(50)
    private val bgClouds    = ArrayList<BgRect>(30)

    // Reusable collision rect (avoids allocation per frame)
    private val colRect = Rectangle()

    private val font   = BitmapFont()
    private val layout = GlyphLayout()

    fun applyCustomization(bodyColor: Color, hatType: Player.HatType, hatColor: Color) {
        player.bodyColor = bodyColor
        player.hatType   = hatType
        player.hatColor  = hatColor
    }

    init {
        worldCam.position.set(VW / 2f, VH / 2f, 0f)
        uiCam.position.set(VW / 2f, VH / 2f, 0f)
        worldCam.update(); uiCam.update()

        font.data.setScale(2.2f)

        // Seed background
        for (i in 0..50) {
            bgBuildings.add(BgRect(i * 380f + MathUtils.random(0f, 190f), Player.GROUND_Y,
                MathUtils.random(55f, 130f), MathUtils.random(80f, 230f)))
            bgTrees.add(BgRect(i * 260f + MathUtils.random(0f, 130f), Player.GROUND_Y,
                22f, MathUtils.random(55f, 100f)))
            if (i < 30) bgClouds.add(BgRect(i * 440f + MathUtils.random(0f, 220f),
                MathUtils.random(680f, 920f), MathUtils.random(60f, 120f), 0f))
        }

        spawnChunk(300f)
        spawnChunk(nextChunkX)
    }

    // ── Chunk Spawning ─────────────────────────────────────────────────────

    private fun spawnChunk(startX: Float) {
        val (obs, cns) = gen.generateChunk(startX, difficulty)
        obstacles.addAll(obs)
        coins.addAll(cns)
        nextChunkX = startX + 6000f
    }

    // ── Render ─────────────────────────────────────────────────────────────

    override fun render(delta: Float) {
        // Cap delta to avoid physics explosion on lag spikes
        val dt = delta.coerceIn(0f, 0.033f)

        handleInput(dt)
        if (!gameOver) update(dt) else goTimer += dt

        // Return to menu after game over
        if (gameOver && goTimer > 1.5f &&
            (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY))) {
            game.setScreen(MenuScreen(game))
            return
        }

        draw()
    }

    // ── Input ──────────────────────────────────────────────────────────────

    private fun handleInput(dt: Float) {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()

        // Left 40% = slide, right 60% = jump
        slideZone.set(0f, 0f, sw * 0.40f, sh)
        jumpZone.set(sw * 0.40f, 0f, sw * 0.60f, sh)

        // Keyboard
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)    ||
            Gdx.input.isKeyJustPressed(Input.Keys.W)     ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) player.jump()

        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)  ||
            Gdx.input.isKeyJustPressed(Input.Keys.S))     player.slide()

        // Touch
        if (Gdx.input.justTouched()) {
            val tx = Gdx.input.x.toFloat()
            val ty = sh - Gdx.input.y.toFloat()
            if (jumpZone.contains(tx, ty))  player.jump()
            if (slideZone.contains(tx, ty)) player.slide()
        }
    }

    // ── Update ─────────────────────────────────────────────────────────────

    private fun update(dt: Float) {
        // Gradually increase speed (caps at 1100)
        runSpeed = (runSpeed + 12f * dt).coerceAtMost(1100f)
        difficulty = ((player.x - 200f) / 28000f).coerceIn(0f, 1f)

        player.update(dt, runSpeed)
        distance = player.x - 200f

        // ── Obstacle collision ────────────────────────────────────────────
        // Use a slightly inset rect for fairness
        colRect.set(
            player.bounds.x + 6f,
            player.bounds.y + 4f,
            player.bounds.width  - 12f,
            player.bounds.height - 6f
        )
        for (obs in obstacles) {
            obs.update(dt)
            if (obs.bounds.overlaps(colRect)) {
                gameOver = true
                player.alive = false
                return
            }
        }

        // ── Coin collection ───────────────────────────────────────────────
        val pcx = player.bounds.x + player.bounds.width  / 2f
        val pcy = player.bounds.y + player.bounds.height / 2f
        val collectR = player.bounds.width * 0.6f + Coin.R

        val ci = coins.iterator()
        while (ci.hasNext()) {
            val c = ci.next()
            c.update(dt)
            if (!c.collected) {
                val dx = pcx - c.cx
                val dy = pcy - c.cy
                if (dx * dx + dy * dy < collectR * collectR) {
                    c.collected = true
                    coinCount++
                    floatTexts.add(FloatText(c.cx, c.cy + 20f, 1.0f, "+10"))
                }
            }
            if (c.done) ci.remove()
        }

        // ── Float texts ───────────────────────────────────────────────────
        val fi = floatTexts.iterator()
        while (fi.hasNext()) {
            val ft = fi.next()
            ft.y    += dt * 90f
            ft.life -= dt * 1.4f
            if (ft.life <= 0f) fi.remove()
        }

        // ── Camera follow ─────────────────────────────────────────────────
        val targetX = player.x + VW * 0.25f
        worldCam.position.x = MathUtils.lerp(worldCam.position.x, targetX, 0.14f)
        worldCam.position.y = VH / 2f
        worldCam.update()

        // ── Chunk spawning ────────────────────────────────────────────────
        if (player.x + 3000f > nextChunkX) spawnChunk(nextChunkX)

        // ── Cull off-screen objects ───────────────────────────────────────
        val cullX = worldCam.position.x - VW
        obstacles.removeAll { it.bounds.x + it.bounds.width < cullX }
        coins.removeAll     { it.done || it.cx < cullX }
        floatTexts.removeAll { it.life <= 0f }

        score = (distance / 6f).toInt() + coinCount * 10
    }

    // ── Draw ───────────────────────────────────────────────────────────────

    private fun draw() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val sr     = game.shape
        val bat    = game.batch
        val camL   = worldCam.position.x - VW / 2f

        // ── World ──────────────────────────────────────────────────────────
        sr.projectionMatrix = worldCam.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)

        drawSky(sr, camL)
        drawParallax(sr, camL)
        drawGround(sr, camL)

        for (obs in obstacles) {
            if (obs.bounds.x > camL + VW + 100f || obs.bounds.x + obs.bounds.width < camL - 100f) continue
            obs.draw(sr)
        }
        for (coin in coins) {
            if (coin.cx > camL + VW + 60f || coin.cx < camL - 60f) continue
            coin.draw(sr)
        }

        player.draw(sr)

        sr.end()

        // ── Floating texts (world space) ───────────────────────────────────
        bat.projectionMatrix = worldCam.combined
        bat.begin()
        font.data.setScale(1.9f)
        for (ft in floatTexts) {
            font.color = Color(1f, 0.95f, 0.25f, ft.life.coerceIn(0f, 1f))
            font.draw(bat, ft.msg, ft.x, ft.y)
        }
        bat.end()

        // ── UI ─────────────────────────────────────────────────────────────
        sr.projectionMatrix = uiCam.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        drawHUDBackground(sr)
        drawControls(sr)
        if (gameOver) drawGameOverOverlay(sr)
        sr.end()

        bat.projectionMatrix = uiCam.combined
        bat.begin()
        drawHUDText(bat)
        if (gameOver) drawGameOverText(bat)
        bat.end()
    }

    // ── Sky ────────────────────────────────────────────────────────────────

    private fun drawSky(sr: ShapeRenderer, camL: Float) {
        val w = VW + 4f

        // Bottom band — deep burnt orange
        sr.color = Color(0.94f, 0.40f, 0.03f, 1f)
        sr.rect(camL - 2f, 0f, w, VH * 0.32f)

        // Mid — warm orange
        sr.color = Color(0.96f, 0.54f, 0.08f, 1f)
        sr.rect(camL - 2f, VH * 0.32f, w, VH * 0.34f)

        // Upper — golden amber
        sr.color = Color(0.90f, 0.66f, 0.18f, 1f)
        sr.rect(camL - 2f, VH * 0.66f, w, VH * 0.20f)

        // Top — pale yellow horizon
        sr.color = Color(0.97f, 0.82f, 0.38f, 1f)
        sr.rect(camL - 2f, VH * 0.86f, w, VH * 0.14f)

        // Sun — fixed at top-right of viewport
        val sx = camL + VW * 0.83f
        val sy = VH  * 0.88f
        sr.color = Color(1f, 0.88f, 0.28f, 0.09f); sr.circle(sx, sy, 130f, 24)
        sr.color = Color(1f, 0.90f, 0.32f, 0.16f); sr.circle(sx, sy, 100f, 24)
        sr.color = Color(1f, 0.92f, 0.40f, 0.88f); sr.circle(sx, sy,  68f, 24)
        sr.color = Color(1f, 1.00f, 0.72f, 0.84f); sr.circle(sx, sy,  40f, 18)
    }

    // ── Parallax ───────────────────────────────────────────────────────────

    private fun drawParallax(sr: ShapeRenderer, camL: Float) {
        val wrap = VW + 1200f

        // Far buildings (0.08x scroll)
        for (b in bgBuildings) {
            val bx = camL + ((b.wx - camL * 0.08f + 999999f) % wrap)
            sr.color = Color(0.62f, 0.25f, 0.04f, 0.18f)
            sr.rect(bx, Player.GROUND_Y, b.w, b.h)
            sr.color = Color(0.70f, 0.30f, 0.05f, 0.10f)
            sr.rect(bx, Player.GROUND_Y + b.h - 6f, b.w, 6f) // roof
        }

        // Trees (0.28x scroll)
        for (t in bgTrees) {
            val tx = camL + ((t.wx - camL * 0.28f + 999999f) % wrap)
            sr.color = Color(0.38f, 0.14f, 0.02f, 0.55f)
            sr.rect(tx + 6f, Player.GROUND_Y, 10f, t.h * 0.65f)
            sr.color = Color(0.28f, 0.10f, 0.02f, 0.65f)
            sr.circle(tx + 11f, Player.GROUND_Y + t.h * 0.65f + 26f, 32f, 12)
        }

        // Clouds (0.12x scroll)
        for (c in bgClouds) {
            val cx = camL + ((c.wx - camL * 0.12f + 999999f) % wrap)
            sr.color = Color(1f, 0.74f, 0.32f, 0.32f)
            sr.circle(cx,        c.wy,       c.w,        12)
            sr.circle(cx + c.w * 0.9f, c.wy - 10f, c.w * 0.70f, 10)
            sr.circle(cx - c.w * 0.7f, c.wy - 12f, c.w * 0.58f, 10)
        }
    }

    // ── Ground ─────────────────────────────────────────────────────────────

    private fun drawGround(sr: ShapeRenderer, camL: Float) {
        val w = VW + 4f

        // Deep soil
        sr.color = Color(0.18f, 0.07f, 0.01f, 1f)
        sr.rect(camL - 2f, 0f, w, Player.GROUND_Y)

        // Surface band
        sr.color = Color(0.34f, 0.14f, 0.03f, 1f)
        sr.rect(camL - 2f, Player.GROUND_Y - 8f, w, 14f)

        // Top crust highlight
        sr.color = Color(0.50f, 0.22f, 0.06f, 1f)
        sr.rect(camL - 2f, Player.GROUND_Y + 4f, w, 5f)

        // Subtle ground grid lines every 128px
        sr.color = Color(0.28f, 0.11f, 0.02f, 0.35f)
        val startX = camL - (camL % 128f)
        var gx = startX
        while (gx < camL + VW + 128f) {
            sr.rect(gx, Player.GROUND_Y - 8f, 2f, 12f)
            gx += 128f
        }
    }

    // ── HUD ────────────────────────────────────────────────────────────────

    private fun drawHUDBackground(sr: ShapeRenderer) {
        // Top bar
        sr.color = Color(0f, 0f, 0f, 0.42f)
        sr.rect(0f, VH - 88f, VW, 88f)
        // Accent line
        sr.color = Color(1f, 0.80f, 0.20f, 0.60f)
        sr.rect(0f, VH - 90f, VW, 3f)
    }

    private fun drawControls(sr: ShapeRenderer) {
        // Slide button — bottom left
        sr.color = Color(1f, 1f, 1f, 0.10f)
        sr.rect(0f, 0f, VW * 0.40f, VH * 0.22f)
        // Jump button — bottom right
        sr.color = Color(1f, 0.85f, 0.10f, 0.10f)
        sr.rect(VW * 0.40f, 0f, VW * 0.60f, VH * 0.22f)

        // Slide icon — down arrow
        sr.color = Color(1f, 1f, 1f, 0.32f)
        sr.triangle(
            VW * 0.10f - 28f, VH * 0.14f,
            VW * 0.10f + 28f, VH * 0.14f,
            VW * 0.10f,       VH * 0.04f
        )
        // Jump icon — up arrow
        sr.color = Color(1f, 0.92f, 0.28f, 0.38f)
        sr.triangle(
            VW * 0.72f - 28f, VH * 0.04f,
            VW * 0.72f + 28f, VH * 0.04f,
            VW * 0.72f,       VH * 0.16f
        )
    }

    private fun drawHUDText(bat: com.badlogic.gdx.graphics.g2d.SpriteBatch) {
        font.data.setScale(2.4f)
        font.color = Color(1f, 0.90f, 0.28f, 1f)
        font.draw(bat, "SCORE  $score", 30f, VH - 28f)

        font.data.setScale(2.0f)
        font.color = Color(1f, 1f, 1f, 0.88f)
        font.draw(bat, "COINS  $coinCount", 30f, VH - 64f)

        // Speed indicator
        val speedPct = ((runSpeed - 480f) / (1100f - 480f)).coerceIn(0f, 1f)
        font.data.setScale(1.6f)
        font.color = Color(MathUtils.lerp(0.6f, 1f, speedPct), MathUtils.lerp(0.9f, 0.4f, speedPct), 0.2f, 0.75f)
        font.draw(bat, "SPEED  ${runSpeed.toInt()}", VW - 380f, VH - 28f)

        // Control hints
        font.data.setScale(1.5f)
        font.color = Color(1f, 1f, 1f, 0.28f)
        font.draw(bat, "SLIDE", VW * 0.05f, VH * 0.20f)
        font.draw(bat, "JUMP",  VW * 0.66f, VH * 0.20f)
    }

    // ── Game Over ──────────────────────────────────────────────────────────

    private fun drawGameOverOverlay(sr: ShapeRenderer) {
        val a = (goTimer * 1.8f).coerceAtMost(0.68f)
        sr.color = Color(0f, 0f, 0f, a)
        sr.rect(0f, 0f, VW, VH)

        // Panel
        if (goTimer > 0.4f) {
            val pa = ((goTimer - 0.4f) * 2.5f).coerceAtMost(1f)
            sr.color = Color(0.14f, 0.06f, 0.01f, pa * 0.88f)
            sr.rect(VW * 0.25f, VH * 0.30f, VW * 0.50f, VH * 0.40f)
            sr.color = Color(1f, 0.55f, 0.05f, pa * 0.70f)
            sr.rect(VW * 0.25f, VH * 0.68f, VW * 0.50f, 4f)
            sr.rect(VW * 0.25f, VH * 0.30f, VW * 0.50f, 4f)
        }
    }

    private fun drawGameOverText(bat: com.badlogic.gdx.graphics.g2d.SpriteBatch) {
        if (goTimer < 0.3f) return
        val a = ((goTimer - 0.3f) * 2.5f).coerceAtMost(1f)

        font.data.setScale(5.0f)
        font.color = Color(1f, 0.22f, 0.04f, a)
        layout.setText(font, "GAME OVER")
        font.draw(bat, "GAME OVER", VW / 2f - layout.width / 2f, VH * 0.72f)

        font.data.setScale(2.6f)
        font.color = Color(1f, 1f, 1f, a)
        val s = "Score: $score    Coins: $coinCount"
        layout.setText(font, s)
        font.draw(bat, s, VW / 2f - layout.width / 2f, VH * 0.56f)

        if (goTimer > 1.5f) {
            val blink = MathUtils.sin(goTimer * 5f) * 0.45f + 0.55f
            font.data.setScale(2.0f)
            font.color = Color(1f, 0.88f, 0.35f, blink)
            val tap = "Tap anywhere to play again"
            layout.setText(font, tap)
            font.draw(bat, tap, VW / 2f - layout.width / 2f, VH * 0.40f)
        }
    }

    // ── Screen lifecycle ───────────────────────────────────────────────────

    override fun resize(width: Int, height: Int) {
        worldCam.setToOrtho(false, VW, VH)
        uiCam.setToOrtho(false, VW, VH)
        worldCam.position.set(VW / 2f, VH / 2f, 0f)
        uiCam.position.set(VW / 2f, VH / 2f, 0f)
        worldCam.update(); uiCam.update()
    }

    override fun show()    {}
    override fun pause()   {}
    override fun resume()  {}
    override fun hide()    {}
    override fun dispose() { font.dispose() }
}
