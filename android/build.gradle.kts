import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.TaskAction
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

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
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
        // x86_64 MUST come before x86 to avoid substring mismatch
        val abiMap = linkedMapOf(
            "natives-armeabi-v7a" to "armeabi-v7a",
            "natives-arm64-v8a"   to "arm64-v8a",
            "natives-x86_64"      to "x86_64",
            "natives-x86"         to "x86"
        )

        val allJars = nativeFiles.files
        if (allJars.isEmpty()) {
            throw GradleException(
                "copyAndroidNatives: no native jars found in 'natives' configuration. " +
                "Check your dependencies block."
            )
        }

        val expectedAbis = abiMap.keys.toMutableSet()
        val copiedAbis = mutableSetOf<String>()

        allJars.forEach { jar ->
            val entry = abiMap.entries.firstOrNull { jar.name.contains(it.key) }
                ?: throw GradleException(
                    "copyAndroidNatives: unrecognised native jar '${jar.name}'. " +
                    "Expected one of: ${abiMap.keys.joinToString()}"
                )

            val abi = entry.value
            val outDir = outputDir.dir(abi).get().asFile.also { it.mkdirs() }

            val before = outDir.listFiles()?.count { it.extension == "so" } ?: 0

            fs.copy {
                from(archives.zipTree(jar))
                into(outDir)
                include("*.so")
            }

            val after = outDir.listFiles()?.count { it.extension == "so" } ?: 0

            if (after == 0 || after == before) {
                throw GradleException(
                    "copyAndroidNatives: failed to copy any .so files from '${jar.name}' " +
                    "into '${outDir.absolutePath}'. The jar may be empty or malformed."
                )
            }

            copiedAbis.add(entry.key)
            logger.lifecycle("copyAndroidNatives: copied ${after - before} .so file(s) for $abi")
        }

        val missingAbis = expectedAbis - copiedAbis
        if (missingAbis.isNotEmpty()) {
            throw GradleException(
                "copyAndroidNatives: missing native jars for: ${missingAbis.joinToString()}. " +
                "Add the corresponding natives() dependencies in build.gradle.kts."
            )
        }

        logger.lifecycle("copyAndroidNatives: all ABIs copied successfully -> ${outputDir.get().asFile.absolutePath}")
    }
}

tasks.register<CopyAndroidNativesTask>("copyAndroidNatives") {
    nativeFiles.from(natives)
    outputDir.set(layout.projectDirectory.dir("libs"))
}

tasks.configureEach {
    if (name == "preBuild") dependsOn("copyAndroidNatives")
}
