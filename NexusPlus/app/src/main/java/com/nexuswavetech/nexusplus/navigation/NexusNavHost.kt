package com.nexuswavetech.nexusplus.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexuswavetech.nexusplus.auth.WelcomeScreen
import com.nexuswavetech.nexusplus.features.radio.RadioPlayerScreen
import com.nexuswavetech.nexusplus.features.pdf.PdfReaderScreen
import com.nexuswavetech.nexusplus.features.imagegen.AiImageGeneratorScreen
import com.nexuswavetech.nexusplus.features.tts.NexusTtsScreen
import com.nexuswavetech.nexusplus.features.iptv.IptvPlayerScreen
import com.nexuswavetech.nexusplus.features.music.MusicStreamingScreen
import com.nexuswavetech.nexusplus.legal.AboutUsScreen
import com.nexuswavetech.nexusplus.legal.PrivacyPolicyScreen
import com.nexuswavetech.nexusplus.legal.TermsConditionsScreen
import com.nexuswavetech.nexusplus.features.stub.StubFeatureScreen

@Composable
fun NexusNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
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
            MainScaffold(rootNavController = navController)
        }

        composable(Screen.RadioPlayer.route) {
            RadioPlayerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.PdfReader.route) {
            PdfReaderScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AiImageGenerator.route) {
            AiImageGeneratorScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.NexusTts.route) {
            NexusTtsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.IptvPlayer.route) {
            IptvPlayerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.MusicStreaming.route) {
            MusicStreamingScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AboutUs.route) {
            AboutUsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.TermsConditions.route) {
            TermsConditionsScreen(onBack = { navController.popBackStack() })
        }

        // Catch-all stub for extended features
        composable("${Screen.Stub.route}/{feature_key}") { backStackEntry ->
            val key = backStackEntry.arguments?.getString("feature_key") ?: "unknown"
            StubFeatureScreen(featureKey = key, onBack = { navController.popBackStack() })
        }
    }
}
