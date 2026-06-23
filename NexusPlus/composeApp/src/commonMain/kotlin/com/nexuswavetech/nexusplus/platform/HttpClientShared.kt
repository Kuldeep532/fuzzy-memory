package com.nexuswavetech.nexusplus.platform

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

/**
 * Shared Ktor HttpClient provider for commonMain.
 * Platform-specific source sets provide the actual engine.
 */
expect fun createHttpClient(): HttpClient

/**
 * Simple fetch helper using the shared Ktor client.
 */
suspend fun fetchHttp(url: String): String {
    val client = createHttpClient()
    return client.get(url).bodyAsText()
}
