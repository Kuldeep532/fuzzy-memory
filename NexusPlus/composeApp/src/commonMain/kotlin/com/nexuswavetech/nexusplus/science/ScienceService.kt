package com.nexuswavetech.nexusplus.science

import com.nexuswavetech.nexusplus.platform.fetchHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ScienceService — Space Explorer + Science Hub.
 *
 * Priority 7: Science features.
 *   - NASA APOD (Astronomy Picture of the Day)
 *   - Planetary data (simplified)
 *   - ISS tracker (API)
 *   - Near-Earth Objects (NASA NEO API)
 *   - Science facts, news, quiz
 */
@Serializable
data class ApodResult(
    val title: String,
    val explanation: String,
    val url: String,
    val hdurl: String = url,
    val date: String,
    val mediaType: String,
)

@Serializable
data class NeoObject(
    val id: String,
    val name: String,
    val diameterMinKm: Double,
    val diameterMaxKm: Double,
    val isPotentiallyHazardous: Boolean,
    val closeApproachDate: String,
    val missDistanceKm: String,
    val velocityKph: String,
)

@Serializable
data class ScienceFact(
    val category: String,
    val title: String,
    val fact: String,
)

@Serializable
data class ScienceQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

class ScienceService {

    private val json = Json { ignoreUnknownKeys = true }

    /** Retrieve NASA API key from BuildConfig (app module) or fall back to DEMO_KEY. */
    private fun getNasaApiKey(): String = runCatching {
        Class.forName("com.nexuswavetech.nexusplus.BuildConfig").getField("NASA_API_KEY").get(null) as? String
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "DEMO_KEY"

    /** NASA APOD — uses NASA_API_KEY from BuildConfig (DEMO_KEY fallback for dev). */
    suspend fun fetchApod(): Result<ApodResult> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = getNasaApiKey()
                val url = "https://api.nasa.gov/planetary/apod?api_key=$apiKey"
                val response = fetchHttp(url)
                val result = json.decodeFromString<ApodResponse>(response)
                Result.success(
                    ApodResult(
                        title = result.title,
                        explanation = result.explanation,
                        url = result.url,
                        hdurl = result.hdurl ?: result.url,
                        date = result.date,
                        mediaType = result.media_type,
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** NASA NEO — Near-Earth Objects for today. */
    suspend fun fetchNeo(): Result<List<NeoObject>> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = getNasaApiKey()
                val url = "https://api.nasa.gov/neo/rest/v1/feed?api_key=$apiKey"
                val response = fetchHttp(url)
                val root = json.decodeFromString<NeoFeedResponse>(response)
                val objects = mutableListOf<NeoObject>()
                root.near_earth_objects.forEach { (_, dayList) ->
                    dayList.forEach { neo ->
                        val est = neo.estimated_diameter.kilometers
                        val approach = neo.close_approach_data.firstOrNull()
                        objects += NeoObject(
                            id = neo.id,
                            name = neo.name,
                            diameterMinKm = est.estimated_diameter_min,
                            diameterMaxKm = est.estimated_diameter_max,
                            isPotentiallyHazardous = neo.is_potentially_hazardous_asteroid,
                            closeApproachDate = approach?.close_approach_date ?: "",
                            missDistanceKm = approach?.miss_distance?.kilometers ?: "",
                            velocityKph = approach?.relative_velocity?.kilometers_per_hour ?: "",
                        )
                    }
                }
                Result.success(objects)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** ISS current position (Open Notify API). */
    suspend fun fetchIssPosition(): Result<IssPosition> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.wheretheiss.at/v1/satellites/25544"
                val response = fetchHttp(url)
                val result = json.decodeFromString<IssNowResponse>(response)
                Result.success(
                    IssPosition(
                        latitude = result.latitude,
                        longitude = result.longitude,
                        timestamp = result.timestamp,
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Built-in science facts (no network needed). */
    fun scienceFacts(): List<ScienceFact> = listOf(
        ScienceFact("Space", "Speed of Light", "Light travels at 299,792 km/s in a vacuum."),
        ScienceFact("Space", "Milky Way", "Our galaxy contains 100–400 billion stars."),
        ScienceFact("Space", "Black Holes", "Time slows down near a black hole due to gravity."),
        ScienceFact("Physics", "E=mc\u00b2", "Mass and energy are equivalent; c is the speed of light."),
        ScienceFact("Biology", "Human DNA", "Your DNA is 99.9% identical to every other human."),
        ScienceFact("Chemistry", "Water", "Water is the only substance that exists naturally in all three states."),
        ScienceFact("Earth", "Core Temperature", "Earth's inner core is as hot as the Sun's surface (~5,500\u00b0C)."),
        ScienceFact("Space", "Olympus Mons", "The largest volcano in the solar system is on Mars, 21 km high."),
    )

    /** Built-in quiz questions (no network needed). */
    fun quizQuestions(): List<ScienceQuizQuestion> = listOf(
        ScienceQuizQuestion(
            "What planet is known as the Red Planet?",
            listOf("Venus", "Mars", "Jupiter", "Saturn"),
            1,
            "Mars appears red due to iron oxide (rust) on its surface."
        ),
        ScienceQuizQuestion(
            "What is the chemical symbol for gold?",
            listOf("Go", "Gd", "Au", "Ag"),
            2,
            "Au comes from the Latin 'aurum', meaning shining dawn."
        ),
        ScienceQuizQuestion(
            "How many bones does an adult human have?",
            listOf("206", "250", "180", "300"),
            0,
            "An adult has 206 bones; babies have about 270 that fuse together."
        ),
        ScienceQuizQuestion(
            "What is the largest planet in our solar system?",
            listOf("Earth", "Saturn", "Jupiter", "Neptune"),
            2,
            "Jupiter is more than twice as massive as all other planets combined."
        ),
    )
}

@Serializable
data class IssPosition(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
)

// ── NASA API DTOs ───────────────────────────────────────────────────────────

@Serializable
data class ApodResponse(
    val title: String,
    val explanation: String,
    val url: String,
    val hdurl: String? = null,
    val date: String,
    val media_type: String,
)

@Serializable
data class NeoFeedResponse(
    val near_earth_objects: Map<String, List<NeoObjectRaw>>,
)

@Serializable
data class NeoObjectRaw(
    val id: String,
    val name: String,
    val is_potentially_hazardous_asteroid: Boolean,
    val estimated_diameter: NeoDiameter,
    val close_approach_data: List<NeoApproach>,
)

@Serializable
data class NeoDiameter(
    val kilometers: NeoKm,
)

@Serializable
data class NeoKm(
    val estimated_diameter_min: Double,
    val estimated_diameter_max: Double,
)

@Serializable
data class NeoApproach(
    val close_approach_date: String,
    val miss_distance: NeoMissDistance,
    val relative_velocity: NeoVelocity,
)

@Serializable
data class NeoMissDistance(
    val kilometers: String,
)

@Serializable
data class NeoVelocity(
    val kilometers_per_hour: String,
)

// ── ISS API DTOs (wheretheiss.at v1) ─────────────────────────────────────────

@Serializable
data class IssNowResponse(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
)
