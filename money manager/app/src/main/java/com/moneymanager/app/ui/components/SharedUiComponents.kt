package com.moneymanager.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.PrimaryBlue
import com.moneymanager.app.ui.theme.PrimarySoft
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary

@Composable
internal fun LargeTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = PrimarySoft, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun SectionHeader(title: String, action: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            color = PrimarySoft,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(12.dp))
        Text(
            action,
            color = PrimaryBlue,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 2
        )
    }
}

@Composable
internal fun ElevatedPanel(
    modifier: Modifier = Modifier,
    animateSize: Boolean = false,
    content: @Composable () -> Unit
) {
    val panelModifier = if (animateSize) {
        modifier.fillMaxWidth().animateContentSize(tween(260, easing = FastOutSlowInEasing))
    } else {
        modifier.fillMaxWidth()
    }
    Card(
        modifier = panelModifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, appBorderColor())
    ) {
        content()
    }
}

@Composable
internal fun EmptyPanel(text: String) {
    ElevatedPanel {
        Text(text, color = TextDim, modifier = Modifier.padding(18.dp), textAlign = TextAlign.Center)
    }
}

@Composable
internal fun IconTile(icon: ImageVector, tint: Color) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = if (isAmoledTheme()) 0.16f else 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
internal fun LabelText(text: String) {
    Text(text, color = PrimarySoft, style = MaterialTheme.typography.labelMedium, letterSpacing = 0.sp)
}

@Composable
internal fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
internal fun MoneyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val dark = isAmoledTheme()
    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(50),
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryBlue,
            selectedLabelColor = primaryContentColor(),
            containerColor = if (dark) Navy800 else Color.White,
            labelColor = TextMuted
        )
    )
}
