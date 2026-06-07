package com.moneymanager.app.data

import com.google.gson.Gson
import com.moneymanager.app.model.AppBackupData
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.BudgetPlan
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.DefaultCategories
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction

class FinanceRepository(private val dao: FinanceDao) {
    suspend fun loadState(current: FinanceUiState = FinanceUiState()): FinanceUiState {
        seedDefaultCategories()
        val settings = dao.getSettings()
        val base = settings?.applyTo(current) ?: current.copy(
            userName = "",
            summarySelectedAccountIds = emptySet()
        )
        return base.copy(
            accounts = dao.getAccounts().map { it.toModel() },
            categories = dao.getCategories().map { it.toModel() },
            transactions = dao.getTransactions().map { it.toModel() },
            budgets = dao.getBudgets().map { it.toModel() },
            detectedDrafts = dao.getDrafts().map { it.toModel() }
        )
    }

    suspend fun persistUserSettings(state: FinanceUiState) {
        dao.saveSettings(state.toSettingsEntity())
    }

    suspend fun addAccount(name: String, balance: Double, smsMatchKey: String? = null): Long {
        return dao.saveAccount(
            AccountEntity(
                name = name,
                balance = balance,
                smsMatchKey = smsMatchKey?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    suspend fun updateAccount(account: BankAccount) {
        dao.saveAccount(
            AccountEntity(
                id = account.id,
                name = account.name,
                balance = account.balance,
                smsMatchKey = account.smsMatchKey?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    suspend fun remapTransactionAccountsFromSmsLabels() {
        val accounts = dao.getAccounts().map { it.toModel() }
        if (accounts.isEmpty()) return
        for (entity in dao.getTransactions()) {
            val tx = entity.toModel()
            val parsedLabel = tx.rawMessage
                ?.let { TransactionMessageParser.parse(it, tx.timestampMillis)?.bankName }
            val label = parsedLabel ?: tx.smsBankLabel ?: continue
            val resolved = SmsBankKeys.resolveAccountId(label, accounts)
            if (resolved != null) {
                accounts.firstOrNull { it.id == resolved && it.smsMatchKey.isNullOrBlank() }
                    ?.let { updateAccount(it.copy(smsMatchKey = SmsBankKeys.normalize(label))) }
            }
            val next = tx.copy(
                smsBankLabel = label,
                accountId = resolved ?: tx.accountId
            )
            if (next.accountId != tx.accountId || next.smsBankLabel != tx.smsBankLabel) {
                dao.saveTransaction(next.toEntity(tx.id))
            }
        }
    }

    suspend fun addCategory(category: CategoryItem) {
        dao.saveCategory(category.toEntity())
    }

    suspend fun addTransaction(transaction: LedgerTransaction): Long {
        return dao.saveTransaction(transaction.toEntity(id = 0))
    }

    suspend fun updateTransaction(transaction: LedgerTransaction): Long {
        return dao.saveTransaction(transaction.toEntity(id = transaction.id))
    }

    suspend fun addBudget(budget: BudgetPlan): Long {
        return dao.saveBudget(budget.toEntity(id = 0))
    }

    suspend fun saveDraft(draft: DetectedTransactionDraft): Long {
        if (draft.amount <= 0.0) return 0L
        return dao.saveDraft(draft.toEntity(id = 0))
    }

    suspend fun deleteDraft(id: Long) = dao.deleteDraft(id)
    suspend fun deleteTransaction(id: Long) = dao.deleteTransaction(id)
    suspend fun deleteBudget(id: Long) = dao.deleteBudget(id)
    suspend fun deleteAccount(id: Long) = dao.deleteAccount(id)
    suspend fun deleteCustomCategory(id: Long) = dao.deleteCustomCategory(id)

    suspend fun clearAllSavedData() {
        dao.deleteDrafts()
        dao.deleteTransactions()
        dao.deleteBudgets()
        dao.deleteAccounts()
        dao.deleteCustomCategories()
        dao.deleteSettings()
        seedDefaultCategories()
    }

    suspend fun cleanupCreditCardRepaymentArtifacts() {
        dao.getTransactions().forEach { entity ->
            val tx = entity.toModel()
            if (tx.amount <= 0.0 || SmsTransactionNormalizer.isNonLedgerTransactionArtifact(tx.rawMessage, tx.type)) {
                dao.deleteTransaction(tx.id)
            }
        }
        dao.getDrafts().forEach { entity ->
            val draft = entity.toModel()
            if (draft.amount <= 0.0 || SmsTransactionNormalizer.isNonLedgerTransactionArtifact(draft.rawMessage, draft.type)) {
                dao.deleteDraft(draft.id)
            }
        }
    }

    suspend fun exportData(): String {
        val data = AppBackupData(
            settings = dao.getSettings(),
            accounts = dao.getAccounts(),
            categories = dao.getCategories(),
            transactions = dao.getTransactions(),
            budgets = dao.getBudgets()
        )
        return Gson().toJson(data)
    }

    suspend fun importData(jsonString: String) {
        val data = Gson().fromJson(jsonString, AppBackupData::class.java)

        dao.deleteTransactions()
        dao.deleteBudgets()
        dao.deleteAccounts()
        dao.deleteCustomCategories()
        dao.deleteSettings()

        data.settings?.let { dao.saveSettings(it) }
        data.accounts.forEach { dao.saveAccount(it) }
        data.categories.forEach { dao.saveCategory(it) }
        data.transactions.forEach { dao.saveTransaction(it) }
        data.budgets.forEach { dao.saveBudget(it) }
    }

    private suspend fun seedDefaultCategories() {
        val existing = dao.getCategories().toMutableList()
        DefaultCategories.items.forEach { category ->
            if (category.isInvestmentCategoryName()) {
                consolidateInvestmentCategory(existing, category)
                return@forEach
            }

            if (existing.any { it.matchesDefaultCategory(category) }) return@forEach

            val entity = if (existing.none { it.id == category.id }) {
                category.toEntity()
            } else {
                category.copy(id = nextCategoryId(existing)).toEntity()
            }
            dao.seedCategory(entity)
            existing.add(entity)
        }
    }

    private fun nextCategoryId(existing: List<CategoryEntity>): Long {
        val usedIds = existing.map { it.id }.toSet()
        var nextId = (existing.maxOfOrNull { it.id } ?: 0L) + 1L
        while (nextId in usedIds) nextId += 1L
        return nextId
    }

    private suspend fun consolidateInvestmentCategory(
        existing: MutableList<CategoryEntity>,
        defaultCategory: CategoryItem
    ) {
        val investmentCategories = existing.filter { it.isInvestmentCategoryName() }
        val defaultIdHolder = existing.firstOrNull { it.id == defaultCategory.id }
        if (defaultIdHolder != null && !defaultIdHolder.isInvestmentCategoryName()) {
            val relocated = defaultIdHolder.copy(id = nextCategoryId(existing))
            dao.saveCategory(relocated)
            dao.moveTransactionsToCategory(defaultIdHolder.id, relocated.id)
            dao.deleteCategory(defaultIdHolder.id)
            existing[existing.indexOf(defaultIdHolder)] = relocated
        }

        val defaultInvestment = defaultCategory.toEntity()
        dao.saveCategory(defaultInvestment)

        if (existing.none { it.id == defaultInvestment.id }) {
            existing.add(defaultInvestment)
        } else {
            existing[existing.indexOfFirst { it.id == defaultInvestment.id }] = defaultInvestment
        }

        investmentCategories
            .filter { it.id != defaultInvestment.id }
            .forEach { duplicate ->
                dao.moveTransactionsToCategory(duplicate.id, defaultInvestment.id)
                dao.deleteCategory(duplicate.id)
                existing.removeAll { it.id == duplicate.id }
            }
    }

    private fun CategoryEntity.isInvestmentCategoryName(): Boolean {
        val name = name.trim().lowercase()
        return name == "investment" || name == "investments"
    }

    private fun CategoryItem.isInvestmentCategoryName(): Boolean {
        val name = name.trim().lowercase()
        return name == "investment" || name == "investments"
    }

    private fun CategoryEntity.matchesDefaultCategory(category: CategoryItem): Boolean {
        return name.equals(category.name, ignoreCase = true) ||
            iconKey.equals(category.iconKey, ignoreCase = true)
    }
}
