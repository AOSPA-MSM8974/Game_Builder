package com.idlekingdom

data class GameState(
    val gold: Long = 0,
    val goldPerSecond: Long = 1,
    val level: Int = 1,
    val upgradeCost: Long = 10,
    val enemyHp: Int = 10,
    val enemyMaxHp: Int = 10
)
