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
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.YearMonth

private const val TRANSACTIONS_PER_PAGE = 10

enum class TransactionType {
    Income,
    Expense,
    Transfer
}

enum class AccountType(val label: String) {
    Bank("Bank"),
    CreditCard("Credit Card")
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
    Dashboard("Home", Icons.Rounded.GridView),
    Activity("Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    Budget("Budget", Icons.Rounded.PieChart),
    Summary("Reports", Icons.Rounded.BarChart),
    Settings("Profile", Icons.Rounded.Person)
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
    val softLightHex: String,
    val onDarkHex: String,
    val onLightHex: String
) {
    Lime("Lime", "#B4F077", "#4E7D1C", "#D3F7AD", "#3F6516", "#16220A", "#F4FBEA"),
    Sky("Metallic Blue", "#7EA2FF", "#2F5FD0", "#B8C7FF", "#274FAD", "#0B1633", "#F2F6FF"),
    Mint("Cool Teal", "#3FE0C4", "#087F7A", "#A7F3E6", "#0B6F6A", "#05261F", "#EFFCF9"),
    Rose("Raspberry", "#FF6FAE", "#C0266D", "#FFD1E6", "#A01C58", "#330E20", "#FFF1F7"),
    Amber("Goldenrod", "#F5C542", "#A16207", "#FFE7A3", "#854D0E", "#33270A", "#FFFBEF"),
    Violet("Amethyst", "#B794F6", "#6D28D9", "#E9D5FF", "#5B21B6", "#1E0E33", "#F8F3FF"),
    Cyan("Aqua Lapis", "#35D6E7", "#0277BD", "#B8F3FA", "#0369A1", "#06262E", "#EFFBFE"),
    Coral("Terracotta", "#FF8A65", "#B45335", "#FFD6C8", "#93452C", "#331507", "#FFF4F0"),
    Pink("Salmon Pop", "#FF7A90", "#BE3A57", "#FFD0DA", "#A62D49", "#330D14", "#FFF1F4"),
    Emerald("Modern Myrtle", "#48D6A5", "#087C64", "#BDEEDC", "#066B56", "#05291F", "#EFFCF7"),
    Indigo("Electric Blue", "#5BC0FF", "#1D4ED8", "#BFDBFE", "#1E40AF", "#072033", "#F0F7FF"),
    Teal("Jade Energy", "#54D17A", "#138A4E", "#C8F6D4", "#0F7A43", "#06220F", "#F1FBF4"),
    Slate("Ruby Slate", "#F0627D", "#9F1239", "#FFD0DB", "#881337", "#330913", "#FFF1F4")
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
    Midnight("Midnight", "#141414", "#1A1A1A", "#202020", "#2A2A2A", "#FAFAFA", "#FFFFFF", "#F3F4F6", "#E8EAEE"),
    Graphite("Graphite", "#101113", "#17191D", "#20232A", "#2A2E37", "#F7F8FA", "#FFFFFF", "#F0F2F5", "#E4E7EC"),
    Ocean("Ocean", "#07131D", "#0D1C2A", "#14293B", "#1B354A", "#F5FAFF", "#FFFFFF", "#EAF3FB", "#DDECF7"),
    Plum("Plum", "#160F1D", "#21172A", "#2B1F37", "#362844", "#FCF7FF", "#FFFFFF", "#F4ECFA", "#EADDF3"),
    Forest("Forest", "#0B1510", "#111F18", "#17291F", "#203529", "#F7FBF6", "#FFFFFF", "#ECF5EA", "#DDEBDA"),
    Warm("Warm", "#17130E", "#211A13", "#2B2218", "#382B1F", "#FFFAF5", "#FFFFFF", "#F7EFE6", "#EDE1D4")
}

@Immutable
data class BankAccount(
    val id: Long,
    val name: String,
    val balance: Double,
    val smsMatchKey: String? = null,
    val type: AccountType = AccountType.Bank,
    /** When this account's balance was last set as ground truth; older transactions never move it. */
    val balanceAnchorAtMillis: Long = 0L
)

data class RegistrationAccountInput(
    val name: String,
    val balance: Double,
    val type: AccountType
)

@Immutable
data class CategoryItem(
    val id: Long,
    val name: String,
    val iconKey: String,
    val icon: ImageVector,
    val isDefault: Boolean,
    val colorHex: String
)

@Immutable
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
    val isCreditCardTransaction: Boolean = false,
    val description: String? = null,
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null
)

