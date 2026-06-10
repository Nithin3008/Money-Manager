package com.moneymanager.app.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.data.FinanceDatabase
import com.moneymanager.app.data.FinanceRepository
import com.moneymanager.app.data.LiteRtLmTransactionInterpreter
import com.moneymanager.app.data.LocalLlmCategoryOption
import com.moneymanager.app.data.OfflineLlmModelConfig
import com.moneymanager.app.data.OfflineLlmModelManager
import com.moneymanager.app.data.SmsBankKeys
import com.moneymanager.app.data.SmsScanProgress
import com.moneymanager.app.data.SmsTransactionNormalizer
import com.moneymanager.app.data.TodaySmsScanner
import com.moneymanager.app.data.TransactionMessageParser
import com.moneymanager.app.model.AccountType
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
import com.moneymanager.app.model.RegistrationAccountInput
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

class MoneyViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val OFFLINE_LLM_RUNTIME_ENABLED = true
        const val PAIRED_TRANSFER_SMS_DELIMITER = "\n--- paired transfer sms ---\n"
    }

    private val repository = FinanceRepository(FinanceDatabase.get(application).dao())
    private val offlineLlmModelManager = OfflineLlmModelManager(application)
    private var offlineLlmInterpreter: LiteRtLmTransactionInterpreter? = null

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState

    init {
        reload()
    }

    override fun onCleared() {
        closeOfflineLlmInterpreter()
        super.onCleared()
    }

    fun completeRegistration(
        name: String,
        accounts: List<RegistrationAccountInput>,
        defaultAccountIndex: Int,
        offlineLlmParsingOptIn: Boolean
    ) {
        viewModelScope.launch {
            val accountIdsWithIndexAndType = accounts
                .mapIndexedNotNull { index, account ->
                    if (account.name.isBlank() || account.balance < 0.0) return@mapIndexedNotNull null
                    val id = repository.addAccount(
                        name = account.name.trim(),
                        balance = account.balance,
                        accountType = account.type
                    )
                    Triple(index, id, account.type)
                }
            val bankAccountIdsWithIndex = accountIdsWithIndexAndType
                .filter { it.third == AccountType.Bank }
                .map { it.first to it.second }
            val defaultId = bankAccountIdsWithIndex.firstOrNull { it.first == defaultAccountIndex }?.second
                ?: bankAccountIdsWithIndex.firstOrNull()?.second
            repository.persistUserSettings(
                _uiState.value.copy(
                    userName = name.trim(),
                    bankSmsSetupCompleted = true,
                    offlineLlmParsingEnabled = offlineLlmParsingOptIn && OFFLINE_LLM_RUNTIME_ENABLED,
                    offlineLlmModelDownloaded = false,
                    offlineLlmStatusMessage = if (offlineLlmParsingOptIn) {
                        if (OFFLINE_LLM_RUNTIME_ENABLED) {
                            "AI assist is enabled. Import or download the offline model to use it during manual scans."
                        } else {
                            "Offline LLM runtime is disabled in this build. Rule parsing stays active."
                        }
                    } else {
                        _uiState.value.offlineLlmStatusMessage
                    },
                    defaultAccountId = defaultId
                )
            )
            reloadState()
            scanMessages(
                range = MessageScanRange.Custom,
                startDate = LocalDate.now().minusMonths(3),
                endDate = LocalDate.now(),
                useLocalLlm = false
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

    fun setOfflineLlmParsingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) closeOfflineLlmInterpreter()
            val modelReady = offlineLlmModelManager.isModelReady()
            val runtimeEnabled = enabled && OFFLINE_LLM_RUNTIME_ENABLED
            val next = _uiState.value.copy(
                offlineLlmParsingEnabled = runtimeEnabled,
                offlineLlmModelDownloaded = modelReady,
                offlineLlmStatusMessage = when {
                    enabled && !OFFLINE_LLM_RUNTIME_ENABLED -> "Offline LLM runtime is disabled in this build. Rule parsing stays active."
                    runtimeEnabled && modelReady -> "AI assist is ready for manual scans."
                    runtimeEnabled -> "AI assist is enabled. Import or download the offline model to use it during manual scans."
                    else -> "AI assist is off. Rule parsing stays active."
                }
            )
            repository.persistUserSettings(next)
            _uiState.value = next
        }
    }

    fun requestOfflineLlmModelDownload() {
        viewModelScope.launch {
            if (_uiState.value.isOfflineLlmModelDownloading) return@launch
            _uiState.update {
                it.copy(
                    isOfflineLlmModelDownloading = true,
                    offlineLlmStatusMessage = "Downloading ${OfflineLlmModelConfig.modelDisplayName}..."
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    offlineLlmModelManager.downloadModel()
                }
            }.onSuccess { result ->
                val next = _uiState.value.copy(
                    offlineLlmParsingEnabled = OFFLINE_LLM_RUNTIME_ENABLED,
                    offlineLlmModelDownloaded = true,
                    isOfflineLlmModelDownloading = false,
                    offlineLlmStatusMessage = if (OFFLINE_LLM_RUNTIME_ENABLED) {
                        "Offline model ready (${result.bytes / (1024 * 1024)} MB). AI assist will run only for credit cards and possible transfers."
                    } else {
                        "Offline model saved (${result.bytes / (1024 * 1024)} MB). LLM runtime is disabled in this build."
                    }
                )
                repository.persistUserSettings(next)
                _uiState.value = next
            }.onFailure { error ->
                closeOfflineLlmInterpreter()
                val next = _uiState.value.copy(
                    offlineLlmModelDownloaded = offlineLlmModelManager.isModelReady(),
                    isOfflineLlmModelDownloading = false,
                    offlineLlmStatusMessage = error.message
                        ?: "Offline model download failed. Rule parsing stays active."
                )
                repository.persistUserSettings(next)
                _uiState.value = next
            }
        }
    }

    fun importOfflineLlmModel(uri: Uri) {
        viewModelScope.launch {
            if (_uiState.value.isOfflineLlmModelDownloading) return@launch
            _uiState.update {
                it.copy(
                    isOfflineLlmModelDownloading = true,
                    offlineLlmStatusMessage = "Importing offline model..."
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    offlineLlmModelManager.importModel(uri)
                }
            }.onSuccess { result ->
                val next = _uiState.value.copy(
                    offlineLlmParsingEnabled = OFFLINE_LLM_RUNTIME_ENABLED,
                    offlineLlmModelDownloaded = true,
                    isOfflineLlmModelDownloading = false,
                    offlineLlmStatusMessage = if (OFFLINE_LLM_RUNTIME_ENABLED) {
                        "Offline model imported (${result.bytes / (1024 * 1024)} MB). AI assist will run only for credit cards and possible transfers."
                    } else {
                        "Offline model imported (${result.bytes / (1024 * 1024)} MB). LLM runtime is disabled in this build."
                    }
                )
                repository.persistUserSettings(next)
                _uiState.value = next
            }.onFailure { error ->
                closeOfflineLlmInterpreter()
                val next = _uiState.value.copy(
                    offlineLlmModelDownloaded = offlineLlmModelManager.isModelReady(),
                    isOfflineLlmModelDownloading = false,
                    offlineLlmStatusMessage = error.message
                        ?: "Offline model import failed. Rule parsing stays active."
                )
                repository.persistUserSettings(next)
                _uiState.value = next
            }
        }
    }

    fun deleteOfflineLlmModel() {
        viewModelScope.launch {
            closeOfflineLlmInterpreter()
            withContext(Dispatchers.IO) {
                offlineLlmModelManager.deleteModel()
            }
            val next = _uiState.value.copy(
                offlineLlmParsingEnabled = false,
                offlineLlmModelDownloaded = false,
                offlineLlmStatusMessage = "Offline LLM model removed. Rule parsing stays active."
            )
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
            val validId = accountId?.takeIf { id ->
                _uiState.value.accounts.any { it.id == id && it.type == AccountType.Bank }
            }
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
            val id = repository.addAccount(name.trim(), balance, accountType = AccountType.Bank)
            val hasDefault = _uiState.value.defaultAccountId != null
            if (!hasDefault) {
                val next = _uiState.value.copy(defaultAccountId = id)
                repository.persistUserSettings(next)
            }
            reloadState()
        }
    }

    fun addCreditCardAccount(name: String, outstanding: Double) {
        if (name.isBlank() || outstanding < 0.0) return
        viewModelScope.launch {
            repository.addAccount(
                name = name.trim(),
                balance = outstanding,
                accountType = AccountType.CreditCard
            )
            reloadState()
        }
    }

    fun deleteAllSavedData() {
        viewModelScope.launch {
            closeOfflineLlmInterpreter()
            withContext(Dispatchers.IO) {
                offlineLlmModelManager.deleteModel()
            }
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
                withContext(Dispatchers.IO) {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    TodaySmsScanner(getApplication()).scanRange(
                        startDate = today.minusDays(120),
                        endDate = today,
                        useLocalLlm = false
                    )
                        .map { it.bankName }
                        .distinct()
                }
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
        isAutoDetected: Boolean = false
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
                timestampMillis = System.currentTimeMillis(),
                isAutoDetected = isAutoDetected,
                rawMessage = rawMessage,
                smsBankLabel = null,
                excludeFromSummary = false,
                isCreditCardTransaction = false
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
            val transferCategoryId = state.transferCategoryId()
            val timestamp = System.currentTimeMillis()
            val label = name.trim().ifBlank { "Transfer" }
            val transfer = LedgerTransaction(
                id = 0,
                name = "$label: ${from.name} to ${to.name}",
                amount = amount,
                type = TransactionType.Transfer,
                categoryId = transferCategoryId,
                accountId = null,
                timestampMillis = timestamp,
                rawMessage = "Manual transfer from ${from.name} to ${to.name}",
                excludeFromSummary = true,
                isCreditCardTransaction = false,
                fromAccountId = from.id,
                toAccountId = to.id
            )
            repository.addTransaction(transfer)
            applyTransactionBalanceMovement(transfer)
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

    fun updateTransactionDetails(transactionId: Long, type: TransactionType, categoryId: Long, description: String?) {
        viewModelScope.launch {
            val transaction = _uiState.value.transactions.firstOrNull { it.id == transactionId } ?: return@launch
            val updatedTransaction = normalizeInvestmentTransaction(
                transaction.copy(
                    type = type,
                    categoryId = categoryId,
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
                    state.bankAccounts.firstOrNull { it.id != id }?.id
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

    fun updateCategoryColor(categoryId: Long, colorHex: String) {
        viewModelScope.launch {
            val category = _uiState.value.categories.firstOrNull { it.id == categoryId } ?: return@launch
            repository.addCategory(category.copy(colorHex = colorHex))
            reloadState()
        }
    }

    fun scanTodayMessages() {
        scanMessages(MessageScanRange.Today, useLocalLlm = false)
    }

    fun scanCurrentActivityPeriod() {
        val state = _uiState.value
        scanMessages(MessageScanRange.Custom, state.activityStartDate, state.activityEndDate, useLocalLlm = true)
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
        scanMessages(MessageScanRange.Custom, today.minusMonths(3), today, useLocalLlm = false)
    }

    fun scanMessages(
        range: MessageScanRange,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        useLocalLlm: Boolean = false
    ) {
        if (_uiState.value.isScanningMessages) return

        viewModelScope.launch {
            val scanStartState = _uiState.value
            val requestedLocalLlm = useLocalLlm && scanStartState.offlineLlmParsingEnabled
            val modelReady = offlineLlmModelManager.isModelReady()
            val canUseLocalLlm = requestedLocalLlm && OFFLINE_LLM_RUNTIME_ENABLED && modelReady
            if (!canUseLocalLlm) closeOfflineLlmInterpreter()
            _uiState.update {
                it.copy(
                    isScanningMessages = true,
                    scanStartedAtMillis = System.currentTimeMillis(),
                    scanProcessedCount = 0,
                    scanTotalCount = 0,
                    scanStatusMessage = when {
                        canUseLocalLlm -> "Scanning messages. AI assist is limited to credit cards and possible transfers."
                        requestedLocalLlm && !modelReady -> "Scanning messages with rules. Import the offline model to enable AI assist."
                        requestedLocalLlm && !OFFLINE_LLM_RUNTIME_ENABLED -> "Scanning messages with rules. AI runtime is unavailable in this build."
                        else -> "Scanning messages..."
                    }
                )
            }
            if (canUseLocalLlm) configureOfflineLlmInterpreter(_uiState.value)
            val effectiveUseLocalLlm = canUseLocalLlm && TransactionMessageParser.localLlmInterpreter != null
            val hasPermission = hasSmsPermission()
            val parsedMessages = if (hasPermission) {
                val categoryOptions = localLlmCategoryOptions()
                val progressCallback: (SmsScanProgress) -> Unit = { progress ->
                    _uiState.update {
                        it.copy(
                            scanProcessedCount = progress.processed,
                            scanTotalCount = progress.total
                        )
                    }
                }
                withContext(Dispatchers.IO) {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    when (range) {
                        MessageScanRange.Today -> TodaySmsScanner(getApplication()).scanToday(categoryOptions, effectiveUseLocalLlm, progressCallback)
                        MessageScanRange.Yesterday -> TodaySmsScanner(getApplication()).scanYesterday(categoryOptions, effectiveUseLocalLlm, progressCallback)
                        MessageScanRange.Week -> TodaySmsScanner(getApplication()).scanLast7Days(categoryOptions, effectiveUseLocalLlm, progressCallback)
                        MessageScanRange.Custom -> {
                            val today = LocalDate.now()
                            val start = startDate ?: today
                            val end = endDate?.coerceAtMost(today) ?: today
                            if (start > end) {
                                emptyList()
                            } else {
                                TodaySmsScanner(getApplication()).scanRange(start, end, categoryOptions, effectiveUseLocalLlm, progressCallback)
                            }
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
                        scanStartedAtMillis = null,
                        scanProcessedCount = 0,
                        scanTotalCount = 0,
                        scanStatusMessage = "SMS permission is needed before scanning can populate transactions."
                    )
                }
                return@launch
            }

            val normalizedExisting = buildSet {
                addAll(_uiState.value.transactions.flatMap { normalizedSmsKeys(it.rawMessage) })
                addAll(_uiState.value.detectedDrafts.flatMap { normalizedSmsKeys(it.rawMessage) })
            }.toMutableSet()
            val filteredParsed = SmsTransactionNormalizer.filterImportBatch(parsedMessages)
            val accountsSnapshot = _uiState.value.accounts.associateBy { it.id }.toMutableMap()
            val plannedImports = SmsImportPlanner.plan(filteredParsed, accountsSnapshot.values.toList())
            val uncategorizedId = _uiState.value.categories.firstOrNull { category ->
                category.name == "Uncategorized"
            }?.id ?: 0L
            val transferCategoryId = _uiState.value.transferCategoryId()
            repository.cleanupCreditCardRepaymentArtifacts()
            var importedCount = 0
            var reviewCount = 0
            var autoMappedCount = 0
            var transferCount = 0
            plannedImports.forEach { planned ->
                when (planned) {
                    is PlannedSmsImport.Transfer -> {
                        val rawKeys = planned.rawMessages.flatMap { normalizedSmsKeys(it) }
                        if (rawKeys.isEmpty() || rawKeys.any { it in normalizedExisting }) return@forEach
                        normalizedExisting.addAll(rawKeys)
                        val transfer = detectedTransferTransaction(planned, transferCategoryId, accountsSnapshot)
                        repository.addTransaction(transfer)
                        applyTransactionBalanceMovement(transfer, accountsSnapshot)
                        importedCount += 1
                        transferCount += 1
                        autoMappedCount += 2
                        return@forEach
                    }
                    is PlannedSmsImport.TransferReview -> {
                        val rawKeys = planned.rawMessages.flatMap { normalizedSmsKeys(it) }
                        if (rawKeys.isEmpty() || rawKeys.any { it in normalizedExisting }) return@forEach
                        normalizedExisting.addAll(rawKeys)
                        repository.saveDraft(detectedTransferDraft(planned, transferCategoryId))
                        reviewCount += 1
                        autoMappedCount += listOfNotNull(planned.fromAccountId, planned.toAccountId).distinct().size
                        return@forEach
                    }
                    is PlannedSmsImport.Message -> Unit
                }

                val msg = (planned as PlannedSmsImport.Message).message
                if (msg.amount <= 0.0) return@forEach
                val rawKeys = normalizedSmsKeys(msg.rawMessage)
                if (rawKeys.isEmpty() || rawKeys.any { it in normalizedExisting }) return@forEach
                normalizedExisting.addAll(rawKeys)
                val learnedCategoryId = inferCategoryId(msg)
                val categoryId = if (msg.isCreditCardTransaction) {
                    learnedCategoryId ?: msg.suggestedCategoryId ?: uncategorizedId
                } else {
                    msg.suggestedCategoryId ?: learnedCategoryId ?: uncategorizedId
                }
                val availableAccounts = accountsSnapshot.values.toList()
                val accountCandidates = if (msg.isCreditCardTransaction) {
                    availableAccounts.filter { it.type == AccountType.CreditCard }
                } else {
                    availableAccounts.filter { it.type == AccountType.Bank }
                }
                val accountId = SmsBankKeys.resolveAccountId(msg.bankName, accountCandidates)
                    ?: if (msg.isCreditCardTransaction) null else accountCandidates.singleOrNull()?.id
                if (accountId != null) autoMappedCount += 1
                if (msg.requiresUserReview || msg.categoryRequiresUserReview) {
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
                    excludeFromSummary = msg.excludeFromSummary,
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
                plannedImports.isEmpty() -> "Found ${filteredParsed.size} transaction SMS, but no new transactions were imported."
                importedCount == 0 && reviewCount == 0 -> "Found ${plannedImports.size} transaction SMS, but no new transactions were imported."
                importedCount == 0 -> "Found $reviewCount transaction SMS that need your review."
                reviewCount > 0 && transferCount > 0 -> "Imported $importedCount transactions, including $transferCount transfers, and sent $reviewCount for review. Auto-mapped $autoMappedCount account links."
                reviewCount > 0 -> "Imported $importedCount transactions and sent $reviewCount for review. Auto-mapped $autoMappedCount account links."
                transferCount > 0 -> "Imported $importedCount transactions, including $transferCount transfers. Auto-mapped $autoMappedCount account links."
                importedCount == 1 -> "Imported 1 transaction. Auto-mapped $autoMappedCount account links."
                else -> "Imported $importedCount transactions. Auto-mapped $autoMappedCount account links."
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
                    scanStartedAtMillis = null,
                    scanProcessedCount = 0,
                    scanTotalCount = 0,
                    scanStatusMessage = status,
                    selectedMonth = newestMonth ?: it.selectedMonth
                )
            }
        }
    }

    fun acceptDetectedTransaction(
        draftId: Long,
        categoryId: Long,
        type: TransactionType,
        fromAccountId: Long? = null,
        toAccountId: Long? = null
    ) {
        viewModelScope.launch {
            val draft = _uiState.value.detectedDrafts.firstOrNull { it.id == draftId } ?: return@launch
            if (draft.amount <= 0.0) {
                repository.deleteDraft(draftId)
                reloadState()
                return@launch
            }
            if (type == TransactionType.Transfer) {
                val resolvedFrom = fromAccountId ?: draft.fromAccountId
                val resolvedTo = toAccountId ?: draft.toAccountId
                if (resolvedFrom == null || resolvedTo == null || resolvedFrom == resolvedTo) return@launch
                val accountsById = _uiState.value.accounts.associateBy { it.id }.toMutableMap()
                val from = accountsById[resolvedFrom] ?: return@launch
                val to = accountsById[resolvedTo] ?: return@launch
                val transfer = LedgerTransaction(
                    id = 0,
                    name = "${draft.name}: ${from.name} to ${to.name}",
                    amount = draft.amount,
                    type = TransactionType.Transfer,
                    categoryId = _uiState.value.transferCategoryId(),
                    accountId = null,
                    timestampMillis = draft.transactionTimestampMillis,
                    isAutoDetected = true,
                    rawMessage = draft.rawMessage,
                    smsBankLabel = draft.bankName,
                    excludeFromSummary = true,
                    isCreditCardTransaction = false,
                    fromAccountId = from.id,
                    toAccountId = to.id
                )
                repository.addTransaction(transfer)
                applyTransactionBalanceMovement(transfer, accountsById)
                repository.deleteDraft(draftId)
                reloadState()
                return@launch
            }
            val accountsSnapshot = _uiState.value.accounts
            val isCreditCardTransaction = SmsTransactionNormalizer.isCreditCardSpend(draft.rawMessage)
            val isCreditCardBillPayment = SmsTransactionNormalizer.isCreditCardBillPaymentDebit(draft.rawMessage)
            val accountCandidates = if (isCreditCardTransaction) {
                accountsSnapshot.filter { it.type == AccountType.CreditCard }
            } else {
                accountsSnapshot.filter { it.type == AccountType.Bank }
            }
            val draftTransaction = LedgerTransaction(
                id = 0,
                name = draft.counterparty,
                amount = draft.amount,
                type = type,
                categoryId = categoryId,
                accountId = SmsBankKeys.resolveAccountId(draft.bankName, accountCandidates)
                    ?: if (isCreditCardTransaction) null else accountCandidates.singleOrNull()?.id,
                timestampMillis = draft.transactionTimestampMillis,
                isAutoDetected = true,
                rawMessage = draft.rawMessage,
                smsBankLabel = draft.bankName,
                excludeFromSummary = isCreditCardBillPayment,
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
        balanceMovementsForTransaction(oldTransaction, accountsById).forEach { (accountId, delta) ->
            updateAccountBalance(accountId, -delta, accountsById)
        }
        balanceMovementsForTransaction(newTransaction, accountsById).forEach { (accountId, delta) ->
            updateAccountBalance(accountId, delta, accountsById)
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
        balanceMovementsForTransaction(transaction, accountsById).forEach { (accountId, movement) ->
            updateAccountBalance(accountId, if (reverse) -movement else movement, accountsById)
        }
    }

    private fun balanceMovementsForTransaction(
        transaction: LedgerTransaction,
        accountsById: MutableMap<Long, BankAccount>? = null
    ): List<Pair<Long, Double>> {
        if (transaction.amount <= 0.0) return emptyList()
        if (transaction.type == TransactionType.Transfer) {
            return listOfNotNull(
                transaction.fromAccountId?.let { id ->
                    accountForBalanceMovement(id, accountsById)?.let { account ->
                        id to outgoingBalanceMovement(account, transaction.amount)
                    }
                },
                transaction.toAccountId?.let { id ->
                    accountForBalanceMovement(id, accountsById)?.let { account ->
                        id to incomingBalanceMovement(account, transaction.amount)
                    }
                }
            )
        }

        val accountId = transaction.accountId ?: return emptyList()
        val account = accountForBalanceMovement(accountId, accountsById) ?: return emptyList()
        return listOf(accountId to singleAccountBalanceMovement(transaction, account))
            .filter { it.second != 0.0 }
    }

    private fun accountForBalanceMovement(
        accountId: Long,
        accountsById: MutableMap<Long, BankAccount>?
    ): BankAccount? {
        return accountsById?.get(accountId)
            ?: _uiState.value.accounts.firstOrNull { it.id == accountId }
    }

    private fun singleAccountBalanceMovement(
        transaction: LedgerTransaction,
        account: BankAccount
    ): Double {
        if (transaction.isCreditCardTransaction && account.type == AccountType.Bank) return 0.0
        return when (account.type) {
            AccountType.Bank -> if (transaction.type == TransactionType.Income) {
                transaction.amount
            } else {
                -transaction.amount
            }
            AccountType.CreditCard -> if (transaction.type == TransactionType.Income) {
                -transaction.amount
            } else {
                transaction.amount
            }
        }
    }

    private fun outgoingBalanceMovement(account: BankAccount, amount: Double): Double {
        return when (account.type) {
            AccountType.Bank -> -amount
            AccountType.CreditCard -> amount
        }
    }

    private fun incomingBalanceMovement(account: BankAccount, amount: Double): Double {
        return when (account.type) {
            AccountType.Bank -> amount
            AccountType.CreditCard -> -amount
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
            scanStartedAtMillis = current.scanStartedAtMillis,
            scanProcessedCount = current.scanProcessedCount,
            scanTotalCount = current.scanTotalCount,
            isOfflineLlmModelDownloading = current.isOfflineLlmModelDownloading,
            scanStatusMessage = current.scanStatusMessage,
            offlineLlmStatusMessage = current.offlineLlmStatusMessage,
            budgetWarning = current.budgetWarning,
            discoveredSmsBanks = current.discoveredSmsBanks,
            offlineLlmModelDownloaded = offlineLlmModelManager.isModelReady()
        )
        _uiState.value = transform(loaded)
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun configureOfflineLlmInterpreter(state: FinanceUiState) {
        if (!OFFLINE_LLM_RUNTIME_ENABLED) {
            closeOfflineLlmInterpreter()
            _uiState.update {
                it.copy(
                    offlineLlmParsingEnabled = false,
                    offlineLlmStatusMessage = "Offline LLM runtime is disabled in this build. Rule parsing stays active."
                )
            }
            return
        }
        if (!state.offlineLlmParsingEnabled || !offlineLlmModelManager.isModelReady()) {
            closeOfflineLlmInterpreter()
            return
        }
        if (offlineLlmInterpreter != null) return

        runCatching {
            withContext(Dispatchers.Default) {
                LiteRtLmTransactionInterpreter(
                    context = getApplication(),
                    modelPath = offlineLlmModelManager.modelFile.absolutePath
                ).also { it.initialize() }
            }
        }.onSuccess { interpreter ->
            offlineLlmInterpreter = interpreter
            TransactionMessageParser.localLlmInterpreter = interpreter
            _uiState.update {
                it.copy(
                    offlineLlmModelDownloaded = true,
                    offlineLlmStatusMessage = "AI assist is ready. It will only run for credit cards and possible transfers."
                )
            }
        }.onFailure { error ->
            closeOfflineLlmInterpreter()
            _uiState.update {
                it.copy(
                    offlineLlmStatusMessage = "Offline LLM could not start: ${error.message ?: "unknown error"}"
                )
            }
        }
    }

    private fun closeOfflineLlmInterpreter() {
        TransactionMessageParser.localLlmInterpreter = null
        offlineLlmInterpreter?.close()
        offlineLlmInterpreter = null
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

    private fun detectedTransferTransaction(
        plan: PlannedSmsImport.Transfer,
        categoryId: Long,
        accountsById: Map<Long, BankAccount>
    ): LedgerTransaction {
        val fromName = accountsById[plan.fromAccountId]?.name ?: "Account"
        val toName = accountsById[plan.toAccountId]?.name ?: "Account"
        val name = when (plan.source) {
            SmsTransferSource.BankTransfer -> "Transfer: $fromName to $toName"
            SmsTransferSource.CreditCardPayment -> "Credit card payment: $fromName to $toName"
        }
        val smsLabel = listOfNotNull(plan.debit.bankName, plan.credit?.bankName)
            .distinct()
            .joinToString(" -> ")
            .ifBlank { null }
        return LedgerTransaction(
            id = 0,
            name = name,
            amount = plan.debit.amount,
            type = TransactionType.Transfer,
            categoryId = categoryId,
            accountId = null,
            timestampMillis = plan.timestampMillis,
            isAutoDetected = true,
            rawMessage = pairedTransferRawMessage(plan.rawMessages),
            smsBankLabel = smsLabel,
            excludeFromSummary = true,
            isCreditCardTransaction = false,
            fromAccountId = plan.fromAccountId,
            toAccountId = plan.toAccountId
        )
    }

    private fun detectedTransferDraft(
        plan: PlannedSmsImport.TransferReview,
        categoryId: Long
    ): DetectedTransactionDraft {
        val title = when (plan.source) {
            SmsTransferSource.BankTransfer -> "Review bank transfer"
            SmsTransferSource.CreditCardPayment -> "Review credit card payment"
        }
        val smsLabel = listOfNotNull(plan.debit.bankName, plan.credit?.bankName)
            .distinct()
            .joinToString(" -> ")
            .ifBlank { "Transfer" }
        return DetectedTransactionDraft(
            id = 0,
            bankName = smsLabel,
            name = title,
            amount = plan.debit.amount,
            type = TransactionType.Transfer,
            counterparty = title,
            rawMessage = pairedTransferRawMessage(plan.rawMessages),
            suggestedCategoryId = categoryId,
            detectedAtMillis = System.currentTimeMillis(),
            transactionTimestampMillis = plan.timestampMillis,
            fromAccountId = plan.fromAccountId,
            toAccountId = plan.toAccountId
        )
    }

    private fun pairedTransferRawMessage(rawMessages: List<String>): String {
        return rawMessages
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(PAIRED_TRANSFER_SMS_DELIMITER)
    }

    private fun normalizedSmsKeys(rawMessage: String?): List<String> {
        val raw = rawMessage.orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(PAIRED_TRANSFER_SMS_DELIMITER)
            .map(::normalizedSmsRaw)
            .filter { it.isNotBlank() }
    }

    private fun FinanceUiState.transferCategoryId(): Long {
        return categories.firstOrNull { category ->
            category.name.equals("Transfer", ignoreCase = true) ||
                category.iconKey.equals("transfer", ignoreCase = true)
        }?.id ?: categories.firstOrNull { it.name == "Uncategorized" }?.id ?: categories.first().id
    }

    private fun localLlmCategoryOptions(): List<LocalLlmCategoryOption> {
        return _uiState.value.categories.map { category ->
            LocalLlmCategoryOption(
                id = category.id,
                name = category.name
            )
        }
    }
}
