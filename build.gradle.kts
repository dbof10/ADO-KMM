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

tasks.register("testAllTargets") {
    group = "verification"
    description =
        "Runs JVM unit tests, composeApp desktop tests, and Android debug unit tests for all KMP modules. " +
            "(iOS Simulator tests are in testIosSimulatorArm64All; they can hang or be slow in some environments.)"
    dependsOn(
        ":common:jvmTest",
        ":feature-login:jvmTest",
        ":feature-pr:jvmTest",
        ":composeApp:desktopTest",
        ":common:testDebugUnitTest",
        ":feature-login:testDebugUnitTest",
        ":feature-pr:testDebugUnitTest",
        ":composeApp:testDebugUnitTest",
    )
}

tasks.register("testIosSimulatorArm64All") {
    group = "verification"
    description =
        "Runs Kotlin/Native unit tests on iOS Simulator arm64 for all KMP modules (requires Xcode/simulator; may be slow)."
    dependsOn(
        ":common:iosSimulatorArm64Test",
        ":feature-login:iosSimulatorArm64Test",
        ":feature-pr:iosSimulatorArm64Test",
        ":composeApp:iosSimulatorArm64Test",
    )
}
