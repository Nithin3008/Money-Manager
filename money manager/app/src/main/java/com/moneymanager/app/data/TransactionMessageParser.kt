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
    val accountHint: String? = null,
    val requiresUserReview: Boolean = false,
    val isCreditCardTransaction: Boolean = false,
    val isInternalTransfer: Boolean = false,
    val excludeFromSummary: Boolean = false,
    val suggestedCategoryId: Long? = null,
    val categoryRequiresUserReview: Boolean = false
)

/**
 * Contract for an on-device LLM or compact local classifier.
 *
 * Implementations should run fully offline and return only fields supported by the message text.
 * The parser still validates critical fields before using them.
 */
fun interface LocalLlmTransactionInterpreter {
    fun interpret(
        message: String,
        sender: String?,
        categories: List<LocalLlmCategoryOption>
    ): LocalLlmTransactionInterpretation?
}

data class LocalLlmCategoryOption(
    val id: Long,
    val name: String
)

data class LocalLlmTransactionInterpretation(
    val amount: Double? = null,
    val type: TransactionType? = null,
    val bankName: String? = null,
    val accountHint: String? = null,
    val counterparty: String? = null,
    val isCreditCardTransaction: Boolean? = null,
    val isInternalTransfer: Boolean? = null,
    val suggestedCategoryId: Long? = null,
    val confidence: Double = 0.0
)

object TransactionMessageParser {

    private const val MIN_LOCAL_LLM_CONFIDENCE = 0.65
    private const val AMOUNT_EPSILON = 0.02

    @Volatile
    var localLlmInterpreter: LocalLlmTransactionInterpreter? = null

    private val amountRegex = Regex(
        """(?i)(?:rs\.?|r\.|inr|rupees?|₹)\s*([\d,]+(?:\.\d{1,2})?)"""
    )

    private val bankRegex = Regex(
        """(?i)\b(hdfc|icici|sbi|axis|kotak|yes bank|idfc|indusind|canara|union bank|pnb|bank of baroda|bob|""" +
            """indian bank|federal bank|bandhan|rbl|hsbc|standard chartered|scb|paytm payments bank|airtel payments bank|""" +
            """slice|onecard|fi money)\b"""
    )

