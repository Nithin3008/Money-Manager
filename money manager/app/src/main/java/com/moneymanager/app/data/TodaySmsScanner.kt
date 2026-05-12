package com.moneymanager.app.data

import android.content.Context
import android.provider.Telephony
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TodaySmsScanner(private val context: Context) {
    fun scanToday(): List<ParsedTransactionMessage> = scanRange(LocalDate.now(), LocalDate.now())

    fun scanYesterday(): List<ParsedTransactionMessage> {
        val yesterday = LocalDate.now().minusDays(1)
        return scanRange(yesterday, yesterday)
    }

    fun scanLast7Days(): List<ParsedTransactionMessage> {
        val today = LocalDate.now()
        return scanRange(today.minusDays(6), today)
    }

    fun scanRange(startDate: LocalDate, endDate: LocalDate): List<ParsedTransactionMessage> {
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val endMillis = endDate.atTime(LocalTime.MAX)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val messages = mutableListOf<ParsedTransactionMessage>()
        val projection = arrayOf(Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.ADDRESS)
        val selection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?"
        val args = arrayOf(startMillis.toString(), endMillis.toString())

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            args,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            while (cursor.moveToNext()) {
                val timestampMillis = cursor.getLong(dateIndex)
                val body = cursor.getString(bodyIndex)
                val sender = cursor.getString(addressIndex)
                TransactionMessageParser.parse(body, timestampMillis, sender)?.let(messages::add)
            }
        }

        return messages
    }
}
