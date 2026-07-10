package com.moneymanager.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.MonthlyCategoryTotal
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.month
import com.moneymanager.app.model.shortLabel
import com.moneymanager.app.ui.theme.LineColor
import com.moneymanager.app.ui.theme.LossRed
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.Navy850
import com.moneymanager.app.ui.theme.Navy900
import com.moneymanager.app.ui.theme.OnAccent
import com.moneymanager.app.ui.theme.PrimaryBlue
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import com.moneymanager.app.ui.theme.WarningAmber
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal fun LazyListScope.summaryContent(
    state: FinanceUiState,
    onMonthSelected: (YearMonth) -> Unit,
    onToggleSummaryAccount: (Long) -> Unit,
    onClearSummaryAccountFilter: () -> Unit
) {
    item {
        ReportsHeader(state, onMonthSelected)
    }
    if (state.accounts.isNotEmpty()) {
        item {
            SummaryAccountFilterRow(
                state = state,
                onToggleAccount = onToggleSummaryAccount,
                onClearFilter = onClearSummaryAccountFilter
            )
        }
    }
    item { MetricGrid(state = state) }
    item { CashFlowCard(state) }
    item {
        val totals = remember(state.monthTransactions, state.categories) { categoryTotals(state) }
        CategoryDonutCard(state, totals)
    }
    item { StatementCheckCard(state) }
}

