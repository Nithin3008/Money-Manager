package com.moneymanager.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.MoneyIcons
import com.moneymanager.app.model.MonthlyCategoryTotal
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.month
import com.moneymanager.app.model.shortLabel
import com.moneymanager.app.model.transactionDate
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
import kotlin.math.abs

private val OtherViolet = Color(0xFFC9A0F0)

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
    item { SpendHeatmapCard(state) }
    item {
        val totals = remember(state.monthTransactions, state.categories) { categoryTotals(state) }
        WhereItWentCard(state, totals)
    }
}

@Composable
private fun ReportsHeader(state: FinanceUiState, onMonthSelected: (YearMonth) -> Unit) {
    var showMonthMenu by remember { mutableStateOf(false) }
    val months = remember(state.transactions) { availableMonths(state) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Reports",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.weight(1f)
        )
        Box {
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Navy800)
                    .clickable { showMonthMenu = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(17.dp))
                Text(
                    state.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            DropdownMenu(expanded = showMonthMenu, onDismissRequest = { showMonthMenu = false }) {
                months.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month.shortLabel()) },
                        onClick = {
                            onMonthSelected(month)
                            showMonthMenu = false
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
    val allSelected = state.summarySelectedAccountIds.isEmpty()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipPill(
            label = "All accounts",
            selected = allSelected,
            showCheck = true,
            onClick = onClearFilter
        )
        state.accounts.forEach { account ->
            FilterChipPill(
                label = account.name,
                selected = !allSelected && account.id in state.summarySelectedAccountIds,
                showCheck = false,
                onClick = { onToggleAccount(account.id) }
            )
        }
    }
}

