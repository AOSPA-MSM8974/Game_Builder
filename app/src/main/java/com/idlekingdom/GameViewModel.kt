package com.idlekingdom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state = _state.asStateFlow()

    private var bot = BotGenerator.generate(1)

    init {
        startIdleLoop()
    }

    private fun startIdleLoop() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                _state.value = s.copy(
                    gold = s.gold + s.goldPerSecond
                )
            }
        }
    }

    fun attackBot() {
        val s = _state.value

        val playerPower = s.level + Random.nextInt(1, 5)
        val botPower = bot.defense + Random.nextInt(1, 5)

        if (playerPower > botPower) {
            val loot = bot.gold / 2

            _state.value = s.copy(
                gold = s.gold + loot,
                enemyHp = s.enemyMaxHp
            )

            bot = BotGenerator.generate(s.level + 1)
        } else {
            _state.value = s.copy(
                gold = (s.gold * 0.95).toLong()
            )
        }
    }

    fun upgrade() {
        val s = _state.value
        if (s.gold < s.upgradeCost) return

        _state.value = s.copy(
            gold = s.gold - s.upgradeCost,
            goldPerSecond = s.goldPerSecond + 1,
            upgradeCost = s.upgradeCost * 2,
            level = s.level + 1
        )
    }
}
