package com.moneymanager.app.model

import java.time.LocalDate
import java.time.YearMonth

internal object SummaryCalculations {
    fun passesAccountFilter(state: FinanceUiState, tx: LedgerTransaction): Boolean {
        val activeIds = activeAccountIds(state)
        if (activeIds.isEmpty()) return true
        return tx.accountId in activeIds ||
            tx.fromAccountId in activeIds ||
            tx.toAccountId in activeIds
    }

    /**
     * The accounts the Reports summary is scoped to. An empty set means "All accounts" —
     * i.e. no filter, so every bank account is included (credit cards are excluded later
     * by the Bank-type checks in [signedMovement]/[selectedAccounts]).
     */
    fun activeAccountIds(state: FinanceUiState): Set<Long> {
        return state.summarySelectedAccountIds
    }

    fun selectedAccounts(state: FinanceUiState): List<BankAccount> {
        val activeIds = activeAccountIds(state)
        return if (activeIds.isEmpty()) {
            state.bankAccounts
        } else {
            state.bankAccounts.filter { it.id in activeIds }
        }
    }

    fun balanceAnchor(state: FinanceUiState): Double {
        return selectedAccounts(state).sumOf { it.balance }
    }

    fun balanceTransactions(state: FinanceUiState): List<LedgerTransaction> {
        // Investments are cash that actually left the bank, so they belong in balance
        // reconstruction (their zero-movement, account-less legacy rows drop out via
        // signedMovement == 0.0). They stay out of the income/expense/budget metrics elsewhere.
        return state.transactions.filter {
            passesAccountFilter(state, it) &&
                signedMovement(state, it) != 0.0
        }
    }

    fun affectsBankBalance(tx: LedgerTransaction): Boolean {
        return tx.type == TransactionType.Transfer ||
            (tx.accountId != null && !tx.isCreditCardTransaction)
    }

    fun signedMovement(state: FinanceUiState, tx: LedgerTransaction): Double {
        val activeIds = activeAccountIds(state)
        fun includes(account: BankAccount): Boolean {
            return account.type == AccountType.Bank && (activeIds.isEmpty() || account.id in activeIds)
        }

        if (tx.type == TransactionType.Transfer) {
            val from = tx.fromAccountId?.let { id -> state.accounts.firstOrNull { it.id == id } }
            val to = tx.toAccountId?.let { id -> state.accounts.firstOrNull { it.id == id } }
            val outgoing = from?.takeIf(::includes)?.let { -tx.amount } ?: 0.0
            val incoming = to?.takeIf(::includes)?.let { tx.amount } ?: 0.0
            return outgoing + incoming
        }

        val account = tx.accountId?.let { id -> state.accounts.firstOrNull { it.id == id } } ?: return 0.0
        if (!includes(account)) return 0.0
        if (tx.isCreditCardTransaction) return 0.0
        return if (tx.type == TransactionType.Income) tx.amount else -tx.amount
    }

    fun balanceBeforeDate(state: FinanceUiState, cutoff: LocalDate): Double {
        val movementFromCutoffToNow = balanceTransactions(state)
            .filter { !it.transactionDate().isBefore(cutoff) }
            .sumOf { signedMovement(state, it) }
        return balanceAnchor(state) - movementFromCutoffToNow
    }

    /** Summary rows for [FinanceUiState.selectedMonth], bucketed by calendar month. */
    fun monthTransactions(state: FinanceUiState): List<LedgerTransaction> {
        return state.transactions.filter { tx ->
            if (state.isInvestmentTransaction(tx)) return@filter false
            if (tx.excludeFromSummary) return@filter false
            if (!passesAccountFilter(state, tx)) return@filter false
            YearMonth.from(tx.transactionDate()) == state.selectedMonth
        }
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
            .sumOf { signedMovement(state, it) }
    }
}
