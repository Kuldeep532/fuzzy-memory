package com.nexuswavetech.nexusplus.features.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexuswavetech.nexusplus.core.*
import com.nexuswavetech.nexusplus.ui.components.FeatureCard
import com.nexuswavetech.nexusplus.ui.components.GatekeeperDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(rootNavController: NavController) {
    val favoritesRepository: FavoritesRepository = koinInject()
    val sessionManager: SessionManager           = koinInject()
    val recentRepo: RecentActivityRepository     = koinInject()
    val view          = LocalView.current
    val scope         = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var query             by remember { mutableStateOf("") }
    var gatekeeperBlocked by remember { mutableStateOf<String?>(null) }

    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    val pinnedIds   by favoritesRepository.pinnedIds.collectAsState(initial = emptySet())

    val results = remember(query, favoriteIds, pinnedIds) {
        if (query.isBlank()) emptyList()
        else FeatureCatalog.allFeatures
            .map { it.copy(isFavorite = it.id.name in favoriteIds, isPinned = it.id.name in pinnedIds) }
            .filter { f ->
                f.name.contains(query, ignoreCase = true) ||
                f.description.contains(query, ignoreCase = true) ||
                f.keywords.any { it.contains(query, ignoreCase = true) }
            }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Search bar ─────────────────────────────────────────────────────
        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            placeholder   = { Text("Search features, tools, settings…") },
            leadingIcon   = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon  = {
                AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine    = true,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .focusRequester(focusRequester)
                .semantics { contentDescription = "Search field. Type to search all ${FeatureCatalog.allFeatures.size} features." },
        )

        // ── Status ─────────────────────────────────────────────────────────
        when {
            query.isBlank() -> {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Type to search across all ${FeatureCatalog.allFeatures.size} features, tools and settings." },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ManageSearch,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        )
                        Text(
                            "Search anything",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Text(
                            "Features · Tools · Categories · Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                    }
                }
            }

            results.isEmpty() -> {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "No results found for $query." },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        )
                        Text(
                            "No results for \"$query\"",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Try a different keyword",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            else -> {
                // ── Result count ───────────────────────────────────────────
                Text(
                    text     = "${results.size} result${if (results.size != 1) "s" else ""}",
                    style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .semantics {
                            contentDescription = "${results.size} results found for $query"
                        },
                )

                // ── Results list ───────────────────────────────────────────
                LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.id.name }) { feature ->
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
    }

    gatekeeperBlocked?.let { name ->
        GatekeeperDialog(
            featureName     = name,
            onSignInClicked = { gatekeeperBlocked = null },
            onDismiss       = { gatekeeperBlocked = null },
        )
    }
}
