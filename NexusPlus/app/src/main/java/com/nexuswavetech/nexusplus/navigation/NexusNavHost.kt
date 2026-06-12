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
import com.nexuswavetech.nexusplus.features.encryptor.TextEncryptorScreen
import com.nexuswavetech.nexusplus.features.translator.TextTranslatorScreen
import com.nexuswavetech.nexusplus.features.objectdetector.ObjectDetectorScreen
import com.nexuswavetech.nexusplus.features.colordetector.ColorDetectorScreen
import com.nexuswavetech.nexusplus.features.imageeditor.SmartImageEditorScreen
import com.nexuswavetech.nexusplus.features.hashgen.HashGeneratorScreen
import com.nexuswavetech.nexusplus.features.passwordgen.PasswordGeneratorScreen
import com.nexuswavetech.nexusplus.features.base64tool.Base64ToolScreen
import com.nexuswavetech.nexusplus.features.morse.MorseCodeScreen
import com.nexuswavetech.nexusplus.features.numbersys.NumberSystemScreen
import com.nexuswavetech.nexusplus.features.jsontools.JsonFormatterScreen
import com.nexuswavetech.nexusplus.features.regextester.RegexTesterScreen
import com.nexuswavetech.nexusplus.features.stub.StubFeatureScreen
import com.nexuswavetech.nexusplus.legal.AboutUsScreen
import com.nexuswavetech.nexusplus.legal.PrivacyPolicyScreen
import com.nexuswavetech.nexusplus.legal.TermsConditionsScreen

@Composable
fun NexusNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Welcome.route) {

        composable(Screen.Welcome.route) {
            WelcomeScreen(onNavigateToMain = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Main.route) {
            MainScaffold(rootNavController = navController)
        }

        // ── Original features ────────────────────────────────────────────────
        composable(Screen.RadioPlayer.route)      { RadioPlayerScreen      (onBack = { navController.popBackStack() }) }
        composable(Screen.PdfReader.route)        { PdfReaderScreen        (onBack = { navController.popBackStack() }) }
        composable(Screen.AiImageGenerator.route) { AiImageGeneratorScreen (onBack = { navController.popBackStack() }) }
        composable(Screen.NexusTts.route)         { NexusTtsScreen         (onBack = { navController.popBackStack() }) }
        composable(Screen.IptvPlayer.route)       { IptvPlayerScreen       (onBack = { navController.popBackStack() }) }
        composable(Screen.MusicStreaming.route)   { MusicStreamingScreen   (onBack = { navController.popBackStack() }) }

        // ── New v1.1 features ────────────────────────────────────────────────
        composable(Screen.TextEncryptor.route)    { TextEncryptorScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.TextTranslator.route)   { TextTranslatorScreen   (onBack = { navController.popBackStack() }) }
        composable(Screen.ObjectDetector.route)   { ObjectDetectorScreen   (onBack = { navController.popBackStack() }) }
        composable(Screen.ColorDetector.route)    { ColorDetectorScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.SmartImageEditor.route) { SmartImageEditorScreen (onBack = { navController.popBackStack() }) }
        composable(Screen.HashGenerator.route)    { HashGeneratorScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.PasswordGenerator.route){ PasswordGeneratorScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Base64Tool.route)       { Base64ToolScreen       (onBack = { navController.popBackStack() }) }
        composable(Screen.MorseCode.route)        { MorseCodeScreen        (onBack = { navController.popBackStack() }) }
        composable(Screen.NumberSystem.route)     { NumberSystemScreen     (onBack = { navController.popBackStack() }) }
        composable(Screen.JsonFormatter.route)    { JsonFormatterScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.RegexTester.route)      { RegexTesterScreen      (onBack = { navController.popBackStack() }) }

        // ── Legal ────────────────────────────────────────────────────────────
        composable(Screen.AboutUs.route)         { AboutUsScreen         (onBack = { navController.popBackStack() }) }
        composable(Screen.PrivacyPolicy.route)   { PrivacyPolicyScreen   (onBack = { navController.popBackStack() }) }
        composable(Screen.TermsConditions.route) { TermsConditionsScreen (onBack = { navController.popBackStack() }) }

        // ── Stub catch-all ───────────────────────────────────────────────────
        composable("${Screen.Stub.route}/{feature_key}") { backStack ->
            StubFeatureScreen(
                featureKey = backStack.arguments?.getString("feature_key") ?: "unknown",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
