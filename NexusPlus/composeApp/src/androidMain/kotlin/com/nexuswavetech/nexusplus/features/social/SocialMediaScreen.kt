package com.nexuswavetech.nexusplus.features.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.platform.PlatformUrlHandler
import com.nexuswavetech.nexusplus.remoteconfig.RemoteConfigRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import org.koin.compose.koinInject

private data class RemoteSocialLink(
    val label: String,
    val handle: String,
    val url: String,
    val icon: ImageVector,
)

@Composable
fun SocialMediaScreen(onBack: () -> Unit) {
    val remoteConfig: RemoteConfigRepository = koinInject()
    val urlHandler: PlatformUrlHandler       = koinInject()

    val socialLinks = remember {
        buildList {
            remoteConfig.officialWebsiteUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("Official Website", "nexuswavetech.com", it, Icons.Filled.Language))
            }
            remoteConfig.instagramUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("Instagram", "@nexuswave_technologies", it, Icons.Filled.PhotoCamera))
            }
            remoteConfig.facebookUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("Facebook", "Nexus Wave Technologies", it, Icons.Filled.People))
            }
            remoteConfig.twitterUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("X (Twitter)", "@nexuswavetech", it, Icons.Filled.Newspaper))
            }
            remoteConfig.youtubeUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("YouTube", "@nexuswavetech", it, Icons.Filled.PlayCircle))
            }
            remoteConfig.tiktokUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("TikTok", "@nexuswavetech", it, Icons.Filled.VideoLibrary))
            }
            remoteConfig.telegramUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("Telegram", "NexusWaveTechnologies27", it, Icons.AutoMirrored.Filled.Message))
            }
            remoteConfig.whatsappUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("WhatsApp Channel", "Nexus Wave Technologies", it, Icons.Filled.Forum))
            }
            remoteConfig.discordUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("Discord", "Nexus Wave Community", it, Icons.Filled.Group))
            }
            remoteConfig.githubUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("GitHub", "@NexusWaveTech", it, Icons.Filled.Code))
            }
            remoteConfig.linkedinUrl.takeIf { it.isNotBlank() }?.let {
                add(RemoteSocialLink("LinkedIn", "Nexus Wave Technologies", it, Icons.Filled.BusinessCenter))
            }
        }
    }

    val supportEmail = remember { remoteConfig.supportEmail }

    Scaffold(
        topBar = {
            NexusTopBar(
                title = "Community & Social",
                onBack = onBack,
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Branding header ──────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Language,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            "Nexus Wave Technologies",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            "Follow us for updates, tips & community",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            // ── Social links section header ──────────────────────────────────
            item {
                Text(
                    "Our Platforms",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // ── Social links ─────────────────────────────────────────────────
            items(socialLinks.size) { index ->
                val link = socialLinks[index]
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Open ${link.label}" },
                    onClick = { urlHandler.openUrl(link.url) },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = link.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                link.label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                link.handle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Icon(
                            Icons.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // ── Contact & Support ────────────────────────────────────────────
            if (supportEmail.isNotBlank()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Contact & Support",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Email support at $supportEmail" },
                        onClick = { urlHandler.openEmail(supportEmail, "Nexus Plus Support") },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                Icons.Filled.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(28.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Email Support",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    supportEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
