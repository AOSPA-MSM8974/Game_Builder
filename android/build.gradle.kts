import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.*
import javax.inject.Inject

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
    implementation(libs.kotlin.stdlib)

    natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86_64")

    natives("com.badlogicgames.gdx:gdx-box2d:${libs.versions.gdx.get()}:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-box2d:${libs.versions.gdx.get()}:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-box2d:${libs.versions.gdx.get()}:natives-x86")
    natives("com.badlogicgames.gdx:gdx-box2d:${libs.versions.gdx.get()}:natives-x86_64")
}

/* ---------------- FIXED COPY TASK ---------------- */

abstract class CopyAndroidNativesTask @Inject constructor(
    private val fs: FileSystemOperations,
    private val archives: ArchiveOperations
) : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val nativeFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {

        val abiMap = mapOf(
            "natives-armeabi-v7a" to "armeabi-v7a",
            "natives-arm64-v8a" to "arm64-v8a",
            "natives-x86" to "x86",
            "natives-x86_64" to "x86_64"
        )

        nativeFiles.forEach { jar ->

            val abi = abiMap.entries
                .firstOrNull { jar.name.contains(it.key) }
                ?.value ?: return@forEach

            val outDir = outputDir.dir(abi).get().asFile
            outDir.mkdirs()

            fs.copy {
                from(archives.zipTree(jar))
                into(outDir)
                include("*.so")
            }
        }
    }
}

/* ---------------- TASK WIRING ---------------- */

tasks.register<CopyAndroidNativesTask>("copyAndroidNatives") {

    nativeFiles.from(natives)

    outputDir.set(
        layout.buildDirectory.dir("androidNatives")
    )
}

tasks.named("preBuild") {
    dependsOn("copyAndroidNatives")
}
