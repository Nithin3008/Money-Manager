package com.moneymanager.app.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Dining
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.YearMonth

enum class TransactionType {
    Income,
    Expense
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

data class BankAccount(
    val id: Long,
    val name: String,
    val balance: Double
)

data class CategoryItem(
    val id: Long,
    val name: String,
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
    val userName: String = "",
    val currency: CurrencyOption = CurrencyOption.INR,
    val selectedTab: ScreenTab = ScreenTab.Dashboard,
    val selectedMonth: YearMonth = YearMonth.now(),
    val accounts: List<BankAccount> = emptyList(),
    val categories: List<CategoryItem> = DefaultCategories.items,
    val transactions: List<LedgerTransaction> = emptyList(),
    val budgets: List<BudgetPlan> = emptyList(),
    val detectedDrafts: List<DetectedTransactionDraft> = emptyList(),
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
}

object DefaultCategories {
    val items = listOf(
        CategoryItem(1, "Grocery", Icons.Rounded.LocalGroceryStore, true),
        CategoryItem(2, "Food", Icons.Rounded.Dining, true),
        CategoryItem(3, "Shopping", Icons.Rounded.ShoppingBag, true),
        CategoryItem(4, "Fuel", Icons.Rounded.LocalGasStation, true),
        CategoryItem(5, "Rent", Icons.Rounded.Home, true)
    )
}

object MoneyIcons {
    val Account = Icons.Rounded.AccountBalance
    val Category = Icons.Rounded.Category
    val Transport = Icons.Rounded.DirectionsCar
}
