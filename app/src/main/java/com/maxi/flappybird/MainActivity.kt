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

// =====================================================
// MAIN ACTIVITY
// =====================================================

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

// =====================================================
// CONFIG
// =====================================================

object Config {

    const val TARGET_FPS = 120
    const val FRAME_TIME = (1000L / TARGET_FPS)

    const val GRAVITY = 1480f
    const val FLAP_FORCE = -570f

    const val PIPE_SPEED = 250f
    const val PIPE_WIDTH = 120f
    const val PIPE_GAP = 390f
    const val PIPE_DISTANCE = 700f

    const val ROCKET_SIZE = 40f

    const val GROUND_HEIGHT = 160f

    const val COUNTDOWN = 3f
}

// =====================================================
// DATA
// =====================================================

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

// =====================================================
// GAME VIEW
// =====================================================

class GameView(context: Context) :
    SurfaceView(context),
    SurfaceHolder.Callback {

    private var thread: GameThread? = null

    private val game = RocketGame()

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

        game.init(width.toFloat(), height.toFloat())

        thread = GameThread(holder, game)
        thread?.start()
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

        thread?.running = false

        try {
            thread?.join()
        } catch (_: Exception) {
        }

        thread = null
    }

    fun resume() {

        if (thread == null && holder.surface.isValid) {

            thread = GameThread(holder, game)
            thread?.start()
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

// =====================================================
// GAME THREAD
// =====================================================

class GameThread(
    private val holder: SurfaceHolder,
    private val game: RocketGame
) : Thread() {

    @Volatile
    var running = true

    override fun run() {

        var last = System.nanoTime()

        while (running) {

            val now = System.nanoTime()

            val dt =
                ((now - last) / 1_000_000_000f)
                    .coerceAtMost(0.033f)

            last = now

            game.update(dt)

            val canvas = holder.lockCanvas()

            if (canvas != null) {

                game.draw(canvas)

                holder.unlockCanvasAndPost(canvas)
            }

            val elapsed =
                (System.nanoTime() - now) / 1_000_000L

            val sleep =
                Config.FRAME_TIME - elapsed

            if (sleep > 0) {
                sleep(sleep)
            }
        }
    }
}

// =====================================================
// GAME
// =====================================================

class RocketGame {

    private var sw = 0f
    private var sh = 0f

    private var groundY = 0f

    // rocket

    private var rocketX = 0f
    private var rocketY = 0f

    private var velocityY = 0f

    private var rotation = 0f

    // state

    private var state = GameState.IDLE

    private var countdown = Config.COUNTDOWN

    // gameplay

    private val pipes = mutableListOf<Pipe>()

    private var score = 0
    private var bestScore = 0

    // animation

    private var time = 0f

    private var bgScroll = 0f

    private var flash = 0f

    // paints

    private val skyPaint = Paint()

    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mountainPaint = Paint()

    private val hillPaint = Paint()

    private val pipePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val pipeDark = Paint(Paint.ANTI_ALIAS_FLAG)

    private val grassPaint = Paint()

    private val dirtPaint = Paint()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val rocketPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun init(w: Float, h: Float) {

        sw = w
        sh = h

        groundY = sh - Config.GROUND_HEIGHT

        rocketX = sw * 0.24f
        rocketY = sh * 0.45f

        // sky

        skyPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            sh,
            Color.parseColor("#5ed0ff"),
            Color.parseColor("#b9f1ff"),
            Shader.TileMode.CLAMP
        )

        cloudPaint.color = Color.WHITE

        mountainPaint.color =
            Color.parseColor("#88b8d8")

        hillPaint.color =
            Color.parseColor("#5fbf5f")

        pipePaint.color =
            Color.parseColor("#32d74b")

        pipeDark.color =
            Color.parseColor("#1ea336")

        grassPaint.color =
            Color.parseColor("#4cff5d")

        dirtPaint.color =
            Color.parseColor("#b86f2d")

        textPaint.apply {

            color = Color.WHITE
            textSize = 90f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        outlinePaint.apply {

            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 10f
            textSize = 90f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        rocketPaint.color = Color.WHITE

        wingPaint.color =
            Color.parseColor("#ff3b30")

        glassPaint.color =
            Color.parseColor("#6fd9ff")

        whitePaint.color = Color.WHITE
    }

    // =====================================================
    // INPUT
    // =====================================================

    fun onTap() {

        when (state) {

            GameState.IDLE -> {

                countdown = Config.COUNTDOWN
                state = GameState.COUNTDOWN
            }

            GameState.COUNTDOWN -> {}

            GameState.RUNNING -> {

                velocityY = Config.FLAP_FORCE

                flash = 0.08f
            }

            GameState.DEAD -> {

                restart()
            }
        }
    }

    private fun restart() {

        state = GameState.IDLE

        score = 0

        pipes.clear()

        velocityY = 0f

        rocketY = sh * 0.45f

        rotation = 0f
    }

    // =====================================================
    // UPDATE
    // =====================================================

    fun update(dt: Float) {

        time += dt

        bgScroll += Config.PIPE_SPEED * dt

        flash = (flash - dt * 2f)
            .coerceAtLeast(0f)

        when (state) {

            GameState.IDLE -> {

                rocketY =
                    sh * 0.45f +
                            sin(time * 3f) * 16f
            }

            GameState.COUNTDOWN -> {

                rocketY =
                    sh * 0.45f +
                            sin(time * 3f) * 16f

                countdown -= dt

                if (countdown <= 0f) {

                    state = GameState.RUNNING

                    velocityY = Config.FLAP_FORCE
                }
            }

            GameState.RUNNING -> {

                velocityY += Config.GRAVITY * dt

                rocketY += velocityY * dt

                rotation =
                    (velocityY / 16f)
                        .coerceIn(-28f, 88f)

                updatePipes(dt)

                if (checkCollision()) {

                    state = GameState.DEAD

                    if (score > bestScore) {
                        bestScore = score
                    }
                }
            }

            GameState.DEAD -> {

                velocityY += Config.GRAVITY * dt

                rocketY += velocityY * dt

                rotation = 90f
            }
        }
    }

    private fun updatePipes(dt: Float) {

        for (pipe in pipes) {

            pipe.x -= Config.PIPE_SPEED * dt

            if (
                !pipe.scored &&
                pipe.x + Config.PIPE_WIDTH < rocketX
            ) {

                pipe.scored = true

                score++
            }
        }

        pipes.removeAll {
            it.x + Config.PIPE_WIDTH < -50f
        }

        if (
            pipes.isEmpty() ||
            pipes.last().x <
            sw - Config.PIPE_DISTANCE
        ) {

            val minGap =
                sh * 0.16f

            val maxGap =
                groundY -
                        Config.PIPE_GAP -
                        sh * 0.16f

            val gapTop =
                Random.nextFloat() *
                        (maxGap - minGap) +
                        minGap

            pipes.add(
                Pipe(
                    sw + 150f,
                    gapTop
                )
            )
        }
    }

    private fun checkCollision(): Boolean {

        val r =
            Config.ROCKET_SIZE * 0.58f

        if (
            rocketY + r >= groundY ||
            rocketY - r <= 0f
        ) {
            return true
        }

        for (pipe in pipes) {

            val left = pipe.x
            val right =
                pipe.x + Config.PIPE_WIDTH

            if (
                rocketX + r > left &&
                rocketX - r < right
            ) {

                if (
                    rocketY - r < pipe.gapTop ||
                    rocketY + r >
                    pipe.gapTop + Config.PIPE_GAP
                ) {
                    return true
                }
            }
        }

        return false
    }

    // =====================================================
    // DRAW
    // =====================================================

    fun draw(canvas: Canvas) {

        drawSky(canvas)

        drawMountains(canvas)

        drawClouds(canvas)

        drawPipes(canvas)

        drawGround(canvas)

        drawRocket(canvas)

        drawUI(canvas)

        drawBoostFlash(canvas)
    }

    // =====================================================
    // BACKGROUND
    // =====================================================

    private fun drawSky(canvas: Canvas) {

        canvas.drawRect(
            0f,
            0f,
            sw,
            sh,
            skyPaint
        )
    }

    private fun drawMountains(canvas: Canvas) {

        val mountainWidth = 420f

        var x =
            -(bgScroll * 0.15f % mountainWidth)

        while (x < sw + mountainWidth) {

            val path = Path()

            path.moveTo(x, groundY)

            path.lineTo(
                x + mountainWidth / 2f,
                groundY - 260f
            )

            path.lineTo(
                x + mountainWidth,
                groundY
            )

            path.close()

            canvas.drawPath(
                path,
                mountainPaint
            )

            x += mountainWidth
        }
    }

    private fun drawClouds(canvas: Canvas) {

        for (i in 0..5) {

            val x =
                ((i * 350f) -
                        (bgScroll * 0.25f % 2000f))

            val y =
                110f +
                        sin(time + i) * 20f

            drawCloud(
                canvas,
                x,
                y
            )
        }
    }

    private fun drawCloud(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

        canvas.drawCircle(
            x,
            y,
            38f,
            cloudPaint
        )

        canvas.drawCircle(
            x + 38f,
            y - 10f,
            48f,
            cloudPaint
        )

        canvas.drawCircle(
            x + 86f,
            y,
            38f,
            cloudPaint
        )

        canvas.drawRect(
            x,
            y,
            x + 86f,
            y + 35f,
            cloudPaint
        )
    }

    // =====================================================
    // PIPES
    // =====================================================

    private fun drawPipes(canvas: Canvas) {

        for (pipe in pipes) {

            drawPipe(
                canvas,
                pipe.x,
                0f,
                pipe.gapTop
            )

            drawPipe(
                canvas,
                pipe.x,
                pipe.gapTop + Config.PIPE_GAP,
                groundY
            )
        }
    }

    private fun drawPipe(
        canvas: Canvas,
        x: Float,
        top: Float,
        bottom: Float
    ) {

        canvas.drawRect(
            x,
            top,
            x + Config.PIPE_WIDTH,
            bottom,
            pipeDark
        )

        canvas.drawRect(
            x + 12f,
            top,
            x + Config.PIPE_WIDTH - 12f,
            bottom,
            pipePaint
        )

        canvas.drawRect(
            x - 10f,
            bottom - 34f,
            x + Config.PIPE_WIDTH + 10f,
            bottom,
            pipeDark
        )

        canvas.drawRect(
            x - 4f,
            bottom - 26f,
            x + Config.PIPE_WIDTH + 4f,
            bottom - 6f,
            pipePaint
        )
    }

    // =====================================================
    // GROUND
    // =====================================================

    private fun drawGround(canvas: Canvas) {

        canvas.drawRect(
            0f,
            groundY,
            sw,
            sh,
            dirtPaint
        )

        canvas.drawRect(
            0f,
            groundY,
            sw,
            groundY + 28f,
            grassPaint
        )

        var x =
            -(bgScroll % 64f)

        while (x < sw) {

            canvas.drawRect(
                x,
                groundY + 28f,
                x + 32f,
                groundY + 72f,
                Paint().apply {
                    color =
                        Color.parseColor("#d38b42")
                }
            )

            x += 64f
        }
    }

    // =====================================================
    // ROCKET
    // =====================================================

    private fun drawRocket(canvas: Canvas) {

        val flame =
            sin(time * 32f) * 14f

        canvas.save()

        canvas.translate(
            rocketX,
            rocketY
        )

        canvas.rotate(rotation)

        // flame outer

        flamePaint.color =
            Color.parseColor("#ff9500")

        canvas.drawOval(
            RectF(
                -88f - flame,
                -14f,
                -34f,
                14f
            ),
            flamePaint
        )

        // flame inner

        flamePaint.color =
            Color.parseColor("#fff700")

        canvas.drawOval(
            RectF(
                -74f - flame,
                -8f,
                -38f,
                8f
            ),
            flamePaint
        )

        // body

        canvas.drawRoundRect(
            RectF(
                -38f,
                -20f,
                40f,
                20f
            ),
            20f,
            20f,
            rocketPaint
        )

        // nose

        val nose = Path()

        nose.moveTo(40f, -20f)
        nose.lineTo(74f, 0f)
        nose.lineTo(40f, 20f)
        nose.close()

        canvas.drawPath(
            nose,
            wingPaint
        )

        // top wing

        val topWing = Path()

        topWing.moveTo(-10f, -18f)
        topWing.lineTo(-38f, -42f)
        topWing.lineTo(6f, -18f)

        canvas.drawPath(
            topWing,
            wingPaint
        )

        // bottom wing

        val bottomWing = Path()

        bottomWing.moveTo(-10f, 18f)
        bottomWing.lineTo(-38f, 42f)
        bottomWing.lineTo(6f, 18f)

        canvas.drawPath(
            bottomWing,
            wingPaint
        )

        // window

        canvas.drawCircle(
            12f,
            0f,
            12f,
            glassPaint
        )

        canvas.drawCircle(
            8f,
            -4f,
            4f,
            whitePaint
        )

        canvas.restore()
    }

    // =====================================================
    // UI
    // =====================================================

    private fun drawUI(canvas: Canvas) {

        if (
            state == GameState.RUNNING ||
            state == GameState.COUNTDOWN
        ) {

            drawOutlinedText(
                canvas,
                score.toString(),
                sw / 2f,
                120f,
                96f
            )
        }

        if (state == GameState.IDLE) {

            drawOutlinedText(
                canvas,
                "ROCKET DASH",
                sw / 2f,
                sh * 0.24f,
                84f
            )

            drawOutlinedText(
                canvas,
                "TAP TO START",
                sw / 2f,
                sh * 0.82f,
                42f
            )
        }

        if (state == GameState.COUNTDOWN) {

            val num =
                ceil(countdown)
                    .toInt()
                    .coerceAtLeast(1)

            drawOutlinedText(
                canvas,
                num.toString(),
                sw / 2f,
                sh * 0.46f,
                180f
            )
        }

        if (state == GameState.DEAD) {

            drawOutlinedText(
                canvas,
                "GAME OVER",
                sw / 2f,
                sh * 0.34f,
                74f
            )

            drawOutlinedText(
                canvas,
                "SCORE: $score",
                sw / 2f,
                sh * 0.48f,
                44f
            )

            drawOutlinedText(
                canvas,
                "BEST: $bestScore",
                sw / 2f,
                sh * 0.56f,
                44f
            )

            drawOutlinedText(
                canvas,
                "TAP TO RESTART",
                sw / 2f,
                sh * 0.76f,
                42f
            )
        }
    }

    private fun drawOutlinedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float
    ) {

        textPaint.textSize = size
        outlinePaint.textSize = size

        canvas.drawText(
            text,
            x,
            y,
            outlinePaint
        )

        canvas.drawText(
            text,
            x,
            y,
            textPaint
        )
    }

    // =====================================================
    // BOOST FLASH
    // =====================================================

    private fun drawBoostFlash(canvas: Canvas) {

        if (flash <= 0f) return

        val paint = Paint()

        paint.color =
            Color.argb(
                (flash * 90f).toInt(),
                255,
                255,
                255
            )

        canvas.drawRect(
            0f,
            0f,
            sw,
            sh,
            paint
        )
    }
}
