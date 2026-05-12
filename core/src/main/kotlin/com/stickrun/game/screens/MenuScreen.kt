package com.stickrun.game.screens

import com.badlogic.gdx.Gdx
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
import com.stickrun.game.entities.Player

class MenuScreen(private val game: StickRunGame) : Screen {

    companion object {
        const val VW = 1920f
        const val VH = 1080f
    }

    private val camera = OrthographicCamera(VW, VH)
    private val font   = BitmapFont()
    private val layout = GlyphLayout()

    private val preview    = Player(860f, Player.GROUND_Y)
    private var animTimer  = 0f
    private var selectedHat   = 0
    private var selectedColor = 0

    private val hatTypes = Player.HatType.values()

    private val bodyColors = listOf(
        Color(0.08f, 0.08f, 0.08f, 1f),   // black
        Color(0.58f, 0.12f, 0.12f, 1f),   // red
        Color(0.12f, 0.28f, 0.58f, 1f),   // blue
        Color(0.12f, 0.46f, 0.16f, 1f),   // green
        Color(0.46f, 0.26f, 0.04f, 1f),   // brown
        Color(0.36f, 0.08f, 0.42f, 1f),   // purple
        Color(0.10f, 0.36f, 0.46f, 1f),   // teal
        Color(0.46f, 0.38f, 0.06f, 1f),   // gold
    )

    private val hatColors = listOf(
        Color(0.76f, 0.10f, 0.10f, 1f),
        Color(0.10f, 0.26f, 0.76f, 1f),
        Color(0.10f, 0.56f, 0.16f, 1f),
        Color(0.56f, 0.46f, 0.04f, 1f),
        Color(0.46f, 0.08f, 0.46f, 1f),
        Color(0.08f, 0.08f, 0.08f, 1f),
        Color(0.08f, 0.34f, 0.44f, 1f),
        Color(0.44f, 0.36f, 0.04f, 1f),
    )

    // UI rects (in virtual 1920x1080 space)
    private val playBtn    = Rectangle(760f,  80f,  400f, 110f)
    private val hatPrev    = Rectangle(660f,  530f,  70f,  60f)
    private val hatNext    = Rectangle(1190f, 530f,  70f,  60f)
    private val colorPrev  = Rectangle(660f,  420f,  70f,  60f)
    private val colorNext  = Rectangle(1190f, 420f,  70f,  60f)

    // Background silhouettes for menu
    private data class Sil(val x: Float, val w: Float, val h: Float)
    private val silhouettes = mutableListOf<Sil>()

    init {
        camera.position.set(VW / 2f, VH / 2f, 0f)
        camera.update()
        font.data.setScale(2.2f)

        for (i in 0..14) {
            silhouettes.add(Sil(
                i * 138f + MathUtils.random(0f, 70f),
                MathUtils.random(40f, 110f),
                MathUtils.random(70f, 220f)
            ))
        }
    }

    override fun render(delta: Float) {
        animTimer += delta

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        handleInput()

        // Update preview player cosmetics
        preview.bodyColor  = bodyColors[selectedColor]
        preview.hatType    = hatTypes[selectedHat]
        preview.hatColor   = hatColors[selectedColor]
        preview.animTimer  = animTimer
        preview.onGround   = true

        val sr  = game.shape
        val bat = game.batch

        sr.projectionMatrix = camera.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)

        drawSky(sr)
        drawGround(sr)
        drawSilhouettes(sr)
        drawPanels(sr)
        drawArrowBtn(sr, hatPrev,   false)
        drawArrowBtn(sr, hatNext,   true)
        drawArrowBtn(sr, colorPrev, false)
        drawArrowBtn(sr, colorNext, true)
        drawColorSwatches(sr)
        drawPlayButton(sr)
        preview.draw(sr)

        sr.end()

