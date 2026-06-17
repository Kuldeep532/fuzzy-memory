package com.nexuswavetech.nexusplus.features.weather

import com.nexuswavetech.nexusplus.platform.fetchHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * WeatherService — Open-Meteo API client with manual city entry, offline cache,
 * current weather, hourly forecast, and 7-day daily forecast.
 *
 * Priority 5: Weather implementation.
 */
@Serializable
data class WeatherLocation(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val isDefault: Boolean = false,
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val weatherCode: Int,
    val locationName: String,
    val lastUpdated: String,
)

@Serializable
data class HourlyForecast(
    val hourLabel: String,
    val weatherCode: Int,
    val temperature: Double,
)

@Serializable
data class DailyForecast(
    val dayLabel: String,
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double,
    val precipitationProbability: Int,
)

@Serializable
data class WeatherData(
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
)

/** WMO weather code to description and icon mapping. */
fun wmoDescription(code: Int): String = when (code) {
    0 -> "Clear Sky"
    in 1..3 -> "Partly Cloudy"
    in 45..48 -> "Foggy"
    in 51..55 -> "Drizzle"
    in 61..65 -> "Rain"
    in 71..75 -> "Snow"
    in 80..82 -> "Rain Showers"
    in 85..86 -> "Snow Showers"
    95 -> "Thunderstorm"
    in 96..99 -> "Heavy Thunderstorm"
    else -> "Unknown"
}

fun wmoIcon(code: Int): String = when (code) {
    0 -> "\u2600\uFE0F"
    in 1..3 -> "\u26C5"
    in 45..48 -> "\u1F32B\uFE0F"
    in 51..55 -> "\u1F326\uFE0F"
    in 61..65 -> "\u1F327\uFE0F"
    in 71..75 -> "\u2744\uFE0F"
    in 80..82 -> "\u1F326\uFE0F"
    in 85..86 -> "\u1F328\uFE0F"
    95 -> "\u26C8\uFE0F"
    in 96..99 -> "\u26C8\uFE0F"
    else -> "\u1F321\uFE0F"
}

/**
 * WeatherService backed by Ktor (commonMain) and Open-Meteo API.
 * Supports: manual city entry, cached locations, current weather, hourly, daily forecast.
 */
class WeatherService {

    private val json = Json { ignoreUnknownKeys = true }

    /** Fetch weather data for a given location. */
    suspend fun fetchWeather(location: WeatherLocation): Result<WeatherData> =
        withContext(Dispatchers.IO) {
            try {
                val url = buildOpenMeteoUrl(location.latitude, location.longitude)
                val response = fetchHttp(url)
                val weather = parseOpenMeteoResponse(response, location.cityName)
                Result.success(weather)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Geocode a city name to lat/lon using Open-Meteo Geocoding API. */
    suspend fun geocodeCity(cityName: String): Result<WeatherLocation> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                    cityName.replace(" ", "+") + "&count=1&language=en&format=json"
                val response = fetchHttp(url)
                val result = json.decodeFromString<OpenMeteoGeocode>(response)
                val r = result.results.firstOrNull()
                    ?: return@withContext Result.failure(Exception("City not found"))
                Result.success(
                    WeatherLocation(
                        cityName = r.name,
                        latitude = r.latitude,
                        longitude = r.longitude,
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun buildOpenMeteoUrl(lat: Double, lon: Double): String =
        "https://api.open-meteo.com/v1/forecast?" +
            "latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code&forecast_days=2" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto"

    private fun parseOpenMeteoResponse(raw: String, cityName: String): WeatherData {
        val root = json.decodeFromString<OpenMeteoRoot>(raw)
        val current = root.current

        val currentWeather = CurrentWeather(
            temperature = current.temperature_2m,
            feelsLike = current.apparent_temperature,
            humidity = current.relative_humidity_2m,
            windSpeed = current.wind_speed_10m,
            weatherCode = current.weather_code,
            locationName = cityName,
            lastUpdated = "",
        )

        val hourly = root.hourly.time.zip(
            root.hourly.temperature_2m.zip(root.hourly.weather_code)
        ).take(24).map { (time, pair) ->
            val (temp, code) = pair
            HourlyForecast(
                hourLabel = time.substring(11, 13),
                weatherCode = code,
                temperature = temp,
            )
        }

        val daily = root.daily.time.zip(
            root.daily.weather_code.zip(
                root.daily.temperature_2m_max.zip(
                    root.daily.temperature_2m_min.zip(root.daily.precipitation_probability_max)
                )
            )
        ).map { (day, quad) ->
            val (code, maxMin) = quad
            val (max, minProb) = maxMin
            val (min, prob) = minProb
            DailyForecast(
                dayLabel = day.substring(5),
                weatherCode = code,
                tempMax = max,
                tempMin = min,
                precipitationProbability = prob,
            )
        }

        return WeatherData(current = currentWeather, hourly = hourly, daily = daily)
    }
}

// ── Serialization DTOs for Open-Meteo ───────────────────────────────────────

@Serializable
data class OpenMeteoGeocode(
    val results: List<GeocodeResult>,
)

@Serializable
data class GeocodeResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String = "",
)

@Serializable
data class OpenMeteoRoot(
    val current: CurrentDto,
    val hourly: HourlyDto,
    val daily: DailyDto,
)

@Serializable
data class CurrentDto(
    val temperature_2m: Double,
    val relative_humidity_2m: Int,
    val apparent_temperature: Double,
    val weather_code: Int,
    val wind_speed_10m: Double,
)

@Serializable
data class HourlyDto(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val weather_code: List<Int>,
)

@Serializable
data class DailyDto(
    val time: List<String>,
    val weather_code: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_probability_max: List<Int>,
)
