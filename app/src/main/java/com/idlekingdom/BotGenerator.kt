package com.idlekingdom

object BotGenerator {

    private val names = listOf(
        "Iron Keep", "Darkhold", "Ashen City", "Ravenfort", "Bonewall"
    )

    fun generate(level: Int): BotCity {
        val scale = 1 + level

        return BotCity(
            name = names.random(),
            gold = (50 * scale).toLong(),
            defense = 5 * scale
        )
    }
}
