package com.nexuswavetech.nexusplus.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexuswavetech.nexusplus.auth.WelcomeScreen
import com.nexuswavetech.nexusplus.features.hub.AIHubScreen
import com.nexuswavetech.nexusplus.features.hub.DocumentsHubScreen
import com.nexuswavetech.nexusplus.features.hub.MediaHubScreen
import com.nexuswavetech.nexusplus.features.hub.SecurityHubScreen
import com.nexuswavetech.nexusplus.features.hub.UtilitiesHubScreen
import com.nexuswavetech.nexusplus.features.radio.RadioPlayerScreen
import com.nexuswavetech.nexusplus.features.pdfsuite.PdfSuiteScreen
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
import com.nexuswavetech.nexusplus.features.settings.SettingsScreen
import com.nexuswavetech.nexusplus.features.profile.ProfileScreen
import com.nexuswavetech.nexusplus.features.notifications.NotificationCenterScreen
import com.nexuswavetech.nexusplus.features.flashlight.FlashlightScreen
import com.nexuswavetech.nexusplus.features.stopwatch.StopwatchScreen
import com.nexuswavetech.nexusplus.features.worldclock.WorldClockScreen
import com.nexuswavetech.nexusplus.features.units.UnitConverterScreen
import com.nexuswavetech.nexusplus.features.currency.CurrencyConverterScreen
import com.nexuswavetech.nexusplus.features.battery.BatteryMonitorScreen
import com.nexuswavetech.nexusplus.features.storage.StorageAnalyzerScreen
import com.nexuswavetech.nexusplus.features.compass.CompassScreen
import com.nexuswavetech.nexusplus.features.wifi.WifiAnalyzerScreen
import com.nexuswavetech.nexusplus.features.voicerecorder.VoiceRecorderScreen
import com.nexuswavetech.nexusplus.features.clipboard.ClipboardManagerScreen
import com.nexuswavetech.nexusplus.features.filemanager.FileManagerScreen
import com.nexuswavetech.nexusplus.features.alarm.AlarmClockScreen
import com.nexuswavetech.nexusplus.features.barcode.BarcodeGeneratorScreen
import com.nexuswavetech.nexusplus.features.nexushealthvault.NexusHealthVaultScreen
import com.nexuswavetech.nexusplus.features.totp.TotpAuthenticatorScreen
import com.nexuswavetech.nexusplus.features.speedtest.NetworkSpeedTestScreen
import com.nexuswavetech.nexusplus.features.aira.AiraAiScreen
import com.nexuswavetech.nexusplus.features.imageviewer.NexusImageViewerScreen
import com.nexuswavetech.nexusplus.features.docreader.NexusDocumentReaderScreen
import com.nexuswavetech.nexusplus.legal.AboutUsScreen
import com.nexuswavetech.nexusplus.legal.PrivacyPolicyScreen
import com.nexuswavetech.nexusplus.legal.TermsConditionsScreen

private const val ANIM_DURATION     = 320
private const val ANIM_DURATION_OUT = 200
private const val SLIDE_FRACTION    = 6

