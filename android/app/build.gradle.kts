import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val chunithmdBuildNumber = providers.environmentVariable("CHUNITHMD_BUILD_NUMBER")
    .orElse(
        providers.exec {
            commandLine(
                "sh",
                rootProject.file("../scripts/build-number.sh").absolutePath,
            )
        }.standardOutput.asText.map(String::trim),
    )
    .map { value ->
        value.toIntOrNull()?.takeIf { it > 0 }
            ?: error("Invalid CHUNITHMD_BUILD_NUMBER: $value")
    }
fun gitBuildValue(vararg arguments: String) = providers.exec {
    workingDir(rootProject.projectDir.parentFile)
    commandLine("git", *arguments)
}.standardOutput.asText.map(String::trim)
val chunithmdGitSha = providers.environmentVariable("GITHUB_SHA")
    .orElse(gitBuildValue("rev-parse", "HEAD"))
val chunithmdGitBranch = providers.environmentVariable("GITHUB_REF_NAME")
    .orElse(gitBuildValue("rev-parse", "--abbrev-ref", "HEAD"))
val chunithmdBuildTime = providers.environmentVariable("CHUNITHMD_BUILD_TIME")
    .orElse(gitBuildValue("show", "-s", "--format=%cI", "HEAD"))
val chunithmdUpdateUrl = providers.gradleProperty("CHUNITHMD_UPDATE_URL")
    .orElse("https://assets.rhythmeta.org/android/chunithmd/ci/main/latest.json")
fun buildConfigString(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val splitReleaseApks = providers.gradleProperty("CHUNITHMD_SPLIT_RELEASE_APKS")
    .map { it.toBoolean() }
    .orElse(false)
val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { provider -> provider.orNull?.isNotBlank() == true }

android {
    namespace = "org.rhythmeta.chunithmd"
    buildFeatures { buildConfig = true }
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.rhythmeta.chunithmd"
        minSdk = 29
        targetSdk = 37
        versionCode = chunithmdBuildNumber.get()
        versionName = "0.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GIT_SHA", buildConfigString(chunithmdGitSha.get()))
        buildConfigField("String", "GIT_BRANCH", buildConfigString(chunithmdGitBranch.get()))
        buildConfigField("String", "BUILD_TIME", buildConfigString(chunithmdBuildTime.get()))
        buildConfigField("String", "UPDATE_URL", buildConfigString(chunithmdUpdateUrl.get()))
    }

    signingConfigs {
        getByName("debug") {
            enableV3Signing = true
        }
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseKeystorePath.get())
                storePassword = releaseKeystorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
                storeType = "PKCS12"
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            optimization {
                // Kotlin Multiplatform/Ktor file classes currently trigger an
                // IllegalAccessError when AGP optimization is enabled.
                enable = false
            }
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    splits {
        abi {
            isEnable = splitReleaseApks.get()
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${libs.versions.lifecycleRuntimeKtx.get()}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${libs.versions.lifecycleRuntimeKtx.get()}")
    implementation(project(":shared"))
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    implementation(libs.coil.compose)
    implementation(libs.miuix.nav)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material.kolor)
    implementation(libs.backdrop)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.protobuf.javalite)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    ksp(libs.androidx.room.compiler)
}
