package com.nexuswavetech.nexusplus.billing

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import org.koin.androidx.compose.koinViewModel

// ── SubscriptionViewModel ─────────────────────────────────────────────────────

class SubscriptionViewModel(private val repo: PremiumRepository) : androidx.lifecycle.ViewModel() {
    val premiumState = repo.premiumState
    val products     = repo.products
    val billingError = repo.billingError

    fun purchaseMonthly(activity: Activity) = repo.purchaseMonthly(activity)
    fun purchaseYearly(activity: Activity)  = repo.purchaseYearly(activity)
    suspend fun refresh() = repo.refresh()
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    val context    = LocalContext.current
    val activity   = context as? Activity
    val state      by viewModel.premiumState.collectAsState()
    val products   by viewModel.products.collectAsState()
    val error      by viewModel.billingError.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Nexus Plus Premium", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // Already premium
            if (state.isPremium) {
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Premium active",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "You're on Premium!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "All features are unlocked. Thank you for your support!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                return@Column
            }

            // Pending
            if (state is PremiumState.Pending) {
                Spacer(Modifier.weight(1f))
                CircularProgressIndicator()
                Text(
                    "Your purchase is being processed…",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.weight(1f))
                return@Column
            }

            // Header
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Unlock Nexus Plus Premium",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Text(
                "Get unlimited access to all 40+ features, AI tools, and more. Cancel anytime.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Benefits list
            val benefits = listOf(
                "Unlimited AI Chat (Aira)",
                "Full QR Code Generator",
                "Advanced Text Encryptor",
                "No ads",
                "Priority support",
                "New features first",
            )
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "What's included",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    benefits.forEach { benefit ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(benefit, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Billing error
            AnimatedVisibility(visible = error != null) {
                error?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Subscription options ───────────────────────────────────────
            Text(
                "Choose a plan",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )

            // Monthly plan
            PlanCard(
                title       = "Monthly",
                price       = "₹35 / month",
                badge       = null,
                description = "Billed every month. Cancel any time.",
                onClick     = { activity?.let { viewModel.purchaseMonthly(it) } },
                enabled     = activity != null,
                modifier    = Modifier.semantics { contentDescription = "Monthly plan: ₹35 per month. Tap to subscribe." },
            )

            // Yearly plan (save 30%)
            PlanCard(
                title       = "Yearly",
                price       = "₹300 / year",
                badge       = "Save 29%",
                description = "Best value. Only ₹25/month — billed yearly.",
                onClick     = { activity?.let { viewModel.purchaseYearly(it) } },
                enabled     = activity != null,
                modifier    = Modifier.semantics { contentDescription = "Yearly plan: ₹300 per year. Save 29 percent. Tap to subscribe." },
            )

            Text(
                "Subscriptions auto-renew. Cancel in Google Play any time before the next renewal.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Plan Card ─────────────────────────────────────────────────────────────────

@Composable
private fun PlanCard(
    title: String,
    price: String,
    badge: String?,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (badge != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    price,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = MaterialTheme.shapes.large,
            ) {
                Text("Subscribe", fontWeight = FontWeight.Bold)
            }
        }
    }
}
