package com.moneymanager.app.data

import com.moneymanager.app.model.TransactionType

data class ParsedTransactionMessage(
    val bankName: String,
    val name: String,
    val amount: Double,
    val type: TransactionType,
    val counterparty: String,
    val rawMessage: String,
    val transactionTimestampMillis: Long = System.currentTimeMillis(),
    val accountHint: String? = null
)

object TransactionMessageParser {

    private val amountRegex = Regex(
        """(?i)(?:rs\.?|inr|rupees?)\s*([\d,]+(?:\.\d{1,2})?)"""
    )

    private val bankRegex = Regex(
        """(?i)\b(hdfc|icici|sbi|axis|kotak|yes bank|idfc|indusind|canara|union bank|pnb|bank of baroda|bob|""" +
            """indian bank|federal bank|bandhan|rbl|hsbc|standard chartered|scb|paytm payments bank|airtel payments bank|""" +
            """slice|onecard|fi money)\b"""
    )

    private val accountHintRegexes = listOf(
        Regex("""(?i)\b(?:a/c|acct|account|acc|ac)\s*(?:no\.?|number|ending|x+|xx+|[*]+)?\s*([0-9]{3,6})\b"""),
        Regex("""(?i)\b(?:ending|ended|no\.?)\s*(?:with|in)?\s*([0-9]{3,6})\b"""),
        Regex("""(?i)\b(?:x{2,}|[*]{2,})([0-9]{3,6})\b""")
    )

    // ICICI-style merchant before debited/credited.
    private val merchantSemicolonRegex = Regex(
        """(?i);\s*([A-Z0-9 .&_-]{2,40})\s+(?:debited|credited)"""
    )

    // Merchant after to/at/for for other bank formats.
    private val merchantToRegex = Regex(
        """(?i)(?:to|at|for|towards)\s+([a-z0-9 .&_-]{3,40})"""
    )

    fun parse(
        message: String,
        transactionTimestampMillis: Long = System.currentTimeMillis(),
        sender: String? = null
    ): ParsedTransactionMessage? {
        val normalized = message.replace('\n', ' ').trim()
        if (SmsTransactionNormalizer.isCreditCardDueReminder(normalized)) return null
        if (!looksLikeBankTransaction(normalized)) return null

        val amount = amountRegex.find(normalized)
            ?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null

        val lower = normalized.lowercase()

        // Prefer direct debit/credit phrases, then fall back to the first keyword occurrence.
        val type = when {
            lower.contains("debited for rs") -> TransactionType.Expense
            lower.contains("credited for rs") -> TransactionType.Income
            else -> {
                val debitIndex = debitWords.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                val creditIndex = creditWords.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                if (creditIndex < debitIndex) TransactionType.Income else TransactionType.Expense
            }
        }

        val senderLabel = sender?.let(::bankNameFromSender)
        val baseBankName = bankRegex.find(normalized)
            ?.value?.trim()?.uppercase()
            ?: senderLabel
            ?: "Bank"
        val accountHint = if (looksLikeCreditCard(normalized)) {
            null
        } else {
            extractAccountHint(normalized)
        }
        val bankName = accountHint?.let { "$baseBankName A/C $it" } ?: baseBankName

        // Try the semicolon pattern first, then fall back to to/at/for.
        var counterparty = (
                merchantSemicolonRegex.find(normalized)?.groupValues?.getOrNull(1)
                    ?: merchantToRegex.find(normalized)?.groupValues?.getOrNull(1)
                        ?.substringBefore(" on ")
                        ?.substringBefore(" ref")
                        ?.substringBefore(" using")
                    ?: if (type == TransactionType.Income) "Bank Credit" else "Bank Transaction"
                ).trim()

        // Avoid taking the amount as counterparty
        if (counterparty.lowercase().startsWith("rs")) {
             counterparty = if (type == TransactionType.Income) "Bank Credit" else "Bank Transaction"
        }

        counterparty = counterparty.take(28)

        return ParsedTransactionMessage(
            bankName = bankName,
            name = counterparty.replaceFirstChar { it.uppercase() },
            amount = amount,
            type = type,
            counterparty = counterparty.replaceFirstChar { it.uppercase() },
            rawMessage = message,
            transactionTimestampMillis = transactionTimestampMillis,
            accountHint = accountHint
        )
    }

    private val debitWords = listOf("debited", "debit", "spent", "paid", "withdrawn", "sent")
    private val creditWords = listOf("credited", "credit", "received", "deposited", "neft", "rtgs")

    private fun looksLikeBankTransaction(message: String): Boolean {
        val lower = message.lowercase()
        val hasMoney = amountRegex.containsMatchIn(message)
        val hasBankWord = listOf("neft", "rtgs", "debited", "credited", "debit", "credit", "upi", "a/c", "account")
            .any { it in lower }
        return hasMoney && hasBankWord
    }

    private fun extractAccountHint(message: String): String? {
        return accountHintRegexes.firstNotNullOfOrNull { regex ->
            regex.find(message)?.groupValues?.getOrNull(1)
        }
    }

    private fun looksLikeCreditCard(message: String): Boolean {
        val lower = message.lowercase()
        return listOf("credit card", "card bill", "cc payment", "card ending", "card no").any { it in lower }
    }

    private fun bankNameFromSender(sender: String): String? {
        val compact = sender.uppercase().filter { it.isLetterOrDigit() }
        return when {
            "HDFC" in compact -> "HDFC"
            "ICICI" in compact -> "ICICI"
            compact.contains("INDIANBK") || compact.contains("INDBNK") || compact.contains("INDIANBANK") -> "INDIAN BANK"
            "SBI" in compact -> "SBI"
            "AXIS" in compact -> "AXIS"
            "KOTAK" in compact -> "KOTAK"
            "CANARA" in compact -> "CANARA"
            "FEDERAL" in compact -> "FEDERAL BANK"
            "YESBANK" in compact -> "YES BANK"
            else -> null
        }
    }
}
