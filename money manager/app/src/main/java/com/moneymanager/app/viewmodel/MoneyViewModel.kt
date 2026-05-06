package com.moneymanager.app.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.data.TodaySmsScanner
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.BudgetPlan
import com.moneymanager.app.model.BudgetWarning
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.CurrencyOption
import com.moneymanager.app.model.DefaultCategories
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.MoneyIcons
import com.moneymanager.app.model.ScreenTab
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.month
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

class MoneyViewModel(application: Application) : AndroidViewModel(application) {
    private var nextAccountId = 1L
    private var nextCategoryId = DefaultCategories.items.maxOf { it.id } + 1
    private var nextTransactionId = 1L
    private var nextBudgetId = 1L
    private var nextDraftId = 1L

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState

    fun completeRegistration(name: String, accounts: List<Pair<String, Double>>) {
        val cleanAccounts = accounts
            .filter { it.first.isNotBlank() && it.second >= 0.0 }
            .map {
                BankAccount(
                    id = nextAccountId++,
                    name = it.first.trim(),
                    balance = it.second
                )
            }

        _uiState.update {
            it.copy(
                userName = name.trim(),
                accounts = cleanAccounts
            )
        }

        scanTodayMessages()
    }

    fun selectTab(tab: ScreenTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectMonth(month: YearMonth) {
        _uiState.update { it.copy(selectedMonth = month) }
    }

    fun selectCurrency(currency: CurrencyOption) {
        _uiState.update { it.copy(currency = currency) }
    }

    fun setTransactionSheet(open: Boolean) {
        _uiState.update { it.copy(showTransactionSheet = open) }
    }

    fun setBudgetSheet(open: Boolean) {
        _uiState.update { it.copy(showBudgetSheet = open) }
    }

    fun setCategorySheet(open: Boolean) {
        _uiState.update { it.copy(showCategorySheet = open) }
    }

    fun clearBudgetWarning() {
        _uiState.update { it.copy(budgetWarning = null) }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        val category = CategoryItem(
            id = nextCategoryId++,
            name = name.trim(),
            icon = MoneyIcons.Category,
            isDefault = false
        )
        _uiState.update {
            it.copy(
                categories = it.categories + category,
                showCategorySheet = false
            )
        }
    }

    fun addTransaction(
        name: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        accountId: Long?,
        rawMessage: String? = null,
        isAutoDetected: Boolean = false
    ) {
        if (name.isBlank() || amount <= 0.0) return
        val transaction = LedgerTransaction(
            id = nextTransactionId++,
            name = name.trim(),
            amount = amount,
            type = type,
            categoryId = categoryId,
            accountId = accountId,
            timestampMillis = System.currentTimeMillis(),
            isAutoDetected = isAutoDetected,
            rawMessage = rawMessage
        )

        _uiState.update { current ->
            val updatedTransactions = listOf(transaction) + current.transactions
            current.copy(
                transactions = updatedTransactions,
                showTransactionSheet = false,
                budgetWarning = findBudgetWarning(current.copy(transactions = updatedTransactions), transaction)
            )
        }
    }

    fun addBudget(name: String, limitAmount: Double, categoryIds: Set<Long>) {
        if (name.isBlank() || limitAmount <= 0.0 || categoryIds.isEmpty()) return
        val budget = BudgetPlan(
            id = nextBudgetId++,
            name = name.trim(),
            limitAmount = limitAmount,
            categoryIds = categoryIds,
            month = _uiState.value.selectedMonth
        )
        _uiState.update {
            it.copy(
                budgets = it.budgets + budget,
                showBudgetSheet = false
            )
        }
    }

    fun scanTodayMessages() {
        if (_uiState.value.isScanningMessages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isScanningMessages = true) }
            delay(900)

            val categories = _uiState.value.categories
            val parsedMessages = if (hasSmsPermission()) {
                TodaySmsScanner(getApplication()).scanToday()
            } else {
                emptyList()
            }
            val drafts = parsedMessages.map {
                DetectedTransactionDraft(
                    id = nextDraftId++,
                    name = it.name,
                    amount = it.amount,
                    type = it.type,
                    rawMessage = it.rawMessage,
                    suggestedCategoryId = suggestCategoryId(it.name),
                    detectedAtMillis = System.currentTimeMillis()
                )
            }.ifEmpty {
                demoDetectedDrafts(categories)
            }

            _uiState.update {
                it.copy(
                    detectedDrafts = drafts,
                    isScanningMessages = false
                )
            }
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun suggestCategoryId(name: String): Long? {
        val lower = name.lowercase()
        val categories = _uiState.value.categories
        return when {
            listOf("amazon", "store", "shop").any { it in lower } -> categories.find { it.name == "Shopping" }?.id
            listOf("food", "cafe", "restaurant", "swiggy", "zomato").any { it in lower } -> categories.find { it.name == "Food" }?.id
            listOf("fuel", "petrol", "diesel").any { it in lower } -> categories.find { it.name == "Fuel" }?.id
            else -> categories.firstOrNull()?.id
        }
    }

    private fun demoDetectedDrafts(categories: List<CategoryItem>): List<DetectedTransactionDraft> {
        return listOf(
            DetectedTransactionDraft(
                id = nextDraftId++,
                name = "Amazon UPI",
                amount = 45.00,
                type = TransactionType.Expense,
                rawMessage = "Rs.45 debited via UPI to Amazon today",
                suggestedCategoryId = categories.find { it.name == "Shopping" }?.id,
                detectedAtMillis = System.currentTimeMillis()
            ),
            DetectedTransactionDraft(
                id = nextDraftId++,
                name = "Salary Credit",
                amount = 6450.00,
                type = TransactionType.Income,
                rawMessage = "INR 6450 credited through NEFT",
                suggestedCategoryId = categories.firstOrNull()?.id,
                detectedAtMillis = System.currentTimeMillis()
            )
        )
    }

    fun acceptDetectedTransaction(draftId: Long, categoryId: Long) {
        val draft = _uiState.value.detectedDrafts.firstOrNull { it.id == draftId } ?: return
        addTransaction(
            name = draft.name,
            amount = draft.amount,
            type = draft.type,
            categoryId = categoryId,
            accountId = _uiState.value.accounts.firstOrNull()?.id,
            rawMessage = draft.rawMessage,
            isAutoDetected = true
        )
        _uiState.update {
            it.copy(detectedDrafts = it.detectedDrafts.filterNot { item -> item.id == draftId })
        }
    }

    fun ignoreDetectedTransaction(draftId: Long) {
        _uiState.update {
            it.copy(detectedDrafts = it.detectedDrafts.filterNot { item -> item.id == draftId })
        }
    }

    private fun findBudgetWarning(state: FinanceUiState, transaction: LedgerTransaction): BudgetWarning? {
        if (transaction.type != TransactionType.Expense) return null

        val matchingBudget = state.budgets.firstOrNull {
            it.month == transaction.month() && transaction.categoryId in it.categoryIds
        } ?: return null

        val spent = state.transactions
            .filter {
                it.type == TransactionType.Expense &&
                    it.month() == matchingBudget.month &&
                    it.categoryId in matchingBudget.categoryIds
            }
            .sumOf { it.amount }

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
