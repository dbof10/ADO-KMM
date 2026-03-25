import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.tasks.JavaExec

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = "dev.azure.desktop"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":feature-login"))
    implementation(project(":feature-pr"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.snipme.highlights)
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
}

compose.desktop {
    application {
        mainClass = "dev.azure.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ADO Desktop"
            packageVersion = "1.0.0"
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    val launcherProvider = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    javaLauncher.set(launcherProvider)
    doFirst {
        executable = javaLauncher.get().executablePath.asFile.absolutePath
    }
}
