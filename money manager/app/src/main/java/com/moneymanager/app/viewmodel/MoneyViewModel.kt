package com.moneymanager.app.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.data.FinanceDatabase
import com.moneymanager.app.data.FinanceRepository
import com.moneymanager.app.data.TodaySmsScanner
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.BudgetPlan
import com.moneymanager.app.model.MessageScanRange
import java.time.LocalDate
import com.moneymanager.app.model.BudgetWarning
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.CurrencyOption
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.MoneyIcons
import com.moneymanager.app.model.ScreenTab
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.month
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

class MoneyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(FinanceDatabase.get(application).dao())

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState

    init {
        reload()
    }

    fun completeRegistration(name: String, accounts: List<Pair<String, Double>>) {
        viewModelScope.launch {
            repository.saveSettings(name.trim(), _uiState.value.currency)
            accounts
                .filter { it.first.isNotBlank() && it.second >= 0.0 }
                .forEach { repository.addAccount(it.first.trim(), it.second) }
            reloadState()
            scanTodayMessages()
        }
    }

    fun selectTab(tab: ScreenTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                activityTransactionPage = if (tab == ScreenTab.Activity) 1 else it.activityTransactionPage
            )
        }
    }

    fun loadMoreTransactions() {
        _uiState.update { state ->
            if (!state.hasMoreTransactions) state
            else state.copy(activityTransactionPage = state.activityTransactionPage + 1)
        }
    }

    fun selectMonth(month: YearMonth) {
        _uiState.update { it.copy(selectedMonth = month) }
    }

    fun selectCurrency(currency: CurrencyOption) {
        viewModelScope.launch {
            repository.saveSettings(_uiState.value.userName, currency)
            _uiState.update { it.copy(currency = currency) }
        }
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

    fun addCategory(name: String, iconKey: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(
                CategoryItem(
                    id = System.currentTimeMillis(),
                    name = name.trim(),
                    iconKey = iconKey,
                    icon = MoneyIcons.resolveCategoryIcon(iconKey),
                    isDefault = false
                )
            )
            reloadState { it.copy(showCategorySheet = false) }
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
        viewModelScope.launch {
            val transaction = LedgerTransaction(
                id = 0,
                name = name.trim(),
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                timestampMillis = System.currentTimeMillis(),
                isAutoDetected = isAutoDetected,
                rawMessage = rawMessage
            )
            repository.addTransaction(transaction)
            val warning = findBudgetWarning(_uiState.value, transaction)
            reloadState { it.copy(showTransactionSheet = false, budgetWarning = warning) }
        }
    }

    fun addBudget(name: String, limitAmount: Double, categoryIds: Set<Long>) {
        if (name.isBlank() || limitAmount <= 0.0 || categoryIds.isEmpty()) return
        viewModelScope.launch {
            repository.addBudget(
                BudgetPlan(
                    id = 0,
                    name = name.trim(),
                    limitAmount = limitAmount,
                    categoryIds = categoryIds,
                    month = _uiState.value.selectedMonth
                )
            )
            reloadState { it.copy(showBudgetSheet = false) }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            reloadState()
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
            reloadState()
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            repository.deleteAccount(id)
            reloadState()
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomCategory(id)
            reloadState()
        }
    }

    fun scanTodayMessages() {
        scanMessages(MessageScanRange.Today)
    }

    fun scanMessages(range: MessageScanRange, startDate: LocalDate? = null, endDate: LocalDate? = null) {
        if (_uiState.value.isScanningMessages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isScanningMessages = true) }
            val parsedMessages = if (hasSmsPermission()) {
                when (range) {
                    MessageScanRange.Today -> TodaySmsScanner(getApplication()).scanToday()
                    MessageScanRange.Yesterday -> TodaySmsScanner(getApplication()).scanYesterday()
                    MessageScanRange.Week -> TodaySmsScanner(getApplication()).scanLast7Days()
                    MessageScanRange.Custom -> {
                        val today = LocalDate.now()
                        val start = startDate ?: today
                        val end = endDate?.coerceAtMost(today) ?: today
                        if (start > end) {
                            emptyList()
                        } else {
                            TodaySmsScanner(getApplication()).scanRange(start, end)
                        }
                    }
                }
            } else {
                emptyList()
            }

            val existingRawMessages = _uiState.value.detectedDrafts.map { it.rawMessage }.toSet()
            parsedMessages
                .filterNot { it.rawMessage in existingRawMessages }
                .forEach {
                    repository.saveDraft(
                        DetectedTransactionDraft(
                            id = 0,
                            bankName = it.bankName,
                            name = it.counterparty,
                            amount = it.amount,
                            type = it.type,
                            counterparty = it.counterparty,
                            rawMessage = it.rawMessage,
                            suggestedCategoryId = suggestCategoryId(it.counterparty),
                            detectedAtMillis = System.currentTimeMillis()
                        )
                    )
                }

            reloadState { it.copy(isScanningMessages = false) }
        }
    }

    fun acceptDetectedTransaction(draftId: Long, categoryId: Long) {
        viewModelScope.launch {
            val draft = _uiState.value.detectedDrafts.firstOrNull { it.id == draftId } ?: return@launch
            repository.addTransaction(
                LedgerTransaction(
                    id = 0,
                    name = draft.counterparty,
                    amount = draft.amount,
                    type = draft.type,
                    categoryId = categoryId,
                    accountId = _uiState.value.accounts.firstOrNull()?.id,
                    timestampMillis = System.currentTimeMillis(),
                    isAutoDetected = true,
                    rawMessage = draft.rawMessage
                )
            )
            repository.deleteDraft(draftId)
            reloadState()
        }
    }

    fun ignoreDetectedTransaction(draftId: Long) {
        viewModelScope.launch {
            repository.deleteDraft(draftId)
            reloadState()
        }
    }

    private fun reload() {
        viewModelScope.launch {
            runCatching { reloadState() }
                .onFailure {
                    _uiState.update { state -> state.copy(isAppInitializing = false) }
                }
        }
    }

    private suspend fun reloadState(transform: (FinanceUiState) -> FinanceUiState = { it }) {
        val current = _uiState.value
        val loaded = repository.loadState(current).copy(
            isAppInitializing = false,
            selectedTab = current.selectedTab,
            selectedMonth = current.selectedMonth,
            activityTransactionPage = current.activityTransactionPage,
            showTransactionSheet = current.showTransactionSheet,
            showBudgetSheet = current.showBudgetSheet,
            showCategorySheet = current.showCategorySheet,
            isScanningMessages = current.isScanningMessages,
            budgetWarning = current.budgetWarning
        )
        _uiState.value = transform(loaded)
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
            listOf("amazon", "store", "shop", "mall").any { it in lower } -> categories.find { it.name == "Shopping" }?.id
            listOf("food", "cafe", "restaurant", "hotel", "swiggy", "zomato").any { it in lower } -> categories.find { it.name == "Food" }?.id
            listOf("fuel", "petrol", "diesel").any { it in lower } -> categories.find { it.name == "Fuel" }?.id
            else -> categories.firstOrNull()?.id
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
