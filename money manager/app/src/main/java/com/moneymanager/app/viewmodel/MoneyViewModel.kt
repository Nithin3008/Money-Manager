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
import com.moneymanager.app.widget.MoneyWidgets
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
        const val PAIRED_TRANSFER_SMS_DELIMITER = "\n--- paired transfer sms ---\n"
    }

    private val repository = FinanceRepository(FinanceDatabase.get(application).dao())

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState

    init {
        reload()
    }

    fun completeRegistration(
        name: String,
        accounts: List<RegistrationAccountInput>,
        defaultAccountIndex: Int
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
                    onboardedAtMillis = System.currentTimeMillis(),
                    defaultAccountId = defaultId
                )
            )
            reloadState()
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
            repository.updateAccount(
                account.copy(
                    balance = balance,
                    // Typing the real balance re-anchors this account: everything dated before
                    // now is already reflected in it, so history can never double-count again.
                    balanceAnchorAtMillis = System.currentTimeMillis()
                )
            )
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
                        endDate = today
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

    /**
     * Deleting is irreversible — an auto-detected row's SMS is dismissed forever — so the
     * UI asks for confirmation first: request shows the dialog, confirm performs the delete.
     */
    fun requestDeleteTransaction(id: Long) {
        _uiState.update { it.copy(pendingDeleteTransactionId = id) }
    }

    fun cancelDeleteTransaction() {
        _uiState.update { it.copy(pendingDeleteTransactionId = null) }
    }

    fun confirmDeleteTransaction() {
        val id = _uiState.value.pendingDeleteTransactionId ?: return
        _uiState.update { it.copy(pendingDeleteTransactionId = null) }
        deleteTransaction(id)
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            val state = _uiState.value
            val transaction = state.transactions.firstOrNull { it.id == id }
            val transferRows = TransferMatching.manualTransferRowsFor(transaction, state.transactions)
            // One shared snapshot for every reversal: sequential balance writes must see
            // each other, or a later write silently clobbers an earlier one.
            val accountsSnapshot = state.accounts.associateBy { it.id }.toMutableMap()
            val rowsToDelete = transferRows.ifEmpty { listOfNotNull(transaction) }
            rowsToDelete.forEach { tx ->
                moveAccountBalanceForTransaction(tx, reverse = true, accountsById = accountsSnapshot)
                repository.deleteTransaction(tx.id)
            }
            if (rowsToDelete.isEmpty()) repository.deleteTransaction(id)
            // Remember the SMS behind deleted auto-detected rows so the next catch-up scan
            // does not resurrect the transaction (and move the balance yet again).
            rememberDismissedSmsKeys(rowsToDelete.filter { it.isAutoDetected }.map { it.rawMessage })
            reloadState { it.copy(showTransactionDetailSheet = false, selectedTransactionId = null) }
        }
    }

    /**
     * Persists the normalized SMS keys of deleted/ignored auto-detected rows; scans treat
     * them as already imported forever, so user deletions stick.
     */
    private suspend fun rememberDismissedSmsKeys(rawMessages: List<String?>) {
        val keys = rawMessages.flatMap { normalizedSmsKeys(it) }
        if (keys.isEmpty()) return
        _uiState.update { it.copy(dismissedSmsKeys = it.dismissedSmsKeys + keys) }
        repository.persistUserSettings(_uiState.value)
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

    /**
     * The single catch-up scan: resumes from the last successful scan (or onboarding on the
     * first run) through today, so no post-onboarding SMS is ever missed no matter how long
     * the app was closed. Decoupled from the Activity date filter, which is view-only.
     */
    fun scanForNewMessages() {
        // Never import before registration completes: onboarding defines where tracking
        // starts, and scanning without it would resurrect history after a data wipe.
        if (!_uiState.value.hasCompletedRegistration) return
        viewModelScope.launch {
            val state = _uiState.value
            val today = LocalDate.now()
            val onboardedDate = state.onboardedAtMillis
                .takeIf { it > 0L }
                ?.let { millisToLocalDate(it) }
            val lastScanDate = state.lastSuccessfulScanMillis
                .takeIf { it > 0L }
                ?.let { millisToLocalDate(it) }
            // Re-cover the last scan's day so messages that arrived later that day are not lost;
            // the duplicate filter absorbs the overlap.
            val start = lastScanDate
                ?: onboardedDate
                ?: today
            scanMessages(MessageScanRange.Custom, start.coerceAtMost(today), today)
        }
    }

    private var pendingScanNote: String? = null

    private fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
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

    fun scanMessages(
        range: MessageScanRange,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ) {
        if (_uiState.value.isScanningMessages) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanningMessages = true,
                    scanStartedAtMillis = System.currentTimeMillis(),
                    scanProcessedCount = 0,
                    scanTotalCount = 0,
                    scanStatusMessage = "Scanning messages..."
                )
            }
            val hasPermission = hasSmsPermission()
            val parsedMessages = if (hasPermission) {
                // Each state push recomposes the whole tree and invalidates every derived
                // cache, so throttle progress to ~4 updates/sec instead of every 2 messages.
                var lastProgressPushMillis = 0L
                val progressCallback: (SmsScanProgress) -> Unit = { progress ->
                    val now = android.os.SystemClock.uptimeMillis()
                    if (progress.processed == progress.total || now - lastProgressPushMillis >= 250L) {
                        lastProgressPushMillis = now
                        _uiState.update {
                            it.copy(
                                scanProcessedCount = progress.processed,
                                scanTotalCount = progress.total
                            )
                        }
                    }
                }
                withContext(Dispatchers.IO) {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    when (range) {
                        MessageScanRange.Today -> TodaySmsScanner(getApplication()).scanToday(progressCallback)
                        MessageScanRange.Yesterday -> TodaySmsScanner(getApplication()).scanYesterday(progressCallback)
                        MessageScanRange.Week -> TodaySmsScanner(getApplication()).scanLast7Days(progressCallback)
                        MessageScanRange.Custom -> {
                            val today = LocalDate.now()
                            val start = startDate ?: today
                            val end = endDate?.coerceAtMost(today) ?: today
                            if (start > end) {
                                emptyList()
                            } else {
                                TodaySmsScanner(getApplication()).scanRange(start, end, progressCallback)
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

            val existingTransactions = _uiState.value.transactions
            val existingDrafts = _uiState.value.detectedDrafts
            val normalizedExisting = buildSet {
                // Keys of rows the user deleted or ignored: treat as imported so they never
                // come back on a rescan.
                addAll(_uiState.value.dismissedSmsKeys)
                addAll(existingTransactions.flatMap { normalizedSmsKeys(it.rawMessage) })
                addAll(existingDrafts.flatMap { normalizedSmsKeys(it.rawMessage) })
            }.toMutableSet()
            val filteredParsed = SmsTransactionNormalizer.filterImportBatch(parsedMessages)
            val accountsSnapshot = _uiState.value.accounts.associateBy { it.id }.toMutableMap()
            // Raw keys already locked into a Transfer row: never re-plan those legs.
            val transferRawKeys = existingTransactions
                .filter { it.type == TransactionType.Transfer }
                .flatMap { normalizedSmsKeys(it.rawMessage) }
                .toHashSet()
            // Previously imported lone legs (and pending drafts) that a newly scanned
            // opposite leg may complete into a transfer.
            val mergeableTransactionByKey = existingTransactions
                .filter { it.type != TransactionType.Transfer && it.isAutoDetected && !it.isCreditCardTransaction }
                .flatMap { tx -> normalizedSmsKeys(tx.rawMessage).map { key -> key to tx } }
                .toMap()
            val draftByKey = existingDrafts
                .flatMap { draft -> normalizedSmsKeys(draft.rawMessage).map { key -> key to draft } }
                .toMap()
            val plannerInput = filteredParsed + existingTransferLegCandidates(
                batch = filteredParsed,
                transactions = existingTransactions,
                drafts = existingDrafts
            )
            val plannedImports = SmsImportPlanner.plan(plannerInput, accountsSnapshot.values.toList())
            val uncategorizedId = _uiState.value.categories.firstOrNull { category ->
                category.name == "Uncategorized"
            }?.id ?: 0L
            val transferCategoryId = _uiState.value.transferCategoryId()
            repository.cleanupCreditCardRepaymentArtifacts()
            // Tracking starts on the onboarding DAY: the whole install day stays visible so the
            // app never looks empty on day one, while older history stays out. Balance movement
            // is still guarded by the exact onboarding moment separately.
            val trackingStartMillis = _uiState.value.onboardedAtMillis
                .takeIf { it > 0L }
                ?.let {
                    millisToLocalDate(it)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                ?: 0L
            var importedCount = 0
            var reviewCount = 0
            var autoMappedCount = 0
            var transferCount = 0
            var skippedPreOnboarding = 0
            plannedImports.forEach { planned ->
                if (trackingStartMillis > 0L && planned.timestampMillis < trackingStartMillis) {
                    skippedPreOnboarding += 1
                    return@forEach
                }
                when (planned) {
                    is PlannedSmsImport.Transfer -> {
                        val rawKeys = planned.rawMessages.flatMap { normalizedSmsKeys(it) }
                        if (rawKeys.isEmpty() || rawKeys.any { it in transferRawKeys }) return@forEach
                        val legsToMerge = rawKeys.mapNotNull { mergeableTransactionByKey[it] }.distinctBy { it.id }
                        val draftsToClear = rawKeys.mapNotNull { draftByKey[it] }.distinctBy { it.id }
                        val alreadyKnownKeys = rawKeys.filter { it in normalizedExisting }
                        val mergeCoveredKeys = buildSet {
                            legsToMerge.forEach { addAll(normalizedSmsKeys(it.rawMessage)) }
                            draftsToClear.forEach { addAll(normalizedSmsKeys(it.rawMessage)) }
                        }
                        // A leg that exists on a row we cannot merge (manual entry, edited row)
                        // keeps the old skip behavior instead of being consumed.
                        if (alreadyKnownKeys.any { it !in mergeCoveredKeys }) return@forEach
                        val addsNewLeg = rawKeys.any { it !in normalizedExisting }
                        if (!addsNewLeg && legsToMerge.isEmpty() && draftsToClear.isEmpty()) return@forEach
                        legsToMerge.forEach { leg ->
                            moveAccountBalanceForTransaction(leg, reverse = true, accountsById = accountsSnapshot)
                            repository.deleteTransaction(leg.id)
                        }
                        draftsToClear.forEach { repository.deleteDraft(it.id) }
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
                        if (rawKeys.isEmpty() || rawKeys.any { it in transferRawKeys }) return@forEach
                        // Allow a newly arrived leg to upgrade a pending single-leg transfer
                        // draft into a two-leg review; otherwise keep the old dedupe rules.
                        val addsNewLeg = rawKeys.any { it !in normalizedExisting }
                        if (!addsNewLeg) return@forEach
                        val draftsToReplace = rawKeys.mapNotNull { draftByKey[it] }
                            .distinctBy { it.id }
                            .filter { it.type == TransactionType.Transfer }
                        val replacedKeys = draftsToReplace
                            .flatMap { normalizedSmsKeys(it.rawMessage) }
                            .toSet()
                        if (rawKeys.any { it in normalizedExisting && it !in replacedKeys }) return@forEach
                        draftsToReplace.forEach { repository.deleteDraft(it.id) }
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
                if (absorbLegIntoExistingTransfer(msg, existingTransactions)) {
                    normalizedExisting.addAll(rawKeys)
                    return@forEach
                }
                normalizedExisting.addAll(rawKeys)
                val categoryId = inferCategoryId(msg) ?: uncategorizedId
                val availableAccounts = accountsSnapshot.values.toList()
                val accountCandidates = if (msg.isCreditCardTransaction) {
                    availableAccounts.filter { it.type == AccountType.CreditCard }
                } else {
                    availableAccounts.filter { it.type == AccountType.Bank }
                }
                val accountId = SmsBankKeys.resolveAccountId(msg.bankName, accountCandidates)
                    ?: if (msg.isCreditCardTransaction) null else accountCandidates.singleOrNull()?.id
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
                    excludeFromSummary = msg.excludeFromSummary,
                    isCreditCardTransaction = msg.isCreditCardTransaction
                )
                val transaction = normalizeInvestmentTransaction(draftTransaction)
                repository.addTransaction(transaction)
                applyTransactionBalanceMovement(transaction, accountsSnapshot)
                importedCount += 1
            }

            val baseStatus = when {
                parsedMessages.isEmpty() -> "No transaction messages found for this period."
                filteredParsed.isEmpty() -> "Found ${parsedMessages.size} transaction-like SMS, but all were filtered as duplicates, reminders, or internal transfers."
                importedCount == 0 && reviewCount == 0 && skippedPreOnboarding > 0 ->
                    "Skipped $skippedPreOnboarding SMS from before you started tracking; nothing new to import."
                plannedImports.isEmpty() -> "Found ${filteredParsed.size} transaction SMS, but no new transactions were imported."
                importedCount == 0 && reviewCount == 0 -> "Found ${plannedImports.size} transaction SMS, but no new transactions were imported."
                importedCount == 0 -> "Found $reviewCount transaction SMS that need your review."
                reviewCount > 0 && transferCount > 0 -> "Imported $importedCount transactions, including $transferCount transfers, and sent $reviewCount for review. Auto-mapped $autoMappedCount account links."
                reviewCount > 0 -> "Imported $importedCount transactions and sent $reviewCount for review. Auto-mapped $autoMappedCount account links."
                transferCount > 0 -> "Imported $importedCount transactions, including $transferCount transfers. Auto-mapped $autoMappedCount account links."
                importedCount == 1 -> "Imported 1 transaction. Auto-mapped $autoMappedCount account links."
                else -> "Imported $importedCount transactions. Auto-mapped $autoMappedCount account links."
            }
            val status = listOfNotNull(pendingScanNote, baseStatus).joinToString(" ")
            pendingScanNote = null
            val newestMonth = parsedMessages.maxByOrNull { it.transactionTimestampMillis }
                ?.let {
                    YearMonth.from(
                        Instant.ofEpochMilli(it.transactionTimestampMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    )
                }
            repository.remapTransactionAccountsFromSmsLabels()
            reloadState()
            mergeExistingCrossAccountTransferPairs()
            // The scan reached this point with SMS permission, so the next catch-up can resume here.
            repository.persistUserSettings(
                _uiState.value.copy(lastSuccessfulScanMillis = System.currentTimeMillis())
            )
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
                // Remove lone legs of this transfer that were already imported as
                // individual income/expense rows, so accepting the draft cannot
                // leave the amount double-counted.
                val draftKeys = normalizedSmsKeys(draft.rawMessage).toSet()
                if (draftKeys.isNotEmpty()) {
                    _uiState.value.transactions
                        .filter { it.type != TransactionType.Transfer && it.isAutoDetected && !it.isCreditCardTransaction }
                        .filter { tx ->
                            val keys = normalizedSmsKeys(tx.rawMessage)
                            keys.isNotEmpty() && keys.all { it in draftKeys }
                        }
                        .forEach { leg ->
                            moveAccountBalanceForTransaction(leg, reverse = true, accountsById = accountsById)
                            repository.deleteTransaction(leg.id)
                        }
                }
                repository.addTransaction(transfer)
                applyTransactionBalanceMovement(transfer, accountsById)
                repository.deleteDraft(draftId)
                reloadState()
                return@launch
            }
            val accountsSnapshot = _uiState.value.accounts
            val isCreditCardTransaction = SmsTransactionNormalizer.isCreditCardSpend(draft.rawMessage) ||
                SmsTransactionNormalizer.isCreditCardRefund(draft.rawMessage)
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
            val draft = _uiState.value.detectedDrafts.firstOrNull { it.id == draftId }
            repository.deleteDraft(draftId)
            rememberDismissedSmsKeys(listOf(draft?.rawMessage))
            reloadState()
        }
    }

    private fun reload() {
        viewModelScope.launch {
            runCatching {
                repository.cleanupCreditCardRepaymentArtifacts()
                reloadState()
                ensureTrackingStartAnchored()
                if (mergeExistingCrossAccountTransferPairs() > 0) {
                    reloadState()
                }
                refreshDiscoveredSmsBanks()
            }
                .onFailure {
                    _uiState.update { state -> state.copy(isAppInitializing = false) }
                }
        }
    }

    /**
     * Older installs (upgraded in place before onboarding stamping existed) can have
     * onboardedAtMillis == 0, which leaves the "tracking starts on install day" cutoff open.
     * Anchor it once to now so scans can never import — and old imports can never move the
     * balance for — transactions dated before this point.
     */
    private suspend fun ensureTrackingStartAnchored() {
        val state = _uiState.value
        if (!state.hasCompletedRegistration) return
        if (state.onboardedAtMillis > 0L) return
        val next = state.copy(onboardedAtMillis = System.currentTimeMillis())
        repository.persistUserSettings(next)
        reloadState()
    }

    /**
     * Repairs cross-account transfers that were imported as two individual rows (an income
     * and an expense) before pairing logic could catch them. Re-plans the raw SMSes of all
     * auto-detected lone legs and collapses resolved pairs into a single Transfer row,
     * reversing the old balance movements. Runs at startup and after scans; idempotent
     * because merged raw SMS keys become part of a Transfer row and are skipped afterwards.
     */
    private suspend fun mergeExistingCrossAccountTransferPairs(): Int {
        val state = _uiState.value
        if (state.accounts.isEmpty()) return 0
        val accountsSnapshot = state.accounts.associateBy { it.id }.toMutableMap()
        val transferRawKeys = state.transactions
            .filter { it.type == TransactionType.Transfer }
            .flatMap { normalizedSmsKeys(it.rawMessage) }
            .toHashSet()
        var mergedCount = 0

        // Rows that duplicate an SMS — or share its bank reference number — already
        // recorded on a Transfer row are leftovers from before the transfer was
        // recognized. The transfer accounts for both sides, so drop them.
        state.transactions
            .filter { tx ->
                tx.type != TransactionType.Transfer &&
                    tx.isAutoDetected &&
                    !tx.isCreditCardTransaction &&
                    normalizedSmsKeys(tx.rawMessage).any { it in transferRawKeys }
            }
            .forEach { duplicate ->
                moveAccountBalanceForTransaction(duplicate, reverse = true, accountsById = accountsSnapshot)
                repository.deleteTransaction(duplicate.id)
                mergedCount += 1
            }
        state.detectedDrafts
            .filter { draft -> normalizedSmsKeys(draft.rawMessage).any { it in transferRawKeys } }
            .forEach { repository.deleteDraft(it.id) }

        val mergeableRows = state.transactions.filter { tx ->
            tx.type != TransactionType.Transfer &&
                tx.isAutoDetected &&
                !tx.isCreditCardTransaction &&
                !tx.rawMessage.isNullOrBlank() &&
                PAIRED_TRANSFER_SMS_DELIMITER !in tx.rawMessage.orEmpty() &&
                normalizedSmsKeys(tx.rawMessage).none { it in transferRawKeys }
        }
        if (mergeableRows.isEmpty()) return mergedCount
        val rowByKey = mergeableRows
            .flatMap { tx -> normalizedSmsKeys(tx.rawMessage).map { key -> key to tx } }
            .toMap()
        val reparsed = mergeableRows.mapNotNull { tx ->
            TransactionMessageParser.parse(tx.rawMessage.orEmpty(), tx.timestampMillis)
        }
        if (reparsed.isEmpty()) return mergedCount
        val planned = SmsImportPlanner.plan(reparsed, accountsSnapshot.values.toList())
        val transferCategoryId = state.transferCategoryId()
        val consumedRowIds = mutableSetOf<Long>()

        planned.forEach { plan ->
            val (creditMsg, plannedFrom, plannedTo, source) = when (plan) {
                is PlannedSmsImport.Transfer ->
                    MergeCandidate(plan.credit, plan.fromAccountId, plan.toAccountId, plan.source)
                is PlannedSmsImport.TransferReview ->
                    MergeCandidate(plan.credit, plan.fromAccountId, plan.toAccountId, plan.source)
                is PlannedSmsImport.Message -> return@forEach
            }
            val debitMsg = when (plan) {
                is PlannedSmsImport.Transfer -> plan.debit
                is PlannedSmsImport.TransferReview -> plan.debit
                is PlannedSmsImport.Message -> return@forEach
            }
            val rawKeys = plan.rawMessages.flatMap { normalizedSmsKeys(it) }
            val legs = rawKeys.mapNotNull { rowByKey[it] }.distinctBy { it.id }
            // Every SMS in the plan must correspond to an existing imported row.
            if (legs.isEmpty() || legs.size != plan.rawMessages.size) return@forEach
            if (legs.any { it.id in consumedRowIds }) return@forEach
            val debitRow = legs.firstOrNull { it.type == TransactionType.Expense }
            val creditRow = legs.firstOrNull { it.type == TransactionType.Income }
            // Single-leg plans (cc bill payments, self transfers whose second SMS never
            // arrived) carry one row; paired plans must map to both rows.
            val isSingleLegPlan = creditMsg == null
            if (!isSingleLegPlan && (debitRow == null || creditRow == null)) return@forEach
            // When SMS labels do not resolve to registered accounts, fall back to the
            // accounts already assigned on the imported rows themselves.
            val fromAccountId = plannedFrom ?: debitRow?.accountId
            val toAccountId = plannedTo ?: creditRow?.accountId
            if (fromAccountId == null || toAccountId == null || fromAccountId == toAccountId) return@forEach

            legs.forEach { leg ->
                moveAccountBalanceForTransaction(leg, reverse = true, accountsById = accountsSnapshot)
                repository.deleteTransaction(leg.id)
                consumedRowIds += leg.id
            }
            val transfer = detectedTransferTransaction(
                plan = PlannedSmsImport.Transfer(
                    debit = debitMsg,
                    credit = creditMsg,
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    source = source
                ),
                categoryId = transferCategoryId,
                accountsById = accountsSnapshot
            )
            repository.addTransaction(transfer)
            applyTransactionBalanceMovement(transfer, accountsSnapshot)
            mergedCount += 1
        }

        // Absorb rows that are really the second leg of a Transfer created from a single
        // SMS: the transfer already moved both balances, so the lone row double-counts
        // its side. Remove the row and record its SMS on the transfer.
        val transferRows = state.transactions.filter { it.type == TransactionType.Transfer }
        val usedTransferIds = mutableSetOf<Long>()
        mergeableRows.filter { it.id !in consumedRowIds }.forEach { row ->
            val parsedRow = TransactionMessageParser.parse(row.rawMessage.orEmpty(), row.timestampMillis)
                ?: return@forEach
            val qualifies = parsedRow.isInternalTransfer ||
                SmsTransactionNormalizer.isNamelessOwnAccountCredit(row.rawMessage)
            if (!qualifies) return@forEach
            val transfer = findSingleLegTransferForLeg(
                amount = row.amount,
                timestampMillis = row.timestampMillis,
                legType = row.type,
                legAccountId = row.accountId,
                transfers = transferRows.filter { it.id !in usedTransferIds }
            ) ?: return@forEach
            usedTransferIds += transfer.id
            moveAccountBalanceForTransaction(row, reverse = true, accountsById = accountsSnapshot)
            repository.deleteTransaction(row.id)
            consumedRowIds += row.id
            repository.updateTransaction(
                transfer.copy(
                    rawMessage = pairedTransferRawMessage(
                        listOf(transfer.rawMessage.orEmpty(), row.rawMessage.orEmpty())
                    )
                )
            )
            mergedCount += 1
        }
        return mergedCount
    }

    private data class MergeCandidate(
        val credit: com.moneymanager.app.data.ParsedTransactionMessage?,
        val fromAccountId: Long?,
        val toAccountId: Long?,
        val source: SmsTransferSource
    )

    /**
     * Finds a Transfer row created from a single SMS leg that the given opposite leg
     * belongs to (same amount, within the delivery-gap window, matching side account).
     */
    private fun findSingleLegTransferForLeg(
        amount: Double,
        timestampMillis: Long,
        legType: TransactionType,
        legAccountId: Long?,
        transfers: List<LedgerTransaction>
    ): LedgerTransaction? {
        return transfers.firstOrNull { transfer ->
            transfer.type == TransactionType.Transfer &&
                !transfer.rawMessage.isNullOrBlank() &&
                PAIRED_TRANSFER_SMS_DELIMITER !in transfer.rawMessage.orEmpty() &&
                kotlin.math.abs(transfer.amount - amount) <= 0.02 &&
                kotlin.math.abs(transfer.timestampMillis - timestampMillis) <= SmsImportPlanner.EXTENDED_PAIR_WINDOW_MS &&
                when (legType) {
                    TransactionType.Income -> legAccountId == null || transfer.toAccountId == legAccountId
                    TransactionType.Expense -> legAccountId == null || transfer.fromAccountId == legAccountId
                    TransactionType.Transfer -> false
                }
        }
    }

    /**
     * A late-arriving second leg of a self transfer whose Transfer row already exists
     * (created from the first leg alone) is recorded on that row instead of being
     * imported as a separate income/expense.
     */
    private suspend fun absorbLegIntoExistingTransfer(
        msg: com.moneymanager.app.data.ParsedTransactionMessage,
        existingTransactions: List<LedgerTransaction>
    ): Boolean {
        if (msg.type == TransactionType.Transfer) return false
        val qualifies = msg.isInternalTransfer ||
            SmsTransactionNormalizer.isNamelessOwnAccountCredit(msg.rawMessage)
        if (!qualifies) return false
        val bankAccounts = _uiState.value.accounts.filter { it.type == AccountType.Bank }
        val legAccountId = SmsBankKeys.resolveAccountId(msg.bankName, bankAccounts)
        val transfer = findSingleLegTransferForLeg(
            amount = msg.amount,
            timestampMillis = msg.transactionTimestampMillis,
            legType = msg.type,
            legAccountId = legAccountId,
            transfers = existingTransactions
        ) ?: return false
        repository.updateTransaction(
            transfer.copy(
                rawMessage = pairedTransferRawMessage(
                    listOf(transfer.rawMessage.orEmpty(), msg.rawMessage)
                )
            )
        )
        return true
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

    private suspend fun reconcileBalanceForTransactionUpdate(
        oldTransaction: LedgerTransaction,
        newTransaction: LedgerTransaction,
        accountsById: MutableMap<Long, BankAccount>? = null
    ) {
        // Net the reversal and re-application per account and write once. Applying them as
        // two separate writes without a shared snapshot loses the first write (the second
        // read sees the stale pre-reversal balance), which double-charged the account on
        // every category edit of an imported transaction.
        val accounts = accountsById ?: _uiState.value.accounts.associateBy { it.id }.toMutableMap()
        val netDeltas = LinkedHashMap<Long, Double>()
        balanceMovementsForTransaction(oldTransaction, accounts).forEach { (accountId, delta) ->
            netDeltas.merge(accountId, -delta, Double::plus)
        }
        balanceMovementsForTransaction(newTransaction, accounts).forEach { (accountId, delta) ->
            netDeltas.merge(accountId, delta, Double::plus)
        }
        netDeltas.forEach { (accountId, delta) -> updateAccountBalance(accountId, delta, accounts) }
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
                    accountForBalanceMovement(id, accountsById)
                        ?.takeIf { transactionMovesBalanceOf(transaction, it) }
                        ?.let { account -> id to outgoingBalanceMovement(account, transaction.amount) }
                },
                transaction.toAccountId?.let { id ->
                    accountForBalanceMovement(id, accountsById)
                        ?.takeIf { transactionMovesBalanceOf(transaction, it) }
                        ?.let { account -> id to incomingBalanceMovement(account, transaction.amount) }
                }
            )
        }

        val accountId = transaction.accountId ?: return emptyList()
        val account = accountForBalanceMovement(accountId, accountsById) ?: return emptyList()
        if (!transactionMovesBalanceOf(transaction, account)) return emptyList()
        return listOf(accountId to singleAccountBalanceMovement(transaction, account))
            .filter { it.second != 0.0 }
    }

    /**
     * An account's balance is ground truth as of its anchor (registration, account creation, or
     * the last manual balance edit — whichever is latest). Transactions dated before that moment
     * are already reflected in the anchored figure, so importing, editing, or deleting them must
     * never move the balance again.
     */
    private fun transactionMovesBalanceOf(transaction: LedgerTransaction, account: BankAccount): Boolean {
        val cutoff = maxOf(_uiState.value.onboardedAtMillis, account.balanceAnchorAtMillis)
        return transaction.timestampMillis >= cutoff
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
            scanStatusMessage = current.scanStatusMessage,
            budgetWarning = current.budgetWarning,
            discoveredSmsBanks = current.discoveredSmsBanks
        )
        _uiState.value = transform(loaded)
        TransactionMessageParser.selfName = _uiState.value.userName.takeIf { it.isNotBlank() }
        // Nearly every data mutation funnels through here, so this one hook keeps the
        // home-screen widgets in sync with the app.
        MoneyWidgets.refresh(getApplication())
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

    /**
     * Re-parses previously imported lone legs (and pending drafts) near the scanned batch so the
     * planner can pair them with a counterpart SMS that arrived after a delivery gap — the two
     * legs of a cross-bank UPI self transfer often land minutes or hours apart, or in different
     * scans entirely.
     */
    private fun existingTransferLegCandidates(
        batch: List<com.moneymanager.app.data.ParsedTransactionMessage>,
        transactions: List<LedgerTransaction>,
        drafts: List<DetectedTransactionDraft>
    ): List<com.moneymanager.app.data.ParsedTransactionMessage> {
        if (batch.isEmpty()) return emptyList()
        val batchKeys = batch.flatMap { normalizedSmsKeys(it.rawMessage) }.toHashSet()
        val earliest = batch.minOf { it.transactionTimestampMillis } - SmsImportPlanner.EXTENDED_PAIR_WINDOW_MS
        val latest = batch.maxOf { it.transactionTimestampMillis } + SmsImportPlanner.EXTENDED_PAIR_WINDOW_MS

        fun reparse(rawMessage: String?, timestampMillis: Long): com.moneymanager.app.data.ParsedTransactionMessage? {
            val raw = rawMessage.orEmpty()
            if (raw.isBlank() || PAIRED_TRANSFER_SMS_DELIMITER in raw) return null
            if (normalizedSmsKeys(raw).any { it in batchKeys }) return null
            return TransactionMessageParser.parse(raw, timestampMillis)
        }

        val fromTransactions = transactions.asSequence()
            .filter { it.type != TransactionType.Transfer && it.isAutoDetected && !it.isCreditCardTransaction }
            .filter { it.timestampMillis in earliest..latest }
            .mapNotNull { reparse(it.rawMessage, it.timestampMillis) }
        val fromDrafts = drafts.asSequence()
            .filter { it.transactionTimestampMillis in earliest..latest }
            .mapNotNull { reparse(it.rawMessage, it.transactionTimestampMillis) }
        return (fromTransactions + fromDrafts)
            .distinctBy { normalizedSmsKeys(it.rawMessage) }
            .toList()
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
            .flatMap { part ->
                val text = normalizedSmsRaw(part)
                if (text.isBlank()) return@flatMap emptyList<String>()
                // The bank reference number identifies the transaction across differently
                // worded SMSes (and across both legs of a transfer), so it dedupes what
                // exact-text matching cannot.
                listOfNotNull(
                    text,
                    SmsTransactionNormalizer.transactionReference(part)?.let { "upiref:$it" }
                )
            }
    }

    private fun FinanceUiState.transferCategoryId(): Long {
        return categories.firstOrNull { category ->
            category.name.equals("Transfer", ignoreCase = true) ||
                category.iconKey.equals("transfer", ignoreCase = true)
        }?.id ?: categories.firstOrNull { it.name == "Uncategorized" }?.id ?: categories.first().id
    }

}
