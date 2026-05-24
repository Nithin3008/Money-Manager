package com.moneymanager.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.MonthlyCategoryTotal
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.month
import com.moneymanager.app.model.shortLabel
import com.moneymanager.app.model.transactionDate
import com.moneymanager.app.ui.theme.LossRed
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.PrimaryBlue
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import com.moneymanager.app.ui.theme.WarningAmber
import java.time.LocalDate
import java.time.YearMonth

internal fun LazyListScope.summaryContent(
    state: FinanceUiState,
    onMonthSelected: (YearMonth) -> Unit,
    onToggleSummaryAccount: (Long) -> Unit,
    onClearSummaryAccountFilter: () -> Unit
) {
    item { LargeTitle("Statistics", state.selectedMonth.shortLabel()) }
    item {
        MonthSelector(
            months = availableMonths(state),
            selected = state.selectedMonth,
            onSelected = onMonthSelected
        )
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
    item { ReportHeroCard(state) }
    item { MetricGrid(state = state) }
    item { CashFlowGraph(state) }
    item { DailyExpenseBarGraph(state) }
    item { CategoryPieChart(state, categoryTotals(state)) }
    item { MonthBalanceStrip(state) }
}

@Composable
private fun ReportHeroCard(state: FinanceUiState) {
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Cash flow", "Income vs spend")
            StatisticLineChart(state)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReportStat("Income", state.money(state.monthReportIncome), MoneyGreen, Modifier.weight(1f))
                ReportStat("Expense", state.money(state.monthExpense), LossRed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatisticLineChart(state: FinanceUiState) {
    val months = (5 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) }
    val income = months.map { month ->
        state.transactions.filter { it.month() == month && it.type == TransactionType.Income }.sumOf { it.amount }
    }
    val expense = months.map { month ->
        state.transactions.filter { it.month() == month && it.type == TransactionType.Expense }.sumOf { it.amount }
    }
    val max = (income + expense).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        fun point(index: Int, value: Double): Offset {
            val x = if (months.size == 1) 0f else size.width * index / (months.lastIndex)
            val y = size.height - ((value / max).toFloat().coerceIn(0f, 1f) * (size.height - 28f)) - 14f
            return Offset(x, y)
        }

        repeat(4) { row ->
            val y = size.height * (row + 1) / 5f
            drawLine(Color(0xFF2C2C2C), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
        }

        fun drawSeries(values: List<Double>, color: Color) {
            if (values.isEmpty()) return
            val path = Path().apply {
                moveTo(point(0, values[0]).x, point(0, values[0]).y)
                values.drop(1).forEachIndexed { index, value ->
                    lineTo(point(index + 1, value).x, point(index + 1, value).y)
                }
            }
            drawPath(path, color, style = Stroke(width = 5f, cap = StrokeCap.Round))
            values.forEachIndexed { index, value ->
                drawCircle(color, radius = 6f, center = point(index, value))
            }
        }

        drawSeries(expense, WarningAmber)
        drawSeries(income, PrimaryBlue)
    }
}

@Composable
private fun ReportStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(Navy800, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = color, style = MaterialTheme.typography.titleLarge, maxLines = 1)
    }
}

@Composable
private fun MetricGrid(state: FinanceUiState) {
    val currency = state.currency
    val creditCardActivity = state.transactions
        .filter {
            it.isCreditCardTransaction &&
                YearMonth.from(it.transactionDate()) == state.selectedMonth &&
                (state.activeSummaryAccountIds.isEmpty() || it.accountId in state.activeSummaryAccountIds)
        }
        .sumOf { it.amount }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetric("Net", money(state.monthReportNet, currency), if (state.monthReportNet >= 0) MoneyGreen else LossRed, Modifier.weight(1f))
            SmallMetric("Balance", money(state.currentBalanceAnchor, currency), PrimaryBlue, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetric("Opening", money(state.selectedMonthOpeningBalance, currency), TextPrimary, Modifier.weight(1f))
            SmallMetric("Credit card", money(creditCardActivity, currency), WarningAmber, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryAccountFilterRow(
    state: FinanceUiState,
    onToggleAccount: (Long) -> Unit,
    onClearFilter: () -> Unit
) {
    val defaultAccount = state.accounts.firstOrNull { it.id == state.defaultAccountId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelText("ACCOUNTS")
        ChipRow {
            MoneyChip(
                label = defaultAccount?.let { "Default: ${it.name}" } ?: "All",
                selected = state.summarySelectedAccountIds.isEmpty(),
                onClick = onClearFilter
            )
            state.accounts.forEach { account ->
                MoneyChip(
                    label = account.name,
                    selected = account.id in state.summarySelectedAccountIds,
                    onClick = { onToggleAccount(account.id) }
                )
            }
        }
    }
}

@Composable
private fun MonthBalanceStrip(state: FinanceUiState) {
    val expectedClosing = state.balanceAtStartOfSelectedMonth + state.calendarMonthNet
    val gap = state.selectedMonthReconciliationGap
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Statement check", state.selectedMonth.shortLabel())
            SummaryMetricRow("Current anchor", state.money(state.currentBalanceAnchor), PrimaryBlue)
            SummaryMetricRow("Opening balance", state.money(state.balanceAtStartOfSelectedMonth), TextPrimary)
            SummaryMetricRow("Closing balance", state.money(state.balanceAtEndOfSelectedMonth), TextPrimary)
            SummaryMetricRow("Calendar cashflow", state.money(state.calendarMonthNet), if (state.calendarMonthNet >= 0) MoneyGreen else LossRed)
            SummaryMetricRow("Expected close", state.money(expectedClosing), TextPrimary)
            if (kotlin.math.abs(gap) > 0.01) {
                Text(
                    "Untracked difference: ${state.money(gap)}",
                    color = WarningAmber,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextDim, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = color, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SmallMetric(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier,
    boxHeight: Dp = 104.dp
) {
    ElevatedPanel(modifier = modifier.height(boxHeight)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = TextDim, style = MaterialTheme.typography.bodyMedium)
            Text(value, color = valueColor, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        }
    }
}

@Composable
private fun MonthSelector(months: List<YearMonth>, selected: YearMonth, onSelected: (YearMonth) -> Unit) {
    ChipRow {
        months.forEach {
            MoneyChip(it.shortLabel(), selected = it == selected, onClick = { onSelected(it) })
        }
    }
}

@Composable
private fun CashFlowGraph(state: FinanceUiState) {
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Summary", "This month")
            val income = state.monthReportIncome.toFloat()
            val expense = state.monthExpense.toFloat()
            val max = maxOf(income, expense, 1f)
            Bar("Income", income / max, MoneyGreen, state.money(income.toDouble()))
            Bar("Expense", expense / max, LossRed, state.money(expense.toDouble()))
        }
    }
}

@Composable
private fun Bar(label: String, progress: Float, color: Color, value: String) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "barProgress"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(label, color = TextMuted, modifier = Modifier.weight(1f))
            Text(value, color = TextPrimary)
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
            color = color,
            trackColor = appTrackColor()
        )
    }
}

