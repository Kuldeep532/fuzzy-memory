package com.nexuswavetech.nexusplus.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
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
import com.nexuswavetech.nexusplus.features.home.HomeScreen
import com.nexuswavetech.nexusplus.features.more.MoreScreen
import com.nexuswavetech.nexusplus.features.search.SearchScreen

private data class NavTabItem(
    val tab: BottomTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDesc: String,
)

private val tabs = listOf(
    NavTabItem(
        tab           = BottomTab.Home,
        selectedIcon  = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        contentDesc   = "Home tab. Dashboard with hub cards and recent activity.",
    ),
    NavTabItem(
        tab           = BottomTab.Explore,
        selectedIcon  = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps,
        contentDesc   = "Explore tab. Browse all 49 features.",
    ),
    NavTabItem(
        tab           = BottomTab.Search,
        selectedIcon  = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        contentDesc   = "Search tab. Find any feature by name or keyword.",
    ),
    NavTabItem(
        tab           = BottomTab.More,
        selectedIcon  = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz,
        contentDesc   = "More tab. Settings, legal information, and developer links.",
    ),
)

@Composable
fun MainScaffold(rootNavController: NavController) {
    val tabNavController = rememberNavController()
    val navBackStack     by tabNavController.currentBackStackEntryAsState()
    val currentRoute     = navBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
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
        ) {
            composable(BottomTab.Home.route) {
                HomeScreen(rootNavController = rootNavController)
            }
            composable(BottomTab.Explore.route) {
                AllFeaturesScreen(rootNavController = rootNavController)
            }
            composable(BottomTab.Search.route) {
                SearchScreen(rootNavController = rootNavController)
            }
            composable(BottomTab.More.route) {
                MoreScreen(rootNavController = rootNavController)
            }
        }
    }
}
