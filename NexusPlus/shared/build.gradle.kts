plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            // Pure Kotlin stdlib — no platform-specific dependencies in shared domain
            implementation(kotlin("stdlib"))
        }
        androidMain.dependencies {
            // Android-specific shared utilities go here
        }
        val desktopMain by getting {
            dependencies {
                // Desktop/JVM-specific shared utilities go here
            }
        }
    }
}

android {
    namespace  = "com.nexuswavetech.nexusplus.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
