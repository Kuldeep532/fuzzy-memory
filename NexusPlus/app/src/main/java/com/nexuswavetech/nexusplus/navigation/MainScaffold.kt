package com.nexuswavetech.nexusplus.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexuswavetech.nexusplus.features.allfeatures.AllFeaturesScreen
import com.nexuswavetech.nexusplus.features.favorites.FavoritesScreen
import com.nexuswavetech.nexusplus.features.more.MoreScreen

private data class NavTabItem(
    val tab: BottomTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String
)

private val tabs = listOf(
    NavTabItem(
        tab = BottomTab.Favorites,
        label = "Favorites",
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.StarBorder,
        contentDescription = "Favorites tab. Shows your bookmarked features."
    ),
    NavTabItem(
        tab = BottomTab.AllFeatures,
        label = "All Features",
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps,
        contentDescription = "All Features tab. Browse all 30 available features."
    ),
    NavTabItem(
        tab = BottomTab.More,
        label = "More",
        selectedIcon = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz,
        contentDescription = "More tab. Settings, legal information, and developer links."
    )
)

@Composable
fun MainScaffold(rootNavController: NavController) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                tabs.forEach { item ->
                    val selected = currentRoute == item.tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != item.tab.route) {
                                tabNavController.navigate(item.tab.route) {
                                    popUpTo(BottomTab.Favorites.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = null
                            )
                        },
                        label = { Text(item.label) },
                        modifier = Modifier.semantics {
                            contentDescription = item.contentDescription +
                                    if (selected) " Currently selected." else " Double tap to switch."
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = BottomTab.AllFeatures.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomTab.Favorites.route) {
                FavoritesScreen(rootNavController = rootNavController)
            }
            composable(BottomTab.AllFeatures.route) {
                AllFeaturesScreen(rootNavController = rootNavController)
            }
            composable(BottomTab.More.route) {
                MoreScreen(rootNavController = rootNavController)
            }
        }
    }
}
