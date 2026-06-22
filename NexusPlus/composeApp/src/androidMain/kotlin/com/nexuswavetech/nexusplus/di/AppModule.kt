package com.nexuswavetech.nexusplus.di

import com.nexuswavetech.nexusplus.auth.AuthRepository
import com.nexuswavetech.nexusplus.auth.ConsentRepository
import com.nexuswavetech.nexusplus.auth.FirebaseAuthRepository
import com.nexuswavetech.nexusplus.auth.WelcomeViewModel
import com.nexuswavetech.nexusplus.core.AdminRepository
import com.nexuswavetech.nexusplus.features.nexushealthvault.HealthVaultRepository
import com.nexuswavetech.nexusplus.features.nexushealthvault.HealthVaultViewModel
import com.nexuswavetech.nexusplus.core.FavoritesRepository
import com.nexuswavetech.nexusplus.core.RecentActivityRepository
import com.nexuswavetech.nexusplus.core.SearchManager
import com.nexuswavetech.nexusplus.core.SessionManager
import com.nexuswavetech.nexusplus.core.HapticHelper
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.features.allfeatures.AllFeaturesViewModel
import com.nexuswavetech.nexusplus.features.biometricvault.BiometricVaultRepository
import com.nexuswavetech.nexusplus.features.biometricvault.BiometricVaultViewModel
import com.nexuswavetech.nexusplus.features.aira.AiraViewModel
import com.nexuswavetech.nexusplus.features.imagegen.AiImageViewModel
import com.nexuswavetech.nexusplus.features.iptv.IptvViewModel
import com.nexuswavetech.nexusplus.features.music.MusicViewModel
import com.nexuswavetech.nexusplus.features.notifications.NotificationRepository
import com.nexuswavetech.nexusplus.features.radio.RadioViewModel
import com.nexuswavetech.nexusplus.features.encryptor.EncrypterDecrypterViewModel
import com.nexuswavetech.nexusplus.features.translator.TextTranslatorViewModel
import com.nexuswavetech.nexusplus.features.hashgen.HashGeneratorViewModel
import com.nexuswavetech.nexusplus.features.passwordgen.PasswordGeneratorViewModel
import com.nexuswavetech.nexusplus.features.morse.MorseCodeViewModel
import com.nexuswavetech.nexusplus.features.numbersys.NumberSystemViewModel
import com.nexuswavetech.nexusplus.features.jsontools.JsonFormatterViewModel
import com.nexuswavetech.nexusplus.features.regextester.RegexTesterViewModel
import com.nexuswavetech.nexusplus.features.reminder.MyReminderViewModel
import com.nexuswavetech.nexusplus.features.qrcode.QrCodeViewModel
import com.nexuswavetech.nexusplus.features.calculator.CalculatorCenterViewModel
import com.nexuswavetech.nexusplus.features.tts.NseEngine
import com.nexuswavetech.nexusplus.features.tts.NseAudioFocusManager
import com.nexuswavetech.nexusplus.features.tts.NsePcmCache
import com.nexuswavetech.nexusplus.features.tts.NsePipelineAndroidEngine
import com.nexuswavetech.nexusplus.features.tts.NseRepository
import com.nexuswavetech.nexusplus.features.tts.NseViewModel
import com.nexuswavetech.nexusplus.features.weather.WeatherService
import com.nexuswavetech.nexusplus.features.weather.WeatherRepository
import com.nexuswavetech.nexusplus.features.weather.WeatherViewModel
import com.nexuswavetech.nexusplus.news.NewsService
import com.nexuswavetech.nexusplus.news.NewsViewModel
import com.nexuswavetech.nexusplus.science.ScienceService
import com.nexuswavetech.nexusplus.science.ScienceViewModel
import com.nexuswavetech.nexusplus.ai.GeminiRepository
import com.nexuswavetech.nexusplus.model.ModelDownloadManager
import com.nexuswavetech.nexusplus.model.FirstLaunchManager
import com.nexuswavetech.nexusplus.platform.SettingsStore
import com.nexuswavetech.nexusplus.remoteconfig.RemoteConfigRepository
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Platform layer ──────────────────────────────────────────────────
    single<SettingsStore> { SettingsStore(androidContext()) }

    // ── Remote Config ─────────────────────────────────────────────────────
    single { RemoteConfigRepository() }

    // ── AI layer ───────────────────────────────────────────────────────────
    single { GeminiRepository(get()) }

    // ── Model management ──────────────────────────────────────────────────
    single { ModelDownloadManager(androidContext()) }
    single { FirstLaunchManager(androidContext(), get()) }

    // ── Core singletons ───────────────────────────────────────────────────
    single<SessionManager>           { SessionManager() }
    single<SettingsRepository>       { SettingsRepository(get()) }
    single<FavoritesRepository>      { FavoritesRepository(get()) }
    single<RecentActivityRepository> { RecentActivityRepository(get()) }
    single<ConsentRepository>        { ConsentRepository(get()) }
    single<AdminRepository>          { AdminRepository() }
    single<AuthRepository>           { FirebaseAuthRepository(get()) }

    // ── Services (KMP, commonMain-backed) ──────────────────────────────
    single { WeatherService() }
    single { WeatherRepository(get()) }
    single { NewsService() }
    single { ScienceService() }
    single { HealthVaultRepository(androidContext()) }
    single<SearchManager>            { SearchManager() }
    single<NotificationRepository>   { NotificationRepository(androidContext()) }
    single<BiometricVaultRepository>   { BiometricVaultRepository(androidContext()) }
    single<com.nexuswavetech.nexusplus.features.encryptednotes.EncryptedNotesRepository> {
        com.nexuswavetech.nexusplus.features.encryptednotes.EncryptedNotesRepository(androidContext())
    }
    single { com.nexuswavetech.nexusplus.features.emergencyguardian.EmergencyGuardianRepository(androidContext()) }
    single { HapticHelper(get()) }
    single<com.nexuswavetech.nexusplus.platform.PlatformToast> { com.nexuswavetech.nexusplus.platform.PlatformToast(androidContext()) }
    single<com.nexuswavetech.nexusplus.platform.PlatformOcr> { com.nexuswavetech.nexusplus.platform.PlatformOcr(androidContext()) }
    single<com.nexuswavetech.nexusplus.platform.PlatformUrlHandler> { com.nexuswavetech.nexusplus.platform.PlatformUrlHandler(androidContext()) }

    // ── NSE 4.0 — Nexus Auto Speech Engine (Pipeline) ──────────────────────
    single  { NsePcmCache() }
    factory { NseAudioFocusManager(androidContext()) }
    factory<NseEngine> { NsePipelineAndroidEngine(androidContext(), get(), get(), get<ModelDownloadManager>().modelsDir) }
    factory { NseRepository(get<NseEngine>()) }

    // ── ViewModels ────────────────────────────────────────────────────────
    viewModel {
        WelcomeViewModel(
            authRepository    = get(),
            sessionManager    = get(),
            consentRepository = get(),
        )
    }

    viewModel { AllFeaturesViewModel(sessionManager = get(), favoritesRepository = get()) }

    // Media
    viewModel { RadioViewModel() }
    viewModel { AiImageViewModel() }
    viewModel { IptvViewModel() }
    viewModel { MusicViewModel() }
    viewModel { AiraViewModel(settingsRepo = get(), geminiRepo = get()) }

    // Security
    viewModel { EncrypterDecrypterViewModel() }
    viewModel { HashGeneratorViewModel() }
    viewModel { PasswordGeneratorViewModel() }
    viewModel {
        BiometricVaultViewModel(
            repository         = get(),
            settingsRepository = get(),
        )
    }
    viewModel { com.nexuswavetech.nexusplus.features.encryptednotes.EncryptedNotesViewModel(repository = get()) }
    viewModel { com.nexuswavetech.nexusplus.features.emergencyguardian.EmergencyGuardianViewModel(repository = get()) }

    // Utilities
    viewModel { TextTranslatorViewModel() }
    viewModel { MorseCodeViewModel(settings = get()) }
    viewModel { NumberSystemViewModel() }
    viewModel { JsonFormatterViewModel() }
    viewModel { RegexTesterViewModel() }
    viewModel { MyReminderViewModel() }
    viewModel { QrCodeViewModel() }
    viewModel { CalculatorCenterViewModel() }

    // Health Vault
    viewModel { HealthVaultViewModel(repository = get()) }

    // NSE — factory-scoped engine + repository wired through Koin
    viewModel { NseViewModel(repository = get(), settings = get()) }

    // New KMP commonMain screens (Priority 5-7)
    viewModel { WeatherViewModel(service = get(), repository = get()) }
    viewModel { NewsViewModel(service = get()) }
    viewModel { ScienceViewModel(service = get()) }

    // Camera / sensor features: ObjectDetector, ColorDetector, SmartImageEditor,
    // DocHub, VoiceTyper — state managed locally in composables.
}