@Immutable
data class BudgetPlan(
    val id: Long,
    val name: String,
    val limitAmount: Double,
    val categoryIds: Set<Long>,
    val month: YearMonth
)

@Immutable
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
    val transactionTimestampMillis: Long,
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null
)

@Immutable
data class BudgetWarning(
    val budgetName: String,
    val limitAmount: Double,
    val spentAmount: Double
)

@Immutable
data class MonthlyCategoryTotal(
    val category: CategoryItem,
    val income: Double,
    val expense: Double
)

@Immutable
data class FinanceUiState(
    val isAppInitializing: Boolean = true,
    val userName: String = "",
    val currency: CurrencyOption = CurrencyOption.INR,
    val themeMode: ThemeMode = ThemeMode.Dark,
    val uiAccent: UiAccent = UiAccent.Lime,
    /** When set, a user-picked custom accent hex (e.g. "#7EA2FF") that overrides [uiAccent]. */
    val customAccentHex: String? = null,
    /** Shared color library backing both the accent picker and category colors. */
    val paletteColors: List<String> = ColorLibrary.defaultPalette,
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
    val scanStartedAtMillis: Long? = null,
    val scanProcessedCount: Int = 0,
    val scanTotalCount: Int = 0,
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
    /** Transaction awaiting delete confirmation; the dialog is showing while non-null. */
    val pendingDeleteTransactionId: Long? = null,
    val budgetWarning: BudgetWarning? = null,
    val bankSmsSetupCompleted: Boolean = false,
    /** When the user completed registration; transactions dated before this never move account balances. */
    val onboardedAtMillis: Long = 0L,
    /** When the last SMS scan completed with permission; the next catch-up scan resumes here. */
    val lastSuccessfulScanMillis: Long = 0L,
    val defaultAccountId: Long? = null,
    /** Empty = all accounts on Summary; otherwise filter to these account ids. */
    val summarySelectedAccountIds: Set<Long> = emptySet(),
    /**
     * Normalized SMS keys of auto-detected rows the user deleted or ignored. Scans skip
     * these forever so a deleted transaction never resurrects on the next catch-up scan.
     */
    val dismissedSmsKeys: Set<String> = emptySet(),
    /** Distinct SMS bank labels seen in scans/transactions; not persisted. */
    val discoveredSmsBanks: List<String> = emptyList()
) {
    val hasCompletedRegistration: Boolean = userName.isNotBlank()

    val bankAccounts: List<BankAccount> by lazy(LazyThreadSafetyMode.NONE) {
        accounts.filter { it.type == AccountType.Bank }
    }

    val creditCardAccounts: List<BankAccount> by lazy(LazyThreadSafetyMode.NONE) {
        accounts.filter { it.type == AccountType.CreditCard }
    }

    val creditCardOutstanding: Double by lazy(LazyThreadSafetyMode.NONE) {
        creditCardAccounts.sumOf { it.balance }
    }

    val categoriesById: Map<Long, CategoryItem> by lazy(LazyThreadSafetyMode.NONE) {
        categories.associateBy { it.id }
    }

    val accountsById: Map<Long, BankAccount> by lazy(LazyThreadSafetyMode.NONE) {
        accounts.associateBy { it.id }
    }

    val activeSummaryAccountIds: Set<Long> by lazy(LazyThreadSafetyMode.NONE) {
        SummaryCalculations.activeAccountIds(this)
    }

    /** Transactions included in Summary metrics for [selectedMonth], matching calendar-month bank statements. */
    val monthTransactions: List<LedgerTransaction> by lazy(LazyThreadSafetyMode.NONE) {
        SummaryCalculations.monthTransactions(this)
    }

    val monthExpenseTransactions: List<LedgerTransaction> by lazy(LazyThreadSafetyMode.NONE) {
        monthTransactions.filter { it.type == TransactionType.Expense && !it.isCreditCardTransaction }
    }

    val monthIncomeTransactions: List<LedgerTransaction> by lazy(LazyThreadSafetyMode.NONE) {
        monthTransactions.filter { it.type == TransactionType.Income && !it.isCreditCardTransaction }
    }

    val monthExpense: Double by lazy(LazyThreadSafetyMode.NONE) {
        monthExpenseTransactions
            .sumOf { it.amount }
    }

    val monthNet: Double by lazy(LazyThreadSafetyMode.NONE) {
        monthIncome - monthExpense
    }

    val monthIncome: Double by lazy(LazyThreadSafetyMode.NONE) {
        monthIncomeTransactions.sumOf { it.amount }
    }

    val monthReportIncome: Double by lazy(LazyThreadSafetyMode.NONE) {
        monthIncome
    }

    val monthReportNet: Double by lazy(LazyThreadSafetyMode.NONE) {
        monthReportIncome - monthExpense
    }

    val investmentCategoryIds: Set<Long> by lazy(LazyThreadSafetyMode.NONE) {
        categories
            .filter { it.isInvestmentCategoryName() }
            .map { it.id }
            .toSet()
    }

    fun isInvestmentTransaction(transaction: LedgerTransaction): Boolean {
        return transaction.categoryId in investmentCategoryIds
    }

    fun investmentTotalFor(month: YearMonth): Double {
        if (investmentCategoryIds.isEmpty()) return 0.0
        return transactions
            .filter {
                it.categoryId in investmentCategoryIds && it.month() == month
            }
            .sumOf { it.amount }
    }

    val monthInvestment: Double by lazy(LazyThreadSafetyMode.NONE) {
        investmentTotalFor(selectedMonth)
    }

    fun creditCardSpendTotalFor(month: YearMonth): Double {
        val spends = transactions
            .filter {
                it.isCreditCardTransaction &&
                    !it.excludeFromSummary &&
                    it.type == TransactionType.Expense &&
                    it.month() == month
            }
            .sumOf { it.amount }
        val refunds = transactions
            .filter {
                it.isCreditCardTransaction &&
                    !it.excludeFromSummary &&
                    it.type == TransactionType.Income &&
                    it.month() == month
            }
            .sumOf { it.amount }
        return spends - refunds
    }

    val monthCreditCardSpend: Double by lazy(LazyThreadSafetyMode.NONE) {
        creditCardSpendTotalFor(selectedMonth)
    }

    /** Actual credits dated inside [selectedMonth] (calendar), for comparison when payroll shift moves income. */
    val calendarMonthIncomeTotal: Double by lazy(LazyThreadSafetyMode.NONE) {
        SummaryCalculations.calendarMonthIncomeTotal(this)
    }

    val selectedMonthOpeningBalance: Double by lazy(LazyThreadSafetyMode.NONE) {
        balanceAtStartOfSelectedMonth
    }

    /** Current user-entered account balance, used as the anchor for reverse reconstruction. */
    val currentBalanceAnchor: Double by lazy(LazyThreadSafetyMode.NONE) {
        SummaryCalculations.balanceAnchor(this)
    }

    /** Cash balance before any transaction dated in [selectedMonth] (calendar), reconstructed from current balance. */
    val balanceAtStartOfSelectedMonth: Double by lazy(LazyThreadSafetyMode.NONE) {
        SummaryCalculations.balanceBeforeDate(this, selectedMonth.atDay(1))
    }

    /** Cash balance after all transactions through the last day of [selectedMonth], reconstructed from current balance. */
    val balanceAtEndOfSelectedMonth: Double by lazy(LazyThreadSafetyMode.NONE) {
        SummaryCalculations.balanceBeforeDate(this, selectedMonth.plusMonths(1).atDay(1))
    }

    /** Actual calendar cashflow for the selected month. This should explain opening to closing balance. */
    val calendarMonthNet: Double by lazy(LazyThreadSafetyMode.NONE) {
        SummaryCalculations.calendarMonthNet(this)
    }

    /** Difference between reconstructed closing balance and calendar cashflow math; non-zero means missing/excluded data. */
    val selectedMonthReconciliationGap: Double by lazy(LazyThreadSafetyMode.NONE) {
        balanceAtEndOfSelectedMonth - (balanceAtStartOfSelectedMonth + calendarMonthNet)
    }

    val trackedBalance: Double by lazy(LazyThreadSafetyMode.NONE) {
        bankAccounts.sumOf { it.balance }
    }

    val activeBudgets: List<BudgetPlan> by lazy(LazyThreadSafetyMode.NONE) {
        budgets.filter { it.month == selectedMonth }
    }

    val todayTransactions: List<LedgerTransaction> by lazy(LazyThreadSafetyMode.NONE) {
        TransactionListCalculations.todayTransactions(this)
    }

    val todayDetectedDrafts: List<DetectedTransactionDraft> by lazy(LazyThreadSafetyMode.NONE) {
        TransactionListCalculations.todayDetectedDrafts(this)
    }

    val activityTransactions: List<LedgerTransaction> by lazy(LazyThreadSafetyMode.NONE) {
        TransactionListCalculations.activityTransactions(this)
    }

    val dashboardTransactionPageCount: Int by lazy(LazyThreadSafetyMode.NONE) {
        ((todayTransactions.size + TRANSACTIONS_PER_PAGE - 1) / TRANSACTIONS_PER_PAGE)
            .coerceAtLeast(1)
    }

    val dashboardCurrentPage: Int by lazy(LazyThreadSafetyMode.NONE) {
        dashboardTransactionPage.coerceIn(1, dashboardTransactionPageCount)
    }

    val dashboardPagedTransactions: List<LedgerTransaction> by lazy(LazyThreadSafetyMode.NONE) {
        todayTransactions
            .drop((dashboardCurrentPage - 1) * TRANSACTIONS_PER_PAGE)
            .take(TRANSACTIONS_PER_PAGE)
    }

    val dashboardDraftPageCount: Int by lazy(LazyThreadSafetyMode.NONE) {
        ((todayDetectedDrafts.size + TRANSACTIONS_PER_PAGE - 1) / TRANSACTIONS_PER_PAGE)
            .coerceAtLeast(1)
    }

    val dashboardCurrentDraftPage: Int by lazy(LazyThreadSafetyMode.NONE) {
        dashboardDraftPage.coerceIn(1, dashboardDraftPageCount)
    }

    val dashboardPagedDrafts: List<DetectedTransactionDraft> by lazy(LazyThreadSafetyMode.NONE) {
        todayDetectedDrafts
            .drop((dashboardCurrentDraftPage - 1) * TRANSACTIONS_PER_PAGE)
            .take(TRANSACTIONS_PER_PAGE)
    }

    val pagedTransactions: List<LedgerTransaction> by lazy(LazyThreadSafetyMode.NONE) {
        activityTransactions.take((activityTransactionPage.coerceAtLeast(1)) * TRANSACTIONS_PER_PAGE)
    }

    /** [pagedTransactions] grouped by day, preserving order; memoized so the Activity list
     *  doesn't re-group on every recomposition. */
    val pagedTransactionsByDay: Map<java.time.LocalDate, List<LedgerTransaction>> by lazy(LazyThreadSafetyMode.NONE) {
        pagedTransactions.groupBy { it.transactionDate() }
    }

    val hasMoreTransactions: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        pagedTransactions.size < activityTransactions.size
    }
}

