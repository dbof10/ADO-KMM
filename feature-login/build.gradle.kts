import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "dev.azure.desktop"
version = "1.0.0"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            api(libs.kotlinx.coroutines.core)
            api(libs.flowredux)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.logging)
            implementation(libs.microsoft.credential.secure.storage)
            implementation(libs.slf4j.nop)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
