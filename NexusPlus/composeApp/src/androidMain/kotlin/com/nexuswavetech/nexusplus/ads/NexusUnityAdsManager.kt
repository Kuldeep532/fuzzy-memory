package com.nexuswavetech.nexusplus.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexuswavetech.nexusplus.billing.PremiumRepository
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private const val TAG = "NexusUnityAds"

/** Reads UNITY_GAME_ID from BuildConfig (injected via GitHub Secret / env var). */
object NexusUnityAdsManager {

    val GAME_ID: String = runCatching {
        Class.forName("com.nexuswavetech.nexusplus.BuildConfig")
            .getField("UNITY_GAME_ID")
            .get(null) as? String
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: ""

    private val isTestMode: Boolean = GAME_ID.isBlank() || GAME_ID == "YOUR_GAME_ID"

    /** Call once from Application.onCreate(). Safe to call multiple times. */
    fun initialize(context: Context) {
        if (GAME_ID.isBlank()) {
            Log.w(TAG, "UNITY_GAME_ID not set — Unity Ads disabled.")
            return
        }
        if (UnityAds.isInitialized) return
        UnityAds.initialize(context, GAME_ID, isTestMode, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.i(TAG, "Unity Ads initialized successfully.")
            }
            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError, message: String) {
                Log.e(TAG, "Unity Ads init failed: $error — $message")
            }
        })
    }
}

// ── Banner Ad (50 dp) — replaces NexusBannerAd ──────────────────────────────

@Composable
fun NexusBannerAd(
    modifier: Modifier = Modifier,
    placementId: String = "Banner_Android",
) {
    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(false) }

    // Graceful degradation: if Unity Game ID not set, show nothing.
    if (NexusUnityAdsManager.GAME_ID.isBlank()) return

    AnimatedVisibility(
        visible = isLoaded,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        AndroidView(
            factory = {
                BannerView(it, placementId, UnityBannerSize(320, 50)).apply {
                    listener = object : BannerView.IListener {
                        override fun onBannerLoaded(bannerAdView: BannerView?) {
                            isLoaded = true
                        }
                        override fun onBannerShown(bannerAdView: BannerView?) {}
                        override fun onBannerClick(bannerAdView: BannerView?) {}
                        override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo) {
                            Log.w(TAG, "Banner load failed: ${errorInfo.errorMessage}")
                            isLoaded = false
                        }
                        override fun onBannerLeftApplication(bannerAdView: BannerView?) {}
                    }
                    load()
                }
            },
            modifier = modifier.fillMaxWidth().height(50.dp),
        )
    }
}

// ── Interstitial Manager ─────────────────────────────────────────────────────

object NexusInterstitialManager {
    private var isLoaded = false
    private const val PLACEMENT = "Interstitial_Android"

    fun load() {
        if (NexusUnityAdsManager.GAME_ID.isBlank()) return
        UnityAds.load(PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                isLoaded = true
                Log.d(TAG, "Interstitial loaded: $placementId")
            }
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                isLoaded = false
                Log.w(TAG, "Interstitial load failed: $message")
            }
        })
    }

    /** Show interstitial if loaded. Always attempts to reload after showing. */
    fun show(activity: Activity) {
        if (!isLoaded || NexusUnityAdsManager.GAME_ID.isBlank()) {
            load(); return
        }
        UnityAds.show(activity, PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                Log.w(TAG, "Interstitial show failed: $message")
                isLoaded = false; load()
            }
            override fun onUnityAdsShowStart(placementId: String) {}
            override fun onUnityAdsShowClick(placementId: String) {}
            override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                isLoaded = false; load()
            }
        })
    }
}

// ── Rewarded Ad Manager ──────────────────────────────────────────────────────

object NexusRewardedManager {
    private var isLoaded = false
    private const val PLACEMENT = "Rewarded_Android"

    fun load() {
        if (NexusUnityAdsManager.GAME_ID.isBlank()) return
        UnityAds.load(PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                isLoaded = true
                Log.d(TAG, "Rewarded loaded: $placementId")
            }
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                isLoaded = false
                Log.w(TAG, "Rewarded load failed: $message")
            }
        })
    }

    /** Show rewarded ad. Calls onRewarded() only if user fully watches the ad. */
    fun show(activity: Activity, onRewarded: () -> Unit, onFailed: (() -> Unit)? = null) {
        if (!isLoaded || NexusUnityAdsManager.GAME_ID.isBlank()) {
            onFailed?.invoke(); load(); return
        }
        UnityAds.show(activity, PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                Log.w(TAG, "Rewarded show failed: $message")
                isLoaded = false; onFailed?.invoke(); load()
            }
            override fun onUnityAdsShowStart(placementId: String) {}
            override fun onUnityAdsShowClick(placementId: String) {}
            override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                isLoaded = false
                if (state == UnityAds.UnityAdsShowCompletionState.SKIPPED) {
                    onFailed?.invoke()   // User skipped = no reward
                } else {
                    onRewarded()          // Watched fully = reward granted
                }
                load()
            }
        })
    }
}

// ── Ad Frequency Controller ──────────────────────────────────────────────────

object NexusAdController {
    private var sessionScreenViews = 0
    fun onScreenView(): Boolean {
        sessionScreenViews++
        return sessionScreenViews % 6 == 0
    }
    fun resetSession() { sessionScreenViews = 0 }
}

// ── NexusAdScaffold (Universal Wrapper) ─────────────────────────────────────
// Wrap ANY feature screen with this ↔ bottom banner ad + premium gate.
// Premium users see NO ads. Free users see banner on every screen.

@Composable
fun NexusAdScaffold(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val premiumRepo: PremiumRepository = koinInject()
    val isPremium by premiumRepo.isPremiumFlow.collectAsState(initial = false)

    if (isPremium) {
        Box(modifier = modifier.fillMaxSize()) { content() }
        return
    }

    // Show interstitial every 6th screen navigation (free users only)
    LaunchedEffect(Unit) {
        if (NexusAdController.onScreenView()) {
            val activity = context as? android.app.Activity
            activity?.let { NexusInterstitialManager.show(it) }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) { content() }
        NexusBannerAd(modifier = Modifier.fillMaxWidth())
    }
}
