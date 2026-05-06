package com.moneymanager.app.model

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

fun LedgerTransaction.month(): YearMonth {
    return YearMonth.from(
        Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    )
}

fun YearMonth.shortLabel(): String {
    return "${month.getDisplayName(TextStyle.SHORT, Locale.US)} $year"
}
