package com.maxi.flappybird

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.*
import kotlin.random.Random

// ─────────────────────────────────────────────
//  MainActivity
// ─────────────────────────────────────────────
class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = GameView(this)
        setContentView(gameView)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    override fun onPause()  { super.onPause();  gameView.pause() }
    override fun onResume() { super.onResume(); gameView.resume() }
}

// ─────────────────────────────────────────────
//  Constants
// ─────────────────────────────────────────────
object Config {
    const val GRAVITY          = 1800f   // px/s²
    const val FLAP_VELOCITY    = -620f   // px/s  (negative = upward)
    const val PIPE_SPEED       = 280f    // px/s
    const val PIPE_SPAWN_DIST  = 420f    // px between pipe columns
    const val PIPE_GAP         = 310f    // px between top & bottom pipe
    const val BIRD_X_RATIO     = 0.22f   // bird's fixed X as fraction of screen width
    const val BIRD_RADIUS      = 32f     // px
    const val PIPE_WIDTH       = 90f     // px
    const val GROUND_HEIGHT    = 110f    // px
    const val MAX_ROTATION_DEG = 30f     // upward tilt cap
    const val MIN_ROTATION_DEG = 90f     // nose-down cap
    const val ROT_SPEED        = 220f    // deg/s toward nose-down
    const val TARGET_FPS       = 60
    const val FRAME_TIME_MS    = (1000.0 / TARGET_FPS).toLong()
    const val FLASH_DURATION   = 0.12f   // s – white flash on death
    const val SQUASH_FLAP      = 0.65f   // scale Y on flap
    const val SQUASH_RECOVER   = 18f     // speed of squash recovery
    const val STAR_COUNT       = 60
}

// ─────────────────────────────────────────────
//  Data classes
// ─────────────────────────────────────────────
data class Pipe(
    var x: Float,
    val gapTop: Float,      // y of gap top edge
    var scored: Boolean = false
) {
    val gapBottom get() = gapTop + Config.PIPE_GAP
}

data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Int)

data class Particle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var life: Float,            // 0..1
    val color: Int
)

// ─────────────────────────────────────────────
//  Enum: game state
// ─────────────────────────────────────────────
enum class GameState { IDLE, RUNNING, DEAD }

// ─────────────────────────────────────────────
//  GameView  (SurfaceView + game loop)
// ─────────────────────────────────────────────
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private val game = FlappyGame()

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(h: SurfaceHolder) {
        game.init(width.toFloat(), height.toFloat())
        gameThread = GameThread(h, game).also { it.start() }
    }

    override fun surfaceDestroyed(h: SurfaceHolder) { pause() }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) {}

    fun pause()  { gameThread?.running = false; gameThread?.join(); gameThread = null }
    fun resume() {
        if (gameThread == null && holder.surface.isValid) {
            gameThread = GameThread(holder, game).also { it.start() }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            game.onTap()
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }
}

// ─────────────────────────────────────────────
//  Game loop thread
// ─────────────────────────────────────────────
class GameThread(private val holder: SurfaceHolder, private val game: FlappyGame) : Thread() {
    @Volatile var running = true

    override fun run() {
        var lastTime = System.nanoTime()
        while (running) {
            val now  = System.nanoTime()
            val dt   = ((now - lastTime) / 1_000_000_000.0).toFloat().coerceAtMost(0.05f)
            lastTime = now

            game.update(dt)

            val canvas = holder.lockCanvas() ?: continue
            try { game.draw(canvas) } finally { holder.unlockCanvasAndPost(canvas) }

            val elapsed = (System.nanoTime() - now) / 1_000_000L
            val sleep   = Config.FRAME_TIME_MS - elapsed
            if (sleep > 0) sleep(sleep)
        }
    }
}

// ─────────────────────────────────────────────
//  FlappyGame  – all logic + rendering
// ─────────────────────────────────────────────
class FlappyGame {

    // screen
    private var sw = 0f
    private var sh = 0f
    private var groundY = 0f

