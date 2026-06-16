package com.nexuswavetech.nexusplus.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Ad Unit ID Constants ──────────────────────────────────────────────────────
// Replace these test IDs with your live AdMob IDs before publishing.
// ANDROID TEST IDs (safe for development — never show real ads in test builds)
object NexusAdIds {
    const val APP_OPEN_ANDROID  = "ca-app-pub-3940256099942544/9257395921"
    const val BANNER_ANDROID    = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_ANDROID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_ANDROID  = "ca-app-pub-3940256099942544/5224354917"

    // iOS Unit IDs (Apple App Store distribution)
    const val BANNER_IOS        = "ca-app-pub-3940256099942544/2934735716"
    const val INTERSTITIAL_IOS  = "ca-app-pub-3940256099942544/4411468910"
    const val APP_OPEN_IOS      = "ca-app-pub-3940256099942544/5575463023"
}

// ── Ad State Controller ───────────────────────────────────────────────────────
// Singleton that tracks interstitial load/show state across the app.
// Wire this into your Koin DI once Google Mobile Ads SDK is added.
object NexusAdController {
    private var interstitialLoadCount = 0
    private var sessionScreenViews = 0

    // Call on every major screen navigation
    fun onScreenView(): Boolean {
        sessionScreenViews++
        // Show interstitial every 5 screen transitions (configurable)
        return sessionScreenViews % 5 == 0
    }

    fun onInterstitialLoaded() { interstitialLoadCount++ }
    fun getLoadCount() = interstitialLoadCount
}

// ── Banner Ad Composable Placeholder ─────────────────────────────────────────
// Replace the inner Box with an AndroidView wrapping AdView once SDK is added:
//   AndroidView(factory = { ctx ->
//       AdView(ctx).apply {
//           setAdSize(AdSize.BANNER)
//           adUnitId = NexusAdIds.BANNER_ANDROID
//           loadAd(AdRequest.Builder().build())
//       }
//   }, modifier = modifier)
@Composable
fun NexusBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = NexusAdIds.BANNER_ANDROID,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = "Advertisement",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Large Banner / Medium Rectangle Ad Composable ────────────────────────────
@Composable
fun NexusMediumRectangleAd(
    modifier: Modifier = Modifier,
    adUnitId: String = NexusAdIds.BANNER_ANDROID,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Advertisement",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ── Native Ad Frame Composable ────────────────────────────────────────────────
// Replace content with NativeAdView binding once SDK is integrated.
@Composable
fun NexusNativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = NexusAdIds.BANNER_ANDROID,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
                    )
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Sponsored",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
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
