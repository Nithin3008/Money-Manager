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
    val excludeFromSummary: Boolean = false
)

object TransactionMessageParser {

    /**
     * The user's bank-registered name (from the profile). UPI legs whose counterparty
     * matches it are money moving between the user's own accounts, not income/expense.
     */
    @Volatile
    var selfName: String? = null

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

    // ICICI credit format: "Acct XX317 is credited with Rs 25.00 on 12-Jun-26 from DEVATHI N NITHI. UPI:..."
    private val creditedFromRegex = Regex(
        """(?i)\bcredited\s+with\s+(?:rs\.?|inr)\s*[\d,]+(?:\.\d{1,2})?\s+on\s+\S+\s+from\s+([a-z0-9 &_-][a-z0-9 .&_-]{1,39})"""
    )

    // Auto-debit into the user's own deposit: "... debited Rs. 6,000.00 ... InfoTo RD Ac no 7 ..."
    private val ownDepositAutoDebitRegex = Regex(
        """(?i)info\s*to\s+(?:rd|fd)\s+ac"""
    )

    // Outward forex remittance: "... debited Rs. 19,999.68 ... InfoNRS*USD206.72 ..."
    private val foreignRemittanceRegex = Regex(
        """(?i)info\s*nrs\*"""
    )

    fun parse(
        message: String,
        transactionTimestampMillis: Long = System.currentTimeMillis(),
        sender: String? = null
    ): ParsedTransactionMessage? {
        val normalized = message.replace('\n', ' ').trim()
        if (SmsTransactionNormalizer.isFailedTransactionArtifact(normalized)) return null
        if (SmsTransactionNormalizer.isCreditCardDueReminder(normalized)) return null
        if (SmsTransactionNormalizer.isCreditCardSettlementArtifact(normalized)) return null
        if (SmsTransactionNormalizer.isCreditCardStatementArtifact(normalized)) return null
        if (SmsTransactionNormalizer.isPaymentAppCardConfirmation(normalized)) return null
        if (!looksLikeBankTransaction(normalized)) return null

        val amount = extractAmounts(normalized).firstOrNull() ?: return null

        val detectedType = detectTransactionType(normalized)
        val isForeignRemittance = foreignRemittanceRegex.containsMatchIn(normalized)
        val requiresUserReview = detectedType == null || isForeignRemittance
        val type = detectedType ?: TransactionType.Expense
        val isCreditCardTransaction = SmsTransactionNormalizer.isCreditCardSpend(normalized) ||
            SmsTransactionNormalizer.isCreditCardRefund(normalized)
        val isCreditCardBillPayment = SmsTransactionNormalizer.isCreditCardBillPaymentDebit(normalized)
        val isOwnDepositDebit = ownDepositAutoDebitRegex.containsMatchIn(normalized)
        val baseInternalTransfer = isCreditCardBillPayment ||
            isOwnDepositDebit ||
            looksLikeInternalTransferMessage(normalized)

        val senderLabel = sender?.let(::bankNameFromSender)
        val baseBankName = senderLabel
            ?: bankRegex.find(normalized)
                ?.value?.trim()?.uppercase()
            ?: "Bank"
        val isCardSpendAccount = isCreditCardTransaction && !isCreditCardBillPayment
        val accountHint = extractAccountHint(normalized)
        val bankName = accountHint?.let {
            if (isCardSpendAccount) "$baseBankName CARD $it" else "$baseBankName A/C $it"
        } ?: baseBankName

        val iciciCounterparty = if (isIciciCreditCardBillDebit(normalized)) "Credit Card Bill" else null

        // Try the semicolon pattern first, then fall back to to/at/for.
        var counterparty = (
            (if (isCreditCardBillPayment) "Credit Card Payment" else null)
                ?: iciciCounterparty
                ?: if (isOwnDepositDebit) "RD/FD Deposit" else null
                ?: if (isForeignRemittance) "Foreign Remittance" else null
                ?: cardSpentOnMerchantRegex.find(normalized)?.groupValues?.getOrNull(1)
                ?: cardSpentAtMerchantRegex.find(normalized)?.groupValues?.getOrNull(1)
                ?: cardUpiMerchantRegex.find(normalized)?.groupValues?.getOrNull(1)
                ?: merchantSemicolonRegex.find(normalized)?.groupValues?.getOrNull(1)
                ?: creditedFromRegex.find(normalized)?.groupValues?.getOrNull(1)
                    ?.substringBefore(".")
                ?: merchantToRegex.find(normalized)?.groupValues?.getOrNull(1)
                    ?.substringBefore(" on ")
                    ?.substringBefore(" ref")
                    ?.substringBefore(" using")
                ?: if (baseInternalTransfer) "Internal Transfer" else null
                ?: if (type == TransactionType.Income) "Bank Credit" else "Bank Transaction"
            ).trim()

        counterparty = cleanCounterparty(counterparty, type)

        val isSelfUpiLeg = !isCreditCardTransaction &&
            !isCreditCardBillPayment &&
            "upi" in normalized.lowercase() &&
            isSelfCounterparty(counterparty)
        val isInternalTransfer = baseInternalTransfer || isSelfUpiLeg

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
            excludeFromSummary = isInternalTransfer
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
            listOf("refund", "refunded", "reversal", "reversed", "cashback", "cash back").any { it in lower } -> TransactionType.Income
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
        // Interest, refunds, and cashback land in "your a/c" too but are real income,
        // never a leg of a self transfer.
        if (listOf("interest", "refund", "cashback", "reversal").any { it in lower }) return false
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

    /**
     * Matches a UPI counterparty against [selfName], tolerating bank-side truncation
     * ("NITHI" for "Nithin", "DEVATHI N NI" for "Devathi N Nithin") and one-character
     * typo/OCR noise ("NIKHI" vs "NITHI").
     */
    fun isSelfCounterparty(counterparty: String?): Boolean {
        val self = selfName?.trim().orEmpty()
        if (self.length < 4 || counterparty.isNullOrBlank()) return false
        val selfTokens = nameTokens(self)
        val otherTokens = nameTokens(counterparty)
        val anyTokenMatches = selfTokens.any { selfToken ->
            otherTokens.any { nearlySameNameToken(it, selfToken) }
        }
        return anyTokenMatches || truncatedNameSequenceMatches(
            smsTokens = nameTokens(counterparty, minLength = 1),
            selfTokens = nameTokens(self, minLength = 1)
        )
    }

    /**
     * Banks cut the receiver name at an arbitrary length, so the last SMS token can be
     * any prefix of the profile-name token ("NI" for "Nithin") as long as the earlier
     * tokens line up and enough characters survived to be unambiguous.
     */
    private fun truncatedNameSequenceMatches(smsTokens: List<String>, selfTokens: List<String>): Boolean {
        if (smsTokens.isEmpty() || smsTokens.size > selfTokens.size) return false
        var matchedChars = 0
        smsTokens.forEachIndexed { index, token ->
            val selfToken = selfTokens[index]
            val matches = if (index == smsTokens.lastIndex) {
                selfToken.startsWith(token)
            } else {
                token == selfToken || (token.length >= 4 && nearlySameNameToken(token, selfToken))
            }
            if (!matches) return false
            matchedChars += token.length
        }
        return matchedChars >= 6
    }

    private fun nameTokens(value: String, minLength: Int = 4): List<String> =
        value.lowercase().split(Regex("[^a-z]+")).filter { it.length >= minLength }

    private fun nearlySameNameToken(a: String, b: String): Boolean {
        if (a == b) return true
        val length = minOf(a.length, b.length)
        val maxEdits = if (length >= 5) 1 else 0
        return editDistanceAtMost(a, b, maxEdits) ||
            editDistanceAtMost(a.take(length), b.take(length), maxEdits)
    }

    private fun editDistanceAtMost(a: String, b: String, maxEdits: Int): Boolean {
        if (kotlin.math.abs(a.length - b.length) > maxEdits) return false
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, substitution)
            }
            if (current.min() > maxEdits) return false
            previous = current
        }
        return previous[b.length] <= maxEdits
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

    private fun cleanCounterparty(counterparty: String, type: TransactionType): String {
        val cleaned = counterparty
            .replace(Regex("""(?i)\s+(?:to\s+dispute|dispute)\b.*$"""), "")
            .replace(Regex("""(?i)\s*/\s*sms\b.*$"""), "")
            .replace(Regex("""(?i)\s+sms\s+block\b.*$"""), "")
            .replace(Regex("""(?i)\s+call\s+\d{5,}.*$"""), "")
            .replace(Regex("""(?i)[\s.]+\b(?:ref|using)\b.*$"""), "")
            .trim(' ', '.', ',', '-', '_', ';', ':')

        if (cleaned.isBlank() || cleaned.lowercase().startsWith("rs")) {
            return if (type == TransactionType.Income) "Bank Credit" else "Bank Transaction"
        }
        return cleaned.take(28)
    }
}
