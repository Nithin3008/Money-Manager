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
        pattern = """(?i)(?:rs\.?|inr|rupees?)\s*([\d,]+(?:\.\d{1,2})?)"""
    )
    private val debitWords = listOf("debited", "debit", "spent", "paid", "withdrawn", "upi")
    private val creditWords = listOf("credited", "credit", "received", "deposited", "neft", "rtgs")
    private val merchantRegex = Regex(
        pattern = """(?i)(?:to|at|for|towards)\s+([a-z0-9 .&_-]{3,40})"""
    )
    private val bankRegex = Regex(
        pattern = """(?i)\b(hdfc|icici|sbi|axis|kotak|yes bank|idfc|indusind|canara|union bank|pnb|bank of baroda|bob)\b"""
    )

    fun parse(message: String): ParsedTransactionMessage? {
        val normalized = message.replace('\n', ' ').trim()
        if (!looksLikeBankTransaction(normalized)) return null

        val amount = amountRegex.find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null

        val lower = normalized.lowercase()
        val type = when {
            creditWords.any { it in lower } && !debitWords.any { it in lower } -> TransactionType.Income
            "credited" in lower || "received" in lower -> TransactionType.Income
            else -> TransactionType.Expense
        }

        val bankName = bankRegex.find(normalized)
            ?.value
            ?.trim()
            ?.uppercase()
            ?: "Bank"

        val counterparty = merchantRegex.find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.substringBefore(" on ")
            ?.substringBefore(" ref")
            ?.substringBefore(" using")
            ?.take(28)
            ?: if (type == TransactionType.Income) "Bank Credit" else "Bank Transaction"

        return ParsedTransactionMessage(
            bankName = bankName,
            name = counterparty.replaceFirstChar { it.uppercase() },
            amount = amount,
            type = type,
            counterparty = counterparty.replaceFirstChar { it.uppercase() },
            rawMessage = message
        )
    }

    private fun looksLikeBankTransaction(message: String): Boolean {
        val lower = message.lowercase()
        val hasMoney = amountRegex.containsMatchIn(message)
        val hasBankWord = listOf("neft", "rtgs", "debited", "credited", "debit", "credit", "upi", "a/c", "account")
            .any { it in lower }
        return hasMoney && hasBankWord
    }
}
