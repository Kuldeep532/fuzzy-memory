package com.nexuswavetech.nexusplus.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Privacy Policy", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Effective Date: July 1, 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Our Core Privacy Commitment",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Nexus Wave Technologies limits sensitive access to user-requested features, " +
                        "clearly discloses SMS, location, package visibility, accessibility, " +
                        "and advertising behavior, and does not sell personal data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            HorizontalDivider()

            LegalSection(
                title = "1. Information We Do Not Collect",
                content = "Nexus Wave Technologies does not collect, store, sell, or transmit any " +
                    "personally identifiable information (PII) from users of Nexus Plus. This includes, " +
                    "but is not limited to: names, email addresses, phone numbers, device identifiers, " +
                    "IP addresses, usage analytics, or behavioral tracking data.\n\n" +
                    "We maintain zero remote tracking logs. No user activity is sent to our servers."
            )

            LegalSection(
                title = "2. API Data Processing",
                content = "Nexus Plus connects to select third-party public APIs (Pollinations AI " +
                    "for image generation, public news RSS feeds, and internet speed-test endpoints) " +
                    "solely to deliver the requested feature functionality. API requests are made " +
                    "directly from your device to the respective third-party endpoint. Nexus Wave " +
                    "Technologies does not intercept, proxy, log, or store any data exchanged " +
                    "during these API interactions.\n\n" +
                    "Music playback is entirely offline — only locally stored audio files are " +
                    "accessed. No music or media streaming is performed via third-party services.\n\n" +
                    "Users should review the privacy policies of each third-party API service " +
                    "independently, as their terms govern their own data handling."
            )

            LegalSection(
                title = "3. Local Storage",
                content = "Certain app features store data exclusively on your local device using " +
                    "Android's DataStore and SharedPreferences mechanisms. This includes your " +
                    "favorites list, TTS preferences, and guest session name. This data never " +
                    "leaves your device and is not accessible to Nexus Wave Technologies or any " +
                    "third party."
            )

            LegalSection(
                title = "4. Authentication",
                content = "Guest session names are stored locally only. If you choose to sign in " +
                    "with Google in a future version, your authentication is handled entirely by " +
                    "Google's Firebase Authentication service. Nexus Plus only receives a " +
                    "session token and basic profile information (name, email) provided by " +
                    "Google, subject to Google's own privacy policy."
            )

            LegalSection(
                title = "5. Media & Generated Content",
                content = "AI-generated images created using the Pollinations AI integration are " +
                    "generated on Pollinations' servers. If you choose to save images to your " +
                    "gallery, they are stored on your device only. Nexus Wave Technologies does " +
                    "not retain copies of generated images."
            )

            LegalSection(
                title = "6. Sensitive Permissions and Safety Features",
                content = "Emergency Guardian requests Send SMS, phone call, notification, and location permissions only after you open that feature. The service starts only when you turn Guardian on, shows a persistent notification, waits through a visible 10-second cancelable countdown, then sends an SOS SMS only to contacts you saved in the app. Location is acquired once for that SOS message and is not used for advertising, analytics, or tracking. Nexus Plus does not read incoming SMS or monitor messages.\n\n" +
                    "The Installed Apps tool does not request QUERY_ALL_PACKAGES; Android may show only the packages visible under platform package-visibility rules. Accessibility service access is optional and user-enabled in Android Settings only for screen reading and text-to-speech support."
            )

            LegalSection(
                title = "7. Advertising",
                content = "Free users may see Unity Ads banners labeled as ads. Ads are not disguised as app controls, are not injected into system screens, and are not shown as sudden automatic pop-ups during navigation. Premium users do not see ads. Ad SDK partners may process device or ad identifiers according to their own policies; Nexus Wave Technologies does not use Emergency Guardian SMS/location data for ads."
            )

            LegalSection(
                title = "8. Children's Privacy",
                content = "Nexus Plus does not knowingly collect any information from children " +
                    "under the age of 13. The application is intended for general audiences. " +
                    "If you believe a child has provided personal information through this app, " +
                    "please contact us immediately."
            )

            LegalSection(
                title = "9. Changes to This Policy",
                content = "Nexus Wave Technologies reserves the right to update this Privacy " +
                    "Policy at any time. Changes will be reflected within the application. " +
                    "Continued use of Nexus Plus after changes constitutes acceptance of " +
                    "the updated policy."
            )

            LegalSection(
                title = "10. Contact",
                content = "For any privacy-related questions or concerns, please contact Nexus Wave " +
                    "Technologies through the official channels listed in the More screen of " +
                    "this application."
            )

            HorizontalDivider()

            Text(
                text = "Nexus Wave Technologies — Committed to Your Privacy",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
