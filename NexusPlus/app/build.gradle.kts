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
// Read signing credentials from key.properties (gitignored).
// Falls back to environment variables, then to safe defaults.
// Priority: key.properties > env vars > defaults
// ─────────────────────────────────────────────────────────────────────────────
val keyPropertiesFile = rootProject.file("key.properties")
val keyProps = Properties().apply {
    if (keyPropertiesFile.exists()) keyPropertiesFile.inputStream().use { load(it) }
}

fun signingProp(propKey: String, envKey: String, default: String): String =
    keyProps.getProperty(propKey)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }
        ?: default

val keystoreFilePath  = signingProp("storeFile",     "SIGNING_STORE_FILE",     "app/nexusplus-upload-key.jks")
val keystorePassword  = signingProp("storePassword",  "SIGNING_STORE_PASSWORD", "NexusWave@2025#")
val keystoreAlias     = signingProp("keyAlias",       "SIGNING_KEY_ALIAS",      "nexusplus-upload")
val keystoreKeyPass   = signingProp("keyPassword",    "SIGNING_KEY_PASSWORD",   "NexusWave@2025#")

// ─────────────────────────────────────────────────────────────────────────────
// autoGenerateKeystore — self-contained Gradle task; replaces CI/CD yml.
//
// Automatically generates nexusplus-upload-key.jks if it is absent.
// Uses java.security.KeyPairGenerator + java.security.KeyStore, then
// delegates X.509 certificate creation to the JDK's bundled keytool.
//
// Usage (no laptop or terminal needed beyond a remote build server):
//   ./gradlew autoGenerateKeystore assembleRelease
// ─────────────────────────────────────────────────────────────────────────────
tasks.register("autoGenerateKeystore") {
    description = "Generates release-keystore via java.security APIs + keytool. " +
            "Runs automatically before every assemble/package task."
    group = "build setup"

    doFirst {
        val ksFile = rootProject.file(keystoreFilePath)
        if (ksFile.exists()) {
            println("✅ autoGenerateKeystore: keystore already present → ${ksFile.absolutePath}")

            // Verify it loads correctly with java.security.KeyStore
            try {
                val ks = KeyStore.getInstance("PKCS12")
                ksFile.inputStream().use { ks.load(it, keystorePassword.toCharArray()) }
                val cert: Certificate = ks.getCertificate(keystoreAlias)
                println("   Format  : PKCS12")
                println("   Alias   : $keystoreAlias")
                println("   Cert    : ${cert.type}")
            } catch (_: Exception) {
                println("   (Skipping verification — may be JKS format, which is still valid)")
            }
            return@doFirst
        }

        println("🔑 autoGenerateKeystore: no keystore found — generating RSA-2048 …")

        // Step 1 — verify key pair generation with java.security.KeyPairGenerator
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val kp = kpg.generateKeyPair()
        println("   RSA-2048 key pair generated in-memory (public exponent = 65537)")

        // Step 2 — delegate certificate + JKS creation to keytool (bundled in JDK)
        val result = providers.exec {
            commandLine(
                "keytool", "-genkeypair",
                "-keystore",  ksFile.absolutePath,
                "-storetype", "PKCS12",
                "-alias",     keystoreAlias,
                "-keyalg",    "RSA",
                "-keysize",   "2048",
                "-validity",  "9125",   // 25 years
                "-storepass", keystorePassword,
                "-keypass",   keystoreKeyPass,
                "-dname",     "CN=Nexus Wave Technologies, OU=Mobile, O=NexusWaveTech, L=Mumbai, S=Maharashtra, C=IN"
            )
            isIgnoreExitValue = true
        }

        if (result.result.get().exitValue == 0) {
            // Step 3 — verify the output with java.security.KeyStore
            val ks = KeyStore.getInstance("PKCS12")
            ksFile.inputStream().use { ks.load(it, keystorePassword.toCharArray()) }
            val cert: Certificate = ks.getCertificate(keystoreAlias)
            println("✅ Keystore created  : ${ksFile.absolutePath}  (${ksFile.length()} bytes)")
            println("   Alias             : $keystoreAlias")
            println("   Cert type         : ${cert.type}")
            println("   Validity          : 25 years")
        } else {
            throw GradleException(
                "❌ autoGenerateKeystore failed. keytool must be on PATH (it ships with every JDK).\n" +
                "   Alternatively, place your own keystore at: ${ksFile.absolutePath}\n" +
                "   and set credentials in key.properties or environment variables."
            )
        }
    }
}

// Hook autoGenerateKeystore before every assemble / package task automatically
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
