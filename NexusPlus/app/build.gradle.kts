import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ─────────────────────────────────────────────────────────────────────────────
// Signing credential resolution — three-tier priority:
//   1. key.properties  (local dev — gitignored)
//   2. Environment variables  (CI/CD — injected by GitHub Actions)
//   3. Safe empty default  (causes loud Gradle failure if creds truly absent)
//
// NEVER store real passwords in this file.
// In CI/CD, the GitHub Actions workflow decodes KEYSTORE_BASE64 and sets
// the SIGNING_* environment variables before Gradle is invoked.
// ─────────────────────────────────────────────────────────────────────────────
val keyPropertiesFile = rootProject.file("key.properties")
val keyProps = Properties().apply {
    if (keyPropertiesFile.exists()) keyPropertiesFile.inputStream().use { load(it) }
}

fun signingProp(propKey: String, envKey: String, default: String = ""): String =
    keyProps.getProperty(propKey)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }
        ?: default

val keystoreFilePath = signingProp("storeFile",    "SIGNING_STORE_FILE",    "app/nexusplus-upload-key.jks")
val keystorePassword = signingProp("storePassword", "SIGNING_STORE_PASSWORD")
val keystoreAlias    = signingProp("keyAlias",      "SIGNING_KEY_ALIAS",     "NexusPlus")
val keystoreKeyPass  = signingProp("keyPassword",   "SIGNING_KEY_PASSWORD")

// ─────────────────────────────────────────────────────────────────────────────
// autoGenerateKeystore — LOCAL DEV ONLY fallback.
// In CI/CD the keystore is decoded from KEYSTORE_BASE64 by the workflow;
// this task is intentionally skipped when env vars are already set.
// ─────────────────────────────────────────────────────────────────────────────
tasks.register("autoGenerateKeystore") {
    description = "Generates a local dev keystore if none is present. Skipped in CI when SIGNING_STORE_PASSWORD is set."
    group = "build setup"

    doFirst {
        val ciMode = System.getenv("SIGNING_STORE_PASSWORD")?.isNotBlank() == true
        if (ciMode) {
            println("ℹ️  autoGenerateKeystore: CI mode detected — skipping (keystore injected by workflow).")
            return@doFirst
        }

        val ksFile = rootProject.file(keystoreFilePath)
        if (ksFile.exists()) {
            println("✅ autoGenerateKeystore: keystore already present → ${ksFile.absolutePath}")
            try {
                val ks = KeyStore.getInstance("PKCS12")
                ksFile.inputStream().use { ks.load(it, keystorePassword.toCharArray()) }
                val cert: Certificate? = ks.getCertificate(keystoreAlias)
                println("   Format : PKCS12  |  Alias : $keystoreAlias  |  Cert : ${cert?.type ?: "n/a"}")
            } catch (_: Exception) {
                println("   (Skipping verification — may be JKS format, which is still valid)")
            }
            return@doFirst
        }

        println("🔑 autoGenerateKeystore: no keystore found — generating local dev RSA-4096 …")

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(4096, SecureRandom())
        kpg.generateKeyPair()
        println("   RSA-4096 key pair generated in-memory")

        val keytoolCmd = listOf(
            "keytool", "-genkeypair",
            "-keystore",  ksFile.absolutePath,
            "-storetype", "PKCS12",
            "-alias",     keystoreAlias,
            "-keyalg",    "RSA",
            "-keysize",   "4096",
            "-validity",  "9125",
            "-storepass", keystorePassword.ifBlank { "localDevOnly@2026" },
            "-keypass",   keystoreKeyPass.ifBlank  { "localDevOnly@2026" },
            "-dname",     "CN=Kuldeep Kumar Yadav, OU=Development, O=Nexus Wave Technologies, L=Korba, ST=Chhattisgarh, C=IN"
        )

        val result = providers.exec {
            commandLine(keytoolCmd)
            isIgnoreExitValue = true
        }

        if (result.result.get().exitValue == 0) {
            val ks = KeyStore.getInstance("PKCS12")
            ksFile.inputStream().use { ks.load(it, keystorePassword.ifBlank { "localDevOnly@2026" }.toCharArray()) }
            val cert: Certificate = ks.getCertificate(keystoreAlias)
            println("✅ Keystore created: ${ksFile.absolutePath}  (${ksFile.length()} bytes)")
            println("   Alias : $keystoreAlias  |  Cert : ${cert.type}")
        } else {
            throw GradleException(
                "❌ autoGenerateKeystore failed. keytool must be on PATH (ships with every JDK).\n" +
                "   Place your own keystore at: ${ksFile.absolutePath}\n" +
                "   and set credentials in key.properties or environment variables."
            )
        }
    }
}

afterEvaluate {
    tasks.matching { it.name.startsWith("assemble") || it.name.startsWith("package") }.configureEach {
        dependsOn("autoGenerateKeystore")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
android {
    namespace  = "com.nexuswavetech.nexusplus"
    compileSdk = 35

    defaultConfig {
        applicationId         = "com.nexuswavetech.nexusplus"
        minSdk                = 26
        targetSdk             = 35
        versionCode           = 3
        versionName           = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile     = rootProject.file(keystoreFilePath)
            storePassword = keystorePassword
            keyAlias      = keystoreAlias
            keyPassword   = keystoreKeyPass
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
        compose     = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Shared KMP module ─────────────────────────────────────────────────
    implementation(project(":shared"))

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
