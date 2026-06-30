package com.nexuswavetech.nexusplus.navigation

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.core.content.FileProvider
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexuswavetech.nexusplus.core.AppConfig
import com.nexuswavetech.nexusplus.features.social.SocialMediaScreen
import com.nexuswavetech.nexusplus.platform.PlatformUrlHandler
import com.nexuswavetech.nexusplus.remoteconfig.RemoteConfigRepository
import org.koin.compose.koinInject
import com.nexuswavetech.nexusplus.auth.WelcomeScreen
import com.nexuswavetech.nexusplus.features.hub.AIHubScreen
import com.nexuswavetech.nexusplus.features.hub.DocumentsHubScreen
import com.nexuswavetech.nexusplus.features.hub.MediaHubScreen
import com.nexuswavetech.nexusplus.features.hub.SecurityHubScreen
import com.nexuswavetech.nexusplus.features.hub.UtilitiesHubScreen
import com.nexuswavetech.nexusplus.features.pdfsuite.PdfSuiteScreen
import com.nexuswavetech.nexusplus.features.imagegen.AiImageGeneratorScreen
import com.nexuswavetech.nexusplus.features.tts.NexusTtsScreen
import com.nexuswavetech.nexusplus.features.music.FullMediaPlayerScreen
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
import com.nexuswavetech.nexusplus.features.videodesc.VideoDescriptionScreen
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
import com.nexuswavetech.nexusplus.features.docscanner.SmartDocumentScannerScreen
import com.nexuswavetech.nexusplus.features.weather.WeatherScreen
import com.nexuswavetech.nexusplus.news.NewsScreen
import com.nexuswavetech.nexusplus.science.ScienceScreen
import com.nexuswavetech.nexusplus.features.urlshortener.UrlShortenerScreen
import com.nexuswavetech.nexusplus.ads.NexusAdScaffold
import com.nexuswavetech.nexusplus.legal.AboutUsScreen
import com.nexuswavetech.nexusplus.legal.PrivacyPolicyScreen
import com.nexuswavetech.nexusplus.legal.TermsConditionsScreen
import com.nexuswavetech.nexusplus.features.games.NexusGamesScreen
import com.nexuswavetech.nexusplus.features.expense.NexusExpenseTrackerScreen
import com.nexuswavetech.nexusplus.features.voices.DownloadVoicesScreen
import com.nexuswavetech.nexusplus.features.encryptednotes.EncryptedNotesScreen
import com.nexuswavetech.nexusplus.features.speedometer.SpeedometerScreen
import com.nexuswavetech.nexusplus.features.taskmanager.TaskManagerScreen
import com.nexuswavetech.nexusplus.features.emergencyguardian.EmergencyGuardianScreen
import com.nexuswavetech.nexusplus.features.texttopdf.TextToPdfScreen
import com.nexuswavetech.nexusplus.features.journal.DailyJournalScreen
import com.nexuswavetech.nexusplus.features.colorpalette.ColorPaletteScreen
import com.nexuswavetech.nexusplus.features.contactbackup.ContactBackupScreen
import com.nexuswavetech.nexusplus.features.appinfo.AppInfoCenterScreen
import com.nexuswavetech.nexusplus.features.networkinfo.NetworkInfoScreen
import com.nexuswavetech.nexusplus.billing.PaymentScreen

private const val ANIM_DURATION     = 300
private const val ANIM_DURATION_OUT = 180
private const val SLIDE_FRACTION    = 8

