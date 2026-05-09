package com.idlekingdom

import android.content.Context

object GameData {
    var coins = 0
    var jumpLevel = 0
    var speedLevel = 0

    fun save(ctx: Context) {
        val sp = ctx.getSharedPreferences("game", Context.MODE_PRIVATE)
        sp.edit()
            .putInt("coins", coins)
            .putInt("jump", jumpLevel)
            .putInt("speed", speedLevel)
            .apply()
    }

    fun load(ctx: Context) {
        val sp = ctx.getSharedPreferences("game", Context.MODE_PRIVATE)
        coins = sp.getInt("coins", 0)
        jumpLevel = sp.getInt("jump", 0)
        speedLevel = sp.getInt("speed", 0)
    }
}
