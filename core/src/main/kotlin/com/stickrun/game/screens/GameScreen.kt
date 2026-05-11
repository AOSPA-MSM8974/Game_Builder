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
import com.badlogic.gdx.math.Vector2
import com.stickrun.game.StickRunGame
import com.stickrun.game.entities.Coin
import com.stickrun.game.entities.Platform
import com.stickrun.game.entities.Player
import com.stickrun.game.world.WorldGenerator

class GameScreen(private val game: StickRunGame) : Screen {

    private val camera = OrthographicCamera()
    private val uiCamera = OrthographicCamera()
    val player = Player(100f, 200f)
    private val generator = WorldGenerator()
    private val platforms: MutableList<Platform>
    private val coins: MutableList<Coin>
    private var score = 0
    private var coinCount = 0
    private val font = BitmapFont()
    private val layout = GlyphLayout()
    private val leftBtnBounds = Rectangle()
    private val rightBtnBounds = Rectangle()
    private val jumpBtnBounds = Rectangle()
    private val bgClouds = mutableListOf<Vector2>()
    private val bgTrees = mutableListOf<Vector2>()
    private val bgSils = mutableListOf<Vector2>()
    private var lastChunkEnd = 5000f
    private val deathY = -200f
    private var gameOver = false
    private var gameOverTimer = 0f
    private data class FloatText(val text: String, var x: Float, var y: Float, var life: Float)
    private val floatTexts = mutableListOf<FloatText>()

    fun applyCustomization(bodyColor: Color, hatType: Player.HatType, hatColor: Color) {
        player.bodyColor = bodyColor
        player.hatType = hatType
        player.hatColor = hatColor
    }

    init {
        camera.setToOrtho(false, 480f, 270f)
        uiCamera.setToOrtho(false, 480f, 270f)
        val (plats, coinList) = generator.generateLevel()
        platforms = plats.toMutableList()
        coins = coinList.toMutableList()
        for (i in 0..30) {
            bgClouds.add(Vector2(i * 160f + MathUtils.random(0f, 80f), 190f + MathUtils.random(-20f, 30f)))
            if (i % 2 == 0) bgTrees.add(Vector2(i * 220f + MathUtils.random(0f, 100f), 58f))
            bgSils.add(Vector2(i * 300f + MathUtils.random(0f, 150f), 60f + MathUtils.random(0f, 60f)))
        }
        font.data.setScale(1.2f)
    }

