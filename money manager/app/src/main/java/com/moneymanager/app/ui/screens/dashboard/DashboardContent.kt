package com.moneymanager.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.MonthlyCategoryTotal
import com.moneymanager.app.model.ScreenTab
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.transactionDate
import com.moneymanager.app.ui.theme.LossRed
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.Navy850
import com.moneymanager.app.ui.theme.PrimaryBlue
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
    item {
        FintrackBalanceHero(state)
    }
    item {
        FintrackQuickActions(
            accounts = state.accounts.size,
            categories = state.categories.size,
            budgets = state.activeBudgets.size,
            transactions = state.transactions.size
        )
    }
    if (state.todayDetectedDrafts.isNotEmpty()) {
        item {
            SectionHeader(
                "Review required",
                "Page ${state.dashboardCurrentDraftPage}/${state.dashboardDraftPageCount}"
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
    }
    item {
        FintrackTodaySegments(state)
    }
    item {
        SectionHeader(
            "Transactions",
            "Page ${state.dashboardCurrentPage}/${state.dashboardTransactionPageCount}"
        )
    }
    if (state.todayTransactions.isEmpty()) {
        item { EmptyPanel("No transactions today. Tap + to add one.") }
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
    item {
        ActionPanel(
            title = "Statistics",
            subtitle = "Open monthly cash flow, spending, and category charts.",
            icon = Icons.Rounded.BarChart,
            action = "Open",
            onClick = { onOpenSummary(ScreenTab.Summary) }
        )
    }
}

@Composable
private fun FintrackBalanceHero(state: FinanceUiState) {
    val todayIncome = state.todayTransactions
        .filter { it.type == TransactionType.Income && !it.excludeFromSummary }
        .sumOf { it.amount }
    val todayExpense = state.todayTransactions
        .filter { it.type == TransactionType.Expense && !it.excludeFromSummary }
        .sumOf { it.amount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
        border = BorderStroke(1.dp, PrimaryBlue)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Balance", color = Color(0xFF141414).copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
                    Text(
                        state.money(state.trackedBalance),
                        color = Color(0xFF141414),
                        style = MaterialTheme.typography.headlineLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFF141414), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Wallet, contentDescription = null, tint = PrimaryBlue)
                }
            }
            MiniFlowChart()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FintrackHeroPill("Income", state.money(todayIncome), Modifier.weight(1f))
                FintrackHeroPill("Spent", state.money(todayExpense), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FintrackHeroPill(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF141414).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(label, color = Color(0xFF141414).copy(alpha = 0.62f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color(0xFF141414), style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun MiniFlowChart() {
    Canvas(Modifier.fillMaxWidth().height(84.dp)) {
        val dark = Color(0xFF141414)
        val muted = dark.copy(alpha = 0.22f)
        val path = Path().apply {
            moveTo(0f, size.height * 0.68f)
            cubicTo(size.width * 0.22f, size.height * 0.18f, size.width * 0.38f, size.height * 0.9f, size.width * 0.58f, size.height * 0.44f)
            cubicTo(size.width * 0.74f, size.height * 0.08f, size.width * 0.88f, size.height * 0.22f, size.width, size.height * 0.12f)
        }
        drawLine(muted, Offset(0f, size.height * 0.82f), Offset(size.width, size.height * 0.82f), strokeWidth = 2f)
        drawPath(path, dark, style = Stroke(width = 6f, cap = StrokeCap.Round))
        listOf(0.14f, 0.36f, 0.62f, 0.84f).forEachIndexed { index, x ->
            val barHeight = size.height * listOf(0.32f, 0.48f, 0.28f, 0.58f)[index]
            drawLine(
                muted,
                Offset(size.width * x, size.height * 0.82f),
                Offset(size.width * x, size.height * 0.82f - barHeight),
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun FintrackQuickActions(
    accounts: Int,
    categories: Int,
    budgets: Int,
    transactions: Int
) {
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Select an action", "Today")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile(Icons.Rounded.AccountBalance, "Accounts", accounts.toString(), Modifier.weight(1f))
                ActionTile(Icons.AutoMirrored.Rounded.ReceiptLong, "History", transactions.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile(Icons.Rounded.Category, "Categories", categories.toString(), Modifier.weight(1f))
                ActionTile(Icons.Rounded.PieChart, "Budgets", budgets.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(Navy800, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun FintrackTodaySegments(state: FinanceUiState) {
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
            SectionHeader("Spend categories", LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d")))
            if (totals.isEmpty()) {
                Text("No categorized movement yet.", color = TextDim, style = MaterialTheme.typography.bodyMedium)
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Sms, PrimaryBlue)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(draft.bankName, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("${type.bankVerb()} | $dateLabel", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    signedAmount(draft.amount, type, state.currency),
                    color = type.amountColor(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )
            }
            Text(draft.counterparty, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
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
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = primaryButtonColors(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onIgnore(draft.id) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, appBorderColor()),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ignore")
                }
            }
        }
    }
}
