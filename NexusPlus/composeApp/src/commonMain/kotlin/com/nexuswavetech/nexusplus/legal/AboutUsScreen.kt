package com.nexuswavetech.nexusplus.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.nexuswavetech.nexusplus.ui.components.NexusWaveLogo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.core.FeatureCatalog
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

@Composable
fun AboutUsScreen(
    onBack: () -> Unit,
    onSocialMedia: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "About Us", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NexusWaveLogo(modifier = Modifier.size(80.dp))

            Text(
                text = "Nexus Plus",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            Text(
                text = "Version 1.4.0 · ${FeatureCatalog.totalCount} Premium Features",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            HorizontalDivider()

            // ── Company Story ─────────────────────────────────────────────
            LegalSection(
                title = "Our Story",
                content = "Nexus Plus is a flagship multi-utility super-app developed by Nexus Wave Technologies, a forward-thinking technology company committed to building powerful, accessible, and privacy-first mobile tools. What started as a simple idea — putting every essential utility in one app — has grown into a comprehensive ecosystem serving thousands of users daily."
            )

            // ── Founder ───────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Person, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        text = "Founder & Lead Developer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Kuldeep Kumar Yadav",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Visionary behind Nexus Wave Technologies. Passionate about accessibility, privacy, and building technology that truly serves people. Led the design and development of every feature in Nexus Plus from the ground up.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Company ─────────────────────────────────────────────────────
            LegalSection(
                title = "Nexus Wave Technologies",
                content = "We are an independent technology company focused on creating intelligent mobile applications that combine utility, security, and accessibility. Our mission is to empower users with tools that work offline, respect privacy, and remain accessible to everyone — including people with visual impairments."
            )

            // ── Mission ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Our Mission",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "1. Privacy First — Your data stays on your device.\n2. Accessibility Always — Built for screen readers and all abilities.\n3. Offline Power — Works without internet whenever possible.\n4. One App, Everything — Replace dozens of single-purpose apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f,
                    )
                }
            }

            // ── Tech Highlights ────────────────────────────────────────────
            LegalSection(
                title = "Technology",
                content = "Nexus Plus is built with Kotlin and Jetpack Compose, featuring AES-256-GCM encryption, on-device ML Kit integration, Media3 ExoPlayer, and a custom NSE (Nexus Speech Engine) for advanced text-to-speech. The app supports Android 8.0+ and follows Material Design 3 guidelines with full TalkBack accessibility."
            )

            // ── Contact / Community ─────────────────────────────────────────
            Button(
                onClick = onSocialMedia,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Group, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Community & Social Media", fontWeight = FontWeight.SemiBold)
            }

            Text(
                text = "© 2024–2026 Nexus Wave Technologies. All rights reserved.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun LegalSection(title: String, content: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}
