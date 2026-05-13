package com.moneymanager.app.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Dining
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.YearMonth

private const val TRANSACTIONS_PER_PAGE = 10

enum class TransactionType {
    Income,
    Expense
}

enum class MessageScanRange(val label: String) {
    Today("Today"),
    Yesterday("Yesterday"),
    Week("Last 7 Days"),
    Custom("Custom Range")
}

enum class ActivityDateFilter(val label: String) {
    Today("Today"),
    Week("Last 7 Days"),
    Month("This Month"),
    Custom("Custom")
}

enum class ScreenTab(val label: String, val icon: ImageVector) {
    Dashboard("Today", Icons.Rounded.GridView),
    Activity("Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    Budget("Budget", Icons.Rounded.PieChart),
    Summary("Summary", Icons.Rounded.BarChart),
    Settings("Settings", Icons.Rounded.Settings)
}

enum class CurrencyOption(
    val label: String,
    val currencyCode: String,
    val symbol: String
) {
    INR("Indian Rupee", "INR", "Rs"),
    USD("US Dollar", "USD", "$"),
    EUR("Euro", "EUR", "EUR"),
    GBP("British Pound", "GBP", "GBP")
}

enum class ThemeMode(val label: String) {
    Dark("Dark"),
    Light("Light")
}

enum class UiAccent(
    val label: String,
    val darkHex: String,
    val lightHex: String,
    val softDarkHex: String,
    val softLightHex: String
) {
    Sky("Sky", "#7C9DFF", "#345CA8", "#AFC6FF", "#1F65C8"),
    Mint("Mint", "#61E6A4", "#007A52", "#B8FFD7", "#006C49"),
    Rose("Rose", "#FF7A8A", "#B32648", "#FFD9DE", "#A3193D"),
    Amber("Amber", "#FFD166", "#8A5600", "#FFE2A3", "#7A4B00"),
    Violet("Violet", "#B589FF", "#6750A4", "#D8C2FF", "#5B45A0"),
    Cyan("Cyan", "#38D5E8", "#006878", "#B3EBF4", "#005E6C"),
    Coral("Coral", "#FF9F7A", "#A33D22", "#FFD7C7", "#91341B"),
    Pink("Pink", "#F472B6", "#9D2868", "#FFD7EC", "#8A1F5B"),
    Emerald("Emerald", "#4ADE80", "#167A3A", "#C4FBD3", "#0D6F31"),
    Indigo("Indigo", "#818CF8", "#3F51B5", "#DCE1FF", "#3342A2"),
    Teal("Teal", "#2DD4BF", "#00796B", "#B6F1E8", "#006D60"),
    Slate("Slate", "#A7B3CC", "#526178", "#D9E2F2", "#445268")
}

enum class UiSurface(
    val label: String,
    val darkBackgroundHex: String,
    val darkCardHex: String,
    val darkPanelHex: String,
    val darkChipHex: String,
    val lightBackgroundHex: String,
    val lightCardHex: String,
    val lightPanelHex: String,
    val lightChipHex: String
) {
    Midnight("Midnight", "#0C0F17", "#111620", "#171D2A", "#202737", "#F8F9FF", "#FFFFFF", "#F0F4FF", "#E3EAF8"),
    Graphite("Graphite", "#101113", "#17191D", "#20232A", "#2A2E37", "#F7F7F5", "#FFFFFF", "#EEEEEA", "#E2E3DE"),
    Ocean("Ocean", "#07131D", "#0D1C2A", "#14293B", "#1B354A", "#F5FAFF", "#FFFFFF", "#EAF3FB", "#DDECF7"),
    Plum("Plum", "#160F1D", "#21172A", "#2B1F37", "#362844", "#FCF7FF", "#FFFFFF", "#F4ECFA", "#EADDF3"),
    Forest("Forest", "#0B1510", "#111F18", "#17291F", "#203529", "#F7FBF6", "#FFFFFF", "#ECF5EA", "#DDEBDA"),
    Warm("Warm", "#17130E", "#211A13", "#2B2218", "#382B1F", "#FFFAF5", "#FFFFFF", "#F7EFE6", "#EDE1D4")
}

data class BankAccount(
    val id: Long,
    val name: String,
    val balance: Double,
    val smsMatchKey: String? = null
)

data class CategoryItem(
    val id: Long,
    val name: String,
    val iconKey: String,
    val icon: ImageVector,
    val isDefault: Boolean,
    val colorHex: String
)

data class LedgerTransaction(
    val id: Long,
    val name: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val accountId: Long?,
    val timestampMillis: Long,
    val isAutoDetected: Boolean = false,
    val rawMessage: String? = null,
    val smsBankLabel: String? = null,
    val excludeFromSummary: Boolean = false,
    val isCreditCardTransaction: Boolean = false
)

data class BudgetPlan(
    val id: Long,
    val name: String,
    val limitAmount: Double,
    val categoryIds: Set<Long>,
    val month: YearMonth
)

data class DetectedTransactionDraft(
    val id: Long,
    val bankName: String,
    val name: String,
    val amount: Double,
    val type: TransactionType,
    val counterparty: String,
    val rawMessage: String,
    val suggestedCategoryId: Long?,
    val detectedAtMillis: Long,
    val transactionTimestampMillis: Long
)

data class BudgetWarning(
    val budgetName: String,
    val limitAmount: Double,
    val spentAmount: Double
)

data class MonthlyCategoryTotal(
    val category: CategoryItem,
    val income: Double,
    val expense: Double
)

data class FinanceUiState(
    val isAppInitializing: Boolean = true,
    val userName: String = "",
    val currency: CurrencyOption = CurrencyOption.INR,
    val themeMode: ThemeMode = ThemeMode.Dark,
    val uiAccent: UiAccent = UiAccent.Sky,
    val uiSurface: UiSurface = UiSurface.Midnight,
    val selectedTab: ScreenTab = ScreenTab.Dashboard,
    val selectedMonth: YearMonth = YearMonth.now(),
    val accounts: List<BankAccount> = emptyList(),
    val categories: List<CategoryItem> = DefaultCategories.items,
    val transactions: List<LedgerTransaction> = emptyList(),
    val budgets: List<BudgetPlan> = emptyList(),
    val detectedDrafts: List<DetectedTransactionDraft> = emptyList(),
    val activityDateFilter: ActivityDateFilter = ActivityDateFilter.Today,
    val activityStartDate: java.time.LocalDate = java.time.LocalDate.now(),
    val activityEndDate: java.time.LocalDate = java.time.LocalDate.now(),
    val scanStatusMessage: String = "",
    val dashboardDraftPage: Int = 1,
    val dashboardTransactionPage: Int = 1,
    val activityTransactionPage: Int = 1,
    val isScanningMessages: Boolean = false,
    val showTransactionSheet: Boolean = false,
    val showBudgetSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showEditCategorySheet: Boolean = false,
    val editingTransactionId: Long? = null,
    val showTransactionDetailSheet: Boolean = false,
    val selectedTransactionId: Long? = null,
    val budgetWarning: BudgetWarning? = null,
    val salaryShiftIncomeEnabled: Boolean = false,
    val salaryShiftWindowDays: Int = 5,
    val salaryCategoryId: Long? = null,
    val salaryKeywordsForUncategorized: Boolean = true,
    val bankSmsSetupCompleted: Boolean = false,
    val defaultAccountId: Long? = null,
    /** Empty = all accounts on Summary; otherwise filter to these account ids. */
    val summarySelectedAccountIds: Set<Long> = emptySet(),
    /** Distinct SMS bank labels seen in scans/transactions; not persisted. */
    val discoveredSmsBanks: List<String> = emptyList()
) {
    val hasCompletedRegistration: Boolean = userName.isNotBlank()

    private fun passesSummaryAccountFilter(tx: LedgerTransaction): Boolean {
        val activeIds = activeSummaryAccountIds
        if (activeIds.isEmpty()) return true
        val aid = tx.accountId ?: return false
        return aid in activeIds
    }

    val activeSummaryAccountIds: Set<Long>
        get() = when {
            summarySelectedAccountIds.isNotEmpty() -> summarySelectedAccountIds
            defaultAccountId != null && accounts.any { it.id == defaultAccountId } -> setOf(defaultAccountId)
            else -> emptySet()
        }

    private val selectedSummaryAccounts: List<BankAccount>
        get() = if (activeSummaryAccountIds.isEmpty()) {
            accounts
        } else {
            accounts.filter { it.id in activeSummaryAccountIds }
        }

    private val summaryBalanceAnchor: Double
        get() = selectedSummaryAccounts.sumOf { it.balance }

    private val summaryBalanceTransactions: List<LedgerTransaction>
        get() = transactions.filter { tx ->
            passesSummaryAccountFilter(tx)
        }

    private fun signedMovement(tx: LedgerTransaction): Double {
        return if (tx.type == TransactionType.Income) tx.amount else -tx.amount
    }

    private fun balanceBeforeDate(cutoff: LocalDate): Double {
        val movementFromCutoffToNow = summaryBalanceTransactions
            .filter { !it.transactionDate().isBefore(cutoff) }
            .sumOf(::signedMovement)
        return summaryBalanceAnchor - movementFromCutoffToNow
    }

    /** Transactions included in Summary metrics for [selectedMonth], matching calendar-month bank statements. */
    val monthTransactions: List<LedgerTransaction>
        get() {
            val monthStart = selectedMonth.atDay(1)
            val monthEnd = selectedMonth.atEndOfMonth()
            return transactions.filter { tx ->
                if (tx.excludeFromSummary) return@filter false
                if (!passesSummaryAccountFilter(tx)) return@filter false
                val d = tx.transactionDate()
                !d.isBefore(monthStart) && !d.isAfter(monthEnd)
            }
        }

    val monthExpenseTransactions: List<LedgerTransaction>
        get() = monthTransactions.filter { it.type == TransactionType.Expense }

    val monthIncomeTransactions: List<LedgerTransaction>
        get() = monthTransactions.filter { it.type == TransactionType.Income }

    private val uncategorizedId: Long?
        get() = categories.firstOrNull { it.name == "Uncategorized" }?.id

    private fun incomeCountsAsSalary(tx: LedgerTransaction): Boolean {
        if (tx.type != TransactionType.Income) return false
        val salaryId = salaryCategoryId
        if (salaryId != null && tx.categoryId == salaryId) return true
        if (!salaryKeywordsForUncategorized) return false
        val uncId = uncategorizedId ?: return false
        if (tx.categoryId != uncId) return false
        return SalaryIncomeRules.matchesSalaryKeywords(tx.name, tx.rawMessage)
    }

    val monthSalaryIncome: Double
        get() = monthIncomeTransactions
            .filter { incomeCountsAsSalary(it) }
            .sumOf { it.amount }

    val monthOtherIncome: Double
        get() = monthIncomeTransactions
            .filter { !incomeCountsAsSalary(it) }
            .sumOf { it.amount }

    val monthExpense: Double
        get() = monthExpenseTransactions
            .sumOf { it.amount }

    val monthNet: Double
        get() = monthIncome - monthExpense

    val monthIncome: Double
        get() = monthSalaryIncome + monthOtherIncome

    val monthReportIncome: Double
        get() = monthIncome

    val monthReportNet: Double
        get() = monthReportIncome - monthExpense

    /** Actual credits dated inside [selectedMonth] (calendar), for comparison when payroll shift moves income. */
    val calendarMonthIncomeTotal: Double
        get() {
            val monthStart = selectedMonth.atDay(1)
            val monthEnd = selectedMonth.atEndOfMonth()
            return summaryBalanceTransactions
                .filter { tx ->
                    if (tx.excludeFromSummary) return@filter false
                    if (tx.type != TransactionType.Income) return@filter false
                    val d = tx.transactionDate()
                    !d.isBefore(monthStart) && !d.isAfter(monthEnd)
                }
                .sumOf { it.amount }
        }

    val selectedMonthOpeningBalance: Double
        get() = balanceAtStartOfSelectedMonth

    val selectedMonthSalaryIncome: Double
        get() = monthSalaryIncome

    /** Current user-entered account balance, used as the anchor for reverse reconstruction. */
    val currentBalanceAnchor: Double
        get() = summaryBalanceAnchor

    /** Cash balance before any transaction dated in [selectedMonth] (calendar), reconstructed from current balance. */
    val balanceAtStartOfSelectedMonth: Double
        get() = balanceBeforeDate(selectedMonth.atDay(1))

    /** Cash balance after all transactions through the last day of [selectedMonth], reconstructed from current balance. */
    val balanceAtEndOfSelectedMonth: Double
        get() = balanceBeforeDate(selectedMonth.plusMonths(1).atDay(1))

    /** Actual calendar cashflow for the selected month. This should explain opening to closing balance. */
    val calendarMonthNet: Double
        get() {
            val monthStart = selectedMonth.atDay(1)
            val monthEnd = selectedMonth.atEndOfMonth()
            return summaryBalanceTransactions
                .filter { tx ->
                    val d = tx.transactionDate()
                    !d.isBefore(monthStart) && !d.isAfter(monthEnd)
                }
                .sumOf(::signedMovement)
        }

    /** Difference between reconstructed closing balance and calendar cashflow math; non-zero means missing/excluded data. */
    val selectedMonthReconciliationGap: Double
        get() = balanceAtEndOfSelectedMonth - (balanceAtStartOfSelectedMonth + calendarMonthNet)

    val trackedBalance: Double
        get() = accounts.sumOf { it.balance }

    val activeBudgets: List<BudgetPlan>
        get() = budgets.filter { it.month == selectedMonth }

    val todayTransactions: List<LedgerTransaction>
        get() = transactions.filter {
            val activeIds = activeSummaryAccountIds
            it.transactionDate() == java.time.LocalDate.now() &&
                (activeIds.isEmpty() ||
                    it.accountId in activeIds)
        }

    val todayDetectedDrafts: List<DetectedTransactionDraft>
        get() = detectedDrafts.filter { it.transactionDate() == java.time.LocalDate.now() }

    val activityTransactions: List<LedgerTransaction>
        get() = transactions.filter {
            val date = it.transactionDate()
            !date.isBefore(activityStartDate) && !date.isAfter(activityEndDate)
        }

    val dashboardTransactionPageCount: Int
        get() = ((todayTransactions.size + TRANSACTIONS_PER_PAGE - 1) / TRANSACTIONS_PER_PAGE)
            .coerceAtLeast(1)

    val dashboardCurrentPage: Int
        get() = dashboardTransactionPage.coerceIn(1, dashboardTransactionPageCount)

    val dashboardPagedTransactions: List<LedgerTransaction>
        get() = todayTransactions
            .drop((dashboardCurrentPage - 1) * TRANSACTIONS_PER_PAGE)
            .take(TRANSACTIONS_PER_PAGE)

    val dashboardDraftPageCount: Int
        get() = ((todayDetectedDrafts.size + TRANSACTIONS_PER_PAGE - 1) / TRANSACTIONS_PER_PAGE)
            .coerceAtLeast(1)

    val dashboardCurrentDraftPage: Int
        get() = dashboardDraftPage.coerceIn(1, dashboardDraftPageCount)

    val dashboardPagedDrafts: List<DetectedTransactionDraft>
        get() = todayDetectedDrafts
            .drop((dashboardCurrentDraftPage - 1) * TRANSACTIONS_PER_PAGE)
            .take(TRANSACTIONS_PER_PAGE)

    val pagedTransactions: List<LedgerTransaction>
        get() = activityTransactions.take((activityTransactionPage.coerceAtLeast(1)) * TRANSACTIONS_PER_PAGE)

    val hasMoreTransactions: Boolean
        get() = pagedTransactions.size < activityTransactions.size
}

object DefaultCategories {
    val items = listOf(
        CategoryItem(0, "Uncategorized", "category", Icons.Rounded.Category, true, "#8F95A3"),
        CategoryItem(1, "Grocery", "grocery", Icons.Rounded.LocalGroceryStore, true, "#38E68B"),
        CategoryItem(2, "Food", "food", Icons.Rounded.Dining, true, "#FFC857"),
        CategoryItem(3, "Shopping", "shopping", Icons.Rounded.ShoppingBag, true, "#FF4FB8"),
        CategoryItem(4, "Fuel", "fuel", Icons.Rounded.LocalGasStation, true, "#FF8A3D"),
        CategoryItem(5, "Rent", "rent", Icons.Rounded.Home, true, "#FF6B7A")
    )
}

object MoneyIcons {
    data class CategoryIconOption(val key: String, val label: String, val icon: ImageVector)

