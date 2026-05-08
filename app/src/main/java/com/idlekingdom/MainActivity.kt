package com.idlekingdom

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var gold = 0
    private var food = 50
    private var troops = 0
    private var population = 5

    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            updateGame()
            updateUI()
            handler.postDelayed(this, 1000) // 1 sec tick
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)

        val trainBtn = findViewById<Button>(R.id.trainBtn)
        val upgradeBtn = findViewById<Button>(R.id.upgradeBtn)

        trainBtn.setOnClickListener { trainTroop() }
        upgradeBtn.setOnClickListener { upgrade() }

        handler.post(tick)
    }

    private fun updateGame() {
        food += population * 2
        gold += population * 3 + troops * 2
        food -= population

        if (food < 0) food = 0
    }

    private fun trainTroop() {
        if (food >= 20) {
            food -= 20
            troops++
        }
    }

    private fun upgrade() {
        val cost = population * 10
        if (gold >= cost) {
            gold -= cost
            population += 2
        }
    }

    private fun updateUI() {
        status.text = """
            Gold: $gold
            Food: $food
            Troops: $troops
            Population: $population
        """.trimIndent()
    }
}
