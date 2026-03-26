plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

tasks.register("compileAllTargets") {
    group = "build"
    description =
        "Compiles composeApp for desktop JVM, Android (debug), iOS Simulator arm64, and iOS device arm64."
    dependsOn(
        ":composeApp:compileKotlinDesktop",
        ":composeApp:compileDebugKotlinAndroid",
        ":composeApp:compileKotlinIosSimulatorArm64",
        ":composeApp:compileKotlinIosArm64",
    )
}
