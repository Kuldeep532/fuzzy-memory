package com.nexuswavetech.nexusplus.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexuswavetech.nexusplus.R
import com.nexuswavetech.nexusplus.core.*
import com.nexuswavetech.nexusplus.navigation.Screen
import com.nexuswavetech.nexusplus.ui.components.FeatureCard
import com.nexuswavetech.nexusplus.ui.components.GatekeeperDialog
import com.nexuswavetech.nexusplus.ui.components.HubCard
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun HomeScreen(rootNavController: NavController) {
    val favoritesRepository: FavoritesRepository = koinInject()
    val sessionManager: SessionManager           = koinInject()
    val recentRepo: RecentActivityRepository     = koinInject()
    val view  = LocalView.current
    val scope = rememberCoroutineScope()

    val session     by sessionManager.session.collectAsState()
    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    val pinnedIds   by favoritesRepository.pinnedIds.collectAsState(initial = emptySet())
    val recentIds   by recentRepo.recentIds.collectAsState(initial = emptyList())
    val mostUsedIds by recentRepo.mostUsedIds.collectAsState(initial = emptyList())

    var gatekeeperBlocked by remember { mutableStateOf<String?>(null) }

    // ── Derived feature lists ─────────────────────────────────────────────

    fun enrich(item: FeatureItem) = item.copy(
        isFavorite = item.id.name in favoriteIds,
        isPinned   = item.id.name in pinnedIds,
    )

    val catalog = remember { FeatureCatalog.allFeatures }

    val pinnedFeatures = remember(pinnedIds, favoriteIds) {
        catalog.filter { it.id.name in pinnedIds }.map(::enrich)
    }

    val recentFeatures = remember(recentIds, favoriteIds, pinnedIds) {
        val byId = catalog.associateBy { it.id.name }
        recentIds.mapNotNull { byId[it]?.let(::enrich) }
    }

    val mostUsedFeatures = remember(mostUsedIds, favoriteIds, pinnedIds) {
        if (mostUsedIds.size < 3) emptyList()
        else {
            val byId = catalog.associateBy { it.id.name }
            mostUsedIds.take(5).mapNotNull { byId[it]?.let(::enrich) }
        }
    }

    val favoritedFeatures = remember(favoriteIds, pinnedIds) {
        catalog.filter { it.id.name in favoriteIds }.map(::enrich)
    }

    val newFeatures = remember { catalog.filter { it.isNew } }

    val suggestedFeatures = remember(recentIds, favoriteIds, pinnedIds) {
        val usedIds = (recentIds + favoriteIds + pinnedIds).toSet()
        catalog.filter { it.id.name !in usedIds }.take(5).map(::enrich)
    }

    // ── Tap handler ───────────────────────────────────────────────────────
    fun onFeatureTap(feature: FeatureItem) {
        val result = NexusGatekeeper.checkAccess(
            feature.id, sessionManager.currentSession(), feature.name
        )
        when (result) {
            is NexusGatekeeper.AccessResult.Allowed -> {
                scope.launch { recentRepo.recordVisit(feature.id) }
                rootNavController.navigate(feature.route)
            }
            is NexusGatekeeper.AccessResult.Blocked -> { gatekeeperBlocked = result.featureName }
        }
    }

    fun onToggleFav(feature: FeatureItem) {
        val msg = if (feature.isFavorite) "${feature.name} removed from favorites"
                  else "${feature.name} added to favorites"
        view.announceForAccessibility(msg)
        scope.launch { favoritesRepository.toggleFavorite(feature.id) }
    }

    fun onTogglePin(feature: FeatureItem) {
        val msg = if (feature.isPinned) "${feature.name} unpinned from Home"
                  else "${feature.name} pinned to Home"
        view.announceForAccessibility(msg)
        scope.launch { favoritesRepository.togglePin(feature.id) }
    }

    // ── UI ────────────────────────────────────────────────────────────────
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {

        // Header
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                JaiShriKrishnaGreeting()
                Spacer(Modifier.height(14.dp))
                val name = session.displayName.ifBlank { "there" }
                Text(
                    text     = "Welcome back, $name 👋",
                    style    = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text  = "What would you like to explore today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
            }
        }

        // Quick Search shortcut
        item {
            Surface(
                onClick  = { /* Search tab handled by MainScaffold */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .semantics { contentDescription = "Search. Tap the Search tab to find any feature." },
                shape    = RoundedCornerShape(28.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Search all ${FeatureCatalog.allFeatures.size} features…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── New Features ──────────────────────────────────────────────────
        if (newFeatures.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title    = "New Features",
                    icon     = Icons.Filled.NewReleases,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(newFeatures.map(::enrich), key = { "new_${it.id.name}" }) { feature ->
                        QuickChip(feature = feature, onClick = { onFeatureTap(feature) })
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Pinned Features ───────────────────────────────────────────────
        if (pinnedFeatures.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title    = "Pinned",
                    icon     = Icons.Filled.PushPin,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            items(pinnedFeatures, key = { "pin_${it.id.name}" }) { feature ->
                FeatureCard(
                    feature          = feature,
                    onTap            = { onFeatureTap(feature) },
                    onToggleFavorite = { onToggleFav(feature) },
                    onTogglePin      = { onTogglePin(feature) },
                    modifier         = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp),
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // ── Recently Used ─────────────────────────────────────────────────
        if (recentFeatures.isNotEmpty()) {
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HomeSectionHeader(title = "Recently Used", icon = Icons.Filled.History)
                    TextButton(onClick = { scope.launch { recentRepo.clearHistory() } }) {
                        Text("Clear", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(recentFeatures, key = { "rec_${it.id.name}" }) { feature ->
                        QuickChip(feature = feature, onClick = { onFeatureTap(feature) })
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Most Used ─────────────────────────────────────────────────────
        if (mostUsedFeatures.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title    = "Most Used",
                    icon     = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(mostUsedFeatures, key = { "top_${it.id.name}" }) { feature ->
                        QuickChip(feature = feature, onClick = { onFeatureTap(feature) })
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Favorites ─────────────────────────────────────────────────────
        if (favoritedFeatures.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title    = "Favorites",
                    icon     = Icons.Filled.Favorite,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(favoritedFeatures, key = { "fav_${it.id.name}" }) { feature ->
                        QuickChip(feature = feature, onClick = { onFeatureTap(feature) })
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Feature Hubs ──────────────────────────────────────────────────
        item {
            HomeSectionHeader(
                title    = "Feature Hubs",
                icon     = Icons.Filled.GridView,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
        }

        items(FeatureHub.entries, key = { "hub_${it.name}" }) { hub ->
            HubCard(
                hub          = hub,
                featureCount = FeatureCatalog.forHub(hub).size,
                onClick      = { rootNavController.navigate(hub.route) },
                modifier     = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
            )
        }

        // ── Suggested for You ─────────────────────────────────────────────
        if (suggestedFeatures.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                HomeSectionHeader(
                    title    = "Suggested for You",
                    icon     = Icons.Filled.AutoAwesome,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(suggestedFeatures, key = { "sug_${it.id.name}" }) { feature ->
                        QuickChip(feature = feature, onClick = { onFeatureTap(feature) })
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Empty nudge ───────────────────────────────────────────────────
        if (pinnedFeatures.isEmpty() && recentFeatures.isEmpty() && favoritedFeatures.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier              = Modifier
                            .padding(24.dp)
                            .semantics { contentDescription = "Tip: Long press any feature card to pin it to Home or add it to favorites." },
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            "Long press any feature card",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "to Pin it here, add to Favorites, or share it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    gatekeeperBlocked?.let { name ->
        GatekeeperDialog(
            featureName     = name,
            onSignInClicked = { gatekeeperBlocked = null },
            onDismiss       = { gatekeeperBlocked = null },
        )
    }
}

// ── Private helpers ─────────────────────────────────────────────────────────

@Composable
private fun HomeSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier.semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun QuickChip(
    feature: FeatureItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${feature.name}. Double tap to open." },
        shape          = RoundedCornerShape(14.dp),
        color          = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(feature.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                text     = feature.name,
                style    = MaterialTheme.typography.labelLarge,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp),
            )
            if (feature.isNew) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                ) {
                    Text(
                        "N",
                        style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color    = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun JaiShriKrishnaGreeting() {
    val text = stringResource(R.string.jai_shri_krishna)
    Surface(
        shape    = MaterialTheme.shapes.medium,
        color    = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = text },
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                "🙏 $text",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
