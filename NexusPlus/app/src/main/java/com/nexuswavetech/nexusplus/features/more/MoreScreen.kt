package com.nexuswavetech.nexusplus.features.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexuswavetech.nexusplus.core.FeatureCatalog
import com.nexuswavetech.nexusplus.core.SessionManager
import com.nexuswavetech.nexusplus.core.displayName
import com.nexuswavetech.nexusplus.core.isGuest
import com.nexuswavetech.nexusplus.features.notifications.NotificationRepository
import com.nexuswavetech.nexusplus.navigation.Screen
import com.nexuswavetech.nexusplus.ui.components.SocialMediaLinksSection
import org.koin.compose.koinInject

@Composable
fun MoreScreen(rootNavController: NavController) {
    val context            = LocalContext.current
    val sessionManager: SessionManager          = koinInject()
    val notifRepo: NotificationRepository       = koinInject()
    val session     by sessionManager.session.collectAsState()
    val notifications by notifRepo.notifications.collectAsState(initial = emptyList())
    val unreadCount = notifications.count { !it.isRead }

    LazyColumn(
        modifier        = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding  = PaddingValues(vertical = 16.dp),
    ) {
        // ── User Card ─────────────────────────────────────────────────────
        item {
            Card(
                onClick  = { rootNavController.navigate(Screen.Profile.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Profile card for ${session.displayName.ifBlank { "Guest User" }}. Tap to view profile." },
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier              = Modifier.padding(16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector        = if (session.isGuest) Icons.Filled.PersonOutline else Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        modifier           = Modifier.size(48.dp),
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = session.displayName.ifBlank { "Guest User" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text  = if (session.isGuest) "Guest Account — Tap to view profile" else "Authenticated · Tap to view profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                }
            }
        }

        // ── Quick Actions ─────────────────────────────────────────────────
        item { SectionHeader("Quick Access") }

        item {
            MoreMenuItem(
                icon        = Icons.Filled.Person,
                title       = "My Profile",
                subtitle    = "View usage stats, manage account",
                onClick     = { rootNavController.navigate(Screen.Profile.route) },
                contentDesc = "My Profile. View usage stats and account settings.",
            )
        }

        item {
            MoreMenuItem(
                icon        = Icons.Filled.Notifications,
                title       = "Notifications",
                subtitle    = if (unreadCount > 0) "$unreadCount unread" else "All caught up",
                onClick     = { rootNavController.navigate(Screen.NotificationCenter.route) },
                contentDesc = "Notifications. ${if (unreadCount > 0) "$unreadCount unread notifications." else "All caught up."}",
                badge       = if (unreadCount > 0) unreadCount else null,
            )
        }

        // ── Health & Wellbeing ────────────────────────────────────────────
        item { SectionHeader("Health & Wellbeing") }

        item {
            MoreMenuItem(
                icon        = Icons.Filled.HealthAndSafety,
                title       = "Health Vault",
                subtitle    = "Secure offline health records & vitals",
                onClick     = { rootNavController.navigate(Screen.NexusHealthVault.route) },
                contentDesc = "Health Vault. Secure offline health records and vitals tracker.",
            )
        }

        // ── App Preferences ───────────────────────────────────────────────
        item { SectionHeader("Preferences") }

        item {
            MoreMenuItem(
                icon        = Icons.Filled.Settings,
                title       = "Settings",
                subtitle    = "Theme, accessibility, font size",
                onClick     = { rootNavController.navigate(Screen.Settings.route) },
                contentDesc = "Settings. Open app preferences for theme, accessibility and font size.",
            )
        }

        // ── Legal & Information ───────────────────────────────────────────
        item { SectionHeader("Legal & Information") }

        item {
            MoreMenuItem(
                icon        = Icons.Filled.Info,
                title       = "About Us",
                subtitle    = "App info and developer details",
                onClick     = { rootNavController.navigate(Screen.AboutUs.route) },
                contentDesc = "About Us. Opens app information and Nexus Wave Technologies developer details.",
            )
        }

        item {
            MoreMenuItem(
                icon        = Icons.Filled.PrivacyTip,
                title       = "Privacy Policy",
                subtitle    = "How your data is handled",
                onClick     = { rootNavController.navigate(Screen.PrivacyPolicy.route) },
                contentDesc = "Privacy Policy. Opens data handling and privacy information.",
            )
        }

        item {
            MoreMenuItem(
                icon        = Icons.Filled.Gavel,
                title       = "Terms & Conditions",
                subtitle    = "Usage terms and liability",
                onClick     = { rootNavController.navigate(Screen.TermsConditions.route) },
                contentDesc = "Terms and Conditions. Opens usage terms and liability information.",
            )
        }

        // ── Connect ───────────────────────────────────────────────────────
        item { SectionHeader("Connect with Nexus Wave") }

        item { SocialMediaLinksSection(modifier = Modifier.fillMaxWidth()) }

        // ── App ───────────────────────────────────────────────────────────
        item { SectionHeader("App") }

        item {
            MoreMenuItem(
                icon        = Icons.Filled.NewReleases,
                title       = "App Version",
                subtitle    = "Nexus Plus v1.2.0 · ${FeatureCatalog.allFeatures.size} features",
                onClick     = {},
                contentDesc = "App version: Nexus Plus version 1.2.0 with ${FeatureCatalog.allFeatures.size} features.",
            )
        }

        item {
            MoreMenuItem(
                icon        = Icons.Filled.RateReview,
                title       = "Rate on Play Store",
                subtitle    = "Leave us a review",
                onClick     = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.nexuswavetech.nexusplus"),
                    )
                    runCatching { context.startActivity(intent) }
                },
                contentDesc = "Rate Nexus Plus on the Google Play Store.",
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    contentDesc: String,
    badge: Int? = null,
) {
    Card(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = contentDesc },
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badge != null && badge > 0) {
                Badge { Text("$badge") }
            } else {
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}
