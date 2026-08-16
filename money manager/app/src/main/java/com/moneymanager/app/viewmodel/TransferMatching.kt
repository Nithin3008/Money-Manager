package com.moneymanager.app.viewmodel

import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.TransactionType

internal object TransferMatching {
    fun manualTransferRowsFor(
        transaction: LedgerTransaction?,
        transactions: List<LedgerTransaction>
    ): List<LedgerTransaction> {
        val raw = transaction?.rawMessage.orEmpty()
        if (transaction == null || !transaction.excludeFromSummary || !raw.startsWith("Manual transfer from ")) {
            return emptyList()
        }
        return transactions
            .filter {
                it.excludeFromSummary &&
                    it.rawMessage == raw &&
                    it.amount == transaction.amount &&
                    kotlin.math.abs(it.timestampMillis - transaction.timestampMillis) <= 1_000L
            }
            .takeIf { rows ->
                rows.size >= 2 &&
                    rows.any { it.type == TransactionType.Expense } &&
                    rows.any { it.type == TransactionType.Income }
            }
            .orEmpty()
    }
}
