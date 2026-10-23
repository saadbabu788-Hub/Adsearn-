package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MIN_WITHDRAWAL_INR
import com.example.data.WithdrawalRequest
import com.example.data.WithdrawalStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardFill
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinDark
import com.example.ui.theme.GoldCoinLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondary
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalSheet(
    availableBalance: Double,
    withdrawalRequests: List<WithdrawalRequest>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSubmitWithdrawal: (Double, String, String?, (Boolean, String) -> Unit) -> Unit
) {
    var upiIdInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var amountInput by remember {
        mutableStateOf(
            if (availableBalance >= MIN_WITHDRAWAL_INR) availableBalance.toInt().toString() else "500"
        )
    }

    var isSubmitting by remember { mutableStateOf(false) }
    var submissionSuccessMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isEligible = availableBalance >= MIN_WITHDRAWAL_INR
    val progressToMin = (availableBalance / MIN_WITHDRAWAL_INR).toFloat().coerceIn(0f, 1f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepPurpleDark,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Sheet Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess.copy(alpha = 0.2f))
                            .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Withdraw",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Reward Withdrawal",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "UPI Instant Transfer Review",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_withdrawal_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // Balance & Progress Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0x35047857), Color(0x25064E3B), CosmicNavy)
                        )
                    )
                    .border(1.5.dp, EmeraldSuccess.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ELIGIBLE REWARD BALANCE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isEligible) EmeraldSuccess.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f)
                                )
                                .border(
                                    1.dp,
                                    if (isEligible) EmeraldSuccess.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isEligible) "Eligible to Withdraw" else "Min. ₹500 Required",
                                color = if (isEligible) EmeraldSuccess else AmberWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "₹${availableBalance.toInt()}",
                            color = TextPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ".${String.format(Locale.US, "%02d", ((availableBalance - availableBalance.toInt()) * 100).toInt())}",
                            color = EmeraldSuccess,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Progress to Minimum ₹500
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isEligible) "Minimum requirement unlocked!" else "₹${availableBalance.toInt()} / ₹${MIN_WITHDRAWAL_INR.toInt()}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${(progressToMin * 100).toInt()}%",
                                color = if (isEligible) EmeraldSuccess else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progressToMin },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isEligible) EmeraldSuccess else GoldCoin,
                            trackColor = Color(0x33FFFFFF)
                        )
                    }
                }
            }

            // Confirmation Message Banner
            AnimatedVisibility(visible = submissionSuccessMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.15f))
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = submissionSuccessMessage ?: "",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Error Message Banner
            AnimatedVisibility(visible = errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RubyError.copy(alpha = 0.15f))
                        .border(1.dp, RubyError.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = RubyError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Withdrawal Form
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassCardFill)
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "NEW WITHDRAWAL REQUEST",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // UPI ID Field (Required)
                    OutlinedTextField(
                        value = upiIdInput,
                        onValueChange = {
                            upiIdInput = it
                            errorMessage = null
                        },
                        label = { Text("UPI ID (Required)") },
                        placeholder = { Text("e.g. yourname@okhdfcbank or 9876543210@paytm") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = PurpleSecondary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdrawal_upi_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PurpleSecondary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = PurplePrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Email Address Field (Optional)
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            errorMessage = null
                        },
                        label = { Text("Email Address (Optional)") },
                        placeholder = { Text("For payment review & transfer receipt") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = CyanAccent
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdrawal_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PurpleSecondary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = PurplePrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Withdrawal Amount Field
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Withdrawal Amount (₹)") },
                        placeholder = { Text("Minimum ₹500") },
                        leadingIcon = {
                            Text(
                                text = "₹",
                                color = EmeraldSuccess,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdrawal_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldSuccess,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = EmeraldSuccess,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = EmeraldSuccess
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Quick Chips
                    if (isEligible) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            QuickAmountChip(
                                label = "₹500",
                                isSelected = amountInput == "500",
                                onClick = { amountInput = "500" }
                            )
                            if (availableBalance >= 1000) {
                                QuickAmountChip(
                                    label = "₹1000",
                                    isSelected = amountInput == "1000",
                                    onClick = { amountInput = "1000" }
                                )
                            }
                            QuickAmountChip(
                                label = "Full (₹${availableBalance.toInt()})",
                                isSelected = amountInput == availableBalance.toInt().toString(),
                                onClick = { amountInput = availableBalance.toInt().toString() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            val parsedAmount = amountInput.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount < MIN_WITHDRAWAL_INR) {
                                errorMessage = "Minimum withdrawal is ₹${MIN_WITHDRAWAL_INR.toInt()}."
                                return@Button
                            }
                            if (parsedAmount > availableBalance) {
                                errorMessage = "Requested amount exceeds your available balance of ₹${availableBalance.toInt()}."
                                return@Button
                            }
                            if (upiIdInput.isBlank() || !upiIdInput.contains("@")) {
                                errorMessage = "Please enter a valid UPI ID (e.g. mobile@upi, username@bank)."
                                return@Button
                            }

                            isSubmitting = true
                            errorMessage = null
                            onSubmitWithdrawal(parsedAmount, upiIdInput, emailInput) { success, msg ->
                                isSubmitting = false
                                if (success) {
                                    submissionSuccessMessage = "Your withdrawal request has been submitted successfully and is pending review."
                                    upiIdInput = ""
                                    emailInput = ""
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        enabled = !isSubmitting && isEligible,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_withdrawal_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            disabledContainerColor = Color(0x3310B981)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verifying Request...", color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEligible) "Submit Withdrawal Request" else "Need ₹${(MIN_WITHDRAWAL_INR - availableBalance).toInt()} more to withdraw",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Withdrawal History Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassCardFill)
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WITHDRAWAL HISTORY",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (withdrawalRequests.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No withdrawal requests yet",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Complete verified ads to reach ₹500 and request payout.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            withdrawalRequests.forEach { req ->
                                WithdrawalHistoryItem(request = req)
                            }
                        }
                    }
                }
            }

            // Verification & Anti-Fraud Notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1806B6D4))
                    .border(1.dp, Color(0x3506B6D4), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Genuine Review Policy",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "All withdrawal requests are kept in Pending status until verified against ad telemetry servers. To prevent duplicate credits and fraud, payouts are dispatched via genuine bank UPI rails once reviewed.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAmountChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) EmeraldSuccess.copy(alpha = 0.25f) else Color(0x1AFFFFFF))
            .border(
                1.dp,
                if (isSelected) EmeraldSuccess else Color(0x33FFFFFF),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) EmeraldSuccess else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WithdrawalHistoryItem(request: WithdrawalRequest) {
    val dateStr = remember(request.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(request.timestamp))
    }

    val (statusColor, statusIcon) = when (request.status) {
        WithdrawalStatus.PENDING -> AmberWarning to Icons.Default.HourglassEmpty
        WithdrawalStatus.APPROVED -> CyanAccent to Icons.Default.CheckCircle
        WithdrawalStatus.PROCESSING -> PurpleSecondary to Icons.Default.Sync
        WithdrawalStatus.PAID -> EmeraldSuccess to Icons.Default.CheckCircle
        WithdrawalStatus.REJECTED -> RubyError to Icons.Default.Close
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${request.amountInr.toInt()}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = request.status.displayName,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "UPI: ${request.maskedUpi}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = dateStr,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            if (request.status == WithdrawalStatus.REJECTED) {
                Text(
                    text = "Note: Request rejected. Amount has been returned to your eligible balance.",
                    color = RubyError,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
