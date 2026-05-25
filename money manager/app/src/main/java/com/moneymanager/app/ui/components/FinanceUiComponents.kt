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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.transactionDate
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.Navy850
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.PrimaryBlue
import com.moneymanager.app.ui.theme.PrimarySoft
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary

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
            selectedLabelColor = if (isAmoledTheme()) Color(0xFF141414) else Color.White,
            containerColor = Navy800,
            labelColor = TextMuted
        )
    )
}

@Composable
internal fun TransactionTypeChip(type: TransactionType, selected: Boolean, onClick: () -> Unit) {
    val accent = if (type == TransactionType.Income) MoneyGreen else com.moneymanager.app.ui.theme.LossRed
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
            selectedLabelColor = if (selected && isAmoledTheme()) Color(0xFF141414) else accent,
            containerColor = if (isAmoledTheme()) Navy800 else Color.White,
            labelColor = TextMuted
        )
    )
}

@Composable
internal fun ActionPanel(
    title: String,
    subtitle: String,
    icon: ImageVector,
    action: String,
    onClick: () -> Unit
) {
    ElevatedPanel {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, PrimaryBlue)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = TextDim, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onClick) {
                Text(action, color = PrimarySoft)
            }
        }
    }
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
    state: FinanceUiState,
    onSelect: (Long) -> Unit
) {
    val category = state.categoriesById[transaction.categoryId]
    val color = categoryColor(category, transaction.type)
    val cardColor = if (isAmoledTheme()) Color(0xFF1A1A1A) else Color.White
    val neutralBorder = if (isAmoledTheme()) Color(0xFF323232) else Color(0xFFD8E5CC)
    val borderColor = if (category?.name == "Uncategorized") {
        neutralBorder
    } else {
        color.copy(alpha = if (isAmoledTheme()) 0.52f else 0.34f)
    }
    val dateLabel = transaction.transactionDate().mediumDateLabel()
    val categoryLabel = category?.name ?: "Set category"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(transaction.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = if (isAmoledTheme()) 0.18f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        transaction.name,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        transaction.signedAmount(state.currency),
                        color = transaction.type.amountColor(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        categoryLabel,
                        color = color,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.isCreditCardTransaction) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "CC",
                            color = OtherIncomeGold,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Text("  |  ", color = TextDim, style = MaterialTheme.typography.labelMedium)
                    Text(
                        dateLabel,
                        color = TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
