package com.moneymanager.app.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory stand-in for the Room DAO so the export/import path runs on the JVM. */
private class FakeFinanceDao : FinanceDao {
    var settings: UserSettingsEntity? = null
    val accounts = LinkedHashMap<Long, AccountEntity>()
    val categories = LinkedHashMap<Long, CategoryEntity>()
    val transactions = LinkedHashMap<Long, TransactionEntity>()
    val budgets = LinkedHashMap<Long, BudgetEntity>()
    val drafts = LinkedHashMap<Long, DetectedDraftEntity>()
    private var nextAccountId = 1L
    private var nextTransactionId = 1L
    private var nextBudgetId = 1L
    private var nextDraftId = 1L

    override suspend fun saveSettings(settings: UserSettingsEntity) { this.settings = settings }
    override suspend fun getSettings(): UserSettingsEntity? = settings

    override suspend fun saveAccount(account: AccountEntity): Long {
        val id = if (account.id == 0L) nextAccountId++ else account.id
        if (account.id != 0L && account.id >= nextAccountId) nextAccountId = account.id + 1
        accounts[id] = account.copy(id = id)
        return id
    }
    override suspend fun deleteAccount(id: Long) { accounts.remove(id) }
    override suspend fun getAccounts(): List<AccountEntity> = accounts.values.sortedBy { it.id }

    override suspend fun saveCategory(category: CategoryEntity) { categories[category.id] = category }
    override suspend fun seedCategory(category: CategoryEntity) { categories.putIfAbsent(category.id, category) }
    override suspend fun deleteCustomCategory(id: Long) {
        categories[id]?.takeIf { !it.isDefault }?.let { categories.remove(id) }
    }
    override suspend fun deleteCategory(id: Long) { categories.remove(id) }
    override suspend fun moveTransactionsToCategory(fromCategoryId: Long, toCategoryId: Long) {
        transactions.replaceAll { _, t -> if (t.categoryId == fromCategoryId) t.copy(categoryId = toCategoryId) else t }
    }
    override suspend fun moveSecondaryCategory(fromCategoryId: Long, toCategoryId: Long) {
        transactions.replaceAll { _, t -> if (t.secondaryCategoryId == fromCategoryId) t.copy(secondaryCategoryId = toCategoryId) else t }
    }
    override suspend fun clearSecondaryCategory(categoryId: Long) {
        transactions.replaceAll { _, t -> if (t.secondaryCategoryId == categoryId) t.copy(secondaryCategoryId = null) else t }
    }
    override suspend fun getCategories(): List<CategoryEntity> =
        categories.values.sortedWith(compareByDescending<CategoryEntity> { it.isDefault }.thenBy { it.id })

    override suspend fun saveTransaction(transaction: TransactionEntity): Long {
        val id = if (transaction.id == 0L) nextTransactionId++ else transaction.id
        if (transaction.id != 0L && transaction.id >= nextTransactionId) nextTransactionId = transaction.id + 1
        transactions[id] = transaction.copy(id = id)
        return id
    }
    override suspend fun deleteTransaction(id: Long) { transactions.remove(id) }
    override suspend fun getTransactions(): List<TransactionEntity> =
        transactions.values.sortedByDescending { it.timestampMillis }

    override suspend fun saveBudget(budget: BudgetEntity): Long {
        val id = if (budget.id == 0L) nextBudgetId++ else budget.id
        if (budget.id != 0L && budget.id >= nextBudgetId) nextBudgetId = budget.id + 1
        budgets[id] = budget.copy(id = id)
        return id
    }
    override suspend fun deleteBudget(id: Long) { budgets.remove(id) }
    override suspend fun getBudgets(): List<BudgetEntity> = budgets.values.sortedBy { it.id }

    override suspend fun saveDraft(draft: DetectedDraftEntity): Long {
        val id = if (draft.id == 0L) nextDraftId++ else draft.id
        drafts[id] = draft.copy(id = id)
        return id
    }
    override suspend fun deleteDraft(id: Long) { drafts.remove(id) }
    override suspend fun getDrafts(): List<DetectedDraftEntity> = drafts.values.sortedByDescending { it.detectedAtMillis }

