package com.moneymanager.app.viewmodel

import com.moneymanager.app.data.ParsedTransactionMessage
import com.moneymanager.app.data.SmsTransactionNormalizer
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.isCreditCardCategory

internal object CategoryLearning {
    fun inferCategoryId(
        msg: ParsedTransactionMessage,
        categories: List<CategoryItem>,
        transactions: List<LedgerTransaction>
    ): Long? {
        val uncategorizedId = categories.firstOrNull { it.name == "Uncategorized" }?.id ?: 0L
        val rawLower = msg.rawMessage.lowercase()
        val looksLikeCreditCardExpense = msg.type == TransactionType.Expense &&
            (
                "infobil*inft" in rawLower ||
                    "info bil*inft" in rawLower ||
                    listOf("credit card", "bank card", "card xx", "card ").any { it in rawLower }
                )
        if (msg.isCreditCardTransaction || looksLikeCreditCardExpense) {
            categories.firstOrNull { it.isCreditCardCategory() }?.id?.let {
                return it
            }
        }
        val key = categoryLearningKey(msg.counterparty, msg.rawMessage, msg.type, msg.bankName)
        if (key.isBlank()) return null
        return transactions
            .asSequence()
            .filter { it.categoryId != uncategorizedId }
            .filter { categoryLearningKey(it.name, it.rawMessage, it.type, it.smsBankLabel) == key }
            .groupingBy { it.categoryId }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    fun categoryLearningKey(
        name: String,
        rawMessage: String?,
        type: TransactionType,
        smsBankLabel: String?
    ): String {
        if (SmsTransactionNormalizer.isNonLedgerTransactionArtifact(rawMessage, type)) return ""
        val merchant = merchantFingerprint(name, rawMessage, smsBankLabel)
        if (merchant.isBlank()) return ""
        val source = rawMessage?.lowercase().orEmpty()
        val channel = when {
            "upi" in source -> "upi"
            "neft" in source -> "neft"
            "imps" in source -> "imps"
            "card" in source -> "card"
            else -> "sms"
        }
        return "${type.name.lowercase()}|$channel|$merchant"
    }

    private fun merchantFingerprint(name: String, rawMessage: String?, smsBankLabel: String?): String {
        val nameCandidate = normalizeMerchantCandidate(name, smsBankLabel)
        if (nameCandidate.isNotBlank()) return nameCandidate

        val raw = rawMessage.orEmpty()
        val candidates = listOfNotNull(
            Regex("""(?i);\s*([A-Z0-9 .&_-]{2,40})\s+(?:debited|credited)""")
                .find(raw)
                ?.groupValues
                ?.getOrNull(1),
            Regex("""(?i)\b(?:to|at|from|for|towards)\s+([a-z0-9 .&_-]{3,40})""")
                .find(raw)
                ?.groupValues
                ?.getOrNull(1)
                ?.substringBefore(" on ")
                ?.substringBefore(" ref")
                ?.substringBefore(" using")
        )
        return candidates
            .asSequence()
            .map { normalizeMerchantCandidate(it, smsBankLabel) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
    }

    private fun normalizeMerchantCandidate(candidate: String, smsBankLabel: String?): String {
        val bankTokens = smsBankLabel
            ?.lowercase()
            .orEmpty()
            .replace(Regex("""\b(?:rs\.?|inr|rupees?)\s*[\d,]+(?:\.\d{1,2})?\b"""), " ")
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .split(" ")
            .filter { it.length >= 3 }
            .toSet()
        val stopWords = setOf(
            "debited",
            "credited",
            "credit",
            "debit",
            "account",
            "bank",
            "card",
            "ending",
            "payment",
            "received",
            "amount",
            "available",
            "balance",
            "transaction",
            "reference",
            "your",
            "a/c",
            "paid",
            "sent",
            "from",
            "with"
        )
        val tokens = candidate
            .lowercase()
            .substringBefore("http")
            .replace(Regex("""\b(?:on|by)\s+\d{1,2}\b"""), " ")
            .replace(Regex("""\b(?:rs\.?|inr|rupees?)\s*[\d,]+(?:\.\d{1,2})?\b"""), " ")
            .replace(Regex("""\b\d{1,2}[-/][a-z]{3}[-/]\d{2,4}\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\b\d{1,2}[-/]\d{1,2}[-/]\d{2,4}\b"""), " ")
            .replace(Regex("""\b(?:ref|rrn|utr|txn|transaction|upi)[\s:.-]*[a-z0-9-]+\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\b[x*]*\d{3,}\b"""), " ")
            .replace(Regex("""\b\d+\b"""), " ")
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .split(" ")
            .map { it.trim() }
            .filter { it.length >= 3 }
            .filterNot { it in stopWords || it in bankTokens }
            .take(4)
        if (tokens.isEmpty()) return ""
        if (tokens.size == 1 && tokens.first() in setOf("hdfc", "icici", "axis", "kotak", "indian", "sbi")) return ""
        return tokens.joinToString(" ")
    }
}
