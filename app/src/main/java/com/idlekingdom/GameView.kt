package com.idlekingdom

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), Runnable {

    private var thread: Thread? = null
    private var running = false
    private val holder = holder

    private var state = 0
    private var countdown = 3f

    private var x = 200f
    private var y = 500f
    private var vy = 0f

    private val gravity = 2000f
    private val jump = -650f

    private var pipeX = 1200f
    private var pipeTop = 400f
    private val gap = 450f

    private var score = 0
    private var coins = 0

    private val paint = Paint()

    override fun run() {
        var last = System.nanoTime()

        while (running) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1e9).toFloat()
            last = now

            update(dt)
            draw()

            Thread.sleep(16)
        }
    }

    private fun update(dt: Float) {

        if (state == 0) {
            countdown -= dt
            if (countdown <= 0f) state = 1
            return
        }

        if (state != 2) return

        vy += gravity * dt
        y += vy * dt

        pipeX -= 500f * dt

        if (pipeX < -200f) {
            pipeX = width.toFloat()
            pipeTop = (300..900).random().toFloat()
        }

        if (pipeX + 160 < x) {
            score++
            coins++
        }

        if (collision()) {
            state = 3
        }
    }

    private fun collision(): Boolean {
        val hitX = x + 80 > pipeX && x < pipeX + 160
        if (!hitX) return false

        val hitTop = y < pipeTop
        val hitBottom = y + 80 > pipeTop + gap

        return hitTop || hitBottom
    }

    override fun draw() {
        if (!holder.surface.isValid) return

        val c = holder.lockCanvas()

        c.drawColor(Color.CYAN)

        paint.color = Color.GREEN
        c.drawRect(pipeX, 0f, pipeX + 160, pipeTop, paint)
        c.drawRect(pipeX, pipeTop + gap, pipeX + 160, height.toFloat(), paint)

        paint.color = Color.YELLOW
        c.drawCircle(x, y, 40f, paint)

        paint.color = Color.WHITE
        paint.textSize = 60f
        c.drawText("Score: $score", 50f, 100f, paint)

        holder.unlockCanvasAndPost(c)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (state == 1) state = 2
            if (state == 2) vy = jump
            if (state == 3) restart()
        }
        return true
    }

    private fun restart() {
        state = 0
        countdown = 3f
        score = 0
        y = 500f
        vy = 0f
        pipeX = width.toFloat()
    }

    fun resume() {
        running = true
        thread = Thread(this)
        thread?.start()
    }

    fun pause() {
        running = false
        thread?.join()
    }
}