    private val accountHintRegexes = listOf(
        Regex("""(?i)\b(?:a/c|acct|account|acc|ac)\s*(?:no\.?|number|ending|x+|xx+|[*]+)?\s*(?:x+|[*]+)?\s*([0-9]{3,6})\b"""),
        Regex("""(?i)\b(?:a/c|acct|account|acc|ac)(?:x+|[*]+)([0-9]{3,6})\b"""),
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
    private val cardSpentOnMerchantRegex = Regex(
        """(?i)\bspent\s+using\s+.+?\bon\s+\d{1,2}-[a-z]{3}-\d{2,4}\s+on\s+([a-z0-9 .&*_-]{3,40})"""
    )
    private val cardSpentAtMerchantRegex = Regex(
        """(?i)\bspent\s+(?:rs\.?|r\.|inr|rupees?|â‚¹)\s*[\d,]+(?:\.\d{1,2})?\s+on\s+.+?\s+at\s+([a-z0-9 .&*_-]{3,40})"""
    )
    private val cardUpiMerchantRegex = Regex(
        """(?i)\bcredit card\b.+?\bfor\s+upi-[0-9]+-([a-z0-9 .&*_-]{2,40})"""
    )

    fun parse(
        message: String,
        transactionTimestampMillis: Long = System.currentTimeMillis(),
        sender: String? = null,
        categories: List<LocalLlmCategoryOption> = emptyList(),
        useLocalLlm: Boolean = false
    ): ParsedTransactionMessage? {
        val normalized = message.replace('\n', ' ').trim()
        if (SmsTransactionNormalizer.isCreditCardDueReminder(normalized)) return null
        if (SmsTransactionNormalizer.isCreditCardSettlementArtifact(normalized)) return null
        if (SmsTransactionNormalizer.isCreditCardStatementArtifact(normalized)) return null
        if (!looksLikeBankTransaction(normalized)) return null

        val amountCandidates = extractAmounts(normalized)
        val llmInterpretation = if (useLocalLlm) {
            localLlmInterpreter
                ?.interpret(normalized, sender, categories)
                ?.takeIf { it.confidence >= MIN_LOCAL_LLM_CONFIDENCE }
        } else {
            null
        }
        val suggestedCategoryId = llmInterpretation
            ?.suggestedCategoryId
            ?.takeIf { suggested -> categories.any { it.id == suggested } }

        val amount = llmInterpretation
            ?.amount
            ?.takeIf { interpreted -> amountCandidates.any { sameAmount(it, interpreted) } }
            ?: amountCandidates.firstOrNull()
            ?: return null

        val detectedType = llmInterpretation?.type ?: detectTransactionType(normalized)
        val requiresUserReview = detectedType == null
        val type = detectedType ?: TransactionType.Expense
        val isCreditCardTransaction = llmInterpretation?.isCreditCardTransaction
            ?: SmsTransactionNormalizer.isCreditCardSpend(normalized)
        val isInternalTransfer = llmInterpretation?.isInternalTransfer
            ?: looksLikeInternalTransferMessage(normalized)

        val senderLabel = sender?.let(::bankNameFromSender)
        val baseBankName = llmInterpretation?.bankName?.cleanModelField(maxLength = 32)?.uppercase()
            ?: bankRegex.find(normalized)
            ?.value?.trim()?.uppercase()
            ?: senderLabel
            ?: "Bank"
        val accountHint = if (looksLikeCreditCard(normalized)) {
            null
        } else {
            llmInterpretation?.accountHint?.filter(Char::isDigit)?.takeLast(6)?.takeIf { it.length >= 3 }
                ?: extractAccountHint(normalized)
        }
        val bankName = accountHint?.let { "$baseBankName A/C $it" } ?: baseBankName

        val modelCounterparty = llmInterpretation?.counterparty
            ?.cleanModelField(maxLength = 40)
            ?.takeIf { it.isNotBlank() && !it.lowercase().startsWith("rs") }
        val iciciCounterparty = if (isIciciCreditCardBillDebit(normalized)) "Credit Card Bill" else null

        // Try the semicolon pattern first, then fall back to to/at/for.
        var counterparty = (
            modelCounterparty
                ?: iciciCounterparty
                ?: cardSpentOnMerchantRegex.find(normalized)?.groupValues?.getOrNull(1)
                ?: cardSpentAtMerchantRegex.find(normalized)?.groupValues?.getOrNull(1)
                ?: cardUpiMerchantRegex.find(normalized)?.groupValues?.getOrNull(1)
                ?: merchantSemicolonRegex.find(normalized)?.groupValues?.getOrNull(1)
                ?: merchantToRegex.find(normalized)?.groupValues?.getOrNull(1)
                    ?.substringBefore(" on ")
                    ?.substringBefore(" ref")
                    ?.substringBefore(" using")
                ?: if (isInternalTransfer) "Internal Transfer" else null
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
            accountHint = accountHint,
            requiresUserReview = requiresUserReview,
            isCreditCardTransaction = isCreditCardTransaction,
            isInternalTransfer = isInternalTransfer,
            excludeFromSummary = isInternalTransfer,
            suggestedCategoryId = suggestedCategoryId,
            categoryRequiresUserReview = suggestedCategoryId != null
        )
    }

    private val debitActionRegexes = listOf(
        Regex("""(?i)\bdebited\b(?:\s+(?:by|for|from|with))?\s*(?:rs\.?|r\.|inr|rupees?|â‚¹)?"""),
        Regex("""(?i)\bdebit\b\s+(?:of\s+)?(?:rs\.?|r\.|inr|rupees?|â‚¹)\s*[\d,]+(?:\.\d{1,2})?"""),
        Regex("""(?i)\b(?:spent|paid|withdrawn|sent)\b(?:\s+(?:by|for|from|to|with))?\s*(?:rs\.?|r\.|inr|rupees?|â‚¹)?"""),
        Regex("""(?i)(?:rs\.?|r\.|inr|rupees?|â‚¹)\s*[\d,]+(?:\.\d{1,2})?\s+(?:has\s+been\s+)?(?:debited|spent|paid|withdrawn|sent)\b""")
    )
    private val creditActionRegexes = listOf(
        Regex("""(?i)\bcredited\b(?:\s+(?:by|for|to|into|in|with))?\s*(?:rs\.?|r\.|inr|rupees?|â‚¹)?"""),
        Regex("""(?i)\bcredit\b\s+(?:of\s+)?(?:rs\.?|r\.|inr|rupees?|â‚¹)\s*[\d,]+(?:\.\d{1,2})?"""),
        Regex("""(?i)\b(?:received|deposited)\b(?:\s+(?:by|for|from|to|into|in|with))?\s*(?:rs\.?|r\.|inr|rupees?|â‚¹)?"""),
        Regex("""(?i)(?:rs\.?|r\.|inr|rupees?|â‚¹)\s*[\d,]+(?:\.\d{1,2})?\s+(?:has\s+been\s+)?(?:credited|received|deposited)\b""")
    )

    private fun detectTransactionType(message: String): TransactionType? {
        val debitIndex = debitActionRegexes
            .mapNotNull { it.find(message)?.range?.first }
            .minOrNull()
        val creditIndex = creditActionRegexes
            .mapNotNull { it.find(message)?.range?.first }
            .minOrNull()

        return when {
            debitIndex == null && creditIndex == null -> inferTypeFromFallbackKeywords(message)
            debitIndex == null -> TransactionType.Income
            creditIndex == null -> TransactionType.Expense
            debitIndex < creditIndex -> TransactionType.Expense
            else -> TransactionType.Income
        }
    }

    private fun inferTypeFromFallbackKeywords(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            listOf("neft", "rtgs", "imps").any { it in lower } &&
                listOf("received", "deposit", "inward").any { it in lower } -> TransactionType.Income
            listOf("neft", "rtgs", "imps").any { it in lower } &&
                listOf("sent", "outward", "transfer to").any { it in lower } -> TransactionType.Expense
            "refund" in lower || "cashback" in lower -> TransactionType.Income
            else -> null
        }
    }

