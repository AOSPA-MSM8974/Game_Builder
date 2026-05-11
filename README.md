# Stick Run - Android Game (LibGDX + Kotlin)

A nostalgic recreation of the classic 2010s Stick Run game with orange atmosphere, box platforms, coins, and stick figure customization.

## Features
- 🟠 Authentic orange sunset atmosphere with parallax backgrounds
- 🏃 Animated stick figure with running legs and arm swing
- 📦 Varied platform types (normal, dark, highlighted boxes)
- 🪙 Coin collection with floating +10 score popups
- 🎩 Character customization: body color + hat (Cap, Top Hat, Beanie, None)
- 📱 Touch controls (left/right arrows + jump button)
- ⌨️ Keyboard controls (WASD / Arrow keys + Space)
- ♾️ Infinite procedural world generation
- 🎮 Double jump support

## Project Structure
```
StickRun/
├── core/                          # Platform-independent game code
│   └── src/main/kotlin/com/stickrun/game/
│       ├── StickRunGame.kt        # Main game class
│       ├── entities/
│       │   ├── Player.kt          # Stick figure with physics & drawing
│       │   ├── Coin.kt            # Animated coin collectibles
│       │   └── Platform.kt        # Box platform types
│       ├── screens/
│       │   ├── MenuScreen.kt      # Main menu + character customizer
│       │   └── GameScreen.kt      # Gameplay screen
│       └── world/
│           └── WorldGenerator.kt  # Level + procedural chunk generation
├── android/                       # Android launcher
│   └── src/main/kotlin/com/stickrun/game/android/
│       └── AndroidLauncher.kt
├── build.gradle
└── settings.gradle
```

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1) or newer
- JDK 17+
- Android SDK with API 34

### Steps
1. Open the `StickRun` folder in Android Studio
2. Let Gradle sync complete
3. Run `./gradlew android:copyAndroidNatives` once to copy native libs
4. Connect an Android device or start an emulator
5. Press **Run ▶** (select `android` configuration)

### Controls

**Keyboard:**
| Key | Action |
|-----|--------|
| A / ← | Move Left |
| D / → | Move Right |
| W / ↑ / Space | Jump (double jump supported) |

**Touch (landscape):**
| Area | Action |
|------|--------|
| Left 25% of screen (bottom) | Move Left |
| 25–50% of screen (bottom) | Move Right |
| Right 30% of screen (bottom) | Jump |

## Gameplay
- Run right to increase your distance score
- Collect gold coins for +10 points each
- Fall off the bottom of the screen = Game Over
- The world extends infinitely to the right
- Tap the menu arrows to change hat style and body color before playing

## Technical Notes
- Built with **LibGDX 1.12.1** + **Kotlin 1.9.22**
- Uses `ShapeRenderer` for all graphics (no sprite assets needed!)
- Camera uses lerp smoothing for that floaty feel
- Procedural generation uses seeded Random for reproducible chunks
- Supports armeabi-v7a, arm64-v8a, x86, x86_64 native builds
