package com.nexuswavetech.nexusplus.features.weather

import com.nexuswavetech.nexusplus.platform.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WeatherRepository — persists saved locations and cached weather data.
 *
 * Priority 5: Weather offline cache + manual city persistence.
 */
class WeatherRepository(private val store: SettingsStore) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val SAVED_LOCATIONS_KEY = "weather_saved_locations"
        private const val LAST_WEATHER_KEY = "weather_last_data"
        private const val DEFAULT_LOCATION_KEY = "weather_default_location"
    }

    /** Saved locations (manual city entries). */
    val savedLocations: Flow<List<WeatherLocation>> =
        store.stringFlow(SAVED_LOCATIONS_KEY, "")
            .map { raw ->
                if (raw.isBlank()) return@map emptyList()
                runCatching { json.decodeFromString<List<WeatherLocation>>(raw) }.getOrDefault(emptyList())
            }

    /** Add or update a saved location. */
    suspend fun saveLocation(location: WeatherLocation) {
        val current = savedLocations.map { it.toMutableList() }.map { list ->
            val existing = list.indexOfFirst { it.cityName == location.cityName }
            if (existing >= 0) list[existing] = location else list.add(location)
            list
        }
        // We need to read the flow value, but Flow doesn't support easy get
        // Use atomic read via SettingsStore
        val raw = store.getString(SAVED_LOCATIONS_KEY, "")
        val list = if (raw.isBlank()) mutableListOf() else
            runCatching { json.decodeFromString<MutableList<WeatherLocation>>(raw) }.getOrDefault(mutableListOf())
        val existing = list.indexOfFirst { it.cityName == location.cityName }
        if (existing >= 0) list[existing] = location else list.add(location)
        store.setString(SAVED_LOCATIONS_KEY, json.encodeToString(list))
    }

    /** Remove a saved location. */
    suspend fun removeLocation(cityName: String) {
        val raw = store.getString(SAVED_LOCATIONS_KEY, "")
        val list = if (raw.isBlank()) return else
            runCatching { json.decodeFromString<MutableList<WeatherLocation>>(raw) }.getOrDefault(mutableListOf())
        list.removeAll { it.cityName == cityName }
        store.setString(SAVED_LOCATIONS_KEY, json.encodeToString(list))
    }

    /** Set the default location. */
    suspend fun setDefaultLocation(cityName: String) {
        val raw = store.getString(SAVED_LOCATIONS_KEY, "")
        val list = if (raw.isBlank()) return else
            runCatching { json.decodeFromString<MutableList<WeatherLocation>>(raw) }.getOrDefault(mutableListOf())
        list.forEachIndexed { i, loc ->
            list[i] = loc.copy(isDefault = loc.cityName == cityName)
        }
        store.setString(SAVED_LOCATIONS_KEY, json.encodeToString(list))
    }

    /** Get the default location, or null if none set. */
    suspend fun getDefaultLocation(): WeatherLocation? {
        val raw = store.getString(SAVED_LOCATIONS_KEY, "")
        val list = if (raw.isBlank()) return null else
            runCatching { json.decodeFromString<List<WeatherLocation>>(raw) }.getOrDefault(emptyList())
        return list.find { it.isDefault } ?: list.firstOrNull()
    }

    /** Cache weather data. */
    suspend fun cacheWeather(data: WeatherData) {
        store.setString(LAST_WEATHER_KEY, json.encodeToString(data))
        store.setString("${LAST_WEATHER_KEY}_timestamp", System.currentTimeMillis().toString())
    }

    /** Get cached weather data. */
    suspend fun getCachedWeather(): WeatherData? {
        val raw = store.getString(LAST_WEATHER_KEY, "")
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<WeatherData>(raw) }.getOrNull()
    }

    /** Check if cache is fresh (within 30 minutes). */
    suspend fun isCacheFresh(): Boolean {
        val ts = store.getString("${LAST_WEATHER_KEY}_timestamp", "0").toLongOrNull() ?: 0L
        return (System.currentTimeMillis() - ts) < 30 * 60 * 1000
    }
}
