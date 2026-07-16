package com.moneymanager.app.data

import com.google.gson.Gson
import com.moneymanager.app.model.AccountType
import com.moneymanager.app.model.AppBackupData
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.BudgetPlan
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.DefaultCategories
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.TransactionType
import kotlin.math.abs

class FinanceRepository(private val dao: FinanceDao) {
    suspend fun loadState(current: FinanceUiState = FinanceUiState()): FinanceUiState {
        seedDefaultCategories()
        val categories = dao.getCategories().map { it.toModel() }
        val transferCategoryId = categories.transferCategoryId()
        consolidateLegacyBankTransferPairs(transferCategoryId)
        normalizeTransferCategories(transferCategoryId)
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

    suspend fun addAccount(
        name: String,
        balance: Double,
        smsMatchKey: String? = null,
        accountType: AccountType = AccountType.Bank
    ): Long {
        return dao.saveAccount(
            AccountEntity(
                name = name,
                balance = balance,
                smsMatchKey = smsMatchKey?.trim()?.takeIf { it.isNotEmpty() },
                accountType = accountType.name,
                // The typed starting balance is ground truth as of this moment; older
                // transactions are already inside it and must not move the balance.
                balanceAnchorAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateAccount(account: BankAccount) {
        dao.saveAccount(
            AccountEntity(
                id = account.id,
                name = account.name,
                balance = account.balance,
                smsMatchKey = account.smsMatchKey?.trim()?.takeIf { it.isNotEmpty() },
                accountType = account.type.name,
                balanceAnchorAtMillis = account.balanceAnchorAtMillis
            )
        )
    }

    suspend fun remapTransactionAccountsFromSmsLabels() {
        val accounts = dao.getAccounts().map { it.toModel() }
        if (accounts.isEmpty()) return
        for (entity in dao.getTransactions()) {
            val tx = entity.toModel()
            if (tx.type == com.moneymanager.app.model.TransactionType.Transfer) continue
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

    private suspend fun consolidateLegacyBankTransferPairs(transferCategoryId: Long) {
        val accounts = dao.getAccounts().map { it.toModel() }
        val bankAccounts = accounts.filter { it.type == com.moneymanager.app.model.AccountType.Bank }
        if (bankAccounts.size < 2) return

        val transactions = dao.getTransactions()
            .map { it.toModel() }
            .sortedBy { it.timestampMillis }
        val used = mutableSetOf<Long>()

        for (debit in transactions) {
            if (debit.id in used || !debit.isLegacyBankTransferDebitCandidate()) continue
            val fromAccountId = debit.resolveBankAccountId(bankAccounts) ?: continue

            val credit = transactions.firstOrNull { candidate ->
                candidate.id !in used &&
                    candidate.id != debit.id &&
                    candidate.isLegacyBankTransferCreditCandidate() &&
                    sameAmount(candidate.amount, debit.amount) &&
                    abs(candidate.timestampMillis - debit.timestampMillis) <= TRANSFER_PAIR_WINDOW_MS &&
                    candidate.resolveBankAccountId(bankAccounts)?.let { it != fromAccountId } == true &&
                    looksLikeLegacyTransferPair(debit, candidate)
            } ?: continue
            val toAccountId = credit.resolveBankAccountId(bankAccounts) ?: continue
            if (fromAccountId == toAccountId) continue

            val from = bankAccounts.firstOrNull { it.id == fromAccountId } ?: continue
            val to = bankAccounts.firstOrNull { it.id == toAccountId } ?: continue
            val rawMessages = listOfNotNull(debit.rawMessage, credit.rawMessage)
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val smsLabel = listOfNotNull(debit.smsBankLabel, credit.smsBankLabel)
                .distinct()
                .joinToString(" -> ")
                .ifBlank { null }
            val transfer = LedgerTransaction(
                id = 0,
                name = "Transfer: ${from.name} to ${to.name}",
                amount = debit.amount,
                type = TransactionType.Transfer,
                categoryId = transferCategoryId,
                accountId = null,
                timestampMillis = minOf(debit.timestampMillis, credit.timestampMillis),
                isAutoDetected = debit.isAutoDetected || credit.isAutoDetected,
                rawMessage = rawMessages.joinToString(PAIRED_TRANSFER_SMS_DELIMITER),
                smsBankLabel = smsLabel,
                excludeFromSummary = true,
                isCreditCardTransaction = false,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId
            )

            dao.deleteTransaction(debit.id)
            dao.deleteTransaction(credit.id)
            dao.saveTransaction(transfer.toEntity(id = 0))
            used += debit.id
            used += credit.id
        }
    }

    private suspend fun normalizeTransferCategories(transferCategoryId: Long) {
        dao.getTransactions().forEach { entity ->
            val tx = entity.toModel()
            if (tx.type == TransactionType.Transfer && tx.categoryId != transferCategoryId) {
                dao.saveTransaction(tx.copy(categoryId = transferCategoryId, excludeFromSummary = true).toEntity(tx.id))
            }
        }
        dao.getDrafts().forEach { entity ->
            val draft = entity.toModel()
            if (draft.type == TransactionType.Transfer && draft.suggestedCategoryId != transferCategoryId) {
                dao.saveDraft(draft.copy(suggestedCategoryId = transferCategoryId).toEntity(draft.id))
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

            val existingDefault = existing.firstOrNull {
                it.isDefault && it.matchesDefaultCategory(category)
            }
            if (existingDefault != null) {
                val updated = existingDefault.copy(
                    name = category.name,
                    iconKey = category.iconKey,
                    colorHex = category.colorHex
                )
                if (updated != existingDefault) {
                    dao.saveCategory(updated)
                    existing[existing.indexOf(existingDefault)] = updated
                }
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

    private fun List<CategoryItem>.transferCategoryId(): Long {
        return firstOrNull { category ->
            category.name.equals("Transfer", ignoreCase = true) ||
                category.iconKey.equals("transfer", ignoreCase = true)
        }?.id ?: firstOrNull { it.name == "Uncategorized" }?.id ?: 0L
    }

    private fun CategoryEntity.matchesDefaultCategory(category: CategoryItem): Boolean {
        return name.equals(category.name, ignoreCase = true) ||
            iconKey.equals(category.iconKey, ignoreCase = true)
    }

    private fun LedgerTransaction.isLegacyBankTransferDebitCandidate(): Boolean {
        return type == TransactionType.Expense &&
            amount > 0.0 &&
            !isCreditCardTransaction &&
            fromAccountId == null &&
            toAccountId == null
    }

    private fun LedgerTransaction.isLegacyBankTransferCreditCandidate(): Boolean {
        return type == TransactionType.Income &&
            amount > 0.0 &&
            !isCreditCardTransaction &&
            fromAccountId == null &&
            toAccountId == null
    }

    private fun LedgerTransaction.resolveBankAccountId(accounts: List<BankAccount>): Long? {
        accountId?.let { existing ->
            if (accounts.any { it.id == existing }) return existing
        }
        val parsedLabel = rawMessage?.let { TransactionMessageParser.parse(it, timestampMillis)?.bankName }
        return SmsBankKeys.resolveAccountId(parsedLabel ?: smsBankLabel, accounts)
    }

    private fun looksLikeLegacyTransferPair(debit: LedgerTransaction, credit: LedgerTransaction): Boolean {
        val combined = "${debit.rawMessage.orEmpty()} ${credit.rawMessage.orEmpty()}".lowercase()
        return listOf(
            "transfer",
            "neft",
            "rtgs",
            "imps",
            "upi",
            "utr",
            "own account",
            "own a/c",
            "self",
            "credited to",
            "debited from"
        ).any { it in combined }
    }

    private fun sameAmount(a: Double, b: Double): Boolean = abs(a - b) <= AMOUNT_EPSILON

    private companion object {
        const val PAIRED_TRANSFER_SMS_DELIMITER = "\n--- paired transfer sms ---\n"
        const val TRANSFER_PAIR_WINDOW_MS = 8 * 60 * 1000L
        const val AMOUNT_EPSILON = 0.02
    }
}