    // bird
    private var birdX  = 0f
    private var birdY  = 0f
    private var velY   = 0f
    private var angle  = 0f   // degrees, 0 = level, + = nose down
    private var scaleY = 1f   // squash/stretch

    // pipes
    private val pipes = mutableListOf<Pipe>()
    private var nextPipeX = 0f

    // game state
    private var state = GameState.IDLE
    private var score = 0
    private var bestScore = 0

    // visuals
    private val stars   = mutableListOf<Star>()
    private val particles = mutableListOf<Particle>()
    private var flashAlpha  = 0f   // 0..1 death flash
    private var groundOffset = 0f
    private var bgScrollOffset = 0f

    // idle bob
    private var idleTime   = 0f
    private var idleBobAmp = 18f

    // screen-shake
    private var shakeMag = 0f
    private var shakeX   = 0f
    private var shakeY   = 0f

    // tap hint pulse
    private var hintPulse = 0f

    // ── Paints ────────────────────────────────
    private val skyPaint     = Paint()
    private val starPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val groundPaint  = Paint()
    private val grassPaint   = Paint()
    private val dirtPaint    = Paint()
    private val pipePaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipeRimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdBodyPaint= Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdEyePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdPupilPaint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdWingPaint= Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdBeakPaint= Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdBlushPaint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val flashPaint   = Paint()
    private val scorePaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scoreShadow  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val uiPaint      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hintPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint= Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelBorder  = Paint(Paint.ANTI_ALIAS_FLAG)

    // sky gradient (recomputed in init)
    private var skyShader: LinearGradient? = null

