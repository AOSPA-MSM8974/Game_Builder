package com.stickrun.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.Texture.TextureWrap
import com.badlogic.gdx.graphics.g2d.TextureRegion

object Assets {
    lateinit var crate:       Texture
    lateinit var platform:    Texture
    lateinit var pole:        Texture
    lateinit var saw:         Texture
    lateinit var coinSheet:   Texture
    lateinit var ground:      Texture
    lateinit var hudPanel:    Texture
    lateinit var buttonGreen: Texture
    lateinit var failedPanel: Texture
    lateinit var toolbar:     Texture
    lateinit var logo:        Texture

    lateinit var coinFrames: Array<TextureRegion>

    fun load() {
        crate       = t("textures/crate.png")
        platform    = t("textures/platform.png")
        pole        = t("textures/pole.png")
        saw         = t("textures/saw.png")
        coinSheet   = t("textures/coin.png")
        ground      = t("textures/ground.png", TextureWrap.Repeat, TextureWrap.ClampToEdge)
        hudPanel    = t("textures/hud_panel.png", TextureWrap.Repeat, TextureWrap.ClampToEdge)
        buttonGreen = t("textures/button_green.png")
        failedPanel = t("textures/failed_panel.png")
        toolbar     = t("textures/toolbar.png", TextureWrap.Repeat, TextureWrap.ClampToEdge)
        logo        = t("textures/logo.png")

        coinFrames = Array(8) { i -> TextureRegion(coinSheet, i * 64, 0, 64, 64) }
    }

    private fun t(path: String,
                  wu: TextureWrap = TextureWrap.ClampToEdge,
                  wv: TextureWrap = TextureWrap.ClampToEdge): Texture {
        val tex = Texture(Gdx.files.internal(path))
        tex.setFilter(TextureFilter.Linear, TextureFilter.Linear)
        tex.setWrap(wu, wv)
        return tex
    }

    fun dispose() {
        crate.dispose(); platform.dispose(); pole.dispose(); saw.dispose()
        coinSheet.dispose(); ground.dispose(); hudPanel.dispose()
        buttonGreen.dispose(); failedPanel.dispose(); toolbar.dispose(); logo.dispose()
    }
}
