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

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Nexus Plus Premium", onBack = onBack)

        if (state.isPremium) {
            // Already premium
            PremiumActiveView(onBack = onBack)
            return@Column
        }

        if (state.submitSuccess) {
            // Payment submitted
            SubmitSuccessView(onBack = onBack)
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
                    listOf("No ads", "All 40+ features unlocked", "Unlimited AI Chat", "Priority support").forEach { b ->
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

            HorizontalDivider()

            // After payment — enter transaction ID
            Text(
                "After payment, enter your UPI Transaction ID below:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value         = state.transactionId,
                onValueChange = viewModel::setTransactionId,
                label         = { Text("UPI Transaction ID") },
                placeholder   = { Text("e.g. 123456789012") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Enter the UPI transaction ID from your payment app"
                },
                leadingIcon   = { Icon(Icons.Filled.ConfirmationNumber, null) },
            )

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

            Button(
                onClick  = { viewModel.submitPayment() },
                enabled  = !state.isSubmitting && state.transactionId.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp).semantics { contentDescription = "Submit payment for verification" },
                shape    = MaterialTheme.shapes.large,
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Submitting…")
                } else {
                    Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Submit for Verification", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                "Our team verifies payments within 24 hours. You'll receive premium access after verification.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun SubmitSuccessView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.HourglassTop, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Payment Submitted!", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Your payment is under review. Premium access will be activated within 24 hours.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Back to App") }
    }
}
