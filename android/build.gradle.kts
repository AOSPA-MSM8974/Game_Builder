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

    natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86_64")
}

tasks.register("copyAndroidNatives") {
    doLast {
        val abiMap = mapOf(
            "natives-armeabi-v7a" to "armeabi-v7a",
            "natives-arm64-v8a"   to "arm64-v8a",
            "natives-x86"         to "x86",
            "natives-x86_64"      to "x86_64"
        )
        natives.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            val abi = abiMap.entries
                .firstOrNull { artifact.file.name.contains(it.key) }?.value ?: return@forEach
            val outDir = file("libs/$abi").also { it.mkdirs() }
            copy {
                from(zipTree(artifact.file))
                into(outDir)
                include("*.so")
            }
        }
    }
}

tasks.configureEach {
    if (name == "preBuild") dependsOn("copyAndroidNatives")
}
