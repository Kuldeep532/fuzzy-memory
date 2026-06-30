package com.nexuswavetech.nexusplus.billing

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    viewModel: PaymentViewModel = koinViewModel(),
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Build UPI deep-link
    fun launchUpi(plan: PaymentPlan) {
        val amount = if (plan == PaymentPlan.MONTHLY) viewModel.monthlyAmount else viewModel.yearlyAmount
        val note   = "Nexus Plus ${plan.name.lowercase()} subscription"
        val uri    = Uri.parse(
            "upi://pay?pa=${viewModel.upiId}" +
            "&pn=${Uri.encode(viewModel.upiName)}" +
            "&am=$amount" +
            "&cu=INR" +
            "&tn=${Uri.encode(note)}"
        )
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    // Auto-verify when user returns to this screen after payment
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && state.autoVerifyPending.not() && state.isPremium.not() && state.submitSuccess.not()) {
                viewModel.onPaymentReturned()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Nexus Plus Premium", onBack = onBack)

        if (state.isPremium) {
            PremiumActiveView(onBack = onBack)
            return@Column
        }

        if (state.submitSuccess) {
            PremiumActivatedView(onBack = onBack)
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Icon(Icons.Filled.Star, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                "Unlock Nexus Plus Premium",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Text(
                "No ads. All features. Cancel any time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // Benefits
            Card(
                shape  = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("No ads", "All 45+ premium features unlocked", "Unlimited AI Chat", "Priority support", "Auto-verified payments").forEach { b ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(b, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Plan selector
            Text("Choose a plan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanCard(
                    title   = "Monthly",
                    price   = "₹${viewModel.monthlyAmount}",
                    sub     = "per month",
                    badge   = null,
                    selected = state.selectedPlan == PaymentPlan.MONTHLY,
                    onClick  = { viewModel.selectPlan(PaymentPlan.MONTHLY) },
                    modifier = Modifier.weight(1f),
                )
                PlanCard(
                    title   = "Yearly",
                    price   = "₹${viewModel.yearlyAmount}",
                    sub     = "per year",
                    badge   = "Save 29%",
                    selected = state.selectedPlan == PaymentPlan.YEARLY,
                    onClick  = { viewModel.selectPlan(PaymentPlan.YEARLY) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Pay via UPI button
            val amount = if (state.selectedPlan == PaymentPlan.MONTHLY) viewModel.monthlyAmount else viewModel.yearlyAmount
            Button(
                onClick  = { launchUpi(state.selectedPlan) },
                modifier = Modifier.fillMaxWidth().height(54.dp).semantics { contentDescription = "Pay ₹$amount via UPI" },
                shape    = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Filled.Payment, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pay ₹$amount via UPI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Text(
                "Supports Google Pay, PhonePe, Paytm, BHIM & all UPI apps.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Auto-verify info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        "After payment, return to Nexus Plus. Your premium will be verified automatically — no transaction ID needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            AnimatedVisibility(visible = state.errorMessage != null) {
                state.errorMessage?.let { err ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = MaterialTheme.shapes.large) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            if (state.isSubmitting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Verifying payment…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String, price: String, sub: String, badge: String?,
    selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    Card(
        onClick  = onClick,
        shape    = MaterialTheme.shapes.extraLarge,
        colors   = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant,
        ),
        border   = if (selected) CardDefaults.outlinedCardBorder() else null,
        modifier = modifier.semantics { contentDescription = "$title plan: $price $sub${if (badge != null) ". $badge" else ""}" },
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (badge != null) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) {
                    Text(badge, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            Text(price, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PremiumActiveView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("You're on Premium!", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("All features are unlocked. Thank you!", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onBack) { Text("Back to App") }
    }
}

@Composable
private fun PremiumActivatedView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Premium Activated!", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Payment verified automatically. Enjoy all premium features!", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Back to App") }
    }
}
