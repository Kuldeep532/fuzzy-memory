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
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@Composable
fun FavoritesScreen(rootNavController: NavController) {
    val favoritesRepository: FavoritesRepository = koinInject()
    val sessionManager: SessionManager = koinInject()
    val view = LocalView.current

    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    val gatekeeperBlocked = remember { mutableStateOf<String?>(null) }

    val favoriteFeatures = remember(favoriteIds) {
        FeatureCatalog.allFeatures
            .filter { it.id.name in favoriteIds }
            .map { it.copy(isFavorite = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Favorites",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${favoriteFeatures.size} bookmarked features",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = "${favoriteFeatures.size} bookmarked features. Long press any feature card to manage favorites."
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (favoriteFeatures.isEmpty()) {
            // Accessible empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "No favorites yet. Go to All Features tab and long press any feature to add it to favorites."
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "No favorites yet",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Long press any feature in All Features\nto bookmark it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = favoriteFeatures,
                    key = { it.id.name }
                ) { feature ->
                    FeatureCard(
                        feature = feature,
                        onTap = {
                            val session = sessionManager.currentSession()
                            val result = NexusGatekeeper.checkAccess(feature.id, session, feature.name)
                            when (result) {
                                is NexusGatekeeper.AccessResult.Allowed -> rootNavController.navigate(feature.route)
                                is NexusGatekeeper.AccessResult.Blocked -> gatekeeperBlocked.value = result.featureName
                            }
                        },
                        onToggleFavorite = {
                            val msg = "${feature.name} removed from favorites"
                            view.announceForAccessibility(msg)
                            // Remove via repository — re-collected automatically
                        }
                    )
                }
            }
        }
    }

    gatekeeperBlocked.value?.let { featureName ->
        GatekeeperDialog(
            featureName = featureName,
            onSignInClicked = { gatekeeperBlocked.value = null },
            onDismiss = { gatekeeperBlocked.value = null }
        )
    }
}
