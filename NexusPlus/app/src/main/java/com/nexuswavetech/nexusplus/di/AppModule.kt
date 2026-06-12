package com.nexuswavetech.nexusplus.di

import com.nexuswavetech.nexusplus.auth.AuthRepository
import com.nexuswavetech.nexusplus.auth.StubFirebaseAuthRepository
import com.nexuswavetech.nexusplus.auth.WelcomeViewModel
import com.nexuswavetech.nexusplus.core.FavoritesRepository
import com.nexuswavetech.nexusplus.core.SessionManager
import com.nexuswavetech.nexusplus.features.allfeatures.AllFeaturesViewModel
import com.nexuswavetech.nexusplus.features.imagegen.AiImageViewModel
import com.nexuswavetech.nexusplus.features.iptv.IptvViewModel
import com.nexuswavetech.nexusplus.features.music.MusicViewModel
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
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Singletons ───────────────────────────────────────────────────────────
    single<SessionManager>      { SessionManager() }
    single<FavoritesRepository> { FavoritesRepository(get()) }
    single<AuthRepository>      { StubFirebaseAuthRepository() }

    // ── ViewModels ───────────────────────────────────────────────────────────
    viewModel { WelcomeViewModel(authRepository = get(), sessionManager = get()) }
    viewModel { AllFeaturesViewModel(sessionManager = get(), favoritesRepository = get()) }

    // Original feature ViewModels
    viewModel { RadioViewModel() }
    viewModel { AiImageViewModel() }
    viewModel { IptvViewModel() }
    viewModel { MusicViewModel() }

    // Security ViewModels
    viewModel { EncrypterDecrypterViewModel() }
    viewModel { HashGeneratorViewModel() }
    viewModel { PasswordGeneratorViewModel() }

    // Utilities ViewModels
    viewModel { TextTranslatorViewModel() }
    viewModel { MorseCodeViewModel() }
    viewModel { NumberSystemViewModel() }
    viewModel { JsonFormatterViewModel() }
    viewModel { RegexTesterViewModel() }
    viewModel { MyReminderViewModel() }
    viewModel { QrCodeViewModel() }
    viewModel { CalculatorCenterViewModel() }

    // Camera/sensor features: ObjectDetector, ColorDetector, SmartImageEditor,
    // BiometricVault, DocHub, VoiceTyper — state managed locally in composables
}
