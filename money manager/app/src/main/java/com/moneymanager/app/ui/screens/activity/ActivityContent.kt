package com.moneymanager.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneymanager.app.model.ActivityDateFilter
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.ui.theme.PrimaryBlue
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

internal fun LazyListScope.activityContent(
    state: FinanceUiState,
    onScanNow: () -> Unit,
    onPopulateThreeMonths: () -> Unit,
    onDateFilterSelected: (ActivityDateFilter, LocalDate?, LocalDate?) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    item {
        LargeTitle("Transactions", "Track every payment, bank message, and manual entry.")
    }
    item {
        ActivityDateFilterPanel(
            state = state,
            onDateFilterSelected = onDateFilterSelected
        )
    }
    item {
        ActivityScanPanel(
            state = state,
            onScanNow = onScanNow,
            onPopulateThreeMonths = onPopulateThreeMonths
        )
    }
    if (state.activityTransactions.isEmpty()) {
        item { EmptyPanel("No transactions in this period.") }
    } else {
        itemsIndexed(
            items = state.pagedTransactions,
            key = { _, transaction -> transaction.id }
        ) { _, transaction ->
            TransactionRow(transaction = transaction, state = state, onSelect = onEditTransaction)
        }
        if (state.hasMoreTransactions) {
            item {
                Button(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("Load more", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ActivityScanPanel(
    state: FinanceUiState,
    onScanNow: () -> Unit,
    onPopulateThreeMonths: () -> Unit
) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.isScanningMessages, state.scanStartedAtMillis) {
        while (state.isScanningMessages) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val etaLabel = state.scanStartedAtMillis
        ?.takeIf { state.isScanningMessages }
        ?.let { startedAt ->
            estimatedRemainingLabel(
                elapsedMillis = nowMillis - startedAt,
                processed = state.scanProcessedCount,
                total = state.scanTotalCount
            )
        }
    val statusLabel = if (state.isScanningMessages) {
        etaLabel ?: "Estimating..."
    } else {
        "Ready"
    }

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Sms, PrimaryBlue)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Message import", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.scanStatusMessage.isBlank()) "Scan SMS and convert alerts into transactions." else state.scanStatusMessage,
                        color = TextDim,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(statusLabel, color = PrimaryBlue, style = MaterialTheme.typography.labelMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScanNow,
                    enabled = !state.isScanningMessages,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = primaryButtonColors()
                ) {
                    Icon(Icons.Rounded.Sms, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Scan", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onPopulateThreeMonths,
                    enabled = !state.isScanningMessages,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    border = BorderStroke(1.dp, PrimaryBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("3 Months", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

private fun estimatedRemainingLabel(elapsedMillis: Long, processed: Int, total: Int): String? {
    if (total <= 0) return null
    if (processed <= 0) return "ETA --:--"
    val remaining = (total - processed).coerceAtLeast(0)
    val remainingMillis = (elapsedMillis.toDouble() / processed.toDouble() * remaining.toDouble()).toLong()
    return "ETA ${formatDurationMillis(remainingMillis)}"
}

private fun formatDurationMillis(elapsedMillis: Long): String {
    val totalSeconds = (elapsedMillis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun ActivityDateFilterPanel(
    state: FinanceUiState,
    onDateFilterSelected: (ActivityDateFilter, LocalDate?, LocalDate?) -> Unit
) {
    val today = LocalDate.now()
    var startDate by remember(state.activityStartDate) { mutableStateOf(state.activityStartDate) }
    var endDate by remember(state.activityEndDate) { mutableStateOf(state.activityEndDate) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val dateRangeLabel = remember(state.activityStartDate, state.activityEndDate) {
        "${state.activityStartDate.mediumDateLabel()} - ${state.activityEndDate.mediumDateLabel()}"
    }

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                dateRangeLabel,
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            ChipRow {
                ActivityDateFilter.entries.forEach { filter ->
                    MoneyChip(
                        label = filter.label,
                        selected = state.activityDateFilter == filter,
                        onClick = { onDateFilterSelected(filter, startDate, endDate) }
                    )
                }
            }
            if (state.activityDateFilter == ActivityDateFilter.Custom) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, appBorderColor())
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(startDate.mediumDateLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, appBorderColor())
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(endDate.mediumDateLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        val context = LocalContext.current
        LaunchedEffect(showStartPicker) {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    startDate = LocalDate.of(year, month + 1, day)
                    if (startDate.isAfter(endDate)) endDate = startDate
                    onDateFilterSelected(ActivityDateFilter.Custom, startDate, endDate)
                    showStartPicker = false
                },
                startDate.year,
                startDate.monthValue - 1,
                startDate.dayOfMonth
            ).apply {
                datePicker.maxDate = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                show()
            }
        }
    }

    if (showEndPicker) {
        val context = LocalContext.current
        LaunchedEffect(showEndPicker) {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    endDate = LocalDate.of(year, month + 1, day).coerceAtMost(today)
                    if (startDate.isAfter(endDate)) startDate = endDate
                    onDateFilterSelected(ActivityDateFilter.Custom, startDate, endDate)
                    showEndPicker = false
                },
                endDate.year,
                endDate.monthValue - 1,
                endDate.dayOfMonth
            ).apply {
                datePicker.maxDate = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                show()
            }
        }
    }
}
