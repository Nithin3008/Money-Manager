package com.moneymanager.app.viewmodel

import com.moneymanager.app.model.BudgetWarning
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.month

internal object BudgetWarningCalculator {
    fun findBudgetWarning(state: FinanceUiState, transaction: LedgerTransaction): BudgetWarning? {
        if (transaction.type != TransactionType.Expense) return null
        if (state.isInvestmentTransaction(transaction)) return null
        if (transaction.excludeFromSummary) return null

        val matchingBudget = state.budgets.firstOrNull {
            it.month == transaction.month() && transaction.categoryId in it.categoryIds
        } ?: return null

        val spent = state.transactions
            .filter {
                it.type == TransactionType.Expense &&
                    !state.isInvestmentTransaction(it) &&
                    !it.excludeFromSummary &&
                    it.month() == matchingBudget.month &&
                    it.categoryId in matchingBudget.categoryIds
            }
            .sumOf { it.amount } + transaction.amount

        return if (spent > matchingBudget.limitAmount) {
            BudgetWarning(
                budgetName = matchingBudget.name,
                limitAmount = matchingBudget.limitAmount,
                spentAmount = spent
            )
        } else {
            null
        }
    }
}
