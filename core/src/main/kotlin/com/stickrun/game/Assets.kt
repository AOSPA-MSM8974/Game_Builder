package com.stickrun.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.TextureRegion

object Assets {
    lateinit var crate:    Texture
    lateinit var glass:    Texture
    lateinit var saw:      Texture
    lateinit var coin:     Texture
    lateinit var ground:   Texture
    lateinit var sky:      Texture
    lateinit var button:   Texture
    lateinit var panel:    Texture
    lateinit var logo:     Texture
    lateinit var particle: Texture

    // Coin animation frames
    lateinit var coinFrames: Array<TextureRegion>

    fun load() {
        crate    = tex("textures/crate.png")
        glass    = tex("textures/glass.png")
        saw      = tex("textures/saw.png")
        coin     = tex("textures/coin.png")
        ground   = tex("textures/ground.png",  Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge)
        sky      = tex("textures/sky.png",     Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
        button   = tex("textures/button.png")
        panel    = tex("textures/panel.png")
        logo     = tex("textures/logo.png")
        particle = tex("textures/particle.png")

        // 8-frame coin sheet (each frame 64px wide)
        coinFrames = Array(8) { i ->
            TextureRegion(coin, i * 64, 0, 64, 64)
        }
    }

    private fun tex(
        path: String,
        wrapU: Texture.TextureWrap = Texture.TextureWrap.ClampToEdge,
        wrapV: Texture.TextureWrap = Texture.TextureWrap.ClampToEdge
    ): Texture {
        val t = Texture(Gdx.files.internal(path))
        t.setFilter(TextureFilter.Linear, TextureFilter.Linear)
        t.setWrap(wrapU, wrapV)
        return t
    }

    fun dispose() {
        crate.dispose(); glass.dispose(); saw.dispose()
        coin.dispose(); ground.dispose(); sky.dispose()
        button.dispose(); panel.dispose(); logo.dispose()
        particle.dispose()
    }
}
