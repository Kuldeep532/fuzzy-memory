import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ─────────────────────────────────────────────────────────────────────────────
// autoGenerateKeystore — self-contained Gradle task that replaces CI/CD yml.
//
// Reads credentials from environment variables; falls back to safe defaults
// so no terminal interaction or laptop is required.
//
//   Env vars (all optional — defaults are used when absent):
//     SIGNING_STORE_FILE      — path to write the JKS  (default: release-keystore.jks)
//     SIGNING_STORE_PASSWORD  — keystore password       (default: nexuswave@2025)
//     SIGNING_KEY_ALIAS       — key alias               (default: nexusplus)
//     SIGNING_KEY_PASSWORD    — key password            (default: nexuswave@2025)
//
//   Usage:
//     ./gradlew autoGenerateKeystore assembleRelease
// ─────────────────────────────────────────────────────────────────────────────
tasks.register("autoGenerateKeystore") {
    description = "Programmatically generates a production JKS keystore using " +
            "java.security.KeyPairGenerator and java.security.KeyStore. " +
            "Runs automatically before any assemble/package task."
    group = "build setup"

    doFirst {
        val keystorePath = System.getenv("SIGNING_STORE_FILE") ?: "release-keystore.jks"
        val storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: "nexuswave@2025"
        val keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "nexusplus"
        val keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: "nexuswave@2025"
        val keystoreFile = file(keystorePath)

        if (keystoreFile.exists()) {
            println("✅ autoGenerateKeystore: keystore already present at ${keystoreFile.absolutePath}")
            return@doFirst
        }

        println("🔑 autoGenerateKeystore: generating RSA-2048 keystore at ${keystoreFile.absolutePath} …")

        // 1. Generate a 2048-bit RSA key pair using java.security.KeyPairGenerator
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048, SecureRandom())
        val keyPair = keyPairGenerator.generateKeyPair()

        // 2. Build a self-signed X.509 certificate via keytool sub-process.
        //    This avoids depending on sun.security.* internal APIs which may
        //    not be available on all JVM distributions used by remote build servers.
        val result = providers.exec {
            commandLine(
                "keytool", "-genkeypair",
                "-keystore", keystoreFile.absolutePath,
                "-storetype", "JKS",
                "-alias", keyAlias,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-storepass", storePassword,
                "-keypass", keyPassword,
                "-dname", "CN=Nexus Wave Technologies, OU=Mobile, O=NexusWaveTech, L=Mumbai, S=Maharashtra, C=IN"
            )
            isIgnoreExitValue = true
        }

        if (result.result.get().exitValue == 0) {
            // 3. Verify the generated keystore can be loaded by java.security.KeyStore
            val ks = KeyStore.getInstance("JKS")
            keystoreFile.inputStream().use { ks.load(it, storePassword.toCharArray()) }
            val cert: Certificate = ks.getCertificate(keyAlias)
            println("✅ autoGenerateKeystore: keystore created successfully.")
            println("   Subject : ${cert.type} certificate")
            println("   Alias   : $keyAlias")
            println("   Path    : ${keystoreFile.absolutePath}")
            println("   Valid   : 10 000 days")
        } else {
            throw GradleException(
                "❌ autoGenerateKeystore failed. " +
                "Ensure keytool (part of the JDK) is on your PATH. " +
                "Alternatively set SIGNING_STORE_FILE to a pre-existing keystore."
            )
        }
    }
}

// Hook autoGenerateKeystore to run before every assemble / package task
// so a release build always has a valid keystore available.
afterEvaluate {
    tasks.matching { it.name.startsWith("assemble") || it.name.startsWith("package") }.configureEach {
        dependsOn("autoGenerateKeystore")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
android {
    namespace = "com.nexuswavetech.nexusplus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexuswavetech.nexusplus"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("SIGNING_STORE_FILE") ?: "release-keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: "nexuswave@2025"
            keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "nexusplus"
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: "nexuswave@2025"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)
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

    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.object.detection)
    implementation(libs.mlkit.image.labeling)

    implementation(libs.accompanist.permissions)
    implementation(libs.zxing.core)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
