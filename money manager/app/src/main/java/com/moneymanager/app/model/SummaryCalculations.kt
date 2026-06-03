package com.moneymanager.app.model

import java.time.LocalDate

internal object SummaryCalculations {
    fun passesAccountFilter(state: FinanceUiState, tx: LedgerTransaction): Boolean {
        val activeIds = activeAccountIds(state)
        if (activeIds.isEmpty()) return true
        val accountId = tx.accountId ?: return false
        return accountId in activeIds
    }

    fun activeAccountIds(state: FinanceUiState): Set<Long> {
        return when {
            state.summarySelectedAccountIds.isNotEmpty() -> state.summarySelectedAccountIds
            state.defaultAccountId != null && state.accounts.any { it.id == state.defaultAccountId } -> setOf(state.defaultAccountId)
            else -> emptySet()
        }
    }

    fun selectedAccounts(state: FinanceUiState): List<BankAccount> {
        val activeIds = activeAccountIds(state)
        return if (activeIds.isEmpty()) state.accounts else state.accounts.filter { it.id in activeIds }
    }

    fun balanceAnchor(state: FinanceUiState): Double {
        return selectedAccounts(state).sumOf { it.balance }
    }

    fun balanceTransactions(state: FinanceUiState): List<LedgerTransaction> {
        return state.transactions.filter { passesAccountFilter(state, it) && affectsBankBalance(it) }
    }

    fun affectsBankBalance(tx: LedgerTransaction): Boolean {
        return !tx.isCreditCardTransaction
    }

    fun signedMovement(tx: LedgerTransaction): Double {
        return if (tx.type == TransactionType.Income) tx.amount else -tx.amount
    }

    fun balanceBeforeDate(state: FinanceUiState, cutoff: LocalDate): Double {
        val movementFromCutoffToNow = balanceTransactions(state)
            .filter { !it.transactionDate().isBefore(cutoff) }
            .sumOf(::signedMovement)
        return balanceAnchor(state) - movementFromCutoffToNow
    }

    fun monthTransactions(state: FinanceUiState): List<LedgerTransaction> {
        val monthStart = state.selectedMonth.atDay(1)
        val monthEnd = state.selectedMonth.atEndOfMonth()
        return state.transactions.filter { tx ->
            if (tx.excludeFromSummary) return@filter false
            if (!passesAccountFilter(state, tx)) return@filter false
            val date = tx.transactionDate()
            !date.isBefore(monthStart) && !date.isAfter(monthEnd)
        }
    }

    fun incomeCountsAsSalary(state: FinanceUiState, tx: LedgerTransaction): Boolean {
        if (tx.type != TransactionType.Income) return false
        val salaryId = state.salaryCategoryId
        if (salaryId != null && tx.categoryId == salaryId) return true
        if (!state.salaryKeywordsForUncategorized) return false
        val uncategorizedId = state.categories.firstOrNull { it.name == "Uncategorized" }?.id ?: return false
        if (tx.categoryId != uncategorizedId) return false
        return SalaryIncomeRules.matchesSalaryKeywords(tx.name, tx.rawMessage)
    }

    fun calendarMonthIncomeTotal(state: FinanceUiState): Double {
        val monthStart = state.selectedMonth.atDay(1)
        val monthEnd = state.selectedMonth.atEndOfMonth()
        return balanceTransactions(state)
            .filter { tx ->
                if (tx.excludeFromSummary) return@filter false
                if (tx.type != TransactionType.Income) return@filter false
                val date = tx.transactionDate()
                !date.isBefore(monthStart) && !date.isAfter(monthEnd)
            }
            .sumOf { it.amount }
    }

    fun calendarMonthNet(state: FinanceUiState): Double {
        val monthStart = state.selectedMonth.atDay(1)
        val monthEnd = state.selectedMonth.atEndOfMonth()
        return balanceTransactions(state)
            .filter { tx ->
                val date = tx.transactionDate()
                !date.isBefore(monthStart) && !date.isAfter(monthEnd)
            }
            .sumOf(::signedMovement)
    }
}
