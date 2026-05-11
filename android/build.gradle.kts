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

    implementation(libs.gdx.core)
    implementation(libs.gdx.backend.android)

    implementation("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-armeabi-v7a")
    implementation("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-arm64-v8a")
    implementation("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86")
    implementation("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86_64")

    implementation("com.badlogicgames.gdx:gdx-box2d:${libs.versions.gdx.get()}")

    implementation("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-armeabi-v7a")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-arm64-v8a")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-x86")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-x86_64")
}
