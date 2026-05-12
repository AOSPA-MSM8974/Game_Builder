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

    companion object { const val VW = 1920f; const val VH = 1080f }

    private val camera  = OrthographicCamera(VW, VH)
    private val font    = BitmapFont()
    private val layout  = GlyphLayout()
    private val preview = Player(920f, Player.GROUND_Y)
    private var anim    = 0f
    private var selHat  = 1   // default TOP_HAT
    private var selBody = 0
    private var selShoe = 0

    private val hatTypes  = Player.HatType.values()
    private val bodyColors = listOf(
        Color(0.05f,0.05f,0.05f,1f), Color(0.55f,0.10f,0.10f,1f),
        Color(0.10f,0.24f,0.55f,1f), Color(0.10f,0.44f,0.14f,1f),
        Color(0.44f,0.22f,0.04f,1f), Color(0.34f,0.06f,0.40f,1f),
    )
    private val shoeColors = listOf(
        Color(0.15f,0.72f,0.15f,1f), Color(0.85f,0.15f,0.15f,1f),
        Color(0.15f,0.30f,0.85f,1f), Color(0.85f,0.72f,0.10f,1f),
        Color(1.00f,1.00f,1.00f,1f), Color(0.05f,0.05f,0.05f,1f),
    )

    private val playBtn  = Rectangle(760f,  80f, 400f, 110f)
    private val hatL     = Rectangle(660f, 530f,  70f,  62f)
    private val hatR     = Rectangle(1190f,530f,  70f,  62f)
    private val bodyL    = Rectangle(660f, 418f,  70f,  62f)
    private val bodyR    = Rectangle(1190f,418f,  70f,  62f)
    private val shoeL    = Rectangle(660f, 306f,  70f,  62f)
    private val shoeR    = Rectangle(1190f,306f,  70f,  62f)

    init {
        camera.position.set(VW/2f, VH/2f, 0f); camera.update()
        font.data.setScale(2.2f)
        preview.onGround = true; preview.hatType = Player.HatType.TOP_HAT
    }

    override fun render(delta: Float) {
        anim += delta
        Gdx.gl.glClearColor(0.96f, 0.62f, 0.10f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        handleInput()

        preview.bodyColor  = bodyColors[selBody]
        preview.hatType    = hatTypes[selHat]
        preview.hatColor   = bodyColors[selBody]
        preview.shoeColor  = shoeColors[selShoe]
        preview.animTimer  = anim

        val sr = game.shape; val bat = game.batch
        sr.projectionMatrix = camera.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        drawSky(sr); drawGround(sr); drawPanel(sr)
        drawArrow(sr, hatL, false);  drawArrow(sr, hatR,  true)
        drawArrow(sr, bodyL,false);  drawArrow(sr, bodyR, true)
        drawArrow(sr, shoeL,false);  drawArrow(sr, shoeR, true)
        drawSwatches(sr, bodyColors, bodyL.x + 80f, 424f)
        drawSwatches(sr, shoeColors, shoeL.x + 80f, 312f)
        drawPlayBtn(sr)
        preview.draw(sr)
        sr.end()

        bat.projectionMatrix = camera.combined
        bat.begin()
        // Logo
        bat.draw(Assets.logo, VW/2f - 340f, VH - 200f, 680f, 153f)
        drawText(bat)
        bat.end()
    }

    private fun handleInput() {
        if (!Gdx.input.justTouched()) return
        val rx = Gdx.input.x.toFloat() / Gdx.graphics.width.toFloat()  * VW
        val ry = (1f - Gdx.input.y.toFloat() / Gdx.graphics.height.toFloat()) * VH
        when {
            playBtn.contains(rx,ry)  -> launch()
            hatL.contains(rx,ry)     -> selHat  = (selHat  - 1 + hatTypes.size)    % hatTypes.size
            hatR.contains(rx,ry)     -> selHat  = (selHat  + 1)                    % hatTypes.size
            bodyL.contains(rx,ry)    -> selBody = (selBody - 1 + bodyColors.size)  % bodyColors.size
            bodyR.contains(rx,ry)    -> selBody = (selBody + 1)                    % bodyColors.size
            shoeL.contains(rx,ry)    -> selShoe = (selShoe - 1 + shoeColors.size)  % shoeColors.size
            shoeR.contains(rx,ry)    -> selShoe = (selShoe + 1)                    % shoeColors.size
        }
    }

    private fun launch() {
        val gs = GameScreen(game)
        gs.applyCustomization(bodyColors[selBody], hatTypes[selHat], bodyColors[selBody], shoeColors[selShoe])
        game.setScreen(gs)
    }

    private fun drawSky(sr: ShapeRenderer) {
        sr.color = Color(0.99f,0.78f,0.22f,1f); sr.rect(0f,VH*0.65f,VW,VH*0.35f)
        sr.color = Color(0.98f,0.68f,0.12f,1f); sr.rect(0f,VH*0.35f,VW,VH*0.30f)
        sr.color = Color(0.96f,0.58f,0.08f,1f); sr.rect(0f,96f,VW,VH*0.35f)
    }

    private fun drawGround(sr: ShapeRenderer) {
        sr.color = Color(0.78f,0.78f,0.80f,1f); sr.rect(0f,0f,VW,96f)
        sr.color = Color(0.68f,0.68f,0.70f,1f)
        var x = 0f
        while (x < VW + 96f) {
            sr.rectLine(x, 0f, x + 96f, 96f, 8f)
            x += 16f
        }
        sr.color = Color(0.85f,0.85f,0.86f,1f); sr.rect(0f,90f,VW,6f)
    }

    private fun drawPanel(sr: ShapeRenderer) {
        sr.color = Color(0f,0f,0f,0.48f); sr.rect(630f,280f,660f,330f)
        sr.color = Color(1f,0.70f,0.12f,0.65f)
        sr.rect(630f,608f,660f,4f); sr.rect(630f,280f,660f,4f)
        sr.rect(630f,280f,4f,332f); sr.rect(1286f,280f,4f,332f)
        // Preview bg
        sr.color = Color(0f,0f,0f,0.22f); sr.rect(850f,90f,220f,Player.H+60f)
    }

    private fun drawArrow(sr: ShapeRenderer, r: Rectangle, right: Boolean) {
        sr.color = Color(0.55f,0.25f,0.06f,0.88f); sr.rect(r.x,r.y,r.width,r.height)
        sr.color = Color(1f,0.82f,0.30f,1f)
        if (right) sr.triangle(r.x+14f,r.y+10f, r.x+14f,r.y+r.height-10f, r.x+r.width-10f,r.y+r.height/2f)
        else       sr.triangle(r.x+r.width-14f,r.y+10f, r.x+r.width-14f,r.y+r.height-10f, r.x+10f,r.y+r.height/2f)
    }

    private fun drawSwatches(sr: ShapeRenderer, colors: List<Color>, startX: Float, y: Float) {
        for (i in colors.indices) {
            val sx = startX + i * 82f
            if ((if (colors === bodyColors) selBody else selShoe) == i) {
                sr.color = Color(1f,0.88f,0.26f,1f); sr.rect(sx-4f,y-4f,52f,52f)
            }
            sr.color = colors[i]; sr.rect(sx,y,44f,44f)
        }
    }

    private fun drawPlayBtn(sr: ShapeRenderer) {
        val p = MathUtils.sin(anim*2.5f)*0.04f+0.96f
        sr.color = Color(1f,0.55f,0.04f,0.20f)
        sr.rect(playBtn.x-10f,playBtn.y-10f,playBtn.width+20f,playBtn.height+20f)
        sr.color = Color(0.88f*p,0.40f*p,0.04f,1f)
        sr.rect(playBtn.x,playBtn.y,playBtn.width,playBtn.height)
        sr.color = Color(1f,0.75f,0.24f,0.50f)
        sr.rect(playBtn.x,playBtn.y+playBtn.height-14f,playBtn.width,14f)
    }

    private fun drawText(bat: com.badlogic.gdx.graphics.g2d.SpriteBatch) {
        font.data.setScale(2.2f); font.color = Color(1f,0.85f,0.38f,1f)
        font.draw(bat,"CUSTOMIZE",830f,650f)
        font.data.setScale(2.0f); font.color = Color(1f,0.80f,0.36f,0.92f)
        font.draw(bat,"HAT",  700f,572f); font.color = Color.WHITE
        font.draw(bat,hatTypes[selHat].name,850f,572f)
        font.color = Color(1f,0.80f,0.36f,0.92f)
        font.draw(bat,"BODY", 700f,460f)
        font.draw(bat,"SHOES",700f,348f)
        font.data.setScale(3.4f); font.color = Color(0.10f,0.04f,0.01f,1f)
        layout.setText(font,"PLAY")
        font.draw(bat,"PLAY",VW/2f-layout.width/2f,158f)
        font.data.setScale(1.6f); font.color = Color(1f,1f,1f,0.30f)
        font.draw(bat,"TAP ANYWHERE TO JUMP",700f,44f)
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false,VW,VH); camera.position.set(VW/2f,VH/2f,0f); camera.update()
    }
    override fun show()    {}
    override fun pause()   {}
    override fun resume()  {}
    override fun hide()    {}
    override fun dispose() { font.dispose() }
}
