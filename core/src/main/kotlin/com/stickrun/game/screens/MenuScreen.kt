package com.stickrun.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.stickrun.game.StickRunGame
import com.stickrun.game.entities.Player

class MenuScreen(private val game: StickRunGame) : Screen {

    private val camera = OrthographicCamera()
    private val font = BitmapFont()
    private val layout = GlyphLayout()
    private val previewPlayer = Player(200f, 80f)

    private var animTimer = 0f
    private var selectedHat = 0
    private var selectedColor = 0

    private val hatTypes = Player.HatType.values()
    private val bodyColors = listOf(
        Color(0.1f, 0.1f, 0.1f, 1f),
        Color(0.6f, 0.15f, 0.15f, 1f),
        Color(0.15f, 0.35f, 0.6f, 1f),
        Color(0.15f, 0.5f, 0.2f, 1f),
        Color(0.5f, 0.3f, 0.05f, 1f),
        Color(0.4f, 0.1f, 0.45f, 1f)
    )
    private val hatColors = listOf(
        Color(0.8f, 0.1f, 0.1f, 1f),
        Color(0.1f, 0.3f, 0.8f, 1f),
        Color(0.1f, 0.6f, 0.2f, 1f),
        Color(0.6f, 0.5f, 0.05f, 1f),
        Color(0.5f, 0.1f, 0.5f, 1f)
    )

    // UI Rects (in screen coords 480x270)
    private val playBtn = Rectangle(170f, 30f, 140f, 38f)
    private val hatLeftBtn = Rectangle(120f, 148f, 28f, 22f)
    private val hatRightBtn = Rectangle(230f, 148f, 28f, 22f)
    private val colorLeftBtn = Rectangle(120f, 118f, 28f, 22f)
    private val colorRightBtn = Rectangle(230f, 118f, 28f, 22f)

    init {
        camera.setToOrtho(false, 480f, 270f)
        font.data.setScale(1.2f)
        previewPlayer.isRunning = true
    }

