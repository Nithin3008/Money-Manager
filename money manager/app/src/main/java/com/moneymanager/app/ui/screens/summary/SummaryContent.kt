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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    item { LargeTitle("Reports", state.selectedMonth.shortLabel()) }
    item {
        val months = remember(state.transactions) { availableMonths(state) }
        MonthSelector(
            months = months,
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
    item {
        val totals = remember(state.monthTransactions, state.categories) { categoryTotals(state) }
        CategoryPieChart(state, totals)
    }
    item { MetricGrid(state = state) }
    item { DailyExpenseBarGraph(state) }
    item { StatementCheckCard(state) }
}

@Composable
private fun MetricGrid(state: FinanceUiState) {
    val currency = state.currency
    val creditCardActivity = remember(
        state.transactions,
        state.selectedMonth,
        state.activeSummaryAccountIds
    ) {
        state.transactions
            .filter {
                it.isCreditCardTransaction &&
                    YearMonth.from(it.transactionDate()) == state.selectedMonth &&
                    (state.activeSummaryAccountIds.isEmpty() || it.accountId in state.activeSummaryAccountIds)
            }
            .sumOf { it.amount }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetric("Total spent", money(state.monthExpense, currency), LossRed, Modifier.weight(1f))
            SmallMetric(
                "Daily avg",
                money(state.monthExpense / state.selectedMonth.lengthOfMonth().coerceAtLeast(1), currency),
                WarningAmber,
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetric("Balance", money(state.currentBalanceAnchor, currency), PrimaryBlue, Modifier.weight(1f))
            SmallMetric("Card spend", money(creditCardActivity, currency), TextPrimary, Modifier.weight(1f))
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
private fun StatementCheckCard(state: FinanceUiState) {
    val gap = state.selectedMonthReconciliationGap
    val movementColor = if (state.calendarMonthNet >= 0.0) PrimaryBlue else LossRed
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Statement check", state.selectedMonth.shortLabel())
            StatementMetricRow("Current bank balance", state.money(state.currentBalanceAnchor), PrimaryBlue)
            StatementMetricRow("Month closing balance", state.money(state.balanceAtEndOfSelectedMonth), TextPrimary)
            StatementMetricRow("Month movement", state.money(state.calendarMonthNet), movementColor)
            if (kotlin.math.abs(gap) > 0.01) {
                Text(
                    "Statement difference: ${state.money(gap)}",
                    color = WarningAmber,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    "Statement looks balanced for the selected account.",
                    color = TextDim,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatementMetricRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextDim, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = color, style = MaterialTheme.typography.titleMedium, maxLines = 1)
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
            SectionHeader("Categories", "Spending mix")
            val expenses = totals.filter { it.expense > 0.0 }
                .sortedByDescending { it.expense }
            val total = expenses.sumOf { it.expense }
            if (total <= 0.0) {
                Text("No category expenses this month.", color = TextDim, style = MaterialTheme.typography.bodyMedium)
            } else {
                val slices = expenseSlices(expenses)
                val topSlice = slices.first()
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 28f
                        val inset = strokeWidth / 2f + 10f
                        val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                        drawArc(
                            color = appTrackColor(),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        var startAngle = -90f
                        slices.forEach { slice ->
                            val sweep = ((slice.amount / total).toFloat() * 360f).coerceAtLeast(3f)
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = (sweep - 4f).coerceAtLeast(1f),
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            compactMoney(total, state.currency),
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1
                        )
                        Text("spent", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                TopCategoryCard(slice = topSlice, total = total, state = state)

                slices.forEach { slice ->
                    CategorySliceRow(slice = slice, total = total, state = state)
                }
            }
        }
    }
}

private data class ExpenseSlice(
    val label: String,
    val amount: Double,
    val color: Color,
    val icon: ImageVector?
)

@Composable
private fun expenseSlices(expenses: List<MonthlyCategoryTotal>): List<ExpenseSlice> {
    val visible = expenses.take(5).map {
        ExpenseSlice(
            label = it.category.name,
            amount = it.expense,
            color = categoryColor(it.category, TransactionType.Expense),
            icon = it.category.icon
        )
    }
    val otherAmount = expenses.drop(5).sumOf { it.expense }
    return if (otherAmount > 0.0) {
        visible + ExpenseSlice("Other", otherAmount, TextMuted, null)
    } else {
        visible
    }
}

@Composable
private fun TopCategoryCard(slice: ExpenseSlice, total: Double, state: FinanceUiState) {
    val percent = ((slice.amount / total) * 100).toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(slice.color.copy(alpha = 0.14f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(slice.color),
            contentAlignment = Alignment.Center
        ) {
            slice.icon?.let {
                Icon(it, contentDescription = null, tint = Color(0xFF141414), modifier = Modifier.size(23.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Top category", color = TextDim, style = MaterialTheme.typography.labelMedium)
            Text(
                slice.label,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(state.money(slice.amount), color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text("$percent%", color = slice.color, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CategorySliceRow(slice: ExpenseSlice, total: Double, state: FinanceUiState) {
    val progress by animateFloatAsState(
        targetValue = (slice.amount / total).toFloat().coerceIn(0.04f, 1f),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "categorySliceProgress"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(slice.color))
            Spacer(Modifier.width(10.dp))
            Text(
                slice.label,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(state.money(slice.amount), color = TextMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(appTrackColor())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(50))
                    .background(slice.color)
            )
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
