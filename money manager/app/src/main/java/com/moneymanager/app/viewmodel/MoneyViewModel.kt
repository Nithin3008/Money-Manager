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
import com.moneymanager.app.model.ActivityDateFilter
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.MoneyIcons
import com.moneymanager.app.model.ScreenTab
import com.moneymanager.app.model.ThemeMode
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.month
import java.time.Instant
import java.time.ZoneId
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
            repository.saveSettings(name.trim(), _uiState.value.currency, _uiState.value.themeMode)
            accounts
                .filter { it.first.isNotBlank() && it.second >= 0.0 }
                .forEach { repository.addAccount(it.first.trim(), it.second) }
            reloadState()
            scanMessages(
                range = MessageScanRange.Custom,
                startDate = LocalDate.now().minusMonths(3),
                endDate = LocalDate.now()
            )
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

    fun selectDashboardTransactionPage(page: Int) {
        _uiState.update { state ->
            state.copy(dashboardTransactionPage = page.coerceIn(1, state.dashboardTransactionPageCount))
        }
    }

    fun selectDashboardDraftPage(page: Int) {
        _uiState.update { state ->
            state.copy(dashboardDraftPage = page.coerceIn(1, state.dashboardDraftPageCount))
        }
    }

    fun selectMonth(month: YearMonth) {
        _uiState.update { it.copy(selectedMonth = month) }
    }

    fun setActivityDateFilter(
        filter: ActivityDateFilter,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ) {
        val today = LocalDate.now()
        val range = when (filter) {
            ActivityDateFilter.Today -> today to today
            ActivityDateFilter.Week -> today.minusDays(6) to today
            ActivityDateFilter.Month -> today.withDayOfMonth(1) to today
            ActivityDateFilter.Custom -> {
                val start = startDate ?: _uiState.value.activityStartDate
                val end = endDate ?: _uiState.value.activityEndDate
                if (start <= end) start to end else end to start
            }
        }
        _uiState.update {
            it.copy(
                activityDateFilter = filter,
                activityStartDate = range.first,
                activityEndDate = range.second,
                activityTransactionPage = 1
            )
        }
    }

    fun selectCurrency(currency: CurrencyOption) {
        viewModelScope.launch {
            repository.saveSettings(_uiState.value.userName, currency, _uiState.value.themeMode)
            _uiState.update { it.copy(currency = currency) }
        }
    }

    fun selectThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.saveSettings(_uiState.value.userName, _uiState.value.currency, themeMode)
            _uiState.update { it.copy(themeMode = themeMode) }
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

    fun addCategory(name: String, iconKey: String, colorHex: String = "#4F8CFF") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(
                CategoryItem(
                    id = System.currentTimeMillis(),
                    name = name.trim(),
                    iconKey = iconKey,
                    icon = MoneyIcons.resolveCategoryIcon(iconKey),
                    isDefault = false,
                    colorHex = colorHex
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
            reloadState { it.copy(showTransactionDetailSheet = false, selectedTransactionId = null) }
        }
    }

    fun requestEditTransactionCategory(transactionId: Long) {
        _uiState.update { it.copy(showTransactionDetailSheet = true, selectedTransactionId = transactionId) }
    }

    fun cancelEditTransactionCategory() {
        _uiState.update { it.copy(showTransactionDetailSheet = false, selectedTransactionId = null) }
    }

    fun editTransactionCategory(transactionId: Long, categoryId: Long) {
        viewModelScope.launch {
            val transaction = _uiState.value.transactions.firstOrNull { it.id == transactionId } ?: return@launch
            repository.updateTransaction(transaction.copy(categoryId = categoryId))
            reloadState { it.copy(showTransactionDetailSheet = false, selectedTransactionId = null) }
        }
    }

    fun updateTransactionDetails(transactionId: Long, type: TransactionType, categoryId: Long) {
        viewModelScope.launch {
            val transaction = _uiState.value.transactions.firstOrNull { it.id == transactionId } ?: return@launch
            repository.updateTransaction(transaction.copy(type = type, categoryId = categoryId))
            reloadState { it.copy(showTransactionDetailSheet = false, selectedTransactionId = null) }
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

    fun updateCategoryColor(categoryId: Long, colorHex: String) {
        viewModelScope.launch {
            val category = _uiState.value.categories.firstOrNull { it.id == categoryId } ?: return@launch
            repository.addCategory(category.copy(colorHex = colorHex))
            reloadState()
        }
    }

    fun createCategoryForTransaction(transactionId: Long, name: String, iconKey: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val transaction = _uiState.value.transactions.firstOrNull { it.id == transactionId } ?: return@launch
            val category = CategoryItem(
                id = System.currentTimeMillis(),
                name = name.trim(),
                iconKey = iconKey,
                icon = MoneyIcons.resolveCategoryIcon(iconKey),
                isDefault = false,
                colorHex = colorHex
            )
            repository.addCategory(category)
            repository.updateTransaction(transaction.copy(categoryId = category.id))
            reloadState { it.copy(showTransactionDetailSheet = false, selectedTransactionId = null) }
        }
    }

    fun scanTodayMessages() {
        scanMessages(MessageScanRange.Today)
    }

    fun scanCurrentActivityPeriod() {
        val state = _uiState.value
        scanMessages(MessageScanRange.Custom, state.activityStartDate, state.activityEndDate)
    }

    fun populateLastThreeMonths() {
        val today = LocalDate.now()
        scanMessages(MessageScanRange.Custom, today.minusMonths(3), today)
    }

    fun scanMessages(range: MessageScanRange, startDate: LocalDate? = null, endDate: LocalDate? = null) {
        if (_uiState.value.isScanningMessages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isScanningMessages = true, scanStatusMessage = "Scanning messages...") }
            val hasPermission = hasSmsPermission()
            val parsedMessages = if (hasPermission) {
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

            if (!hasPermission) {
                reloadState {
                    it.copy(
                        isScanningMessages = false,
                        scanStatusMessage = "SMS permission is needed before scanning can populate transactions."
                    )
                }
                return@launch
            }

            val existingRawMessages = (
                _uiState.value.transactions.mapNotNull { it.rawMessage } +
                    _uiState.value.detectedDrafts.map { it.rawMessage }
                ).toMutableSet()
            var importedCount = 0
            parsedMessages
                .filterNot { it.rawMessage in existingRawMessages }
                .forEach {
                    existingRawMessages.add(it.rawMessage)
                    val categoryId = _uiState.value.categories.firstOrNull { category ->
                        category.name == "Uncategorized"
                    }?.id ?: 0L
                    repository.addTransaction(
                        LedgerTransaction(
                            id = 0,
                            name = it.counterparty,
                            amount = it.amount,
                            type = it.type,
                            categoryId = categoryId,
                            accountId = _uiState.value.accounts.firstOrNull()?.id,
                            timestampMillis = it.transactionTimestampMillis,
                            isAutoDetected = true,
                            rawMessage = it.rawMessage,
                        )
                    )
                    importedCount += 1
                }

            val status = when {
                parsedMessages.isEmpty() -> "No transaction messages found for this period."
                importedCount == 0 -> "No new transactions found. Existing messages were already imported."
                importedCount == 1 -> "Imported 1 transaction."
                else -> "Imported $importedCount transactions."
            }
            val newestMonth = parsedMessages.maxByOrNull { it.transactionTimestampMillis }
                ?.let {
                    YearMonth.from(
                        Instant.ofEpochMilli(it.transactionTimestampMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    )
                }
            reloadState {
                it.copy(
                    isScanningMessages = false,
                    scanStatusMessage = status,
                    selectedMonth = newestMonth ?: it.selectedMonth
                )
            }
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
                    timestampMillis = draft.transactionTimestampMillis,
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
        val persisted = repository.loadState(current)
        val loaded = persisted.copy(
            isAppInitializing = false,
            selectedTab = current.selectedTab,
            selectedMonth = current.selectedMonth,
            dashboardDraftPage = current.dashboardCurrentDraftPage.coerceIn(1, persisted.dashboardDraftPageCount),
            dashboardTransactionPage = current.dashboardCurrentPage.coerceIn(1, persisted.dashboardTransactionPageCount),
            activityTransactionPage = current.activityTransactionPage,
            showTransactionSheet = current.showTransactionSheet,
            showBudgetSheet = current.showBudgetSheet,
            showCategorySheet = current.showCategorySheet,
            showEditCategorySheet = current.showEditCategorySheet,
            editingTransactionId = current.editingTransactionId,
            showTransactionDetailSheet = current.showTransactionDetailSheet,
            selectedTransactionId = current.selectedTransactionId,
            activityDateFilter = current.activityDateFilter,
            activityStartDate = current.activityStartDate,
            activityEndDate = current.activityEndDate,
            isScanningMessages = current.isScanningMessages,
            scanStatusMessage = current.scanStatusMessage,
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