    // ── Typeface ──────────────────────────────
    private val tf = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    // ─────────────────────────────────────────
    fun init(w: Float, h: Float) {
        sw = w; sh = h
        groundY = sh - Config.GROUND_HEIGHT

        skyShader = LinearGradient(0f, 0f, 0f, groundY,
            intArrayOf(
                Color.parseColor("#1a1a2e"),
                Color.parseColor("#16213e"),
                Color.parseColor("#0f3460")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP)
        skyPaint.shader = skyShader

        groundPaint.color  = Color.parseColor("#2d5016")
        grassPaint.color   = Color.parseColor("#4a7c1f")
        dirtPaint.color    = Color.parseColor("#8b6914")

        pipePaint.color    = Color.parseColor("#2ecc71")
        pipeRimPaint.color = Color.parseColor("#27ae60")
        pipeRimPaint.style = Paint.Style.STROKE
        pipeRimPaint.strokeWidth = 4f

        birdBodyPaint.color  = Color.parseColor("#f1c40f")
        birdWingPaint.color  = Color.parseColor("#e67e22")
        birdEyePaint.color   = Color.WHITE
        birdPupilPaint.color = Color.parseColor("#2c3e50")
        birdBeakPaint.color  = Color.parseColor("#e74c3c")
        birdBlushPaint.color = Color.argb(80, 231, 76, 60)

        flashPaint.color = Color.WHITE

        scorePaint.typeface  = tf
        scorePaint.textSize  = 90f
        scorePaint.color     = Color.WHITE
        scorePaint.textAlign = Paint.Align.CENTER

        scoreShadow.typeface  = tf
        scoreShadow.textSize  = 90f
        scoreShadow.color     = Color.parseColor("#88000000")
        scoreShadow.textAlign = Paint.Align.CENTER

        uiPaint.typeface  = tf
        uiPaint.textSize  = 52f
        uiPaint.color     = Color.WHITE
        uiPaint.textAlign = Paint.Align.CENTER

        hintPaint.typeface  = tf
        hintPaint.textSize  = 38f
        hintPaint.textAlign = Paint.Align.CENTER

        panelPaint.color  = Color.parseColor("#CC1a1a2e")
        panelBorder.color = Color.parseColor("#f1c40f")
        panelBorder.style = Paint.Style.STROKE
        panelBorder.strokeWidth = 4f

        // stars
        stars.clear()
        repeat(Config.STAR_COUNT) {
            stars += Star(
                Random.nextFloat() * sw,
                Random.nextFloat() * groundY * 0.8f,
                Random.nextFloat() * 2.5f + 0.5f,
                Random.nextInt(120, 255)
            )
        }

        resetBird()
        pipes.clear()
        score = 0
        nextPipeX = sw + 100f
    }

    private fun resetBird() {
        birdX  = sw * Config.BIRD_X_RATIO
        birdY  = sh * 0.45f
        velY   = 0f
        angle  = 0f
        scaleY = 1f
        idleTime = 0f
    }

    // ─────────────────────────────────────────
    //  Input
    // ─────────────────────────────────────────
    fun onTap() {
        when (state) {
            GameState.IDLE    -> startGame()
            GameState.RUNNING -> flap()
            GameState.DEAD    -> restart()
        }
    }

    private fun startGame() {
        state = GameState.RUNNING
        velY  = Config.FLAP_VELOCITY
        scaleY = Config.SQUASH_FLAP
        angle  = -20f
        spawnFeatherBurst(birdX, birdY, 8)
    }

    private fun flap() {
        velY   = Config.FLAP_VELOCITY
        scaleY = Config.SQUASH_FLAP
        angle  = -Config.MAX_ROTATION_DEG
        spawnFeatherBurst(birdX, birdY, 5)
    }

    private fun restart() {
        state = GameState.IDLE
        pipes.clear()
        score = 0
        nextPipeX = sw + 100f
        resetBird()
        particles.clear()
        flashAlpha = 0f
        shakeMag   = 0f
    }

    // ─────────────────────────────────────────
    //  Update
    // ─────────────────────────────────────────
    fun update(dt: Float) {
        hintPulse  = (hintPulse + dt * 3f) % (2f * PI.toFloat())
        groundOffset = (groundOffset + Config.PIPE_SPEED * dt) % 120f
        bgScrollOffset += dt * 20f

        when (state) {
            GameState.IDLE    -> updateIdle(dt)
            GameState.RUNNING -> updateRunning(dt)
            GameState.DEAD    -> updateDead(dt)
        }
        updateParticles(dt)
    }

    private fun updateIdle(dt: Float) {
        idleTime += dt
        birdY  = sh * 0.45f + sin(idleTime * 2.5f) * idleBobAmp
        angle  = sin(idleTime * 2.5f) * 10f
        scaleY += (1f - scaleY) * Config.SQUASH_RECOVER * dt
    }

    private fun updateRunning(dt: Float) {
        // physics
        velY   += Config.GRAVITY * dt
        birdY  += velY * dt
        scaleY += (1f - scaleY) * Config.SQUASH_RECOVER * dt

        // rotation
        val targetAngle = when {
            velY < 0 -> -Config.MAX_ROTATION_DEG
            else     -> Config.MIN_ROTATION_DEG
        }
        val rotDir = if (targetAngle > angle) 1f else -1f
        angle += rotDir * Config.ROT_SPEED * dt
        angle  = angle.coerceIn(-Config.MAX_ROTATION_DEG, Config.MIN_ROTATION_DEG)

        // pipes
        updatePipes(dt)

        // collision
        if (checkCollision()) die()

        // screen shake decay
        shakeMag *= (1f - dt * 12f)
        shakeX = if (shakeMag > 1f) (Random.nextFloat() - 0.5f) * shakeMag else 0f
        shakeY = if (shakeMag > 1f) (Random.nextFloat() - 0.5f) * shakeMag else 0f
    }

    private fun updateDead(dt: Float) {
        // let bird fall
        velY  += Config.GRAVITY * dt
        birdY += velY * dt
        birdY  = birdY.coerceAtMost(groundY - Config.BIRD_RADIUS)
        angle  = Config.MIN_ROTATION_DEG

        flashAlpha = (flashAlpha - dt / Config.FLASH_DURATION).coerceAtLeast(0f)
        shakeMag  *= (1f - dt * 8f)
        shakeX = if (shakeMag > 1f) (Random.nextFloat() - 0.5f) * shakeMag else 0f
        shakeY = if (shakeMag > 1f) (Random.nextFloat() - 0.5f) * shakeMag else 0f
    }

    private fun updatePipes(dt: Float) {
        // move pipes
        for (pipe in pipes) pipe.x -= Config.PIPE_SPEED * dt

        // score
        for (pipe in pipes) {
            if (!pipe.scored && pipe.x + Config.PIPE_WIDTH < birdX) {
                pipe.scored = true
                score++
                if (score > bestScore) bestScore = score
                spawnScoreParticles(birdX, birdY)
            }
        }

        // spawn
        if (pipes.isEmpty() || pipes.last().x < nextPipeX - Config.PIPE_SPAWN_DIST) {
            val minGapTop = sh * 0.12f
            val maxGapTop = groundY - Config.PIPE_GAP - sh * 0.12f
            val gapTop    = Random.nextFloat() * (maxGapTop - minGapTop) + minGapTop
            pipes += Pipe(sw + Config.PIPE_WIDTH, gapTop)
        }

        // remove off-screen
        pipes.removeAll { it.x + Config.PIPE_WIDTH < -20f }
    }

    private fun checkCollision(): Boolean {
        val r = Config.BIRD_RADIUS * 0.78f  // slightly forgiving hitbox

        // ground / ceiling
        if (birdY + r >= groundY || birdY - r <= 0f) return true

        // pipes
        for (pipe in pipes) {
            val pLeft  = pipe.x
            val pRight = pipe.x + Config.PIPE_WIDTH
            if (birdX + r < pLeft || birdX - r > pRight) continue
            if (birdY - r < pipe.gapTop || birdY + r > pipe.gapBottom) return true
        }
        return false
    }

    private fun die() {
        state      = GameState.DEAD
        flashAlpha = 1f
        shakeMag   = 28f
        spawnDeathParticles(birdX, birdY)
    }

    // ─────────────────────────────────────────
    //  Particles
    // ─────────────────────────────────────────
    private fun spawnFeatherBurst(x: Float, y: Float, count: Int) {
        val colors = intArrayOf(
            Color.parseColor("#f1c40f"),
            Color.parseColor("#e67e22"),
            Color.WHITE
        )
        repeat(count) {
            val speed  = Random.nextFloat() * 320f + 80f
            val angleR = Random.nextFloat() * 2f * PI.toFloat()
            particles += Particle(
                x, y,
                cos(angleR) * speed,
                sin(angleR) * speed - 200f,
                1f,
                colors[Random.nextInt(colors.size)]
            )
        }
    }

    private fun spawnDeathParticles(x: Float, y: Float) {
        val colors = intArrayOf(
            Color.parseColor("#e74c3c"),
            Color.parseColor("#f1c40f"),
            Color.parseColor("#e67e22"),
            Color.WHITE
        )
        repeat(22) {
            val speed  = Random.nextFloat() * 500f + 150f
            val angleR = Random.nextFloat() * 2f * PI.toFloat()
            particles += Particle(
                x, y,
                cos(angleR) * speed,
                sin(angleR) * speed - 300f,
                1f,
                colors[Random.nextInt(colors.size)]
            )
        }
    }

    private fun spawnScoreParticles(x: Float, y: Float) {
        repeat(6) {
            val speed  = Random.nextFloat() * 200f + 60f
            val angleR = Random.nextFloat() * 2f * PI.toFloat()
            particles += Particle(
                x, y - 60f,
                cos(angleR) * speed,
                sin(angleR) * speed - 150f,
                1f,
                Color.parseColor("#f1c40f")
            )
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.vy   += 900f * dt        // gravity on particles
            p.x    += p.vx * dt
            p.y    += p.vy * dt
            p.life -= dt * 2.2f
            if (p.life <= 0f) iter.remove()
        }
    }

    // ─────────────────────────────────────────
    //  Draw
    // ─────────────────────────────────────────
    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(shakeX, shakeY)

        drawSky(canvas)
        drawStars(canvas)
        drawPipes(canvas)
        drawGround(canvas)
        drawBird(canvas)
        drawParticles(canvas)

        when (state) {
            GameState.IDLE    -> drawIdleUI(canvas)
            GameState.RUNNING -> drawScore(canvas)
            GameState.DEAD    -> { drawScore(canvas); drawDeadUI(canvas) }
        }

        drawFlash(canvas)
        canvas.restore()
    }

