package com.moneymanager.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.MonthlyCategoryTotal
import com.moneymanager.app.model.ScreenTab
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.transactionDate
import com.moneymanager.app.ui.theme.LossRed
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.PrimaryBlue
import com.moneymanager.app.ui.theme.PrimarySoft
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun LazyListScope.dashboardContent(
    state: FinanceUiState,
    onOpenSummary: (ScreenTab) -> Unit,
    onAcceptDraft: (Long, Long, TransactionType) -> Unit,
    onIgnoreDraft: (Long) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDashboardPageSelected: (Int) -> Unit,
    onDraftPageSelected: (Int) -> Unit
) {
    item { TodayDonutCard(state) }
    item { TodayCategoryBreakdown(state) }
    if (state.todayDetectedDrafts.isNotEmpty()) {
        item {
            SectionHeader(
                "Today Pending",
                "Page ${state.dashboardCurrentDraftPage} of ${state.dashboardDraftPageCount}"
            )
        }
        items(state.dashboardPagedDrafts, key = { "draft_${it.id}" }) {
            DetectedDraftRow(
                state = state,
                draft = it,
                onAccept = onAcceptDraft,
                onIgnore = onIgnoreDraft
            )
        }
        item {
            DashboardPagination(
                pageCount = state.dashboardDraftPageCount,
                currentPage = state.dashboardCurrentDraftPage,
                onPageSelected = onDraftPageSelected,
                label = "Pages"
            )
        }
    } else {
        item {
            SectionHeader(
                "Today",
                "Page ${state.dashboardCurrentPage} of ${state.dashboardTransactionPageCount}"
            )
        }
        if (state.todayTransactions.isEmpty()) {
            item { EmptyPanel("No transactions for today yet.") }
        } else {
            items(state.dashboardPagedTransactions, key = { "dashboard_txn_${it.id}" }) {
                TransactionRow(transaction = it, state = state, onSelect = onEditTransaction)
            }
            item {
                DashboardPagination(
                    pageCount = state.dashboardTransactionPageCount,
                    currentPage = state.dashboardCurrentPage,
                    onPageSelected = onDashboardPageSelected,
                    label = "Pages"
                )
            }
        }
    }
    item {
        ActionPanel(
            title = "Monthly Analysis",
            subtitle = "Open category history, monthly totals, and graphical breakdowns.",
            icon = Icons.Rounded.BarChart,
            action = "Open",
            onClick = { onOpenSummary(ScreenTab.Summary) }
        )
    }
}

@Composable
private fun DetectedDraftRow(
    state: FinanceUiState,
    draft: DetectedTransactionDraft,
    onAccept: (Long, Long, TransactionType) -> Unit,
    onIgnore: (Long) -> Unit
) {
    var categoryId by remember { mutableStateOf(draft.suggestedCategoryId ?: state.categories.first().id) }
    var type by remember { mutableStateOf(draft.type) }
    val dateLabel = draft.transactionDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

    ElevatedPanel {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Sms, MoneyGreen)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(draft.bankName, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("${type.bankVerb()} | $dateLabel", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                }
                Text(signedAmount(draft.amount, type, state.currency), color = type.amountColor(), style = MaterialTheme.typography.titleLarge)
            }
            Text(draft.counterparty, color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
            LabelText("TYPE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach {
                    TransactionTypeChip(it, selected = type == it, onClick = { type = it })
                }
            }
            LabelText("CATEGORY")
            ChipRow {
                state.categories.forEach { category ->
                    CategoryChoiceChip(
                        category = category,
                        type = type,
                        selected = categoryId == category.id,
                        onClick = { categoryId = category.id }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onAccept(draft.id, categoryId, type) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAmoledTheme()) PrimarySoft else Color(0xFFEAF2FF),
                        contentColor = if (isAmoledTheme()) Color(0xFF001A42) else PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add")
                }
                OutlinedButton(
                    onClick = { onIgnore(draft.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ignore")
                }
            }
        }
    }
}

@Composable
private fun TodayDonutCard(state: FinanceUiState) {
    val reportTransactions = state.todayTransactions.filterNot { it.excludeFromSummary }
    val income = reportTransactions
        .filter { it.type == TransactionType.Income }
        .sumOf { it.amount }
    val expense = reportTransactions
        .filter { it.type == TransactionType.Expense }
        .sumOf { it.amount }
    val creditCardActivity = state.todayTransactions
        .filter { it.isCreditCardTransaction }
        .sumOf { it.amount }
    val total = expense.coerceAtLeast(1.0)
    val expenseSweep by animateFloatAsState(
        targetValue = ((expense / total) * 360f).toFloat(),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "todayExpenseSweep"
    )

    ElevatedPanel {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 34f, cap = StrokeCap.Round)
                    drawArc(
                        color = appTrackColor(),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(20f, 20f),
                        size = Size(size.width - 40f, size.height - 40f),
                        style = stroke
                    )
                    drawArc(
                        color = LossRed,
                        startAngle = -90f,
                        sweepAngle = expenseSweep,
                        useCenter = false,
                        topLeft = Offset(20f, 20f),
                        size = Size(size.width - 40f, size.height - 40f),
                        style = stroke
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Today", color = TextDim, style = MaterialTheme.typography.labelMedium)
                    Text(state.money(expense), color = LossRed, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text("spent", color = TextDim, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Today Spend", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${reportTransactions.size} transaction${if (reportTransactions.size == 1) "" else "s"} counted today",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                LegendDot(LossRed, "Expense")
                if (creditCardActivity > 0.0) {
                    Text(
                        "Credit card activity tracked: ${state.money(creditCardActivity)}",
                        color = TextDim,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "Net movement: ${state.money(income - expense)}",
                    color = if (income >= expense) PrimarySoft else LossRed,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun TodayCategoryBreakdown(state: FinanceUiState) {
    val totals = state.categories.map { category ->
        val transactions = state.todayTransactions.filter { it.categoryId == category.id && !it.excludeFromSummary }
        MonthlyCategoryTotal(
            category = category,
            income = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount },
            expense = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
        )
    }.filter { it.income > 0.0 || it.expense > 0.0 }

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Today Segments", LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d")))
            if (totals.isEmpty()) {
                Text("No categorized movement today.", color = TextDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                val max = totals.maxOf { it.income + it.expense }.coerceAtLeast(1.0)
                totals.forEach { item ->
                    val amount = item.income + item.expense
                    SegmentAmountRow(
                        label = item.category.name,
                        value = state.money(amount),
                        color = categoryColor(item.category, if (item.expense > 0.0) TransactionType.Expense else TransactionType.Income),
                        progress = (amount / max).toFloat()
                    )
                }
            }
        }
    }
}
