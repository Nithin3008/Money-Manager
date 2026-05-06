package com.moneymanager.app.data

import android.content.Context
import android.provider.Telephony
import java.time.LocalDate
import java.time.ZoneId

class TodaySmsScanner(private val context: Context) {
    fun scanToday(): List<ParsedTransactionMessage> {
        val startMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val messages = mutableListOf<ParsedTransactionMessage>()
        val projection = arrayOf(Telephony.Sms.DATE, Telephony.Sms.BODY)
        val selection = "${Telephony.Sms.DATE} >= ?"
        val args = arrayOf(startMillis.toString())

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            args,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            while (cursor.moveToNext()) {
                val body = cursor.getString(bodyIndex)
                TransactionMessageParser.parse(body)?.let(messages::add)
            }
        }

        return messages
    }
}
