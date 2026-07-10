package com.moneymanager.app.ui

import androidx.compose.ui.graphics.Color
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.CurrencyOption
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.ui.theme.LineColor
import com.moneymanager.app.ui.theme.LossRed
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.OnAccent
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.EnumMap
import java.util.Locale

internal val OtherIncomeGold = Color(0xFFFFC857)
private val mediumDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d")
private val parsedColorCache = mutableMapOf<String, Color>()
private val moneyFormatterCache = ThreadLocal<MutableMap<CurrencyOption, NumberFormat>>()

internal fun appBorderColor(): Color {
    return LineColor
}

internal fun appDividerColor(): Color {
    return LineColor
}

internal fun appTrackColor(): Color {
    return if (isAmoledTheme()) Color(0xFF303030) else Color(0xFFE8ECF2)
}

fun isAmoledTheme(): Boolean {
    return isDarkTheme()
}

internal fun isDarkTheme(): Boolean {
    return TextPrimary == Color(0xFFF1F1F1)
}

internal fun primaryContentColor(): Color {
    return OnAccent
}

internal fun accentContentColor(): Color {
    return OnAccent
}

internal fun categoryColor(category: CategoryItem?, type: TransactionType): Color {
    if (category == null) return TextMuted
    if (category.name == "Uncategorized") return if (isAmoledTheme()) Color(0xFF9B9B9B) else Color(0xFF737373)
    return colorFromHex(category.colorHex)
}

internal fun categoryColor(category: String, type: TransactionType): Color {
    return when (category) {
        "Uncategorized" -> if (isAmoledTheme()) Color(0xFF9B9B9B) else Color(0xFF737373)
        "Grocery" -> Color(0xFF38E68B)
        "Food" -> Color(0xFFFFC857)
        "Shopping" -> Color(0xFFFF4FB8)
        "Fuel" -> Color(0xFFFF8A3D)
        "Rent" -> Color(0xFFFF6B7A)
        else -> TextMuted
    }
}

internal fun colorFromHex(hex: String): Color {
    return synchronized(parsedColorCache) {
        parsedColorCache.getOrPut(hex) {
            runCatching {
                Color(android.graphics.Color.parseColor(hex))
            }.getOrDefault(TextDim)
        }
    }
}

internal fun FinanceUiState.money(value: Double): String {
    return money(value, currency)
}

internal fun money(value: Double, currency: CurrencyOption): String {
    val formatter = moneyFormatters().getOrPut(currency) {
        NumberFormat.getCurrencyInstance(Locale.US).apply {
            this.currency = Currency.getInstance(currency.currencyCode)
            maximumFractionDigits = 2
        }
    }
    return formatter.format(value)
}

private fun moneyFormatters(): MutableMap<CurrencyOption, NumberFormat> {
    moneyFormatterCache.get()?.let { return it }
    return EnumMap<CurrencyOption, NumberFormat>(CurrencyOption::class.java).also {
        moneyFormatterCache.set(it)
    }
}

internal fun LocalDate.mediumDateLabel(): String = format(mediumDateFormatter)

internal fun LocalDate.monthDayLabel(): String = format(monthDayFormatter)

internal fun compactMoney(value: Double, currency: CurrencyOption): String {
    val abs = kotlin.math.abs(value)
    val suffixValue = when {
        abs >= 1_000_000.0 -> abs / 1_000_000.0 to "M"
        abs >= 100_000.0 -> abs / 100_000.0 to "L"
        abs >= 1_000.0 -> abs / 1_000.0 to "K"
        else -> abs to ""
    }
    val number = if (suffixValue.first >= 10 || suffixValue.second.isEmpty()) {
        suffixValue.first.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", suffixValue.first).trimEnd('0').trimEnd('.')
    }
    return "${currency.symbol}$number${suffixValue.second}"
}

internal fun LedgerTransaction.signedAmount(currency: CurrencyOption): String {
    return signedAmount(amount, type, currency)
}

internal fun DetectedTransactionDraft.signedAmount(currency: CurrencyOption): String {
    return signedAmount(amount, type, currency)
}

internal fun signedAmount(amount: Double, type: TransactionType, currency: CurrencyOption): String {
    val prefix = if (type == TransactionType.Income) "+" else "-"
    return "$prefix${money(amount, currency)}"
}

internal fun TransactionType.amountColor(): Color {
    return if (this == TransactionType.Income) MoneyGreen else LossRed
}

internal fun TransactionType.bankVerb(): String {
    return if (this == TransactionType.Income) "credited" else "debited"
}
