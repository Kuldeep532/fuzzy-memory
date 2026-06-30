package com.nexuswavetech.nexusplus.features.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.nexuswavetech.nexusplus.ui.components.HelpSection
import org.koin.compose.koinInject

@Composable
fun MoreScreen(
    rootNavController: NavController,
    onRateApp        : () -> Unit = {},
) {
    val sessionManager: SessionManager    = koinInject()
    val notifRepo: NotificationRepository = koinInject()
    val session       by sessionManager.session.collectAsState()
    val notifications by notifRepo.notifications.collectAsState(initial = emptyList())
    val unreadCount = notifications.count { !it.isRead }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {

        // ── Hero header ────────────────────────────────────────────────────
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(modifier = Modifier.padding(24.dp, 28.dp, 24.dp, 20.dp)) {

                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector        = if (session.isGuest) Icons.Filled.PersonOutline else Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                modifier           = Modifier.size(30.dp),
                                tint               = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text  = session.displayName.ifBlank { "Guest User" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text  = if (session.isGuest) "Guest Account" else "Authenticated",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            )
                        }
                        FilledTonalIconButton(
                            onClick  = { rootNavController.navigate(Screen.Profile.route) },
                            modifier = Modifier.semantics { contentDescription = "Open profile settings" },
                        ) {
                            Icon(Icons.Filled.ChevronRight, null)
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatPill(label = "Features", value = "${FeatureCatalog.totalCount}")
                        StatPill(label = "Alerts", value = if (unreadCount > 0) "$unreadCount new" else "None")
                        StatPill(label = "Version", value = "1.3.0")
                    }
                }
            }
        }

        // ── Quick Access ──────────────────────────────────────────────────
        item { Spacer(Modifier.height(16.dp)) }
        item { MoreSectionLabel("Quick Access", Modifier.padding(horizontal = 20.dp)) }
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MoreMenuCard(
                    icon        = Icons.Filled.Person,
                    title       = "My Profile",
                    subtitle    = "View usage stats, manage account",
                    onClick     = { rootNavController.navigate(Screen.Profile.route) },
                    contentDesc = "My Profile. View usage stats and account settings.",
                )
                MoreMenuCard(
                    icon        = Icons.Filled.Notifications,
                    title       = "Notifications",
                    subtitle    = if (unreadCount > 0) "$unreadCount unread" else "All caught up",
                    onClick     = { rootNavController.navigate(Screen.NotificationCenter.route) },
                    contentDesc = "Notifications. ${if (unreadCount > 0) "$unreadCount unread notifications." else "All caught up."}",
                    badge       = if (unreadCount > 0) unreadCount else null,
                )
            }
        }

        // ── Health ─────────────────────────────────────────────────────────
        item { Spacer(Modifier.height(18.dp)) }
        item { MoreSectionLabel("Health & Wellbeing", Modifier.padding(horizontal = 20.dp)) }
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                MoreMenuCard(
                    icon        = Icons.Filled.HealthAndSafety,
                    title       = "Health Vault",
                    subtitle    = "Secure offline health records and vitals",
                    onClick     = { rootNavController.navigate(Screen.NexusHealthVault.route) },
                    contentDesc = "Health Vault. Secure offline health records and vitals tracker.",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        // ── Preferences ────────────────────────────────────────────────────
        item { Spacer(Modifier.height(18.dp)) }
        item { MoreSectionLabel("Preferences", Modifier.padding(horizontal = 20.dp)) }
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                MoreMenuCard(
                    icon        = Icons.Filled.Settings,
                    title       = "Settings",
                    subtitle    = "Theme, accessibility, font size",
                    onClick     = { rootNavController.navigate(Screen.Settings.route) },
                    contentDesc = "Settings. Open app preferences for theme, accessibility and font size.",
                    accentColor = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // ── Legal ──────────────────────────────────────────────────────────
        item { Spacer(Modifier.height(18.dp)) }
        item { MoreSectionLabel("Legal & Information", Modifier.padding(horizontal = 20.dp)) }
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MoreMenuCard(
                    icon        = Icons.Filled.Info,
                    title       = "About",
                    subtitle    = "App info and developer details",
                    onClick     = { rootNavController.navigate(Screen.AboutUs.route) },
                    contentDesc = "About. Opens app information and developer details.",
                )
                MoreMenuCard(
                    icon        = Icons.Filled.PrivacyTip,
                    title       = "Privacy Policy",
                    subtitle    = "How your data is handled",
                    onClick     = { rootNavController.navigate(Screen.PrivacyPolicy.route) },
                    contentDesc = "Privacy Policy. Opens data handling and privacy information.",
                )
                MoreMenuCard(
                    icon        = Icons.Filled.Gavel,
                    title       = "Terms & Conditions",
                    subtitle    = "Usage terms and liability",
                    onClick     = { rootNavController.navigate(Screen.TermsConditions.route) },
                    contentDesc = "Terms and Conditions. Opens usage terms and liability information.",
                )
            }
        }

        // ── Help ───────────────────────────────────────────────────────────
        item { Spacer(Modifier.height(18.dp)) }
        item { MoreSectionLabel("Help & Support", Modifier.padding(horizontal = 20.dp)) }
        item { Spacer(Modifier.height(8.dp)) }
        item { HelpSection(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) }

        // ── App ────────────────────────────────────────────────────────────
        item { Spacer(Modifier.height(18.dp)) }
        item { MoreSectionLabel("App", Modifier.padding(horizontal = 20.dp)) }
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MoreMenuCard(
                    icon        = Icons.Filled.NewReleases,
                    title       = "App Version",
                    subtitle    = "Nexus Plus v1.4.0 — ${FeatureCatalog.totalCount} features",
                    onClick     = {},
                    contentDesc = "App version: Nexus Plus version 1.4.0 with ${FeatureCatalog.totalCount} features.",
                    showChevron = false,
                )
                MoreMenuCard(
                    icon        = Icons.Filled.RateReview,
                    title       = "Rate the App",
                    subtitle    = "Leave us a review on the Play Store",
                    onClick     = onRateApp,
                    contentDesc = "Rate Nexus Plus on the Play Store.",
                )
            }
        }
    }
}

// ── Private helpers ─────────────────────────────────────────────────────────

@Composable
private fun StatPill(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier            = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $value"
        },
    ) {
        Text(
            text  = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun MoreSectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
        color    = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun MoreMenuCard(
    icon        : ImageVector,
    title       : String,
    subtitle    : String,
    onClick     : () -> Unit,
    contentDesc : String,
    badge       : Int?   = null,
    showChevron : Boolean = true,
    accentColor : Color   = MaterialTheme.colorScheme.primary,
) {
    Card(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = contentDesc },
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = accentColor,
                    modifier           = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                badge != null && badge > 0 -> Badge { Text("$badge") }
                showChevron -> Icon(
                    imageVector        = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}