@Composable
private fun FilterChipPill(label: String, selected: Boolean, showCheck: Boolean, onClick: () -> Unit) {
    val container = if (selected) PrimaryBlue.copy(alpha = if (isDarkTheme()) 0.28f else 0.20f) else Navy800
    val ink = if (selected) PrimaryBlue else TextMuted
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (selected && showCheck) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
        }
        Text(
            label,
            color = ink,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetricGrid(state: FinanceUiState) {
    val spentTint = WarningAmber.copy(alpha = if (isDarkTheme()) 0.22f else 0.16f)
    val incomeTint = MoneyGreen.copy(alpha = if (isDarkTheme()) 0.20f else 0.15f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = "Spent",
                value = state.money(state.monthExpense).withCurrencyGap(),
                container = spentTint,
                labelColor = WarningAmber,
                valueColor = TextPrimary,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 12.dp, bottomStart = 28.dp),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Income",
                value = state.money(state.monthReportIncome).withCurrencyGap(),
                container = incomeTint,
                labelColor = MoneyGreen,
                valueColor = TextPrimary,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 12.dp),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val net = state.monthReportNet
            MetricTile(
                label = "Net saved",
                value = ((if (net >= 0) "+" else "-") + state.money(abs(net))).withCurrencyGap(),
                container = Navy850,
                labelColor = TextDim,
                valueColor = if (net >= 0) PrimaryBlue else com.moneymanager.app.ui.theme.LossRed,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 28.dp),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Closing bal.",
                value = state.money(state.balanceAtEndOfSelectedMonth).withCurrencyGap(),
                container = Navy850,
                labelColor = TextDim,
                valueColor = TextPrimary,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 28.dp, bottomStart = 28.dp),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    container: Color,
    labelColor: Color,
    valueColor: Color,
    shape: RoundedCornerShape,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(container)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(label, color = labelColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        Text(
            value,
            color = valueColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SpendHeatmapCard(state: FinanceUiState) {
    val month = state.selectedMonth
    val dailySpend = remember(state.monthExpenseTransactions, month) {
        state.monthExpenseTransactions
            .filter { YearMonth.from(it.transactionDate()) == month }
            .groupBy { it.transactionDate().dayOfMonth }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
    }
    val maxSpend = (dailySpend.values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val accentContainer = PrimaryBlue.copy(alpha = if (isDarkTheme()) 0.32f else 0.24f)

    ReportCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Spend heatmap",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.weight(1f)
            )
            CardPill(month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()))
        }
        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..month.lengthOfMonth()).chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { day ->
                        val spend = dailySpend[day] ?: 0.0
                        val cellBg = when {
                            spend <= 0.0 -> Navy800
                            spend < maxSpend * 0.5 -> accentContainer
                            else -> PrimaryBlue
                        }
                        val ink = when {
                            spend <= 0.0 -> TextDim
                            spend < maxSpend * 0.5 -> TextPrimary
                            else -> OnAccent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cellBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.toString(), color = ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            Text("Less", color = TextDim, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            LegendSwatch(Navy800)
            LegendSwatch(accentContainer)
            LegendSwatch(PrimaryBlue)
            Text("More", color = TextDim, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
}

@Composable
private fun CardPill(label: String) {
    Box(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Navy800)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private data class SpendSlice(
    val name: String,
    val amount: Double,
    val fraction: Float,
    val color: Color,
    val icon: ImageVector
)

@Composable
private fun WhereItWentCard(state: FinanceUiState, totals: List<MonthlyCategoryTotal>) {
    val expenses = remember(totals) { totals.filter { it.expense > 0.0 }.sortedByDescending { it.expense } }
    val total = expenses.sumOf { it.expense }

    ReportCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Where it went",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.weight(1f)
            )
            CardPill("${expenses.size} categories")
        }

        if (total <= 0.0) {
            Text(
                "No category expenses this month.",
                color = TextDim,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 14.dp)
            )
            return@ReportCard
        }

        val slices = remember(expenses, total) {
            val top = expenses.take(4).map {
                SpendSlice(
                    name = it.category.name,
                    amount = it.expense,
                    fraction = (it.expense / total).toFloat(),
                    color = colorFromHexStatic(it.category.colorHex),
                    icon = it.category.icon
                )
            }
            val restAmount = expenses.drop(4).sumOf { it.expense }
            if (restAmount > 0.0) {
                top + SpendSlice("Other", restAmount, (restAmount / total).toFloat(), OtherViolet, MoneyIcons.Category)
            } else {
                top
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(112.dp)) {
                    val strokeWidth = 19.dp.toPx()
                    val inset = strokeWidth / 2f
                    var startAngle = -90f
                    slices.forEach { slice ->
                        val sweep = slice.fraction * 360f
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SPENT", color = TextDim, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
                    Text(
                        compactMoney(total, state.currency).removePrefix(state.currency.symbol).trim(),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                }
            }

            val top = slices.first()
            Column(Modifier.weight(1f)) {
                Text("Top category", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(top.color.copy(alpha = if (isDarkTheme()) 0.22f else 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(top.icon, contentDescription = null, tint = top.color, modifier = Modifier.size(17.dp))
                    }
                    Text(
                        top.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    topCategoryCaption(state, top),
                    color = TextDim,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(slice.color.copy(alpha = if (isDarkTheme()) 0.22f else 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(slice.icon, contentDescription = null, tint = slice.color, modifier = Modifier.size(19.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Text(
                                slice.name,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                state.money(slice.amount),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Navy800)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(slice.fraction.coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(slice.color)
                            )
                        }
                    }
                    Text(
                        "${(slice.fraction * 100).toInt()}%",
                        color = TextDim,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(34.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** "Rs X · 36% of spend" plus how the top category moved vs the previous month. */
private fun topCategoryCaption(state: FinanceUiState, top: SpendSlice): String {
    val pct = (top.fraction * 100).toInt()
    val prevMonth = state.selectedMonth.minusMonths(1)
    val prevAmount = state.transactions
        .filter {
            it.type == TransactionType.Expense &&
                !it.isCreditCardTransaction &&
                !it.excludeFromSummary &&
                it.month() == prevMonth &&
                state.categoriesById[it.categoryId]?.name == top.name
        }
        .sumOf { it.amount }
    val delta = top.amount - prevAmount
    val prevLabel = prevMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val deltaLine = when {
        prevAmount <= 0.0 -> "No spend in $prevLabel"
        delta >= 0.0 -> "${money(delta, state.currency)} more than $prevLabel"
        else -> "${money(-delta, state.currency)} less than $prevLabel"
    }
    return "${money(top.amount, state.currency)} · $pct% of spend\n$deltaLine"
}

@Composable
private fun ReportCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Navy900)
            .padding(20.dp),
        content = content
    )
}

private fun colorFromHexStatic(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(OtherViolet)

private fun availableMonths(state: FinanceUiState): List<YearMonth> {
    val fromTransactions = state.transactions.map { it.month() }
    return (fromTransactions + YearMonth.now()).distinct().sortedDescending()
}

private fun categoryTotals(state: FinanceUiState): List<MonthlyCategoryTotal> {
    return state.categories.map { category ->
        val expense = state.monthExpenseTransactions
            .filter { it.categoryId == category.id }
            .sumOf { it.amount }
        MonthlyCategoryTotal(
            category = category,
            income = 0.0,
            expense = expense
        )
    }
}