    private fun looksLikeBankTransaction(message: String): Boolean {
        val lower = message.lowercase()
        val hasMoney = amountRegex.containsMatchIn(message)
        val hasBankWord = listOf("neft", "rtgs", "debited", "credited", "debit", "credit", "spent", "upi", "a/c", "account", "bank", "card")
            .any { it in lower }
        return hasMoney && hasBankWord
    }

    private fun extractAmounts(message: String): List<Double> {
        return amountRegex.findAll(message)
            .mapNotNull { match ->
                match.groupValues.getOrNull(1)
                    ?.replace(",", "")
                    ?.toDoubleOrNull()
            }
            .toList()
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

    private fun looksLikeInternalTransferMessage(message: String): Boolean {
        val lower = message.lowercase()
        val explicitHints = listOf(
            "transfer to own",
            "transfer from own",
            "own account",
            "own a/c",
            "my account",
            "my a/c",
            "your account",
            "your a/c",
            "self transfer",
            "to self",
            "from self",
            "between your accounts",
            "between your a/c",
            "internal transfer",
            "account to account",
            "a/c to a/c",
            "txn-a2a",
            "a2a transfer"
        )
        if (explicitHints.any { it in lower }) return true

        val hasTransferRail = listOf("transfer", "neft", "rtgs", "imps", "upi").any { it in lower }
        if (!hasTransferRail) return false

        val distinctAccountHints = accountHintRegexes
            .flatMap { regex -> regex.findAll(message).mapNotNull { it.groupValues.getOrNull(1) } }
            .map { it.takeLast(4) }
            .distinct()
        return distinctAccountHints.size >= 2 && listOf("own", "self", "my account", "your account").any { it in lower }
    }

    private fun isIciciCreditCardBillDebit(message: String): Boolean {
        val lower = message.lowercase()
        return "infobil*inft" in lower || "info bil*inft" in lower
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

    private fun String.cleanModelField(maxLength: Int): String {
        return replace(Regex("""[\t\r\n]+"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxLength)
    }

    private fun sameAmount(a: Double, b: Double): Boolean {
        return kotlin.math.abs(a - b) <= AMOUNT_EPSILON
    }
}
