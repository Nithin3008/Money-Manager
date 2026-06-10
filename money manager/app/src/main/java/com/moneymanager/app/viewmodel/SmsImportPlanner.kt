package com.moneymanager.app.viewmodel

import com.moneymanager.app.data.ParsedTransactionMessage
import com.moneymanager.app.data.SmsBankKeys
import com.moneymanager.app.data.SmsTransactionNormalizer
import com.moneymanager.app.model.AccountType
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.TransactionType
import kotlin.math.abs

internal sealed class PlannedSmsImport {
    abstract val timestampMillis: Long
    open val rawMessages: List<String> = emptyList()

    data class Message(
        val message: ParsedTransactionMessage
    ) : PlannedSmsImport() {
        override val timestampMillis: Long = message.transactionTimestampMillis
        override val rawMessages: List<String> = listOf(message.rawMessage)
    }

    data class Transfer(
        val debit: ParsedTransactionMessage,
        val credit: ParsedTransactionMessage?,
        val fromAccountId: Long,
        val toAccountId: Long,
        val source: SmsTransferSource
    ) : PlannedSmsImport() {
        override val timestampMillis: Long = credit
            ?.let { minOf(debit.transactionTimestampMillis, it.transactionTimestampMillis) }
            ?: debit.transactionTimestampMillis

        override val rawMessages: List<String>
            get() = listOfNotNull(debit.rawMessage, credit?.rawMessage)
    }

    data class TransferReview(
        val debit: ParsedTransactionMessage,
        val credit: ParsedTransactionMessage?,
        val fromAccountId: Long?,
        val toAccountId: Long?,
        val source: SmsTransferSource
    ) : PlannedSmsImport() {
        override val timestampMillis: Long = credit
            ?.let { minOf(debit.transactionTimestampMillis, it.transactionTimestampMillis) }
            ?: debit.transactionTimestampMillis

        override val rawMessages: List<String>
            get() = listOfNotNull(debit.rawMessage, credit?.rawMessage)
    }
}

internal enum class SmsTransferSource {
    BankTransfer,
    CreditCardPayment
}

internal object SmsImportPlanner {
    private const val PAIR_WINDOW_MS = 8 * 60 * 1000L
    private const val AMOUNT_EPSILON = 0.02

    fun plan(
        messages: List<ParsedTransactionMessage>,
        accounts: List<BankAccount>
    ): List<PlannedSmsImport> {
        if (messages.isEmpty()) return emptyList()
        val sorted = messages.sortedBy { it.transactionTimestampMillis }
        val used = mutableSetOf<Int>()
        val plannedTransfers = mutableListOf<PlannedSmsImport>()
        val bankAccounts = accounts.filter { it.type == AccountType.Bank }
        val creditCardAccounts = accounts.filter { it.type == AccountType.CreditCard }

        sorted.forEachIndexed { index, message ->
            if (index in used) return@forEachIndexed
            val plan = creditCardPaymentTransfer(message, bankAccounts, creditCardAccounts) ?: return@forEachIndexed
            used += index
            plannedTransfers += plan
        }

        for (i in sorted.indices) {
            if (i in used) continue
            val first = sorted[i]
            if (first.amount <= 0.0 || first.isCreditCardTransaction) continue

            for (j in i + 1 until sorted.size) {
                if (j in used) continue
                val second = sorted[j]
                if (second.transactionTimestampMillis - first.transactionTimestampMillis > PAIR_WINDOW_MS) break
                val transfer = bankTransfer(first, second, bankAccounts) ?: continue
                used += i
                used += j
                plannedTransfers += transfer
                break
            }
        }

        sorted.forEachIndexed { index, message ->
            if (index in used) return@forEachIndexed
            val review = singleLegBankTransferReview(message, bankAccounts) ?: return@forEachIndexed
            used += index
            plannedTransfers += review
        }

        val plannedByRaw = plannedTransfers
            .flatMap { plan -> plan.rawMessages.map { it to plan } }
            .toMap()

        return buildList {
            addAll(plannedTransfers)
            sorted.forEachIndexed { index, message ->
                if (index !in used && message.rawMessage !in plannedByRaw) {
                    add(PlannedSmsImport.Message(message))
                }
            }
        }.sortedWith(
            compareBy<PlannedSmsImport> { it.timestampMillis }
                .thenBy { if (it is PlannedSmsImport.Transfer) 0 else 1 }
        )
    }

    private fun creditCardPaymentTransfer(
        message: ParsedTransactionMessage,
        bankAccounts: List<BankAccount>,
        creditCardAccounts: List<BankAccount>
    ): PlannedSmsImport? {
        if (!SmsTransactionNormalizer.isCreditCardBillPaymentDebit(message.rawMessage)) return null
        val fromAccountId = resolveKnownAccountId(message.bankName, bankAccounts)
        val toAccountId = resolveCreditCardAccount(message, creditCardAccounts)
        val debit = message.copy(
            isInternalTransfer = true,
            excludeFromSummary = true,
            isCreditCardTransaction = false
        )
        if (fromAccountId != null && toAccountId != null && fromAccountId != toAccountId) {
            return PlannedSmsImport.Transfer(
                debit = debit,
                credit = null,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                source = SmsTransferSource.CreditCardPayment
            )
        }
        return PlannedSmsImport.TransferReview(
            debit = message.copy(
                isInternalTransfer = true,
                excludeFromSummary = true,
                isCreditCardTransaction = false
            ),
            credit = null,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            source = SmsTransferSource.CreditCardPayment
        )
    }

