package com.maxi.flappybird

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(RpgReleaseEngine(this))
    }
}

/* =========================================================
   VERSION 6 — RELEASE RPG ENGINE (VERTICAL SLICE)
========================================================= */

class RpgReleaseEngine(context: Context) : SurfaceView(context), Runnable, SurfaceHolder.Callback {

    private var thread = Thread(this)
    private var running = false

    private val paint = Paint()
    private val prefs = context.getSharedPreferences("rpg_v6", Context.MODE_PRIVATE)

    /* =========================================================
       SCENES (REAL GAME STRUCTURE)
    ========================================================= */
    private enum class Scene { TOWN, WORLD, DUNGEON, BATTLE, INVENTORY, DIALOGUE, SHOP }
    private var scene = Scene.TOWN

    /* =========================================================
       SAVE SYSTEM (MULTI SLOT READY)
    ========================================================= */
    private var saveSlot = 0

    /* =========================================================
       TILE WORLD
    ========================================================= */
    private val tile = 70
    private val mapW = 18
    private val mapH = 12

    private val map = Array(mapH) {
        IntArray(mapW) { if (Random.nextFloat() < 0.12f) 1 else 0 }
    }

    /* =========================================================
       ENTITY CORE
    ========================================================= */
    open class Entity(
        var x: Float,
        var y: Float,
        var hp: Int,
        var maxHp: Int
    )

    /* =========================================================
       PLAYER (FULL RPG STATS)
    ========================================================= */
    class Player(x: Float, y: Float, hp: Int, maxHp: Int) : Entity(x, y, hp, maxHp) {
        var atk = 25
        var def = 8
        var crit = 15
        var level = 1
        var xp = 0
        var gold = 0

        var frame = 0
        var facing = 0 // 0 up 1 down 2 left 3 right
        var anim = 0f
    }

    private val player = Player(3f, 3f, load("hp", 250), load("maxHp", 250))

    /* =========================================================
       EQUIPMENT SYSTEM (REAL RPG STYLE)
    ========================================================= */
    data class Item(
        val name: String,
        val atk: Int,
        val def: Int,
        val rarity: Int // 0 common, 1 rare, 2 epic
    )

    private var weapon = Item("Iron Sword", 8, 0, 1)
    private var armor = Item("Leather Armor", 0, 6, 1)

    /* =========================================================
       INVENTORY GRID
    ========================================================= */
    private val inventory = MutableList<Item?>(16) { null }
    private var selectedSlot = 0

    /* =========================================================
       ENEMY SYSTEM (DUNGEON READY)
    ========================================================= */
    class Enemy(x: Float, y: Float, hp: Int, maxHp: Int) : Entity(x, y, hp, maxHp) {
        var atk = 12
        var frame = 0
        var anim = 0f
    }

    private val enemies = mutableListOf<Enemy>()

    /* =========================================================
       DUNGEON SYSTEM (ROOM BASED)
    ========================================================= */
    private var dungeonRoom = 0
    private val dungeonMaxRooms = 5

    /* =========================================================
       QUEST SYSTEM
    ========================================================= */
    private var questStage = load("quest", 0)

    private val quests = listOf(
        "Enter Dungeon",
        "Kill 3 Enemies",
        "Find Treasure",
        "Defeat Boss"
    )

    /* =========================================================
       INPUT
    ========================================================= */
    private var dx = 0f
    private var dy = 0f

    /* =========================================================
       LOOP
    ========================================================= */
    init {
        holder.addCallback(this)
        spawnDungeon()
    }

    override fun run() {
        var last = System.currentTimeMillis()

        while (running) {
            if (!holder.surface.isValid) continue

            val now = System.currentTimeMillis()
            val dt = (now - last) / 1000f
            last = now

            update(dt)
            render()
        }
    }

    /* =========================================================
       UPDATE ENGINE
    ========================================================= */

    private fun update(dt: Float) {

        updatePlayer(dt)

        when (scene) {

            Scene.WORLD, Scene.TOWN -> {
                updateEnemies(dt)
                checkNpcInteraction()
            }

            Scene.DUNGEON -> {
                updateEnemies(dt)
                dungeonLogic()
            }

            Scene.BATTLE -> battleSystem()
            else -> {}
        }
    }

    /* =========================================================
       PLAYER MOVEMENT + ANIMATION
    ========================================================= */

    private fun updatePlayer(dt: Float) {

        val speed = 2.8f * dt

        if (dx != 0f || dy != 0f) {
            player.x += dx * speed
            player.y += dy * speed

            player.anim += dt
            if (player.anim > 0.15f) {
                player.frame = (player.frame + 1) % 4
                player.anim = 0f
            }

            player.facing = when {
                abs(dx) > abs(dy) && dx > 0 -> 3
                abs(dx) > abs(dy) && dx < 0 -> 2
                dy > 0 -> 1
                else -> 0
            }
        }
    }

    /* =========================================================
       ENEMY AI
    ========================================================= */

