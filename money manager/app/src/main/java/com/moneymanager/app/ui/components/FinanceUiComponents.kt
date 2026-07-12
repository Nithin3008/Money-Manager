package com.moneymanager.app.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.CurrencyOption
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.MoneyIcons
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.transactionDate
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.Navy850
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.OnAccent
import com.moneymanager.app.ui.theme.PrimaryBlue
import com.moneymanager.app.ui.theme.PrimarySoft
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import com.moneymanager.app.ui.theme.TransferBlue

/** Trims the font's asymmetric line padding so short labels sit optically centered
 *  inside fixed-height pills. */
private val pillLabelStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

@Composable
internal fun CategoryChoiceChip(
    category: CategoryItem,
    type: TransactionType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = categoryColor(category, type)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(category.name) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = accentContentColor(),
            containerColor = Navy800,
            labelColor = TextMuted
        )
    )
}

@Composable
internal fun TransactionTypeChip(type: TransactionType, selected: Boolean, onClick: () -> Unit) {
    val accent = when (type) {
        TransactionType.Income -> MoneyGreen
        TransactionType.Expense -> com.moneymanager.app.ui.theme.LossRed
        TransactionType.Transfer -> TextMuted
    }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "typeChipScale"
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(50),
        label = { Text(type.name, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (selected) accent else accent.copy(alpha = 0.14f),
            selectedLabelColor = if (selected) accentContentColor() else accent,
            containerColor = if (isAmoledTheme()) Navy800 else Color.White,
            labelColor = TextMuted
        )
    )
}

@Composable
internal fun DashboardPagination(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    label: String
) {
    if (pageCount <= 1) return

    ElevatedPanel {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = label,
                color = TextMuted,
                style = MaterialTheme.typography.labelMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pageCount) { index ->
                    val page = index + 1
                    MoneyChip(
                        label = page.toString(),
                        selected = page == currentPage,
                        onClick = { onPageSelected(page) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun TransactionRow(
    transaction: LedgerTransaction,
    categoriesById: Map<Long, CategoryItem>,
    accountsById: Map<Long, BankAccount>,
    currency: CurrencyOption,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val category = categoriesById[transaction.categoryId]
    val isTransfer = transaction.type == TransactionType.Transfer
    val rowIcon = when {
        isTransfer -> MoneyIcons.Account
        transaction.isCreditCardTransaction -> MoneyIcons.resolveCategoryIcon("credit_card")
        else -> category?.icon ?: MoneyIcons.Category
    }
    val accountName = transaction.accountId?.let { id -> accountsById[id]?.name }
        ?: transaction.smsBankLabel
    val categoryLabel = if (isTransfer) "Transfer" else category?.name ?: "Set category"
    val accountLabel = buildList {
        accountName?.takeIf { it.isNotBlank() }?.let(::add)
        if (transaction.isCreditCardTransaction) add("CC")
    }.joinToString(" · ")

    // Category-tinted icon tile + pill, per the Activity design.
    val ink = if (isTransfer) TransferBlue else categoryColor(category, transaction.type)
    val tint = ink.copy(alpha = if (isDarkTheme()) 0.18f else 0.13f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect(transaction.id) }
            .padding(horizontal = 2.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(42.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(tint),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = rowIcon,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(21.dp)
                )
            }
            if (transaction.isAutoDetected) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 3.dp, y = 3.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "Auto-detected",
                        tint = OnAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                transaction.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(tint)
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        categoryLabel,
                        color = ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        style = pillLabelStyle
                    )
                }
                if (accountLabel.isNotBlank()) {
                    Text(
                        accountLabel,
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
            transaction.signedAmount(currency),
            color = transaction.type.amountColor(),
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
