package com.nexuswavetech.nexusplus.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

// ── Live Ad Unit IDs (Nexus Wave Technologies) ────────────────────────────────
object NexusAdIds {
    // Banner — shown inline in scrollable content (non-intrusive)
    const val BANNER_ANDROID    = "ca-app-pub-9723434393305967/3163996172"
    // Interstitial — shown on natural transition points (not during active use)
    const val INTERSTITIAL_ANDROID = "ca-app-pub-9723434393305967/6401326195"

    // iOS Unit IDs (future)
    const val BANNER_IOS           = "ca-app-pub-9723434393305967/3163996172"
    const val INTERSTITIAL_IOS     = "ca-app-pub-9723434393305967/6401326195"
}

// ── Ad frequency controller ───────────────────────────────────────────────────
// Tracks per-session screen views so interstitials don't fire too often.
object NexusAdController {
    private var sessionScreenViews = 0

    /** Call once per feature screen entry. Returns true when an interstitial should fire. */
    fun onScreenView(): Boolean {
        sessionScreenViews++
        // Interstitial every 6 navigations — enough to earn without annoying
        return sessionScreenViews % 6 == 0
    }

    fun resetSession() { sessionScreenViews = 0 }
}

// ── Compact Banner Ad (50 dp) ─────────────────────────────────────────────────
// Shown at the bottom of every feature screen, pinned above keyboard.
// Wire: replace inner Box with AndroidView { AdView(ctx).apply { adUnitId = …; loadAd(…) } }
@Composable
fun NexusBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = NexusAdIds.BANNER_ANDROID,
) {
    Surface(
        modifier        = modifier.fillMaxWidth().height(50.dp),
        color           = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation  = 1.dp,
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

// ── Medium Rectangle Ad (250 dp) ─────────────────────────────────────────────
// Used in result-heavy screens (Currency, Weather) between content sections.
@Composable
fun NexusMediumRectangleAd(
    modifier: Modifier = Modifier,
    adUnitId: String = NexusAdIds.BANNER_ANDROID,
) {
    Surface(
        modifier       = modifier.fillMaxWidth().height(250.dp),
        color          = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
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
// Used inside LazyColumn item lists — blends naturally with list content.
@Composable
fun NexusNativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = NexusAdIds.BANNER_ANDROID,
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
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
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

// ── NexusAdScaffold ───────────────────────────────────────────────────────────
// Master wrapper — wrap ANY feature screen with this to get a sticky bottom
// banner ad (50dp) pinned above the system nav bar.
//
// Usage:
//   NexusAdScaffold { YourFeatureContent() }
//
// The ad is pinned at the very bottom and never overlaps scrollable content.
// The inner Box applies the Scaffold's bottomPadding automatically so the
// screen content scrolls above the ad.
@Composable
fun NexusAdScaffold(
    modifier: Modifier = Modifier,
    adUnitId: String   = NexusAdIds.BANNER_ANDROID,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier  = modifier,
        bottomBar = {
            NexusBannerAd(
                modifier = Modifier.fillMaxWidth(),
                adUnitId = adUnitId,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            content()
        }
    }
}
