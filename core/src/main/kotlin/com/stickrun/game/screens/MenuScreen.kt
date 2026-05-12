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
import com.stickrun.game.Assets
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

    private val preview   = Player(900f, Player.GROUND_Y)
    private var anim      = 0f

    private var selHat    = 0
    private var selColor  = 0

    private val hatTypes = Player.HatType.values()

    private val bodyColors = listOf(
        Color(0.08f, 0.08f, 0.08f, 1f),
        Color(0.58f, 0.10f, 0.10f, 1f),
        Color(0.10f, 0.26f, 0.58f, 1f),
        Color(0.10f, 0.46f, 0.14f, 1f),
        Color(0.46f, 0.24f, 0.04f, 1f),
        Color(0.36f, 0.06f, 0.42f, 1f),
        Color(0.08f, 0.34f, 0.44f, 1f),
        Color(0.44f, 0.36f, 0.04f, 1f),
    )
    private val hatColors = listOf(
        Color(0.76f, 0.10f, 0.10f, 1f),
        Color(0.10f, 0.24f, 0.76f, 1f),
        Color(0.10f, 0.54f, 0.14f, 1f),
        Color(0.56f, 0.44f, 0.04f, 1f),
        Color(0.46f, 0.06f, 0.46f, 1f),
        Color(0.06f, 0.06f, 0.06f, 1f),
        Color(0.06f, 0.32f, 0.42f, 1f),
        Color(0.42f, 0.34f, 0.04f, 1f),
    )

    // UI hit rects (1920x1080 space)
    private val playBtn   = Rectangle(760f,  80f,  400f, 110f)
    private val hatPrev   = Rectangle(660f,  536f,  72f,  64f)
    private val hatNext   = Rectangle(1188f, 536f,  72f,  64f)
    private val colPrev   = Rectangle(660f,  424f,  72f,  64f)
    private val colNext   = Rectangle(1188f, 424f,  72f,  64f)

    init {
        camera.position.set(VW/2f, VH/2f, 0f); camera.update()
        font.data.setScale(2.2f)
        preview.onGround = true
    }

    override fun render(delta: Float) {
        anim += delta

        Gdx.gl.glClearColor(0.06f, 0.02f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        handleInput()

        preview.bodyColor = bodyColors[selColor]
        preview.hatType   = hatTypes[selHat]
        preview.hatColor  = hatColors[selColor]
        preview.animTimer = anim

        val sr  = game.shape
        val bat = game.batch

        sr.projectionMatrix = camera.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        drawSky(sr)
        drawGround(sr)
        drawDecoSilhouettes(sr)
        drawPanelBg(sr)
        drawColorSwatches(sr)
        drawArrow(sr, hatPrev,  false)
        drawArrow(sr, hatNext,  true)
        drawArrow(sr, colPrev,  false)
        drawArrow(sr, colNext,  true)
        drawPlayBtn(sr)
        preview.draw(sr)
        sr.end()

        bat.projectionMatrix = camera.combined
        bat.begin()
        drawLogo(bat)
        drawText(bat)
        bat.end()
    }

    private fun handleInput() {
        if (!Gdx.input.justTouched()) return
        val rx = Gdx.input.x.toFloat() / Gdx.graphics.width.toFloat() * VW
        val ry = (1f - Gdx.input.y.toFloat() / Gdx.graphics.height.toFloat()) * VH
        when {
            playBtn.contains(rx,ry)  -> launch()
            hatPrev.contains(rx,ry)  -> selHat   = (selHat   - 1 + hatTypes.size)   % hatTypes.size
            hatNext.contains(rx,ry)  -> selHat   = (selHat   + 1)                   % hatTypes.size
            colPrev.contains(rx,ry)  -> selColor = (selColor - 1 + bodyColors.size) % bodyColors.size
            colNext.contains(rx,ry)  -> selColor = (selColor + 1)                   % bodyColors.size
        }
    }

    private fun launch() {
        val gs = GameScreen(game)
        gs.applyCustomization(bodyColors[selColor], hatTypes[selHat], hatColors[selColor])
        game.setScreen(gs)
    }

    private fun drawSky(sr: ShapeRenderer) {
        sr.color = Color(0.94f,0.40f,0.03f,1f); sr.rect(0f,0f,       VW,VH*0.28f)
        sr.color = Color(0.96f,0.54f,0.08f,1f); sr.rect(0f,VH*0.28f, VW,VH*0.30f)
        sr.color = Color(0.90f,0.66f,0.18f,1f); sr.rect(0f,VH*0.58f, VW,VH*0.24f)
        sr.color = Color(0.96f,0.82f,0.36f,1f); sr.rect(0f,VH*0.82f, VW,VH*0.18f)
        val sx = VW*0.84f; val sy = VH*0.90f
        sr.color = Color(1f,0.88f,0.26f,0.08f); sr.circle(sx,sy,150f,24)
        sr.color = Color(1f,0.90f,0.32f,0.14f); sr.circle(sx,sy,110f,24)
        sr.color = Color(1f,0.92f,0.40f,0.90f); sr.circle(sx,sy, 72f,24)
        sr.color = Color(1f,1.00f,0.72f,0.86f); sr.circle(sx,sy, 42f,18)
    }

    private fun drawGround(sr: ShapeRenderer) {
        sr.color = Color(0.16f,0.06f,0.01f,1f); sr.rect(0f,0f,               VW,Player.GROUND_Y)
        sr.color = Color(0.32f,0.13f,0.03f,1f); sr.rect(0f,Player.GROUND_Y-8f,VW,14f)
        sr.color = Color(0.48f,0.20f,0.05f,1f); sr.rect(0f,Player.GROUND_Y+4f,VW,6f)
    }

    private fun drawDecoSilhouettes(sr: ShapeRenderer) {
        // Building silhouettes
        val bldgs = listOf(150f to 190f, 340f to 130f, 1580f to 170f, 1720f to 110f, 1820f to 200f)
        for ((bx,bh) in bldgs) {
            sr.color = Color(0.58f,0.22f,0.03f,0.18f)
            sr.rect(bx, Player.GROUND_Y, 100f, bh)
        }
        // Deco crates on the ground
        sr.color = Color(0.50f,0.24f,0.06f,0.50f)
        sr.rect(260f, Player.GROUND_Y, 96f, 96f)
        sr.rect(1600f,Player.GROUND_Y, 96f,192f)
        sr.rect(1710f,Player.GROUND_Y, 96f, 96f)
        // Top highlight on deco crates
        sr.color = Color(0.70f,0.38f,0.12f,0.40f)
        sr.rect(260f, Player.GROUND_Y+88f, 96f, 8f)
        sr.rect(1600f,Player.GROUND_Y+184f,96f, 8f)
        sr.rect(1710f,Player.GROUND_Y+88f, 96f, 8f)
    }

    private fun drawPanelBg(sr: ShapeRenderer) {
        // Customize panel
        sr.color = Color(0f,0f,0f,0.55f)
        sr.rect(636f, 388f, 648f, 280f)
        // Accent borders
        sr.color = Color(1f,0.65f,0.10f,0.60f)
        sr.rect(636f, 666f, 648f, 4f)
        sr.rect(636f, 388f, 648f, 4f)
        sr.rect(636f, 388f, 4f,   280f)
        sr.rect(1280f,388f, 4f,   280f)
        // Preview box
        sr.color = Color(0f,0f,0f,0.30f)
        sr.rect(840f, Player.GROUND_Y-4f, 240f, Player.H+50f)
        sr.color = Color(1f,0.65f,0.10f,0.25f)
        sr.rect(840f, Player.GROUND_Y-4f, 240f, 4f)
    }

    private fun drawColorSwatches(sr: ShapeRenderer) {
        val sx0 = 740f; val sy = 400f; val size = 48f; val gap = 12f
        for (i in bodyColors.indices) {
            val bx = sx0 + i*(size+gap)
            if (i == selColor) {
                sr.color = Color(1f,0.88f,0.28f,1f)
                sr.rect(bx-5f, sy-5f, size+10f, size+10f)
            }
            sr.color = bodyColors[i]
            sr.rect(bx, sy, size, size)
        }
    }

    private fun drawArrow(sr: ShapeRenderer, r: Rectangle, right: Boolean) {
        sr.color = Color(0.60f,0.28f,0.07f,0.90f)
        sr.rect(r.x, r.y, r.width, r.height)
        sr.color = Color(1f,0.76f,0.28f,0.35f)
        sr.rect(r.x, r.y+r.height-10f, r.width, 10f)
        sr.color = Color(1f,0.90f,0.38f,1f)
        if (right)
            sr.triangle(r.x+14f, r.y+10f, r.x+14f, r.y+r.height-10f, r.x+r.width-10f, r.y+r.height/2f)
        else
            sr.triangle(r.x+r.width-14f, r.y+10f, r.x+r.width-14f, r.y+r.height-10f, r.x+10f, r.y+r.height/2f)
    }

    private fun drawPlayBtn(sr: ShapeRenderer) {
        val pulse = MathUtils.sin(anim*2.6f)*0.05f+0.95f
        // Glow
        sr.color = Color(1f,0.60f,0.04f,0.18f)
        sr.rect(playBtn.x-10f, playBtn.y-10f, playBtn.width+20f, playBtn.height+20f)
        // Body
        sr.color = Color(0.88f*pulse,0.40f*pulse,0.04f,1f)
        sr.rect(playBtn.x, playBtn.y, playBtn.width, playBtn.height)
        // Top shine
        sr.color = Color(1f,0.76f,0.24f,0.50f)
        sr.rect(playBtn.x, playBtn.y+playBtn.height-14f, playBtn.width, 14f)
        // Bottom shadow
        sr.color = Color(0.38f,0.14f,0.01f,0.60f)
        sr.rect(playBtn.x, playBtn.y, playBtn.width, 12f)
    }

    private fun drawLogo(bat: com.badlogic.gdx.graphics.g2d.SpriteBatch) {
        // Draw the generated logo PNG
        val lw = 1024f * 0.85f
        val lh = 256f  * 0.85f
        bat.draw(Assets.logo, VW/2f - lw/2f, VH - lh - 30f, lw, lh)
    }

    private fun drawText(bat: com.badlogic.gdx.graphics.g2d.SpriteBatch) {
        // Tagline
        font.data.setScale(2.2f)
        font.color = Color(1f,0.76f,0.26f,0.85f)
        font.draw(bat, "Run. Jump. Slide. Survive.", 570f, VH-220f)

        // Customize header
        font.data.setScale(2.6f)
        font.color = Color(1f,0.85f,0.38f,1f)
        font.draw(bat, "CUSTOMIZE", 830f, 680f)

        // Hat row
        font.data.setScale(2.1f)
        font.color = Color(1f,0.78f,0.36f,0.92f)
        font.draw(bat, "HAT", 700f, 580f)
        font.color = Color.WHITE
        font.draw(bat, hatTypes[selHat].name, 850f, 580f)

        // Color row label
        font.color = Color(1f,0.78f,0.36f,0.92f)
        font.draw(bat, "COLOR", 700f, 465f)

        // Play button label
        font.data.setScale(3.6f)
        font.color = Color(0.10f,0.04f,0.01f,1f)
        layout.setText(font,"PLAY")
        font.draw(bat,"PLAY", VW/2f-layout.width/2f, 164f)

        // Controls hint
        font.data.setScale(1.7f)
        font.color = Color(1f,1f,1f,0.32f)
        font.draw(bat, "LEFT = SLIDE     RIGHT = JUMP", 600f, 44f)
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, VW, VH)
        camera.position.set(VW/2f, VH/2f, 0f)
        camera.update()
    }

    override fun show()    {}
    override fun pause()   {}
    override fun resume()  {}
    override fun hide()    {}
    override fun dispose() { font.dispose() }
}