    private fun drawSky(canvas: Canvas) {
        canvas.drawRect(0f, 0f, sw, sh, skyPaint)
    }

    private fun drawStars(canvas: Canvas) {
        for (s in stars) {
            val twinkle = (sin(bgScrollOffset * 0.02f + s.x * 0.01f) * 0.4f + 0.6f)
            starPaint.alpha = (s.alpha * twinkle).toInt()
            starPaint.color = Color.WHITE
            starPaint.alpha = (s.alpha * twinkle).toInt()
            canvas.drawCircle(s.x, s.y, s.radius, starPaint)
        }
    }

    private fun drawGround(canvas: Canvas) {
        // dirt strip
        dirtPaint.color = Color.parseColor("#7d5a17")
        canvas.drawRect(0f, groundY + 14f, sw, sh, dirtPaint)

        // grass strip
        grassPaint.color = Color.parseColor("#4a7c1f")
        canvas.drawRect(0f, groundY, sw, groundY + 18f, grassPaint)

        // scrolling grass tufts
        grassPaint.color = Color.parseColor("#5a9e26")
        var gx = -(groundOffset % 120f)
        while (gx < sw) {
            canvas.drawRoundRect(gx, groundY - 6f, gx + 18f, groundY + 2f, 4f, 4f, grassPaint)
            gx += 120f
        }

        // subtle line
        groundPaint.color = Color.parseColor("#2d5016")
        groundPaint.strokeWidth = 2f
        groundPaint.style = Paint.Style.STROKE
        canvas.drawLine(0f, groundY, sw, groundY, groundPaint)
        groundPaint.style = Paint.Style.FILL
    }

