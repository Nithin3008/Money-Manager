package com.moneymanager.app.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

fun LedgerTransaction.month(): YearMonth {
    return YearMonth.from(transactionDate())
}

/**
 * Month bucket used on the Summary tab for income when payroll shift is enabled:
 * credits in the last [windowDays] of a calendar month move to the following month.
 */
fun LedgerTransaction.summaryIncomeMonth(
    salaryShiftIncomeEnabled: Boolean,
    salaryShiftWindowDays: Int
): YearMonth {
    val calendarMonth = YearMonth.from(transactionDate())
    if (!salaryShiftIncomeEnabled || type != TransactionType.Income) return calendarMonth
    val windowDays = salaryShiftWindowDays.coerceIn(1, 14)
    val date = transactionDate()
    val lastDayOfMonth = date.withDayOfMonth(date.lengthOfMonth())
    val windowStart = lastDayOfMonth.minusDays((windowDays - 1).toLong())
    return if (!date.isBefore(windowStart)) calendarMonth.plusMonths(1) else calendarMonth
}

fun LedgerTransaction.transactionDate(): LocalDate {
    return Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun DetectedTransactionDraft.transactionDate(): LocalDate {
    return Instant.ofEpochMilli(transactionTimestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun YearMonth.shortLabel(): String {
    return "${month.getDisplayName(TextStyle.SHORT, Locale.US)} $year"
}
