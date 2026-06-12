package com.nexuswavetech.nexusplus.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import com.nexuswavetech.nexusplus.ui.components.SocialMediaLinksSection

@Composable
fun AboutUsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "About Us", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App identity
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Nexus Plus",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )

            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            LegalSection(
                title = "Our Vision",
                content = "Nexus Plus was built to be the ultimate all-in-one digital toolkit — " +
                    "a thoughtfully designed suite of 30+ features that respect every user, " +
                    "including those who rely on assistive technologies like TalkBack. " +
                    "We believe powerful software should be universally accessible."
            )

            LegalSection(
                title = "Accessibility Commitment",
                content = "Every screen, every button, and every interaction in Nexus Plus is " +
                    "engineered for full TalkBack screen reader compatibility. We follow " +
                    "WCAG guidelines and Material Design 3 accessibility standards throughout " +
                    "the entire application. Accessibility is never an afterthought — it is " +
                    "a core design principle."
            )

            LegalSection(
                title = "Technology",
                content = "Nexus Plus is built natively with Jetpack Compose and Material Design 3, " +
                    "targeting Android with a modular Compose Multiplatform architecture ready " +
                    "for future cross-platform compilation. The codebase is designed for " +
                    "long-term maintainability and gradual feature expansion."
            )

            // Developer attribution — required on About Us per spec
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Developed and Maintained by",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Nexus Wave Technologies",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Crafting accessible, modular, and intelligent Android experiences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider()

            // Social links — required at bottom of About Us per spec
            SocialMediaLinksSection(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))
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