    private fun updateEnemies(dt: Float) {
        enemies.forEach { e ->
            val dx = player.x - e.x
            val dy = player.y - e.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 1.2f) {
                scene = Scene.BATTLE
            } else {
                e.x += (dx / max(1f, dist)) * dt
                e.y += (dy / max(1f, dist)) * dt
            }

            e.anim += dt
            if (e.anim > 0.2f) {
                e.frame = (e.frame + 1) % 2
                e.anim = 0f
            }
        }
    }

    /* =========================================================
       DUNGEON LOGIC
    ========================================================= */

    private fun dungeonLogic() {
        if (enemies.isEmpty()) {
            dungeonRoom++

            if (dungeonRoom >= dungeonMaxRooms) {
                scene = Scene.TOWN
                player.gold += 100
                dungeonRoom = 0
                spawnDungeon()
            } else {
                spawnDungeon()
            }
        }
    }

    /* =========================================================
       BATTLE SYSTEM (FINALIZED CORE)
    ========================================================= */

    private fun battleSystem() {

        if (enemies.isEmpty()) {
            scene = Scene.WORLD
            return
        }

        val e = enemies.first()

        val dmg = player.atk + weapon.atk + Random.nextInt(0, 5)
        e.hp -= dmg

        if (e.hp > 0) {
            player.hp -= max(1, e.atk - (player.def + armor.def))
        }

        if (e.hp <= 0) {
            player.gold += 25
            player.xp += 15

            dropLoot()

            enemies.removeAt(0)

            if (player.xp >= player.level * 50) {
                player.level++
                player.xp = 0
                player.atk += 3
                player.maxHp += 20
                player.hp = player.maxHp
            }

            scene = Scene.DUNGEON
        }

        if (player.hp <= 0) {
            player.hp = player.maxHp
            scene = Scene.TOWN
        }
    }

    /* =========================================================
       LOOT SYSTEM
    ========================================================= */

    private fun dropLoot() {
        val r = Random.nextInt(100)

        val item = when {
            r < 60 -> Item("Steel Sword", 10, 0, 0)
            r < 90 -> Item("Knight Blade", 18, 0, 1)
            else -> Item("Dragon Sword", 30, 5, 2)
        }

        for (i in inventory.indices) {
            if (inventory[i] == null) {
                inventory[i] = item
                break
            }
        }
    }

    /* =========================================================
       DUNGEON SPAWN
    ========================================================= */

    private fun spawnDungeon() {
        enemies.clear()

        val count = 3 + dungeonRoom

        repeat(count) {
            enemies.add(
                Enemy(
                    Random.nextInt(mapW).toFloat(),
                    Random.nextInt(mapH).toFloat(),
                    60 + dungeonRoom * 20,
                    60 + dungeonRoom * 20
                )
            )
        }
    }

    /* =========================================================
       NPC CHECK
    ========================================================= */

    private fun checkNpcInteraction() {
        if (player.x < 2 && player.y < 2) {
            scene = Scene.DIALOGUE
        }
    }

    /* =========================================================
       RENDER SYSTEM
    ========================================================= */

    private fun render() {
        val canvas = holder.lockCanvas()

        drawMap(canvas)
        drawEntities(canvas)
        drawUI(canvas)

        holder.unlockCanvasAndPost(canvas)
    }

    /* =========================================================
       MAP RENDER
    ========================================================= */

    private fun drawMap(canvas: Canvas) {
        for (y in map.indices) {
            for (x in map[y].indices) {

                paint.color = if (map[y][x] == 1)
                    Color.rgb(30, 90, 30)
                else
                    Color.rgb(80, 200, 80)

                canvas.drawRect(
                    x * tile.toFloat(),
                    y * tile.toFloat(),
                    (x + 1) * tile.toFloat(),
                    (y + 1) * tile.toFloat(),
                    paint
                )
            }
        }
    }

    /* =========================================================
       ENTITIES
    ========================================================= */

    private fun drawEntities(canvas: Canvas) {

        paint.color = Color.BLUE
        canvas.drawRect(
            player.x * tile,
            player.y * tile,
            player.x * tile + 50,
            player.y * tile + 50,
            paint
        )

        enemies.forEach {
            paint.color = if (it.maxHp > 120) Color.RED else Color.MAGENTA
            canvas.drawRect(
                it.x * tile,
                it.y * tile,
                it.x * tile + 50,
                it.y * tile + 50,
                paint
            )
        }
    }

    /* =========================================================
       UI
    ========================================================= */

    private fun drawUI(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.textSize = 38f

        canvas.drawText("LVL: ${player.level}", 40f, 60f, paint)
        canvas.drawText("HP: ${player.hp}/${player.maxHp}", 40f, 110f, paint)
        canvas.drawText("ATK: ${player.atk}", 40f, 160f, paint)
        canvas.drawText("GOLD: ${player.gold}", 40f, 210f, paint)
        canvas.drawText("SCENE: $scene", 40f, 260f, paint)
        canvas.drawText("QUEST: ${quests[questStage]}", 40f, 310f, paint)
    }

    /* =========================================================
       INPUT (JOYSTICK STYLE)
    ========================================================= */

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {

            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                dx = (event.x - width / 2) / (width / 2)
                dy = (event.y - height / 2) / (height / 2)
            }

            MotionEvent.ACTION_UP -> {
                dx = 0f
                dy = 0f
            }
        }

        return true
    }

    /* =========================================================
       SAVE SYSTEM
    ========================================================= */

    private fun load(k: String, d: Int) = prefs.getInt(k, d)

    private fun save() {
        prefs.edit()
            .putInt("hp", player.hp)
            .putInt("maxHp", player.maxHp)
            .putInt("quest", questStage)
            .apply()
    }

    /* =========================================================
       SURFACE
    ========================================================= */

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread(this)
        thread.start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        save()
        thread.join()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
}
