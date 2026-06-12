package com.nexuswavetech.nexusplus.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class SocialLink(
    val label: String,
    val handle: String,
    val url: String,
    val contentDesc: String
)

val nexusSocialLinks = listOf(
    SocialLink(
        label = "Instagram",
        handle = "@nexuswave_technologies",
        url = "https://www.instagram.com/nexuswave_technologies?igsh=MTBia3ZxODcwOTFrNg==",
        contentDesc = "Instagram page of Nexus Wave Technologies. Opens in browser."
    ),
    SocialLink(
        label = "Facebook",
        handle = "Nexus Wave Technologies",
        url = "https://www.facebook.com/profile.php?id=61590971301245",
        contentDesc = "Facebook page of Nexus Wave Technologies. Opens in browser."
    ),
    SocialLink(
        label = "Telegram",
        handle = "NexusWaveTechnologies27",
        url = "https://t.me/NexusWaveTechnologies27",
        contentDesc = "Telegram channel of Nexus Wave Technologies. Opens in browser."
    ),
    SocialLink(
        label = "WhatsApp Channel",
        handle = "Nexus Wave Technologies",
        url = "https://whatsapp.com/channel/0029VbDI2cL42Dcc9m6nfm3T",
        contentDesc = "WhatsApp channel of Nexus Wave Technologies. Opens in browser."
    ),
    SocialLink(
        label = "Discord",
        handle = "Nexus Wave Community",
        url = "https://discord.gg/3yp8MMwJe",
        contentDesc = "Discord server of Nexus Wave Technologies. Opens in browser."
    ),
    SocialLink(
        label = "GitHub",
        handle = "@NexusWaveTech",
        url = "https://github.com/NexusWaveTechnologies",
        contentDesc = "GitHub profile of Nexus Wave Technologies. Opens in browser."
    ),
    SocialLink(
        label = "Official Website",
        handle = "nexuswavetech.com",
        url = "https://nexuswavetech.com",
        contentDesc = "Official website of Nexus Wave Technologies. Opens in browser."
    )
)

@Composable
fun SocialMediaLinksSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Connect with Us",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        nexusSocialLinks.forEach { link ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = link.contentDesc },
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                    context.startActivity(intent)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = when (link.label) {
                            "Instagram"       -> Icons.Filled.PhotoCamera
                            "Facebook"        -> Icons.Filled.People
                            "Telegram"        -> Icons.Filled.Message
                            "WhatsApp Channel"-> Icons.Filled.Forum
                            "Discord"         -> Icons.Filled.Forum
                            "GitHub"          -> Icons.Filled.Code
                            "Official Website"-> Icons.Filled.Language
                            else              -> Icons.Filled.Share
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = link.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = link.handle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
