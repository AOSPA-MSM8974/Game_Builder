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
import java.util.concurrent.atomic.AtomicBoolean

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
    const val TARGET_FPS        = 60  // Reduced from 120 for better stability
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
    
    // Debounce for tap input
    const val TAP_DEBOUNCE_MS   = 100L
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
    var size: Float,
    var type: ParticleType
)

enum class ParticleType { SPARK, SMOKE, STAR }

enum class GameState { IDLE, COUNTDOWN, RUNNING, DEAD }

// =====================================================
// GAME VIEW
// =====================================================

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var thread: GameThread? = null
    private val game = RocketGame()
    private val lastTapTime = AtomicBoolean(false)
    private var lastTapTimeMs = 0L

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
        try { 
            thread?.join(1000) // Add timeout to prevent deadlock
        } catch (_: Exception) {}
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
            // Debounce tap: ignore if called within 100ms
            val currentTimeMs = System.currentTimeMillis()
            if (currentTimeMs - lastTapTimeMs > Config.TAP_DEBOUNCE_MS) {
                lastTapTimeMs = currentTimeMs
                game.onTap()
                performClick()
            }
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

            try {
                game.update(dt)

                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    try {
                        game.draw(canvas)
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
            } catch (e: Exception) {
                // Prevent thread crash
                e.printStackTrace()
            }

            val elapsed = (System.nanoTime() - now) / 1_000_000L
            val sleepTime = Config.FRAME_TIME - elapsed
            if (sleepTime > 0) Thread.sleep(sleepTime)
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
    private val particlesToRemove = mutableListOf<Particle>() // Pre-allocated for GC efficiency

    // score pop animation
    private var scorePop = 0f

    // death panel slide
    private var deathPanelY = 0f
    private var deathTimer = 0f

    // stars for background
    private val stars = mutableListOf<Triple<Float, Float, Float>>() // x, y, brightness

    // ──────────────────────────────────────────────
    // PAINTS (Lazy initialization to reduce startup time)
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

        // Sky gradient - enhanced with better colors
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, sh,
            intArrayOf(
                Color.parseColor("#000000"),      // Deep black at top
                Color.parseColor("#1a1a3e"),
                Color.parseColor("#0f3460"),
                Color.parseColor("#2a1a5e"),
                Color.parseColor("#e94560")
            ),
            floatArrayOf(0f, 0.2f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )

        cloudPaint.color = Color.argb(180, 255, 255, 255)

        mountainPaint.color = Color.parseColor("#1a0f3e")
        hillPaint.color = Color.parseColor("#1e3a5f")

        // Pipes – enhanced green with better depth
        pipeBodyPaint.color  = Color.parseColor("#3ddc5d")
        pipeEdgePaint.color  = Color.parseColor("#1ea336")
        pipeCapPaint.color   = Color.parseColor("#44e866")
        pipeCapEdge.color    = Color.parseColor("#1a8f30")
        pipeShadow.color     = Color.parseColor("#0d4a15")
        pipeShine.apply {
            color = Color.argb(100, 255, 255, 255)
        }

        // Ground
        grassPaint.color  = Color.parseColor("#2ecc40")
        dirtPaint.color   = Color.parseColor("#8b5e3c")
        dirtStripePaint.color = Color.parseColor("#6a3d20")

        // Rocket body - enhanced colors
        rocketBodyPaint.color  = Color.parseColor("#f0f0f8")
        rocketShadePaint.color = Color.parseColor("#a0a0c8")
        wingPaint.color        = Color.parseColor("#ff3b30")
        wingShade.color        = Color.parseColor("#8f1a0f")
        glassPaint.color       = Color.parseColor("#7ee8ff")
        glassShine.color       = Color.argb(220, 255, 255, 255)

        flamePaint.color = Color.parseColor("#ff9500")

        // UI
        textPaint.apply {
            color = Color.WHITE
            textSize = 96f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(8f, 4f, 4f, Color.argb(150, 0, 0, 0))
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
            color = Color.argb(240, 15, 15, 40)
        }
        panelBorder.apply {
            color = Color.parseColor("#6a5acd")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        dividerPaint.apply {
            color = Color.argb(100, 255, 255, 255)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        medalPaint.apply {
            color = Color.parseColor("#ffd700")
        }
        flashPaint.color = Color.WHITE
        particlePaint.apply { style = Paint.Style.FILL }
        starPaint.apply { 
            color = Color.WHITE
            textSize = 20f
        }
        tapPulsePaint.apply {
            color = Color.argb(180, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 5f
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
            GameState.COUNTDOWN -> {
                // Ignore taps during countdown
            }
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
        particlesToRemove.clear()
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

        // Reset scrolls to prevent overflow
        if (bgScroll1 > sw * 2) bgScroll1 -= sw * 2
        if (bgScroll2 > sw * 2) bgScroll2 -= sw * 2
        if (bgScroll3 > sw) bgScroll3 -= sw

        flash = (flash - dt * 3f).coerceAtLeast(0f)

        // Screen shake decay
        if (shakeTimer > 0f) {
            shakeTimer = (shakeTimer - dt).coerceAtLeast(0f)
            val mag = shakeTimer / Config.SHAKE_DURATION * Config.SHAKE_MAGNITUDE
            shakeX = (Random.nextFloat() * 2f - 1f) * mag
            shakeY = (Random.nextFloat() * 2f - 1f) * mag
        } else {
            shakeX = 0f
            shakeY = 0f
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
        
        // Efficient pipe removal - just remove off-screen pipes
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
            val angle = Random.nextFloat() * PI.toFloat() + PI.toFloat() / 2f
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
        particlesToRemove.clear()
        for (p in particles) {
            p.life -= dt
            if (p.life <= 0f) {
                particlesToRemove.add(p)
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vy += 400f * dt // gravity
            }
        }
        particles.removeAll(particlesToRemove)
    }

    // ──────────────────────────────────────────────
    // DRAWING
    // ──────────────────────────────────────────────

    fun draw(canvas: Canvas) {
        try {
            canvas.drawRect(0f, 0f, sw, sh, skyPaint)
            
            // Apply shake offset
            canvas.save()
            canvas.translate(shakeX, shakeY)

            drawStars(canvas)
            drawMountains(canvas)
            drawHills(canvas)
            drawPipes(canvas)
            drawGround(canvas)
            drawRocket(canvas)
            drawParticles(canvas)

            canvas.restore()

            if (flash > 0f) {
                flashPaint.alpha = (flash * 255).toInt()
                canvas.drawRect(0f, 0f, sw, sh, flashPaint)
            }

            drawUI(canvas)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawStars(canvas: Canvas) {
        for ((x, y, brightness) in stars) {
            starPaint.alpha = (brightness * 200).toInt()
            canvas.drawCircle(x, y, 2f, starPaint)
        }
    }

    private fun drawMountains(canvas: Canvas) {
        val scrollPos = bgScroll1 % (sw * 2)
        val path = Path()
        path.moveTo(-scrollPos, sh * 0.55f)
        for (i in 0..100) {
            val xx = i * 50f - scrollPos
            val yy = sh * 0.55f - sin(i * 0.3f) * 100f
            path.lineTo(xx, yy)
        }
        path.lineTo(sw + 100f - scrollPos, sh)
        path.lineTo(-scrollPos, sh)
        path.close()
        canvas.drawPath(path, mountainPaint)

        // Draw second layer for seamless loop
        val scrollPos2 = scrollPos - sw * 2
        path.reset()
        path.moveTo(-scrollPos2, sh * 0.55f)
        for (i in 0..100) {
            val xx = i * 50f - scrollPos2
            val yy = sh * 0.55f - sin(i * 0.3f) * 100f
            path.lineTo(xx, yy)
        }
        path.lineTo(sw + 100f - scrollPos2, sh)
        path.lineTo(-scrollPos2, sh)
        path.close()
        canvas.drawPath(path, mountainPaint)
    }

    private fun drawHills(canvas: Canvas) {
        val scrollPos = bgScroll2 % (sw * 2)
        val path = Path()
        path.moveTo(-scrollPos, sh * 0.68f)
        for (i in 0..80) {
            val xx = i * 40f - scrollPos
            val yy = sh * 0.68f - sin(i * 0.5f) * 60f
            path.lineTo(xx, yy)
        }
        path.lineTo(sw + 100f - scrollPos, sh)
        path.lineTo(-scrollPos, sh)
        path.close()
        canvas.drawPath(path, hillPaint)

        val scrollPos2 = scrollPos - sw * 2
        path.reset()
        path.moveTo(-scrollPos2, sh * 0.68f)
        for (i in 0..80) {
            val xx = i * 40f - scrollPos2
            val yy = sh * 0.68f - sin(i * 0.5f) * 60f
            path.lineTo(xx, yy)
        }
        path.lineTo(sw + 100f - scrollPos2, sh)
        path.lineTo(-scrollPos2, sh)
        path.close()
        canvas.drawPath(path, hillPaint)
    }

    private fun drawPipes(canvas: Canvas) {
        for (pipe in pipes) {
            // Top pipe
            drawPipeSegment(canvas, pipe.x, pipe.gapTop - 150f, true)
            // Bottom pipe
            drawPipeSegment(canvas, pipe.x, pipe.gapTop + Config.PIPE_GAP, false)
        }
    }

    private fun drawPipeSegment(canvas: Canvas, x: Float, y: Float, isTop: Boolean) {
        val height = if (isTop) y else sh - y
        if (height <= 0) return

        // Shadow
        canvas.drawRect(x + 8f, y, x + Config.PIPE_WIDTH, y + height, pipeShadow)
        // Body
        canvas.drawRect(x, y, x + Config.PIPE_WIDTH, y + height, pipeBodyPaint)
        // Edge highlight
        canvas.drawRect(x, y, x + 6f, y + height, pipeEdgePaint)
        // Shine
        canvas.drawRect(x + 20f, y, x + 30f, y + height, pipeShine)

        // Cap
        val capY = if (isTop) y + height - 20f else y
        canvas.drawRect(x - 15f, capY, x + Config.PIPE_WIDTH + 15f, capY + 20f, pipeCapPaint)
        canvas.drawRect(x - 15f, capY, x + Config.PIPE_WIDTH + 15f, capY + 20f, pipeCapEdge)
    }

    private fun drawGround(canvas: Canvas) {
        val scrollPos = bgScroll3 % sw
        // Grass
        canvas.drawRect(0f, groundY, sw, sh, grassPaint)
        
        // Dirt stripes for detail
        for (i in -1..10) {
            val x = i * 40f - scrollPos
            canvas.drawLine(x, groundY, x, groundY + 50f, dirtStripePaint)
        }
    }

    private fun drawRocket(canvas: Canvas) {
        canvas.save()
        canvas.translate(rocketX, rocketY)
        canvas.rotate(rotation)

        val sz = Config.ROCKET_SIZE

        // Body shadow
        canvas.drawCircle(-2f, 2f, sz * 0.65f, rocketShadePaint)
        // Body
        canvas.drawCircle(0f, 0f, sz * 0.65f, rocketBodyPaint)

        // Wings
        val ww = sz * 0.4f
        val wh = sz * 0.5f
        
        // Left wing
        canvas.drawRect(-sz * 0.75f, -wh * 0.5f, -sz * 0.3f, wh * 0.5f, wingShade)
        canvas.drawRect(-sz * 0.75f, -wh * 0.4f, -sz * 0.3f, wh * 0.4f, wingPaint)
        
        // Right wing
        canvas.drawRect(sz * 0.3f, -wh * 0.5f, sz * 0.75f, wh * 0.5f, wingShade)
        canvas.drawRect(sz * 0.3f, -wh * 0.4f, sz * 0.75f, wh * 0.4f, wingPaint)

        // Glass cockpit
        canvas.drawCircle(0f, -sz * 0.2f, sz * 0.25f, glassPaint)
        canvas.drawCircle(-2f, -sz * 0.25f, sz * 0.2f, glassShine)

        // Flame
        if (velocityY < 100f) {
            val flamelen = 40f + sin(time * 8f) * 10f
            canvas.drawRect(-sz * 0.2f, sz * 0.5f, sz * 0.2f, sz * 0.5f + flamelen, flamePaint)
        }

        canvas.restore()
    }

    private fun drawParticles(canvas: Canvas) {
        for (p in particles) {
            val alpha = (p.life / p.maxLife * 255).toInt().coerceIn(0, 255)
            particlePaint.color = p.color
            particlePaint.alpha = alpha
            canvas.drawCircle(p.x, p.y, p.size, particlePaint)
        }
    }

    private fun drawUI(canvas: Canvas) {
        when (state) {
            GameState.IDLE -> {
                textPaint.textSize = 80f
                outlinePaint.textSize = 80f
                canvas.drawText("TAP TO START", sw * 0.5f, sh * 0.35f, outlinePaint)
                canvas.drawText("TAP TO START", sw * 0.5f, sh * 0.35f, textPaint)

                subTextPaint.textSize = 36f
                canvas.drawText("Best: $bestScore", sw * 0.5f, sh * 0.65f, subTextPaint)
            }
            GameState.COUNTDOWN -> {
                textPaint.textSize = 120f
                outlinePaint.textSize = 120f
                val countNum = (countdown + 0.5f).toInt()
                canvas.drawText("$countNum", sw * 0.5f, sh * 0.5f, outlinePaint)
                canvas.drawText("$countNum", sw * 0.5f, sh * 0.5f, textPaint)
            }
            GameState.RUNNING -> {
                textPaint.textSize = 96f
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("$score", sw - 60f, 120f, textPaint)
                textPaint.textAlign = Paint.Align.CENTER
            }
            GameState.DEAD -> {
                // Death panel
                val panelW = sw * 0.8f
                val panelH = sh * 0.5f
                val panelX = (sw - panelW) * 0.5f
                val panelY = deathPanelY

                canvas.drawRoundRect(panelX, panelY, panelX + panelW, panelY + panelH, 20f, 20f, panelPaint)
                canvas.drawRoundRect(panelX, panelY, panelX + panelW, panelY + panelH, 20f, 20f, panelBorder)

                // Title
                textPaint.textSize = 72f
                canvas.drawText("GAME OVER", sw * 0.5f, panelY + 80f, textPaint)

                // Score
                subTextPaint.textSize = 48f
                canvas.drawText("Score: $score", sw * 0.5f, panelY + 160f, subTextPaint)
                canvas.drawText("Best: $bestScore", sw * 0.5f, panelY + 220f, subTextPaint)

                // Tap to restart
                if (deathTimer > 0.4f) {
                    subTextPaint.textSize = 36f
                    canvas.drawText("TAP TO RESTART", sw * 0.5f, panelY + 300f, subTextPaint)
                }
            }
        }
    }
}
