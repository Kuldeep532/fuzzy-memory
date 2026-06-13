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
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.features.allfeatures.AllFeaturesViewModel
import com.nexuswavetech.nexusplus.features.biometricvault.BiometricVaultRepository
import com.nexuswavetech.nexusplus.features.biometricvault.BiometricVaultViewModel
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
import com.nexuswavetech.nexusplus.features.tts.NseAndroidEngine
import com.nexuswavetech.nexusplus.features.tts.NseAudioFocusManager
import com.nexuswavetech.nexusplus.features.tts.NseRepository
import com.nexuswavetech.nexusplus.features.tts.NseViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Core singletons ───────────────────────────────────────────────────
    single<SessionManager>           { SessionManager() }
    single<FavoritesRepository>      { FavoritesRepository(androidContext()) }
    single<AdminRepository>          { AdminRepository() }
    single<AuthRepository>           { FirebaseAuthRepository(get()) }
    single { HealthVaultRepository(androidContext()) }
    single<SearchManager>            { SearchManager() }
    single<RecentActivityRepository> { RecentActivityRepository(androidContext()) }
    single<SettingsRepository>       { SettingsRepository(androidContext()) }
    single<NotificationRepository>   { NotificationRepository(androidContext()) }
    single<BiometricVaultRepository> { BiometricVaultRepository(androidContext()) }

    // ConsentRepository — process-scoped, DataStore-backed legal consent gate.
    single { ConsentRepository(androidContext()) }

    // ── NSE 2.0 — Nexus Speech Engine ─────────────────────────────────────
    factory { NseAudioFocusManager(androidContext()) }
    factory { NseAndroidEngine(androidContext(), get()) }
    factory { NseRepository(get()) }

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

    // Security
    viewModel { EncrypterDecrypterViewModel() }
    viewModel { HashGeneratorViewModel() }
    viewModel { PasswordGeneratorViewModel() }
    viewModel { BiometricVaultViewModel(repository = get()) }

    // Utilities
    viewModel { TextTranslatorViewModel() }
    viewModel { MorseCodeViewModel() }
    viewModel { NumberSystemViewModel() }
    viewModel { JsonFormatterViewModel() }
    viewModel { RegexTesterViewModel() }
    viewModel { MyReminderViewModel() }
    viewModel { QrCodeViewModel() }
    viewModel { CalculatorCenterViewModel() }

    // Health Vault
    viewModel { HealthVaultViewModel(repository = get()) }

    // NSE — factory-scoped engine + repository wired through Koin
    viewModel { NseViewModel(get()) }

    // Camera / sensor features: ObjectDetector, ColorDetector, SmartImageEditor,
    // DocHub, VoiceTyper — state managed locally in composables.
}
