package com.nexuswavetech.nexusplus.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexuswavetech.nexusplus.features.allfeatures.AllFeaturesScreen
import com.nexuswavetech.nexusplus.features.favorites.FavoritesScreen
import com.nexuswavetech.nexusplus.features.home.HomeScreen
import com.nexuswavetech.nexusplus.features.more.MoreScreen

private data class NavTabItem(
    val tab: BottomTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDesc: String,
)

private val tabs = listOf(
    NavTabItem(
        tab            = BottomTab.Home,
        selectedIcon   = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        contentDesc    = "Home tab. Dashboard with hub cards and recent activity.",
    ),
    NavTabItem(
        tab            = BottomTab.Explore,
        selectedIcon   = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps,
        contentDesc    = "Explore tab. Browse all features by category.",
    ),
    NavTabItem(
        tab            = BottomTab.Favorites,
        selectedIcon   = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        contentDesc    = "Favorites tab. Your bookmarked and pinned features.",
    ),
    NavTabItem(
        tab            = BottomTab.More,
        selectedIcon   = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz,
        contentDesc    = "More tab. Profile, settings, and app information.",
    ),
)

@Composable
fun MainScaffold(rootNavController: NavController) {
    val tabNavController = rememberNavController()
    val navBackStack     by tabNavController.currentBackStackEntryAsState()
    val currentRoute     = navBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                tabs.forEach { item ->
                    val selected = currentRoute == item.tab.route
                    NavigationBarItem(
                        selected  = selected,
                        onClick   = {
                            if (currentRoute != item.tab.route) {
                                tabNavController.navigate(item.tab.route) {
                                    popUpTo(BottomTab.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        },
                        icon      = {
                            Icon(
                                imageVector        = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = null,
                            )
                        },
                        label     = { Text(item.tab.label) },
                        colors    = NavigationBarItemDefaults.colors(
                            selectedIconColor      = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor      = MaterialTheme.colorScheme.primary,
                            indicatorColor         = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor    = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor    = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier  = Modifier.semantics {
                            contentDescription = item.contentDesc +
                                if (selected) " Currently selected." else " Double tap to switch."
                        },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = tabNavController,
            startDestination = BottomTab.Home.route,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn(tween(220)) },
            exitTransition   = { fadeOut(tween(150)) },
            popEnterTransition  = { fadeIn(tween(220)) },
            popExitTransition   = { fadeOut(tween(150)) },
        ) {
            composable(BottomTab.Home.route) {
                HomeScreen(rootNavController = rootNavController)
            }
            composable(BottomTab.Explore.route) {
                AllFeaturesScreen(rootNavController = rootNavController)
            }
            composable(BottomTab.Favorites.route) {
                FavoritesScreen(rootNavController = rootNavController)
            }
            composable(BottomTab.More.route) {
                MoreScreen(rootNavController = rootNavController)
            }
        }
    }
}
