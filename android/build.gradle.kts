plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {

    namespace = "com.stickrun.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stickrun.game"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {

    implementation(project(":core"))

    // core libGDX (IMPORTANT: DO NOT use catalog for now)
    implementation("com.badlogicgames.gdx:gdx:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.12.1")

    // natives (THIS is what provides libgdx.so)
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-armeabi-v7a")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-arm64-v8a")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86_64")

    // Box2D core
    implementation("com.badlogicgames.gdx:gdx-box2d:1.12.1")

    // Box2D natives
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:1.12.1:natives-armeabi-v7a")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:1.12.1:natives-arm64-v8a")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:1.12.1:natives-x86")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:1.12.1:natives-x86_64")
}
