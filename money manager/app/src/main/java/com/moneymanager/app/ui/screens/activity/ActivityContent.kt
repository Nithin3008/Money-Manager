package com.moneymanager.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun LazyListScope.activityContent(
    state: FinanceUiState,
    onScanNow: () -> Unit,
    onPopulateThreeMonths: () -> Unit,
    onDateFilterSelected: (ActivityDateFilter, LocalDate?, LocalDate?) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    item { LargeTitle("Activity", "Review transactions for today or pick another period.") }
    item {
        ActivityScanPanel(
            state = state,
            onScanNow = onScanNow,
            onPopulateThreeMonths = onPopulateThreeMonths
        )
    }
    item {
        ActivityDateFilterPanel(
            state = state,
            onDateFilterSelected = onDateFilterSelected
        )
    }
    item {
        SearchBarSurface("Search in selected period")
    }
    if (state.activityTransactions.isEmpty()) {
        item { EmptyPanel("No transactions in this period.") }
    } else {
        itemsIndexed(
            items = state.pagedTransactions,
            key = { index, transaction -> "${transaction.id}_${transaction.timestampMillis}_$index" }
        ) { _, transaction ->
            TransactionRow(transaction = transaction, state = state, onSelect = onEditTransaction)
        }
        if (state.hasMoreTransactions) {
            item {
                Button(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
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
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Message Import", if (state.isScanningMessages) "Scanning" else "Ready")
            Text(
                if (state.scanStatusMessage.isBlank()) {
                    "Scan the selected period or populate the last 3 months from SMS."
                } else {
                    state.scanStatusMessage
                },
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScanNow,
                    enabled = !state.isScanningMessages,
                    modifier = Modifier.weight(1f).height(52.dp),
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
                    modifier = Modifier.weight(1f).height(52.dp),
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

@Composable
private fun ActivityDateFilterPanel(
    state: FinanceUiState,
    onDateFilterSelected: (ActivityDateFilter, LocalDate?, LocalDate?) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val today = LocalDate.now()
    var startDate by remember(state.activityStartDate) { mutableStateOf(state.activityStartDate) }
    var endDate by remember(state.activityEndDate) { mutableStateOf(state.activityEndDate) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Period", "${formatter.format(state.activityStartDate)} - ${formatter.format(state.activityEndDate)}")
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(formatter.format(startDate), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(formatter.format(endDate), maxLines = 1, overflow = TextOverflow.Ellipsis)
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

@Composable
private fun SearchBarSurface(placeholder: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(if (isAmoledTheme()) 16.dp else 10.dp),
        colors = CardDefaults.cardColors(containerColor = if (isAmoledTheme()) com.moneymanager.app.ui.theme.Navy850 else androidx.compose.ui.graphics.Color.White),
        border = BorderStroke(1.dp, appBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = TextDim)
            Spacer(Modifier.width(8.dp))
            Text(placeholder, color = TextDim, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