private fun CategoryItem.isInvestmentCategoryName(): Boolean {
    val normalized = name.trim().lowercase()
    return normalized == "investment" ||
        normalized == "investments" ||
        iconKey.equals("investment", ignoreCase = true)
}

object DefaultCategories {
    val items = listOf(
        CategoryItem(0, "Uncategorized", "category", Icons.Rounded.Category, true, "#8F95A3"),
        CategoryItem(1, "Grocery", "grocery", Icons.Rounded.LocalGroceryStore, true, "#38E68B"),
        CategoryItem(2, "Food", "food", Icons.Rounded.Dining, true, "#FFC857"),
        CategoryItem(3, "Shopping", "shopping", Icons.Rounded.ShoppingBag, true, "#FF4FB8"),
        CategoryItem(4, "Fuel", "fuel", Icons.Rounded.LocalGasStation, true, "#FF8A3D"),
        CategoryItem(5, "Rent", "rent", Icons.Rounded.Home, true, "#FF6B7A"),
        CategoryItem(6, "Investment", "investment", Icons.AutoMirrored.Rounded.TrendingUp, true, "#8B5CF6"),
        CategoryItem(7, "CC", "credit_card", Icons.Rounded.Payments, true, "#35D6E7"),
        CategoryItem(8, "Transfer", "transfer", Icons.Rounded.AccountBalance, true, "#5BC0FF")
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
        CategoryIconOption("credit_card", "Credit Card", Icons.Rounded.Payments),
        CategoryIconOption("transfer", "Transfer", Icons.Rounded.AccountBalance),
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
