package com.moneymanager.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.model.ActivityDateFilter
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.transactionDate
import com.moneymanager.app.ui.theme.LineColor
import com.moneymanager.app.ui.theme.LossRed
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.Navy850
import com.moneymanager.app.ui.theme.OnAccent
import com.moneymanager.app.ui.theme.PrimaryBlue
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val activityDayFormatter = DateTimeFormatter.ofPattern("d MMM")

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
        ActivityHeader(state, onScanNow, onPopulateThreeMonths)
    }
    item {
        ActivityDateFilterRow(state, onDateFilterSelected)
    }
    if (state.scanStatusMessage.isNotBlank() || state.isScanningMessages) {
        item {
            Text(
                if (state.isScanningMessages) "Scanning messages..." else state.scanStatusMessage,
                color = TextDim,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
    item {
        ActivitySummaryStrip(state)
    }
    if (state.activityTransactions.isEmpty()) {
        item {
            ActivityEmptyState(onScanNow)
        }
    } else {
        val groups = state.pagedTransactions.groupBy { it.transactionDate() }
        groups.forEach { (day, transactions) ->
            item(key = "day_header_$day") {
                ActivityDayHeader(day = day, transactions = transactions, state = state)
            }
            transactions.forEach { transaction ->
                item(key = "activity_txn_${transaction.id}") {
                    TransactionRow(transaction = transaction, state = state, onSelect = onEditTransaction)
                }
            }
        }
        if (state.hasMoreTransactions) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .drawBehind {
                            drawRoundRect(
                                color = LineColor,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        }
                        .clickable(onClick = onLoadMore),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Load more", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ActivityHeader(
    state: FinanceUiState,
    onScanNow: () -> Unit,
    onPopulateThreeMonths: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Transactions",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Navy850)
                .clickable(enabled = !state.isScanningMessages, onClick = onPopulateThreeMonths),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = "Backfill 3 months",
                tint = TextMuted,
                modifier = Modifier.size(21.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Navy850)
                .clickable(enabled = !state.isScanningMessages, onClick = onScanNow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Sync,
                contentDescription = "Scan SMS",
                tint = TextMuted,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun ActivityDateFilterRow(
    state: FinanceUiState,
    onDateFilterSelected: (ActivityDateFilter, LocalDate?, LocalDate?) -> Unit
) {
    val today = LocalDate.now()
    var startDate by remember(state.activityStartDate) { mutableStateOf(state.activityStartDate) }
    var endDate by remember(state.activityEndDate) { mutableStateOf(state.activityEndDate) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateRangePill("Today", state.activityDateFilter == ActivityDateFilter.Today) {
                onDateFilterSelected(ActivityDateFilter.Today, startDate, endDate)
            }
            DateRangePill("7 days", state.activityDateFilter == ActivityDateFilter.Week) {
                onDateFilterSelected(ActivityDateFilter.Week, startDate, endDate)
            }
            DateRangePill("Month", state.activityDateFilter == ActivityDateFilter.Month) {
                onDateFilterSelected(ActivityDateFilter.Month, startDate, endDate)
            }
            val customSelected = state.activityDateFilter == ActivityDateFilter.Custom
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (customSelected) PrimaryBlue else Navy800)
                    .clickable { onDateFilterSelected(ActivityDateFilter.Custom, startDate, endDate) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = "Custom range",
                    tint = if (customSelected) OnAccent else TextMuted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        if (state.activityDateFilter == ActivityDateFilter.Custom) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, LineColor)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(startDate.mediumDateLabel(), color = TextMuted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, LineColor)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(endDate.mediumDateLabel(), color = TextMuted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun DateRangePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PrimaryBlue else Navy800)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) OnAccent else TextMuted,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun ActivitySummaryStrip(state: FinanceUiState) {
    val moneyIn = remember(state.activityTransactions) {
        state.activityTransactions.filter { it.type == TransactionType.Income }.sumOf { it.amount }
    }
    val moneyOut = remember(state.activityTransactions) {
        state.activityTransactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Navy850)
                .padding(14.dp)
        ) {
            Text("Money in", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(
                "+${state.money(moneyIn)}",
                color = MoneyGreen,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Navy850)
                .padding(14.dp)
        ) {
            Text("Money out", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(
                "-${state.money(moneyOut)}",
                color = LossRed,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ActivityDayHeader(
    day: LocalDate,
    transactions: List<LedgerTransaction>,
    state: FinanceUiState
) {
    val today = LocalDate.now()
    val dayLabel = when (day) {
        today -> "Today · ${day.format(activityDayFormatter)}"
        today.minusDays(1) -> "Yesterday · ${day.format(activityDayFormatter)}"
        else -> day.format(activityDayFormatter)
    }
    val net = transactions.sumOf { if (it.type == TransactionType.Income) it.amount else -it.amount }
    val netLabel = if (net >= 0) "+${state.money(net)}" else "-${state.money(-net)}"

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(dayLabel, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(LineColor)
        )
        Text(netLabel, color = TextDim, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActivityEmptyState(onScanNow: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .drawBehind {
                drawRoundRect(
                    color = LineColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
            .padding(horizontal = 20.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Navy850),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SearchOff, contentDescription = null, tint = TextDim, modifier = Modifier.size(28.dp))
        }
        Text(
            "Nothing in this range",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            "Try a wider date filter or scan your\nSMS inbox for missed alerts.",
            color = TextDim,
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp)
        )
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(PrimaryBlue)
                .clickable(onClick = onScanNow)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Sms, contentDescription = null, tint = OnAccent, modifier = Modifier.size(18.dp))
            Text("Scan SMS", color = OnAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
