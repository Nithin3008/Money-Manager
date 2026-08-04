package com.moneymanager.app.model

internal object TransactionListCalculations {
    fun todayTransactions(state: FinanceUiState): List<LedgerTransaction> {
        // Home mirrors the default-account balance hero, so its "today" list stays scoped
        // to the default account (falls back to all accounts when there's no default).
        val activeIds = state.defaultAccountId
            ?.takeIf { id -> state.bankAccounts.any { it.id == id } }
            ?.let { setOf(it) }
            ?: emptySet()
        return state.transactions.filter {
            it.transactionDate() == java.time.LocalDate.now() &&
                (
                    activeIds.isEmpty() ||
                        it.accountId in activeIds ||
                        it.fromAccountId in activeIds ||
                        it.toAccountId in activeIds
                    )
        }
    }

    fun todayDetectedDrafts(state: FinanceUiState): List<DetectedTransactionDraft> {
        return state.detectedDrafts.filter { it.transactionDate() == java.time.LocalDate.now() }
    }

    fun activityTransactions(state: FinanceUiState): List<LedgerTransaction> {
        return state.transactions.filter {
            val date = it.transactionDate()
            !date.isBefore(state.activityStartDate) && !date.isAfter(state.activityEndDate)
        }
    }
}