    private fun drawPipes(canvas: Canvas) {
        for (pipe in pipes) {
            drawSinglePipe(canvas, pipe)
        }
    }

    private fun drawSinglePipe(canvas: Canvas, pipe: Pipe) {
        val x = pipe.x
        val w = Config.PIPE_WIDTH
        val rimH = 24f
        val rimExtra = 10f

        // ── top pipe (hangs down from top) ──
        // body
        pipePaint.color = Color.parseColor("#27ae60")
        canvas.drawRect(x, 0f, x + w, pipe.gapTop - rimH, pipePaint)
        // highlight stripe
        pipePaint.color = Color.parseColor("#2ecc71")
        canvas.drawRect(x + 10f, 0f, x + 24f, pipe.gapTop - rimH, pipePaint)
        // rim cap
        pipePaint.color = Color.parseColor("#1e8449")
        val rimRect = RectF(x - rimExtra, pipe.gapTop - rimH, x + w + rimExtra, pipe.gapTop)
        canvas.drawRoundRect(rimRect, 8f, 8f, pipePaint)
        // rim highlight
        pipePaint.color = Color.parseColor("#27ae60")
        canvas.drawRect(x - rimExtra + 6f, pipe.gapTop - rimH + 4f,
                        x - rimExtra + 14f, pipe.gapTop - 4f, pipePaint)

        // ── bottom pipe (rises from bottom) ──
        pipePaint.color = Color.parseColor("#27ae60")
        canvas.drawRect(x, pipe.gapBottom + rimH, x + w, groundY, pipePaint)
        pipePaint.color = Color.parseColor("#2ecc71")
        canvas.drawRect(x + 10f, pipe.gapBottom + rimH, x + 24f, groundY, pipePaint)
        // rim cap
        pipePaint.color = Color.parseColor("#1e8449")
        val rimRect2 = RectF(x - rimExtra, pipe.gapBottom, x + w + rimExtra, pipe.gapBottom + rimH)
        canvas.drawRoundRect(rimRect2, 8f, 8f, pipePaint)
        pipePaint.color = Color.parseColor("#27ae60")
        canvas.drawRect(x - rimExtra + 6f, pipe.gapBottom + 4f,
                        x - rimExtra + 14f, pipe.gapBottom + rimH - 4f, pipePaint)
    }

