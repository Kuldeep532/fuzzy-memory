package com.nexuswavetech.nexusplus.features.allfeatures

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexuswavetech.nexusplus.core.FeatureCategory
import com.nexuswavetech.nexusplus.platform.PlatformToast
import com.nexuswavetech.nexusplus.ui.components.FeatureCard
import com.nexuswavetech.nexusplus.ui.components.GatekeeperDialog
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private fun FeatureCategory.icon(): ImageVector = when (this) {
    FeatureCategory.MEDIA        -> Icons.Filled.PlayCircle
    FeatureCategory.PRODUCTIVITY -> Icons.Filled.Work
    FeatureCategory.UTILITIES    -> Icons.Filled.Build
    FeatureCategory.SECURITY     -> Icons.Filled.Lock
    FeatureCategory.TOOLS        -> Icons.Filled.Handyman
    else                         -> Icons.Filled.Apps
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFeaturesScreen(
    rootNavController: NavController,
    viewModel: AllFeaturesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val toast: PlatformToast = koinInject()

    LaunchedEffect(uiState.pendingRoute) {
        uiState.pendingRoute?.let { route ->
            rootNavController.navigate(route)
            viewModel.onNavigationConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header banner ─────────────────────────────────────────────────
        Surface(
            color    = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Filled.Apps,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "All Features",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.semantics { heading() },
                    )
                    AnimatedContent(
                        targetState = uiState.features.size,
                        label       = "feature_count",
                    ) { count ->
                        Text(
                            text  = "$count feature${if (count != 1) "s" else ""}${if (uiState.selectedCategory != null || uiState.searchQuery.isNotBlank()) " found" else " available"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier.semantics {
                                contentDescription = "$count features${if (uiState.selectedCategory != null || uiState.searchQuery.isNotBlank()) " found" else " available"}"
                            },
                        )
                    }
                }
            }
        }

        // ── Search bar ─────────────────────────────────────────────────────
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query            = uiState.searchQuery,
                    onQueryChange    = viewModel::onSearchChanged,
                    onSearch         = {},
                    expanded         = false,
                    onExpandedChange = {},
                    placeholder      = { Text("Search ${uiState.features.size} features…") },
                    leadingIcon      = {
                        Icon(Icons.Filled.Search, contentDescription = "Search icon")
                    },
                    trailingIcon     = if (uiState.searchQuery.isNotBlank()) ({
                        IconButton(
                            onClick  = { viewModel.onSearchChanged("") },
                            modifier = Modifier.semantics { contentDescription = "Clear search" },
                        ) { Icon(Icons.Filled.Clear, null) }
                    }) else null,
                )
            },
            expanded         = false,
            onExpandedChange = {},
            modifier         = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { contentDescription = "Search features. Type to filter the list." },
        ) {}

        // ── Category filter chips ──────────────────────────────────────────
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.semantics { contentDescription = "Category filters" },
        ) {
            item {
                FilterChip(
                    selected     = uiState.selectedCategory == null,
                    onClick      = { viewModel.onCategorySelected(null) },
                    label        = { Text("All") },
                    leadingIcon  = if (uiState.selectedCategory == null) ({
                        Icon(Icons.Filled.Done, null, modifier = Modifier.size(16.dp))
                    }) else null,
                    modifier     = Modifier.semantics {
                        contentDescription = "All categories${if (uiState.selectedCategory == null) ". Selected." else ""}"
                    },
                )
            }
            items(FeatureCategory.entries) { category ->
                FilterChip(
                    selected    = uiState.selectedCategory == category,
                    onClick     = { viewModel.onCategorySelected(category) },
                    leadingIcon = {
                        Icon(category.icon(), null, modifier = Modifier.size(16.dp))
                    },
                    label       = { Text(category.label) },
                    modifier    = Modifier.semantics {
                        contentDescription = "${category.label} filter${if (uiState.selectedCategory == category) ". Selected." else ""}"
                    },
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Grid or empty state ───────────────────────────────────────────
        AnimatedContent(
            targetState = uiState.features.isEmpty(),
            label       = "empty_state",
        ) { isEmpty ->
            if (isEmpty) {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = if (uiState.searchQuery.isNotBlank())
                                "No features match your search for ${uiState.searchQuery}."
                            else "No features in this category."
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(52.dp),
                        )
                        Text(
                            text  = "No features found",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (uiState.searchQuery.isNotBlank()) {
                            OutlinedButton(
                                onClick  = { viewModel.onSearchChanged("") },
                                modifier = Modifier.semantics { contentDescription = "Clear search and show all features" },
                            ) {
                                Icon(Icons.Filled.Clear, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Clear search")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns               = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.fillMaxSize(),
                ) {
                    items(uiState.features, key = { it.id.name }) { feature ->
                        FeatureCard(
                            feature          = feature,
                            onTap            = { viewModel.onFeatureTapped(feature) },
                            onToggleFavorite = {
                                val msg = if (feature.isFavorite) "${feature.name} removed from favorites"
                                          else "${feature.name} added to favorites"
                                toast.show(msg)
                                viewModel.onToggleFavorite(feature)
                            },
                            onTogglePin      = {
                                val msg = if (feature.isPinned) "${feature.name} unpinned from Home"
                                          else "${feature.name} pinned to Home"
                                toast.show(msg)
                                viewModel.onTogglePin(feature)
                            },
                        )
                    }
                }
            }
        }
    }

    uiState.gatekeeperBlocked?.let { featureName ->
        GatekeeperDialog(
            featureName     = featureName,
            onSignInClicked = { viewModel.onGatekeeperDismissed() },
            onDismiss       = viewModel::onGatekeeperDismissed,
        )
    }
}
