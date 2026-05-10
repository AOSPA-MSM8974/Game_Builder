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
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
    const val TARGET_FPS        = 120
    const val FRAME_TIME        = (1000L / TARGET_FPS)

    const val GRAVITY           = 1500f
    const val FLAP_FORCE        = -560f

    const val PIPE_SPEED        = 260f
    const val PIPE_WIDTH        = 110f
    const val PIPE_GAP          = 400f
    const val PIPE_DISTANCE     = 680f

    const val ROCKET_SIZE       = 40f

    const val GROUND_HEIGHT     = 160f
    const val COUNTDOWN         = 3f

    // screen-shake
    const val SHAKE_DURATION    = 0.35f
    const val SHAKE_MAGNITUDE   = 14f
}

// =====================================================
// DATA
// =====================================================

data class Pipe(
    var x: Float,
    val gapTop: Float,
    var scored: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,       // 0-1
    var maxLife: Float,
    val color: Int,
    val size: Float,
    val type: ParticleType
)

enum class ParticleType { SPARK, SMOKE, STAR }

enum class GameState { IDLE, COUNTDOWN, RUNNING, DEAD }

// =====================================================
// GAME VIEW
// =====================================================

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

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

    override fun surfaceDestroyed(holder: SurfaceHolder) { pause() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    fun pause() {
        thread?.running = false
        try { thread?.join() } catch (_: Exception) {}
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

    @Volatile var running = true

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.033f)
            last = now

            game.update(dt)

            val canvas = holder.lockCanvas()
            if (canvas != null) {
                game.draw(canvas)
                holder.unlockCanvasAndPost(canvas)
            }

            val elapsed = (System.nanoTime() - now) / 1_000_000L
            val sleep = Config.FRAME_TIME - elapsed
            if (sleep > 0) sleep(sleep)
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
    private var targetRotation = 0f

    // state
    private var state = GameState.IDLE
    private var countdown = Config.COUNTDOWN

    // gameplay
    private val pipes = mutableListOf<Pipe>()
    private var score = 0
    private var bestScore = 0

    // animation
    private var time = 0f
    private var bgScroll1 = 0f   // far mountains
    private var bgScroll2 = 0f   // mid hills
    private var bgScroll3 = 0f   // ground parallax

    // effects
    private var flash = 0f
    private var shakeTimer = 0f
    private var shakeX = 0f
    private var shakeY = 0f

    // particles
    private val particles = mutableListOf<Particle>()

    // score pop animation
    private var scorePop = 0f

    // death panel slide
    private var deathPanelY = 0f
    private var deathTimer = 0f

    // stars for background
    private val stars = mutableListOf<Triple<Float, Float, Float>>() // x, y, brightness

    // ──────────────────────────────────────────────
    // PAINTS
    // ──────────────────────────────────────────────

    private val skyPaint       = Paint()
    private val cloudPaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mountainPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hillPaint      = Paint(Paint.ANTI_ALIAS_FLAG)

    // pipes
    private val pipeBodyPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipeEdgePaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipeCapPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipeCapEdge    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipeShine      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipeShadow     = Paint(Paint.ANTI_ALIAS_FLAG)

    // ground
    private val grassPaint     = Paint()
    private val dirtPaint      = Paint()
    private val dirtStripePaint= Paint()

    // rocket
    private val rocketBodyPaint= Paint(Paint.ANTI_ALIAS_FLAG)
    private val rocketShadePaint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val wingPaint      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wingShade      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glassPaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glassShine     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flamePaint     = Paint(Paint.ANTI_ALIAS_FLAG)

    // ui
    private val textPaint      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelPaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelBorder    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val medalPaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flashPaint     = Paint()
    private val particlePaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tapPulsePaint  = Paint(Paint.ANTI_ALIAS_FLAG)

    // ──────────────────────────────────────────────
    // INIT
    // ──────────────────────────────────────────────

    fun init(w: Float, h: Float) {
        sw = w; sh = h
        groundY = sh - Config.GROUND_HEIGHT
        rocketX = sw * 0.24f
        rocketY = sh * 0.45f

        // Sky gradient
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, sh,
            intArrayOf(
                Color.parseColor("#1a1a3e"),
                Color.parseColor("#16213e"),
                Color.parseColor("#0f3460"),
                Color.parseColor("#533483"),
                Color.parseColor("#e94560")
            ),
            floatArrayOf(0f, 0.25f, 0.55f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )

        cloudPaint.color = Color.argb(200, 255, 255, 255)

        mountainPaint.color = Color.parseColor("#2a1a5e")
        hillPaint.color = Color.parseColor("#1e3a5f")

        // Pipes – green with depth
        pipeBodyPaint.color  = Color.parseColor("#3ddc5d")
        pipeEdgePaint.color  = Color.parseColor("#1ea336")
        pipeCapPaint.color   = Color.parseColor("#44e866")
        pipeCapEdge.color    = Color.parseColor("#1a8f30")
        pipeShadow.color     = Color.parseColor("#155e22")
        pipeShine.apply {
            color = Color.argb(80, 255, 255, 255)
        }

        // Ground
        grassPaint.color  = Color.parseColor("#2ecc40")
        dirtPaint.color   = Color.parseColor("#8b5e3c")
        dirtStripePaint.color = Color.parseColor("#7a4f30")

        // Rocket body
        rocketBodyPaint.color  = Color.parseColor("#f0f0f8")
        rocketShadePaint.color = Color.parseColor("#c8c8e0")
        wingPaint.color        = Color.parseColor("#ff3b30")
        wingShade.color        = Color.parseColor("#c0211a")
        glassPaint.color       = Color.parseColor("#7ee8ff")
        glassShine.color       = Color.argb(200, 255, 255, 255)

        flamePaint.color = Color.parseColor("#ff9500")

        // UI
        textPaint.apply {
            color = Color.WHITE
            textSize = 96f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 3f, 3f, Color.argb(120, 0, 0, 0))
        }
        outlinePaint.apply {
            color = Color.parseColor("#1a1a3e")
            style = Paint.Style.STROKE
            strokeWidth = 12f
            textSize = 96f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            strokeJoin = Paint.Join.ROUND
        }
        subTextPaint.apply {
            color = Color.parseColor("#ffe066")
            textSize = 42f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        panelPaint.apply {
            color = Color.argb(230, 20, 20, 50)
        }
        panelBorder.apply {
            color = Color.parseColor("#6a5acd")
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        dividerPaint.apply {
            color = Color.argb(80, 255, 255, 255)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        medalPaint.apply {
            color = Color.parseColor("#ffd700")
        }
        flashPaint.color = Color.WHITE
        particlePaint.apply { style = Paint.Style.FILL }
        starPaint.apply { color = Color.WHITE }
        tapPulsePaint.apply {
            color = Color.argb(180, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        // Generate stars
        repeat(80) {
            stars.add(Triple(
                Random.nextFloat() * sw,
                Random.nextFloat() * sh * 0.65f,
                Random.nextFloat()
            ))
        }
    }

    // ──────────────────────────────────────────────
    // INPUT
    // ──────────────────────────────────────────────

    fun onTap() {
        when (state) {
            GameState.IDLE -> {
                countdown = Config.COUNTDOWN
                state = GameState.COUNTDOWN
            }
            GameState.COUNTDOWN -> {}
            GameState.RUNNING -> {
                velocityY = Config.FLAP_FORCE
                flash = 0.06f
                spawnFlapParticles()
            }
            GameState.DEAD -> {
                if (deathTimer > 0.4f) restart()
            }
        }
    }

    private fun restart() {
        state = GameState.IDLE
        score = 0
        pipes.clear()
        particles.clear()
        velocityY = 0f
        rocketY = sh * 0.45f
        rotation = 0f
        targetRotation = 0f
        deathTimer = 0f
        deathPanelY = sh + 200f
    }

    // ──────────────────────────────────────────────
    // UPDATE
    // ──────────────────────────────────────────────

    fun update(dt: Float) {
        time += dt
        val scrollDt = dt * Config.PIPE_SPEED

        bgScroll1 += scrollDt * 0.08f
        bgScroll2 += scrollDt * 0.18f
        bgScroll3 += scrollDt

        flash = (flash - dt * 3f).coerceAtLeast(0f)

        // Screen shake decay
        if (shakeTimer > 0f) {
            shakeTimer = (shakeTimer - dt).coerceAtLeast(0f)
            val mag = shakeTimer / Config.SHAKE_DURATION * Config.SHAKE_MAGNITUDE
            shakeX = (Random.nextFloat() * 2f - 1f) * mag
            shakeY = (Random.nextFloat() * 2f - 1f) * mag
        } else {
            shakeX = 0f; shakeY = 0f
        }

        // Score pop decay
        scorePop = (scorePop - dt * 4f).coerceAtLeast(0f)

        updateParticles(dt)

        when (state) {
            GameState.IDLE -> {
                rocketY = sh * 0.45f + sin(time * 2.8f) * 18f
                rotation = sin(time * 2.8f) * 6f
            }
            GameState.COUNTDOWN -> {
                rocketY = sh * 0.45f + sin(time * 2.8f) * 18f
                rotation = sin(time * 2.8f) * 6f
                countdown -= dt
                if (countdown <= 0f) {
                    state = GameState.RUNNING
                    velocityY = Config.FLAP_FORCE
                    spawnFlapParticles()
                }
            }
            GameState.RUNNING -> {
                velocityY += Config.GRAVITY * dt
                rocketY += velocityY * dt

                // Smooth rotation: aim for velocity-based angle
                targetRotation = (velocityY / 14f).coerceIn(-30f, 90f)
                rotation += (targetRotation - rotation) * (1f - exp(-18f * dt))

                updatePipes(dt)

                if (checkCollision()) {
                    state = GameState.DEAD
                    if (score > bestScore) bestScore = score
                    shakeTimer = Config.SHAKE_DURATION
                    flash = 0.4f
                    spawnDeathParticles()
                    deathPanelY = sh + 200f
                    deathTimer = 0f
                }
            }
            GameState.DEAD -> {
                deathTimer += dt

                // Rocket tumbles off screen
                if (rocketY < sh + 100f) {
                    velocityY += Config.GRAVITY * dt
                    rocketY += velocityY * dt
                    rotation += 360f * dt
                }

                // Panel slides up after short delay
                if (deathTimer > 0.6f) {
                    val targetY = sh * 0.28f
                    deathPanelY += (targetY - deathPanelY) * (1f - exp(-12f * dt))
                }
            }
        }
    }

    private fun updatePipes(dt: Float) {
        for (pipe in pipes) {
            pipe.x -= Config.PIPE_SPEED * dt
            if (!pipe.scored && pipe.x + Config.PIPE_WIDTH < rocketX) {
                pipe.scored = true
                score++
                scorePop = 1f
                spawnScoreParticles()
            }
        }
        pipes.removeAll { it.x + Config.PIPE_WIDTH < -100f }

        if (pipes.isEmpty() || pipes.last().x < sw - Config.PIPE_DISTANCE) {
            val minGap = sh * 0.15f
            val maxGap = groundY - Config.PIPE_GAP - sh * 0.15f
            val gapTop = Random.nextFloat() * (maxGap - minGap) + minGap
            pipes.add(Pipe(sw + 150f, gapTop))
        }
    }

    private fun checkCollision(): Boolean {
        val r = Config.ROCKET_SIZE * 0.56f
        if (rocketY + r >= groundY || rocketY - r <= 0f) return true
        for (pipe in pipes) {
            val left = pipe.x
            val right = pipe.x + Config.PIPE_WIDTH
            if (rocketX + r > left && rocketX - r < right) {
                if (rocketY - r < pipe.gapTop || rocketY + r > pipe.gapTop + Config.PIPE_GAP)
                    return true
            }
        }
        return false
    }

    // ──────────────────────────────────────────────
    // PARTICLES
    // ──────────────────────────────────────────────

    private fun spawnFlapParticles() {
        repeat(10) {
            val angle = Random.nextFloat() * PI.toFloat() + PI.toFloat() / 2f // downward fan
            val speed = Random.nextFloat() * 220f + 80f
            particles.add(Particle(
                x = rocketX - 30f,
                y = rocketY,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 1f,
                maxLife = 1f,
                color = if (Random.nextBoolean()) Color.parseColor("#ff9500") else Color.parseColor("#fff700"),
                size = Random.nextFloat() * 8f + 4f,
                type = ParticleType.SPARK
            ))
        }
        // Smoke puffs
        repeat(5) {
            particles.add(Particle(
                x = rocketX - 40f + Random.nextFloat() * 10f,
                y = rocketY + Random.nextFloat() * 10f - 5f,
                vx = -Random.nextFloat() * 60f - 20f,
                vy = (Random.nextFloat() - 0.5f) * 50f,
                life = 1f,
                maxLife = 0.6f,
                color = Color.argb(160, 200, 200, 200),
                size = Random.nextFloat() * 18f + 10f,
                type = ParticleType.SMOKE
            ))
        }
    }

    private fun spawnDeathParticles() {
        repeat(24) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 400f + 150f
            val colors = listOf(
                Color.parseColor("#ff3b30"),
                Color.parseColor("#ff9500"),
                Color.parseColor("#fff700"),
                Color.WHITE
            )
            particles.add(Particle(
                x = rocketX, y = rocketY,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 1f,
                maxLife = 1f + Random.nextFloat() * 0.5f,
                color = colors[Random.nextInt(colors.size)],
                size = Random.nextFloat() * 12f + 5f,
                type = ParticleType.SPARK
            ))
        }
    }

    private fun spawnScoreParticles() {
        repeat(8) {
            val angle = -PI.toFloat() / 2f + (Random.nextFloat() - 0.5f) * PI.toFloat()
            val speed = Random.nextFloat() * 180f + 80f
            particles.add(Particle(
                x = rocketX + Random.nextFloat() * 30f,
                y = rocketY - 20f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 1f,
                maxLife = 0.7f,
                color = Color.parseColor("#ffd700"),
                size = Random.nextFloat() * 8f + 4f,
                type = ParticleType.STAR
            ))
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt / p.maxLife
            if (p.life <= 0f) { iter.remove(); continue }
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (p.type == ParticleType.SPARK) p.vy += 600f * dt
            if (p.type == ParticleType.SMOKE) { p.vx *= (1f - dt * 3f); p.size *= (1f + dt * 1.5f) }
        }
    }

    // ──────────────────────────────────────────────
    // DRAW
    // ──────────────────────────────────────────────

    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(shakeX, shakeY)

        drawSky(canvas)
        drawStars(canvas)
        drawMountains(canvas)
        drawHills(canvas)
        drawClouds(canvas)
        drawPipes(canvas)
        drawGround(canvas)
        drawParticles(canvas)
        drawRocket(canvas)
        drawUI(canvas)
        drawBoostFlash(canvas)

        canvas.restore()
    }

    // ──────────────────────────────────────────────
    // BACKGROUND
    // ──────────────────────────────────────────────

    private fun drawSky(canvas: Canvas) {
        canvas.drawRect(0f, 0f, sw, sh, skyPaint)
    }

    private fun drawStars(canvas: Canvas) {
        for ((x, y, brightness) in stars) {
            val twinkle = (0.5f + 0.5f * sin(time * 2.5f + brightness * 10f))
            starPaint.alpha = (twinkle * 200f + 55f).toInt()
            val r = 1.5f + brightness * 2.5f
            canvas.drawCircle(x, y, r, starPaint)
        }
    }

    private fun drawMountains(canvas: Canvas) {
        val w = 500f
        var x = -(bgScroll1 % w)
        while (x < sw + w) {
            // Back layer (darker, taller)
            mountainPaint.color = Color.parseColor("#1a1040")
            val p1 = Path().apply {
                moveTo(x, groundY)
                lineTo(x + w * 0.35f, groundY - 340f)
                lineTo(x + w * 0.7f, groundY - 200f)
                lineTo(x + w, groundY)
                close()
            }
            canvas.drawPath(p1, mountainPaint)

            // Snow cap
            val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, 255, 255, 255) }
            val p2 = Path().apply {
                val peak = x + w * 0.35f
                moveTo(peak - 30f, groundY - 280f)
                lineTo(peak, groundY - 340f)
                lineTo(peak + 30f, groundY - 280f)
                close()
            }
            canvas.drawPath(p2, snowPaint)

            x += w
        }
    }

    private fun drawHills(canvas: Canvas) {
        val w = 360f
        var x = -(bgScroll2 % w) - w
        hillPaint.color = Color.parseColor("#0d2137")
        while (x < sw + w) {
            val p = Path().apply {
                moveTo(x, groundY)
                cubicTo(
                    x + w * 0.15f, groundY - 180f,
                    x + w * 0.85f, groundY - 180f,
                    x + w, groundY
                )
                close()
            }
            canvas.drawPath(p, hillPaint)
            x += w * 0.9f
        }
    }

    private fun drawClouds(canvas: Canvas) {
        for (i in 0..7) {
            val spacing = 420f
            val totalWidth = 8 * spacing + 250f
            val rawX = i * spacing - (bgScroll2 * 0.3f % totalWidth)
            val x = ((rawX % totalWidth) + totalWidth) % totalWidth - 250f
            val y = 80f + (i % 3) * 90f + sin(time * 0.6f + i) * 14f
            val alpha = (160 + i * 10).coerceAtMost(220)
            cloudPaint.alpha = alpha
            drawCloud(canvas, x, y, 0.7f + (i % 3) * 0.25f)
        }
    }

    private fun drawCloud(canvas: Canvas, x: Float, y: Float, scale: Float) {
        val s = scale
        canvas.drawCircle(x, y, 36f * s, cloudPaint)
        canvas.drawCircle(x + 38f * s, y - 14f * s, 50f * s, cloudPaint)
        canvas.drawCircle(x + 86f * s, y, 36f * s, cloudPaint)
        canvas.drawRect(x, y, x + 86f * s, y + 34f * s, cloudPaint)
    }

    // ──────────────────────────────────────────────
    // PIPES  (fully redrawn with depth + clean caps)
    // ──────────────────────────────────────────────

    private fun drawPipes(canvas: Canvas) {
        for (pipe in pipes) {
            // Top pipe (hangs from top)
            drawPipeSegment(canvas, pipe.x, 0f, pipe.gapTop, isTop = true)
            // Bottom pipe (rises from gap bottom)
            drawPipeSegment(canvas, pipe.x, pipe.gapTop + Config.PIPE_GAP, groundY, isTop = false)
        }
    }

    private fun drawPipeSegment(
        canvas: Canvas,
        x: Float,
        top: Float,
        bottom: Float,
        isTop: Boolean
    ) {
        val pw  = Config.PIPE_WIDTH
        val capH = 36f       // cap (lip) height
        val capW = pw + 22f  // cap is wider than pipe body
        val capX = x - 11f   // centered on pipe

        // ── BODY ──────────────────────────────────

        // Shadow side (right)
        pipeShadow.shader = null
        canvas.drawRect(x + pw - 18f, top, x + pw, bottom, pipeShadow)

        // Main body gradient (left bright → right dark)
        pipeBodyPaint.shader = LinearGradient(
            x, 0f, x + pw, 0f,
            intArrayOf(
                Color.parseColor("#50e870"),
                Color.parseColor("#3ddc5d"),
                Color.parseColor("#2ab848"),
                Color.parseColor("#1ea336")
            ),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(x, top, x + pw, bottom, pipeBodyPaint)

        // Shine strip (left edge highlight)
        pipeShine.shader = LinearGradient(
            x, 0f, x + 22f, 0f,
            Color.argb(90, 255, 255, 255),
            Color.argb(0, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(x, top, x + 22f, bottom, pipeShine)

        // ── CAP (lip) ──────────────────────────────

        val capTop: Float
        val capBottom: Float
        if (isTop) {
            // Cap sits at the bottom of the top pipe
            capTop    = bottom - capH
            capBottom = bottom
        } else {
            // Cap sits at the top of the bottom pipe
            capTop    = top
            capBottom = top + capH
        }

        // Cap shadow
        pipeShadow.shader = null
        canvas.drawRoundRect(
            RectF(capX + capW - 20f, capTop, capX + capW, capBottom),
            6f, 6f, pipeShadow
        )

        // Cap body gradient
        pipeCapPaint.shader = LinearGradient(
            capX, 0f, capX + capW, 0f,
            intArrayOf(
                Color.parseColor("#5cf07a"),
                Color.parseColor("#44e866"),
                Color.parseColor("#2ecc52"),
                Color.parseColor("#1a8f30")
            ),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(
            RectF(capX, capTop, capX + capW, capBottom),
            8f, 8f, pipeCapPaint
        )

        // Cap shine
        pipeShine.shader = LinearGradient(
            capX, 0f, capX + 28f, 0f,
            Color.argb(100, 255, 255, 255),
            Color.argb(0, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(
            RectF(capX, capTop, capX + 28f, capBottom),
            8f, 8f, pipeShine
        )

        // Cap top/bottom edge line
        pipeCapEdge.shader = null
        pipeCapEdge.style = Paint.Style.STROKE
        pipeCapEdge.strokeWidth = 3f
        pipeCapEdge.color = Color.parseColor("#155e22")
        canvas.drawRoundRect(
            RectF(capX, capTop, capX + capW, capBottom),
            8f, 8f, pipeCapEdge
        )

        // ── HORIZONTAL RING DETAIL on body ────────
        pipeEdgePaint.style = Paint.Style.STROKE
        pipeEdgePaint.strokeWidth = 5f
        pipeEdgePaint.color = Color.parseColor("#1ea336")

        val ringCount = 3
        val bodyLen = abs(bottom - top) - capH
        if (bodyLen > 60f) {
            val step = bodyLen / (ringCount + 1)
            for (r in 1..ringCount) {
                val ry = if (isTop) top + step * r else top + capH + step * r
                canvas.drawLine(x, ry, x + pw, ry, pipeEdgePaint)
            }
        }
    }

    // ──────────────────────────────────────────────
    // GROUND
    // ──────────────────────────────────────────────

    private fun drawGround(canvas: Canvas) {
        // Dirt
        canvas.drawRect(0f, groundY, sw, sh, dirtPaint)

        // Grass stripe
        canvas.drawRect(0f, groundY, sw, groundY + 30f, grassPaint)

        // Dirt texture stripes
        var x = -(bgScroll3 % 80f)
        while (x < sw) {
            canvas.drawRect(x, groundY + 32f, x + 40f, groundY + 80f, dirtStripePaint)
            x += 80f
        }

        // Grass tufts
        x = -(bgScroll3 % 55f)
        val tuftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#26a831") }
        while (x < sw) {
            canvas.drawRoundRect(RectF(x, groundY - 6f, x + 10f, groundY + 4f), 5f, 5f, tuftPaint)
            canvas.drawRoundRect(RectF(x + 15f, groundY - 10f, x + 26f, groundY + 4f), 5f, 5f, tuftPaint)
            x += 55f
        }
    }

    // ──────────────────────────────────────────────
    // ROCKET
    // ──────────────────────────────────────────────

    private fun drawRocket(canvas: Canvas) {
        if (state == GameState.DEAD && rocketY > sh + 100f) return

        canvas.save()
        canvas.translate(rocketX, rocketY)
        canvas.rotate(rotation)

        // ── FLAME ─────────────────────────────────
        val flicker = sin(time * 40f) * 0.2f + sin(time * 27f) * 0.15f
        val flameLen = 54f + flicker * 20f

        // Outer flame
        flamePaint.shader = LinearGradient(
            -80f, 0f, -34f, 0f,
            Color.parseColor("#ff4500"),
            Color.argb(0, 255, 80, 0),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(-80f - flameLen, -15f, -30f, 15f), flamePaint)

        // Mid flame
        flamePaint.shader = LinearGradient(
            -66f, 0f, -34f, 0f,
            Color.parseColor("#ff9500"),
            Color.argb(0, 255, 180, 0),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(-66f - flameLen * 0.75f, -10f, -30f, 10f), flamePaint)

        // Inner white core
        flamePaint.shader = LinearGradient(
            -52f, 0f, -36f, 0f,
            Color.WHITE,
            Color.argb(0, 255, 240, 100),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(-52f - flameLen * 0.4f, -5f, -32f, 5f), flamePaint)

        // ── BODY ──────────────────────────────────
        // Main body with gradient shading
        rocketBodyPaint.shader = LinearGradient(
            -38f, -20f, 40f, 20f,
            Color.parseColor("#f8f8ff"),
            Color.parseColor("#c0c0d8"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(-38f, -20f, 40f, 20f), 18f, 18f, rocketBodyPaint)

        // Under-shadow
        rocketShadePaint.shader = LinearGradient(
            0f, 6f, 0f, 20f,
            Color.argb(0, 100, 100, 140),
            Color.argb(80, 80, 80, 120),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(-38f, 6f, 40f, 20f), 18f, 18f, rocketShadePaint)

        // ── NOSE CONE ─────────────────────────────
        val nose = Path().apply {
            moveTo(40f, -20f)
            quadTo(78f, -10f, 78f, 0f)
            quadTo(78f, 10f, 40f, 20f)
            close()
        }
        wingPaint.shader = LinearGradient(
            40f, -20f, 78f, 20f,
            Color.parseColor("#ff5a51"),
            Color.parseColor("#c0211a"),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(nose, wingPaint)

        // ── WINGS ─────────────────────────────────
        // Top wing
        val topWing = Path().apply {
            moveTo(-6f, -18f)
            lineTo(-36f, -46f)
            lineTo(-36f, -34f)
            lineTo(8f, -18f)
            close()
        }
        wingPaint.shader = LinearGradient(
            -36f, -46f, 8f, -18f,
            Color.parseColor("#ff5a51"),
            Color.parseColor("#a01810"),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(topWing, wingPaint)

        // Bottom wing
        val bottomWing = Path().apply {
            moveTo(-6f, 18f)
            lineTo(-36f, 46f)
            lineTo(-36f, 34f)
            lineTo(8f, 18f)
            close()
        }
        canvas.drawPath(bottomWing, wingPaint)

        // ── WINDOW ────────────────────────────────
        glassPaint.shader = RadialGradient(
            10f, -4f, 14f,
            Color.parseColor("#b8f0ff"),
            Color.parseColor("#2a90c0"),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(14f, 0f, 13f, glassPaint)

        // Shine dot
        glassShine.shader = null
        glassShine.alpha = 200
        canvas.drawCircle(9f, -5f, 4f, glassShine)

        canvas.restore()
    }

    // ──────────────────────────────────────────────
    // PARTICLES
    // ──────────────────────────────────────────────

    private fun drawParticles(canvas: Canvas) {
        for (p in particles) {
            val alpha = (p.life * 255f).toInt().coerceIn(0, 255)
            particlePaint.color = p.color
            particlePaint.alpha = alpha

            when (p.type) {
                ParticleType.SPARK -> {
                    canvas.drawCircle(p.x, p.y, p.size * p.life, particlePaint)
                }
                ParticleType.SMOKE -> {
                    particlePaint.alpha = (alpha * 0.5f).toInt()
                    canvas.drawCircle(p.x, p.y, p.size, particlePaint)
                }
                ParticleType.STAR -> {
                    drawStar(canvas, p.x, p.y, p.size * (0.5f + p.life * 0.5f), particlePaint)
                }
            }
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        val path = Path()
        val innerR = r * 0.4f
        for (i in 0 until 10) {
            val angle = (i * 36f - 90f) * PI.toFloat() / 180f
            val radius = if (i % 2 == 0) r else innerR
            val px = cx + cos(angle) * radius
            val py = cy + sin(angle) * radius
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    // ──────────────────────────────────────────────
    // UI
    // ──────────────────────────────────────────────

    private fun drawUI(canvas: Canvas) {
        when (state) {
            GameState.RUNNING, GameState.COUNTDOWN -> drawHUD(canvas)
            GameState.IDLE     -> drawIdleScreen(canvas)
            GameState.DEAD     -> { drawHUD(canvas); drawDeathPanel(canvas) }
        }

        if (state == GameState.COUNTDOWN) {
            val num = ceil(countdown).toInt().coerceAtLeast(1)
            val scale = 1f + (countdown - floor(countdown)) * 0.6f
            canvas.save()
            canvas.scale(scale, scale, sw / 2f, sh * 0.44f)
            drawOutlinedText(canvas, num.toString(), sw / 2f, sh * 0.44f, 200f)
            canvas.restore()
        }
    }

    private fun drawHUD(canvas: Canvas) {
        val popScale = 1f + scorePop * 0.35f
        canvas.save()
        canvas.scale(popScale, popScale, sw / 2f, 120f)
        drawOutlinedText(canvas, score.toString(), sw / 2f, 130f, 100f)
        canvas.restore()
    }

    private fun drawIdleScreen(canvas: Canvas) {
        // Title panel backdrop
        val panelW = sw * 0.82f
        val panelH = 140f
        val panelX = (sw - panelW) / 2f
        val panelY = sh * 0.18f

        panelPaint.alpha = 200
        canvas.drawRoundRect(RectF(panelX, panelY, panelX + panelW, panelY + panelH), 28f, 28f, panelPaint)
        canvas.drawRoundRect(RectF(panelX, panelY, panelX + panelW, panelY + panelH), 28f, 28f, panelBorder)

        drawOutlinedText(canvas, "ROCKET DASH", sw / 2f, panelY + 90f, 78f)

        // Best score
        if (bestScore > 0) {
            subTextPaint.textSize = 36f
            subTextPaint.color = Color.parseColor("#ffe066")
            canvas.drawText("BEST: $bestScore", sw / 2f, sh * 0.4f, subTextPaint)
        }

        // Tap to start — pulsing
        val pulse = (sin(time * 3.5f) + 1f) / 2f
        subTextPaint.textSize = 44f
        subTextPaint.color = Color.argb((180 + pulse * 75f).toInt(), 255, 255, 255)
        canvas.drawText("TAP TO START", sw / 2f, sh * 0.82f, subTextPaint)

        // Tap ring pulse
        tapPulsePaint.alpha = ((1f - pulse) * 180f).toInt()
        val ringR = 52f + pulse * 22f
        canvas.drawCircle(sw / 2f, sh * 0.82f + 18f, ringR, tapPulsePaint)
    }

    private fun drawDeathPanel(canvas: Canvas) {
        val panelW  = sw * 0.86f
        val panelH  = sh * 0.52f
        val panelX  = (sw - panelW) / 2f
        val panelY  = deathPanelY
        val cornerR = 36f

        if (panelY > sh) return

        // Drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 0, 0, 0)
        }
        canvas.drawRoundRect(RectF(panelX + 8f, panelY + 12f, panelX + panelW + 8f, panelY + panelH + 12f), cornerR, cornerR, shadowPaint)

        // Panel body
        panelPaint.apply {
            color = Color.argb(235, 15, 15, 45)
            shader = null
        }
        canvas.drawRoundRect(RectF(panelX, panelY, panelX + panelW, panelY + panelH), cornerR, cornerR, panelPaint)

        // Border
        panelBorder.color = Color.parseColor("#7b68ee")
        panelBorder.strokeWidth = 5f
        canvas.drawRoundRect(RectF(panelX, panelY, panelX + panelW, panelY + panelH), cornerR, cornerR, panelBorder)

        // Header
        drawOutlinedText(canvas, "GAME OVER", sw / 2f, panelY + 90f, 72f)

        // Divider
        canvas.drawLine(panelX + 40f, panelY + 110f, panelX + panelW - 40f, panelY + 110f, dividerPaint)

        // Score section
        val scoreY = panelY + 175f
        subTextPaint.textSize = 34f
        subTextPaint.color = Color.argb(180, 200, 200, 255)
        canvas.drawText("SCORE", sw / 2f - panelW * 0.23f, scoreY - 12f, subTextPaint)
        canvas.drawText("BEST", sw / 2f + panelW * 0.23f, scoreY - 12f, subTextPaint)

        textPaint.textSize = 72f
        textPaint.color = Color.WHITE
        canvas.drawText(score.toString(), sw / 2f - panelW * 0.23f, scoreY + 60f, textPaint)

        val bestColor = if (score >= bestScore && score > 0) Color.parseColor("#ffd700") else Color.WHITE
        textPaint.color = bestColor
        canvas.drawText(bestScore.toString(), sw / 2f + panelW * 0.23f, scoreY + 60f, textPaint)

        // Vertical divider between scores
        canvas.drawLine(sw / 2f, panelY + 130f, sw / 2f, panelY + 250f, dividerPaint)

        // Medal if score > 0
        if (score > 0) {
            val medalY = panelY + panelH - 145f
            drawMedal(canvas, sw / 2f, medalY, score)
        }

        // Tap to retry
        dividerPaint.alpha = 60
        canvas.drawLine(panelX + 40f, panelY + panelH - 90f, panelX + panelW - 40f, panelY + panelH - 90f, dividerPaint)

        val pulse = (sin(time * 3f) + 1f) / 2f
        subTextPaint.textSize = 38f
        subTextPaint.color = Color.argb((160 + pulse * 95f).toInt(), 255, 230, 100)
        canvas.drawText("TAP TO PLAY AGAIN", sw / 2f, panelY + panelH - 44f, subTextPaint)

        // New best badge
        if (score > 0 && score >= bestScore) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#ffd700") }
            val badgeX = panelX + panelW - 90f
            val badgeY = panelY + 52f
            canvas.drawRoundRect(RectF(badgeX - 68f, badgeY - 26f, badgeX + 68f, badgeY + 26f), 20f, 20f, badgePaint)
            val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1a1a3e")
                textSize = 26f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("NEW BEST!", badgeX, badgeY + 9f, badgeText)
        }
    }

    private fun drawMedal(canvas: Canvas, cx: Float, cy: Float, score: Int) {
        val (color, label) = when {
            score >= 30 -> Color.parseColor("#b9f2ff") to "PLATINUM"
            score >= 20 -> Color.parseColor("#ffd700") to "GOLD"
            score >= 10 -> Color.parseColor("#c0c0c0") to "SILVER"
            else        -> Color.parseColor("#cd7f32") to "BRONZE"
        }
        medalPaint.color = color
        canvas.drawCircle(cx, cy, 28f, medalPaint)

        val medalText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#1a1a3e")
            textSize = 18f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(label, cx, cy + 7f, medalText)
    }

    private fun drawBoostFlash(canvas: Canvas) {
        if (flash > 0f) {
            flashPaint.alpha = (flash * 200f).toInt().coerceIn(0, 200)
            canvas.drawRect(0f, 0f, sw, sh, flashPaint)
        }
    }

    // ──────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────

    private fun drawOutlinedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float
    ) {
        outlinePaint.textSize = size
        outlinePaint.strokeWidth = size * 0.1f
        textPaint.textSize = size
        canvas.drawText(text, x, y, outlinePaint)
        canvas.drawText(text, x, y, textPaint)
    }
}
