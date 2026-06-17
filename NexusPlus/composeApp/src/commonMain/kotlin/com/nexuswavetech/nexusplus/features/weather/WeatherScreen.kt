package com.nexuswavetech.nexusplus.features.weather

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.platform.getPlatformName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// ── ViewModel ─────────────────────────────────────────────────────────────────

class WeatherViewModel(
    private val service: WeatherService = WeatherService(),
    private val repository: WeatherRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherScreenState())
    val state: StateFlow<WeatherScreenState> = _state.asStateFlow()

    private val _locations = MutableStateFlow<List<WeatherLocation>>(emptyList())
    val locations: StateFlow<List<WeatherLocation>> = _locations.asStateFlow()

    fun loadWeather(location: WeatherLocation) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = service.fetchWeather(location)
            _state.value = when {
                result.isSuccess -> {
                    val data = result.getOrNull()!!
                    WeatherScreenState(
                        isLoading = false,
                        data = data,
                        location = location,
                    )
                }
                else -> WeatherScreenState(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun searchCity(city: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = service.geocodeCity(city)
            if (result.isSuccess) {
                val loc = result.getOrNull()!!
                repository?.saveLocation(loc)
                loadWeather(loc)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "City not found",
                )
            }
        }
    }

    fun refresh() {
        val loc = _state.value.location ?: return
        loadWeather(loc)
    }
}

// ── UI State ──────────────────────────────────────────────────────────────────

data class WeatherScreenState(
    val isLoading: Boolean = false,
    val data: WeatherData? = null,
    val location: WeatherLocation? = null,
    val error: String? = null,
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun WeatherScreen(
    onBack: () -> Unit,
    viewModel: WeatherViewModel = koinInject(),
) {
    val state by viewModel.state.collectAsState()
    var cityInput by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            com.nexuswavetech.nexusplus.ui.components.NexusTopBar(
                title = "Nexus Weather",
                onBack = onBack,
                actions = {
                    if (state.data != null) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search city")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Search bar
            if (showSearch || state.data == null) {
                CitySearchBar(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    onSearch = { viewModel.searchCity(cityInput) },
                    isLoading = state.isLoading,
                )
            }

            // Content
            when {
                state.isLoading && state.data == null -> LoadingIndicator()
                state.error != null -> ErrorCard(state.error!!, onRetry = { viewModel.refresh() })
                state.data != null -> WeatherContent(state.data!!, state.location)
            }
        }
    }
}

@Composable
private fun CitySearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Enter city name") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        trailingIcon = {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }
        },
    )
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text("Fetching weather data…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun WeatherContent(data: WeatherData, location: WeatherLocation?) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Current weather card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(
                        location?.cityName ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(wmoIcon(data.current.weatherCode), fontSize = 72.sp)
                Text(
                    "${data.current.temperature.toInt()}\u00b0C",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(wmoDescription(data.current.weatherCode), style = MaterialTheme.typography.titleMedium)
                Text("Feels like ${data.current.feelsLike.toInt()}\u00b0C", style = MaterialTheme.typography.bodyMedium)

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    WeatherDetail(Icons.Filled.Water, "${data.current.humidity}%", "Humidity")
                    WeatherDetail(Icons.Filled.Air, "${data.current.windSpeed.toInt()} km/h", "Wind")
                }
            }
        }

        // Hourly forecast
        if (data.hourly.isNotEmpty()) {
            Text("Hourly", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(data.hourly) { hour ->
                    HourlyCard(hour)
                }
            }
        }

        // Daily forecast
        if (data.daily.isNotEmpty()) {
            Text("7-Day Forecast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(data.daily) { day ->
                    DailyCard(day)
                }
            }
        }

        Text(
            "Powered by Open-Meteo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun WeatherDetail(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HourlyCard(hour: HourlyForecast) {
    Surface(
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp,
        modifier = Modifier.width(64.dp),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(hour.hourLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(wmoIcon(hour.weatherCode), fontSize = 20.sp)
            Text("${hour.temperature.toInt()}\u00b0", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DailyCard(day: DailyForecast) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.width(80.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(day.dayLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(wmoIcon(day.weatherCode), fontSize = 24.sp)
            Text("${day.tempMax.toInt()}\u00b0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("${day.tempMin.toInt()}\u00b0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
