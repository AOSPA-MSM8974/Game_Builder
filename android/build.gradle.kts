plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Separate configuration for LibGDX native .so files
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
    implementation(libs.gdx.backend.android)

    // Native .so libs — one per ABI
    natives(libs.gdx.platform.armeabi.v7a) { artifact { classifier = "natives-armeabi-v7a" } }
    natives(libs.gdx.platform.arm64.v8a)   { artifact { classifier = "natives-arm64-v8a"   } }
    natives(libs.gdx.platform.x86)         { artifact { classifier = "natives-x86"          } }
    natives(libs.gdx.platform.x86.64)      { artifact { classifier = "natives-x86-64"       } }
}

// Unpack .so files from the native JARs into android/libs/<abi>/
tasks.register("copyAndroidNatives") {
    doLast {
        val abiMap = mapOf(
            "natives-armeabi-v7a" to "armeabi-v7a",
            "natives-arm64-v8a"   to "arm64-v8a",
            "natives-x86"         to "x86",
            "natives-x86_64"      to "x86_64"
        )
        natives.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            val abi = abiMap.entries.firstOrNull { artifact.name.contains(it.key) }?.value ?: return@forEach
            val outDir = file("libs/$abi").also { it.mkdirs() }
            copy {
                from(zipTree(artifact.file))
                into(outDir)
                include("*.so")
            }
        }
    }
}

// Auto-run before every build so you never have to think about it
tasks.configureEach {
    if (name == "preBuild") dependsOn("copyAndroidNatives")
}
