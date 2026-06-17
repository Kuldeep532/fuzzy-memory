package com.nexuswavetech.nexusplus.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            followRedirects(true)
            connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        }
    }
}
