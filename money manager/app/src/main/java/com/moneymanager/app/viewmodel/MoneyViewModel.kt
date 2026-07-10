package com.moneymanager.app.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.data.FinanceDatabase
import com.moneymanager.app.data.FinanceRepository
import com.moneymanager.app.data.SmsBankKeys
import com.moneymanager.app.data.SmsTransactionNormalizer
import com.moneymanager.app.data.TodaySmsScanner
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.BudgetPlan
import com.moneymanager.app.model.MessageScanRange
import java.time.LocalDate
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
import com.moneymanager.app.model.UiAccent
import com.moneymanager.app.model.UiSurface
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

    fun completeRegistration(name: String, accounts: List<Pair<String, Double>>, defaultAccountIndex: Int) {
        viewModelScope.launch {
            val accountIds = accounts
                .filter { it.first.isNotBlank() && it.second >= 0.0 }
                .map { repository.addAccount(it.first.trim(), it.second) }
            val defaultId = accountIds.getOrNull(defaultAccountIndex.coerceIn(0, (accountIds.size - 1).coerceAtLeast(0)))
                ?: accountIds.firstOrNull()
            repository.persistUserSettings(
                _uiState.value.copy(
                    userName = name.trim(),
                    bankSmsSetupCompleted = true,
                    onboardedAtMillis = System.currentTimeMillis(),
                    defaultAccountId = defaultId
                )
            )
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
            val next = _uiState.value.copy(currency = currency)
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun selectThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            val next = _uiState.value.copy(themeMode = themeMode)
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun selectUiAccent(uiAccent: UiAccent) {
        viewModelScope.launch {
            val next = _uiState.value.copy(uiAccent = uiAccent)
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun selectUiSurface(uiSurface: UiSurface) {
        viewModelScope.launch {
            val next = _uiState.value.copy(uiSurface = uiSurface)
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun setSalaryShiftIncomeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val next = _uiState.value.copy(salaryShiftIncomeEnabled = enabled)
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun setSalaryShiftWindowDays(days: Int) {
        viewModelScope.launch {
            val coerced = days.coerceIn(3, 14)
            val next = _uiState.value.copy(salaryShiftWindowDays = coerced)
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun setSalaryCategoryId(categoryId: Long?) {
        viewModelScope.launch {
            val next = _uiState.value.copy(salaryCategoryId = categoryId)
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun setSalaryKeywordsForUncategorized(enabled: Boolean) {
        viewModelScope.launch {
            val next = _uiState.value.copy(salaryKeywordsForUncategorized = enabled)
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun setSummaryAccountFilter(accountIds: Set<Long>) {
        viewModelScope.launch {
            val next = _uiState.value.copy(summarySelectedAccountIds = accountIds)
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun toggleSummaryAccountInFilter(accountId: Long) {
        setSummaryAccountFilter(setOf(accountId))
    }

    fun clearSummaryAccountFilter() {
        setSummaryAccountFilter(emptySet())
    }

    fun setDefaultAccount(accountId: Long?) {
        viewModelScope.launch {
            val validId = accountId?.takeIf { id -> _uiState.value.accounts.any { it.id == id } }
            val next = _uiState.value.copy(
                defaultAccountId = validId,
                summarySelectedAccountIds = emptySet()
            )
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun completeBankSmsOnboarding() {
        viewModelScope.launch {
            val next = _uiState.value.copy(bankSmsSetupCompleted = true)
            repository.persistUserSettings(next)
            repository.remapTransactionAccountsFromSmsLabels()
            reloadState()
        }
    }

    fun mapSmsBankToAccount(smsBankLabel: String, accountId: Long) {
        viewModelScope.launch {
            val account = _uiState.value.accounts.firstOrNull { it.id == accountId } ?: return@launch
            val key = SmsBankKeys.normalize(smsBankLabel)
            repository.updateAccount(account.copy(smsMatchKey = key))
            repository.remapTransactionAccountsFromSmsLabels()
            reloadState()
        }
    }

    fun createAccountForSmsLabel(smsBankLabel: String, accountName: String) {
        if (accountName.isBlank()) return
        viewModelScope.launch {
            repository.addAccount(
                name = accountName.trim(),
                balance = 0.0,
                smsMatchKey = SmsBankKeys.normalize(smsBankLabel)
            )
            repository.remapTransactionAccountsFromSmsLabels()
            reloadState()
        }
    }

    fun updateAccountBalance(accountId: Long, balance: Double) {
        if (balance < 0.0) return
        viewModelScope.launch {
            val account = _uiState.value.accounts.firstOrNull { it.id == accountId } ?: return@launch
            repository.updateAccount(account.copy(balance = balance))
            reloadState()
        }
    }

    fun addBankAccount(name: String, balance: Double) {
        if (name.isBlank() || balance < 0.0) return
        viewModelScope.launch {
            val id = repository.addAccount(name.trim(), balance)
            val hasDefault = _uiState.value.defaultAccountId != null
            if (!hasDefault) {
                val next = _uiState.value.copy(defaultAccountId = id)
                repository.persistUserSettings(next)
            }
            reloadState()
        }
    }

    fun deleteAllSavedData() {
        viewModelScope.launch {
            repository.clearAllSavedData()
            _uiState.value = FinanceUiState(isAppInitializing = false)
            reloadState()
        }
    }

    fun refreshDiscoveredSmsBanks() {
        viewModelScope.launch {
            val state = _uiState.value
            val fromDb = state.transactions.mapNotNull { it.smsBankLabel }.distinct()
            val scanned = if (hasSmsPermission()) {
                val today = LocalDate.now()
                TodaySmsScanner(getApplication()).scanRange(today.minusDays(120), today)
                    .map { it.bankName }
                    .distinct()
            } else {
                emptyList()
            }
            val merged = (fromDb + scanned)
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals("Bank", ignoreCase = true) }
                .distinct()
                .sorted()
            _uiState.update { it.copy(discoveredSmsBanks = merged) }
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
        isAutoDetected: Boolean = false,
        description: String? = null,
        timestampMillis: Long? = null
    ) {
        if (name.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            val draftTransaction = LedgerTransaction(
                id = 0,
                name = name.trim(),
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                timestampMillis = timestampMillis ?: System.currentTimeMillis(),
                isAutoDetected = isAutoDetected,
                rawMessage = rawMessage,
                smsBankLabel = null,
                excludeFromSummary = false,
                isCreditCardTransaction = false,
                description = description?.trim()?.takeIf { it.isNotBlank() }
            )
            val transaction = normalizeInvestmentTransaction(draftTransaction)
            repository.addTransaction(transaction)
            applyTransactionBalanceMovement(transaction)
            val warning = BudgetWarningCalculator.findBudgetWarning(_uiState.value, transaction)
            reloadState { it.copy(showTransactionSheet = false, budgetWarning = warning) }
        }
    }

    fun addTransfer(name: String, amount: Double, fromAccountId: Long?, toAccountId: Long?) {
        if (amount <= 0.0 || fromAccountId == null || toAccountId == null || fromAccountId == toAccountId) return
        viewModelScope.launch {
            val state = _uiState.value
            val from = state.accounts.firstOrNull { it.id == fromAccountId } ?: return@launch
            val to = state.accounts.firstOrNull { it.id == toAccountId } ?: return@launch
            val uncategorizedId = state.categories.firstOrNull { it.name == "Uncategorized" }?.id
                ?: state.categories.first().id
            val timestamp = System.currentTimeMillis()
            val label = name.trim().ifBlank { "Transfer" }
            val expenseTransfer = LedgerTransaction(
                    id = 0,
                    name = "$label to ${to.name}",
                    amount = amount,
                    type = TransactionType.Expense,
                    categoryId = uncategorizedId,
                    accountId = from.id,
                    timestampMillis = timestamp,
                    rawMessage = "Manual transfer from ${from.name} to ${to.name}",
                    excludeFromSummary = true,
                    isCreditCardTransaction = false
            )
            val incomeTransfer = LedgerTransaction(
                id = 0,
                name = "$label from ${from.name}",
                amount = amount,
                type = TransactionType.Income,
                categoryId = uncategorizedId,
                accountId = to.id,
                timestampMillis = timestamp + 1,
                rawMessage = "Manual transfer from ${from.name} to ${to.name}",
                excludeFromSummary = true,
                isCreditCardTransaction = false
            )
            repository.addTransaction(expenseTransfer)
            repository.addTransaction(incomeTransfer)
            applyTransactionBalanceMovement(expenseTransfer)
            applyTransactionBalanceMovement(incomeTransfer)
            reloadState { it.copy(showTransactionSheet = false) }
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
            val state = _uiState.value
            val transaction = state.transactions.firstOrNull { it.id == id }
            val transferRows = TransferMatching.manualTransferRowsFor(transaction, state.transactions)
            if (transferRows.isNotEmpty()) {
                transferRows.forEach { tx ->
                    reverseTransactionBalanceMovement(tx)
                    repository.deleteTransaction(tx.id)
                }
            } else {
                transaction?.let { reverseTransactionBalanceMovement(it) }
                repository.deleteTransaction(id)
            }
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
            val updatedTransaction = normalizeInvestmentTransaction(
                transaction.copy(categoryId = categoryId),
                originalTransaction = transaction
            )
            reconcileBalanceForTransactionUpdate(transaction, updatedTransaction)
            repository.updateTransaction(updatedTransaction)
            reloadState { it.copy(showTransactionDetailSheet = false, selectedTransactionId = null) }
        }
    }

    fun updateTransactionDetails(
        transactionId: Long,
        type: TransactionType,
        categoryId: Long,
        description: String?,
        name: String? = null,
        amount: Double? = null,
        accountId: Long? = null,
        timestampMillis: Long? = null
    ) {
        viewModelScope.launch {
            val transaction = _uiState.value.transactions.firstOrNull { it.id == transactionId } ?: return@launch
            val updatedTransaction = normalizeInvestmentTransaction(
                transaction.copy(
                    name = name?.trim()?.takeIf { it.isNotBlank() } ?: transaction.name,
                    amount = amount?.takeIf { it > 0.0 } ?: transaction.amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    timestampMillis = timestampMillis ?: transaction.timestampMillis,
                    description = description?.trim()?.takeIf { it.isNotBlank() }
                ),
                originalTransaction = transaction
            )
            reconcileBalanceForTransactionUpdate(transaction, updatedTransaction)
            repository.updateTransaction(updatedTransaction)
            applyCategoryToSimilarUncategorized(transaction, categoryId, type)
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
            val state = _uiState.value
            if (state.defaultAccountId == id || id in state.summarySelectedAccountIds) {
                val nextDefault = if (state.defaultAccountId == id) {
                    state.accounts.firstOrNull { it.id != id }?.id
                } else {
                    state.defaultAccountId
                }
                repository.persistUserSettings(
                    state.copy(
                        defaultAccountId = nextDefault,
                        summarySelectedAccountIds = state.summarySelectedAccountIds - id
                    )
                )
            }
            reloadState()
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomCategory(id)
            reloadState()
        }
    }

    fun updateCategory(categoryId: Long, name: String, iconKey: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val category = _uiState.value.categories.firstOrNull { it.id == categoryId } ?: return@launch
            repository.addCategory(
                category.copy(
                    name = name.trim(),
                    iconKey = iconKey,
                    icon = MoneyIcons.resolveCategoryIcon(iconKey),
                    colorHex = colorHex
                )
            )
            reloadState()
        }
    }

    fun scanTodayMessages() {
        scanMessages(MessageScanRange.Today)
    }

    fun scanCurrentActivityPeriod() {
        val state = _uiState.value
        scanMessages(MessageScanRange.Custom, state.activityStartDate, state.activityEndDate)
    }

    fun exportPreviousMonthSmsDebug() {
        viewModelScope.launch {
            if (!hasSmsPermission()) {
                _uiState.update {
                    it.copy(scanStatusMessage = "SMS permission is needed before exporting parser debug data.")
                }
                return@launch
            }
            val month = YearMonth.now().minusMonths(1)
            runCatching {
                TodaySmsScanner(getApplication()).exportDebugMonth(month)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        scanStatusMessage = "Exported ${result.candidateCount} parser SMS from $month to ${result.filePath}"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(scanStatusMessage = "SMS debug export failed: ${error.message ?: "unknown error"}")
                }
            }
        }
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

            val normalizedExisting = buildSet {
                addAll(_uiState.value.transactions.mapNotNull { normalizedSmsRaw(it.rawMessage) })
                addAll(_uiState.value.detectedDrafts.map { normalizedSmsRaw(it.rawMessage) })
            }.toMutableSet()
            val filteredParsed = SmsTransactionNormalizer.filterImportBatch(parsedMessages)
            val accountsSnapshot = _uiState.value.accounts.associateBy { it.id }.toMutableMap()
            repository.cleanupCreditCardRepaymentArtifacts()
            var importedCount = 0
            var reviewCount = 0
            var autoMappedCount = 0
            filteredParsed.forEach { msg ->
                if (msg.amount <= 0.0) return@forEach
                val normRaw = normalizedSmsRaw(msg.rawMessage)
                if (normRaw.isBlank() || normRaw in normalizedExisting) return@forEach
                normalizedExisting.add(normRaw)
                val categoryId = inferCategoryId(msg) ?: _uiState.value.categories.firstOrNull { category ->
                    category.name == "Uncategorized"
                }?.id ?: 0L
                val availableAccounts = accountsSnapshot.values.toList()
                val accountId = SmsBankKeys.resolveAccountId(msg.bankName, availableAccounts)
                    ?: availableAccounts.singleOrNull()?.id
                if (accountId != null) autoMappedCount += 1
                if (msg.requiresUserReview) {
                    repository.saveDraft(
                        DetectedTransactionDraft(
                            id = 0,
                            bankName = msg.bankName,
                            name = msg.counterparty,
                            amount = msg.amount,
                            type = msg.type,
                            counterparty = msg.counterparty,
                            rawMessage = msg.rawMessage,
                            suggestedCategoryId = categoryId,
                            detectedAtMillis = System.currentTimeMillis(),
                            transactionTimestampMillis = msg.transactionTimestampMillis
                        )
                    )
                    reviewCount += 1
                    return@forEach
                }
                val draftTransaction = LedgerTransaction(
                    id = 0,
                    name = msg.counterparty,
                    amount = msg.amount,
                    type = msg.type,
                    categoryId = categoryId,
                    accountId = accountId,
                    timestampMillis = msg.transactionTimestampMillis,
                    isAutoDetected = true,
                    rawMessage = msg.rawMessage,
                    smsBankLabel = msg.bankName,
                    excludeFromSummary = msg.isCreditCardTransaction,
                    isCreditCardTransaction = msg.isCreditCardTransaction
                )
                val transaction = normalizeInvestmentTransaction(draftTransaction)
                repository.addTransaction(transaction)
                applyTransactionBalanceMovement(transaction, accountsSnapshot)
                importedCount += 1
            }

            val status = when {
                parsedMessages.isEmpty() -> "No transaction messages found for this period."
                filteredParsed.isEmpty() -> "Found ${parsedMessages.size} transaction-like SMS, but all were filtered as duplicates, reminders, or internal transfers."
                importedCount == 0 && reviewCount == 0 -> "Found ${filteredParsed.size} transaction SMS, but no new transactions were imported."
                importedCount == 0 -> "Found $reviewCount transaction SMS that need your review."
                reviewCount > 0 -> "Imported $importedCount transactions and sent $reviewCount for review. Auto-mapped $autoMappedCount to bank accounts."
                importedCount == 1 -> "Imported 1 transaction. Auto-mapped $autoMappedCount to bank accounts."
                else -> "Imported $importedCount transactions. Auto-mapped $autoMappedCount to bank accounts."
            }
            val newestMonth = parsedMessages.maxByOrNull { it.transactionTimestampMillis }
                ?.let {
                    YearMonth.from(
                        Instant.ofEpochMilli(it.transactionTimestampMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    )
                }
            repository.remapTransactionAccountsFromSmsLabels()
            reloadState {
                it.copy(
                    isScanningMessages = false,
                    scanStatusMessage = status,
                    selectedMonth = newestMonth ?: it.selectedMonth
                )
            }
        }
    }

    fun acceptDetectedTransaction(draftId: Long, categoryId: Long, type: TransactionType) {
        viewModelScope.launch {
            val draft = _uiState.value.detectedDrafts.firstOrNull { it.id == draftId } ?: return@launch
            if (draft.amount <= 0.0) {
                repository.deleteDraft(draftId)
                reloadState()
                return@launch
            }
            val accountsSnapshot = _uiState.value.accounts
            val isCreditCardTransaction = SmsTransactionNormalizer.isCreditCardSpend(draft.rawMessage)
            val draftTransaction = LedgerTransaction(
                id = 0,
                name = draft.counterparty,
                amount = draft.amount,
                type = type,
                categoryId = categoryId,
                accountId = SmsBankKeys.resolveAccountId(draft.bankName, accountsSnapshot)
                    ?: accountsSnapshot.singleOrNull()?.id,
                timestampMillis = draft.transactionTimestampMillis,
                isAutoDetected = true,
                rawMessage = draft.rawMessage,
                smsBankLabel = draft.bankName,
                excludeFromSummary = isCreditCardTransaction,
                isCreditCardTransaction = isCreditCardTransaction
            )
            val transaction = normalizeInvestmentTransaction(draftTransaction)
            repository.addTransaction(transaction)
            applyTransactionBalanceMovement(transaction)
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
            runCatching {
                repository.cleanupCreditCardRepaymentArtifacts()
                reloadState()
                refreshDiscoveredSmsBanks()
            }
                .onFailure {
                    _uiState.update { state -> state.copy(isAppInitializing = false) }
                }
        }
    }

    suspend fun getExportData(): String {
        return repository.exportData()
    }

    fun importData(jsonString: String) {
        viewModelScope.launch {
            try {
                repository.importData(jsonString)
                reloadState()
            } catch (e: Exception) {
                // handle error or add to a state error message
            }
        }
    }

    private suspend fun applyTransactionBalanceMovement(
        transaction: LedgerTransaction,
        accountsById: MutableMap<Long, BankAccount>? = null
    ) {
        moveAccountBalanceForTransaction(transaction, reverse = false, accountsById = accountsById)
    }

    private suspend fun reverseTransactionBalanceMovement(transaction: LedgerTransaction) {
        moveAccountBalanceForTransaction(transaction, reverse = true)
    }

    private suspend fun reconcileBalanceForTransactionUpdate(
        oldTransaction: LedgerTransaction,
        newTransaction: LedgerTransaction,
        accountsById: MutableMap<Long, BankAccount>? = null
    ) {
        val oldAccountId = oldTransaction.accountId
        val newAccountId = newTransaction.accountId
        if (oldAccountId == null && newAccountId == null) return

        if (oldAccountId != null && oldAccountId == newAccountId) {
            val delta = storedBalanceMovement(newTransaction) - storedBalanceMovement(oldTransaction)
            if (delta != 0.0) {
                updateAccountBalance(oldAccountId, delta, accountsById)
            }
            return
        }

        oldAccountId?.let { accountId ->
            updateAccountBalance(accountId, -storedBalanceMovement(oldTransaction), accountsById)
        }
        newAccountId?.let { accountId ->
            updateAccountBalance(accountId, storedBalanceMovement(newTransaction), accountsById)
        }
    }

    private suspend fun updateAccountBalance(
        accountId: Long,
        delta: Double,
        accountsById: MutableMap<Long, BankAccount>? = null
    ) {
        if (delta == 0.0) return
        val account = accountsById?.get(accountId)
            ?: _uiState.value.accounts.firstOrNull { it.id == accountId }
            ?: return
        val updated = account.copy(balance = account.balance + delta)
        repository.updateAccount(updated)
        accountsById?.put(accountId, updated)
    }

    private suspend fun moveAccountBalanceForTransaction(
        transaction: LedgerTransaction,
        reverse: Boolean,
        accountsById: MutableMap<Long, BankAccount>? = null
    ) {
        val accountId = transaction.accountId ?: return
        val movement = storedBalanceMovement(transaction)
        if (movement == 0.0) return
        val account = accountsById?.get(accountId)
            ?: _uiState.value.accounts.firstOrNull { it.id == accountId }
            ?: return
        val delta = if (reverse) -movement else movement
        val updated = account.copy(balance = account.balance + delta)
        repository.updateAccount(updated)
        accountsById?.put(accountId, updated)
    }

    private fun storedBalanceMovement(transaction: LedgerTransaction): Double {
        // Account balances were entered as-of onboarding, so transactions dated before that
        // moment are already reflected in them and must not move the balance again.
        if (transaction.timestampMillis < _uiState.value.onboardedAtMillis) return 0.0
        return if (transaction.type == TransactionType.Income) {
            transaction.amount
        } else {
            -transaction.amount
        }
    }

    private fun normalizeInvestmentTransaction(
        transaction: LedgerTransaction,
        originalTransaction: LedgerTransaction? = null
    ): LedgerTransaction {
        val state = _uiState.value
        if (state.isInvestmentTransaction(transaction)) {
            return transaction.copy(
                type = TransactionType.Expense,
                accountId = null,
                excludeFromSummary = true
            )
        }

        val wasInvestment = originalTransaction?.let { state.isInvestmentTransaction(it) } == true
        if (!wasInvestment) return transaction
        if (transaction.isCreditCardTransaction) return transaction.copy(excludeFromSummary = true)
        return transaction.copy(excludeFromSummary = false)
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
            budgetWarning = current.budgetWarning,
            discoveredSmsBanks = current.discoveredSmsBanks
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

    private suspend fun applyCategoryToSimilarUncategorized(
        source: LedgerTransaction,
        categoryId: Long,
        type: TransactionType
    ) {
        val uncategorizedId = _uiState.value.categories.firstOrNull { it.name == "Uncategorized" }?.id ?: 0L
        if (categoryId == uncategorizedId) return
        val sourceKey = CategoryLearning.categoryLearningKey(source.name, source.rawMessage, source.type, source.smsBankLabel)
        if (sourceKey.isBlank()) return
        val accountsSnapshot = _uiState.value.accounts.associateBy { it.id }.toMutableMap()
        _uiState.value.transactions
            .filter {
                it.id != source.id &&
                    it.categoryId == uncategorizedId &&
                    CategoryLearning.categoryLearningKey(it.name, it.rawMessage, it.type, it.smsBankLabel) == sourceKey
            }
            .forEach {
                val updated = normalizeInvestmentTransaction(
                    it.copy(type = type, categoryId = categoryId),
                    originalTransaction = it
                )
                reconcileBalanceForTransactionUpdate(it, updated, accountsSnapshot)
                repository.updateTransaction(updated)
            }
    }

    private fun inferCategoryId(msg: com.moneymanager.app.data.ParsedTransactionMessage): Long? {
        return CategoryLearning.inferCategoryId(
            msg = msg,
            categories = _uiState.value.categories,
            transactions = _uiState.value.transactions
        )
    }
}
