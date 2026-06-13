package com.nexuswavetech.nexusplus.features.hub

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.core.*
import com.nexuswavetech.nexusplus.ui.components.FeatureCard
import com.nexuswavetech.nexusplus.ui.components.GatekeeperDialog
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun UtilitiesHubScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val favoritesRepository: FavoritesRepository = koinInject()
    val sessionManager: SessionManager           = koinInject()
    val recentRepo: RecentActivityRepository     = koinInject()
    val view  = LocalView.current
    val scope = rememberCoroutineScope()

    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    val pinnedIds   by favoritesRepository.pinnedIds.collectAsState(initial = emptySet())
    var searchQuery       by remember { mutableStateOf("") }
    var gatekeeperBlocked by remember { mutableStateOf<String?>(null) }

    val allUtilities = remember(favoriteIds, pinnedIds) {
        FeatureCatalog.forHub(FeatureHub.UTILITIES).map {
            it.copy(isFavorite = it.id.name in favoriteIds, isPinned = it.id.name in pinnedIds)
        }
    }

    val displayed = remember(allUtilities, searchQuery) {
        if (searchQuery.isBlank()) allUtilities
        else allUtilities.filter { f ->
            f.name.contains(searchQuery, ignoreCase = true) ||
            f.description.contains(searchQuery, ignoreCase = true) ||
            f.keywords.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Utilities Hub", onBack = onBack)

        HubHeader(title = "Utilities & Smart Tools", icon = FeatureHub.UTILITIES.icon, description = FeatureHub.UTILITIES.description, color = FeatureHub.UTILITIES.color, count = allUtilities.size)

        OutlinedTextField(
            value         = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder   = { Text("Search utilities…") },
            leadingIcon   = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine    = true,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { contentDescription = "Search utilities. Type to filter." },
        )

        Text(
            text     = "${displayed.size} tools",
            style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
        )

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns               = GridCells.Adaptive(minSize = 160.dp),
            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
        ) {
            items(displayed, key = { it.id.name }) { feature ->
                FeatureCard(
                    feature          = feature,
                    onTap            = {
                        val result = NexusGatekeeper.checkAccess(feature.id, sessionManager.currentSession(), feature.name)
                        when (result) {
                            is NexusGatekeeper.AccessResult.Allowed -> { scope.launch { recentRepo.recordVisit(feature.id) }; onNavigate(feature.route) }
                            is NexusGatekeeper.AccessResult.Blocked -> { gatekeeperBlocked = result.featureName }
                        }
                    },
                    onToggleFavorite = {
                        view.announceForAccessibility(if (feature.isFavorite) "${feature.name} removed from favorites" else "${feature.name} added to favorites")
                        scope.launch { favoritesRepository.toggleFavorite(feature.id) }
                    },
                    onTogglePin = {
                        view.announceForAccessibility(if (feature.isPinned) "${feature.name} unpinned from Home" else "${feature.name} pinned to Home")
                        scope.launch { favoritesRepository.togglePin(feature.id) }
                    },
                )
            }
        }
    }

    gatekeeperBlocked?.let { name ->
        GatekeeperDialog(featureName = name, onSignInClicked = { gatekeeperBlocked = null }, onDismiss = { gatekeeperBlocked = null })
    }
}
