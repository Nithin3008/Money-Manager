package com.moneymanager.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.transactionDate
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM")

internal fun LazyListScope.dashboardContent(
    state: FinanceUiState,
    onAcceptDraft: (Long, Long, TransactionType, Long?, Long?) -> Unit,
    onIgnoreDraft: (Long) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDashboardPageSelected: (Int) -> Unit,
    onDraftPageSelected: (Int) -> Unit,
    onSeeAllTransactions: () -> Unit = {}
) {
    item {
        FintrackBalanceHero(state)
    }
    item {
        QuickStatGrid(state)
    }
    if (state.todayDetectedDrafts.isNotEmpty()) {
        item {
            NeedsReviewStrip(
                state = state,
                onAccept = onAcceptDraft,
                onIgnore = onIgnoreDraft,
                onPageSelected = onDraftPageSelected
            )
        }
    }
    item {
        TodaySplitCard(state)
    }
    item {
        SectionHeader("Transactions", "See all", onAction = onSeeAllTransactions)
    }
    if (state.todayTransactions.isEmpty()) {
        item {
            EmptyStateCard(
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                title = "No transactions today",
                caption = "Tap + to add your first one."
            )
        }
    } else {
        items(state.dashboardPagedTransactions, key = { "dashboard_txn_${it.id}" }) {
            TransactionRow(
                transaction = it,
                categoriesById = state.categoriesById,
                accountsById = state.accountsById,
                currency = state.currency,
                onSelect = onEditTransaction,
                modifier = Modifier.animateItem()
            )
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

@Composable
private fun FintrackBalanceHero(state: FinanceUiState) {
    val currentAccount = state.accounts.firstOrNull { it.id == state.defaultAccountId }
        ?: state.accounts.firstOrNull()
    val currentBalance = currentAccount?.balance ?: 0.0
    val balanceSource = currentAccount?.name ?: "No default account"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
    ) {
        Column(Modifier.padding(22.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Current bank balance",
                        color = OnAccent.copy(alpha = 0.72f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        state.money(currentBalance).withCurrencyGap(),
                        color = OnAccent,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                        lineHeight = 42.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.Black.copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = OnAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            balanceSource,
                            color = OnAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Wallet,
                        contentDescription = null,
                        tint = OnAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(34.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(0.40f, 0.65f, 0.35f, 0.80f, 0.55f, 1f, 0.70f, 0.48f).forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(4.dp))
                            .background(OnAccent.copy(alpha = 0.55f))
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatGrid(state: FinanceUiState) {
    val invested = remember(state.transactions, state.categories, state.currency) {
        compactMoney(state.investmentTotalFor(YearMonth.now()), state.currency)
    }
    val ccSpend = remember(state.transactions, state.currency) {
        compactMoney(state.creditCardSpendTotalFor(YearMonth.now()), state.currency)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickStatTile(Icons.Rounded.CreditCard, ccSpend, "CC spend · month", Modifier.weight(1f))
            QuickStatTile(Icons.Rounded.PieChart, state.activeBudgets.size.toString(), "Budgets", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickStatTile(Icons.AutoMirrored.Rounded.TrendingUp, invested, "Invested · month", Modifier.weight(1f))
            QuickStatTile(Icons.Rounded.AccountBalanceWallet, compactMoney(state.creditCardOutstanding, state.currency), "CC outstanding", Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickStatTile(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Navy850)
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        Text(
            value,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(label, color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun NeedsReviewStrip(
    state: FinanceUiState,
    onAccept: (Long, Long, TransactionType, Long?, Long?) -> Unit,
    onIgnore: (Long) -> Unit,
    onPageSelected: (Int) -> Unit
) {
    val amberSoft = WarningAmber.copy(alpha = if (isDarkTheme()) 0.10f else 0.08f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(amberSoft)
            .dashedBorder(WarningAmber, 22.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Rule, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
            Text("Needs review", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(WarningAmber)
                    .padding(horizontal = 9.dp, vertical = 2.dp)
            ) {
                Text(
                    state.todayDetectedDrafts.size.toString(),
                    color = if (isDarkTheme()) Color(0xFF2A2205) else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            "SMS-detected transactions we couldn't auto-confirm.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
        state.dashboardPagedDrafts.forEach { draft ->
            DetectedDraftRow(state = state, draft = draft, onAccept = onAccept, onIgnore = onIgnore)
        }
        DashboardPagination(
            pageCount = state.dashboardDraftPageCount,
            currentPage = state.dashboardCurrentDraftPage,
            onPageSelected = onPageSelected,
            label = "Pages"
        )
    }
}

@Composable
private fun DetectedDraftRow(
    state: FinanceUiState,
    draft: DetectedTransactionDraft,
    onAccept: (Long, Long, TransactionType, Long?, Long?) -> Unit,
    onIgnore: (Long) -> Unit
) {
    var categoryId by remember { mutableStateOf(draft.suggestedCategoryId ?: state.categories.first().id) }
    var type by remember { mutableStateOf(draft.type) }
    var fromAccountId by remember(draft.id, draft.fromAccountId) { mutableStateOf(draft.fromAccountId) }
    var toAccountId by remember(draft.id, draft.toAccountId) { mutableStateOf(draft.toAccountId) }
    val dateLabel = draft.transactionDate().mediumDateLabel()
    val isTransferReview = draft.type == TransactionType.Transfer
    val canAccept = !isTransferReview ||
        (fromAccountId != null && toAccountId != null && fromAccountId != toAccountId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .drawBehind {
                drawLine(LineColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            }
            .padding(top = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Navy800),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        isTransferReview -> Icons.Rounded.AccountBalance
                        type == TransactionType.Income -> Icons.Rounded.NorthEast
                        else -> Icons.Rounded.SouthWest
                    },
                    contentDescription = null,
                    tint = when {
                        isTransferReview -> TextMuted
                        type == TransactionType.Income -> MoneyGreen
                        else -> LossRed
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    draft.counterparty,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${draft.bankName} · $dateLabel",
                    color = TextDim,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                signedAmount(draft.amount, type, state.currency),
                color = type.amountColor(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val typeChoices = if (isTransferReview) {
                listOf(TransactionType.Transfer)
            } else {
                listOf(TransactionType.Income, TransactionType.Expense)
            }
            typeChoices.forEach {
                TransactionTypeChip(it, selected = type == it, onClick = { type = it })
            }
        }
        if (isTransferReview) {
            LabelText("FROM ACCOUNT")
            ChipRow {
                state.accounts.forEach { account ->
                    MoneyChip(
                        label = account.name,
                        selected = fromAccountId == account.id,
                        onClick = {
                            fromAccountId = account.id
                            if (toAccountId == account.id) {
                                toAccountId = state.accounts.firstOrNull { it.id != account.id }?.id
                            }
                        }
                    )
                }
            }
            LabelText("TO ACCOUNT")
            ChipRow {
                state.accounts.forEach { account ->
                    MoneyChip(
                        label = account.name,
                        selected = toAccountId == account.id,
                        onClick = {
                            toAccountId = account.id
                            if (fromAccountId == account.id) {
                                fromAccountId = state.accounts.firstOrNull { it.id != account.id }?.id
                            }
                        }
                    )
                }
            }
            if (!canAccept) {
                Text(
                    "Choose two different accounts to approve this transfer.",
                    color = TextDim,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
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
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canAccept) PrimaryBlue else PrimaryBlue.copy(alpha = 0.4f))
                    .clickable(enabled = canAccept) {
                        onAccept(draft.id, categoryId, type, fromAccountId, toAccountId)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Accept", color = OnAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Navy800)
                    .clickable { onIgnore(draft.id) },
                contentAlignment = Alignment.Center
            ) {
                Text("Ignore", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TodaySplitCard(state: FinanceUiState) {
    val todayIncome = remember(state.todayTransactions) {
        state.todayTransactions
            .filter {
                it.type == TransactionType.Income && !state.isInvestmentTransaction(it) && !it.excludeFromSummary
            }
            .sumOf { it.amount }
    }
    val todayExpense = remember(state.todayTransactions) {
        state.todayTransactions
            .filter {
                it.type == TransactionType.Expense && !state.isInvestmentTransaction(it) && !it.excludeFromSummary
            }
            .sumOf { it.amount }
    }
    val todayTotal = todayIncome + todayExpense
    val inWeight = if (todayTotal <= 0.0) 0.5f else (todayIncome / todayTotal).toFloat().coerceIn(0.08f, 0.92f)
    val outWeight = 1f - inWeight

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineColor)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    LocalDate.now().format(dayMonthFormatter),
                    color = TextDim,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier
                        .weight(inWeight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(MoneyGreen)
                )
                Box(
                    Modifier
                        .weight(outWeight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(LossRed)
                )
            }
            Row(Modifier.padding(top = 12.dp)) {
                Column {
                    Text("Income", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(
                        state.money(todayIncome),
                        color = MoneyGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Expense", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(
                        state.money(todayExpense),
                        color = LossRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

internal fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp, strokeWidth: androidx.compose.ui.unit.Dp = 1.5.dp): Modifier {
    return drawBehind {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 7.dp.toPx()), 0f)
        )
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(radius.toPx()),
            style = stroke
        )
    }
}

@Composable
internal fun EmptyStateCard(icon: ImageVector, title: String, caption: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .drawBehind {
                drawRoundRect(
                    color = LineColor,
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(horizontal = 20.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Navy850),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextDim, modifier = Modifier.size(28.dp))
        }
        Text(
            title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            caption,
            color = TextDim,
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}
