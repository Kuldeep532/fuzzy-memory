package com.nexuswavetech.nexusplus.di

import com.nexuswavetech.nexusplus.auth.AuthRepository
import com.nexuswavetech.nexusplus.auth.AuthResult
import com.nexuswavetech.nexusplus.auth.ConsentRepository
import com.nexuswavetech.nexusplus.auth.WelcomeViewModel
import com.nexuswavetech.nexusplus.core.FavoritesRepository
import com.nexuswavetech.nexusplus.core.RecentActivityRepository
import com.nexuswavetech.nexusplus.core.SearchManager
import com.nexuswavetech.nexusplus.core.SessionManager
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.features.allfeatures.AllFeaturesViewModel
import com.nexuswavetech.nexusplus.features.tts.NseRepository
import com.nexuswavetech.nexusplus.features.tts.NseViewModel
import com.nexuswavetech.nexusplus.features.weather.WeatherRepository
import com.nexuswavetech.nexusplus.features.weather.WeatherService
import com.nexuswavetech.nexusplus.features.weather.WeatherViewModel
import org.koin.dsl.module

/**
 * CommonMain Koin module — platform-agnostic dependencies.
 * Android adds platform bindings via appModule in androidMain.
 */
val commonModule = module {

    // ── Core singletons ──────────────────────────────────────────────────────
    single<SessionManager>           { SessionManager() }
    single<SettingsRepository>       { SettingsRepository(get()) }
    single<FavoritesRepository>      { FavoritesRepository(get()) }
    single<RecentActivityRepository> { RecentActivityRepository(get()) }
    single<ConsentRepository>        { ConsentRepository(get()) }
    single<SearchManager>            { SearchManager() }

    // ── Services ────────────────────────────────────────────────────────
    single { WeatherService() }
    single { WeatherRepository(get()) }

    // ── ViewModels (factory scope) ────────────────────────────────────────────────
    factory { WelcomeViewModel(authRepository = get(), sessionManager = get(), consentRepository = get()) }
    factory { AllFeaturesViewModel(sessionManager = get(), favoritesRepository = get()) }
    factory { NseViewModel(repository = get(), settings = get()) }
    factory { WeatherViewModel(service = get(), repository = get()) }

    // ── Auth (placeholder for non-Android) ─────────────────────────────────────────
    // Android overrides with FirebaseAuthRepository in appModule
    single<AuthRepository> { GuestAuthRepository() }
}

/** Simple guest-only auth for non-Android platforms. */
class GuestAuthRepository : AuthRepository {
    override suspend fun signInWithGoogle(idToken: String): AuthResult =
        AuthResult.Success("guest", "Guest User", "", null)
    override suspend fun signOut() {}
    override suspend fun isSignedIn(): Boolean = false
    override suspend fun getCurrentUserId(): String? = null
    override suspend fun getCurrentUserEmail(): String? = null
    override suspend fun getCurrentUserDisplayName(): String? = null
    override suspend fun getCurrentUserPhotoUrl(): String? = null
}
