plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val natives: Configuration by configurations.creating

android {
    namespace = "com.stickrun.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stickrun.game"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
            assets.srcDirs("src/main/assets")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {

    implementation(project(":core"))
    implementation(libs.kotlin.stdlib)

    // LibGDX Android backend
    implementation(libs.gdx.backend.android)

    // LibGDX core
    implementation("com.badlogicgames.gdx:gdx:1.12.1")

    // Natives (correct way — no manual unpacking needed)
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-armeabi-v7a")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-arm64-v8a")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86_64")
}
