plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "org.rhythmeta.chunithmd.shared"
        compileSdk = 37
        minSdk = 29
        withHostTest { }
    }
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.11.0")
            implementation(libs.ktor.client.core)
            implementation(libs.okio)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // 1.23.2 dispatches SME2 instructions on SME-only chips (e.g. SM8850), causing SIGILL.
            // https://github.com/microsoft/onnxruntime/issues/26377
            implementation("com.microsoft.onnxruntime:onnxruntime-android:1.30.0")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        }
    }
}
