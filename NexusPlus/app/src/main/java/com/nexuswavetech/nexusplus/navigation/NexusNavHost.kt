package com.nexuswavetech.nexusplus.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexuswavetech.nexusplus.auth.WelcomeScreen
import com.nexuswavetech.nexusplus.features.radio.RadioPlayerScreen
import com.nexuswavetech.nexusplus.features.pdfsuite.PdfSuiteScreen
import com.nexuswavetech.nexusplus.features.pdf.PdfReaderScreen
import com.nexuswavetech.nexusplus.features.imagegen.AiImageGeneratorScreen
import com.nexuswavetech.nexusplus.features.tts.NexusTtsScreen
import com.nexuswavetech.nexusplus.features.iptv.IptvPlayerScreen
import com.nexuswavetech.nexusplus.features.music.MusicStreamingScreen
import com.nexuswavetech.nexusplus.features.encryptor.EncrypterDecrypterScreen
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
import com.nexuswavetech.nexusplus.features.biometricvault.BiometricVaultScreen
import com.nexuswavetech.nexusplus.features.reminder.MyReminderScreen
import com.nexuswavetech.nexusplus.features.qrcode.QrCodeScreen
import com.nexuswavetech.nexusplus.features.calculator.CalculatorCenterScreen
import com.nexuswavetech.nexusplus.features.dochub.DocHubScreen
import com.nexuswavetech.nexusplus.features.voicetyper.VoiceTyperScreen
import com.nexuswavetech.nexusplus.features.formx.AutoUniversalFormX
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

        // ── Media ────────────────────────────────────────────────────────────
        composable(Screen.RadioPlayer.route)      { RadioPlayerScreen      (onBack = { navController.popBackStack() }) }
        composable(Screen.AiImageGenerator.route) { AiImageGeneratorScreen (onBack = { navController.popBackStack() }) }
        composable(Screen.NexusTts.route)         { NexusTtsScreen         (onBack = { navController.popBackStack() }) }
        composable(Screen.IptvPlayer.route)       { IptvPlayerScreen       (onBack = { navController.popBackStack() }) }
        composable(Screen.MusicStreaming.route)   { MusicStreamingScreen   (onBack = { navController.popBackStack() }) }
        composable(Screen.SmartImageEditor.route) { SmartImageEditorScreen (onBack = { navController.popBackStack() }) }

        // ── PDF Suite (new) + legacy PdfReader route ─────────────────────────
        composable(Screen.PdfSuite.route)         { PdfSuiteScreen         (onBack = { navController.popBackStack() }) }
        composable(Screen.PdfReader.route)        { PdfReaderScreen        (onBack = { navController.popBackStack() }) }

        // ── Security ─────────────────────────────────────────────────────────
        composable(Screen.EncrypterDecrypter.route) { EncrypterDecrypterScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.TextEncryptor.route)    { EncrypterDecrypterScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.HashGenerator.route)    { HashGeneratorScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.PasswordGenerator.route){ PasswordGeneratorScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Base64Tool.route)       { Base64ToolScreen       (onBack = { navController.popBackStack() }) }
        composable(Screen.BiometricVault.route)   { BiometricVaultScreen   (onBack = { navController.popBackStack() }) }

        // ── Utilities ────────────────────────────────────────────────────────
        composable(Screen.TextTranslator.route)   { TextTranslatorScreen   (onBack = { navController.popBackStack() }) }
        composable(Screen.MorseCode.route)        { MorseCodeScreen        (onBack = { navController.popBackStack() }) }
        composable(Screen.NumberSystem.route)     { NumberSystemScreen     (onBack = { navController.popBackStack() }) }
        composable(Screen.JsonFormatter.route)    { JsonFormatterScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.RegexTester.route)      { RegexTesterScreen      (onBack = { navController.popBackStack() }) }
        composable(Screen.CalculatorCenter.route) { CalculatorCenterScreen (onBack = { navController.popBackStack() }) }
        composable(Screen.VoiceTyper.route)       { VoiceTyperScreen       (onBack = { navController.popBackStack() }) }
        composable(Screen.MyReminder.route)       { MyReminderScreen       (onBack = { navController.popBackStack() }) }

        // ── Smart Tools ───────────────────────────────────────────────────────
        composable(Screen.ObjectDetector.route)   { ObjectDetectorScreen   (onBack = { navController.popBackStack() }) }
        composable(Screen.ColorDetector.route)    { ColorDetectorScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.QrCode.route)           { QrCodeScreen           (onBack = { navController.popBackStack() }) }
        composable(Screen.DocHub.route)           { DocHubScreen           (onBack = { navController.popBackStack() }) }

        // ── Forms ────────────────────────────────────────────────────────────
        composable(Screen.FormX.route) { AutoUniversalFormX(onBack = { navController.popBackStack() }) }

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
