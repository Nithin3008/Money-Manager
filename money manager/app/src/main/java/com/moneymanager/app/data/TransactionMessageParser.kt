package com.moneymanager.app.data

import com.moneymanager.app.model.TransactionType

data class ParsedTransactionMessage(
    val bankName: String,
    val name: String,
    val amount: Double,
    val type: TransactionType,
    val counterparty: String,
    val rawMessage: String
)

object TransactionMessageParser {

    private val amountRegex = Regex(
        """(?i)(?:rs\.?|inr|rupees?)\s*([\d,]+(?:\.\d{1,2})?)"""
    )

    private val bankRegex = Regex(
        """(?i)\b(hdfc|icici|sbi|axis|kotak|yes bank|idfc|indusind|canara|union bank|pnb|bank of baroda|bob)\b"""
    )

    // ✅ Fixed: Removed '?' inside interval and simplified
    private val merchantSemicolonRegex = Regex(
        """(?i);\s*([A-Z0-9 .&_-]{2,40})\s+(?:debited|credited)"""
    )

    // ✅ Merchant after to/at/for (other banks)
    private val merchantToRegex = Regex(
        """(?i)(?:to|at|for|towards)\s+([a-z0-9 .&_-]{3,40})"""
    )

    fun parse(message: String): ParsedTransactionMessage? {
        val normalized = message.replace('\n', ' ').trim()
        if (!looksLikeBankTransaction(normalized)) return null

        val amount = amountRegex.find(normalized)
            ?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null

        val lower = normalized.lowercase()

        // ✅ Robust detection: Priority to "debited/credited for rs", then first occurring keyword
        val type = when {
            lower.contains("debited for rs") -> TransactionType.Expense
            lower.contains("credited for rs") -> TransactionType.Income
            else -> {
                val debitIndex = debitWords.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                val creditIndex = creditWords.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                if (creditIndex < debitIndex) TransactionType.Income else TransactionType.Expense
            }
        }

        val bankName = bankRegex.find(normalized)
            ?.value?.trim()?.uppercase()
            ?: "Bank"

        // ✅ Try semicolon pattern first (ICICI), then fallback to to/at/for
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
            rawMessage = message
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
}