    override suspend fun deleteSettings() { settings = null }
    override suspend fun deleteAccounts() { accounts.clear() }
    override suspend fun deleteTransactions() { transactions.clear() }
    override suspend fun deleteBudgets() { budgets.clear() }
    override suspend fun deleteDrafts() { drafts.clear() }
    override suspend fun deleteCustomCategories() {
        categories.values.filter { !it.isDefault }.forEach { categories.remove(it.id) }
    }
}

class BackupRestoreRoundTripTest {

    private fun monthsAgo(months: Long): Long =
        System.currentTimeMillis() - months * 30L * 24 * 60 * 60 * 1000

    /** Builds a device with ~4 months of realistic data: banks, a credit card with linked
     *  cards + anchor balance, custom categories, categorized + CC-tagged transactions. */
    private suspend fun populateSourceDevice(dao: FakeFinanceDao, repo: FinanceRepository) {
        repo.loadState() // seeds default categories, like a real first launch

        dao.saveSettings(
            UserSettingsEntity(
                userName = "Nithin",
                currencyCode = "INR",
                themeMode = "Dark",
                bankSmsSetupCompleted = true,
                onboardedAtMillis = monthsAgo(4),
                lastSuccessfulScanMillis = monthsAgo(0),
                defaultAccountId = 1L,
                dismissedSmsKeys = "key-a\nkey-b",
                paletteHexCsv = "#FF0000,#00FF00"
            )
        )

        val bankId = dao.saveAccount(
            AccountEntity(
                name = "HDFC Bank", balance = 52341.50, smsMatchKey = "hdfc",
                accountType = "Bank", balanceAnchorAtMillis = monthsAgo(4)
            )
        )
        val ccId = dao.saveAccount(
            AccountEntity(
                name = "ICICI Credit Card", balance = 8500.0, smsMatchKey = "icici cc",
                accountType = "CreditCard", balanceAnchorAtMillis = monthsAgo(4),
                linkedCardsCsv = "1234,5678"
            )
        )

        val defaultCategories = dao.getCategories()
        val customId = (defaultCategories.maxOf { it.id } + 1)
        dao.saveCategory(CategoryEntity(id = customId, name = "Pets", iconKey = "paid", isDefault = false, colorHex = "#AA66CC"))
        val shoppingLike = defaultCategories.first()

        // ~4 months of transactions, oldest to newest
        for (m in 4 downTo 1) {
            dao.saveTransaction(
                TransactionEntity(
                    name = "Salary", amount = 90000.0, type = "Income",
                    categoryId = shoppingLike.id, accountId = bankId,
                    timestampMillis = monthsAgo(m.toLong()), isAutoDetected = true,
                    rawMessage = "Credited INR 90000 to a/c", smsBankLabel = "HDFC"
                )
            )
            dao.saveTransaction(
                TransactionEntity(
                    name = "Amazon", amount = 2499.0, type = "Expense",
                    categoryId = shoppingLike.id, accountId = ccId,
                    timestampMillis = monthsAgo(m.toLong()) + 1000, isAutoDetected = true,
                    rawMessage = "Spent INR 2499 on card 1234", smsBankLabel = "ICICI",
                    isCreditCardTransaction = true, secondaryCategoryId = customId
                )
            )
            dao.saveTransaction(
                TransactionEntity(
                    name = "Vet visit", amount = 1200.0, type = "Expense",
                    categoryId = customId, accountId = bankId,
                    timestampMillis = monthsAgo(m.toLong()) + 2000, isAutoDetected = false,
                    rawMessage = null
                )
            )
        }

        dao.saveBudget(BudgetEntity(name = "Shopping cap", limitAmount = 10000.0, categoryIdsCsv = "$customId", month = "2026-07"))

        dao.saveDraft(
            DetectedDraftEntity(
                bankName = "HDFC", name = "Swiggy", amount = 350.0, type = "Expense",
                counterparty = "swiggy", rawMessage = "Debited INR 350",
                suggestedCategoryId = shoppingLike.id,
                detectedAtMillis = monthsAgo(0), transactionTimestampMillis = monthsAgo(0)
            )
        )
    }

