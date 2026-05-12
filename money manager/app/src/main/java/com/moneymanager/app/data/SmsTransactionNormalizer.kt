package com.moneymanager.app.data

import com.moneymanager.app.model.TransactionType
import kotlin.math.abs

/**
 * Post-parse filtering for SMS imports: drops paired transfer/CC-leg duplicates before DB insert.
 */
object SmsTransactionNormalizer {

    private const val PAIR_WINDOW_MS = 8 * 60 * 1000L
    private const val AMOUNT_EPSILON = 0.02

    fun filterImportBatch(messages: List<ParsedTransactionMessage>): List<ParsedTransactionMessage> {
        val withoutCardArtifacts = messages.filterNot { isNonLedgerTransactionArtifact(it.rawMessage, it.type) }
        if (withoutCardArtifacts.size <= 1) return withoutCardArtifacts
        val sorted = withoutCardArtifacts.sortedBy { it.transactionTimestampMillis }
        val droppedIndices = mutableSetOf<Int>()

        for (i in sorted.indices) {
            if (i in droppedIndices) continue
            val a = sorted[i]
            for (j in i + 1 until sorted.size) {
                if (j in droppedIndices) continue
                val b = sorted[j]
                if (b.transactionTimestampMillis - a.transactionTimestampMillis > PAIR_WINDOW_MS) break
                if (sameAmount(a.amount, b.amount) && a.type != b.type) {
                    when {
                        looksLikeCreditCardBillPair(a, b) -> {
                            val dropIncomeIdx = if (a.type == TransactionType.Income) i else j
                            droppedIndices.add(dropIncomeIdx)
                        }
                        looksLikeInternalTransfer(a, b) -> {
                            val dropIncomeIdx = if (a.type == TransactionType.Income) i else j
                            droppedIndices.add(dropIncomeIdx)
                        }
                    }
                }
            }
        }

        return sorted.filterIndexed { idx, msg ->
            idx !in droppedIndices && !isNonLedgerTransactionArtifact(msg.rawMessage, msg.type)
        }
    }

    fun isNonLedgerTransactionArtifact(rawMessage: String?, type: TransactionType): Boolean {
        return isCreditCardRepaymentArtifact(rawMessage, type) || isCreditCardDueReminder(rawMessage)
    }

    fun isCreditCardRepaymentArtifact(rawMessage: String?, type: TransactionType): Boolean {
        if (type != TransactionType.Income) return false
        val raw = rawMessage?.lowercase().orEmpty()
        val hasCard = listOf(
            "credit card",
            "cardmember",
            "card bill",
            "card payment",
            "cc payment",
            "card ending",
            "card no",
            "your card ending",
            "statement"
        ).any { it in raw }
        val hasPayment = listOf(
            "payment received",
            "payment of",
            "bill payment",
            "thank you for payment",
            "received towards",
            "credited towards",
            "credited to credit card",
            "credited to your card",
            "credited in your card",
            "credited to card",
            "credited to your card ending",
            "limit restored",
            "outstanding"
        ).any { it in raw }
        return hasCard && hasPayment
    }

    fun isCreditCardDueReminder(rawMessage: String?): Boolean {
        val raw = rawMessage?.lowercase().orEmpty()
        if (raw.isBlank()) return false
        val hasCard = listOf(
            "credit card",
            "cardmember",
            "card ending",
            "card no",
            "cc "
        ).any { it in raw }
        val hasDueReminder = listOf(
            "amount due",
            "total amount due",
            "minimum amount due",
            "amt due",
            "due date",
            "pay by",
            "pay instantly",
            "pay before",
            "payment due",
            "bill due"
        ).any { it in raw }
        val hasReminderAction = listOf(
            "payzapp",
            "bill pay",
            "statement",
            "https://",
            "http://"
        ).any { it in raw }
        return hasCard && hasDueReminder && (hasReminderAction || "pay" in raw)
    }

    private fun looksLikeCreditCardBillPair(a: ParsedTransactionMessage, b: ParsedTransactionMessage): Boolean {
        val texts = listOf(a.rawMessage.lowercase(), b.rawMessage.lowercase())
        val hasCc = texts.any {
            it.contains("credit card") ||
                it.contains("card bill") ||
                it.contains("card payment") ||
                it.contains("cc payment") ||
                it.contains("card ending")
        }
        return hasCc && (a.type != b.type)
    }

    private fun looksLikeInternalTransfer(a: ParsedTransactionMessage, b: ParsedTransactionMessage): Boolean {
        val combined = a.rawMessage.lowercase() + " " + b.rawMessage.lowercase()
        val transferHints = listOf(
            "transfer to own",
            "transfer from own",
            "internal transfer",
            "txn-a2a",
            "a/c transfer",
            "account transfer",
            "to self",
            "from self",
            "neft to",
            "imps to",
            "between your accounts"
        )
        if (transferHints.any { it in combined }) return true
        val sameNamedBankDifferentAccounts =
            a.bankName.substringBefore(" A/C ") == b.bankName.substringBefore(" A/C ") &&
                a.accountHint != null &&
                b.accountHint != null &&
                a.accountHint != b.accountHint
        val bankLikeCounterparties = listOf(a.counterparty.lowercase(), b.counterparty.lowercase())
            .all { it.contains("bank") || it.contains("transfer") || it.contains("self") }
        return sameNamedBankDifferentAccounts && bankLikeCounterparties
    }

    private fun sameAmount(a: Double, b: Double): Boolean = abs(a - b) <= AMOUNT_EPSILON
}
