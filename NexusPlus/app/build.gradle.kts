import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

fun environmentVariable(name: String): String? =
    providers.environmentVariable(name).orNull?.trim()?.takeIf(String::isNotEmpty)

val ciKeystoreFile = layout.buildDirectory.file("signing/release-keystore")
val keystoreBase64 = environmentVariable("KEYSTORE_BASE64")
val releaseStorePassword = environmentVariable("KEYSTORE_PASSWORD")
val releaseKeyAlias = environmentVariable("KEY_ALIAS")
val releaseKeyPassword = environmentVariable("KEY_PASSWORD")
val configuredStoreFile = keystoreBase64?.let { ciKeystoreFile.get().asFile }

fun missingSigningConfiguration(): List<String> = buildList {
    if (keystoreBase64 == null) add("KEYSTORE_BASE64")
    if (releaseStorePassword == null) add("KEYSTORE_PASSWORD")
    if (releaseKeyAlias == null) add("KEY_ALIAS")
    if (releaseKeyPassword == null) add("KEY_PASSWORD")
}

val hasReleaseSigningConfiguration = missingSigningConfiguration().isEmpty()

val googleServicesJson = environmentVariable("GOOGLE_SERVICES_JSON")

val prepareGoogleServicesJson by tasks.registering {
    group = "build setup"
    description = "Writes app/google-services.json exclusively from the GOOGLE_SERVICES_JSON environment variable."

    doLast {
        val json = googleServicesJson ?: throw GradleException(
            "GOOGLE_SERVICES_JSON must be provided by GitHub Secrets/environment variables for production builds."
        )
        file("google-services.json").writeText(json)
        logger.lifecycle("✓ google-services.json written from GOOGLE_SERVICES_JSON secret.")
    }
}

val prepareReleaseKeystore by tasks.registering {
    group = "build setup"
    description = "Validates release signing credentials and materializes the CI keystore when KEYSTORE_BASE64 is used."

    inputs.property("hasKeyStoreBase64", keystoreBase64 != null)
    outputs.file(ciKeystoreFile)
        .optional()

    doLast {
        val missing = missingSigningConfiguration()
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release signing is not configured. Missing GitHub Secrets/environment variables: ${missing.joinToString()}. " +
                    "Provide GOOGLE_SERVICES_JSON, KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD in CI."
            )
        }

        val keystoreFile = configuredStoreFile ?: error("Signing store file was unexpectedly null.")
        keystoreFile.parentFile.mkdirs()
        keystoreFile.writeBytes(Base64.getDecoder().decode(keystoreBase64!!))

        if (!keystoreFile.isFile) {
            throw GradleException("Release signing keystore does not exist: ${keystoreFile.absolutePath}")
        }
    }
}

kotlin {
    jvmToolchain(17)
}


androidComponents {
    beforeVariants(selector().withBuildType("debug")) { variantBuilder ->
        variantBuilder.enable = false
    }
}

android {
    namespace = "com.nexuswavetech.nexusplus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexuswavetech.nexusplus"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.4.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AdMob App ID must be supplied by GitHub Secrets / environment variables for production builds.
        val admobAppId = environmentVariable("ADMOB_APP_ID") ?: ""
        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId

        // Gemini API key — set GEMINI_API_KEY in GitHub Secrets / local env.
        // When set, the app uses it as the default key for Aira AI (Gemini) on first launch.
        val geminiApiKey = environmentVariable("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    signingConfigs {
        if (hasReleaseSigningConfiguration) {
            create("release") {
                storeFile = configuredStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs(
                "src/main/java",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

tasks.matching {
    it.name.startsWith("process") && it.name.endsWith("GoogleServices")
}.configureEach {
    dependsOn(prepareGoogleServicesJson)
}

tasks.matching {
    it.name == "validateSigningRelease" ||
        (it.name.startsWith("package") && it.name.endsWith("Release")) ||
        (it.name.startsWith("assemble") && it.name.endsWith("Release")) ||
        (it.name.startsWith("bundle") && it.name.endsWith("Release"))
}.configureEach {
    if (hasReleaseSigningConfiguration) {
        dependsOn(prepareReleaseKeystore)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":composeApp"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
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
    implementation(libs.guava)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.mlkit.translate)
    // REMOVED: ML Kit Object Detection - Causing persistent build failures
    // If needed later, can be re-added as: implementation(libs.mlkit.object.detection)
    implementation(libs.mlkit.image.labeling)
    implementation(libs.mlkit.text.recognition)

    implementation(libs.accompanist.permissions)
    implementation(libs.zxing.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.google.play.auth)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
