package com.maxi.flappybird

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameView = GameView(this)
        setContentView(gameView)

        hideSystemUI()
    }

    private fun hideSystemUI() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.insetsController?.let {

                it.hide(
                    WindowInsets.Type.statusBars() or
                            WindowInsets.Type.navigationBars()
                )

                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        } else {

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }
}

object Config {

    const val GRAVITY = 1500f
    const val FLAP_VELOCITY = -560f

    // smoother movement
    const val PIPE_SPEED = 240f

    // more separated
    const val PIPE_SPAWN_DIST = 620f

    // larger opening
    const val PIPE_GAP = 360f

    const val PIPE_WIDTH = 90f

    const val BIRD_RADIUS = 32f
    const val BIRD_X_RATIO = 0.22f

    const val GROUND_HEIGHT = 110f

    const val TARGET_FPS = 120
    const val FRAME_TIME_MS = (1000.0 / TARGET_FPS).toLong()

    const val COUNTDOWN_TIME = 3f
}

data class Pipe(
    var x: Float,
    val gapTop: Float,
    var scored: Boolean = false
)

enum class GameState {
    IDLE,
    COUNTDOWN,
    RUNNING,
    DEAD
}

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null

    private val game = FlappyGame()

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

        game.init(width.toFloat(), height.toFloat())

        gameThread = GameThread(holder, game)
        gameThread?.start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {}

    fun pause() {

        gameThread?.running = false

        try {
            gameThread?.join()
        } catch (_: Exception) {
        }

        gameThread = null
    }

    fun resume() {

        if (gameThread == null && holder.surface.isValid) {

            gameThread = GameThread(holder, game)
            gameThread?.start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_DOWN) {

            game.onTap()

            performClick()
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

class GameThread(
    private val holder: SurfaceHolder,
    private val game: FlappyGame
) : Thread() {

    @Volatile
    var running = true

    override fun run() {

        var lastTime = System.nanoTime()

        while (running) {

            val now = System.nanoTime()

            val dt =
                ((now - lastTime) / 1_000_000_000f)
                    .coerceAtMost(0.033f)

            lastTime = now

            game.update(dt)

            val canvas = holder.lockCanvas()

            if (canvas != null) {

                game.draw(canvas)

                holder.unlockCanvasAndPost(canvas)
            }

            val elapsed = (System.nanoTime() - now) / 1_000_000L

            val sleep = Config.FRAME_TIME_MS - elapsed

            if (sleep > 0) {
                sleep(sleep)
            }
        }
    }
}

class FlappyGame {

    private var sw = 0f
    private var sh = 0f

    private var groundY = 0f

    private var birdX = 0f
    private var birdY = 0f

    private var velY = 0f

    private var rotation = 0f

    private val pipes = mutableListOf<Pipe>()

    private var score = 0
    private var bestScore = 0

    private var state = GameState.IDLE

    private var countdownTimer = Config.COUNTDOWN_TIME

    private var idleTime = 0f

    private val skyPaint = Paint()

    private val pipePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val birdPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val groundPaint = Paint()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun init(w: Float, h: Float) {

        sw = w
        sh = h

        groundY = sh - Config.GROUND_HEIGHT

        birdX = sw * Config.BIRD_X_RATIO
        birdY = sh * 0.45f

        skyPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            sh,
            Color.parseColor("#5dade2"),
            Color.parseColor("#85c1e9"),
            Shader.TileMode.CLAMP
        )

        pipePaint.color = Color.parseColor("#2ecc71")

        birdPaint.color = Color.parseColor("#f1c40f")

        groundPaint.color = Color.parseColor("#58d68d")

        textPaint.apply {
            color = Color.WHITE
            textSize = 90f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        shadowPaint.apply {
            color = Color.BLACK
            alpha = 100
            textSize = 90f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        hintPaint.apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        glowPaint.apply {
            color = Color.WHITE
            alpha = 120
            textSize = 170f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            maskFilter = BlurMaskFilter(
                18f,
                BlurMaskFilter.Blur.NORMAL
            )
        }
    }

    fun onTap() {

        when (state) {

            GameState.IDLE -> {

                countdownTimer = Config.COUNTDOWN_TIME
                state = GameState.COUNTDOWN
            }

            GameState.COUNTDOWN -> {
            }

            GameState.RUNNING -> {

                velY = Config.FLAP_VELOCITY
            }

            GameState.DEAD -> {

                restart()
            }
        }
    }

    private fun restart() {

        pipes.clear()

        birdY = sh * 0.45f

        velY = 0f

        rotation = 0f

        score = 0

        countdownTimer = Config.COUNTDOWN_TIME

        state = GameState.IDLE
    }

    fun update(dt: Float) {

        idleTime += dt

        when (state) {

            GameState.IDLE -> {

                birdY =
                    sh * 0.45f +
                            sin(idleTime * 2f) * 18f
            }

            GameState.COUNTDOWN -> {

                birdY =
                    sh * 0.45f +
                            sin(idleTime * 2f) * 18f

                countdownTimer -= dt

                if (countdownTimer <= 0f) {

                    state = GameState.RUNNING

                    velY = Config.FLAP_VELOCITY
                }
            }

            GameState.RUNNING -> {

                velY += Config.GRAVITY * dt

                birdY += velY * dt

                rotation = (velY / 18f)
                    .coerceIn(-25f, 90f)

                updatePipes(dt)

                if (checkCollision()) {

                    state = GameState.DEAD

                    if (score > bestScore) {
                        bestScore = score
                    }
                }
            }

            GameState.DEAD -> {

                velY += Config.GRAVITY * dt

                birdY += velY * dt

                birdY =
                    birdY.coerceAtMost(
                        groundY - Config.BIRD_RADIUS
                    )
            }
        }
    }

    private fun updatePipes(dt: Float) {

        for (pipe in pipes) {

            pipe.x -= Config.PIPE_SPEED * dt

            if (
                !pipe.scored &&
                pipe.x + Config.PIPE_WIDTH < birdX
            ) {

                pipe.scored = true

                score++
            }
        }

        pipes.removeAll {
            it.x + Config.PIPE_WIDTH < 0f
        }

        if (
            pipes.isEmpty() ||
            pipes.last().x < sw - Config.PIPE_SPAWN_DIST
        ) {

            val minGapTop = sh * 0.18f

            val maxGapTop =
                groundY - Config.PIPE_GAP - sh * 0.18f

            val gapTop =
                Random.nextFloat() *
                        (maxGapTop - minGapTop) +
                        minGapTop

            pipes.add(
                Pipe(
                    sw + 100f,
                    gapTop
                )
            )
        }
    }

    private fun checkCollision(): Boolean {

        val r = Config.BIRD_RADIUS * 0.68f

        if (
            birdY + r >= groundY ||
            birdY - r <= 0f
        ) {
            return true
        }

        for (pipe in pipes) {

            val left = pipe.x
            val right = pipe.x + Config.PIPE_WIDTH

            if (
                birdX + r > left &&
                birdX - r < right
            ) {

                if (
                    birdY - r < pipe.gapTop ||
                    birdY + r > pipe.gapTop + Config.PIPE_GAP
                ) {
                    return true
                }
            }
        }

        return false
    }

    fun draw(canvas: Canvas) {

        drawBackground(canvas)

        drawPipes(canvas)

        drawGround(canvas)

        drawBird(canvas)

        drawUI(canvas)
    }

    private fun drawBackground(canvas: Canvas) {

        canvas.drawRect(
            0f,
            0f,
            sw,
            sh,
            skyPaint
        )
    }

    private fun drawGround(canvas: Canvas) {

        canvas.drawRect(
            0f,
            groundY,
            sw,
            sh,
            groundPaint
        )
    }

    private fun drawPipes(canvas: Canvas) {

        for (pipe in pipes) {

            canvas.drawRoundRect(
                RectF(
                    pipe.x,
                    0f,
                    pipe.x + Config.PIPE_WIDTH,
                    pipe.gapTop
                ),
                12f,
                12f,
                pipePaint
            )

            canvas.drawRoundRect(
                RectF(
                    pipe.x,
                    pipe.gapTop + Config.PIPE_GAP,
                    pipe.x + Config.PIPE_WIDTH,
                    groundY
                ),
                12f,
                12f,
                pipePaint
            )
        }
    }

    private fun drawBird(canvas: Canvas) {

        canvas.save()

        canvas.translate(birdX, birdY)

        canvas.rotate(rotation)

        canvas.drawCircle(
            0f,
            0f,
            Config.BIRD_RADIUS,
            birdPaint
        )

        val eyePaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }

        canvas.drawCircle(
            12f,
            -8f,
            8f,
            eyePaint
        )

        val pupilPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        canvas.drawCircle(
            14f,
            -8f,
            4f,
            pupilPaint
        )

        canvas.restore()
    }

    private fun drawUI(canvas: Canvas) {

        if (state == GameState.RUNNING) {

            canvas.drawText(
                score.toString(),
                sw / 2f + 4f,
                124f,
                shadowPaint
            )

            canvas.drawText(
                score.toString(),
                sw / 2f,
                120f,
                textPaint
            )
        }

        if (state == GameState.IDLE) {

            textPaint.textSize = 82f

            canvas.drawText(
                "FLAPPY BIRD",
                sw / 2f,
                sh * 0.24f,
                textPaint
            )

            hintPaint.textSize = 42f

            canvas.drawText(
                "TAP TO START",
                sw / 2f,
                sh * 0.80f,
                hintPaint
            )
        }

        if (state == GameState.COUNTDOWN) {

            val num = ceil(countdownTimer)
                .toInt()
                .coerceAtLeast(1)

            canvas.drawText(
                num.toString(),
                sw / 2f,
                sh * 0.45f,
                glowPaint
            )

            textPaint.textSize = 170f

            canvas.drawText(
                num.toString(),
                sw / 2f,
                sh * 0.45f,
                textPaint
            )

            hintPaint.textSize = 44f

            canvas.drawText(
                "GET READY",
                sw / 2f,
                sh * 0.60f,
                hintPaint
            )
        }

        if (state == GameState.DEAD) {

            textPaint.textSize = 70f

            canvas.drawText(
                "GAME OVER",
                sw / 2f,
                sh * 0.35f,
                textPaint
            )

            hintPaint.textSize = 42f

            canvas.drawText(
                "Score: $score",
                sw / 2f,
                sh * 0.48f,
                hintPaint
            )

            canvas.drawText(
                "Best: $bestScore",
                sw / 2f,
                sh * 0.56f,
                hintPaint
            )

            canvas.drawText(
                "TAP TO RESTART",
                sw / 2f,
                sh * 0.75f,
                hintPaint
            )
        }
    }
}
