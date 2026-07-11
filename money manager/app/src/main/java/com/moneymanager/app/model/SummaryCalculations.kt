package com.moneymanager.app.model

import java.time.LocalDate
import java.time.YearMonth

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
        return state.transactions.filter {
            passesAccountFilter(state, it) &&
                affectsBankBalance(it) &&
                !state.isInvestmentTransaction(it)
        }
    }

    fun affectsBankBalance(tx: LedgerTransaction): Boolean {
        return tx.accountId != null
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

    /**
     * Summary rows for [FinanceUiState.selectedMonth]. Expenses always follow the calendar;
     * income follows [summaryIncomeMonth], so late-month credits shift forward when the
     * payroll-month setting is on.
     */
    fun monthTransactions(state: FinanceUiState): List<LedgerTransaction> {
        return state.transactions.filter { tx ->
            if (state.isInvestmentTransaction(tx)) return@filter false
            if (tx.excludeFromSummary) return@filter false
            if (!passesAccountFilter(state, tx)) return@filter false
            tx.summaryIncomeMonth(state.salaryShiftIncomeEnabled, state.salaryShiftWindowDays) == state.selectedMonth
        }
    }

    fun incomeCountsAsSalary(state: FinanceUiState, tx: LedgerTransaction): Boolean {
        if (tx.type != TransactionType.Income) return false
        val salaryId = state.salaryCategoryId
        if (salaryId != null && tx.categoryId == salaryId) return true
        return SalaryDetection.matchesConfirmedSalary(state, tx)
    }

    /** Income dated inside the calendar month, using the same exclusions as [monthTransactions]. */
    fun calendarMonthIncomeTotal(state: FinanceUiState): Double {
        return state.transactions
            .filter { tx ->
                if (tx.type != TransactionType.Income) return@filter false
                if (state.isInvestmentTransaction(tx)) return@filter false
                if (tx.excludeFromSummary) return@filter false
                if (!passesAccountFilter(state, tx)) return@filter false
                YearMonth.from(tx.transactionDate()) == state.selectedMonth
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
