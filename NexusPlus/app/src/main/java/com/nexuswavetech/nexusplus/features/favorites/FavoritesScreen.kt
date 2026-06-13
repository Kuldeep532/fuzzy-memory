package com.nexuswavetech.nexusplus.features.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexuswavetech.nexusplus.core.*
import com.nexuswavetech.nexusplus.ui.components.FeatureCard
import com.nexuswavetech.nexusplus.ui.components.GatekeeperDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun FavoritesScreen(rootNavController: NavController) {
    val favoritesRepository: FavoritesRepository = koinInject()
    val sessionManager: SessionManager           = koinInject()
    val recentRepo: RecentActivityRepository     = koinInject()
    val view  = LocalView.current
    val scope = rememberCoroutineScope()

    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    val pinnedIds   by favoritesRepository.pinnedIds.collectAsState(initial = emptySet())
    var gatekeeperBlocked by remember { mutableStateOf<String?>(null) }

    val favoriteFeatures = remember(favoriteIds, pinnedIds) {
        FeatureCatalog.allFeatures
            .filter { it.id.name in favoriteIds }
            .map { it.copy(isFavorite = true, isPinned = it.id.name in pinnedIds) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text     = "Favorites",
            style    = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text     = "${favoriteFeatures.size} bookmarked features",
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = "${favoriteFeatures.size} bookmarked features. Long press any card for options."
            },
        )

        Spacer(Modifier.height(16.dp))

        if (favoriteFeatures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "No favorites yet. Long press any feature card and choose Add to Favorites."
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Filled.StarBorder,
                        contentDescription = null,
                        modifier           = Modifier.size(80.dp),
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Text(
                        "No favorites yet",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Long press any feature card,\nthen tap \"Add to Favorites\".",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Adaptive(minSize = 160.dp),
                contentPadding        = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxSize(),
            ) {
                items(favoriteFeatures, key = { it.id.name }) { feature ->
                    FeatureCard(
                        feature          = feature,
                        onTap            = {
                            val result = NexusGatekeeper.checkAccess(
                                feature.id, sessionManager.currentSession(), feature.name
                            )
                            when (result) {
                                is NexusGatekeeper.AccessResult.Allowed -> {
                                    scope.launch { recentRepo.recordVisit(feature.id) }
                                    rootNavController.navigate(feature.route)
                                }
                                is NexusGatekeeper.AccessResult.Blocked -> {
                                    gatekeeperBlocked = result.featureName
                                }
                            }
                        },
                        onToggleFavorite = {
                            val msg = if (feature.isFavorite) "${feature.name} removed from favorites"
                                      else "${feature.name} added to favorites"
                            view.announceForAccessibility(msg)
                            scope.launch { favoritesRepository.toggleFavorite(feature.id) }
                        },
                        onTogglePin = {
                            val msg = if (feature.isPinned) "${feature.name} unpinned from Home"
                                      else "${feature.name} pinned to Home"
                            view.announceForAccessibility(msg)
                            scope.launch { favoritesRepository.togglePin(feature.id) }
                        },
                    )
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