    @Test
    fun `full backup restores onto a fresh install`() = runBlocking {
        val sourceDao = FakeFinanceDao()
        val sourceRepo = FinanceRepository(sourceDao)
        populateSourceDevice(sourceDao, sourceRepo)

        val json = sourceRepo.exportData()
        assertTrue("export should not be blank", json.isNotBlank())

        // Fresh install: defaults seeded, user completed registration with a throwaway account
        val freshDao = FakeFinanceDao()
        val freshRepo = FinanceRepository(freshDao)
        freshRepo.loadState()
        freshDao.saveAccount(AccountEntity(name = "Temp bank", balance = 0.0, balanceAnchorAtMillis = System.currentTimeMillis()))
        freshDao.saveSettings(UserSettingsEntity(userName = "Temp", currencyCode = "INR", themeMode = "Dark"))

        freshRepo.importData(json)
        val restored = freshRepo.loadState()

        // Settings
        assertEquals("Nithin", restored.userName)
        assertEquals(1L, freshDao.settings?.defaultAccountId)
        assertEquals(sourceDao.settings?.dismissedSmsKeys, freshDao.settings?.dismissedSmsKeys)
        assertEquals(sourceDao.settings?.lastSuccessfulScanMillis, freshDao.settings?.lastSuccessfulScanMillis)

        // Accounts, incl. anchor balance and linked card numbers
        assertEquals(sourceDao.getAccounts(), freshDao.getAccounts())
        val cc = freshDao.getAccounts().first { it.accountType == "CreditCard" }
        assertEquals("1234,5678", cc.linkedCardsCsv)
        assertTrue(cc.balanceAnchorAtMillis > 0)

        // Categories: defaults + the custom one survive with same ids
        assertEquals(
            sourceDao.getCategories().map { it.id to it.name },
            freshDao.getCategories().map { it.id to it.name }
        )
        assertNotNull(freshDao.getCategories().firstOrNull { it.name == "Pets" && !it.isDefault })

        // Transactions with category + secondary category + CC flag intact
        assertEquals(sourceDao.getTransactions(), freshDao.getTransactions())

        // Budgets and pending drafts
        assertEquals(sourceDao.getBudgets(), freshDao.getBudgets())
        assertEquals(sourceDao.getDrafts(), freshDao.getDrafts())
    }

    @Test
    fun `import tolerates trailing garbage from a non-truncated overwrite`() = runBlocking {
        val sourceDao = FakeFinanceDao()
        val sourceRepo = FinanceRepository(sourceDao)
        populateSourceDevice(sourceDao, sourceRepo)
        // Simulate an old, longer backup left behind after the file was rewritten in "w" mode.
        val json = sourceRepo.exportData() + """{"stale":"tail of the previous, longer backup"}]}"""

        val freshDao = FakeFinanceDao()
        val freshRepo = FinanceRepository(freshDao)
        freshRepo.loadState()
        freshRepo.importData(json)

        assertEquals(sourceDao.getTransactions(), freshDao.getTransactions())
        assertEquals(sourceDao.getAccounts(), freshDao.getAccounts())
    }

    @Test
    fun `import rejects a wrong file without wiping existing data`() = runBlocking {
        val dao = FakeFinanceDao()
        val repo = FinanceRepository(dao)
        populateSourceDevice(dao, repo)
        val transactionsBefore = dao.getTransactions()
        val accountsBefore = dao.getAccounts()

        for (badFile in listOf("not json at all", "{}", """{"foo": [1,2,3]}""")) {
            try {
                repo.importData(badFile)
                throw AssertionError("import should have rejected: $badFile")
            } catch (e: Exception) {
                // expected
            }
            assertEquals("data must survive a failed import of: $badFile", transactionsBefore, dao.getTransactions())
            assertEquals(accountsBefore, dao.getAccounts())
        }
    }
}
