import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val keyPropertiesFile = rootProject.file("key.properties")
val keyProperties = Properties().apply {
    if (keyPropertiesFile.isFile) {
        keyPropertiesFile.inputStream().use(::load)
    }
}

fun localProperty(name: String): String? =
    keyProperties.getProperty(name)?.trim()?.takeIf(String::isNotEmpty)

fun environmentVariable(name: String): String? =
    providers.environmentVariable(name).orNull?.trim()?.takeIf(String::isNotEmpty)

fun signingValue(propertyName: String, environmentName: String): String? =
    localProperty(propertyName) ?: environmentVariable(environmentName)

val ciKeystoreFile = layout.buildDirectory.file("signing/nexusplus-release.jks")
val keystoreBase64 = environmentVariable("KEYSTORE_BASE64")
val configuredStoreFile = localProperty("storeFile")
    ?.let { rootProject.file(it) }
    ?: environmentVariable("KEYSTORE_FILE")?.let { file(it) }
    ?: keystoreBase64?.let { ciKeystoreFile.get().asFile }

val releaseStorePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "KEY_PASSWORD")

fun missingSigningConfiguration(): List<String> = buildList {
    if (configuredStoreFile == null) add("storeFile in key.properties, KEYSTORE_FILE, or KEYSTORE_BASE64")
    if (releaseStorePassword == null) add("storePassword in key.properties or KEYSTORE_PASSWORD")
    if (releaseKeyAlias == null) add("keyAlias in key.properties or KEY_ALIAS")
    if (releaseKeyPassword == null) add("keyPassword in key.properties or KEY_PASSWORD")
}


val googleServicesJson = environmentVariable("GOOGLE_SERVICES_JSON")

val prepareGoogleServicesJson by tasks.registering {
    group = "build setup"
    description = "Writes app/google-services.json from GOOGLE_SERVICES_JSON when supplied by CI."

    onlyIf { googleServicesJson != null && !file("google-services.json").isFile }

    doLast {
        file("google-services.json").writeText(googleServicesJson ?: return@doLast)
    }
}

val prepareReleaseKeystore by tasks.registering {
    group = "build setup"
    description = "Validates release signing credentials and materializes the CI keystore when KEYSTORE_BASE64 is used."

    inputs.property("hasKeyProperties", keyPropertiesFile.isFile)
    inputs.property("hasKeyStoreBase64", keystoreBase64 != null)
    outputs.file(ciKeystoreFile)
        .optional()

    doLast {
        val missing = missingSigningConfiguration()
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release signing is not configured. Missing: ${missing.joinToString()}. " +
                    "For local builds, create root key.properties with storeFile, storePassword, keyAlias, and keyPassword. " +
                    "For GitHub Actions, provide GOOGLE_SERVICES_JSON, KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD."
            )
        }

        val keystoreFile = configuredStoreFile ?: error("Signing store file was unexpectedly null.")
        if (keystoreBase64 != null && localProperty("storeFile") == null && environmentVariable("KEYSTORE_FILE") == null) {
            keystoreFile.parentFile.mkdirs()
            keystoreFile.writeBytes(Base64.getDecoder().decode(keystoreBase64))
        }

        if (!keystoreFile.isFile) {
            throw GradleException("Release signing keystore does not exist: ${keystoreFile.absolutePath}")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

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
            storeFile = configuredStoreFile
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
    dependsOn(prepareReleaseKeystore)
}

dependencies {
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

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.google.play.auth)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
