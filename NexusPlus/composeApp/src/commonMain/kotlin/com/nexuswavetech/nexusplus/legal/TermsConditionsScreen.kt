package com.nexuswavetech.nexusplus.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
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
fun TermsConditionsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Terms & Conditions", onBack = onBack)

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
                    Icons.Filled.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Terms & Conditions",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Last Updated: July 1, 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "By downloading, installing, or using Nexus Plus, you agree to be bound " +
                    "by these Terms and Conditions. If you do not agree, do not use the application.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            LegalSection(
                title = "1. Acceptance of Terms",
                content = "These Terms and Conditions (\"Terms\") govern your use of the Nexus Plus " +
                    "mobile application (\"Application\") developed and maintained by Nexus Wave " +
                    "Technologies (\"Developer\", \"we\", \"us\"). By accessing or using the Application, " +
                    "you confirm that you are at least 13 years of age and agree to comply with " +
                    "and be legally bound by these Terms."
            )

            LegalSection(
                title = "2. License",
                content = "Nexus Wave Technologies grants you a limited, non-exclusive, " +
                    "non-transferable, revocable license to use the Application for your " +
                    "personal, non-commercial purposes strictly in accordance with these Terms. " +
                    "You may not: (a) copy, modify, or distribute the Application; " +
                    "(b) reverse engineer or attempt to extract the source code; " +
                    "(c) sell, rent, or sublicense the Application to any third party."
            )

            LegalSection(
                title = "3. Fair Use Policy — AI Tools",
                content = "The AI Image Generator feature uses the Pollinations AI public API. " +
                    "You agree to use this feature only for lawful purposes and in compliance " +
                    "with Pollinations AI's own terms of service. You must not generate content " +
                    "that is unlawful, harmful, threatening, abusive, harassing, defamatory, " +
                    "vulgar, obscene, or otherwise objectionable. Nexus Wave Technologies " +
                    "reserves the right to revoke access to AI tools for users who violate " +
                    "this fair use policy."
            )

            LegalSection(
                title = "4. Fair Use Policy — Streaming Features",
                content = "The IPTV, Music Streaming, and Radio features are provided for " +
                    "convenience and access to publicly available streams and APIs. You are " +
                    "solely responsible for ensuring that your use of these features complies " +
                    "with applicable copyright laws and the terms of service of the respective " +
                    "content providers. Nexus Wave Technologies does not host, own, or control " +
                    "any third-party streamed content and makes no warranties about its " +
                    "availability, accuracy, or legality."
            )

            LegalSection(
                title = "5. User Responsibilities",
                content = "You are solely responsible for: (a) maintaining the confidentiality " +
                    "of any account credentials; (b) all activities that occur under your " +
                    "account; (c) ensuring your use of the Application does not violate any " +
                    "applicable laws or regulations; (d) obtaining any necessary permissions " +
                    "before accessing or sharing third-party content through the Application."
            )

            LegalSection(
                title = "6. Sensitive Permission Consent",
                content = "By enabling features that require sensitive permissions, you consent to the specific use described in-app: Emergency Guardian may send SMS messages, place a call, and include a one-time location link only after you activate the service and the cancelable countdown completes; location tools use location only to display requested results; media and storage tools access files you choose or files required for the selected tool; accessibility features operate only after you enable them in Android Settings. You may revoke permissions in Android Settings at any time, but affected features may stop working."
            )

            LegalSection(
                title = "7. Emergency and Safety Feature Limitations",
                content = "Emergency Guardian is a convenience safety aid, not a replacement for emergency services, police, medical care, or professional monitoring. SMS delivery, phone calls, GPS accuracy, notifications, sensors, battery optimization, carrier coverage, and device settings can fail or be delayed. You are responsible for testing the feature, keeping contacts current, obtaining consent from emergency contacts, and calling local emergency services directly when needed."
            )

            LegalSection(
                title = "8. Advertising and Monetization",
                content = "The free version may display clearly labeled ads or sponsored placements. Ads must not be used to mislead, create hidden clicks, or interrupt critical safety flows. Premium purchases remove ads where supported. Third-party ad networks may process ad-related identifiers under their own terms; Nexus Plus does not use Emergency Guardian SMS or location content for advertising."
            )

            LegalSection(
                title = "9. Limitation of Liability",
                content = "TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, NEXUS WAVE " +
                    "TECHNOLOGIES SHALL NOT BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, " +
                    "CONSEQUENTIAL, OR PUNITIVE DAMAGES WHATSOEVER, INCLUDING BUT NOT LIMITED " +
                    "TO DAMAGES FOR LOSS OF PROFITS, DATA, GOODWILL, OR OTHER INTANGIBLE " +
                    "LOSSES, RESULTING FROM: (i) YOUR USE OR INABILITY TO USE THE APPLICATION; " +
                    "(ii) ANY THIRD-PARTY CONTENT ACCESSED THROUGH THE APPLICATION; " +
                    "(iii) ANY UNAUTHORIZED ACCESS TO OR ALTERATION OF YOUR TRANSMISSIONS " +
                    "OR DATA; OR (iv) ANY OTHER MATTER RELATING TO THE APPLICATION.\n\n" +
                    "IN NO EVENT SHALL NEXUS WAVE TECHNOLOGIES' TOTAL LIABILITY TO YOU FOR " +
                    "ALL DAMAGES EXCEED THE AMOUNT PAID BY YOU FOR THE APPLICATION IN THE " +
                    "TWELVE (12) MONTHS PRECEDING THE CLAIM."
            )

            LegalSection(
                title = "10. Disclaimer of Warranties",
                content = "THE APPLICATION IS PROVIDED \"AS IS\" AND \"AS AVAILABLE\" WITHOUT " +
                    "WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT " +
                    "LIMITED TO IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A " +
                    "PARTICULAR PURPOSE, AND NON-INFRINGEMENT. NEXUS WAVE TECHNOLOGIES DOES " +
                    "NOT WARRANT THAT THE APPLICATION WILL BE UNINTERRUPTED, ERROR-FREE, " +
                    "OR FREE OF VIRUSES OR OTHER HARMFUL COMPONENTS."
            )

            LegalSection(
                title = "11. Modifications",
                content = "Nexus Wave Technologies reserves the right to modify these Terms at " +
                    "any time. We will notify you of significant changes by updating the " +
                    "\"Last Updated\" date. Your continued use of the Application after any " +
                    "changes indicates your acceptance of the updated Terms."
            )

            LegalSection(
                title = "12. Governing Law",
                content = "These Terms shall be governed by and construed in accordance with " +
                    "applicable laws. Any disputes arising from or related to these Terms or " +
                    "the Application shall be resolved through binding arbitration, except " +
                    "where prohibited by law."
            )

            HorizontalDivider()

            Text(
                text = "© 2025 Nexus Wave Technologies. All rights reserved.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