@Composable
private fun ReportsHeader(state: FinanceUiState, onMonthSelected: (YearMonth) -> Unit) {
    var showMonthMenu by remember { mutableStateOf(false) }
    val months = remember(state.transactions) { availableMonths(state) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Reports",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.weight(1f)
        )
        Box {
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Navy850)
                    .clickable { showMonthMenu = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
                Text(
                    state.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    color = TextMuted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            DropdownMenu(expanded = showMonthMenu, onDismissRequest = { showMonthMenu = false }) {
                months.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month.shortLabel()) },
                        onClick = {
                            showMonthMenu = false
                            onMonthSelected(month)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryAccountFilterRow(
    state: FinanceUiState,
    onToggleAccount: (Long) -> Unit,
    onClearFilter: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccountFilterPill(
            label = "All accounts",
            selected = state.summarySelectedAccountIds.isEmpty(),
            onClick = onClearFilter
        )
        state.accounts.forEach { account ->
            AccountFilterPill(
                label = account.name,
                selected = account.id in state.summarySelectedAccountIds,
                onClick = { onToggleAccount(account.id) }
            )
        }
    }
}

@Composable
private fun AccountFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PrimaryBlue else Navy800)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) OnAccent else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun MetricGrid(state: FinanceUiState) {
    val currency = state.currency
    val net = state.monthReportNet
    val netLabel = if (net >= 0) "+${money(net, currency)}" else "-${money(-net, currency)}"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("Spent", money(state.monthExpense, currency), LossRed, Modifier.weight(1f))
            MetricTile("Income", money(state.monthReportIncome, currency), MoneyGreen, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("Net saved", netLabel, TextPrimary, Modifier.weight(1f))
            MetricTile("Closing bal.", money(state.balanceAtEndOfSelectedMonth, currency), TextPrimary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Navy850)
            .padding(16.dp)
    ) {
        Text(label, color = TextDim, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        Text(
            value,
            color = valueColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun CashFlowCard(state: FinanceUiState) {
    val months = remember(state.selectedMonth) {
        (5 downTo 0).map { state.selectedMonth.minusMonths(it.toLong()) }
    }
    val monthTotals = remember(state.transactions, months) {
        months.map { month ->
            val txns = state.transactions.filter {
                it.month() == month && !it.excludeFromSummary && !state.isInvestmentTransaction(it)
            }
            Triple(
                month,
                txns.filter { it.type == TransactionType.Income }.sumOf { it.amount },
                txns.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
            )
        }
    }
    val maxTotal = monthTotals.maxOf { (it.second + it.third) }.coerceAtLeast(1.0)

    ReportCard {
        Text("Cash flow", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Last 6 months", color = TextDim, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            monthTotals.forEach { (month, income, expense) ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val inFraction = (income / maxTotal).toFloat().coerceIn(0.02f, 1f)
                    val outFraction = (expense / maxTotal).toFloat().coerceIn(0.02f, 1f)
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.Bottom)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(inFraction * 0.85f)
                                .clip(RoundedCornerShape(5.dp))
                                .background(PrimaryBlue)
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((88 * outFraction * 0.85f).dp.coerceAtLeast(2.dp))
                                .clip(RoundedCornerShape(5.dp))
                                .background(LossRed.copy(alpha = 0.85f))
                        )
                    }
                    Text(
                        month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        color = TextDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChartLegend(PrimaryBlue, "Income")
            ChartLegend(LossRed, "Expense")
        }
    }
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ReportCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        border = BorderStroke(1.dp, LineColor)
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun CategoryDonutCard(state: FinanceUiState, totals: List<MonthlyCategoryTotal>) {
    ReportCard {
        Text("Where it went", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        val expenses = totals.filter { it.expense > 0.0 }.sortedByDescending { it.expense }
        val total = expenses.sumOf { it.expense }
        if (total <= 0.0) {
            Text(
                "No category expenses this month.",
                color = TextDim,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        } else {
            val slices = donutSlices(expenses)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val ringWidth = 24.dp.toPx()
                        val inset = ringWidth / 2f
                        val arcSize = Size(size.width - ringWidth, size.height - ringWidth)
                        var startAngle = -90f
                        slices.forEach { slice ->
                            val sweep = ((slice.amount / total) * 360f).toFloat()
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = ringWidth)
                            )
                            startAngle += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text(
                            compactMoney(total, state.currency),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    slices.forEach { slice ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(Modifier.size(11.dp).clip(RoundedCornerShape(4.dp)).background(slice.color))
                            Text(
                                slice.label,
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${((slice.amount / total) * 100).toInt()}%",
                                color = TextDim,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DonutSlice(
    val label: String,
    val amount: Double,
    val color: Color
)

@Composable
private fun donutSlices(expenses: List<MonthlyCategoryTotal>): List<DonutSlice> {
    val palette = listOf(
        PrimaryBlue,
        WarningAmber,
        LossRed,
        if (isDarkTheme()) Color(0xFF7EA2FF) else Color(0xFF7C9EE0)
    )
    val visible = expenses.take(3).mapIndexed { index, item ->
        DonutSlice(item.category.name, item.expense, palette[index])
    }
    val otherAmount = expenses.drop(3).sumOf { it.expense }
    return if (otherAmount > 0.0) {
        visible + DonutSlice("Other", otherAmount, palette[3])
    } else {
        visible
    }
}

@Composable
private fun StatementCheckCard(state: FinanceUiState) {
    val gap = state.selectedMonthReconciliationGap
    val movementColor = if (state.calendarMonthNet >= 0.0) PrimaryBlue else LossRed
    ReportCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Statement check", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(state.selectedMonth.shortLabel(), color = TextDim, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatementMetricRow("Current bank balance", state.money(state.currentBalanceAnchor), PrimaryBlue)
            StatementMetricRow("Month closing balance", state.money(state.balanceAtEndOfSelectedMonth), TextPrimary)
            StatementMetricRow("Month movement", state.money(state.calendarMonthNet), movementColor)
            if (kotlin.math.abs(gap) > 0.01) {
                Text(
                    "Statement difference: ${state.money(gap)}",
                    color = WarningAmber,
                    fontSize = 12.5.sp
                )
            } else {
                Text(
                    "Statement looks balanced for the selected account.",
                    color = TextDim,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

@Composable
private fun StatementMetricRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextDim, fontSize = 12.5.sp)
        Text(value, color = color, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun availableMonths(state: FinanceUiState): List<YearMonth> {
    val fromTransactions = state.transactions.map { it.month() }
    return (fromTransactions + YearMonth.now()).distinct().sortedDescending()
}

private fun categoryTotals(state: FinanceUiState): List<MonthlyCategoryTotal> {
    return state.categories.map { category ->
        val expense = state.monthTransactions
            .filter { it.categoryId == category.id && it.type == TransactionType.Expense }
            .sumOf { it.amount }
        MonthlyCategoryTotal(
            category = category,
            income = 0.0,
            expense = expense
        )
    }
}