    override fun render(delta: Float) {
        val dt = delta.coerceAtMost(0.05f)
        handleInput()
        if (!gameOver) update(dt)
        draw()
        if (gameOver) {
            gameOverTimer += dt
            if (gameOverTimer > 2f && (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) || Gdx.input.justTouched())) {
                game.setScreen(MenuScreen(game))
            }
        }
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) player.jump()
        val sw = Gdx.graphics.width.toFloat(); val sh = Gdx.graphics.height.toFloat()
        leftBtnBounds.set(0f, 0f, sw * 0.25f, sh * 0.4f)
        rightBtnBounds.set(sw * 0.25f, 0f, sw * 0.25f, sh * 0.4f)
        jumpBtnBounds.set(sw * 0.65f, 0f, sw * 0.35f, sh * 0.4f)
        for (i in 0 until 5) {
            if (!Gdx.input.isTouched(i)) continue
            val tx = Gdx.input.getX(i).toFloat(); val ty = sh - Gdx.input.getY(i).toFloat()
            if (leftBtnBounds.contains(tx, ty)) { player.velocity.x = -Player.MOVE_SPEED; player.facingRight = false; player.isRunning = true }
            if (rightBtnBounds.contains(tx, ty)) { player.velocity.x = Player.MOVE_SPEED; player.facingRight = true; player.isRunning = true }
            if (jumpBtnBounds.contains(tx, ty) && Gdx.input.justTouched()) player.jump()
        }
    }

    private fun update(delta: Float) {
        player.update(delta)
        for (plat in platforms) {
            val pb = plat.bounds
            if (!pb.overlaps(player.bounds)) continue
            val platTop = pb.y + pb.height
            when {
                player.velocity.y <= 0f && player.position.y <= platTop + 8f && player.position.y >= pb.y + pb.height - 8f -> {
                    player.position.y = platTop; player.land()
                }
                player.velocity.y > 0f && player.position.y + Player.HEIGHT >= pb.y && player.position.y + Player.HEIGHT <= pb.y + 14f -> {
                    player.velocity.y = 0f; player.position.y = pb.y - Player.HEIGHT
                }
                player.velocity.x > 0f && player.position.x + Player.WIDTH > pb.x && player.position.x < pb.x + 8f -> {
                    player.position.x = pb.x - Player.WIDTH; player.velocity.x = 0f
                }
                player.velocity.x < 0f && player.position.x < pb.x + pb.width && player.position.x + Player.WIDTH > pb.x + pb.width - 8f -> {
                    player.position.x = pb.x + pb.width; player.velocity.x = 0f
                }
            }
            player.bounds.setPosition(player.position.x, player.position.y)
        }
        val iter = coins.iterator()
        while (iter.hasNext()) {
            val coin = iter.next()
            coin.update(delta)
            if (!coin.collected && Vector2(player.position.x + Player.WIDTH / 2f, player.position.y + Player.HEIGHT / 2f).dst(coin.position.x + Coin.RADIUS, coin.position.y + Coin.RADIUS) < Player.WIDTH * 0.65f + Coin.RADIUS) {
                coin.collect(); coinCount++; floatTexts.add(FloatText("+10", coin.position.x, coin.position.y, 1f))
            }
            if (coin.isFinished()) iter.remove()
        }
        floatTexts.forEach { it.y += delta * 40f; it.life -= delta }
        floatTexts.removeAll { it.life <= 0f }
        camera.position.x = MathUtils.lerp(camera.position.x, player.position.x - camera.viewportWidth * 0.35f + camera.viewportWidth / 2f, 0.1f)
        camera.position.y = MathUtils.lerp(camera.position.y, camera.viewportHeight / 2f + 30f, 0.08f)
        camera.update()
        if (player.position.x > lastChunkEnd - 1200f) { val (np, nc) = generator.generateChunk(lastChunkEnd); platforms.addAll(np); coins.addAll(nc); lastChunkEnd += 800f }
        val camLeft = camera.position.x - camera.viewportWidth / 2f
        platforms.removeAll { it.bounds.x + it.bounds.width < camLeft - camera.viewportWidth }
        if (player.position.y < deathY) gameOver = true
        score = (player.position.x / 10f).toInt() + coinCount * 10
    }

    private fun draw() {
        val camLeft = camera.position.x - camera.viewportWidth / 2f
        Gdx.gl.glClearColor(0.95f, 0.55f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.projectionMatrix = camera.combined
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Sky layers
        game.shapeRenderer.color = Color(0.98f, 0.45f, 0.04f, 1f); game.shapeRenderer.rect(camLeft-5f, 0f, camera.viewportWidth+10f, 100f)
        game.shapeRenderer.color = Color(0.96f, 0.56f, 0.10f, 1f); game.shapeRenderer.rect(camLeft-5f, 100f, camera.viewportWidth+10f, 100f)
        game.shapeRenderer.color = Color(0.88f, 0.66f, 0.22f, 1f); game.shapeRenderer.rect(camLeft-5f, 200f, camera.viewportWidth+10f, 100f)

        // Sun
        val sunX = camLeft + camera.viewportWidth * 0.82f; val sunY = 230f
        game.shapeRenderer.color = Color(1f, 0.9f, 0.3f, 0.1f); game.shapeRenderer.circle(sunX, sunY, 55f, 20)
        game.shapeRenderer.color = Color(1f, 0.92f, 0.4f, 0.9f); game.shapeRenderer.circle(sunX, sunY, 30f, 20)
        game.shapeRenderer.color = Color(1f, 1f, 0.75f, 0.85f); game.shapeRenderer.circle(sunX, sunY, 18f, 16)

        // Background elements
        for (b in bgSils) {
            val bx = camLeft + ((b.x - camLeft * 0.12f + 10000f).mod(camera.viewportWidth + 500f))
            game.shapeRenderer.color = Color(0.68f, 0.28f, 0.04f, 0.2f)
            game.shapeRenderer.rect(bx, 60f, 35f + b.y*1.4f, b.y + 55f)
        }
        for (t in bgTrees) {
            val tx = camLeft + ((t.x - camLeft*0.28f + 10000f).mod(camera.viewportWidth + 500f))
            game.shapeRenderer.color = Color(0.42f, 0.16f, 0.03f, 0.55f)
            game.shapeRenderer.rect(tx+7f, 60f, 7f, 30f)
            game.shapeRenderer.color = Color(0.32f, 0.12f, 0.02f, 0.65f)
            game.shapeRenderer.circle(tx+10f, 96f, 20f, 12)
        }
        for (c in bgClouds) {
            val cx = camLeft + ((c.x - camLeft*0.18f + 10000f).mod(camera.viewportWidth + 600f))
            game.shapeRenderer.color = Color(1f, 0.75f, 0.35f, 0.42f)
            game.shapeRenderer.circle(cx, c.y, 18f, 12); game.shapeRenderer.circle(cx+22f, c.y-5f, 13f, 10); game.shapeRenderer.circle(cx-16f, c.y-7f, 11f, 10)
        }

        // Ground
        game.shapeRenderer.color = Color(0.28f, 0.11f, 0.02f, 1f); game.shapeRenderer.rect(camLeft-10f, 0f, camera.viewportWidth+20f, 62f)
        game.shapeRenderer.color = Color(0.40f, 0.17f, 0.05f, 1f); game.shapeRenderer.rect(camLeft-10f, 57f, camera.viewportWidth+20f, 7f)

        for (p in platforms) { if (p.bounds.x > camLeft+camera.viewportWidth+60f || p.bounds.x+p.bounds.width < camLeft-60f) continue; p.draw(game.shapeRenderer) }
        for (c in coins) { if (c.position.x > camLeft+camera.viewportWidth+30f || c.position.x < camLeft-30f) continue; c.draw(game.shapeRenderer) }
        player.draw(game.shapeRenderer)
        game.shapeRenderer.end()

        // UI overlay
        game.shapeRenderer.projectionMatrix = uiCamera.combined
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val a = 0.28f
        game.shapeRenderer.color = Color(1f, 1f, 1f, a)
        game.shapeRenderer.triangle(480f*0.05f, 270f*0.08f, 480f*0.05f, 270f*0.24f, 480f*0.14f, 270f*0.16f)
        game.shapeRenderer.triangle(480f*0.30f, 270f*0.08f, 480f*0.30f, 270f*0.24f, 480f*0.21f, 270f*0.16f)
        game.shapeRenderer.color = Color(1f, 0.85f, 0.1f, a)
        game.shapeRenderer.circle(480f*0.87f, 270f*0.16f, 26f, 16)
        // Jump label
        game.shapeRenderer.end()

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        font.data.setScale(0.85f); font.color = Color(1f, 0.95f, 0.3f, 1f)
        for (ft in floatTexts) { font.color.a = ft.life.coerceAtLeast(0f); font.draw(game.batch, ft.text, ft.x, ft.y+20f) }
        game.batch.end()

        game.batch.projectionMatrix = uiCamera.combined
        game.batch.begin()
        font.data.setScale(1.15f); font.color = Color.WHITE
        font.draw(game.batch, "SCORE: $score", 10f, 262f)
        font.draw(game.batch, "COINS: $coinCount", 10f, 244f)
        font.data.setScale(0.9f); font.color = Color(1f, 0.9f, 0.5f, 0.65f)
        font.draw(game.batch, "JUMP", 480f*0.83f, 270f*0.08f)
        if (gameOver) {
            font.data.setScale(2.5f); font.color = Color(1f, 0.25f, 0.05f, 1f)
            layout.setText(font, "GAME OVER")
            font.draw(game.batch, "GAME OVER", 240f - layout.width/2f, 175f)
            font.data.setScale(1.2f); font.color = Color.WHITE
            layout.setText(font, "Score: $score   Coins: $coinCount")
            font.draw(game.batch, "Score: $score   Coins: $coinCount", 240f - layout.width/2f, 145f)
            if (gameOverTimer > 2f) { font.color = Color(1f, 0.88f, 0.4f, 1f); layout.setText(font, "Tap to return to menu"); font.draw(game.batch, "Tap to return to menu", 240f - layout.width/2f, 115f) }
        }
        game.batch.end()
    }

    override fun resize(width: Int, height: Int) { camera.setToOrtho(false, 480f, 270f); uiCamera.setToOrtho(false, 480f, 270f) }
    override fun show() {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { font.dispose() }
}