    private fun drawBird(canvas: Canvas) {
        val cx = birdX
        val cy = birdY
        val r  = Config.BIRD_RADIUS

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(angle)
        canvas.scale(1f, scaleY)

        // drop shadow
        birdBodyPaint.color = Color.argb(60, 0, 0, 0)
        canvas.drawOval(RectF(-r * 0.9f, r * 0.3f, r * 0.9f, r * 1.1f), birdBodyPaint)

        // wing (behind body)
        birdWingPaint.color = Color.parseColor("#e67e22")
        val wingAnim = if (state == GameState.RUNNING) sin(idleTime * 18f) * 10f else 0f
        canvas.save()
        canvas.rotate(-15f + wingAnim)
        canvas.drawOval(RectF(-r * 0.8f, -r * 0.3f, r * 0.2f, r * 0.7f), birdWingPaint)
        canvas.restore()

        // body
        birdBodyPaint.color = Color.parseColor("#f1c40f")
        canvas.drawCircle(0f, 0f, r, birdBodyPaint)

        // belly highlight
        birdBodyPaint.color = Color.parseColor("#f9e14b")
        canvas.drawOval(RectF(-r * 0.45f, -r * 0.3f, r * 0.35f, r * 0.5f), birdBodyPaint)

        // blush
        birdBlushPaint.color = Color.argb(90, 231, 76, 60)
        canvas.drawCircle(r * 0.35f, r * 0.3f, r * 0.3f, birdBlushPaint)

        // eye white
        birdEyePaint.color = Color.WHITE
        canvas.drawCircle(r * 0.35f, -r * 0.2f, r * 0.32f, birdEyePaint)

        // pupil (slight right gaze)
        birdPupilPaint.color = Color.parseColor("#1a252f")
        canvas.drawCircle(r * 0.44f, -r * 0.22f, r * 0.17f, birdPupilPaint)

        // eye shine
        birdEyePaint.color = Color.WHITE
        canvas.drawCircle(r * 0.50f, -r * 0.30f, r * 0.07f, birdEyePaint)

        // beak
        birdBeakPaint.color = Color.parseColor("#e74c3c")
        val beakPath = Path().apply {
            moveTo(r * 0.55f, -r * 0.05f)
            lineTo(r * 1.10f, r * 0.10f)
            lineTo(r * 0.55f, r * 0.25f)
            close()
        }
        canvas.drawPath(beakPath, birdBeakPaint)
        // beak line
        birdBeakPaint.color = Color.parseColor("#c0392b")
        birdBeakPaint.style = Paint.Style.STROKE
        birdBeakPaint.strokeWidth = 2f
        canvas.drawLine(r * 0.55f, r * 0.10f, r * 1.10f, r * 0.10f, birdBeakPaint)
        birdBeakPaint.style = Paint.Style.FILL

        canvas.restore()
    }

    private fun drawParticles(canvas: Canvas) {
        for (p in particles) {
            val a = (p.life * 255f).toInt().coerceIn(0, 255)
            val radius = p.life * 10f + 3f
            particlePaint.color = p.color
            particlePaint.alpha = a
            canvas.drawCircle(p.x, p.y, radius, particlePaint)
        }
    }

