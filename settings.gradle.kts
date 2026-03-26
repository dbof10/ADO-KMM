import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

rootProject.name = "ado-desktop"

/**
 * If [local.properties] has no usable sdk.dir, set it from ANDROID_SDK_ROOT,
 * ANDROID_HOME, or the default macOS SDK path so Android modules configure without hand-editing.
 */
fun ensureAndroidSdkInLocalProperties(rootDir: File) {
    val fromEnv = System.getenv("ANDROID_SDK_ROOT")?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv("ANDROID_HOME")?.trim()?.takeIf { it.isNotEmpty() }
    val macDefault = File(System.getProperty("user.home"), "Library/Android/sdk")
    val sdkPath = fromEnv ?: macDefault.absolutePath.takeIf { macDefault.isDirectory } ?: return

    val local = File(rootDir, "local.properties")
    val props = Properties()
    if (local.exists()) {
        FileInputStream(local).use { props.load(it) }
    }
    val existing = props.getProperty("sdk.dir")?.trim().orEmpty()
    val existingOk = existing.isNotEmpty() && File(existing).isDirectory
    if (!existingOk) {
        props.setProperty("sdk.dir", sdkPath)
        FileOutputStream(local).use { out ->
            props.store(out, "sdk.dir from env or default location; edit if your SDK lives elsewhere")
        }
    }
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application", "com.android.library" ->
                    useModule("com.android.tools.build:gradle:${requested.version}")
                else -> Unit
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

ensureAndroidSdkInLocalProperties(settings.rootDir)

include(":common", ":feature-login", ":feature-pr", ":composeApp")