    val Account: ImageVector = Icons.Rounded.AccountBalance
    val Category: ImageVector = Icons.Rounded.Category
    val Transport: ImageVector = Icons.Rounded.DirectionsCar

    val allCategoryIcons = listOf(
        CategoryIconOption("category", "General", Icons.Rounded.Category),
        CategoryIconOption("grocery", "Grocery", Icons.Rounded.LocalGroceryStore),
        CategoryIconOption("food", "Food", Icons.Rounded.Dining),
        CategoryIconOption("coffee", "Coffee", Icons.Rounded.LocalCafe),
        CategoryIconOption("shopping", "Shopping", Icons.Rounded.ShoppingBag),
        CategoryIconOption("fuel", "Fuel", Icons.Rounded.LocalGasStation),
        CategoryIconOption("rent", "Home", Icons.Rounded.Home),
        CategoryIconOption("gym", "Gym", Icons.Rounded.FitnessCenter),
        CategoryIconOption("transport", "Transport", Icons.Rounded.DirectionsCar),
        CategoryIconOption("work", "Work", Icons.Rounded.Work),
        CategoryIconOption("account", "Bank", Icons.Rounded.AccountBalance),
        CategoryIconOption("health", "Health", Icons.Rounded.LocalHospital),
        CategoryIconOption("education", "Study", Icons.Rounded.School),
        CategoryIconOption("travel", "Travel", Icons.Rounded.Flight),
        CategoryIconOption("world", "International", Icons.Rounded.Public),
        CategoryIconOption("investment", "Investment", Icons.AutoMirrored.Rounded.TrendingUp),
        CategoryIconOption("bills", "Bills", Icons.Rounded.Payments),
        CategoryIconOption("utilities", "Utilities", Icons.Rounded.ElectricBolt),
        CategoryIconOption("games", "Games", Icons.Rounded.SportsEsports),
        CategoryIconOption("movie", "Movies", Icons.Rounded.Movie),
        CategoryIconOption("music", "Music", Icons.Rounded.MusicNote),
        CategoryIconOption("subscription", "Subscriptions", Icons.Rounded.Subscriptions),
        CategoryIconOption("pets", "Pets", Icons.Rounded.Pets),
        CategoryIconOption("social", "Social", Icons.Rounded.Forum),
        CategoryIconOption("care", "Care", Icons.Rounded.Favorite)
    )

    val frequentCategoryIcons = listOf(
        "category",
        "food",
        "shopping",
        "fuel",
        "rent",
        "gym",
        "transport",
        "bills"
    ).mapNotNull { key -> allCategoryIcons.firstOrNull { it.key == key } }

    private val iconByKey = allCategoryIcons.associate { it.key to it.icon }

    fun resolveCategoryIcon(key: String): ImageVector = iconByKey[key] ?: Category
}
