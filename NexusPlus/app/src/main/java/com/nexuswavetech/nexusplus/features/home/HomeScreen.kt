package com.nexuswavetech.nexusplus.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
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

    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    val recentIds   by recentRepo.recentIds.collectAsState(initial = emptyList())
    var gatekeeperBlocked by remember { mutableStateOf<String?>(null) }

    val favoritedFeatures = remember(favoriteIds) {
        FeatureCatalog.allFeatures
            .filter { it.id.name in favoriteIds }
            .map { it.copy(isFavorite = true) }
    }

    val recentFeatures = remember(recentIds, favoriteIds) {
        val catalog = FeatureCatalog.allFeatures.associateBy { it.id.name }
        recentIds.mapNotNull { id -> catalog[id]?.copy(isFavorite = id in favoriteIds) }
    }

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

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                JaiShriKrishnaGreeting()
                Spacer(Modifier.height(16.dp))
                val session by sessionManager.session.collectAsState()
                val name    = session.displayName.ifBlank { "there" }
                Text(
                    text     = "Welcome, $name 👋",
                    style    = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color    = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text  = "What would you like to do today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Quick Search Bar ──────────────────────────────────────────────
        item {
            Surface(
                onClick   = { rootNavController.navigate(Screen.Main.route) },
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .semantics { contentDescription = "Search all features. Double tap to open search." },
                shape     = RoundedCornerShape(28.dp),
                color     = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text  = "Search all features…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Hub Cards ─────────────────────────────────────────────────────
        item {
            SectionHeader(
                title   = "Feature Hubs",
                icon    = Icons.Filled.GridView,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
        }

        items(FeatureHub.entries) { hub ->
            HubCard(
                hub          = hub,
                featureCount = FeatureCatalog.forHub(hub).size,
                onClick      = { rootNavController.navigate(hub.route) },
                modifier     = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Recent Activity ───────────────────────────────────────────────
        if (recentFeatures.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionHeader(
                        title = "Recent Activity",
                        icon  = Icons.Filled.History,
                    )
                    TextButton(onClick = { scope.launch { recentRepo.clearHistory() } }) {
                        Text("Clear", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.semantics {
                        contentDescription = "Recent features. ${recentFeatures.size} items."
                    },
                ) {
                    items(recentFeatures, key = { it.id.name }) { feature ->
                        RecentFeatureChip(
                            feature = feature,
                            onClick = { onFeatureTap(feature) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Favorites ─────────────────────────────────────────────────────
        if (favoritedFeatures.isNotEmpty()) {
            item {
                SectionHeader(
                    title    = "Favorites",
                    icon     = Icons.Filled.Favorite,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.semantics {
                        contentDescription = "Favorite features. ${favoritedFeatures.size} items."
                    },
                ) {
                    items(favoritedFeatures, key = { it.id.name }) { feature ->
                        RecentFeatureChip(
                            feature = feature,
                            onClick = { onFeatureTap(feature) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Empty State (no recent, no favorites) ─────────────────────────
        if (recentFeatures.isEmpty() && favoritedFeatures.isEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .semantics { contentDescription = "No recent activity yet. Tap any hub card to get started." },
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Star,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            "Tap a hub above to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier.semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp),
        )
        Text(
            text  = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun RecentFeatureChip(
    feature: FeatureItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${feature.name}. Double tap to open." },
        shape  = RoundedCornerShape(14.dp),
        color  = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector        = feature.icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(20.dp),
            )
            Text(
                text      = feature.name,
                style     = MaterialTheme.typography.labelLarge,
                color     = MaterialTheme.colorScheme.onSurface,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                modifier  = Modifier.widthIn(max = 120.dp),
            )
        }
    }
}

@Composable
private fun JaiShriKrishnaGreeting() {
    val text = stringResource(R.string.jai_shri_krishna)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
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
                text  = "🙏 $text",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
