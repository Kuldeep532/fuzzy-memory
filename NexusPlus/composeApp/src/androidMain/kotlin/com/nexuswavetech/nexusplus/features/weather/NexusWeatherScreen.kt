package com.nexuswavetech.nexusplus.features.weather

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.nexuswavetech.nexusplus.ads.NexusBannerAd
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

// ── Data classes ──────────────────────────────────────────────────────────────

data class CurrentWeather(
    val temperature     : Double,
    val feelsLike       : Double,
    val humidity        : Int,
    val windSpeed       : Double,
    val weatherCode     : Int,
    val locationName    : String,
)

data class DailyForecast(
    val dayLabel  : String,
    val weatherCode: Int,
    val tempMax   : Double,
    val tempMin   : Double,
)

data class WeatherUiState(
    val isLoading      : Boolean              = false,
    val current        : CurrentWeather?      = null,
    val forecast       : List<DailyForecast>  = emptyList(),
    val error          : String?              = null,
    val lastUpdated    : String               = "",
)

// ── WMO weather code helpers ──────────────────────────────────────────────────

private fun wmoDescription(code: Int): String = when (code) {
    0            -> "Clear Sky"
    in 1..3      -> "Partly Cloudy"
    in 45..48    -> "Foggy"
    in 51..55    -> "Drizzle"
    in 61..65    -> "Rain"
    in 71..75    -> "Snow"
    in 80..82    -> "Rain Showers"
    in 85..86    -> "Snow Showers"
    95           -> "Thunderstorm"
    in 96..99    -> "Heavy Thunderstorm"
    else         -> "Unknown"
}

private fun wmoEmoji(code: Int): String = when (code) {
    0            -> "☀️"
    in 1..3      -> "⛅"
    in 45..48    -> "🌫️"
    in 51..55    -> "🌦️"
    in 61..65    -> "🌧️"
    in 71..75    -> "❄️"
    in 80..82    -> "🌦️"
    in 85..86    -> "🌨️"
    95           -> "⛈️"
    in 96..99    -> "⛈️"
    else         -> "🌡️"
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class NexusWeatherViewModel : ViewModel() {

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun fetchWeather(context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val (lat, lon) = getLocation(context)
                val locationName = reverseGeocode(context, lat, lon)
                val json = fetchJson(lat, lon)
                val parsed = parseWeather(json, locationName)
                _state.value = _state.value.copy(
                    isLoading   = false,
                    current     = parsed.first,
                    forecast    = parsed.second,
                    lastUpdated = "Updated just now"
                )
            } catch (e: SecurityException) {
                _state.value = _state.value.copy(isLoading = false, error = "Location permission denied")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load weather")
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun getLocation(context: Context): Pair<Double, Double> =
        withContext(Dispatchers.IO) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            var loc: android.location.Location? = null
            for (provider in providers.reversed()) {
                try {
                    val l = lm.getLastKnownLocation(provider)
                    if (l != null && (loc == null || l.accuracy < loc.accuracy)) loc = l
                } catch (_: SecurityException) { }
            }
            if (loc != null) loc.latitude to loc.longitude
            else throw Exception("Could not get location. Make sure GPS is enabled.")
        }

    private suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(lat, lon, 1)
                results?.firstOrNull()?.let { addr ->
                    addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Your Location"
                } ?: "Your Location"
            } catch (_: Exception) { "Your Location" }
        }

    private suspend fun fetchJson(lat: Double, lon: Double): String =
        withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=${String.format("%.4f", lat)}" +
                "&longitude=${String.format("%.4f", lon)}" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                "&timezone=auto&forecast_days=7"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Server error ${response.code}")
                response.body?.string() ?: throw Exception("Empty response")
            }
        }

    private fun parseWeather(json: String, locationName: String): Pair<CurrentWeather, List<DailyForecast>> {
        val root    = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily   = root.getJSONObject("daily")

        val currentWeather = CurrentWeather(
            temperature  = current.getDouble("temperature_2m"),
            feelsLike    = current.getDouble("apparent_temperature"),
            humidity     = current.getInt("relative_humidity_2m"),
            windSpeed    = current.getDouble("wind_speed_10m"),
            weatherCode  = current.getInt("weather_code"),
            locationName = locationName,
        )

        val days = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxes = daily.getJSONArray("temperature_2m_max")
        val mins  = daily.getJSONArray("temperature_2m_min")
        val dayNames = listOf("Today","Mon","Tue","Wed","Thu","Fri","Sat","Sun","Mon","Tue","Wed")
        val forecasts = (0 until days.length()).map { i ->
            DailyForecast(
                dayLabel   = if (i == 0) "Today" else dayNames.getOrElse(i) { "Day ${i+1}" },
                weatherCode= codes.getInt(i),
                tempMax    = maxes.getDouble(i),
                tempMin    = mins.getDouble(i),
            )
        }
        return currentWeather to forecasts
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NexusWeatherScreen(onBack: () -> Unit) {
    val context   = LocalContext.current
    val viewModel = remember { NexusWeatherViewModel() }
    val state     by viewModel.state.collectAsState()

    val locationPerms = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )

    LaunchedEffect(locationPerms.allPermissionsGranted) {
        if (locationPerms.allPermissionsGranted) viewModel.fetchWeather(context)
    }

    Scaffold(
        topBar = {
            NexusTopBar(
                title = "Weather",
                onBack = onBack,
                actions = {
                    if (locationPerms.allPermissionsGranted) {
                        IconButton(
                            onClick = { viewModel.fetchWeather(context) },
                            modifier = Modifier.semantics { contentDescription = "Refresh weather" }
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    !locationPerms.allPermissionsGranted -> LocationPermissionPrompt(
                        onGrant = { locationPerms.launchMultiplePermissionRequest() }
                    )
                    state.isLoading -> LoadingIndicator()
                    state.error != null -> ErrorCard(state.error!!, onRetry = { viewModel.fetchWeather(context) })
                    state.current != null -> WeatherContent(state)
                }
            }

            // Ad — only on network-dependent feature
            NexusBannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LocationPermissionPrompt(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🌤️", fontSize = 72.sp)
        Text("Location access needed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Nexus Weather uses your GPS location to show real-time forecasts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onGrant) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Enable Location")
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Getting weather data…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun WeatherContent(state: WeatherUiState) {
    val current = state.current ?: return

    // Current conditions card
    AnimatedContent(
        targetState = current,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "weather_content"
    ) { weather ->
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(weather.locationName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(wmoEmoji(weather.weatherCode), fontSize = 72.sp)
                Text(
                    "${weather.temperature.toInt()}°C",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(wmoDescription(weather.weatherCode), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Feels like ${weather.feelsLike.toInt()}°C", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    WeatherDetail(Icons.Filled.Water, "${weather.humidity}%", "Humidity")
                    WeatherDetail(Icons.Filled.Air, "${weather.windSpeed.toInt()} km/h", "Wind")
                }
            }
        }
    }

    // 7-day forecast
    if (state.forecast.isNotEmpty()) {
        Text("7-Day Forecast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.forecast) { day ->
                ForecastDayCard(day)
            }
        }
    }

    if (state.lastUpdated.isNotEmpty()) {
        Text(
            state.lastUpdated,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun WeatherDetail(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ForecastDayCard(day: DailyForecast) {
    Surface(
        shape         = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier      = Modifier.width(80.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(day.dayLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(wmoEmoji(day.weatherCode), fontSize = 24.sp)
            Text("${day.tempMax.toInt()}°", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("${day.tempMin.toInt()}°", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
