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
import androidx.compose.material3.HorizontalDivider
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
import com.moneymanager.app.ui.theme.PrimarySoft
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
    item { LargeTitle("Monthly Summary", state.selectedMonth.shortLabel()) }
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
    item { MetricGrid(state = state) }
    item { MonthBalanceStrip(state) }
    item { CashFlowGraph(state) }
    item { DailyExpenseBarGraph(state) }
    item { CategoryPieChart(state, categoryTotals(state)) }
    item { CategoryHistoryGraph(state, categoryTotals(state)) }
}

@Composable
private fun MetricGrid(state: FinanceUiState) {
    val currency = state.currency
    val salary = state.monthSalaryIncome
    val other = state.monthOtherIncome
    val expense = state.monthExpense
    val reportIncome = state.monthReportIncome
    val creditCardActivity = state.transactions
        .filter {
            it.isCreditCardTransaction &&
                YearMonth.from(it.transactionDate()) == state.selectedMonth &&
                (state.activeSummaryAccountIds.isEmpty() || it.accountId in state.activeSummaryAccountIds)
        }
        .sumOf { it.amount }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ElevatedPanel(modifier = Modifier.weight(1f).height(124.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
                    Text("Income", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        money(reportIncome, currency),
                        color = MoneyGreen,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1
                    )
                    if (salary > 0.0 || other > 0.0) {
                        Text(
                            "${money(salary, currency)} salary + ${money(other, currency)} other",
                            color = OtherIncomeGold,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
            }
            SmallMetric("Expenses", money(expense, currency), LossRed, Modifier.weight(1f), boxHeight = 124.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetric("Current balance", money(state.currentBalanceAnchor, currency), PrimarySoft, Modifier.weight(1f))
            CreditCardActivityMetric(
                amount = money(creditCardActivity, currency),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetric(
                "Opening balance",
                money(state.selectedMonthOpeningBalance, currency),
                TextPrimary,
                Modifier.weight(1f)
            )
            SmallMetric(
                "Salary",
                money(state.selectedMonthSalaryIncome, currency),
                MoneyGreen,
                Modifier.weight(1f)
            )
        }
        Text(
            "Income matches calendar-month deposits for the selected bank.",
            color = TextDim,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CreditCardActivityMetric(amount: String, modifier: Modifier) {
    ElevatedPanel(
        modifier = modifier.height(104.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text("CC activity", color = TextDim, style = MaterialTheme.typography.bodyMedium)
            Text(amount, color = OtherIncomeGold, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
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
        LabelText("SUMMARY ACCOUNTS")
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
    val isCurrentMonth = state.selectedMonth == YearMonth.now()
    val expectedClosing = state.balanceAtStartOfSelectedMonth + state.calendarMonthNet
    val gap = state.selectedMonthReconciliationGap
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Balance reconstruction", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            SummaryMetricRow("Current anchor", state.money(state.currentBalanceAnchor), PrimarySoft)
            SummaryMetricRow("Opening balance", state.money(state.balanceAtStartOfSelectedMonth), TextPrimary)
            SummaryMetricRow(if (isCurrentMonth) "Balance now" else "Closing balance", state.money(state.balanceAtEndOfSelectedMonth), TextPrimary)
            HorizontalDivider(color = appBorderColor())
            SummaryMetricRow(
                "Calendar cashflow",
                state.money(state.calendarMonthNet),
                if (state.calendarMonthNet >= 0) MoneyGreen else LossRed
            )
            SummaryMetricRow("Expected close", state.money(expectedClosing), TextPrimary)
            if (kotlin.math.abs(gap) > 0.01) {
                Text(
                    "Untracked difference: ${state.money(gap)}. This usually means an SMS is missing, excluded, duplicated, or mapped to another account.",
                    color = WarningAmber,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.salaryShiftIncomeEnabled) {
                Text(
                    "Income cards may use payroll month shift; reconstruction always follows actual SMS dates.",
                    color = TextDim,
                    style = MaterialTheme.typography.bodySmall
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
            Text(value, color = valueColor, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
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
            SectionHeader("Graph", "Income vs Expense")
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
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)),
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
            SectionHeader("Daily Expenses", "Last 7 days")
            if (daySegments.all { it.second.isEmpty() }) {
                EmptyPanel("No daily expenses for this month.")
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
            Text(
                value,
                color = TextDim,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
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
            SectionHeader("Pie Chart", "Expenses by category")
            val expenses = totals.filter { it.expense > 0.0 }
            val total = expenses.sumOf { it.expense }.toFloat()
            if (total <= 0f) {
                EmptyPanel("No category expenses this month.")
            } else {
                Canvas(modifier = Modifier.align(Alignment.CenterHorizontally).size(170.dp)) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(10.dp).clip(CircleShape)
                                .background(categoryColor(item.category, TransactionType.Expense))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(item.category.name, color = TextMuted, modifier = Modifier.weight(1f))
                        Text(state.money(item.expense), color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHistoryGraph(state: FinanceUiState, totals: List<MonthlyCategoryTotal>) {
    val visibleTotals = totals
        .filter { it.expense > 0.0 }
        .sortedByDescending { it.expense }
    val max = visibleTotals.maxOfOrNull { it.expense }?.coerceAtLeast(1.0) ?: 1.0

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Category History", "Spending - ${state.selectedMonth.shortLabel()}")
            if (visibleTotals.isEmpty()) {
                EmptyPanel("No category expenses for this month.")
            } else {
                visibleTotals.forEach { item ->
                    SegmentAmountRow(
                        label = item.category.name,
                        value = state.money(item.expense),
                        color = categoryColor(item.category, TransactionType.Expense),
                        progress = (item.expense / max).toFloat()
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
