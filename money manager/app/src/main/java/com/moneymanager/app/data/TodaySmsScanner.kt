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

data class SmsScanProgress(
    val processed: Int,
    val total: Int
)

/**
 * A card-side "payment received" SMS. Alone it is a non-ledger artifact, but it names the
 * card ("...CREDIT CARD ENDING WITH 0887"), which the matching bank debit often does not
 * ("...debited; PhonePe credited") — pairing the two identifies a credit card bill payment.
 */
data class CardPaymentReceipt(
    val amount: Double,
    val timestampMillis: Long,
    val body: String
)

data class SmsScanBatch(
    val messages: List<ParsedTransactionMessage>,
    val cardPaymentReceipts: List<CardPaymentReceipt>
)

class TodaySmsScanner(private val context: Context) {

    fun scanToday(
        onProgress: (SmsScanProgress) -> Unit = {}
    ): SmsScanBatch =
        scanRange(LocalDate.now(), LocalDate.now(), onProgress)

    fun scanYesterday(
        onProgress: (SmsScanProgress) -> Unit = {}
    ): SmsScanBatch {
        val yesterday = LocalDate.now().minusDays(1)
        return scanRange(yesterday, yesterday, onProgress)
    }

    fun scanLast7Days(
        onProgress: (SmsScanProgress) -> Unit = {}
    ): SmsScanBatch {
        val today = LocalDate.now()
        return scanRange(today.minusDays(6), today, onProgress)
    }

    fun scanRange(
        startDate: LocalDate,
        endDate: LocalDate,
        onProgress: (SmsScanProgress) -> Unit = {}
    ): SmsScanBatch {
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val endMillis = endDate.atTime(LocalTime.MAX)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val messages = mutableListOf<ParsedTransactionMessage>()
        val projection = arrayOf(Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.ADDRESS)
        val selection = buildSmsCandidateSelection()
        val args = arrayOf(startMillis.toString(), endMillis.toString())
        val rows = mutableListOf<SmsCandidateRow>()

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
                rows += SmsCandidateRow(
                    timestampMillis = cursor.getLong(dateIndex),
                    body = cursor.getString(bodyIndex).orEmpty(),
                    sender = cursor.getString(addressIndex)
                )
            }
        }

        val total = rows.size
        val receipts = mutableListOf<CardPaymentReceipt>()
        onProgress(SmsScanProgress(processed = 0, total = total))
        rows.forEachIndexed { index, row ->
            val parsed = TransactionMessageParser.parse(
                message = row.body,
                transactionTimestampMillis = row.timestampMillis,
                sender = row.sender
            )
            if (parsed != null) {
                messages += parsed
            } else {
                cardPaymentReceiptFrom(row)?.let(receipts::add)
            }
            val processed = index + 1
            if (processed == total || processed % 2 == 0) {
                onProgress(SmsScanProgress(processed = processed, total = total))
            }
            if (processed % 4 == 0) {
                Thread.yield()
            }
        }

        return SmsScanBatch(messages = messages, cardPaymentReceipts = receipts)
    }

    private fun cardPaymentReceiptFrom(row: SmsCandidateRow): CardPaymentReceipt? {
        if (!SmsTransactionNormalizer.isCreditCardSettlementArtifact(row.body)) return null
        val amount = TransactionMessageParser.firstAmountIn(row.body) ?: return null
        if (amount <= 0.0) return null
        return CardPaymentReceipt(
            amount = amount,
            timestampMillis = row.timestampMillis,
            body = row.body
        )
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
        val selection = buildSmsCandidateSelection()
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

    private fun buildSmsCandidateSelection(): String {
        val body = Telephony.Sms.BODY
        val moneyClauses = listOf(
            "$body LIKE '%Rs%'",
            "$body LIKE '%INR%'",
            "$body LIKE '%rupees%'",
            "$body LIKE '%₹%'"
        )
        val bankClauses = listOf(
            "$body LIKE '%debited%'",
            "$body LIKE '%credited%'",
            "$body LIKE '%debit%'",
            "$body LIKE '%credit%'",
            "$body LIKE '%spent%'",
            "$body LIKE '%paid%'",
            "$body LIKE '%received%'",
            "$body LIKE '%deposited%'",
            "$body LIKE '%withdrawn%'",
            "$body LIKE '%upi%'",
            "$body LIKE '%NEFT%'",
            "$body LIKE '%RTGS%'",
            "$body LIKE '%IMPS%'",
            "$body LIKE '%a/c%'",
            "$body LIKE '%account%'",
            "$body LIKE '%bank%'",
            "$body LIKE '%card%'"
        )
        return "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ? " +
            "AND (${moneyClauses.joinToString(" OR ")}) " +
            "AND (${bankClauses.joinToString(" OR ")})"
    }

    private data class SmsCandidateRow(
        val timestampMillis: Long,
        val body: String,
        val sender: String?
    )

    private fun creditCardArtifactReason(message: String, parsedType: TransactionType?): String {
        return when {
            SmsTransactionNormalizer.isCreditCardStatementArtifact(message) -> "credit_card_statement"
            SmsTransactionNormalizer.isCreditCardDueReminder(message) -> "credit_card_due_reminder"
            SmsTransactionNormalizer.isCreditCardSettlementArtifact(message) -> "credit_card_settlement"
            SmsTransactionNormalizer.isPaymentAppCardConfirmation(message) -> "payment_app_card_duplicate"
            SmsTransactionNormalizer.isFailedTransactionArtifact(message) -> "failed_transaction"
            SmsTransactionNormalizer.isOtpVerificationArtifact(message) -> "otp_verification"
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
