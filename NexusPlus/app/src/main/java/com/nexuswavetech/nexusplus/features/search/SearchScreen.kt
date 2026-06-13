package com.nexuswavetech.nexusplus.features.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.nexuswavetech.nexusplus.ui.components.GatekeeperDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(rootNavController: NavController) {
    val favoritesRepository: FavoritesRepository = koinInject()
    val sessionManager: SessionManager           = koinInject()
    val recentRepo: RecentActivityRepository     = koinInject()
    val view         = LocalView.current
    val scope        = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var query         by remember { mutableStateOf("") }
    var gatekeeperBlocked by remember { mutableStateOf<String?>(null) }

    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())

    val results = remember(query, favoriteIds) {
        if (query.isBlank()) emptyList()
        else FeatureCatalog.allFeatures
            .map { it.copy(isFavorite = it.id.name in favoriteIds) }
            .filter { f ->
                f.name.contains(query, ignoreCase = true) ||
                f.description.contains(query, ignoreCase = true) ||
                f.keywords.any { it.contains(query, ignoreCase = true) }
            }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Search Bar ────────────────────────────────────────────────────
        SearchBar(
            query          = query,
            onQueryChange  = { query = it },
            onSearch       = {},
            active         = false,
            onActiveChange = {},
            placeholder    = { Text("Search all 49 features…") },
            leadingIcon    = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            trailingIcon   = {
                AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier       = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester)
                .semantics { contentDescription = "Search bar. Type to search all features." },
        ) {}

        if (query.isBlank()) {
            // ── Prompt state ──────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Start typing to search across all features by name, category, or keyword." },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier            = Modifier.padding(32.dp),
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    )
                    Text(
                        "Search any feature",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Search by name, description, or keyword\n(e.g. \"encrypt\", \"pdf\", \"radio\", \"sha\")",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        } else if (results.isEmpty()) {
            // ── No results state ──────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "No features match \"$query\"." },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier            = Modifier.padding(32.dp),
                ) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    )
                    Text(
                        "No results for \"$query\"",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Try a different search term",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            // ── Results list ──────────────────────────────────────────────
            Text(
                text     = "${results.size} result${if (results.size != 1) "s" else ""}",
                style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .semantics { contentDescription = "${results.size} results found for $query" },
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = { it.id.name }) { feature ->
                    SearchResultCard(
                        feature = feature,
                        query   = query,
                        onTap   = {
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

@Composable
private fun SearchResultCard(
    feature: FeatureItem,
    query: String,
    onTap: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(
        onClick   = onTap,
        modifier  = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${feature.name}. ${feature.description}. " +
                    "Category: ${feature.category.label}. " +
                    if (feature.isFavorite) "Favorited." else "" +
                    " Double tap to open."
            },
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = feature.icon,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier           = Modifier.size(22.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = feature.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text  = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text  = feature.category.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            IconButton(
                onClick  = onToggleFavorite,
                modifier = Modifier.semantics {
                    contentDescription = if (feature.isFavorite)
                        "Remove ${feature.name} from favorites"
                    else
                        "Add ${feature.name} to favorites"
                },
            ) {
                Icon(
                    imageVector        = if (feature.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint               = if (feature.isFavorite) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
