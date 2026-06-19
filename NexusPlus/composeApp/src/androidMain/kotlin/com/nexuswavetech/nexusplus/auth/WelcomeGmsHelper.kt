package com.nexuswavetech.nexusplus.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

internal object WelcomeGmsHelper {

    interface Impl {
        fun handleSignInResult(data: Intent?, viewModel: WelcomeViewModel)
        fun launchSignIn(
            context: Context,
            webClientId: String,
            viewModel: WelcomeViewModel,
            launcher: ActivityResultLauncher<Intent>,
        )
    }

    @Volatile
    var impl: Impl? = try {
        @Suppress("UNCHECKED_CAST")
        val cls = Class.forName("com.nexuswavetech.nexusplus.auth.WelcomeGmsHelperImpl")
        cls.getDeclaredConstructor().newInstance() as Impl
    } catch (_: Exception) {
        null
    }

    fun handleSignInResult(data: Intent?, viewModel: WelcomeViewModel) {
        impl?.handleSignInResult(data, viewModel)
            ?: viewModel.onGoogleSignInError("Google Sign-In library not available")
    }

    fun launchSignIn(
        context: Context,
        webClientId: String,
        viewModel: WelcomeViewModel,
        launcher: ActivityResultLauncher<Intent>,
    ) {
        impl?.launchSignIn(context, webClientId, viewModel, launcher)
            ?: viewModel.onGoogleSignInError("Google Sign-In library not available")
    }
}
