package com.moneymanager.app.ui

import androidx.compose.ui.graphics.Color
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.CurrencyOption
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.ui.theme.LossRed
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal val OtherIncomeGold = Color(0xFFFFC857)

internal fun appBorderColor(): Color {
    return if (isAmoledTheme()) Color(0xFF343D4E) else Color(0xFFC9D3E6)
}

internal fun appDividerColor(): Color {
    return if (isAmoledTheme()) Color(0xFF343D4E) else Color(0xFFD7DEED)
}

internal fun appTrackColor(): Color {
    return if (isAmoledTheme()) Navy800 else Color(0xFFDCE4F3)
}

fun isAmoledTheme(): Boolean {
    return TextPrimary == Color(0xFFF8FAFF)
}

internal fun categoryColor(category: CategoryItem?, type: TransactionType): Color {
    if (category == null) return TextMuted
    if (category.name == "Uncategorized") return if (isAmoledTheme()) Color(0xFFA7B3CC) else Color(0xFF7D889D)
    if (type == TransactionType.Income) return MoneyGreen
    return colorFromHex(category.colorHex)
}

internal fun categoryColor(category: String, type: TransactionType): Color {
    if (type == TransactionType.Income) return MoneyGreen
    return when (category) {
        "Uncategorized" -> if (isAmoledTheme()) Color(0xFFA7B3CC) else Color(0xFF7D889D)
        "Grocery" -> Color(0xFF38E68B)
        "Food" -> Color(0xFFFFC857)
        "Shopping" -> Color(0xFFFF4FB8)
        "Fuel" -> Color(0xFFFF8A3D)
        "Rent" -> Color(0xFFFF6B7A)
        else -> TextMuted
    }
}

internal fun colorFromHex(hex: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(TextDim)
}

internal fun FinanceUiState.money(value: Double): String {
    return money(value, currency)
}

internal fun money(value: Double, currency: CurrencyOption): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    formatter.currency = Currency.getInstance(currency.currencyCode)
    formatter.maximumFractionDigits = 2
    return formatter.format(value)
}

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
