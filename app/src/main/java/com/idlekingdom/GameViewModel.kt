package com.idlekingdom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state = _state.asStateFlow()

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

    fun attackEnemy() {
        val s = _state.value
        val newHp = s.enemyHp - 1

        if (newHp <= 0) {
            _state.value = s.copy(
                gold = s.gold + 5,
                enemyHp = s.enemyMaxHp
            )
        } else {
            _state.value = s.copy(enemyHp = newHp)
        }
    }

    fun buyUpgrade() {
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
