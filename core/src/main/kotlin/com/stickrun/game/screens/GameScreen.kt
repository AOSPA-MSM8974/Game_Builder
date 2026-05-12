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

    companion object {
        const val VW = 1920f
        const val VH = 1080f
    }

    private val worldCam = OrthographicCamera(VW, VH)
    private val uiCam    = OrthographicCamera(VW, VH)

    val player    = Player(260f, Player.GROUND_Y)
    private val gen       = WorldGenerator()
    private val obstacles = mutableListOf<Obstacle>()
    private val coins     = mutableListOf<Coin>()

    private var score      = 0
    private var coinCount  = 0
    private var distance   = 0f
    private var speed      = 520f
    private var difficulty = 0f
    private var gameOver   = false
    private var goTimer    = 0f
    private var nextChunk  = 0f

    private data class BgEl(val wx: Float, val wy: Float, val w: Float, val h: Float)
    private val bgBuildings = ArrayList<BgEl>(40)
    private val bgTrees     = ArrayList<BgEl>(50)
    private val bgClouds    = ArrayList<BgEl>(30)

    private data class Popup(var wx: Float, var wy: Float, var life: Float, val text: String)
    private val popups  = mutableListOf<Popup>()
    private val hitRect = Rectangle()

    private val font   = BitmapFont()
    private val layout = GlyphLayout()

    fun applyCustomization(bodyColor: Color, hatType: Player.HatType, hatColor: Color) {
        player.bodyColor = bodyColor
        player.hatType   = hatType
        player.hatColor  = hatColor
    }

    init {
        worldCam.position.set(VW / 2f, VH / 2f, 0f); worldCam.update()
        uiCam.position.set(VW / 2f, VH / 2f, 0f);    uiCam.update()
        font.data.setScale(2.2f)

        for (i in 0..55) {
            bgBuildings.add(BgEl(i * 360f + MathUtils.random(0f, 180f), Player.GROUND_Y,
                MathUtils.random(50f, 130f), MathUtils.random(80f, 260f)))
            bgTrees.add(BgEl(i * 240f + MathUtils.random(0f, 120f), Player.GROUND_Y,
                22f, MathUtils.random(55f, 100f)))
            if (i < 32)
                bgClouds.add(BgEl(i * 420f + MathUtils.random(0f, 210f),
                    MathUtils.random(680f, 940f), MathUtils.random(60f, 120f), 0f))
        }

        spawnChunk(400f)
        spawnChunk(nextChunk)
    }

    private fun spawnChunk(startX: Float) {
        val (obs, cns) = gen.generateChunk(startX, difficulty)
        obstacles.addAll(obs)
        coins.addAll(cns)
        nextChunk = startX + 7000f
    }

    // ── Render ────────────────────────────────────────────────────────────

    override fun render(delta: Float) {
        val dt = delta.coerceIn(0.001f, 0.032f)

        handleInput()

        if (!gameOver) {
            update(dt)
        } else {
            goTimer += dt
            if (goTimer > 1.5f &&
                (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY))) {
                game.setScreen(MenuScreen(game))
                return
            }
        }

        draw()
    }

    // ── Input ─────────────────────────────────────────────────────────────

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.UP)    ||
            Gdx.input.isKeyJustPressed(Input.Keys.W))     player.jump()

        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)  ||
            Gdx.input.isKeyJustPressed(Input.Keys.S))     player.slide()

        if (Gdx.input.justTouched()) {
            val tx = Gdx.input.x.toFloat() / Gdx.graphics.width.toFloat()
            if (tx > 0.45f) player.jump() else player.slide()
        }
    }

    // ── Update ────────────────────────────────────────────────────────────

    private fun update(dt: Float) {
        speed      = (speed + 18f * dt).coerceAtMost(1200f)
        difficulty = ((player.x - 260f) / 32000f).coerceIn(0f, 1f)

        player.update(dt, speed)
        distance = (player.x - 260f).coerceAtLeast(0f)

        hitRect.set(
            player.bounds.x + 8f,
            player.bounds.y + 6f,
            player.bounds.width  - 16f,
            player.bounds.height - 8f
        )

        for (obs in obstacles) {
            obs.update(dt)
            if (obs.bounds.overlaps(hitRect)) {
                player.alive = false
                gameOver     = true
                return
            }
        }

        val pcx     = player.bounds.x + player.bounds.width  / 2f
        val pcy     = player.bounds.y + player.bounds.height / 2f
        val colDist = player.bounds.width * 0.55f + Coin.R
        val ci = coins.iterator()
        while (ci.hasNext()) {
            val c = ci.next()
            c.update(dt)
            if (!c.collected) {
                val dx = pcx - c.cx; val dy = pcy - c.cy
                if (dx*dx + dy*dy < colDist*colDist) {
                    c.collected = true; coinCount++
                    popups.add(Popup(c.cx, c.cy + 30f, 1.2f, "+10"))
                }
            }
            if (c.done) ci.remove()
        }

        val pi = popups.iterator()
        while (pi.hasNext()) {
            val p = pi.next(); p.wy += dt * 100f; p.life -= dt * 1.2f
            if (p.life <= 0f) pi.remove()
        }

        val targetX = player.x + VW * 0.22f
        worldCam.position.x = MathUtils.lerp(worldCam.position.x, targetX, 0.12f)
        worldCam.position.y = VH / 2f
        worldCam.update()

        if (player.x + 3500f > nextChunk) spawnChunk(nextChunk)

        val cullX = worldCam.position.x - VW * 0.8f
        obstacles.removeAll { it.bounds.x + it.bounds.width < cullX }
        coins.removeAll     { it.done || it.cx < cullX }

        score = (distance / 8f).toInt() + coinCount * 10
    }

    // ── Draw ──────────────────────────────────────────────────────────────
    // Rule: never switch between batch and shape without ending the previous one.
    // Order: shape(world bg) → batch(sprites) → shape(player) → shape(ui) → batch(ui text)

    private fun draw() {
        Gdx.gl.glClearColor(0.94f, 0.40f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val camL = worldCam.position.x - VW / 2f
        val sr   = game.shape
        val bat  = game.batch

        // ── 1. World background shapes ────────────────────────────────
        sr.projectionMatrix = worldCam.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        drawSky(camL)
        drawParallax(camL)
        drawGround(camL)
        sr.end()

        // ── 2. World sprites (obstacles + coins) ──────────────────────
        bat.projectionMatrix = worldCam.combined
        bat.begin()
        for (obs in obstacles) {
            if (obs.bounds.x > camL + VW + 150f || obs.bounds.x + obs.bounds.width < camL - 150f) continue
            obs.draw(bat)
        }
        for (coin in coins) {
            if (coin.cx > camL + VW + 80f || coin.cx < camL - 80f) continue
            coin.draw(bat)
        }
        bat.end()

        // ── 3. Player drawn as shapes on top ──────────────────────────
        sr.projectionMatrix = worldCam.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        player.draw(sr)
        sr.end()

        // ── 4. UI shapes (HUD bar + tiny speed bar only, NO solid zones) ──
        sr.projectionMatrix = uiCam.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        drawHUDBg()
        if (gameOver) drawGameOverBg()
        sr.end()

        // ── 5. UI text + popups ───────────────────────────────────────
        bat.projectionMatrix = worldCam.combined
        bat.begin()
        font.data.setScale(1.8f)
        for (p in popups) {
            font.color = Color(1f, 0.95f, 0.25f, p.life.coerceIn(0f, 1f))
            font.draw(bat, p.text, p.wx, p.wy)
        }
        bat.end()

        bat.projectionMatrix = uiCam.combined
        bat.begin()
        drawHUDText()
        if (gameOver) drawGameOverText()
        bat.end()
    }

    // ── Sky ───────────────────────────────────────────────────────────────

    private fun drawSky(camL: Float) {
        val sr = game.shape; val w = VW + 4f
        sr.color = Color(0.94f, 0.40f, 0.03f, 1f); sr.rect(camL-2f, 0f,       w, VH*0.28f)
        sr.color = Color(0.96f, 0.54f, 0.08f, 1f); sr.rect(camL-2f, VH*0.28f, w, VH*0.30f)
        sr.color = Color(0.90f, 0.66f, 0.18f, 1f); sr.rect(camL-2f, VH*0.58f, w, VH*0.24f)
        sr.color = Color(0.96f, 0.82f, 0.36f, 1f); sr.rect(camL-2f, VH*0.82f, w, VH*0.18f)
        val sx = camL + VW*0.84f; val sy = VH*0.90f
        sr.color = Color(1f,0.88f,0.26f,0.08f); sr.circle(sx,sy,150f,24)
        sr.color = Color(1f,0.90f,0.30f,0.14f); sr.circle(sx,sy,110f,24)
        sr.color = Color(1f,0.92f,0.40f,0.90f); sr.circle(sx,sy, 72f,24)
        sr.color = Color(1f,1.00f,0.72f,0.86f); sr.circle(sx,sy, 42f,18)
    }

    // ── Parallax ──────────────────────────────────────────────────────────

    private fun drawParallax(camL: Float) {
        val sr = game.shape; val wrap = VW + 1400f
        for (b in bgBuildings) {
            val bx = camL + ((b.wx - camL*0.08f + 1e6f) % wrap)
            sr.color = Color(0.60f,0.22f,0.03f,0.16f)
            sr.rect(bx, Player.GROUND_Y, b.w, b.h)
        }
        for (t in bgTrees) {
            val tx = camL + ((t.wx - camL*0.28f + 1e6f) % wrap)
            sr.color = Color(0.36f,0.13f,0.02f,0.52f)
            sr.rect(tx+5f, Player.GROUND_Y, 12f, t.h*0.60f)
            sr.color = Color(0.26f,0.09f,0.01f,0.62f)
            sr.circle(tx+11f, Player.GROUND_Y+t.h*0.60f+30f, 34f, 12)
        }
        for (c in bgClouds) {
            val cx = camL + ((c.wx - camL*0.12f + 1e6f) % wrap)
            sr.color = Color(1f,0.74f,0.30f,0.28f)
            sr.circle(cx,          c.wy,     c.w,       12)
            sr.circle(cx+c.w*0.9f, c.wy-12f, c.w*0.68f, 10)
            sr.circle(cx-c.w*0.7f, c.wy-14f, c.w*0.56f, 10)
        }
    }

    // ── Ground ────────────────────────────────────────────────────────────

    private fun drawGround(camL: Float) {
        val sr = game.shape; val w = VW + 4f
        sr.color = Color(0.16f,0.06f,0.01f,1f); sr.rect(camL-2f, 0f,                w, Player.GROUND_Y)
        sr.color = Color(0.32f,0.13f,0.03f,1f); sr.rect(camL-2f, Player.GROUND_Y-8f, w, 14f)
        sr.color = Color(0.48f,0.20f,0.05f,1f); sr.rect(camL-2f, Player.GROUND_Y+4f, w, 6f)
        sr.color = Color(0.26f,0.10f,0.02f,0.4f)
        var gx = camL - (camL % 128f)
        while (gx < camL + VW + 128f) { sr.rect(gx, Player.GROUND_Y-7f, 2f, 10f); gx += 128f }
    }

    // ── HUD (top bar + speed strip only — no solid touch zones) ──────────

    private fun drawHUDBg() {
        val sr  = game.shape
        val pct = ((speed - 520f) / (1200f - 520f)).coerceIn(0f, 1f)

        // Semi-transparent top bar
        sr.color = Color(0f, 0f, 0f, 0.45f)
        sr.rect(0f, VH - 90f, VW, 90f)

        // Gold accent line under bar
        sr.color = Color(1f, 0.76f, 0.18f, 0.60f)
        sr.rect(0f, VH - 93f, VW, 3f)

        // Speed strip at very bottom — thin, non-intrusive
        sr.color = Color(0f, 0f, 0f, 0.25f)
        sr.rect(0f, 0f, VW, 14f)
        sr.color = Color(MathUtils.lerp(0.2f,1f,pct), MathUtils.lerp(0.9f,0.25f,pct), 0.1f, 0.55f)
        sr.rect(0f, 0f, VW * pct, 14f)

        // Small tap-hint icons — just arrow outlines, no solid zones
        // Down arrow hint (slide — bottom left)
        sr.color = Color(1f, 1f, 1f, 0.18f)
        sr.triangle(VW*0.10f - 22f, VH*0.12f, VW*0.10f + 22f, VH*0.12f, VW*0.10f, VH*0.04f)
        // Up arrow hint (jump — bottom right)
        sr.color = Color(1f, 0.90f, 0.20f, 0.22f)
        sr.triangle(VW*0.80f - 22f, VH*0.04f, VW*0.80f + 22f, VH*0.04f, VW*0.80f, VH*0.12f)
    }

    private fun drawHUDText() {
        font.data.setScale(2.6f)
        font.color = Color(1f, 0.90f, 0.26f, 1f)
        font.draw(game.batch, "SCORE  $score", 32f, VH - 18f)

        font.data.setScale(2.1f)
        font.color = Color(1f, 1f, 1f, 0.88f)
        font.draw(game.batch, "COINS  $coinCount", 32f, VH - 55f)

        font.data.setScale(1.6f)
        font.color = Color(1f, 1f, 1f, 0.35f)
        font.draw(game.batch, "SPEED  ${speed.toInt()}", VW - 320f, VH - 26f)

        // Tiny control labels near the arrow hints
        font.data.setScale(1.4f)
        font.color = Color(1f, 1f, 1f, 0.22f)
        font.draw(game.batch, "SLIDE", VW*0.06f, VH*0.14f)
        font.draw(game.batch, "JUMP",  VW*0.76f, VH*0.14f)
    }

    // ── Game Over ─────────────────────────────────────────────────────────

    private fun drawGameOverBg() {
        val sr  = game.shape
        val dim = (goTimer * 2f).coerceAtMost(0.70f)
        sr.color = Color(0f, 0f, 0f, dim); sr.rect(0f, 0f, VW, VH)
        if (goTimer > 0.4f) {
            val a = ((goTimer - 0.4f) * 2.5f).coerceAtMost(0.92f)
            sr.color = Color(0.10f, 0.04f, 0.01f, a)
            sr.rect(VW*0.22f, VH*0.30f, VW*0.56f, VH*0.40f)
            sr.color = Color(0.85f, 0.40f, 0.05f, a * 0.80f)
            sr.rect(VW*0.22f, VH*0.68f, VW*0.56f, 5f)
            sr.rect(VW*0.22f, VH*0.30f, VW*0.56f, 5f)
        }
    }

    private fun drawGameOverText() {
        if (goTimer < 0.3f) return
        val a = ((goTimer - 0.3f) * 2.5f).coerceAtMost(1f)

        font.data.setScale(6.0f)
        font.color = Color(1f, 0.20f, 0.04f, a)
        layout.setText(font, "GAME OVER")
        font.draw(game.batch, "GAME OVER", VW/2f - layout.width/2f, VH*0.74f)

        font.data.setScale(2.6f)
        font.color = Color(1f, 1f, 1f, a)
        val s = "Score: $score     Coins: $coinCount"
        layout.setText(font, s)
        font.draw(game.batch, s, VW/2f - layout.width/2f, VH*0.56f)

        if (goTimer > 1.5f) {
            val blink = MathUtils.sin(goTimer * 5f) * 0.45f + 0.55f
            font.data.setScale(2.0f)
            font.color = Color(1f, 0.85f, 0.30f, blink)
            val tap = "Tap anywhere to play again"
            layout.setText(font, tap)
            font.draw(game.batch, tap, VW/2f - layout.width/2f, VH*0.42f)
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
