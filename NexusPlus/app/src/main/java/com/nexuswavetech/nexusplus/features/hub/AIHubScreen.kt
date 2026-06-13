package com.nexuswavetech.nexusplus.features.hub

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
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

/**
 * AI & Voice Hub — consolidates: AI Image Generator, TTS (NSE),
 * Text Translator, Voice Typer, Object Detector, Colour Detector.
 *
 * The hub maps features by their content affinity with AI/ML,
 * not just by their [FeatureCategory], so it pulls from MEDIA,
 * UTILITIES, and TOOLS buckets.
 */
@Composable
fun AIHubScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val favoritesRepository: FavoritesRepository = koinInject()
    val sessionManager: SessionManager           = koinInject()
    val recentRepo: RecentActivityRepository     = koinInject()
    val view  = LocalView.current
    val scope = rememberCoroutineScope()

    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    var gatekeeperBlocked by remember { mutableStateOf<String?>(null) }

    val aiFeatureIds = setOf(
        FeatureId.AI_IMAGE_GENERATOR,
        FeatureId.NEXUS_TTS,
        FeatureId.TEXT_TRANSLATOR,
        FeatureId.VOICE_TYPER,
        FeatureId.OBJECT_DETECTOR,
        FeatureId.COLOR_DETECTOR,
    )

    val features = remember(favoriteIds) {
        FeatureCatalog.allFeatures
            .filter { it.id in aiFeatureIds }
            .map { it.copy(isFavorite = it.id.name in favoriteIds) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "AI & Voice Hub", onBack = onBack)

        HubHeader(
            icon        = FeatureHub.AI.icon,
            description = FeatureHub.AI.description,
            color       = FeatureHub.AI.color,
            count       = features.size,
        )

        Text(
            text     = "AI & Voice Tools",
            style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() },
        )

        LazyVerticalGrid(
            columns               = GridCells.Adaptive(minSize = 160.dp),
            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
        ) {
            items(items = features, key = { it.id.name }) { feature ->
                FeatureCard(
                    feature = feature,
                    onTap   = {
                        val result = NexusGatekeeper.checkAccess(
                            feature.id, sessionManager.currentSession(), feature.name
                        )
                        when (result) {
                            is NexusGatekeeper.AccessResult.Allowed -> {
                                scope.launch { recentRepo.recordVisit(feature.id) }
                                onNavigate(feature.route)
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
                )
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
