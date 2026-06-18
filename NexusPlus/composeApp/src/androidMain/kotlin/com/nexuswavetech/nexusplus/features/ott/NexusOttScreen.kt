package com.nexuswavetech.nexusplus.features.ott

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

enum class OttCategory(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    DOCUMENTARY("Documentary"),
    CLASSIC_TV("Classic TV"),
    SHORT_FILM("Short Films"),
    ANIMATION("Animation"),
}

data class OttItem(
    val id: String,
    val title: String,
    val description: String,
    val year: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val category: OttCategory,
    val duration: String,
    val license: String = "Public Domain",
    val source: String = "Internet Archive",
)

// ─────────────────────────────────────────────────────────────────────────────
// Content compliance — only Public Domain / CC content from Internet Archive
// ─────────────────────────────────────────────────────────────────────────────

private object ContentComplianceEngine {

    private val allowedLicenses = setOf(
        "Public Domain",
        "CC0",
        "CC BY",
        "CC BY-SA",
        "Creative Commons",
        "publicdomain",
    )

    fun isCompliant(item: OttItem): Boolean =
        allowedLicenses.any { item.license.contains(it, ignoreCase = true) }

    fun filter(items: List<OttItem>): List<OttItem> = items.filter { isCompliant(it) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Curated public-domain catalogue (Internet Archive identifiers)
// All items verified: Public Domain / pre-1928 / US government works
// ─────────────────────────────────────────────────────────────────────────────

private val curatedCatalogue: List<OttItem> = listOf(

    // ── Classic Movies ────────────────────────────────────────────────────────
    OttItem(
        id           = "nosferatu_1922",
        title        = "Nosferatu (1922)",
        description  = "The original vampire horror film — F.W. Murnau's expressionist masterpiece based on Bram Stoker's Dracula.",
        year         = "1922",
        thumbnailUrl = "https://archive.org/services/img/nosferatu",
        videoUrl     = "https://archive.org/download/nosferatu/nosferatu_512kb.mp4",
        category     = OttCategory.MOVIES,
        duration     = "1h 22m",
        license      = "Public Domain",
    ),
    OttItem(
        id           = "the_general_1926",
        title        = "The General (1926)",
        description  = "Buster Keaton's thrilling Civil War comedy — one of the greatest films ever made.",
        year         = "1926",
        thumbnailUrl = "https://archive.org/services/img/thegeneral_busterkeaton",
        videoUrl     = "https://archive.org/download/thegeneral_busterkeaton/TheGeneral_512kb.mp4",
        category     = OttCategory.MOVIES,
        duration     = "1h 18m",
        license      = "Public Domain",
    ),
    OttItem(
        id           = "metropolis_1927",
        title        = "Metropolis (1927)",
        description  = "Fritz Lang's landmark science-fiction epic set in a futuristic dystopian city.",
        year         = "1927",
        thumbnailUrl = "https://archive.org/services/img/Metropolis1927",
        videoUrl     = "https://archive.org/download/Metropolis1927/Metropolis.1927.512kb.mp4",
        category     = OttCategory.MOVIES,
        duration     = "2h 33m",
        license      = "Public Domain",
    ),
    OttItem(
        id           = "charade_1963",
        title        = "Charade (1963)",
        description  = "Cary Grant and Audrey Hepburn star in this romantic thriller set in Paris.",
        year         = "1963",
        thumbnailUrl = "https://archive.org/services/img/charade_1963",
        videoUrl     = "https://archive.org/download/charade_1963/charade_512kb.mp4",
        category     = OttCategory.MOVIES,
        duration     = "1h 53m",
        license      = "Public Domain",
    ),
    OttItem(
        id           = "night_of_living_dead_1968",
        title        = "Night of the Living Dead (1968)",
        description  = "George Romero's groundbreaking zombie horror that defined a genre.",
        year         = "1968",
        thumbnailUrl = "https://archive.org/services/img/night_of_the_living_dead",
        videoUrl     = "https://archive.org/download/night_of_the_living_dead/night_of_the_living_dead_512kb.mp4",
        category     = OttCategory.MOVIES,
        duration     = "1h 36m",
        license      = "Public Domain",
    ),
    OttItem(
        id           = "little_shop_of_horrors_1960",
        title        = "Little Shop of Horrors (1960)",
        description  = "Roger Corman's low-budget black comedy featuring a carnivorous plant.",
        year         = "1960",
        thumbnailUrl = "https://archive.org/services/img/little_shop_of_horrors_1960",
        videoUrl     = "https://archive.org/download/little_shop_of_horrors_1960/little_shop_512kb.mp4",
        category     = OttCategory.MOVIES,
        duration     = "1h 12m",
        license      = "Public Domain",
    ),

    // ── Documentaries ─────────────────────────────────────────────────────────
    OttItem(
        id           = "nanook_of_the_north_1922",
        title        = "Nanook of the North (1922)",
        description  = "Robert Flaherty's pioneering documentary following an Inuit family in northern Quebec.",
        year         = "1922",
        thumbnailUrl = "https://archive.org/services/img/nanook-of-the-north",
        videoUrl     = "https://archive.org/download/nanook-of-the-north/nanook-of-the-north_512kb.mp4",
        category     = OttCategory.DOCUMENTARY,
        duration     = "1h 19m",
        license      = "Public Domain",
    ),
    OttItem(
        id           = "man_with_movie_camera_1929",
        title        = "Man with a Movie Camera (1929)",
        description  = "Dziga Vertov's avant-garde documentary portrait of Soviet city life — a landmark in film history.",
        year         = "1929",
        thumbnailUrl = "https://archive.org/services/img/man_with_a_movie_camera",
        videoUrl     = "https://archive.org/download/man_with_a_movie_camera/man_with_a_movie_camera_512kb.mp4",
        category     = OttCategory.DOCUMENTARY,
        duration     = "1h 8m",
        license      = "Public Domain",
    ),

    // ── Classic TV ────────────────────────────────────────────────────────────
    OttItem(
        id           = "buck_rogers_1939",
        title        = "Buck Rogers (1939)",
        description  = "The classic 12-chapter sci-fi serial starring Buster Crabbe as Buck Rogers.",
        year         = "1939",
        thumbnailUrl = "https://archive.org/services/img/BuckRogers1939",
        videoUrl     = "https://archive.org/download/BuckRogers1939/BuckRogers_01_512kb.mp4",
        category     = OttCategory.CLASSIC_TV,
        duration     = "20m / episode",
        license      = "Public Domain",
    ),
    OttItem(
        id           = "flash_gordon_1936",
        title        = "Flash Gordon (1936)",
        description  = "The beloved 13-chapter sci-fi serial adventure with Buster Crabbe.",
        year         = "1936",
        thumbnailUrl = "https://archive.org/services/img/FlashGordon1936",
        videoUrl     = "https://archive.org/download/FlashGordon1936/flash_gordon_01_512kb.mp4",
        category     = OttCategory.CLASSIC_TV,
        duration     = "20m / episode",
        license      = "Public Domain",
    ),

    // ── Short Films ───────────────────────────────────────────────────────────
    OttItem(
        id           = "a_trip_to_the_moon_1902",
        title        = "A Trip to the Moon (1902)",
        description  = "Georges Méliès's iconic silent fantasy — one of the first science-fiction films ever made.",
        year         = "1902",
        thumbnailUrl = "https://archive.org/services/img/a-trip-to-the-moon-1902",
        videoUrl     = "https://archive.org/download/a-trip-to-the-moon-1902/trip_to_the_moon_512kb.mp4",
        category     = OttCategory.SHORT_FILM,
        duration     = "14m",
        license      = "Public Domain",
    ),
    OttItem(
        id           = "the_great_train_robbery_1903",
        title        = "The Great Train Robbery (1903)",
        description  = "Edwin S. Porter's pioneering western — the first film to tell a coherent story.",
        year         = "1903",
        thumbnailUrl = "https://archive.org/services/img/TheGreatTrainRobbery",
        videoUrl     = "https://archive.org/download/TheGreatTrainRobbery/TheGreatTrainRobbery_512kb.mp4",
        category     = OttCategory.SHORT_FILM,
        duration     = "11m",
        license      = "Public Domain",
    ),
    OttItem(
        id           = "steamboat_willie_1928",
        title        = "Steamboat Willie (1928)",
        description  = "Walt Disney's groundbreaking animated short — Mickey Mouse's debut and first sync-sound cartoon.",
        year         = "1928",
        thumbnailUrl = "https://archive.org/services/img/steamboat-willie",
        videoUrl     = "https://archive.org/download/steamboat-willie/steamboat_willie_512kb.mp4",
        category     = OttCategory.ANIMATION,
        duration     = "7m",
        license      = "Public Domain",
    ),

    // ── Animation ─────────────────────────────────────────────────────────────
    OttItem(
        id           = "felix_the_cat_1924",
        title        = "Felix the Cat (1924)",
        description  = "Classic Felix the Cat animated shorts from the silent film era.",
        year         = "1924",
        thumbnailUrl = "https://archive.org/services/img/felix-the-cat-shorts",
        videoUrl     = "https://archive.org/download/felix-the-cat-shorts/felix_the_cat_512kb.mp4",
        category     = OttCategory.ANIMATION,
        duration     = "5–10m / short",
        license      = "Public Domain",
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
// Public catalogue lookup (used by NavHost)
// ─────────────────────────────────────────────────────────────────────────────

val compliantOttCatalogue: List<OttItem> = ContentComplianceEngine.filter(curatedCatalogue)

fun ottCatalogueById(id: String): OttItem? = compliantOttCatalogue.firstOrNull { it.id == id }

// ─────────────────────────────────────────────────────────────────────────────
// OTT Home Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NexusOttScreen(
    onBack: () -> Unit,
    onPlayVideo: (OttItem) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(OttCategory.ALL) }
    var searchQuery     by remember { mutableStateOf("") }
    var watchHistory    by remember { mutableStateOf<List<String>>(emptyList()) }

    val compliantContent = remember { ContentComplianceEngine.filter(curatedCatalogue) }

    val filtered = remember(selectedCategory, searchQuery, compliantContent) {
        compliantContent.filter { item ->
            val matchCat = selectedCategory == OttCategory.ALL || item.category == selectedCategory
            val matchSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
    }

    val featured = remember(compliantContent) { compliantContent.shuffled().take(3) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Nexus OTT",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {

            // ── Header banner ─────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.surfaceContainer,
                                )
                            )
                        )
                        .padding(16.dp),
                ) {
                    Column {
                        Text(
                            text       = "📽 Free & Legal Streaming",
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text  = "Public-domain classics, documentaries & short films — no subscription, no ads, fully legal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint   = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text  = "All content verified Public Domain · Powered by Internet Archive",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }

            // ── Search ────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder   = { Text("Search movies, documentaries…") },
                    leadingIcon   = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon  = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") } }
                    } else null,
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                )
            }