@Composable
fun NexusNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Screen.Welcome.route,
        enterTransition  = {
            fadeIn(tween(ANIM_DURATION)) +
            slideInHorizontally(tween(ANIM_DURATION)) { it / SLIDE_FRACTION }
        },
        exitTransition   = {
            fadeOut(tween(ANIM_DURATION_OUT)) +
            slideOutHorizontally(tween(ANIM_DURATION)) { -it / SLIDE_FRACTION }
        },
        popEnterTransition = {
            fadeIn(tween(ANIM_DURATION)) +
            slideInHorizontally(tween(ANIM_DURATION)) { -it / SLIDE_FRACTION }
        },
        popExitTransition  = {
            fadeOut(tween(ANIM_DURATION_OUT)) +
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
            MainScaffold(rootNavController = navController)
        }

        // ── Hub destinations ──────────────────────────────────────────────
        composable(Screen.SecurityHub.route)  { SecurityHubScreen (onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }) }
        composable(Screen.DocumentsHub.route) { DocumentsHubScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }) }
        composable(Screen.AIHub.route)        { AIHubScreen       (onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }) }
        composable(Screen.MediaHub.route)     { MediaHubScreen    (onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }) }
        composable(Screen.UtilitiesHub.route) { UtilitiesHubScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }) }

        // ── Global screens ────────────────────────────────────────────────
        composable(Screen.Settings.route)           { SettingsScreen          (onBack = { navController.popBackStack() }) }
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
                },
            )
        }
        composable(Screen.NotificationCenter.route) { NotificationCenterScreen (onBack = { navController.popBackStack() }) }

        // ── Media ─────────────────────────────────────────────────────────
        composable(Screen.RadioPlayer.route)      { RadioPlayerScreen     (onBack = { navController.popBackStack() }) }
        composable(Screen.AiImageGenerator.route) { AiImageGeneratorScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.NexusTts.route)         { NexusTtsScreen        (onBack = { navController.popBackStack() }) }
        composable(Screen.IptvPlayer.route)       { IptvPlayerScreen      (onBack = { navController.popBackStack() }) }
        composable(Screen.MusicStreaming.route)   { MusicStreamingScreen  (onBack = { navController.popBackStack() }) }
        composable(Screen.SmartImageEditor.route) { SmartImageEditorScreen(onBack = { navController.popBackStack() }) }

        // ── Documents ─────────────────────────────────────────────────────
        composable(Screen.PdfSuite.route)  { PdfSuiteScreen (onBack = { navController.popBackStack() }) }
        composable(Screen.DocHub.route)    { DocHubScreen   (onBack = { navController.popBackStack() }) }

        // ── Security ──────────────────────────────────────────────────────
        composable(Screen.EncrypterDecrypter.route) { EncrypterDecrypterScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.TextEncryptor.route)      { EncrypterDecrypterScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.HashGenerator.route)      { HashGeneratorScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.PasswordGenerator.route)  { PasswordGeneratorScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Base64Tool.route)         { Base64ToolScreen       (onBack = { navController.popBackStack() }) }
        composable(Screen.BiometricVault.route)     { BiometricVaultScreen   (onBack = { navController.popBackStack() }) }

        // ── Core Utilities ────────────────────────────────────────────────
        composable(Screen.TextTranslator.route)   { TextTranslatorScreen  (onBack = { navController.popBackStack() }) }
        composable(Screen.MorseCode.route)        { MorseCodeScreen       (onBack = { navController.popBackStack() }) }
        composable(Screen.NumberSystem.route)     { NumberSystemScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.JsonFormatter.route)    { JsonFormatterScreen   (onBack = { navController.popBackStack() }) }
        composable(Screen.RegexTester.route)      { RegexTesterScreen     (onBack = { navController.popBackStack() }) }
        composable(Screen.CalculatorCenter.route) { CalculatorCenterScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.VoiceTyper.route)       { VoiceTyperScreen      (onBack = { navController.popBackStack() }) }
        composable(Screen.MyReminder.route)       { MyReminderScreen      (onBack = { navController.popBackStack() }) }
        composable(Screen.QrCode.route)           { QrCodeScreen          (onBack = { navController.popBackStack() }) }

        // ── New Utility Implementations ───────────────────────────────────
        composable(Screen.Flashlight.route)        { FlashlightScreen       (onBack = { navController.popBackStack() }) }
        composable(Screen.Stopwatch.route)         { StopwatchScreen        (onBack = { navController.popBackStack() }) }
        composable(Screen.WorldClock.route)        { WorldClockScreen       (onBack = { navController.popBackStack() }) }
        composable(Screen.UnitConverter.route)     { UnitConverterScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.CurrencyConverter.route) { CurrencyConverterScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.BatteryMonitor.route)    { BatteryMonitorScreen   (onBack = { navController.popBackStack() }) }
        composable(Screen.StorageAnalyzer.route)   { StorageAnalyzerScreen  (onBack = { navController.popBackStack() }) }
        composable(Screen.Compass.route)           { CompassScreen          (onBack = { navController.popBackStack() }) }
        composable(Screen.WifiAnalyzer.route)      { WifiAnalyzerScreen     (onBack = { navController.popBackStack() }) }
        composable(Screen.VoiceRecorder.route)     { VoiceRecorderScreen    (onBack = { navController.popBackStack() }) }
        composable(Screen.ClipboardManager.route)  { ClipboardManagerScreen (onBack = { navController.popBackStack() }) }
        composable(Screen.FileManager.route) {
            FileManagerScreen(
                onBack             = { navController.popBackStack() },
                onOpenImageViewer  = { uri ->
                    navController.navigate(Screen.NexusImageViewer.route)
                },
                onOpenDocReader    = { uri ->
                    navController.navigate(Screen.NexusDocReader.route)
                }
            )
        }
        composable(Screen.AlarmClock.route)        { AlarmClockScreen       (onBack = { navController.popBackStack() }) }
        composable(Screen.BarcodeGenerator.route)  { BarcodeGeneratorScreen (onBack = { navController.popBackStack() }) }
        composable(Screen.Weather.route)           { StubFeatureScreen(featureKey = "weather", onBack = { navController.popBackStack() }) }

        // ── AI / Smart Tools ──────────────────────────────────────────────
        composable(Screen.ObjectDetector.route)  { ObjectDetectorScreen  (onBack = { navController.popBackStack() }) }
        composable(Screen.ColorDetector.route)   { ColorDetectorScreen   (onBack = { navController.popBackStack() }) }
        composable(Screen.AiraAi.route)          { AiraAiScreen          (onBack = { navController.popBackStack() }) }

        // ── Image Viewer ──────────────────────────────────────────────────
        composable(Screen.NexusImageViewer.route) {
            NexusImageViewerScreen(
                onBack       = { navController.popBackStack() },
                onOpenEditor = { navController.navigate(Screen.SmartImageEditor.route) }
            )
        }

        // ── Document Reader ───────────────────────────────────────────────
        composable(Screen.NexusDocReader.route) {
            NexusDocumentReaderScreen(onBack = { navController.popBackStack() })
        }

        // ── Forms ─────────────────────────────────────────────────────────

        // ── Health & Wellbeing ────────────────────────────────────────────
        composable(Screen.NexusHealthVault.route)   { NexusHealthVaultScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.TotpAuthenticator.route)  { TotpAuthenticatorScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.NetworkSpeedTest.route)   { NetworkSpeedTestScreen (onBack = { navController.popBackStack() }) }

        // ── Legal ─────────────────────────────────────────────────────────
        composable(Screen.AboutUs.route)         { AboutUsScreen        (onBack = { navController.popBackStack() }) }
        composable(Screen.PrivacyPolicy.route)   { PrivacyPolicyScreen  (onBack = { navController.popBackStack() }) }
        composable(Screen.TermsConditions.route) { TermsConditionsScreen(onBack = { navController.popBackStack() }) }

        // ── Stub catch-all ────────────────────────────────────────────────
        composable("${Screen.Stub.route}/{feature_key}") { backStack ->
            StubFeatureScreen(
                featureKey = backStack.arguments?.getString("feature_key") ?: "unknown",
                onBack     = { navController.popBackStack() },
            )
        }
    }
}
