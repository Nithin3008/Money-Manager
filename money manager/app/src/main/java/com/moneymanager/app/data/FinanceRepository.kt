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
        DefaultCategories.items.forEach { dao.seedCategory(it.toEntity()) }
    }
}
