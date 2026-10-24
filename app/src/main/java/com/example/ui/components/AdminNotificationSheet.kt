package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notification.DailyNotificationScheduler
import com.example.notification.NotificationSlotConfig
import com.example.notification.ScheduledNotificationPrefs
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardFill
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinDark
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.RubyError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { ScheduledNotificationPrefs(context) }

    var isAuthenticated by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // State cached from prefs
    var isMasterEnabled by remember { mutableStateOf(prefs.isMasterEnabled) }
    var slots by remember { mutableStateOf(prefs.getAllSlotConfigs()) }
    var logs by remember { mutableStateOf(prefs.getDeliveryLogs()) }

    var editingSlot by remember { mutableStateOf<NotificationSlotConfig?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepPurpleDark,
        dragHandle = null,
        modifier = Modifier.testTag("admin_notification_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
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
                            .background(
                                Brush.radialGradient(
                                    listOf(PurplePrimary.copy(alpha = 0.6f), Color(0xFF1B0E3E))
                                )
                            )
                            .border(1.dp, GoldCoin.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Shield",
                            tint = GoldCoin,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Admin Notifications",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAuthenticated) "Authorized Control Panel" else "Restricted Access",
                            color = if (isAuthenticated) EmeraldSuccess else TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("admin_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            if (!isAuthenticated) {
                // PIN Gate
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GlassCardFill),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = GoldCoin,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Admin Authentication",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "Enter your administrator PIN to configure daily notification schedules, delivery times, and test alerts.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                pinInput = it
                                pinError = null
                            },
                            label = { Text("Admin PIN") },
                            placeholder = { Text("Default: 7788") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = pinError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = GlassCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_pin_input")
                        )

                        if (pinError != null) {
                            Text(
                                text = pinError ?: "",
                                color = RubyError,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (pinInput == prefs.adminPin) {
                                    isAuthenticated = true
                                    pinError = null
                                } else {
                                    pinError = "Incorrect PIN. Please try again."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_auth_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurplePrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Unlock Controls", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Authenticated Controls

                // Master Schedule Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GlassCardFill),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isMasterEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (isMasterEnabled) EmeraldSuccess else RubyError,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "3x Daily Automated Schedule",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Target Timezone: Asia/Kolkata (IST)",
                                        color = GoldCoin,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Switch(
                                checked = isMasterEnabled,
                                onCheckedChange = { checked ->
                                    isMasterEnabled = checked
                                    prefs.isMasterEnabled = checked
                                    DailyNotificationScheduler.rescheduleAll(context)
                                    slots = prefs.getAllSlotConfigs()
                                    Toast.makeText(
                                        context,
                                        if (checked) "Daily schedule activated" else "Daily schedule disabled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldSuccess,
                                    checkedTrackColor = EmeraldSuccess.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SurfaceDark
                                ),
                                modifier = Modifier.testTag("master_notification_switch")
                            )
                        }

                        Text(
                            text = "Controls the automatic 9:00 AM, 12:00 PM, and 5:00 PM scheduled notification alarms on Indian Standard Time.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Text(
                    text = "Scheduled Slots",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // List the 3 slots
                slots.forEach { slot ->
                    SlotConfigCard(
                        slot = slot,
                        onToggle = { enabled ->
                            prefs.setSlotEnabled(slot.id, enabled)
                            DailyNotificationScheduler.rescheduleAll(context)
                            slots = prefs.getAllSlotConfigs()
                        },
                        onEdit = { editingSlot = slot },
                        onSendTest = {
                            DailyNotificationScheduler.sendTestNotification(context, slot.id)
                            logs = prefs.getDeliveryLogs()
                            Toast.makeText(context, "Test alert sent for Slot ${slot.id}!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Scheduling Status & System Actions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GlassCardFill),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "AlarmManager Sync & Status",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (isMasterEnabled) {
                                "Active • Alarms registered in Android AlarmManager with RTC_WAKEUP & Asia/Kolkata offset. Device reboot and clock change handlers active."
                            } else {
                                "Paused • Master schedule switch is currently disabled."
                            },
                            color = if (isMasterEnabled) EmeraldSuccess else TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    DailyNotificationScheduler.rescheduleAll(context)
                                    slots = prefs.getAllSlotConfigs()
                                    Toast.makeText(context, "All alarms re-synced", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resync Alarms", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    prefs.clearLogs()
                                    logs = emptyList()
                                    Toast.makeText(context, "Delivery logs cleared", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear Logs", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Delivery Logs Section
                if (logs.isNotEmpty()) {
                    Text(
                        text = "Recent Delivery Logs",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        logs.take(5).forEach { log ->
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.US).format(Date(log.timestampMillis))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceDark.copy(alpha = 0.5f))
                                    .border(1.dp, GlassCardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.title,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Slot #${log.slotId} • $dateStr",
                                            color = TextMuted,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (log.isPreview) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(PurplePrimary.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("TEST", color = GoldCoin, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Slot Dialog
    if (editingSlot != null) {
        val current = editingSlot!!
        var editedHour by remember(current) { mutableStateOf(current.hour.toString()) }
        var editedMinute by remember(current) { mutableStateOf(current.minute.toString()) }
        var editedTitle by remember(current) { mutableStateOf(current.title) }
        var editedMessage by remember(current) { mutableStateOf(current.message) }
        var editError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = {
                Text(
                    text = "Edit Slot #${current.id}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Customize delivery time (24h format) and copy:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editedHour,
                            onValueChange = { editedHour = it.filter { char -> char.isDigit() } },
                            label = { Text("Hour (0-23)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editedMinute,
                            onValueChange = { editedMinute = it.filter { char -> char.isDigit() } },
                            label = { Text("Minute (0-59)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Notification Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editedMessage,
                        onValueChange = { editedMessage = it },
                        label = { Text("Notification Message") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editError != null) {
                        Text(text = editError ?: "", color = RubyError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h = editedHour.toIntOrNull()
                        val m = editedMinute.toIntOrNull()
                        if (h == null || h !in 0..23) {
                            editError = "Invalid hour (must be 0-23)"
                            return@Button
                        }
                        if (m == null || m !in 0..59) {
                            editError = "Invalid minute (must be 0-59)"
                            return@Button
                        }
                        if (editedTitle.isBlank()) {
                            editError = "Title cannot be empty"
                            return@Button
                        }
                        if (editedMessage.isBlank()) {
                            editError = "Message cannot be empty"
                            return@Button
                        }

                        val updated = current.copy(
                            hour = h,
                            minute = m,
                            title = editedTitle.trim(),
                            message = editedMessage.trim()
                        )
                        prefs.saveSlotConfig(updated)
                        DailyNotificationScheduler.scheduleSlot(context, updated)
                        slots = prefs.getAllSlotConfigs()
                        editingSlot = null
                        Toast.makeText(context, "Slot #${updated.id} updated & rescheduled", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSlot = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1E153D),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SlotConfigCard(
    slot: NotificationSlotConfig,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onSendTest: () -> Unit
) {
    val nextTimeFormatted = remember(slot.hour, slot.minute) {
        DailyNotificationScheduler.getNextFormattedTime(slot.hour, slot.minute)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCardFill),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldCoinDark.copy(alpha = 0.2f))
                            .border(1.dp, GoldCoin.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = slot.formattedTime + " IST",
                            color = GoldCoin,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Slot #${slot.id}",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = slot.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldSuccess,
                        checkedTrackColor = EmeraldSuccess.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceDark
                    )
                )
            }

            Text(
                text = slot.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = slot.message,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Next: $nextTimeFormatted",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSendTest,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Test Notification",
                        modifier = Modifier.size(14.dp),
                        tint = GoldCoin
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Alert", fontSize = 11.sp, color = GoldCoin)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Slot",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.sp)
                }
            }
        }
    }
}
