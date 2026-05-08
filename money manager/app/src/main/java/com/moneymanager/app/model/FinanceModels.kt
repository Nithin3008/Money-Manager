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

enum class ScreenTab(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Rounded.GridView),
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

data class BankAccount(
    val id: Long,
    val name: String,
    val balance: Double
)

data class CategoryItem(
    val id: Long,
    val name: String,
    val iconKey: String,
    val icon: ImageVector,
    val isDefault: Boolean
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
    val rawMessage: String? = null
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
    val detectedAtMillis: Long
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
    val selectedTab: ScreenTab = ScreenTab.Dashboard,
    val selectedMonth: YearMonth = YearMonth.now(),
    val accounts: List<BankAccount> = emptyList(),
    val categories: List<CategoryItem> = DefaultCategories.items,
    val transactions: List<LedgerTransaction> = emptyList(),
    val budgets: List<BudgetPlan> = emptyList(),
    val detectedDrafts: List<DetectedTransactionDraft> = emptyList(),
    val dashboardDraftPage: Int = 1,
    val dashboardTransactionPage: Int = 1,
    val activityTransactionPage: Int = 1,
    val isScanningMessages: Boolean = false,
    val showTransactionSheet: Boolean = false,
    val showBudgetSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val budgetWarning: BudgetWarning? = null
) {
    val hasCompletedRegistration: Boolean = userName.isNotBlank()

    val monthTransactions: List<LedgerTransaction>
        get() = transactions.filter { it.month() == selectedMonth }

    val monthIncome: Double
        get() = monthTransactions
            .filter { it.type == TransactionType.Income }
            .sumOf { it.amount }

    val monthExpense: Double
        get() = monthTransactions
            .filter { it.type == TransactionType.Expense }
            .sumOf { it.amount }

    val monthNet: Double
        get() = monthIncome - monthExpense

    val trackedBalance: Double
        get() = accounts.sumOf { it.balance } + transactions.sumOf {
            if (it.type == TransactionType.Income) it.amount else -it.amount
        }

    val activeBudgets: List<BudgetPlan>
        get() = budgets.filter { it.month == selectedMonth }

    val dashboardTransactionPageCount: Int
        get() = ((transactions.size + TRANSACTIONS_PER_PAGE - 1) / TRANSACTIONS_PER_PAGE)
            .coerceAtLeast(1)

    val dashboardCurrentPage: Int
        get() = dashboardTransactionPage.coerceIn(1, dashboardTransactionPageCount)

    val dashboardPagedTransactions: List<LedgerTransaction>
        get() = transactions
            .drop((dashboardCurrentPage - 1) * TRANSACTIONS_PER_PAGE)
            .take(TRANSACTIONS_PER_PAGE)

    val dashboardDraftPageCount: Int
        get() = ((detectedDrafts.size + TRANSACTIONS_PER_PAGE - 1) / TRANSACTIONS_PER_PAGE)
            .coerceAtLeast(1)

    val dashboardCurrentDraftPage: Int
        get() = dashboardDraftPage.coerceIn(1, dashboardDraftPageCount)

    val dashboardPagedDrafts: List<DetectedTransactionDraft>
        get() = detectedDrafts
            .drop((dashboardCurrentDraftPage - 1) * TRANSACTIONS_PER_PAGE)
            .take(TRANSACTIONS_PER_PAGE)

    val pagedTransactions: List<LedgerTransaction>
        get() = transactions.take((activityTransactionPage.coerceAtLeast(1)) * TRANSACTIONS_PER_PAGE)

    val hasMoreTransactions: Boolean
        get() = pagedTransactions.size < transactions.size
}

object DefaultCategories {
    val items = listOf(
        CategoryItem(1, "Grocery", "grocery", Icons.Rounded.LocalGroceryStore, true),
        CategoryItem(2, "Food", "food", Icons.Rounded.Dining, true),
        CategoryItem(3, "Shopping", "shopping", Icons.Rounded.ShoppingBag, true),
        CategoryItem(4, "Fuel", "fuel", Icons.Rounded.LocalGasStation, true),
        CategoryItem(5, "Rent", "rent", Icons.Rounded.Home, true)
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