    override fun render(delta: Float) {
        animTimer += delta

        handleInput()

        previewPlayer.bodyColor = bodyColors[selectedColor]
        previewPlayer.hatType = hatTypes[selectedHat]
        previewPlayer.hatColor = hatColors[selectedColor % hatColors.size]

        Gdx.gl.glClearColor(0.95f, 0.55f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        game.shapeRenderer.projectionMatrix = camera.combined
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        drawBackground()
        drawPreviewPlayer()
        drawButtons()

        game.shapeRenderer.end()

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        drawText()
        game.batch.end()
    }

    private fun handleInput() {
        if (!Gdx.input.justTouched()) return
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val rx = Gdx.input.x.toFloat() / sw * 480f
        val ry = (1f - Gdx.input.y.toFloat() / sh) * 270f

        if (playBtn.contains(rx, ry)) {
            val gs = GameScreen(game)
            // Pass customization
            gs.javaClass.getDeclaredField("player").let {} // we'll pass via factory
            startGame()
        }
        if (hatLeftBtn.contains(rx, ry)) selectedHat = (selectedHat - 1 + hatTypes.size) % hatTypes.size
        if (hatRightBtn.contains(rx, ry)) selectedHat = (selectedHat + 1) % hatTypes.size
        if (colorLeftBtn.contains(rx, ry)) selectedColor = (selectedColor - 1 + bodyColors.size) % bodyColors.size
        if (colorRightBtn.contains(rx, ry)) selectedColor = (selectedColor + 1) % bodyColors.size
    }

    private fun startGame() {
        val screen = GameScreen(game)
        screen.applyCustomization(bodyColors[selectedColor], hatTypes[selectedHat], hatColors[selectedColor % hatColors.size])
        game.setScreen(screen)
    }

    private fun drawBackground() {
        // Sky gradient
        game.shapeRenderer.color = Color(0.98f, 0.48f, 0.05f, 1f)
        game.shapeRenderer.rect(0f, 0f, 480f, 135f)
        game.shapeRenderer.color = Color(0.92f, 0.62f, 0.18f, 1f)
        game.shapeRenderer.rect(0f, 135f, 480f, 135f)
        // Sun
        game.shapeRenderer.color = Color(1f, 0.9f, 0.3f, 0.15f)
        game.shapeRenderer.circle(390f, 220f, 55f, 20)
        game.shapeRenderer.color = Color(1f, 0.92f, 0.4f, 0.85f)
        game.shapeRenderer.circle(390f, 220f, 32f, 20)
        game.shapeRenderer.color = Color(1f, 1f, 0.7f, 0.75f)
        game.shapeRenderer.circle(390f, 220f, 20f, 16)
        // Ground
        game.shapeRenderer.color = Color(0.3f, 0.12f, 0.02f, 1f)
        game.shapeRenderer.rect(0f, 0f, 480f, 62f)
        game.shapeRenderer.color = Color(0.42f, 0.18f, 0.05f, 1f)
        game.shapeRenderer.rect(0f, 58f, 480f, 6f)
        // Some platforms for decoration
        game.shapeRenderer.color = Color(0.55f, 0.28f, 0.08f, 1f)
        game.shapeRenderer.rect(300f, 100f, 120f, 18f)
        game.shapeRenderer.rect(60f, 140f, 100f, 18f)
        game.shapeRenderer.color = Color(0.72f, 0.42f, 0.15f, 1f)
        game.shapeRenderer.rect(300f, 114f, 120f, 4f)
        game.shapeRenderer.rect(60f, 154f, 100f, 4f)
    }

    private fun drawPreviewPlayer() {
        // Preview box
        game.shapeRenderer.color = Color(0f, 0f, 0f, 0.25f)
        game.shapeRenderer.rect(168f, 72f, 104f, 100f)
        game.shapeRenderer.color = Color(0.55f, 0.28f, 0.08f, 0.7f)
        game.shapeRenderer.rect(170f, 74f, 100f, 96f)

        previewPlayer.position.set(200f, 90f)
        previewPlayer.animTimer = animTimer
        previewPlayer.draw(game.shapeRenderer)
    }

    private fun drawButtons() {
        // Play button
        game.shapeRenderer.color = Color(0.9f, 0.55f, 0.05f, 1f)
        game.shapeRenderer.rect(playBtn.x, playBtn.y, playBtn.width, playBtn.height)
        game.shapeRenderer.color = Color(1f, 0.8f, 0.3f, 1f)
        game.shapeRenderer.rect(playBtn.x, playBtn.y + playBtn.height - 5f, playBtn.width, 5f)

        // Arrow buttons
        drawArrowBtn(hatLeftBtn, false)
        drawArrowBtn(hatRightBtn, true)
        drawArrowBtn(colorLeftBtn, false)
        drawArrowBtn(colorRightBtn, true)

        // Color swatches
        for (i in bodyColors.indices) {
            val sx = 120f + i * 22f
            game.shapeRenderer.color = if (i == selectedColor) Color.WHITE else Color(0.6f, 0.4f, 0.2f, 0.5f)
            game.shapeRenderer.rect(sx - 1f, 99f, 16f, 16f)
            game.shapeRenderer.color = bodyColors[i]
            game.shapeRenderer.rect(sx, 100f, 14f, 14f)
        }
    }

    private fun drawArrowBtn(rect: Rectangle, pointRight: Boolean) {
        game.shapeRenderer.color = Color(0.7f, 0.35f, 0.1f, 0.85f)
        game.shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
        game.shapeRenderer.color = Color(1f, 0.85f, 0.4f, 1f)
        if (pointRight) {
            game.shapeRenderer.triangle(
                rect.x + 6f, rect.y + 4f,
                rect.x + 6f, rect.y + rect.height - 4f,
                rect.x + rect.width - 4f, rect.y + rect.height / 2f
            )
        } else {
            game.shapeRenderer.triangle(
                rect.x + rect.width - 6f, rect.y + 4f,
                rect.x + rect.width - 6f, rect.y + rect.height - 4f,
                rect.x + 4f, rect.y + rect.height / 2f
            )
        }
    }

    private fun drawText() {
        // Title
        font.data.setScale(2.8f)
        font.color = Color(0.15f, 0.07f, 0.01f, 0.4f)
        font.draw(game.batch, "STICK RUN", 103f, 253f)
        font.color = Color(1f, 0.88f, 0.3f, 1f)
        font.draw(game.batch, "STICK RUN", 100f, 256f)

        // Subtitle
        font.data.setScale(1.0f)
        font.color = Color(1f, 0.78f, 0.3f, 0.8f)
        font.draw(game.batch, "Jump. Run. Collect.", 148f, 228f)

        // Play button text
        font.data.setScale(1.6f)
        font.color = Color(0.15f, 0.07f, 0.01f, 1f)
        layout.setText(font, "PLAY")
        font.draw(game.batch, "PLAY", 240f - layout.width / 2f, 57f)

        // Customization labels
        font.data.setScale(1.0f)
        font.color = Color(1f, 0.88f, 0.5f, 1f)
        font.draw(game.batch, "HAT:", 162f, 165f)
        font.color = Color.WHITE
        font.draw(game.batch, hatTypes[selectedHat].name, 188f, 165f)
        font.color = Color(1f, 0.88f, 0.5f, 1f)
        font.draw(game.batch, "COLOR:", 162f, 135f)
        font.color = Color(1f, 0.88f, 0.5f, 1f)
        font.draw(game.batch, "STYLE:", 80f, 115f)
    }

    override fun resize(width: Int, height: Int) { camera.setToOrtho(false, 480f, 270f) }
    override fun show() {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { font.dispose() }
}
