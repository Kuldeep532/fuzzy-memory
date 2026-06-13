package com.nexuswavetech.nexusplus.features.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexuswavetech.nexusplus.R
import com.nexuswavetech.nexusplus.core.*
import com.nexuswavetech.nexusplus.ui.components.FeatureCard
import com.nexuswavetech.nexusplus.ui.components.GatekeeperDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun HomeScreen(rootNavController: NavController) {
    val favoritesRepository: FavoritesRepository = koinInject()
    val sessionManager: SessionManager           = koinInject()
    val view  = LocalView.current
    val scope = rememberCoroutineScope()

    val favoriteIds    by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    val gatekeeperBlocked = remember { mutableStateOf<String?>(null) }

    val pinnedFeatures = remember(favoriteIds) {
        FeatureCatalog.allFeatures
            .filter { it.id.name in favoriteIds }
            .map { it.copy(isFavorite = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Sacred designation — screen-reader optimised ───────────────────
        JaiShriKrishnaGreeting()

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text  = "Home",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text  = "${pinnedFeatures.size} pinned feature${if (pinnedFeatures.size != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription =
                    "${pinnedFeatures.size} pinned features. " +
                    "Long press any feature card in All Features to pin it here."
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (pinnedFeatures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription =
                            "No features pinned yet. Go to All Features tab and long press " +
                            "any feature card to pin it to Home."
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Home,
                        contentDescription = null,
                        modifier           = Modifier.size(80.dp),
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Text(
                        text  = "Nothing pinned yet",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text      = "Long press any feature in All Features\nto pin it here for quick access.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Adaptive(minSize = 160.dp),
                contentPadding        = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxSize(),
            ) {
                items(items = pinnedFeatures, key = { it.id.name }) { feature ->
                    FeatureCard(
                        feature = feature,
                        onTap   = {
                            val session = sessionManager.currentSession()
                            val result  = NexusGatekeeper.checkAccess(feature.id, session, feature.name)
                            when (result) {
                                is NexusGatekeeper.AccessResult.Allowed  -> rootNavController.navigate(feature.route)
                                is NexusGatekeeper.AccessResult.Blocked  -> gatekeeperBlocked.value = result.featureName
                            }
                        },
                        onToggleFavorite = {
                            view.announceForAccessibility("${feature.name} unpinned from Home")
                            scope.launch { favoritesRepository.toggleFavorite(feature.id) }
                        },
                    )
                }
            }
        }
    }

    gatekeeperBlocked.value?.let { featureName ->
        GatekeeperDialog(
            featureName    = featureName,
            onSignInClicked = { gatekeeperBlocked.value = null },
            onDismiss       = { gatekeeperBlocked.value = null },
        )
    }
}

// ── Sacred greeting ────────────────────────────────────────────────────────

/**
 * "Jai Shri Krishna" — sacred designation per the architecture directive.
 *
 * Requirements met:
 *  - Localised resource entry: [R.string.jai_shri_krishna]
 *  - Screen-reader optimised: semantics contentDescription set explicitly
 *  - Semantic accessibility support: mergeDescendants = true
 *  - Material 3 styling: primaryContainer surface with body typography
 */
@Composable
private fun JaiShriKrishnaGreeting() {
    val text = stringResource(R.string.jai_shri_krishna)
    Surface(
        shape  = MaterialTheme.shapes.medium,
        color  = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = text
            },
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
