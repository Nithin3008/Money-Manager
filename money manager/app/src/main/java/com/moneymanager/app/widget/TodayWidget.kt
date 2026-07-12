package com.moneymanager.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.moneymanager.app.MainActivity
import com.moneymanager.app.R

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

/**
 * Today-at-a-glance widget: today's money in and out, plus the backlog of
 * uncategorized transactions. 2x1 shows the in/out amounts inline; 4x2 adds the
 * design's tinted panels and the uncategorized banner.
 */
class TodayWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadWidgetSnapshot(context)
        provideContent {
            val large = LocalSize.current.height >= LARGE.height
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(WidgetPalette.bg)
                    .cornerRadius(24.dp)
                    .padding(horizontal = 18.dp, vertical = if (large) 25.dp else 20.dp)
                    .clickable(openAppAction())
            ) {
                when {
                    !snapshot.registered -> SetupHint()
                    !large -> CompactContent(snapshot)
                    else -> FullContent(snapshot)
                }
            }
        }
    }

    private companion object {
        val SMALL = DpSize(110.dp, 40.dp)
        val LARGE = DpSize(250.dp, 110.dp)
    }
}

private fun openAppAction() = actionStartActivity<MainActivity>(
    actionParametersOf(WidgetOpenTab to "dashboard")
)

@Composable
private fun SetupHint() {
    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Set up Money Manager",
            style = TextStyle(color = WidgetPalette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            "Tap to finish onboarding",
            style = TextStyle(color = WidgetPalette.textDim, fontSize = 11.sp)
        )
    }
}

@Composable
private fun ColumnScope.CompactContent(snapshot: WidgetSnapshot) {
    LabelText("TODAY")
    Spacer(GlanceModifier.defaultWeight())
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        InlineStat(R.drawable.ic_widget_in, snapshot.inText, WidgetPalette.green)
        Spacer(GlanceModifier.defaultWeight())
        InlineStat(R.drawable.ic_widget_out, snapshot.outText, WidgetPalette.red)
        if (snapshot.uncategorizedCount > 0) {
            Spacer(GlanceModifier.width(12.dp))
            Row(
                modifier = GlanceModifier
                    .background(WidgetPalette.accentTint)
                    .cornerRadius(9.dp)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetIcon(R.drawable.ic_widget_bolt, WidgetPalette.accent, 13.dp)
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    snapshot.uncategorizedCount.toString(),
                    style = TextStyle(color = WidgetPalette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.FullContent(snapshot: WidgetSnapshot) {
    // Flexible spacers above and below keep the content group vertically centered no
    // matter how tall the widget is resized (root verticalAlignment is unreliable here).
    Spacer(GlanceModifier.defaultWeight())
    LabelText("TODAY · UPDATED ${snapshot.updatedAt}")
    Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 15.dp)) {
        TodayPanel(R.drawable.ic_widget_in, "IN TODAY", snapshot.inText, WidgetPalette.green, GlanceModifier.defaultWeight())
        Spacer(GlanceModifier.width(12.dp))
        TodayPanel(R.drawable.ic_widget_out, "OUT TODAY", snapshot.outText, WidgetPalette.red, GlanceModifier.defaultWeight())
    }
    val caughtUp = snapshot.uncategorizedCount == 0
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 15.dp)
            .background(WidgetPalette.accentTint)
            .cornerRadius(14.dp)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetIcon(
            if (caughtUp) R.drawable.ic_widget_check else R.drawable.ic_widget_bolt,
            WidgetPalette.accent,
            19.dp
        )
        Spacer(GlanceModifier.width(10.dp))
        Text(
            if (caughtUp) {
                "All transactions categorized"
            } else {
                "${snapshot.uncategorizedCount} uncategorized transaction${if (snapshot.uncategorizedCount == 1) "" else "s"}"
            },
            style = TextStyle(color = WidgetPalette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        if (!caughtUp) {
            WidgetIcon(R.drawable.ic_widget_chevron, WidgetPalette.accent, 19.dp)
        }
    }
    Spacer(GlanceModifier.defaultWeight())
}

@Composable
private fun InlineStat(icon: Int, amount: String, tone: androidx.glance.unit.ColorProvider) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WidgetIcon(icon, tone, 14.dp)
        Spacer(GlanceModifier.width(4.dp))
        Text(
            amount,
            style = TextStyle(color = tone, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@Composable
private fun TodayPanel(
    icon: Int,
    label: String,
    amount: String,
    tone: androidx.glance.unit.ColorProvider,
    modifier: GlanceModifier
) {
    Column(
        modifier = modifier
            .background(WidgetPalette.panel)
            .cornerRadius(14.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WidgetIcon(icon, tone, 15.dp)
            Spacer(GlanceModifier.width(5.dp))
            LabelText(label)
        }
        Text(
            amount,
            style = TextStyle(color = tone, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = GlanceModifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun LabelText(text: String) {
    Text(
        text,
        style = TextStyle(color = WidgetPalette.textDim, fontSize = 10.5.sp, fontWeight = FontWeight.Medium),
        maxLines = 1
    )
}

@Composable
private fun WidgetIcon(resId: Int, tint: androidx.glance.unit.ColorProvider, sizeDp: Dp) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = GlanceModifier.size(sizeDp)
    )
}
