package com.moneymanager.app.model

internal object TransactionListCalculations {
    fun todayTransactions(state: FinanceUiState): List<LedgerTransaction> {
        val activeIds = SummaryCalculations.activeAccountIds(state)
        return state.transactions.filter {
            it.transactionDate() == java.time.LocalDate.now() &&
                (activeIds.isEmpty() || it.accountId in activeIds)
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
