package com.nexuswavetech.nexusplus.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java

actual fun createHttpClient(): HttpClient = HttpClient(Java) {
    engine {
        config {
            followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            connectTimeout(java.time.Duration.ofSeconds(20))
        }
    }
}
