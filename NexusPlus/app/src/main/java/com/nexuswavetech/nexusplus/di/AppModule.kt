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
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Central Koin DI module for Nexus Plus.
 *
 * Structure:
 *  - Singletons: SessionManager, FavoritesRepository, AuthRepository
 *  - ViewModels: one per screen that needs state
 *
 * To add a new feature ViewModel:
 *  1. Create your ViewModel class
 *  2. Add `viewModel { MyFeatureViewModel(...) }` here
 *  3. Call `koinViewModel<MyFeatureViewModel>()` in your composable
 */
val appModule = module {

    // ── Singletons ──────────────────────────────────────────────────────────

    single<SessionManager> { SessionManager() }

    single<FavoritesRepository> { FavoritesRepository(get()) }

    single<AuthRepository> { StubFirebaseAuthRepository() }
    // When Firebase is ready, swap the line above with:
    // single<AuthRepository> { FirebaseAuthRepositoryImpl() }

    // ── ViewModels ──────────────────────────────────────────────────────────

    viewModel {
        WelcomeViewModel(
            authRepository = get(),
            sessionManager = get()
        )
    }

    viewModel {
        AllFeaturesViewModel(
            sessionManager = get(),
            favoritesRepository = get()
        )
    }

    viewModel { RadioViewModel() }

    viewModel { AiImageViewModel() }

    viewModel { IptvViewModel() }

    viewModel { MusicViewModel() }

    // Extended features (7–30+): add viewModels here as each is implemented.
    // Example:
    // viewModel { CurrencyConverterViewModel(get()) }
    // viewModel { WeatherViewModel(get()) }
    // viewModel { EncryptedNotesViewModel(get()) }
}
