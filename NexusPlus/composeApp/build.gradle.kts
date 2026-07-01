import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-Xexpect-actual-classes",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=kotlin.RequiresOptIn",
            )
        }
    }
    jvm("desktop")
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.androidx.navigation.compose)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.coroutines.core)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.process)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.palette)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.androidx.camera.mlkit)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.exoplayer.hls)
            implementation(libs.media3.ui)
            implementation(libs.media3.session)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.okhttp)
            implementation(libs.retrofit)
            implementation(libs.retrofit.gson)
            implementation(libs.guava)
            implementation(libs.coil.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.mlkit.translate)
            implementation(libs.mlkit.image.labeling)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.accompanist.permissions)
            implementation(libs.zxing.core)
            implementation(project.dependencies.platform(libs.firebase.bom.get()))
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.config)
            implementation(libs.google.play.auth)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services)
            implementation(libs.googleid)
            implementation(libs.unity.ads)
        }

        val desktopMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
                implementation(libs.ktor.client.java)
            }
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.nexuswavetech.nexusplus.composeapp"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        targetSdk = 35

        // ═════════════════════════════════════════════════════════════
        // SECRETS INJECTION VIA GRADLE — Require secrets only from CI (-P properties)
        // No local System.getenv fallbacks allowed. Build will fail fast if a required secret is missing.
        // ═════════════════════════════════════════════════════════════

        fun requireSecret(key: String): String {
            return project.findProperty(key)?.toString()
                ?: throw GradleException("Missing required secret Gradle project property '$' + key + "'. Provide it via -P<key> in CI (GitHub Actions secrets).")
        }

        val webClient = requireSecret("WEB_CLIENT_ID")
        val gemini = requireSecret("GEMINI_API_KEY")
        val admobApp = requireSecret("ADMOB_APP_ID")
        val admobBanner = requireSecret("ADMOB_BANNER_ID")
        val admobInterstitial = requireSecret("ADMOB_INTERSTITIAL_ID")

        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClient\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$gemini\"")
        buildConfigField("String", "ADMOB_APP_ID", "\"$admobApp\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBanner\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitial\"")

        manifestPlaceholders["WEB_CLIENT_ID"] = webClient
        manifestPlaceholders["ADMOB_APP_ID"] = admobApp
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "google-services.json"
            )
            pickFirsts += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
    }
}
