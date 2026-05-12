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
import com.moneymanager.app.model.UiAccent
import com.moneymanager.app.model.UiSurface
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
            repository.persistUserSettings(
                _uiState.value.copy(
                    userName = name.trim(),
                    bankSmsSetupCompleted = true
                )
            )
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
        val cur = _uiState.value.summarySelectedAccountIds
        val nextSet = if (accountId in cur) cur - accountId else cur + accountId
        setSummaryAccountFilter(nextSet)
    }

    fun clearSummaryAccountFilter() {
        setSummaryAccountFilter(emptySet())
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
                rawMessage = rawMessage,
                smsBankLabel = null,
                excludeFromSummary = false
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

            val normalizedExisting = buildSet {
                addAll(_uiState.value.transactions.mapNotNull { normalizedSmsRaw(it.rawMessage) })
                addAll(_uiState.value.detectedDrafts.map { normalizedSmsRaw(it.rawMessage) })
            }.toMutableSet()
            val filteredParsed = SmsTransactionNormalizer.filterImportBatch(parsedMessages)
            val accountsSnapshot = _uiState.value.accounts
            repository.cleanupCreditCardRepaymentArtifacts()
            var importedCount = 0
            filteredParsed.forEach { msg ->
                val normRaw = normalizedSmsRaw(msg.rawMessage)
                if (normRaw.isBlank() || normRaw in normalizedExisting) return@forEach
                normalizedExisting.add(normRaw)
                val categoryId = inferCategoryId(msg) ?: _uiState.value.categories.firstOrNull { category ->
                    category.name == "Uncategorized"
                }?.id ?: 0L
                val accountId = SmsBankKeys.resolveAccountId(msg.bankName, accountsSnapshot)
                    ?: accountsSnapshot.singleOrNull()?.id
                repository.addTransaction(
                    LedgerTransaction(
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
                        excludeFromSummary = false
                    )
                )
                importedCount += 1
            }

            val status = when {
                parsedMessages.isEmpty() -> "No transaction messages found for this period."
                filteredParsed.isEmpty() -> "Found ${parsedMessages.size} transaction-like SMS, but all were filtered as duplicates, reminders, or internal transfers."
                importedCount == 0 -> "Found ${filteredParsed.size} transaction SMS, but no new transactions were imported."
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
            val accountsSnapshot = _uiState.value.accounts
            repository.addTransaction(
                LedgerTransaction(
                    id = 0,
                    name = draft.counterparty,
                    amount = draft.amount,
                    type = draft.type,
                    categoryId = categoryId,
                    accountId = SmsBankKeys.resolveAccountId(draft.bankName, accountsSnapshot)
                        ?: accountsSnapshot.singleOrNull()?.id,
                    timestampMillis = draft.transactionTimestampMillis,
                    isAutoDetected = true,
                    rawMessage = draft.rawMessage,
                    smsBankLabel = draft.bankName,
                    excludeFromSummary = false
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

    private fun normalizedSmsRaw(raw: String?): String =
        raw?.replace('\n', ' ')?.trim()?.replace(Regex("\\s+"), " ").orEmpty()

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

    private suspend fun applyCategoryToSimilarUncategorized(
        source: LedgerTransaction,
        categoryId: Long,
        type: TransactionType
    ) {
        val uncategorizedId = _uiState.value.categories.firstOrNull { it.name == "Uncategorized" }?.id ?: 0L
        if (categoryId == uncategorizedId) return
        val sourceKey = categoryLearningKey(source.name, source.rawMessage, source.type, source.smsBankLabel)
        if (sourceKey.isBlank()) return
        _uiState.value.transactions
            .filter {
                it.id != source.id &&
                    it.categoryId == uncategorizedId &&
                    categoryLearningKey(it.name, it.rawMessage, it.type, it.smsBankLabel) == sourceKey
            }
            .forEach {
                repository.updateTransaction(it.copy(type = type, categoryId = categoryId))
            }
    }

    private fun inferCategoryId(msg: com.moneymanager.app.data.ParsedTransactionMessage): Long? {
        val uncategorizedId = _uiState.value.categories.firstOrNull { it.name == "Uncategorized" }?.id ?: 0L
        val key = categoryLearningKey(msg.counterparty, msg.rawMessage, msg.type, msg.bankName)
        if (key.isBlank()) return null
        return _uiState.value.transactions
            .asSequence()
            .filter { it.categoryId != uncategorizedId }
            .filter { categoryLearningKey(it.name, it.rawMessage, it.type, it.smsBankLabel) == key }
            .groupingBy { it.categoryId }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    private fun categoryLearningKey(
        name: String,
        rawMessage: String?,
        type: TransactionType,
        smsBankLabel: String?
    ): String {
        if (SmsTransactionNormalizer.isNonLedgerTransactionArtifact(rawMessage, type)) return ""
        val merchant = merchantFingerprint(name, rawMessage, smsBankLabel)
        if (merchant.isBlank()) return ""
        val source = rawMessage?.lowercase().orEmpty()
        val channel = when {
            "upi" in source -> "upi"
            "neft" in source -> "neft"
            "imps" in source -> "imps"
            "card" in source -> "card"
            else -> "sms"
        }
        return "${type.name.lowercase()}|$channel|$merchant"
    }

    private fun merchantFingerprint(name: String, rawMessage: String?, smsBankLabel: String?): String {
        val nameCandidate = normalizeMerchantCandidate(name, smsBankLabel)
        if (nameCandidate.isNotBlank()) return nameCandidate

        val raw = rawMessage.orEmpty()
        val candidates = listOfNotNull(
            Regex("""(?i);\s*([A-Z0-9 .&_-]{2,40})\s+(?:debited|credited)""")
                .find(raw)
                ?.groupValues
                ?.getOrNull(1),
            Regex("""(?i)\b(?:to|at|from|for|towards)\s+([a-z0-9 .&_-]{3,40})""")
                .find(raw)
                ?.groupValues
                ?.getOrNull(1)
                ?.substringBefore(" on ")
                ?.substringBefore(" ref")
                ?.substringBefore(" using")
        )
        return candidates
            .asSequence()
            .map { normalizeMerchantCandidate(it, smsBankLabel) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
    }

    private fun normalizeMerchantCandidate(candidate: String, smsBankLabel: String?): String {
        val bankTokens = smsBankLabel
            ?.lowercase()
            .orEmpty()
            .replace(Regex("""\b(?:rs\.?|inr|rupees?)\s*[\d,]+(?:\.\d{1,2})?\b"""), " ")
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .split(" ")
            .filter { it.length >= 3 }
            .toSet()
        val stopWords = setOf(
            "debited",
            "credited",
            "credit",
            "debit",
            "account",
            "bank",
            "card",
            "ending",
            "payment",
            "received",
            "amount",
            "available",
            "balance",
            "transaction",
            "reference",
            "your",
            "a/c",
            "paid",
            "sent",
            "from",
            "with"
        )
        val tokens = candidate
            .lowercase()
            .substringBefore("http")
            .replace(Regex("""\b(?:on|by)\s+\d{1,2}\b"""), " ")
            .replace(Regex("""\b(?:rs\.?|inr|rupees?)\s*[\d,]+(?:\.\d{1,2})?\b"""), " ")
            .replace(Regex("""\b\d{1,2}[-/][a-z]{3}[-/]\d{2,4}\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\b\d{1,2}[-/]\d{1,2}[-/]\d{2,4}\b"""), " ")
            .replace(Regex("""\b(?:ref|rrn|utr|txn|transaction|upi)[\s:.-]*[a-z0-9-]+\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\b[x*]*\d{3,}\b"""), " ")
            .replace(Regex("""\b\d+\b"""), " ")
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .split(" ")
            .map { it.trim() }
            .filter { it.length >= 3 }
            .filterNot { it in stopWords || it in bankTokens }
            .take(4)
        if (tokens.isEmpty()) return ""
        if (tokens.size == 1 && tokens.first() in setOf("hdfc", "icici", "axis", "kotak", "indian", "sbi")) return ""
        return tokens.joinToString(" ")
    }
}