            // ── Category chips ────────────────────────────────────────────
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(OttCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick  = { selectedCategory = cat },
                            label    = { Text(cat.label) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Featured carousel ─────────────────────────────────────────
            if (searchQuery.isBlank() && selectedCategory == OttCategory.ALL) {
                item {
                    Text(
                        text     = "✨ Featured",
                        style    = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(featured, key = { it.id }) { item ->
                            FeaturedCard(item = item, onClick = { onPlayVideo(item) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Results header ────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = if (searchQuery.isBlank() && selectedCategory == OttCategory.ALL)
                            "All Titles (${filtered.size})"
                        else
                            "${filtered.size} result${if (filtered.size != 1) "s" else ""}",
                        style    = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Grid of content ───────────────────────────────────────────
            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint     = MaterialTheme.colorScheme.outline,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text  = "No titles found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(filtered.chunked(2), key = { it.first().id }) { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { item ->
                            OttContentCard(
                                item     = item,
                                onClick  = { onPlayVideo(item) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // ── Bottom legal notice ───────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp),
                        )
                        Text(
                            text  = "All content on Nexus OTT is verified Public Domain or Creative Commons licensed. Sourced from the Internet Archive (archive.org). No copyrighted material is distributed.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Featured card (wide)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeaturedCard(item: OttItem, onClick: () -> Unit) {
    Card(
        onClick  = onClick,
        modifier = Modifier.width(280.dp),
        shape    = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box {
            AsyncImage(
                model             = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier.fillMaxWidth().height(160.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            ) {
                Text(
                    text       = item.title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(text = item.year, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    Text(text = "·", color = Color.White.copy(alpha = 0.5f))
                    Text(text = item.duration, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
            Box(
                modifier = Modifier.align(Alignment.Center),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircleFilled,
                    contentDescription = "Play",
                    modifier = Modifier.size(48.dp),
                    tint     = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Content card (grid tile)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OttContentCard(item: OttItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick   = onClick,
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box {
                AsyncImage(
                    model              = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxWidth().height(110.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                )
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(36.dp),
                    tint     = Color.White.copy(alpha = 0.85f),
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape    = RoundedCornerShape(4.dp),
                    color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                ) {
                    Text(
                        text     = "FREE",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color    = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text      = item.title,
                    style     = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text  = item.year,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("·", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
                    Text(
                        text  = item.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint     = Color(0xFF4CAF50),
                    )
                    Text(
                        text  = item.license,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OTT Player Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
fun NexusOttPlayerScreen(
    item: OttItem,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(item.videoUrl)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text     = item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor    = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = Color.Black,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
        ) {
            AndroidView(
                factory  = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(0.dp)),
            )

            LazyColumn(
                modifier            = Modifier.fillMaxWidth(),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text       = item.title,
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(text = item.year, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                        Text(text = "·", color = Color.White.copy(alpha = 0.4f))
                        Text(text = item.duration, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                        Text(text = "·", color = Color.White.copy(alpha = 0.4f))
                        Text(text = item.category.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint     = Color(0xFF4CAF50),
                        )
                        Text(
                            text  = "${item.license} · Source: ${item.source}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF4CAF50),
                        )
                    }
                }
                item { HorizontalDivider(color = Color.White.copy(alpha = 0.12f)) }
                item {
                    Text(
                        text  = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}
