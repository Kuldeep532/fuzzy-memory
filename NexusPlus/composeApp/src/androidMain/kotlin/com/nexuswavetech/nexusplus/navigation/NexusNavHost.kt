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
import com.nexuswavetech.nexusplus.features.dialer.NexusDialerScreen
import com.nexuswavetech.nexusplus.features.textanalyzer.NexusTextAnalyzerScreen
import com.nexuswavetech.nexusplus.features.weather.WeatherScreen
import com.nexuswavetech.nexusplus.news.NewsScreen
import com.nexuswavetech.nexusplus.science.ScienceScreen
import com.nexuswavetech.nexusplus.features.urlshortener.UrlShortenerScreen
import com.nexuswavetech.nexusplus.ads.NexusAdScaffold
import com.nexuswavetech.nexusplus.legal.AboutUsScreen
import com.nexuswavetech.nexusplus.legal.PrivacyPolicyScreen
import com.nexuswavetech.nexusplus.legal.TermsConditionsScreen
import com.nexuswavetech.nexusplus.features.formx.AutoUniversalFormX
import com.nexuswavetech.nexusplus.features.games.NexusGamesScreen
import com.nexuswavetech.nexusplus.features.voices.DownloadVoicesScreen
import com.nexuswavetech.nexusplus.features.ott.NexusOttScreen
import com.nexuswavetech.nexusplus.features.ott.NexusOttPlayerScreen
import com.nexuswavetech.nexusplus.features.ott.ottCatalogueById

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
        composable(Screen.Settings.route)           { SettingsScreen(onBack = { navController.popBackStack() }, onDownloadVoices = { navController.navigate(Screen.DownloadVoices.route) }) }
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
        // NexusAdScaffold provides a sticky bottom banner on every feature screen.
        // Screens that already have internal banner ads (Aira, Radio, IPTV, Weather)
        // are wrapped too — the bottom banner is compact (50dp) and won't double-up
        // since those screens' banners are placed inside the scrollable content.
        composable(Screen.RadioPlayer.route)      { NexusAdScaffold { RadioPlayerScreen     (onBack = { navController.popBackStack() }) } }
        composable(Screen.AiImageGenerator.route) { NexusAdScaffold { AiImageGeneratorScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.NexusTts.route)         { NexusAdScaffold { NexusTtsScreen        (onBack = { navController.popBackStack() }) } }
        composable(Screen.IptvPlayer.route)       { NexusAdScaffold { IptvPlayerScreen      (onBack = { navController.popBackStack() }) } }
        composable(Screen.MusicStreaming.route)   { NexusAdScaffold { MusicStreamingScreen  (onBack = { navController.popBackStack() }) } }
        composable(Screen.SmartImageEditor.route) { NexusAdScaffold { SmartImageEditorScreen(onBack = { navController.popBackStack() }) } }

        // ── Documents ─────────────────────────────────────────────────────
        composable(Screen.PdfSuite.route)  { NexusAdScaffold { PdfSuiteScreen (onBack = { navController.popBackStack() }) } }
        composable(Screen.DocHub.route)    { NexusAdScaffold { DocHubScreen   (onBack = { navController.popBackStack() }) } }

        // ── Security ──────────────────────────────────────────────────────
        composable(Screen.EncrypterDecrypter.route) { NexusAdScaffold { EncrypterDecrypterScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.TextEncryptor.route)      { NexusAdScaffold { EncrypterDecrypterScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.HashGenerator.route)      { NexusAdScaffold { HashGeneratorScreen    (onBack = { navController.popBackStack() }) } }
        composable(Screen.PasswordGenerator.route)  { NexusAdScaffold { PasswordGeneratorScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.Base64Tool.route)         { NexusAdScaffold { Base64ToolScreen       (onBack = { navController.popBackStack() }) } }
        composable(Screen.BiometricVault.route)     { NexusAdScaffold { BiometricVaultScreen   (onBack = { navController.popBackStack() }) } }

        // ── Core Utilities ────────────────────────────────────────────────
        composable(Screen.TextTranslator.route)   { NexusAdScaffold { TextTranslatorScreen  (onBack = { navController.popBackStack() }) } }
        composable(Screen.MorseCode.route)        { NexusAdScaffold { MorseCodeScreen       (onBack = { navController.popBackStack() }) } }
        composable(Screen.NumberSystem.route)     { NexusAdScaffold { NumberSystemScreen    (onBack = { navController.popBackStack() }) } }
        composable(Screen.JsonFormatter.route)    { NexusAdScaffold { JsonFormatterScreen   (onBack = { navController.popBackStack() }) } }
        composable(Screen.RegexTester.route)      { NexusAdScaffold { RegexTesterScreen     (onBack = { navController.popBackStack() }) } }
        composable(Screen.CalculatorCenter.route) { NexusAdScaffold { CalculatorCenterScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.VoiceTyper.route)       { NexusAdScaffold { VoiceTyperScreen      (onBack = { navController.popBackStack() }) } }
        composable(Screen.MyReminder.route)       { NexusAdScaffold { MyReminderScreen      (onBack = { navController.popBackStack() }) } }
        composable(Screen.QrCode.route)           { NexusAdScaffold { QrCodeScreen          (onBack = { navController.popBackStack() }) } }

        // ── New Utility Implementations ───────────────────────────────────
        composable(Screen.Flashlight.route)        { NexusAdScaffold { FlashlightScreen       (onBack = { navController.popBackStack() }) } }
        composable(Screen.Stopwatch.route)         { NexusAdScaffold { StopwatchScreen        (onBack = { navController.popBackStack() }) } }
        composable(Screen.WorldClock.route)        { NexusAdScaffold { WorldClockScreen       (onBack = { navController.popBackStack() }) } }
        composable(Screen.UnitConverter.route)     { NexusAdScaffold { UnitConverterScreen    (onBack = { navController.popBackStack() }) } }
        composable(Screen.CurrencyConverter.route) { NexusAdScaffold { CurrencyConverterScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.BatteryMonitor.route)    { NexusAdScaffold { BatteryMonitorScreen   (onBack = { navController.popBackStack() }) } }
        composable(Screen.StorageAnalyzer.route)   { NexusAdScaffold { StorageAnalyzerScreen  (onBack = { navController.popBackStack() }) } }
        composable(Screen.Compass.route)           { NexusAdScaffold { CompassScreen          (onBack = { navController.popBackStack() }) } }
        composable(Screen.WifiAnalyzer.route)      { NexusAdScaffold { WifiAnalyzerScreen     (onBack = { navController.popBackStack() }) } }
        composable(Screen.VoiceRecorder.route)     { NexusAdScaffold { VoiceRecorderScreen    (onBack = { navController.popBackStack() }) } }
        composable(Screen.ClipboardManager.route)  { NexusAdScaffold { ClipboardManagerScreen (onBack = { navController.popBackStack() }) } }
        composable(Screen.FileManager.route) {
            NexusAdScaffold {
                FileManagerScreen(
                    onBack            = { navController.popBackStack() },
                    onOpenImageViewer = { navController.navigate(Screen.NexusImageViewer.route) },
                    onOpenDocReader   = { navController.navigate(Screen.NexusDocReader.route) }
                )
            }
        }
        composable(Screen.AlarmClock.route)        { NexusAdScaffold { AlarmClockScreen       (onBack = { navController.popBackStack() }) } }
        composable(Screen.BarcodeGenerator.route)  { NexusAdScaffold { BarcodeGeneratorScreen (onBack = { navController.popBackStack() }) } }
        composable(Screen.Weather.route)           { NexusAdScaffold { WeatherScreen      (onBack = { navController.popBackStack() }) } }
        composable(Screen.NexusDialer.route)       { NexusAdScaffold { NexusDialerScreen       (onBack = { navController.popBackStack() }) } }
        composable(Screen.TextAnalyzer.route)      { NexusAdScaffold { NexusTextAnalyzerScreen (onBack = { navController.popBackStack() }) } }
        composable(Screen.UrlShortener.route)      { NexusAdScaffold { UrlShortenerScreen      (onBack = { navController.popBackStack() }) } }

        // ── AI / Smart Tools ──────────────────────────────────────────────
        composable(Screen.ObjectDetector.route)  { NexusAdScaffold { ObjectDetectorScreen  (onBack = { navController.popBackStack() }) } }
        composable(Screen.ColorDetector.route)   { NexusAdScaffold { ColorDetectorScreen   (onBack = { navController.popBackStack() }) } }
        composable(Screen.AiraAi.route)          { NexusAdScaffold { AiraAiScreen          (onBack = { navController.popBackStack() }) } }

        // ── Image Viewer ──────────────────────────────────────────────────
        composable(Screen.NexusImageViewer.route) {
            NexusAdScaffold {
                NexusImageViewerScreen(
                    onBack       = { navController.popBackStack() },
                    onOpenEditor = { navController.navigate(Screen.SmartImageEditor.route) }
                )
            }
        }

        // ── Document Reader ───────────────────────────────────────────────
        composable(Screen.NexusDocReader.route) {
            NexusAdScaffold { NexusDocumentReaderScreen(onBack = { navController.popBackStack() }) }
        }

        // ── Health & Wellbeing ────────────────────────────────────────────
        composable(Screen.NexusHealthVault.route)   { NexusAdScaffold { NexusHealthVaultScreen  (onBack = { navController.popBackStack() }) } }
        composable(Screen.TotpAuthenticator.route)  { NexusAdScaffold { TotpAuthenticatorScreen (onBack = { navController.popBackStack() }) } }
        composable(Screen.NetworkSpeedTest.route)   { NexusAdScaffold { NetworkSpeedTestScreen  (onBack = { navController.popBackStack() }) } }

        // ── Legal ─────────────────────────────────────────────────────────
        composable(Screen.AboutUs.route)         { AboutUsScreen        (onBack = { navController.popBackStack() }) }
        composable(Screen.PrivacyPolicy.route)   { PrivacyPolicyScreen  (onBack = { navController.popBackStack() }) }
        composable(Screen.TermsConditions.route) { TermsConditionsScreen(onBack = { navController.popBackStack() }) }

        // ── News & Science (commonMain screens) ──────────────────────────────────
        composable(Screen.News.route)    { NexusAdScaffold { NewsScreen   (onBack = { navController.popBackStack() }) } }
        composable(Screen.Science.route) { NexusAdScaffold { ScienceScreen(onBack = { navController.popBackStack() }) } }

        // ── Form X ────────────────────────────────────────────────────────────
        composable(Screen.FormX.route) { NexusAdScaffold { AutoUniversalFormX(onBack = { navController.popBackStack() }) } }

        // ── Nexus Games Hub ───────────────────────────────────────────────────
        composable(Screen.NexusGames.route) { NexusGamesScreen(onBack = { navController.popBackStack() }) }

        // ── Nexus OTT ─────────────────────────────────────────────────────────
        composable(Screen.NexusOtt.route) {
            NexusAdScaffold {
                NexusOttScreen(
                    onBack      = { navController.popBackStack() },
                    onPlayVideo = { item ->
                        navController.navigate(Screen.NexusOttPlayer.route(item.id))
                    },
                )
            }
        }
        composable(
            route     = Screen.NexusOttPlayer.route,
            arguments = listOf(androidx.navigation.navArgument("itemId") {
                type = androidx.navigation.NavType.StringType
            }),
        ) { backStack ->
            val itemId = backStack.arguments?.getString("itemId") ?: ""
            val item   = ottCatalogueById(itemId)
            if (item != null) {
                NexusOttPlayerScreen(item = item, onBack = { navController.popBackStack() })
            } else {
                navController.popBackStack()
            }
        }

        // ── Download Voices ───────────────────────────────────────────────────
        composable(Screen.DownloadVoices.route) { DownloadVoicesScreen(onBack = { navController.popBackStack() }) }

        // ── Stub catch-all ────────────────────────────────────────────────
        composable("${Screen.Stub.route}/{feature_key}") { backStack ->
            StubFeatureScreen(
                featureKey = backStack.arguments?.getString("feature_key") ?: "unknown",
                onBack     = { navController.popBackStack() },
            )
        }
    }
}
