package com.nexuswavetech.nexusplus.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexuswavetech.nexusplus.auth.WelcomeScreen
import com.nexuswavetech.nexusplus.features.allfeatures.AllFeaturesScreen
import com.nexuswavetech.nexusplus.features.base64tool.Base64ToolScreen
import com.nexuswavetech.nexusplus.features.favorites.FavoritesScreen
import com.nexuswavetech.nexusplus.features.home.HomeScreen
import com.nexuswavetech.nexusplus.features.hub.AIHubScreen
import com.nexuswavetech.nexusplus.features.hub.DocumentsHubScreen
import com.nexuswavetech.nexusplus.features.hub.MediaHubScreen
import com.nexuswavetech.nexusplus.features.hub.SecurityHubScreen
import com.nexuswavetech.nexusplus.features.hub.UtilitiesHubScreen
import com.nexuswavetech.nexusplus.features.more.MoreScreen
import com.nexuswavetech.nexusplus.features.notifications.NotificationCenterScreen
import com.nexuswavetech.nexusplus.features.numbersys.NumberSystemScreen
import com.nexuswavetech.nexusplus.features.passwordgen.PasswordGeneratorScreen
import com.nexuswavetech.nexusplus.features.profile.ProfileScreen
import com.nexuswavetech.nexusplus.features.regextester.RegexTesterScreen
import com.nexuswavetech.nexusplus.features.settings.SettingsScreen
import com.nexuswavetech.nexusplus.features.stopwatch.StopwatchScreen
import com.nexuswavetech.nexusplus.features.stub.StubFeatureScreen
import com.nexuswavetech.nexusplus.features.units.UnitConverterScreen
import com.nexuswavetech.nexusplus.features.weather.WeatherScreen
import com.nexuswavetech.nexusplus.ios.features.emergencyguardian.IosEmergencyGuardianScreen

private const val ANIM_DURATION = 300
private const val SLIDE_FRACTION = 6

private data class IosNavTabItem(
    val tab: BottomTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDesc: String,
)

private val iosTabs = listOf(
    IosNavTabItem(BottomTab.Home, Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    IosNavTabItem(BottomTab.Explore, Icons.Filled.Apps, Icons.Outlined.Apps, "Explore"),
    IosNavTabItem(BottomTab.Favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "Favorites"),
    IosNavTabItem(BottomTab.More, Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, "More"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusIosNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route,
        enterTransition = {
            fadeIn(tween(ANIM_DURATION)) +
                    slideInHorizontally(tween(ANIM_DURATION)) { it / SLIDE_FRACTION }
        },
        exitTransition = {
            fadeOut(tween(ANIM_DURATION)) +
                    slideOutHorizontally(tween(ANIM_DURATION)) { -it / SLIDE_FRACTION }
        },
        popEnterTransition = {
            fadeIn(tween(ANIM_DURATION)) +
                    slideInHorizontally(tween(ANIM_DURATION)) { -it / SLIDE_FRACTION }
        },
        popExitTransition = {
            fadeOut(tween(ANIM_DURATION)) +
                    slideOutHorizontally(tween(ANIM_DURATION)) { it / SLIDE_FRACTION }
        },
    ) {

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            IosMainScaffold(rootNavController = navController)
        }

        // ── Hubs (in commonMain) ───────────────────────────────────────────────
        composable(Screen.SecurityHub.route) {
            SecurityHubScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) })
        }
        composable(Screen.DocumentsHub.route) {
            DocumentsHubScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) })
        }
        composable(Screen.AIHub.route) {
            AIHubScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) })
        }
        composable(Screen.MediaHub.route) {
            MediaHubScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) })
        }
        composable(Screen.UtilitiesHub.route) {
            UtilitiesHubScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) })
        }

        // ── Global screens ─────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSignIn = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.NotificationCenter.route) {
            NotificationCenterScreen(onBack = { navController.popBackStack() })
        }

        // ── Features available cross-platform ─────────────────────────────────
        composable(Screen.Stopwatch.route) {
            StopwatchScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.UnitConverter.route) {
            UnitConverterScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.NumberSystem.route) {
            NumberSystemScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.RegexTester.route) {
            RegexTesterScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Base64Tool.route) {
            Base64ToolScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PasswordGenerator.route) {
            PasswordGeneratorScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Weather.route) {
            WeatherScreen(onBack = { navController.popBackStack() })
        }

        // ── Emergency Guardian — iOS native implementation ─────────────────────
        composable(Screen.EmergencyGuardian.route) {
            IosEmergencyGuardianScreen(onBack = { navController.popBackStack() })
        }

        // ── Features not yet available on iOS (platform-specific stub) ─────────
        val androidOnlyRoutes = listOf(
            Screen.AiImageGenerator,
            Screen.MusicStreaming, Screen.SmartImageEditor, Screen.NexusImageViewer,
            Screen.PdfSuite, Screen.DocHub, Screen.NexusDocReader,
            Screen.EncrypterDecrypter, Screen.HashGenerator,
            Screen.BiometricVault, Screen.TextTranslator, Screen.MorseCode,
            Screen.JsonFormatter, Screen.CalculatorCenter, Screen.VoiceTyper,
            Screen.MyReminder, Screen.QrCode, Screen.Flashlight,
            Screen.WorldClock, Screen.CurrencyConverter, Screen.BatteryMonitor,
            Screen.StorageAnalyzer, Screen.Compass, Screen.WifiAnalyzer,
            Screen.VoiceRecorder, Screen.ClipboardManager, Screen.FileManager,
            Screen.AlarmClock, Screen.BarcodeGenerator, Screen.ObjectDetector,
            Screen.ColorDetector, Screen.AiraAi, Screen.NexusHealthVault,
            Screen.TotpAuthenticator, Screen.NetworkSpeedTest, Screen.NexusDialer,
            Screen.TextAnalyzer, Screen.UrlShortener, Screen.NexusGames,
            Screen.ExpenseTracker, Screen.EncryptedNotes,
            Screen.TaskManager, Screen.ContactBackup,
            Screen.NexusTts, Screen.SmartDocumentScanner,
            Screen.VideoDescription, Screen.QrCodeScanner,
        )
        androidOnlyRoutes.forEach { screen ->
            composable(screen.route) {
                StubFeatureScreen(
                    featureKey = screen.route.removePrefix("feature/"),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IosMainScaffold(rootNavController: NavController) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                iosTabs.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.tab.route,
                        onClick = {
                            tabNavController.navigate(item.tab.route) {
                                popUpTo(tabNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == item.tab.route) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.contentDesc,
                            )
                        },
                        label = { Text(item.tab.label) },
                        modifier = Modifier.semantics { contentDescription = item.contentDesc }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = BottomTab.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
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