    private fun bankTransfer(
        first: ParsedTransactionMessage,
        second: ParsedTransactionMessage,
        bankAccounts: List<BankAccount>
    ): PlannedSmsImport? {
        if (!sameAmount(first.amount, second.amount)) return null
        if (first.type == second.type) return null
        if (first.isCreditCardTransaction || second.isCreditCardTransaction) return null

        val debit = if (first.type == TransactionType.Expense) first else second
        val credit = if (first.type == TransactionType.Income) first else second
        if (SmsTransactionNormalizer.isCreditCardBillPaymentDebit(debit.rawMessage)) return null

        val fromAccountId = resolveKnownAccountId(debit.bankName, bankAccounts)
        val toAccountId = resolveKnownAccountId(credit.bankName, bankAccounts)
        val resolvesToTwoUserAccounts = fromAccountId != null &&
            toAccountId != null &&
            fromAccountId != toAccountId
        if (!resolvesToTwoUserAccounts && !looksLikeOwnBankTransfer(debit, credit)) return null

        val transferDebit = debit.copy(isInternalTransfer = true, excludeFromSummary = true)
        val transferCredit = credit.copy(isInternalTransfer = true, excludeFromSummary = true)

        if (fromAccountId != null && toAccountId != null && fromAccountId != toAccountId) {
            return PlannedSmsImport.Transfer(
                debit = transferDebit,
                credit = transferCredit,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                source = SmsTransferSource.BankTransfer
            )
        }
        return PlannedSmsImport.TransferReview(
            debit = transferDebit,
            credit = transferCredit,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            source = SmsTransferSource.BankTransfer
        )
    }

    private fun singleLegBankTransferReview(
        message: ParsedTransactionMessage,
        bankAccounts: List<BankAccount>
    ): PlannedSmsImport.TransferReview? {
        if (message.amount <= 0.0) return null
        if (message.isCreditCardTransaction) return null
        if (SmsTransactionNormalizer.isCreditCardBillPaymentDebit(message.rawMessage)) return null
        if (!looksLikeSingleLegOwnTransfer(message)) return null

        val accountId = resolveKnownAccountId(message.bankName, bankAccounts)
        val transferMessage = message.copy(
            isInternalTransfer = true,
            excludeFromSummary = true
        )
        return when (message.type) {
            TransactionType.Expense -> PlannedSmsImport.TransferReview(
                debit = transferMessage,
                credit = null,
                fromAccountId = accountId,
                toAccountId = null,
                source = SmsTransferSource.BankTransfer
            )
            TransactionType.Income -> PlannedSmsImport.TransferReview(
                debit = transferMessage,
                credit = null,
                fromAccountId = null,
                toAccountId = accountId,
                source = SmsTransferSource.BankTransfer
            )
            TransactionType.Transfer -> null
        }
    }

    private fun resolveCreditCardAccount(
        message: ParsedTransactionMessage,
        creditCardAccounts: List<BankAccount>
    ): Long? {
        if (creditCardAccounts.isEmpty()) return null
        if (creditCardAccounts.size == 1) return creditCardAccounts.first().id

        val cardHint = SmsBankKeys.cardHint(message.rawMessage)
            ?: SmsBankKeys.cardHint(message.bankName)
            ?: return null
        val bankRoot = SmsBankKeys.bankRoot(message.bankName).ifBlank { "CARD" }
        return SmsBankKeys.resolveAccountId("$bankRoot CARD $cardHint", creditCardAccounts)
    }

    private fun resolveKnownAccountId(
        smsBankLabel: String?,
        accounts: List<BankAccount>
    ): Long? {
        if (accounts.isEmpty()) return null
        val label = smsBankLabel?.trim().orEmpty()
        if (label.isBlank()) return null
        if (accounts.size == 1) {
            val account = accounts.single()
            return account.id.takeIf { SmsBankKeys.accountNameMatchesLabel(account, label) }
        }
        return SmsBankKeys.resolveAccountId(label, accounts)
    }

    private fun looksLikeOwnBankTransfer(
        debit: ParsedTransactionMessage,
        credit: ParsedTransactionMessage
    ): Boolean {
        if (debit.isInternalTransfer || credit.isInternalTransfer) return true

        val combined = "${debit.rawMessage} ${credit.rawMessage}".lowercase()
        val hasTransferRail = listOf("transfer", "neft", "rtgs", "imps", "upi", "utr").any { it in combined }
        val bothHaveAccountHints = debit.accountHint != null && credit.accountHint != null
        val hasSelfHint = listOf(
            "own account",
            "own a/c",
            "self transfer",
            "to self",
            "from self",
            "between your accounts",
            "internal transfer",
            "account to account",
            "a/c to a/c"
        ).any { it in combined }
        return hasSelfHint || (hasTransferRail && bothHaveAccountHints)
    }

    private fun looksLikeSingleLegOwnTransfer(message: ParsedTransactionMessage): Boolean {
        if (message.isInternalTransfer) return true
        val raw = message.rawMessage.lowercase()
        val hasTransferRail = listOf("transfer", "neft", "rtgs", "imps", "upi", "utr").any { it in raw }
        if (!hasTransferRail) return false
        val hasSelfHint = listOf(
            "own account",
            "own a/c",
            "self transfer",
            "to self",
            "from self",
            "between your accounts",
            "internal transfer",
            "account to account",
            "a/c to a/c"
        ).any { it in raw }
        return hasSelfHint
    }

    private fun sameAmount(a: Double, b: Double): Boolean = abs(a - b) <= AMOUNT_EPSILON
}