        bat.projectionMatrix = camera.combined
        bat.begin()
        drawText(bat)
        bat.end()
    }

    private fun handleInput() {
        if (!Gdx.input.justTouched()) return
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val rx = Gdx.input.x.toFloat()       / sw * VW
        val ry = (1f - Gdx.input.y.toFloat() / sh) * VH

        when {
            playBtn.contains(rx, ry)   -> launchGame()
            hatPrev.contains(rx, ry)   -> selectedHat   = (selectedHat   - 1 + hatTypes.size)   % hatTypes.size
            hatNext.contains(rx, ry)   -> selectedHat   = (selectedHat   + 1)                   % hatTypes.size
            colorPrev.contains(rx, ry) -> selectedColor = (selectedColor - 1 + bodyColors.size) % bodyColors.size
            colorNext.contains(rx, ry) -> selectedColor = (selectedColor + 1)                   % bodyColors.size
        }
    }

    private fun launchGame() {
        val gs = GameScreen(game)
        gs.applyCustomization(bodyColors[selectedColor], hatTypes[selectedHat], hatColors[selectedColor])
        game.setScreen(gs)
    }

    // ── Draw helpers ──────────────────────────────────────────────────────

    private fun drawSky(sr: ShapeRenderer) {
        sr.color = Color(0.94f, 0.40f, 0.03f, 1f); sr.rect(0f, 0f,           VW, VH * 0.32f)
        sr.color = Color(0.96f, 0.54f, 0.08f, 1f); sr.rect(0f, VH * 0.32f,   VW, VH * 0.34f)
        sr.color = Color(0.90f, 0.66f, 0.18f, 1f); sr.rect(0f, VH * 0.66f,   VW, VH * 0.20f)
        sr.color = Color(0.97f, 0.82f, 0.38f, 1f); sr.rect(0f, VH * 0.86f,   VW, VH * 0.14f)

        // Sun
        val sx = VW * 0.84f; val sy = VH * 0.88f
        sr.color = Color(1f, 0.88f, 0.28f, 0.09f); sr.circle(sx, sy, 130f, 24)
        sr.color = Color(1f, 0.90f, 0.32f, 0.16f); sr.circle(sx, sy, 100f, 24)
        sr.color = Color(1f, 0.92f, 0.40f, 0.88f); sr.circle(sx, sy,  68f, 24)
        sr.color = Color(1f, 1.00f, 0.72f, 0.84f); sr.circle(sx, sy,  40f, 18)
    }

    private fun drawGround(sr: ShapeRenderer) {
        sr.color = Color(0.18f, 0.07f, 0.01f, 1f); sr.rect(0f, 0f,                    VW, Player.GROUND_Y)
        sr.color = Color(0.34f, 0.14f, 0.03f, 1f); sr.rect(0f, Player.GROUND_Y - 8f,  VW, 14f)
        sr.color = Color(0.50f, 0.22f, 0.06f, 1f); sr.rect(0f, Player.GROUND_Y + 4f,  VW, 5f)
    }

    private fun drawSilhouettes(sr: ShapeRenderer) {
        for (s in silhouettes) {
            sr.color = Color(0.60f, 0.24f, 0.04f, 0.18f)
            sr.rect(s.x, Player.GROUND_Y, s.w, s.h)
        }
        // A few decorative crates on the ground
        sr.color = Color(0.50f, 0.25f, 0.06f, 0.45f)
        sr.rect(280f,  Player.GROUND_Y, 64f, 64f)
        sr.rect(1580f, Player.GROUND_Y, 64f, 128f)
        sr.rect(1660f, Player.GROUND_Y, 64f,  64f)
    }

    private fun drawPanels(sr: ShapeRenderer) {
        // Main customize panel
        sr.color = Color(0f, 0f, 0f, 0.50f)
        sr.rect(640f, 380f, 640f, 260f)

        // Top accent
        sr.color = Color(1f, 0.65f, 0.10f, 0.55f)
        sr.rect(640f, 638f, 640f, 4f)
        sr.rect(640f, 380f, 640f, 4f)

        // Preview bg
        sr.color = Color(0f, 0f, 0f, 0.28f)
        sr.rect(820f, Player.GROUND_Y - 4f, 280f, Player.H + 40f)
        sr.color = Color(1f, 0.65f, 0.10f, 0.20f)
        sr.rect(820f, Player.GROUND_Y - 4f, 280f, 4f)
    }

    private fun drawArrowBtn(sr: ShapeRenderer, r: Rectangle, right: Boolean) {
        // Button bg
        sr.color = Color(0.62f, 0.30f, 0.08f, 0.88f)
        sr.rect(r.x, r.y, r.width, r.height)
        // Hover shine top
        sr.color = Color(1f, 0.75f, 0.30f, 0.30f)
        sr.rect(r.x, r.y + r.height - 8f, r.width, 8f)
        // Arrow
        sr.color = Color(1f, 0.90f, 0.40f, 1f)
        if (right) {
            sr.triangle(
                r.x + 14f,             r.y + 10f,
                r.x + 14f,             r.y + r.height - 10f,
                r.x + r.width - 10f,   r.y + r.height / 2f
            )
        } else {
            sr.triangle(
                r.x + r.width - 14f,   r.y + 10f,
                r.x + r.width - 14f,   r.y + r.height - 10f,
                r.x + 10f,             r.y + r.height / 2f
            )
        }
    }

    private fun drawColorSwatches(sr: ShapeRenderer) {
        val startX = 740f
        val y      = 408f
        val size   = 44f
        val gap    = 12f
        for (i in bodyColors.indices) {
            val sx = startX + i * (size + gap)
            if (i == selectedColor) {
                sr.color = Color(1f, 0.88f, 0.30f, 1f)
                sr.rect(sx - 4f, y - 4f, size + 8f, size + 8f)
            }
            sr.color = bodyColors[i]
            sr.rect(sx, y, size, size)
        }
    }

    private fun drawPlayButton(sr: ShapeRenderer) {
        val pulse = MathUtils.sin(animTimer * 2.8f) * 0.04f + 0.96f

        // Outer glow
        sr.color = Color(1f, 0.60f, 0.04f, 0.22f)
        sr.rect(playBtn.x - 8f, playBtn.y - 8f, playBtn.width + 16f, playBtn.height + 16f)

        // Button body
        sr.color = Color(0.88f * pulse, 0.40f * pulse, 0.04f, 1f)
        sr.rect(playBtn.x, playBtn.y, playBtn.width, playBtn.height)

        // Top highlight
        sr.color = Color(1f, 0.76f, 0.26f, 0.55f)
        sr.rect(playBtn.x, playBtn.y + playBtn.height - 12f, playBtn.width, 12f)

        // Bottom shadow
        sr.color = Color(0.40f, 0.16f, 0.01f, 0.60f)
        sr.rect(playBtn.x, playBtn.y, playBtn.width, 10f)
    }

    private fun drawText(bat: com.badlogic.gdx.graphics.g2d.SpriteBatch) {
        // ── Title ──────────────────────────────────────────────────────
        font.data.setScale(8.0f)
        // Shadow
        font.color = Color(0.10f, 0.04f, 0.01f, 0.55f)
        font.draw(bat, "STICK RUN", 310f, 1010f)
        // Main
        font.color = Color(1f, 0.88f, 0.26f, 1f)
        font.draw(bat, "STICK RUN", 306f, 1016f)

        // ── Tagline ────────────────────────────────────────────────────
        font.data.setScale(2.4f)
        font.color = Color(1f, 0.76f, 0.28f, 0.82f)
        font.draw(bat, "Run. Jump. Slide. Survive.", 568f, 920f)

        // ── Customize header ───────────────────────────────────────────
        font.data.setScale(2.8f)
        font.color = Color(1f, 0.85f, 0.40f, 1f)
        font.draw(bat, "CUSTOMIZE", 830f, 660f)

        // Hat row
        font.data.setScale(2.2f)
        font.color = Color(1f, 0.80f, 0.38f, 0.90f)
        font.draw(bat, "HAT", 700f, 578f)
        font.color = Color.WHITE
        font.draw(bat, hatTypes[selectedHat].name, 850f, 578f)

        // Color row label
        font.color = Color(1f, 0.80f, 0.38f, 0.90f)
        font.draw(bat, "COLOR", 700f, 468f)

        // ── Play button label ──────────────────────────────────────────
        font.data.setScale(3.8f)
        font.color = Color(0.10f, 0.04f, 0.01f, 1f)
        layout.setText(font, "PLAY")
        font.draw(bat, "PLAY", VW / 2f - layout.width / 2f, 166f)

        // ── Controls hint ──────────────────────────────────────────────
        font.data.setScale(1.8f)
        font.color = Color(1f, 1f, 1f, 0.38f)
        font.draw(bat, "LEFT TAP = SLIDE   |   RIGHT TAP = JUMP", 540f, 40f)
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, VW, VH)
        camera.position.set(VW / 2f, VH / 2f, 0f)
        camera.update()
    }

    override fun show()    {}
    override fun pause()   {}
    override fun resume()  {}
    override fun hide()    {}
    override fun dispose() { font.dispose() }
}
