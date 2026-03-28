import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

group = "dev.azure.desktop"
version = "1.0.2"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosArm64()
    iosSimulatorArm64()
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines.core)
            api(libs.flowredux)
        }
        jvmMain.dependencies {
            implementation(project(":feature-login"))
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.logging)
        }
        androidMain.dependencies {
            implementation(project(":feature-login"))
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.logging)
        }
        iosMain.dependencies {
            implementation(project(":feature-login"))
            implementation(libs.ktor.client.darwin)
            implementation(libs.ktor.client.logging)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

android {
    namespace = "dev.azure.desktop.featurepr"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
