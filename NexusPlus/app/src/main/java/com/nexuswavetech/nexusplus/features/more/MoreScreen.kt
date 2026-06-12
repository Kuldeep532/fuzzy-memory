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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexuswavetech.nexusplus.core.SessionManager
import com.nexuswavetech.nexusplus.core.displayName
import com.nexuswavetech.nexusplus.core.isGuest
import com.nexuswavetech.nexusplus.navigation.Screen
import com.nexuswavetech.nexusplus.ui.components.SocialMediaLinksSection
import org.koin.compose.koinInject

@Composable
fun MoreScreen(rootNavController: NavController) {
    val context = LocalContext.current
    val sessionManager: SessionManager = koinInject()
    val session by sessionManager.session.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            // User profile card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (session.isGuest) Icons.Filled.PersonOutline else Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = session.displayName.ifBlank { "Guest User" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (session.isGuest) "Guest Account — Limited Access" else "Authenticated Account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("Legal & Information")
        }

        item {
            MoreMenuItem(
                icon = Icons.Filled.Info,
                title = "About Us",
                subtitle = "App info and developer details",
                onClick = { rootNavController.navigate(Screen.AboutUs.route) },
                contentDesc = "About Us. Opens app information and Nexus Wave Technologies developer details."
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Filled.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "How your data is handled",
                onClick = { rootNavController.navigate(Screen.PrivacyPolicy.route) },
                contentDesc = "Privacy Policy. Opens data handling and privacy information."
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Filled.Gavel,
                title = "Terms & Conditions",
                subtitle = "Usage terms and liability",
                onClick = { rootNavController.navigate(Screen.TermsConditions.route) },
                contentDesc = "Terms and Conditions. Opens usage terms and liability information."
            )
        }

        item {
            SectionHeader("Developer")
        }

        item {
            SocialMediaLinksSection(modifier = Modifier.fillMaxWidth())
        }

        item {
            SectionHeader("App")
        }

        item {
            MoreMenuItem(
                icon = Icons.Filled.NewReleases,
                title = "App Version",
                subtitle = "Nexus Plus v1.0.0",
                onClick = {},
                contentDesc = "App version: Nexus Plus version 1.0.0"
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Filled.RateReview,
                title = "Rate on Play Store",
                subtitle = "Leave us a review",
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.nexuswavetech.nexusplus")
                    )
                    runCatching { context.startActivity(intent) }
                },
                contentDesc = "Rate Nexus Plus on the Google Play Store."
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() }
    )
}

@Composable
private fun MoreMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    contentDesc: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = contentDesc },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
