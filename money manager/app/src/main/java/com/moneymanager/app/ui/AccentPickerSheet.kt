package com.moneymanager.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.theme.LineColor
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.Navy850
import com.moneymanager.app.ui.theme.Navy900
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val WHEEL_DP = 190f
private const val RING_FRACTION = 0.73f
private val PRESET_HEXES = listOf("#B4F077", "#7EA2FF", "#3FE0C4", "#B794F6", "#F5C542")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccentPickerSheet(
    initialHex: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(after: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { after() }
    }

    val initial = remember(initialHex) { hueBrightnessOf(initialHex) }
    var hue by remember { mutableFloatStateOf(initial.first) }
    var brightness by remember { mutableFloatStateOf(initial.second) }

    val working = accentFromHueBrightness(hue, brightness)
    val ink = inkFor(working)
    val hex = working.toHex()
    val pureHue = hsvColor(hue, 1f, 1f)
    val hueRing = remember { (0..360 step 30).map { hsvColor(it.toFloat(), 1f, 1f) } }

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Accent color", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
                    Text("Drag around the wheel", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Navy800)
                        .clickable { close(onDismiss) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }

            // Hue wheel with center readout disc + draggable thumb.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(WHEEL_DP.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { hue = angleFromCenter(it, size.width, size.height) }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ -> hue = angleFromCenter(change.position, size.width, size.height) }
                        }
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        val radius = size.minDimension / 2f
                        drawCircle(brush = Brush.sweepGradient(hueRing, center = center), radius = radius)
                        drawCircle(color = Navy900, radius = radius * 0.46f)
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(Navy900)
                            .border(1.dp, LineColor, CircleShape),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(working))
                        Text(hex, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    val center = WHEEL_DP / 2f
                    val ringR = center * RING_FRACTION
                    val rad = Math.toRadians(hue.toDouble())
                    val tx = center + ringR * cos(rad).toFloat()
                    val ty = center + ringR * sin(rad).toFloat()
                    Box(
                        modifier = Modifier
                            .offset(x = (tx - 15f).dp, y = (ty - 15f).dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(working)
                            .border(3.5.dp, Color.White, CircleShape)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Brightness", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.horizontalGradient(listOf(Color.Black, pureHue, Color.White)))
                            .pointerInput(Unit) {
                                detectTapGestures { brightness = (it.x / widthPx).coerceIn(0f, 1f) }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ -> brightness = (change.position.x / widthPx).coerceIn(0f, 1f) }
                            }
                    )
                    val thumbPx = with(LocalDensity.current) { 24.dp.toPx() }
                    val thumbX = with(LocalDensity.current) { (brightness * (widthPx - thumbPx)).toDp() }
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = thumbX)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(working)
                            .border(3.5.dp, Color.White, CircleShape)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                PRESET_HEXES.forEach { preset ->
                    val presetColor = colorFromHex(preset)
                    val selected = hex.equals(preset, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(presetColor)
                            .border(
                                width = if (selected) 2.5.dp else 0.dp,
                                color = if (selected) TextPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                val hb = hueBrightnessOf(preset)
                                hue = hb.first
                                brightness = hb.second
                            }
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Navy850)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("#", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(hex.removePrefix("#"), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Navy850)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(working),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Live preview", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("Buttons, chips & highlights", color = TextDim, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier.height(34.dp).clip(RoundedCornerShape(999.dp)).background(working).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Button", color = ink, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Navy800)
                        .clickable { close(onDismiss) }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(working)
                        .clickable { close { onApply(hex) } },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                    Text("Apply accent", color = ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun angleFromCenter(pos: Offset, width: Int, height: Int): Float {
    val deg = Math.toDegrees(atan2((pos.y - height / 2f).toDouble(), (pos.x - width / 2f).toDouble())).toFloat()
    return ((deg % 360f) + 360f) % 360f
}

private fun hsvColor(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

/** Maps the black → pure-hue → white brightness track (t in 0..1) to the final accent color. */
private fun accentFromHueBrightness(hue: Float, t: Float): Color {
    val pure = hsvColor(hue, 1f, 1f)
    return if (t <= 0.5f) lerp(Color.Black, pure, (t * 2f).coerceIn(0f, 1f))
    else lerp(pure, Color.White, ((t - 0.5f) * 2f).coerceIn(0f, 1f))
}

private fun inkFor(color: Color): Color = if (color.luminance() > 0.42f) Color(0xFF10131A) else Color.White

private fun Color.toHex(): String {
    val r = (red * 255f).roundToInt()
    val g = (green * 255f).roundToInt()
    val b = (blue * 255f).roundToInt()
    return "#%02X%02X%02X".format(r, g, b)
}

/** Inverse of [accentFromHueBrightness]: recovers a wheel hue + brightness position from a hex. */
private fun hueBrightnessOf(hex: String): Pair<Float, Float> {
    val argb = runCatching { android.graphics.Color.parseColor(hex) }
        .getOrDefault(android.graphics.Color.parseColor("#7EA2FF"))
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    val t = if (hsv[2] < 0.999f) hsv[2] / 2f else 1f - hsv[1] / 2f
    return hsv[0] to t.coerceIn(0f, 1f)
}
