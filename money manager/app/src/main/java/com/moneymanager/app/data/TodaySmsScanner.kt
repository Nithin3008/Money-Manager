package com.moneymanager.app.data

import android.content.Context
import android.os.Environment
import android.provider.Telephony
import com.moneymanager.app.model.TransactionType
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SmsDebugExportResult(
    val filePath: String,
    val candidateCount: Int,
    val scannedCount: Int
)

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

    fun exportDebugMonth(month: YearMonth): SmsDebugExportResult {
        val startMillis = month.atDay(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val endMillis = month.atEndOfMonth().atTime(LocalTime.MAX)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val projection = arrayOf(Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.ADDRESS)
        val selection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?"
        val args = arrayOf(startMillis.toString(), endMillis.toString())
        val rows = mutableListOf<String>()
        var scannedCount = 0

        rows += listOf(
            "date",
            "sender",
            "parserDecision",
            "artifactReason",
            "parsedType",
            "parsedAmount",
            "parsedName",
            "rawSms"
        ).joinToString("\t")

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            args,
            "${Telephony.Sms.DATE} ASC"
        )?.use { cursor ->
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            while (cursor.moveToNext()) {
                scannedCount += 1
                val timestampMillis = cursor.getLong(dateIndex)
                val body = cursor.getString(bodyIndex).orEmpty()
                val sender = cursor.getString(addressIndex).orEmpty()
                if (!looksUsefulForParserDebug(body)) continue

                val parsed = TransactionMessageParser.parse(body, timestampMillis, sender)
                val artifactReason = creditCardArtifactReason(body, parsed?.type)
                val decision = when {
                    artifactReason.isNotBlank() -> "ignored"
                    parsed == null -> "unparsed"
                    parsed.requiresUserReview -> "needs_review"
                    else -> "auto_import"
                }
                rows += listOf(
                    formatSmsDate(timestampMillis),
                    sender,
                    decision,
                    artifactReason,
                    parsed?.type?.name.orEmpty(),
                    parsed?.amount?.toString().orEmpty(),
                    parsed?.name.orEmpty(),
                    body
                ).joinToString("\t") { it.cleanCell() }
            }
        }

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(dir, "money-manager-sms-debug-${month}.tsv")
        file.writeText(rows.joinToString("\n"))
        return SmsDebugExportResult(
            filePath = file.absolutePath,
            candidateCount = (rows.size - 1).coerceAtLeast(0),
            scannedCount = scannedCount
        )
    }

    private fun looksUsefulForParserDebug(message: String): Boolean {
        val lower = message.lowercase()
        val hasAmount = Regex("""(?i)\b(?:rs\.?|inr|rupees?)\s*[\d,]+(?:\.\d{1,2})?""").containsMatchIn(message)
        if (!hasAmount) return false
        return listOf(
            "debited",
            "credited",
            "debit",
            "credit",
            "spent",
            "paid",
            "received",
            "deposited",
            "withdrawn",
            "upi",
            "neft",
            "rtgs",
            "imps",
            "a/c",
            "account",
            "card",
            "statement",
            "bill",
            "due",
            "outstanding"
        ).any { it in lower }
    }

    private fun creditCardArtifactReason(message: String, parsedType: TransactionType?): String {
        return when {
            SmsTransactionNormalizer.isCreditCardStatementArtifact(message) -> "credit_card_statement"
            SmsTransactionNormalizer.isCreditCardDueReminder(message) -> "credit_card_due_reminder"
            SmsTransactionNormalizer.isCreditCardSettlementArtifact(message) -> "credit_card_settlement"
            parsedType != null && SmsTransactionNormalizer.isCreditCardRepaymentArtifact(message, parsedType) -> "credit_card_repayment"
            else -> ""
        }
    }

    private fun formatSmsDate(timestampMillis: Long): String {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
            Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault())
        )
    }

    private fun String.cleanCell(): String {
        return replace('\t', ' ')
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
