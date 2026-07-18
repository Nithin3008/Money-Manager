package com.moneymanager.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.ColorLibrary
import com.moneymanager.app.ui.theme.LineColor
import com.moneymanager.app.ui.theme.Navy850
import com.moneymanager.app.ui.theme.Navy900
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch

/**
 * "My colors" — the shared color library. One palette drives both the app accent and
 * category colors, so a color created here is reusable in both places.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorLibrarySheet(
    palette: List<String>,
    categories: List<CategoryItem>,
    accentHex: String,
    onDismiss: () -> Unit,
    onAccentApplied: (String) -> Unit,
    onColorCreated: (String) -> Unit,
    onColorRemoved: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(after: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { after() }
    }

    var pickerMode by remember { mutableStateOf<PickerMode?>(null) }
    var inspected by remember { mutableStateOf<String?>(null) }

    val usageByColor = remember(categories) {
        categories.groupBy { ColorLibrary.normalize(it.colorHex) }
    }
    val accent = ColorLibrary.normalize(accentHex)

    ModalBottomSheet(
        onDismissRequest = { close(onDismiss) },
        sheetState = sheetState,
        containerColor = Navy900,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(LineColor)
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("My colors", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Navy850)
                        .clickable { close(onDismiss) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(22.dp))
                }
            }

            Text(
                "One palette for everything. Colors you save here appear when picking an app accent and when creating categories.",
                color = TextDim,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
            )

            // Current accent callout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Navy850)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(colorFromHex(accent)))
                Column(Modifier.weight(1f)) {
                    Text("APP ACCENT", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
                    Text(ColorLibrary.nameOf(accent), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                }
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colorFromHex(accent))
                        .clickable { pickerMode = PickerMode.Accent }
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val ink = inkOn(colorFromHex(accent))
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = ink, modifier = Modifier.size(17.dp))
                    Text("Change", color = ink, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Palette", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${palette.size} colors", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            // Plain chunked grid: the sheet's column scrolls, so no nested lazy grid or
            // fixed-height math — rows lay out naturally and nothing below gets clipped.
            val tiles: List<String?> = listOf<String?>(null) + palette
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                tiles.chunked(4).forEach { rowTiles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        rowTiles.forEach { hex ->
                            if (hex == null) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.5.dp, LineColor, RoundedCornerShape(20.dp))
                                        .clickable { pickerMode = PickerMode.Create },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Create a color", tint = TextDim, modifier = Modifier.size(24.dp))
                                    Text("New", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                }
                            } else {
                                val color = colorFromHex(hex)
                                val isAccent = hex.equals(accent, ignoreCase = true)
                                val usedCount = usageByColor[ColorLibrary.normalize(hex)]?.size ?: 0
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(color)
                                        .border(
                                            width = if (isAccent) 3.dp else 0.dp,
                                            color = if (isAccent) TextPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable { inspected = if (inspected == hex) null else hex }
                                ) {
                                    if (isAccent) {
                                        Icon(
                                            Icons.Rounded.Star,
                                            contentDescription = "App accent",
                                            tint = inkOn(color),
                                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp)
                                        )
                                    }
                                    if (usedCount > 0) {
                                        Text(
                                            usedCount.toString(),
                                            color = inkOn(color),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                        repeat(4 - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                    Text("App accent", color = TextDim, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.size(14.dp).clip(RoundedCornerShape(5.dp)).background(Navy850),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("3", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("used by categories", color = TextDim, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            val focus = inspected
            if (focus != null) {
                val users = usageByColor[ColorLibrary.normalize(focus)].orEmpty()
                Text(
                    "Where “${ColorLibrary.nameOf(focus)}” is used",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
                if (users.isEmpty()) {
                    Text(
                        "Not used yet. Pick it when creating a category, or set it as your app accent.",
                        color = TextDim,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (!focus.equals(accent, true)) {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SmallAction("Set as accent") { close { onAccentApplied(focus) } }
                            SmallAction("Remove") { inspected = null; onColorRemoved(focus) }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        users.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, LineColor, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val tint = colorFromHex(focus).copy(alpha = if (isDarkTheme()) 0.18f else 0.13f)
                                Box(
                                    Modifier.size(36.dp).clip(RoundedCornerShape(16.dp)).background(tint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(category.icon, contentDescription = null, tint = colorFromHex(focus), modifier = Modifier.size(19.dp))
                                }
                                Text(category.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("Category", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colorFromHex(accent))
                    .clickable { pickerMode = PickerMode.Create },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                val ink = inkOn(colorFromHex(accent))
                Icon(Icons.Rounded.Add, contentDescription = null, tint = ink, modifier = Modifier.size(21.dp))
                Text("Create a color", color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    when (pickerMode) {
        PickerMode.Accent -> AccentPickerSheet(
            initialHex = accent,
            onDismiss = { pickerMode = null },
            onApply = { hex ->
                pickerMode = null
                onAccentApplied(hex)
            }
        )
        PickerMode.Create -> AccentPickerSheet(
            initialHex = accent,
            onDismiss = { pickerMode = null },
            onApply = { hex ->
                pickerMode = null
                onColorCreated(hex)
            }
        )
        null -> Unit
    }
}

private enum class PickerMode { Accent, Create }

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Navy850)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

private fun inkOn(color: Color): Color =
    if (color.luminanceCompat() > 0.42f) Color(0xFF10131A) else Color.White

private fun Color.luminanceCompat(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
