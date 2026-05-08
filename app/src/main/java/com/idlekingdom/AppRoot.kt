package com.idlekingdom

import androidx.compose.runtime.*
import com.idlekingdom.engine.FlappyEngine
import com.idlekingdom.model.*
import com.idlekingdom.ui.*

sealed class Scene {
    object Menu : Scene()
    object Game : Scene()
    object Upgrade : Scene()
}

@Composable
fun AppRoot() {

    val engine = remember { FlappyEngine() }

    var scene by remember { mutableStateOf<Scene>(Scene.Menu) }
    var kingdom by remember { mutableStateOf(KingdomState()) }

    when (scene) {

        Scene.Menu -> MenuScreen(
            kingdom = kingdom,
            onPlay = { scene = Scene.Game },
            onUpgrade = { scene = Scene.Upgrade }
        )

        Scene.Game -> GameScreen(
            engine = engine,
            kingdom = kingdom,
            onExit = {
                kingdom = it.kingdom
                scene = Scene.Menu
            }
        )

        Scene.Upgrade -> UpgradeScreen(
            kingdom = kingdom,
            onUpdate = { kingdom = it },
            onBack = { scene = Scene.Menu }
        )
    }
}
