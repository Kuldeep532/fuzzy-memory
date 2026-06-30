package com.nexuswavetech.nexusplus.ads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.billing.PremiumRepository
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

// ── Live Ad Unit IDs (Nexus Wave Technologies) ────────────────────────────────
object NexusAdIds {
    const val BANNER_ANDROID       = "ca-app-pub-9723434393305967/3163996172"
    const val INTERSTITIAL_ANDROID = "ca-app-pub-9723434393305967/6401326195"
    const val BANNER_IOS           = "ca-app-pub-9723434393305967/3163996172"
    const val INTERSTITIAL_IOS     = "ca-app-pub-9723434393305967/6401326195"
}

// ── Ad frequency controller ───────────────────────────────────────────────────
object NexusAdController {
    private var sessionScreenViews = 0

    fun onScreenView(): Boolean {
        sessionScreenViews++
        return sessionScreenViews % 6 == 0
    }

    fun resetSession() { sessionScreenViews = 0 }
}

// ── Ad load state ─────────────────────────────────────────────────────────────
enum class AdLoadState { IDLE, LOADING, LOADED, FAILED }

// ── Compact Banner Ad (50 dp) ─────────────────────────────────────────────────
// Only rendered once the ad is LOADED — never shows an empty placeholder.
// When integrating a real AdMob AndroidView, call onAdLoaded() in the
// AdListener.onAdLoaded callback and onAdFailed() in onAdFailedToLoad.
@Composable
fun NexusBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = NexusAdIds.BANNER_ANDROID,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailed: (() -> Unit)? = null,
) {
    var loadState by remember { mutableStateOf(AdLoadState.LOADING) }

    // Simulate ad load lifecycle (replace with real AdMob callback).
    LaunchedEffect(adUnitId) {
        loadState = AdLoadState.LOADING
        delay(1200)
        loadState = AdLoadState.LOADED
        onAdLoaded?.invoke()
    }

    AnimatedVisibility(
        visible = loadState == AdLoadState.LOADED,
        enter   = slideInVertically { it } + fadeIn(),
        exit    = slideOutVertically { it } + fadeOut(),
    ) {
        Surface(
            modifier       = modifier.fillMaxWidth().height(50.dp),
            color          = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            tonalElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text      = "Advertisement",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Medium Rectangle Ad (250 dp) ─────────────────────────────────────────────
@Composable
fun NexusMediumRectangleAd(
    modifier: Modifier = Modifier,
    adUnitId: String = NexusAdIds.BANNER_ANDROID,
) {
    var loadState by remember { mutableStateOf(AdLoadState.LOADING) }

    LaunchedEffect(adUnitId) {
        loadState = AdLoadState.LOADING
        delay(1200)
        loadState = AdLoadState.LOADED
    }

    AnimatedVisibility(
        visible = loadState == AdLoadState.LOADED,
        enter   = fadeIn(),
        exit    = fadeOut(),
    ) {
        Surface(
            modifier       = modifier.fillMaxWidth().height(250.dp),
            color          = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            tonalElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    "Advertisement",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }
        }
    }
}

// ── Native-style Ad Card ──────────────────────────────────────────────────────
@Composable
fun NexusNativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = NexusAdIds.BANNER_ANDROID,
) {
    var loadState by remember { mutableStateOf(AdLoadState.LOADING) }

    LaunchedEffect(adUnitId) {
        loadState = AdLoadState.LOADING
        delay(1200)
        loadState = AdLoadState.LOADED
    }

    AnimatedVisibility(
        visible = loadState == AdLoadState.LOADED,
        enter   = fadeIn(),
        exit    = fadeOut(),
    ) {
        Surface(
            modifier       = modifier.fillMaxWidth().defaultMinSize(minHeight = 80.dp),
            shape          = MaterialTheme.shapes.medium,
            color          = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .then(
                            Modifier.padding(0.dp)
                        )
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Sponsored",
                        style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                    )
                    Text(
                        "Ad Headline — Your product here",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                    Text(
                        "Short ad description for your service or app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

// ── NexusAdScaffold ───────────────────────────────────────────────────────────
// Master wrapper — wrap ANY feature screen with this to get a sticky bottom
// banner ad (50dp) that only appears after the ad is loaded (no empty gap).
//
// Usage:
//   NexusAdScaffold { YourFeatureContent() }
@Composable
fun NexusAdScaffold(
    modifier: Modifier = Modifier,
    adUnitId: String   = NexusAdIds.BANNER_ANDROID,
    content: @Composable () -> Unit,
) {
    val premiumRepo: PremiumRepository = koinInject()
    val isPremium by premiumRepo.isPremiumFlow.collectAsState(initial = false)

    if (isPremium) {
        // Premium users see no ads — just full-screen content
        Box(modifier = modifier.fillMaxSize()) { content() }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        NexusBannerAd(
            modifier    = Modifier.fillMaxWidth(),
            adUnitId    = adUnitId,
            onAdLoaded  = {},
            onAdFailed  = {},
        )
    }
}
