package com.nexuswavetech.nexusplus.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    engine {
        configureRequest {
            setTimeoutInterval(30.0)
        }
    }
}
