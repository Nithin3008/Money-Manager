package com.moneymanager.app.viewmodel

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal fun normalizedSmsRaw(raw: String?): String {
    return raw?.replace('\n', ' ')?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
}

internal fun isToday(timestampMillis: Long): Boolean {
    return Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate() == LocalDate.now()
}