@Composable
fun NexusNavHost(currentVersionCode: Int = 0) {
    val navController = rememberNavController()

    val remoteConfig: RemoteConfigRepository = koinInject()
    val urlHandler: PlatformUrlHandler       = koinInject()

    // ── Update dialog ──────────────────────────────────────────────────────
    var showUpdateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (remoteConfig.updateDialogEnabled) {
            val minVersion = remoteConfig.updateMinVersion
            if (minVersion <= 0L || currentVersionCode < minVersion) {
                showUpdateDialog = true
            }
        }
    }
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = {},
            title   = { Text(remoteConfig.updateDialogTitle) },
            text    = { Text(remoteConfig.updateDialogMessage) },
            confirmButton = {
                TextButton(onClick = { urlHandler.openUrl(remoteConfig.updateDialogUrl) }) { Text("Update Now") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Later") }
            },
        )
    }

    // Shared URI state for deep-link screens
    var pendingViewerUri    by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDocReaderUri by rememberSaveable { mutableStateOf<String?>(null) }

    // ── LOGIN GATE ─────────────────────────────────────────────────────────
    // AppConfig.LOGIN_REQUIRED = false  → skip Welcome, go straight to Main
    // AppConfig.LOGIN_REQUIRED = true   → show Welcome/login screen first
    // Change ONE constant in AppConfig.kt to re-enable login for all features.
    val startDestination = if (AppConfig.LOGIN_REQUIRED) Screen.Welcome.route else Screen.Main.route

    NavHost(
        navController    = navController,
        startDestination = startDestination,
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

        // ── Welcome / Login ───────────────────────────────────────────────
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
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack           = { navController.popBackStack() },
                onDownloadVoices = { navController.navigate(Screen.DownloadVoices.route) },
                onSubscription   = { navController.navigate(Screen.Subscription.route) },
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack    = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSignIn  = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Screen.NotificationCenter.route) { NotificationCenterScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Subscription.route) { PaymentScreen(onBack = { navController.popBackStack() }) }

        // ── Media ─────────────────────────────────────────────────────────
        composable(Screen.AiImageGenerator.route) { NexusAdScaffold { AiImageGeneratorScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.NexusTts.route)         { NexusAdScaffold { NexusTtsScreen        (onBack = { navController.popBackStack() }) } }
        composable(Screen.MusicStreaming.route)   { NexusAdScaffold { FullMediaPlayerScreen (onBack = { navController.popBackStack() }) } }
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
        composable(Screen.Flashlight.route)       { NexusAdScaffold { FlashlightScreen      (onBack = { navController.popBackStack() }) } }
        composable(Screen.Stopwatch.route)        { NexusAdScaffold { StopwatchScreen       (onBack = { navController.popBackStack() }) } }
        composable(Screen.WorldClock.route)       { NexusAdScaffold { WorldClockScreen      (onBack = { navController.popBackStack() }) } }
        composable(Screen.UnitConverter.route)    { NexusAdScaffold { UnitConverterScreen   (onBack = { navController.popBackStack() }) } }
        composable(Screen.CurrencyConverter.route){ NexusAdScaffold { CurrencyConverterScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.BatteryMonitor.route)   { NexusAdScaffold { BatteryMonitorScreen  (onBack = { navController.popBackStack() }) } }
        composable(Screen.StorageAnalyzer.route)  { NexusAdScaffold { StorageAnalyzerScreen (onBack = { navController.popBackStack() }) } }
        composable(Screen.Compass.route)          { NexusAdScaffold { CompassScreen         (onBack = { navController.popBackStack() }) } }
        composable(Screen.WifiAnalyzer.route)     { NexusAdScaffold { WifiAnalyzerScreen    (onBack = { navController.popBackStack() }) } }
        composable(Screen.VoiceRecorder.route)    { NexusAdScaffold { VoiceRecorderScreen   (onBack = { navController.popBackStack() }) } }
        composable(Screen.ClipboardManager.route) { NexusAdScaffold { ClipboardManagerScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.FileManager.route) {
            NexusAdScaffold {
                FileManagerScreen(
                    onBack            = { navController.popBackStack() },
                    onOpenImageViewer = { uri ->
                        pendingViewerUri = uri.toString()
                        navController.navigate(Screen.NexusImageViewer.route)
                    },
                    onOpenDocReader   = { uri ->
                        pendingDocReaderUri = uri.toString()
                        navController.navigate(Screen.NexusDocReader.route)
                    },
                )
            }
        }
        composable(Screen.AlarmClock.route)       { NexusAdScaffold { AlarmClockScreen      (onBack = { navController.popBackStack() }) } }
        composable(Screen.BarcodeGenerator.route) { NexusAdScaffold { BarcodeGeneratorScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.Weather.route)          { NexusAdScaffold { WeatherScreen         (onBack = { navController.popBackStack() }) } }
        composable(Screen.NexusDialer.route)      { NexusAdScaffold { NexusDialerScreen     (onBack = { navController.popBackStack() }) } }
        composable(Screen.TextAnalyzer.route)     { NexusAdScaffold { NexusTextAnalyzerScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.UrlShortener.route)     { NexusAdScaffold { UrlShortenerScreen    (onBack = { navController.popBackStack() }) } }

        // ── AI / Smart Tools ──────────────────────────────────────────────
        composable(Screen.ObjectDetector.route)  { NexusAdScaffold { ObjectDetectorScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.ColorDetector.route)   { NexusAdScaffold { ColorDetectorScreen (onBack = { navController.popBackStack() }) } }
        composable(Screen.AiraAi.route)          { NexusAdScaffold { AiraAiScreen        (onBack = { navController.popBackStack() }) } }

        // ── Image Viewer ──────────────────────────────────────────────────
        composable(Screen.NexusImageViewer.route) {
            val uri = pendingViewerUri?.let { Uri.parse(it) }
            NexusAdScaffold {
                NexusImageViewerScreen(
                    initialUri   = uri,
                    onBack       = { pendingViewerUri = null; navController.popBackStack() },
                    onOpenEditor = { navController.navigate(Screen.SmartImageEditor.route) },
                )
            }
        }

        // ── Document Reader ───────────────────────────────────────────────
        composable(Screen.NexusDocReader.route) {
            val uri = pendingDocReaderUri?.let { Uri.parse(it) }
            NexusAdScaffold {
                NexusDocumentReaderScreen(
                    initialUri = uri,
                    onBack     = { pendingDocReaderUri = null; navController.popBackStack() },
                )
            }
        }

        // ── Health & Security ─────────────────────────────────────────────
        composable(Screen.NexusHealthVault.route)  { NexusAdScaffold { NexusHealthVaultScreen (onBack = { navController.popBackStack() }) } }
        composable(Screen.TotpAuthenticator.route) { NexusAdScaffold { TotpAuthenticatorScreen(onBack = { navController.popBackStack() }) } }
        composable(Screen.NetworkSpeedTest.route)  { NexusAdScaffold { NetworkSpeedTestScreen (onBack = { navController.popBackStack() }) } }
        composable(Screen.EncryptedNotes.route)    { NexusAdScaffold { EncryptedNotesScreen   (onBack = { navController.popBackStack() }) } }
        composable(Screen.Speedometer.route)       { NexusAdScaffold { SpeedometerScreen      (onBack = { navController.popBackStack() }) } }
        composable(Screen.TaskManager.route)       { NexusAdScaffold { TaskManagerScreen      (onBack = { navController.popBackStack() }) } }
        composable(Screen.EmergencyGuardian.route) { NexusAdScaffold { EmergencyGuardianScreen(onBack = { navController.popBackStack() }) } }

        // ── New features ──────────────────────────────────────────────────
        composable(Screen.TextToPdf.route) {
            val ctx = LocalContext.current
            NexusAdScaffold {
                TextToPdfScreen(
                    onBack      = { navController.popBackStack() },
                    onExportPdf = { docTitle, body, fontSizePt ->
                        runCatching {
                            val pdfFile = buildPdfFile(ctx, docTitle, body, fontSizePt)
                            val uri     = FileProvider.getUriForFile(
                                ctx,
                                "${ctx.packageName}.fileprovider",
                                pdfFile,
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type  = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(Intent.createChooser(intent, "Share PDF"))
                        }
                    },
                )
            }
        }
        composable(Screen.DailyJournal.route) {
            NexusAdScaffold { DailyJournalScreen(onBack = { navController.popBackStack() }) }
        }
        composable(Screen.ColorPalette.route) {
            val ctx = LocalContext.current
            NexusAdScaffold {
                ColorPaletteScreen(
                    onBack       = { navController.popBackStack() },
                    onCopyToClip = { hex ->
                        val cb = ctx.getSystemService(android.content.ClipboardManager::class.java)
                        cb?.setPrimaryClip(android.content.ClipData.newPlainText("Colour Hex", hex))
                    },
                )
            }
        }

        // ── Legal ─────────────────────────────────────────────────────────
        composable(Screen.AboutUs.route) {
            AboutUsScreen(
                onBack        = { navController.popBackStack() },
                onSocialMedia = { navController.navigate(Screen.SocialMedia.route) },
            )
        }
        composable(Screen.PrivacyPolicy.route)   { PrivacyPolicyScreen  (onBack = { navController.popBackStack() }) }
        composable(Screen.TermsConditions.route) { TermsConditionsScreen(onBack = { navController.popBackStack() }) }

        // ── News & Science ────────────────────────────────────────────────
        composable(Screen.News.route)    { NexusAdScaffold { NewsScreen   (onBack = { navController.popBackStack() }) } }
        composable(Screen.Science.route) { NexusAdScaffold { ScienceScreen(onBack = { navController.popBackStack() }) } }

        // ── Entertainment ─────────────────────────────────────────────────
        composable(Screen.NexusGames.route) { NexusGamesScreen(onBack = { navController.popBackStack() }) }

        // ── Finance ───────────────────────────────────────────────────────
        composable(Screen.ExpenseTracker.route) {
            NexusAdScaffold { NexusExpenseTrackerScreen(onBack = { navController.popBackStack() }) }
        }

        // ── Settings sub-screens ──────────────────────────────────────────
        composable(Screen.DownloadVoices.route) { DownloadVoicesScreen(onBack = { navController.popBackStack() }) }

        // ── Community ─────────────────────────────────────────────────────
        composable(Screen.SocialMedia.route) {
            NexusAdScaffold { SocialMediaScreen(onBack = { navController.popBackStack() }) }
        }

        // ── Contact Backup ────────────────────────────────────────────────
        composable(Screen.ContactBackup.route) {
            NexusAdScaffold { ContactBackupScreen(onBack = { navController.popBackStack() }) }
        }

        // ── Installed Apps & Network Info ─────────────────────────────────
        composable(Screen.AppInfoCenter.route) {
            NexusAdScaffold { AppInfoCenterScreen(onBack = { navController.popBackStack() }) }
        }
        composable(Screen.NetworkInfo.route) {
            NexusAdScaffold { NetworkInfoScreen(onBack = { navController.popBackStack() }) }
        }
        composable(Screen.SmartDocumentScanner.route) {
            NexusAdScaffold { SmartDocumentScannerScreen(onBack = { navController.popBackStack() }) }
        }

        // ── Stub catch-all ────────────────────────────────────────────────
        composable("${Screen.Stub.route}/{feature_key}") { backStack ->
            StubFeatureScreen(
                featureKey = backStack.arguments?.getString("feature_key") ?: "unknown",
                onBack     = { navController.popBackStack() },
            )
        }
    }
}

// ── PDF generation helper ────────────────────────────────────────────────────

private fun buildPdfFile(
    context    : Context,
    docTitle   : String,
    body       : String,
    fontSizePt : Float,
): java.io.File {
    val pageW     = 595          // A4 width  at 72 dpi
    val pageH     = 842          // A4 height at 72 dpi
    val margin    = 60f
    val lineGap   = fontSizePt * 1.5f
    val titleGap  = (fontSizePt + 8f) * 1.6f
    val textW     = pageW - 2 * margin

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontSizePt
        color    = android.graphics.Color.BLACK
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize       = fontSizePt + 8f
        isFakeBoldText = true
        color          = android.graphics.Color.BLACK
    }

    fun Paint.wrap(text: String): List<String> {
        val out = mutableListOf<String>()
        for (para in text.split('\n')) {
            if (para.isBlank()) { out.add(""); continue }
            var rem = para
            while (rem.isNotEmpty()) {
                val n = breakText(rem, true, textW, null).coerceAtLeast(1)
                out.add(rem.substring(0, n))
                rem = rem.substring(n)
            }
        }
        return out
    }

    val titleLines = if (docTitle.isNotBlank()) titlePaint.wrap(docTitle) else emptyList()
    val bodyLines  = bodyPaint.wrap(body)

    val pdf   = PdfDocument()
    var pageN = 1
    var y     = margin + if (titleLines.isNotEmpty()) titleGap else lineGap

    fun newPage(): Pair<PdfDocument.Page, android.graphics.Canvas> {
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageN++).create()
        val p    = pdf.startPage(info)
        return p to p.canvas
    }

    var (page, canvas) = newPage()

    for (line in titleLines) {
        if (y + titleGap > pageH - margin) {
            pdf.finishPage(page)
            val (p2, c2) = newPage(); page = p2; canvas = c2
            y = margin + titleGap
        }
        canvas.drawText(line, margin, y, titlePaint)
        y += titleGap
    }
    if (titleLines.isNotEmpty()) y += lineGap * 0.5f

    for (line in bodyLines) {
        if (y + lineGap > pageH - margin) {
            pdf.finishPage(page)
            val (p2, c2) = newPage(); page = p2; canvas = c2
            y = margin + lineGap
        }
        canvas.drawText(line, margin, y, bodyPaint)
        y += lineGap
    }
    pdf.finishPage(page)

    val dir  = java.io.File(context.cacheDir, "shared_pdfs").also { it.mkdirs() }
    val file = java.io.File(dir, "nexus_export.pdf")
    file.outputStream().use { pdf.writeTo(it) }
    pdf.close()
    return file
}
