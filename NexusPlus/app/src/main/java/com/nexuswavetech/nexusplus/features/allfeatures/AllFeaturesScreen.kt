package com.nexuswavetech.nexusplus.features.allfeatures

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexuswavetech.nexusplus.core.FeatureCategory
import com.nexuswavetech.nexusplus.ui.components.FeatureCard
import com.nexuswavetech.nexusplus.ui.components.GatekeeperDialog
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFeaturesScreen(
    rootNavController: NavController,
    viewModel: AllFeaturesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val view    = LocalView.current

    LaunchedEffect(uiState.pendingRoute) {
        uiState.pendingRoute?.let { route ->
            rootNavController.navigate(route)
            viewModel.onNavigationConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Search bar ─────────────────────────────────────────────────────
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query            = uiState.searchQuery,
                    onQueryChange    = viewModel::onSearchChanged,
                    onSearch         = {},
                    expanded         = false,
                    onExpandedChange = {},
                    placeholder      = { Text("Search all features…") },
                    leadingIcon      = { Icon(Icons.Filled.Search, contentDescription = "Search") },
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
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.semantics {
                contentDescription = "Category filters"
            },
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick  = { viewModel.onCategorySelected(null) },
                    label    = { Text("All") },
                    modifier = Modifier.semantics {
                        contentDescription = "All categories" +
                            if (uiState.selectedCategory == null) ". Currently selected." else ""
                    },
                )
            }
            items(FeatureCategory.entries) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick  = { viewModel.onCategorySelected(category) },
                    label    = { Text(category.label) },
                    modifier = Modifier.semantics {
                        contentDescription = "${category.label} filter" +
                            if (uiState.selectedCategory == category) ". Currently selected." else ""
                    },
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Feature count ──────────────────────────────────────────────────
        Text(
            text     = "${uiState.features.size} features",
            style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .semantics { contentDescription = "${uiState.features.size} features available" },
        )

        Spacer(Modifier.height(4.dp))

        // ── Grid or empty state ───────────────────────────────────────────
        if (uiState.features.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "No features match your search." },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "No features found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                            view.announceForAccessibility(msg)
                            viewModel.onToggleFavorite(feature)
                        },
                        onTogglePin = {
                            val msg = if (feature.isPinned) "${feature.name} unpinned from Home"
                                      else "${feature.name} pinned to Home"
                            view.announceForAccessibility(msg)
                            viewModel.onTogglePin(feature)
                        },
                    )
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