@Composable
private fun DailyExpenseBarGraph(state: FinanceUiState) {
    val selectedMonth = state.selectedMonth
    val today = LocalDate.now()
    val lastDay = if (selectedMonth == YearMonth.now()) today.dayOfMonth else selectedMonth.lengthOfMonth()
    val firstVisibleDay = (lastDay - 6).coerceAtLeast(1)
    val scopedExpenses = state.monthExpenseTransactions
    val daySegments = remember(scopedExpenses, state.categories, selectedMonth, firstVisibleDay, lastDay) {
        val categoriesById = state.categories.associateBy { it.id }
        val grouped = scopedExpenses
            .asSequence()
            .filter { it.transactionDate().dayOfMonth in firstVisibleDay..lastDay }
            .groupBy { it.transactionDate().dayOfMonth to it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        (firstVisibleDay..lastDay).map { day ->
            val segments = grouped
                .filterKeys { it.first == day }
                .mapNotNull { (key, amount) ->
                    categoriesById[key.second]?.takeIf { amount > 0.0 }?.let { it to amount }
                }
            selectedMonth.atDay(day) to segments
        }
    }
    val max = daySegments.maxOfOrNull { it.second.sumOf { segment -> segment.second } }?.coerceAtLeast(1.0) ?: 1.0

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Daily spending", "Last 7 days")
            if (daySegments.all { it.second.isEmpty() }) {
                Text("No daily expenses for this month.", color = TextDim, style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    items(daySegments, key = { it.first.toString() }) { (date, segments) ->
                        val total = segments.sumOf { it.second }
                        DailyExpenseBar(
                            day = date.dayOfMonth,
                            segments = segments,
                            maxAmount = max,
                            value = compactMoney(total, state.currency)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyExpenseBar(day: Int, segments: List<Pair<CategoryItem, Double>>, maxAmount: Double, value: String) {
    val amount = segments.sumOf { it.second }
    Column(
        modifier = Modifier.width(42.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (amount > 0.0) {
            Text(value, color = TextDim, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
        }
        Column(
            modifier = Modifier
                .width(22.dp)
                .height((112 * (amount / maxAmount).toFloat().coerceIn(0.05f, 1f)).dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(appTrackColor()),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (amount > 0.0) {
                segments.forEach { (category, segmentAmount) ->
                    val segmentHeight = (112 * (segmentAmount / maxAmount).toFloat()).coerceAtLeast(3f).dp
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(segmentHeight)
                            .background(categoryColor(category, TransactionType.Expense))
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(day.toString(), color = TextMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CategoryPieChart(state: FinanceUiState, totals: List<MonthlyCategoryTotal>) {
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Categories", "Expenses")
            val expenses = totals.filter { it.expense > 0.0 }
            val total = expenses.sumOf { it.expense }.toFloat()
            if (total <= 0f) {
                Text("No category expenses this month.", color = TextDim, style = MaterialTheme.typography.bodyMedium)
            } else {
                Canvas(modifier = Modifier.align(Alignment.CenterHorizontally).size(178.dp)) {
                    var startAngle = -90f
                    expenses.forEach { item ->
                        val sweep = (item.expense.toFloat() / total) * 360f
                        drawArc(
                            color = categoryColor(item.category, TransactionType.Expense),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(12f, 12f),
                            size = Size(size.width - 24f, size.height - 24f),
                            style = Stroke(width = 34f, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                expenses.forEach { item ->
                    SegmentAmountRow(
                        label = item.category.name,
                        value = state.money(item.expense),
                        color = categoryColor(item.category, TransactionType.Expense),
                        progress = (item.expense / total).coerceIn(0.04, 1.0).toFloat()
                    )
                }
            }
        }
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