    // ─────────────────────────────────────────
    //  UI overlays
    // ─────────────────────────────────────────
    private fun drawScore(canvas: Canvas) {
        if (state == GameState.DEAD) return  // drawn in dead UI
        scoreShadow.textSize = 90f
        scorePaint.textSize  = 90f
        canvas.drawText(score.toString(), sw / 2f + 4f, 140f + 4f, scoreShadow)
        canvas.drawText(score.toString(), sw / 2f,      140f,       scorePaint)
    }

    private fun drawIdleUI(canvas: Canvas) {
        // Title
        scorePaint.textSize = 80f
        scoreShadow.textSize = 80f
        canvas.drawText("FLAPPY BIRD", sw / 2f + 3f, sh * 0.22f + 3f, scoreShadow)
        canvas.drawText("FLAPPY BIRD", sw / 2f,       sh * 0.22f,      scorePaint)

        // Tap hint (pulsing)
        val pulse = (sin(hintPulse) * 0.35f + 0.65f)
        hintPaint.color = Color.WHITE
        hintPaint.alpha = (pulse * 255f).toInt()
        canvas.drawText("TAP TO START", sw / 2f, sh * 0.82f, hintPaint)
        hintPaint.alpha = 255

        // Best score chip
        if (bestScore > 0) {
            drawChip(canvas, "BEST: $bestScore", sw / 2f, sh * 0.88f)
        }
    }

    private fun drawDeadUI(canvas: Canvas) {
        // Dim overlay
        val dimPaint = Paint()
        dimPaint.color = Color.argb(140, 0, 0, 0)
        canvas.drawRect(0f, 0f, sw, sh, dimPaint)

        val panelW = sw * 0.78f
        val panelH = sh * 0.38f
        val px = (sw - panelW) / 2f
        val py = sh * 0.28f

        // Panel
        panelPaint.color = Color.parseColor("#DD0f3460")
        canvas.drawRoundRect(RectF(px, py, px + panelW, py + panelH), 28f, 28f, panelPaint)
        panelBorder.color = Color.parseColor("#f1c40f")
        canvas.drawRoundRect(RectF(px, py, px + panelW, py + panelH), 28f, 28f, panelBorder)

        // Game Over text
        scorePaint.textSize  = 72f
        scoreShadow.textSize = 72f
        canvas.drawText("GAME OVER", sw / 2f + 3f, py + 68f + 3f, scoreShadow)
        canvas.drawText("GAME OVER", sw / 2f,       py + 68f,       scorePaint)

        // Score
        uiPaint.textSize = 52f
        uiPaint.color    = Color.parseColor("#ecf0f1")
        canvas.drawText("Score: $score", sw / 2f, py + 140f, uiPaint)

        // Best
        uiPaint.color = Color.parseColor("#f1c40f")
        canvas.drawText("Best:  $bestScore", sw / 2f, py + 200f, uiPaint)

        // Tap to restart
        val pulse = (sin(hintPulse) * 0.35f + 0.65f)
        hintPaint.color = Color.WHITE
        hintPaint.alpha = (pulse * 255f).toInt()
        canvas.drawText("TAP TO PLAY AGAIN", sw / 2f, py + panelH + 64f, hintPaint)
        hintPaint.alpha = 255
    }

    private fun drawChip(canvas: Canvas, text: String, cx: Float, cy: Float) {
        uiPaint.textSize = 36f
        val tw = uiPaint.measureText(text)
        val pad = 20f
        panelPaint.color = Color.parseColor("#AA0f3460")
        canvas.drawRoundRect(
            RectF(cx - tw/2 - pad, cy - 28f, cx + tw/2 + pad, cy + 12f),
            16f, 16f, panelPaint
        )
        uiPaint.color = Color.parseColor("#f1c40f")
        canvas.drawText(text, cx, cy, uiPaint)
        uiPaint.color = Color.WHITE
    }

    private fun drawFlash(canvas: Canvas) {
        if (flashAlpha > 0f) {
            flashPaint.alpha = (flashAlpha * 255f).toInt()
            canvas.drawRect(0f, 0f, sw, sh, flashPaint)
        }
    }
}
