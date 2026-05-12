package com.stickrun.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.stickrun.game.screens.MenuScreen

class StickRunGame : Game() {
    lateinit var batch: SpriteBatch
    lateinit var shape: ShapeRenderer

    override fun create() {
        batch = SpriteBatch()
        shape = ShapeRenderer()
        Assets.load()
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        shape.dispose()
        Assets.dispose()
        screen?.dispose()
    }
}
