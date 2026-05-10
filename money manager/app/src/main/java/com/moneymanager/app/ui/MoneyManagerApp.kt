package com.moneymanager.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.BudgetPlan
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.CurrencyOption
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.ActivityDateFilter
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.MonthlyCategoryTotal
import com.moneymanager.app.model.MoneyIcons
import com.moneymanager.app.model.ScreenTab
import com.moneymanager.app.model.ThemeMode
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.month
import com.moneymanager.app.model.shortLabel
import com.moneymanager.app.model.transactionDate
import com.moneymanager.app.ui.theme.LossRed
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.Navy850
import com.moneymanager.app.ui.theme.Navy900
import com.moneymanager.app.ui.theme.Navy950
import com.moneymanager.app.ui.theme.PrimaryBlue
import com.moneymanager.app.ui.theme.PrimarySoft
import com.moneymanager.app.ui.theme.TextDim
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import com.moneymanager.app.ui.theme.WarningAmber
import com.moneymanager.app.viewmodel.MoneyViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyManagerApp(viewModel: MoneyViewModel) {
    val state by viewModel.uiState.collectAsState()

    if (state.isAppInitializing) {
        InitialLoadingScreen()
        return
    }

    if (!state.hasCompletedRegistration) {
        RegistrationScreen(onComplete = viewModel::completeRegistration)
        return
    }

    LaunchedEffect(Unit) {
        viewModel.scanTodayMessages()
        while (true) {
            delay(5 * 60 * 1000L)
            viewModel.scanTodayMessages()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950),
        containerColor = Navy950,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            BottomNavigation(
                selectedTab = state.selectedTab,
                onTabSelected = viewModel::selectTab
            )
        },
        floatingActionButton = {
            if (state.selectedTab != ScreenTab.Settings) {
                val fabDark = isAmoledTheme()
                Button(
                    onClick = {
                        when (state.selectedTab) {
                            ScreenTab.Budget -> viewModel.setBudgetSheet(true)
                            else -> viewModel.setTransactionSheet(true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = if (fabDark) Color(0xFF001A42) else Color.White
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { BrandHeader(state.userName) }
            when (state.selectedTab) {
                ScreenTab.Dashboard -> dashboardContent(
                    state = state,
                    onOpenSummary = viewModel::selectTab,
                    onAcceptDraft = viewModel::acceptDetectedTransaction,
                    onIgnoreDraft = viewModel::ignoreDetectedTransaction,
                    onDeleteTransaction = viewModel::deleteTransaction,
                    onEditTransaction = viewModel::requestEditTransactionCategory,
                    onDashboardPageSelected = viewModel::selectDashboardTransactionPage,
                    onDraftPageSelected = viewModel::selectDashboardDraftPage
                )
                ScreenTab.Activity -> activityContent(
                    state = state,
                    onScanNow = viewModel::scanCurrentActivityPeriod,
                    onPopulateThreeMonths = viewModel::populateLastThreeMonths,
                    onDateFilterSelected = viewModel::setActivityDateFilter,
                    onDeleteTransaction = viewModel::deleteTransaction,
                    onEditTransaction = viewModel::requestEditTransactionCategory,
                    onLoadMore = viewModel::loadMoreTransactions
                )
                ScreenTab.Budget -> budgetContent(state, viewModel::setBudgetSheet, viewModel::deleteBudget)
                ScreenTab.Summary -> summaryContent(state, viewModel::selectMonth)
                ScreenTab.Settings -> settingsContent(
                    state = state,
                    onAddCategory = viewModel::setCategorySheet,
                    onCurrencySelected = viewModel::selectCurrency,
                    onThemeSelected = viewModel::selectThemeMode,
                    onDeleteAccount = viewModel::deleteAccount,
                    onDeleteCategory = viewModel::deleteCategory,
                    onCategoryColorSelected = viewModel::updateCategoryColor
                )
            }
        }
    }

    if (state.showTransactionSheet) {
        AddTransactionSheet(
            state = state,
            onDismiss = { viewModel.setTransactionSheet(false) },
            onAdd = viewModel::addTransaction
        )
    }

    if (state.showBudgetSheet) {
        AddBudgetSheet(
            state = state,
            onDismiss = { viewModel.setBudgetSheet(false) },
            onAdd = viewModel::addBudget
        )
    }

    if (state.showCategorySheet) {
        AddCategorySheet(
            onDismiss = { viewModel.setCategorySheet(false) },
            onAdd = viewModel::addCategory
        )
    }

    if (state.showTransactionDetailSheet && state.selectedTransactionId != null) {
        val selectedTransaction = state.transactions.firstOrNull { it.id == state.selectedTransactionId }
        TransactionDetailSheet(
            state = state,
            transaction = selectedTransaction,
            onDismiss = viewModel::cancelEditTransactionCategory,
            onSave = viewModel::updateTransactionDetails,
            onDelete = viewModel::deleteTransaction,
            onAddCustomCategory = viewModel::createCategoryForTransaction
        )
    }

    state.budgetWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = viewModel::clearBudgetWarning,
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = WarningAmber) },
            title = { Text("Budget limit crossed") },
            text = {
                Text(
                    "${warning.budgetName} has crossed ${state.money(warning.limitAmount)}. Current spend is ${state.money(warning.spentAmount)}."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::clearBudgetWarning) {
                    Text("Got it")
                }
            },
            containerColor = Navy850,
            titleContentColor = TextPrimary,
            textContentColor = TextMuted
        )
    }
}

@Composable
private fun InitialLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.dashboardContent(
    state: FinanceUiState,
    onOpenSummary: (ScreenTab) -> Unit,
    onAcceptDraft: (Long, Long) -> Unit,
    onIgnoreDraft: (Long) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDashboardPageSelected: (Int) -> Unit,
    onDraftPageSelected: (Int) -> Unit
) {
    item {
        TodayDonutCard(state)
    }
    item {
        TodayCategoryBreakdown(state)
    }
    if (state.todayDetectedDrafts.isNotEmpty()) {
        item {
            SectionHeader(
                "Today Pending",
                "Page ${state.dashboardCurrentDraftPage} of ${state.dashboardDraftPageCount}"
            )
        }
        items(state.dashboardPagedDrafts, key = { "draft_${it.id}" }) {
            DetectedDraftRow(
                state = state,
                draft = it,
                onAccept = onAcceptDraft,
                onIgnore = onIgnoreDraft
            )
        }
        item {
            DashboardPagination(
                pageCount = state.dashboardDraftPageCount,
                currentPage = state.dashboardCurrentDraftPage,
                onPageSelected = onDraftPageSelected,
                label = "Pages"
            )
        }
    } else {
        item {
            SectionHeader(
                "Today",
                "Page ${state.dashboardCurrentPage} of ${state.dashboardTransactionPageCount}"
            )
        }
        if (state.todayTransactions.isEmpty()) {
            item { EmptyPanel("No transactions for today yet.") }
        } else {
            items(state.dashboardPagedTransactions, key = { "dashboard_txn_${it.id}" }) {
                TransactionRow(transaction = it, state = state, onSelect = onEditTransaction)
            }
            item {
                DashboardPagination(
                    pageCount = state.dashboardTransactionPageCount,
                    currentPage = state.dashboardCurrentPage,
                    onPageSelected = onDashboardPageSelected,
                    label = "Pages"
                )
            }
        }
    }
    item {
        ActionPanel(
            title = "Monthly Analysis",
            subtitle = "Open category history, monthly totals, and graphical breakdowns.",
            icon = Icons.Rounded.BarChart,
            action = "Open",
            onClick = { onOpenSummary(ScreenTab.Summary) }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.activityContent(
    state: FinanceUiState,
    onScanNow: () -> Unit,
    onPopulateThreeMonths: () -> Unit,
    onDateFilterSelected: (ActivityDateFilter, LocalDate?, LocalDate?) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    item { LargeTitle("Activity", "Review transactions for today or pick another period.") }
    item {
        ActivityScanPanel(
            state = state,
            onScanNow = onScanNow,
            onPopulateThreeMonths = onPopulateThreeMonths
        )
    }
    item {
        ActivityDateFilterPanel(
            state = state,
            onDateFilterSelected = onDateFilterSelected
        )
    }
    item {
        SearchBarSurface("Search in selected period")
    }
    if (state.activityTransactions.isEmpty()) {
        item { EmptyPanel("No transactions in this period.") }
    } else {
        itemsIndexed(
            items = state.pagedTransactions,
            key = { index, transaction -> "${transaction.id}_${transaction.timestampMillis}_$index" }
        ) { _, transaction ->
            TransactionRow(transaction = transaction, state = state, onSelect = onEditTransaction)
        }
        if (state.hasMoreTransactions) {
            item {
                Button(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("Load more", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.budgetContent(
    state: FinanceUiState,
    onCreateBudget: (Boolean) -> Unit,
    onDeleteBudget: (Long) -> Unit
) {
    item {
        HeroMetricCard(
            label = "This Month Budgeted",
            value = state.money(state.activeBudgets.sumOf { it.limitAmount }),
            helper = "${state.activeBudgets.size} active budget${if (state.activeBudgets.size == 1) "" else "s"}"
        )
    }
    item {
        ActionPanel(
            title = "Create Budget",
            subtitle = "Attach one or multiple categories to one monthly limit.",
            icon = Icons.Rounded.PieChart,
            action = "Create",
            onClick = { onCreateBudget(true) }
        )
    }
    if (state.activeBudgets.isEmpty()) {
        item { EmptyPanel("No budgets yet. Example: Grocery 5000, or Essentials for Grocery + Food + Fuel.") }
    } else {
        items(state.activeBudgets, key = { "budget_${it.id}" }) {
            BudgetRow(budget = it, state = state, onDelete = onDeleteBudget)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.summaryContent(
    state: FinanceUiState,
    onMonthSelected: (YearMonth) -> Unit
) {
    item { LargeTitle("Monthly Summary", state.selectedMonth.shortLabel()) }
    item {
        MonthSelector(
            months = availableMonths(state),
            selected = state.selectedMonth,
            onSelected = onMonthSelected
        )
    }
    item {
        MetricGrid(
            income = state.monthIncome,
            expense = state.monthExpense,
            net = state.monthNet,
            currency = state.currency
        )
    }
    item { CashFlowGraph(state) }
    item { DailyExpenseBarGraph(state) }
    item { CategoryPieChart(state, categoryTotals(state)) }
    item { CategoryHistoryGraph(state, categoryTotals(state)) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.settingsContent(
    state: FinanceUiState,
    onAddCategory: (Boolean) -> Unit,
    onCurrencySelected: (CurrencyOption) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onCategoryColorSelected: (Long, String) -> Unit
) {
    item {
        ProfileHeader(state)
    }
    item {
        AccountSettingsGroup(state = state, onDelete = onDeleteAccount)
    }
    item {
        CurrencySelector(
            selected = state.currency,
            onSelected = onCurrencySelected
        )
    }
    item {
        ThemeSelector(
            selected = state.themeMode,
            onSelected = onThemeSelected
        )
    }
    item {
        CategorySettingsGroup(
            categories = state.categories,
            onDelete = onDeleteCategory,
            onColorSelected = onCategoryColorSelected
        )
    }
    item {
        Button(
            onClick = { onAddCategory(true) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = primaryButtonColors()
        ) {
            Icon(Icons.Rounded.Category, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create Category", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RegistrationScreen(onComplete: (String, List<Pair<String, Double>>) -> Unit) {
    var name by remember { mutableStateOf("") }
    val accounts = remember { mutableStateListOf(AccountDraft()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { BrandHeader(name.ifBlank { "You" }) }
        item {
            LargeTitle(
                title = "Create Account",
                subtitle = "Start with your name. Bank accounts are optional and can be added now for tracking."
            )
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        item { LabelText("BANK ACCOUNTS OPTIONAL") }
        items(accounts.size) { index ->
            AccountDraftRow(
                account = accounts[index],
                onNameChanged = { accounts[index] = accounts[index].copy(name = it) },
                onBalanceChanged = { accounts[index] = accounts[index].copy(balance = it) },
                onRemove = {
                    if (accounts.size > 1) accounts.removeAt(index)
                }
            )
        }
        item {
            OutlinedButton(
                onClick = { accounts.add(AccountDraft()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PrimarySoft)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Bank Account")
            }
        }
        item {
            Button(
                onClick = {
                    onComplete(
                        name,
                        accounts.mapNotNull {
                            val amount = it.balance.toDoubleOrNull()
                            if (it.name.isBlank() || amount == null) null else it.name to amount
                        }
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(12.dp),
                colors = primaryButtonColors()
            ) {
                Text("Start Tracking", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class AccountDraft(val name: String = "", val balance: String = "")

@Composable
private fun AccountDraftRow(
    account: AccountDraft,
    onNameChanged: (String) -> Unit,
    onBalanceChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    ElevatedPanel {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = PrimarySoft)
                Spacer(Modifier.width(8.dp))
                Text("Bank Account", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onRemove) { Text("Remove") }
            }
            OutlinedTextField(
                value = account.name,
                onValueChange = onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Account Name") },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = account.balance,
                onValueChange = onBalanceChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Current Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    state: FinanceUiState,
    onDismiss: () -> Unit,
    onAdd: (String, Double, TransactionType, Long, Long?, String?, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.Expense) }
    var categoryId by remember { mutableStateOf(state.categories.first().id) }
    var accountId by remember { mutableStateOf(state.accounts.firstOrNull()?.id) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Navy900, contentColor = TextPrimary) {
        SheetContent(title = "Add Transaction") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Transaction Name") },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            ChipRow {
                TransactionType.entries.forEach {
                    MoneyChip(it.name, selected = type == it, onClick = { type = it })
                }
            }
            LabelText("CATEGORY")
            ChipRow {
                state.categories.forEach {
                    CategoryChoiceChip(it, type, selected = categoryId == it.id, onClick = { categoryId = it.id })
                }
            }
            if (state.accounts.isNotEmpty()) {
                LabelText("BANK ACCOUNT OPTIONAL")
                ChipRow {
                    MoneyChip("None", selected = accountId == null, onClick = { accountId = null })
                    state.accounts.forEach {
                        MoneyChip(it.name, selected = accountId == it.id, onClick = { accountId = it.id })
                    }
                }
            }
            Button(
                onClick = {
                    onAdd(name, amount.toDoubleOrNull() ?: 0.0, type, categoryId, accountId, null, false)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = primaryButtonColors()
            ) {
                Text("Add Transaction", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(
    state: FinanceUiState,
    transaction: LedgerTransaction?,
    onDismiss: () -> Unit,
    onSave: (Long, TransactionType, Long) -> Unit,
    onDelete: (Long) -> Unit,
    onAddCustomCategory: (Long, String, String, String) -> Unit
) {
    if (transaction == null) return
    var categoryId by remember { mutableStateOf(transaction.categoryId) }
    var type by remember { mutableStateOf(transaction.type) }
    var showAllCategories by remember { mutableStateOf(false) }
    var showOriginalMessage by remember { mutableStateOf(false) }
    var showCustomCategoryForm by remember { mutableStateOf(false) }
    var customCategoryName by remember { mutableStateOf("") }
    var customColor by remember { mutableStateOf(categoryPalette.first()) }
    val categoryRanking = remember(state.transactions, state.categories) {
        state.transactions
            .groupingBy { it.categoryId }
            .eachCount()
    }
    val uncategorizedId = state.categories.firstOrNull { it.name == "Uncategorized" }?.id
    val frequentCategories = state.categories
        .filter { it.id == categoryId || it.id == uncategorizedId || categoryRanking.containsKey(it.id) }
        .sortedWith(
            compareByDescending<CategoryItem> { it.id == categoryId }
                .thenByDescending { categoryRanking[it.id] ?: 0 }
                .thenBy { it.id }
        )
        .take(7)
    val visibleCategories = if (showAllCategories) state.categories else frequentCategories
    val dateLabel = transaction.transactionDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy900,
        contentColor = TextPrimary
    ) {
        SheetContent(title = "Edit Category") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(transaction.name, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                    Text("$dateLabel | ${state.money(transaction.amount)}", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Text(signedAmount(transaction.amount, type, state.currency), color = type.amountColor(), style = MaterialTheme.typography.titleMedium)
            }
            ChipRow {
                TransactionType.entries.forEach {
                    MoneyChip(it.name, selected = type == it, onClick = { type = it })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelText("CATEGORY")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showAllCategories = !showAllCategories }) {
                    Text(if (showAllCategories) "Frequent" else "Show all", color = PrimarySoft)
                }
            }
            ChipRow {
                visibleCategories.forEach { category ->
                    CategoryChoiceChip(
                        category = category,
                        type = type,
                        selected = categoryId == category.id,
                        onClick = { categoryId = category.id }
                    )
                }
                MoneyChip("Custom +", selected = showCustomCategoryForm, onClick = { showCustomCategoryForm = !showCustomCategoryForm })
            }
            if (showCustomCategoryForm) {
                OutlinedTextField(
                    value = customCategoryName,
                    onValueChange = { customCategoryName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom category") },
                    singleLine = true,
                    colors = inputColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                ColorSwatches(selected = customColor, onSelected = { customColor = it })
                Button(
                    onClick = {
                        onAddCustomCategory(transaction.id, customCategoryName, "category", customColor)
                        customCategoryName = ""
                        showCustomCategoryForm = false
                    },
                    enabled = customCategoryName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("Create Custom Category", fontWeight = FontWeight.Bold)
                }
            }
            if (transaction.rawMessage != null) {
                OutlinedButton(
                    onClick = { showOriginalMessage = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Sms, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Show Original Message")
                }
            }
            Button(
                onClick = { onSave(transaction.id, type, categoryId) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = primaryButtonColors()
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { onDelete(transaction.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete Transaction", color = LossRed, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showOriginalMessage) {
        AlertDialog(
            onDismissRequest = { showOriginalMessage = false },
            title = { Text("Original SMS") },
            text = {
                Text(transaction.rawMessage.orEmpty(), color = TextMuted)
            },
            confirmButton = {
                TextButton(onClick = { showOriginalMessage = false }) {
                    Text("Close")
                }
            },
            containerColor = Navy850,
            titleContentColor = TextPrimary,
            textContentColor = TextMuted
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetSheet(
    state: FinanceUiState,
    onDismiss: () -> Unit,
    onAdd: (String, Double, Set<Long>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val selectedCategoryIds = remember { mutableStateListOf<Long>() }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Navy900, contentColor = TextPrimary) {
        SheetContent(title = "Create Budget") {
            Text(
                "Suggestion: allow multiple categories for shared limits like Essentials. Keep single-category budgets for strict tracking like Grocery only.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Budget Name") },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monthly Limit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            LabelText("ASSOCIATE AT LEAST ONE CATEGORY")
            ChipRow {
                state.categories.forEach { category ->
                    val selected = category.id in selectedCategoryIds
                    MoneyChip(
                        label = category.name,
                        selected = selected,
                        onClick = {
                            if (selected) selectedCategoryIds.remove(category.id) else selectedCategoryIds.add(category.id)
                        }
                    )
                }
            }
            Button(
                onClick = { onAdd(name, amount.toDoubleOrNull() ?: 0.0, selectedCategoryIds.toSet()) },
                enabled = selectedCategoryIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = primaryButtonColors()
            ) {
                Text("Save Budget", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategorySheet(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf(MoneyIcons.frequentCategoryIcons.first().key) }
    var selectedColor by remember { mutableStateOf(categoryPalette.first()) }
    var iconQuery by remember { mutableStateOf("") }
    val filteredIcons by remember(iconQuery) { derivedStateOf {
        val query = iconQuery.trim().lowercase()
        if (query.isBlank()) {
            emptyList()
        } else {
            MoneyIcons.allCategoryIcons.filter {
                it.label.lowercase().contains(query) || it.key.contains(query)
            }
        }
    } }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy900,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Create Category", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category Name") },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            LabelText("FREQUENT ICONS")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = MoneyIcons.frequentCategoryIcons,
                    key = { it.key }
                ) { option ->
                    CategoryIconChip(
                        option = option,
                        selected = selectedIconKey == option.key,
                        onClick = { selectedIconKey = option.key }
                    )
                }
            }
            OutlinedTextField(
                value = iconQuery,
                onValueChange = { iconQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search more icons") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            LabelText("COLOR")
            ColorSwatches(selected = selectedColor, onSelected = { selectedColor = it })
            LabelText("ALL ICONS")
            if (iconQuery.isBlank()) {
                Text(
                    "Type to search more icons",
                    color = TextDim,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (filteredIcons.isEmpty()) {
                Text(
                    "No matching icons found",
                    color = TextDim,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(
                        items = filteredIcons,
                        key = { it.key }
                    ) { option ->
                        CategoryIconChip(
                            option = option,
                            selected = selectedIconKey == option.key,
                            onClick = { selectedIconKey = option.key }
                        )
                    }
                }
            }
            Button(
                onClick = { onAdd(name, selectedIconKey, selectedColor) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = primaryButtonColors()
            ) {
                Text("Create Category", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CategoryIconChip(
    option: MoneyIcons.CategoryIconOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(option.label) },
        leadingIcon = {
            Icon(option.icon, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (isAmoledTheme()) PrimaryBlue else Color(0xFFEAF2FF),
            selectedLabelColor = if (isAmoledTheme()) Color(0xFF001A42) else PrimaryBlue,
            selectedLeadingIconColor = if (isAmoledTheme()) Color(0xFF001A42) else PrimaryBlue,
            containerColor = Navy800,
            labelColor = TextMuted
        )
    )
}

@Composable
private fun ColorSwatches(selected: String, onSelected: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(categoryPalette, key = { it }) { colorHex ->
            val color = colorFromHex(colorHex)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelected(colorHex) },
                contentAlignment = Alignment.Center
            ) {
                if (selected == colorHex) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CategoryChoiceChip(
    category: CategoryItem,
    type: TransactionType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = categoryColor(category, type)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(category.name) },
        leadingIcon = {
            Icon(category.icon, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = if (isAmoledTheme()) Color(0xFF10131F) else Color.White,
            selectedLeadingIconColor = if (isAmoledTheme()) Color(0xFF10131F) else Color.White,
            containerColor = Navy800,
            labelColor = TextMuted
        )
    )
}

@Composable
private fun SheetContent(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetectedDraftRow(
    state: FinanceUiState,
    draft: DetectedTransactionDraft,
    onAccept: (Long, Long) -> Unit,
    onIgnore: (Long) -> Unit
) {
    var categoryId by remember { mutableStateOf(draft.suggestedCategoryId ?: state.categories.first().id) }
    val dateLabel = draft.transactionDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

    ElevatedPanel {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Sms, MoneyGreen)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(draft.bankName, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("${draft.type.bankVerb()} | $dateLabel", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                }
                Text(draft.signedAmount(state.currency), color = draft.type.amountColor(), style = MaterialTheme.typography.titleLarge)
            }
            Text(draft.counterparty, color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
            LabelText("CATEGORY")
            ChipRow {
                state.categories.forEach { category ->
                    CategoryChoiceChip(
                        category = category,
                        type = draft.type,
                        selected = categoryId == category.id,
                        onClick = { categoryId = category.id }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onAccept(draft.id, categoryId) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAmoledTheme()) PrimarySoft else Color(0xFFEAF2FF),
                        contentColor = if (isAmoledTheme()) Color(0xFF001A42) else PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add")
                }
                OutlinedButton(
                    onClick = { onIgnore(draft.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ignore")
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(userName: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Wallet, contentDescription = null, tint = PrimarySoft, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(8.dp))
        Text("Money Manager", style = MaterialTheme.typography.headlineMedium, color = PrimarySoft)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(42.dp).clip(CircleShape).background(Navy800), contentAlignment = Alignment.Center) {
            Text(userName.take(1).uppercase(), color = PrimarySoft, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LargeTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HeroMetricCard(label: String, value: String, helper: String) {
    val dark = isAmoledTheme()
    val container = if (dark) PrimaryBlue else Color.White
    val labelColor = if (dark) Color(0xCC00285D) else TextDim
    val valueColor = if (dark) Color(0xFF07265C) else TextPrimary
    Card(
        modifier = Modifier.fillMaxWidth().height(158.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, appBorderColor())
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.Center) {
            Text(label.uppercase(), color = labelColor, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                color = valueColor,
                fontSize = 38.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(helper, color = if (dark) Color(0xFF07265C) else PrimaryBlue, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TodayDonutCard(state: FinanceUiState) {
    val income = state.todayTransactions
        .filter { it.type == TransactionType.Income }
        .sumOf { it.amount }
    val expense = state.todayTransactions
        .filter { it.type == TransactionType.Expense }
        .sumOf { it.amount }
    val total = expense.coerceAtLeast(1.0)

    ElevatedPanel {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 34f, cap = StrokeCap.Round)
                    drawArc(
                        color = appTrackColor(),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(20f, 20f),
                        size = Size(size.width - 40f, size.height - 40f),
                        style = stroke
                    )
                    drawArc(
                        color = LossRed,
                        startAngle = -90f,
                        sweepAngle = ((expense / total) * 360f).toFloat(),
                        useCenter = false,
                        topLeft = Offset(20f, 20f),
                        size = Size(size.width - 40f, size.height - 40f),
                        style = stroke
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Today", color = TextDim, style = MaterialTheme.typography.labelMedium)
                    Text(state.money(expense), color = LossRed, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text("spent", color = TextDim, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Today Spend", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${state.todayTransactions.size} transaction${if (state.todayTransactions.size == 1) "" else "s"} detected today",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                LegendDot(LossRed, "Expense")
                Text(
                    "Net movement: ${state.money(income - expense)}",
                    color = if (income >= expense) PrimarySoft else LossRed,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun TodayCategoryBreakdown(state: FinanceUiState) {
    val totals = state.categories.map { category ->
        val transactions = state.todayTransactions.filter { it.categoryId == category.id }
        MonthlyCategoryTotal(
            category = category,
            income = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount },
            expense = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
        )
    }.filter { it.income > 0.0 || it.expense > 0.0 }

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Today Segments", LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d")))
            if (totals.isEmpty()) {
                Text("No categorized movement today.", color = TextDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                val max = totals.maxOf { it.income + it.expense }.coerceAtLeast(1.0)
                totals.forEach { item ->
                    val amount = item.income + item.expense
                    SegmentAmountRow(
                        label = item.category.name,
                        value = state.money(amount),
                        color = categoryColor(item.category, if (item.expense > 0.0) TransactionType.Expense else TransactionType.Income),
                        progress = (amount / max).toFloat()
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityScanPanel(
    state: FinanceUiState,
    onScanNow: () -> Unit,
    onPopulateThreeMonths: () -> Unit
) {
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Message Import", if (state.isScanningMessages) "Scanning" else "Ready")
            Text(
                if (state.scanStatusMessage.isBlank()) {
                    "Scan the selected period or populate the last 3 months from SMS."
                } else {
                    state.scanStatusMessage
                },
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScanNow,
                    enabled = !state.isScanningMessages,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = primaryButtonColors()
                ) {
                    Icon(Icons.Rounded.Sms, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Now", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onPopulateThreeMonths,
                    enabled = !state.isScanningMessages,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Populate 3M", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ActivityDateFilterPanel(
    state: FinanceUiState,
    onDateFilterSelected: (ActivityDateFilter, LocalDate?, LocalDate?) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val today = LocalDate.now()
    var startDate by remember(state.activityStartDate) { mutableStateOf(state.activityStartDate) }
    var endDate by remember(state.activityEndDate) { mutableStateOf(state.activityEndDate) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Period", "${formatter.format(state.activityStartDate)} - ${formatter.format(state.activityEndDate)}")
            ChipRow {
                ActivityDateFilter.entries.forEach { filter ->
                    MoneyChip(
                        label = filter.label,
                        selected = state.activityDateFilter == filter,
                        onClick = { onDateFilterSelected(filter, startDate, endDate) }
                    )
                }
            }
            if (state.activityDateFilter == ActivityDateFilter.Custom) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(formatter.format(startDate), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(formatter.format(endDate), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        val context = LocalContext.current
        LaunchedEffect(showStartPicker) {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    startDate = LocalDate.of(year, month + 1, day)
                    if (startDate.isAfter(endDate)) endDate = startDate
                    onDateFilterSelected(ActivityDateFilter.Custom, startDate, endDate)
                    showStartPicker = false
                },
                startDate.year,
                startDate.monthValue - 1,
                startDate.dayOfMonth
            ).apply {
                datePicker.maxDate = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                show()
            }
        }
    }

    if (showEndPicker) {
        val context = LocalContext.current
        LaunchedEffect(showEndPicker) {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    endDate = LocalDate.of(year, month + 1, day).coerceAtMost(today)
                    if (startDate.isAfter(endDate)) startDate = endDate
                    onDateFilterSelected(ActivityDateFilter.Custom, startDate, endDate)
                    showEndPicker = false
                },
                endDate.year,
                endDate.monthValue - 1,
                endDate.dayOfMonth
            ).apply {
                datePicker.maxDate = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                show()
            }
        }
    }
}

@Composable
private fun MetricGrid(income: Double, expense: Double, net: Double, currency: CurrencyOption) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetric("Income", money(income, currency), MoneyGreen, Modifier.weight(1f))
            SmallMetric("Expenses", money(expense, currency), LossRed, Modifier.weight(1f))
        }
        SmallMetric("Net Total", money(net, currency), if (net >= 0) PrimarySoft else LossRed, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SmallMetric(label: String, value: String, valueColor: Color, modifier: Modifier) {
    ElevatedPanel(modifier = modifier.height(104.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = TextDim, style = MaterialTheme.typography.bodyMedium)
            Text(value, color = valueColor, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
        }
    }
}

@Composable
private fun MonthSelector(months: List<YearMonth>, selected: YearMonth, onSelected: (YearMonth) -> Unit) {
    ChipRow {
        months.forEach {
            MoneyChip(it.shortLabel(), selected = it == selected, onClick = { onSelected(it) })
        }
    }
}

@Composable
private fun ActionPanel(title: String, subtitle: String, icon: ImageVector, action: String, onClick: () -> Unit) {
    ElevatedPanel {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, MoneyGreen)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = TextDim, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onClick) {
                Text(action, color = PrimarySoft)
            }
        }
    }
}

@Composable
private fun DashboardPagination(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    label: String
) {
    if (pageCount <= 1) return

    ElevatedPanel {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = label,
                color = TextMuted,
                style = MaterialTheme.typography.labelMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pageCount) { index ->
                    val page = index + 1
                    MoneyChip(
                        label = page.toString(),
                        selected = page == currentPage,
                        onClick = { onPageSelected(page) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBarSurface(placeholder: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(if (isAmoledTheme()) 16.dp else 10.dp),
        colors = CardDefaults.cardColors(containerColor = if (isAmoledTheme()) Navy850 else Color.White),
        border = BorderStroke(1.dp, appBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = TextDim)
            Spacer(Modifier.width(8.dp))
            Text(placeholder, color = TextDim, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CashFlowOverviewCard(state: FinanceUiState) {
    val months = (5 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) }
    val income = months.map { month ->
        state.transactions
            .filter { it.month() == month && it.type == TransactionType.Income }
            .sumOf { it.amount }
    }
    val expenses = months.map { month ->
        state.transactions
            .filter { it.month() == month && it.type == TransactionType.Expense }
            .sumOf { it.amount }
    }
    val max = (income + expenses).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    ElevatedPanel {
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Cash Flow", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                LegendDot(PrimaryBlue, "Income")
                Spacer(Modifier.width(10.dp))
                LegendDot(TextDim, "Expenses")
            }
            HorizontalDivider(color = appBorderColor())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                months.forEachIndexed { index, month ->
                    CashFlowMonthBar(
                        label = month.month.name.take(3),
                        incomeProgress = (income[index] / max).toFloat(),
                        expenseProgress = (expenses[index] / max).toFloat(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CashFlowMonthBar(
    label: String,
    incomeProgress: Float,
    expenseProgress: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(incomeProgress.coerceIn(0.08f, 1f))
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(PrimaryBlue)
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(expenseProgress.coerceIn(0.08f, 1f))
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(if (isAmoledTheme()) TextMuted else Color(0xFFC2C8D8))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label.uppercase(), color = TextDim, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TransactionRow(
    transaction: LedgerTransaction,
    state: FinanceUiState,
    onSelect: (Long) -> Unit
) {
    val category = state.categories.firstOrNull { it.id == transaction.categoryId }
    val color = categoryColor(category, transaction.type)
    val dateLabel = transaction.transactionDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(transaction.id) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy850),
        border = BorderStroke(1.dp, color.copy(alpha = if (isAmoledTheme()) 0.82f else 0.64f))
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(54.dp).clip(RoundedCornerShape(8.dp)).background(color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    transaction.name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        category?.name ?: "Set category",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("  |  $dateLabel", color = TextDim, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    transaction.signedAmount(state.currency),
                    color = transaction.type.amountColor(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text("Tap to edit", color = TextDim, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun BudgetRow(budget: BudgetPlan, state: FinanceUiState, onDelete: (Long) -> Unit) {
    val spent = state.transactions
        .filter { it.type == TransactionType.Expense && it.month() == budget.month && it.categoryId in budget.categoryIds }
        .sumOf { it.amount }
    val progress = (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f)
    val over = spent > budget.limitAmount
    val names = state.categories.filter { it.id in budget.categoryIds }.joinToString { it.name }

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.PieChart, if (over) LossRed else PrimarySoft)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(budget.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(names, color = TextDim, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(state.money(spent), color = if (over) LossRed else TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("of ${state.money(budget.limitAmount)}", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                color = if (over) LossRed else PrimarySoft,
                trackColor = appTrackColor()
            )
            TextButton(onClick = { onDelete(budget.id) }, modifier = Modifier.align(Alignment.End)) {
                Text("Delete", color = LossRed)
            }
        }
    }
}

@Composable
private fun CashFlowGraph(state: FinanceUiState) {
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Graph", "Income vs Expense")
            val transactions = state.monthTransactions
            val income = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount }.toFloat()
            val expense = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }.toFloat()
            val max = maxOf(income, expense, 1f)
            Bar("Income", income / max, MoneyGreen, state.money(income.toDouble()))
            Bar("Expense", expense / max, LossRed, state.money(expense.toDouble()))
        }
    }
}

@Composable
private fun Bar(label: String, progress: Float, color: Color, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(label, color = TextMuted, modifier = Modifier.weight(1f))
            Text(value, color = TextPrimary)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)),
            color = color,
            trackColor = Navy800
        )
    }
}

@Composable
private fun SegmentAmountRow(label: String, value: String, color: Color, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
            Text(value, color = TextMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0.04f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
            color = color,
            trackColor = appTrackColor()
        )
    }
}

@Composable
private fun DailyExpenseBarGraph(state: FinanceUiState) {
    val selectedMonth = state.selectedMonth
    val today = LocalDate.now()
    val lastDay = if (selectedMonth == YearMonth.now()) today.dayOfMonth else selectedMonth.lengthOfMonth()
    val daySegments = (1..lastDay).map { day ->
        val date = selectedMonth.atDay(day)
        val transactions = state.transactions
            .filter { it.type == TransactionType.Expense && it.transactionDate() == date }
        val segments = state.categories.mapNotNull { category ->
            val amount = transactions.filter { it.categoryId == category.id }.sumOf { it.amount }
            if (amount > 0.0) category to amount else null
        }
        date to segments
    }
    val max = daySegments.maxOfOrNull { it.second.sumOf { segment -> segment.second } }?.coerceAtLeast(1.0) ?: 1.0

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Daily Expenses", selectedMonth.shortLabel())
            if (daySegments.all { it.second.isEmpty() }) {
                EmptyPanel("No daily expenses for this month.")
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    items(daySegments, key = { it.first.toString() }) { (date, segments) ->
                        val total = segments.sumOf { it.second }
                        DailyExpenseBar(
                            day = date.dayOfMonth,
                            segments = segments,
                            maxAmount = max,
                            value = state.money(total)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyExpenseBar(day: Int, segments: List<Pair<CategoryItem, Double>>, maxAmount: Double, value: String) {
    val amount = segments.sumOf { it.second }
    Column(
        modifier = Modifier.width(34.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (amount > 0.0) {
            Text(
                value,
                color = TextDim,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
        }
        Column(
            modifier = Modifier
                .width(22.dp)
                .height((112 * (amount / maxAmount).toFloat().coerceIn(0.05f, 1f)).dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(appTrackColor()),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (amount > 0.0) {
                segments.forEach { (category, segmentAmount) ->
                    val segmentHeight = (112 * (segmentAmount / maxAmount).toFloat()).coerceAtLeast(3f).dp
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(segmentHeight)
                            .background(categoryColor(category, TransactionType.Expense))
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(day.toString(), color = TextMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CategoryPieChart(state: FinanceUiState, totals: List<MonthlyCategoryTotal>) {
    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Pie Chart", "Outgoing by Category")
            val expenses = totals.filter { it.expense > 0.0 }
            val total = expenses.sumOf { it.expense }.toFloat()
            if (total <= 0f) {
                EmptyPanel("No category expenses this month.")
            } else {
                Canvas(modifier = Modifier.align(Alignment.CenterHorizontally).size(170.dp)) {
                    var startAngle = -90f
                    expenses.forEachIndexed { index, item ->
                        val sweep = (item.expense.toFloat() / total) * 360f
                        drawArc(
                            color = chartColors[index % chartColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(12f, 12f),
                            size = Size(size.width - 24f, size.height - 24f),
                            style = Stroke(width = 34f, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                expenses.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(chartColors[index % chartColors.size]))
                        Spacer(Modifier.width(8.dp))
                        Text(item.category.name, color = TextMuted, modifier = Modifier.weight(1f))
                        Text(state.money(item.expense), color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHistoryGraph(state: FinanceUiState, totals: List<MonthlyCategoryTotal>) {
    val visibleTotals = totals
        .filter { it.income > 0.0 || it.expense > 0.0 }
        .sortedByDescending { it.income + it.expense }
    val max = visibleTotals.maxOfOrNull { it.income + it.expense }?.coerceAtLeast(1.0) ?: 1.0

    ElevatedPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Category History", state.selectedMonth.shortLabel())
            if (visibleTotals.isEmpty()) {
                EmptyPanel("No category history for this month.")
            } else {
                visibleTotals.forEach { item ->
                    val total = item.income + item.expense
                    SegmentAmountRow(
                        label = item.category.name,
                        value = state.money(total),
                        color = categoryColor(item.category, if (item.expense >= item.income) TransactionType.Expense else TransactionType.Income),
                        progress = (total / max).toFloat()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(state: FinanceUiState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp).clip(CircleShape).background(Navy800), contentAlignment = Alignment.Center) {
            Text(state.userName.take(1).uppercase(), color = PrimarySoft, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(state.userName, color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
        Text("Local finance tracking", color = TextDim, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsGroup(title: String, rows: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText(title)
        ElevatedPanel {
            Column {
                rows.forEachIndexed { index, row ->
                    Text(row, color = TextPrimary, modifier = Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                    if (index != rows.lastIndex) HorizontalDivider(color = appDividerColor(), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun AccountSettingsGroup(state: FinanceUiState, onDelete: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText("BANK ACCOUNTS")
        ElevatedPanel {
            if (state.accounts.isEmpty()) {
                Text("No accounts added", color = TextDim, modifier = Modifier.padding(16.dp))
            } else {
                Column {
                    state.accounts.forEachIndexed { index, account ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(account.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                Text(state.money(account.balance), color = TextDim, style = MaterialTheme.typography.bodyMedium)
                            }
                            TextButton(onClick = { onDelete(account.id) }) {
                                Text("Delete", color = LossRed)
                            }
                        }
                        if (index != state.accounts.lastIndex) {
                            HorizontalDivider(color = appDividerColor(), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySettingsGroup(
    categories: List<CategoryItem>,
    onDelete: (Long) -> Unit,
    onColorSelected: (Long, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText("CATEGORIES")
        ElevatedPanel {
            Column {
                categories.forEachIndexed { index, category ->
                    var expanded by remember { mutableStateOf(false) }
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(categoryColor(category, TransactionType.Expense)))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                category.name,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(if (expanded) "Hide" else "Edit", color = PrimarySoft, style = MaterialTheme.typography.labelMedium)
                        }
                        if (expanded) {
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ColorSwatches(
                                    selected = category.colorHex,
                                    onSelected = { onColorSelected(category.id, it) }
                                )
                                if (!category.isDefault) {
                                    TextButton(onClick = { onDelete(category.id) }) {
                                        Text("Delete Category", color = LossRed)
                                    }
                                }
                            }
                        }
                    }
                    if (index != categories.lastIndex) {
                        HorizontalDivider(color = appDividerColor(), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySelector(
    selected: CurrencyOption,
    onSelected: (CurrencyOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText("CURRENCY")
        ElevatedPanel {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Selected: ${selected.label} (${selected.currencyCode})",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                ChipRow {
                    CurrencyOption.entries.forEach {
                        MoneyChip(
                            label = "${it.currencyCode} ${it.symbol}",
                            selected = selected == it,
                            onClick = { onSelected(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSelector(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit
) {
    val dark = selected == ThemeMode.Dark
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText("THEME")
        ElevatedPanel {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dark Mode", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (dark) "AMOLED black" else "Clean white",
                        color = TextDim,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = dark,
                    onCheckedChange = { onSelected(if (it) ThemeMode.Dark else ThemeMode.Light) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.weight(1f))
        Text(action, color = PrimarySoft, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ElevatedPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isAmoledTheme()) 16.dp else 10.dp),
        colors = CardDefaults.cardColors(containerColor = Navy850),
        border = BorderStroke(1.dp, appBorderColor())
    ) {
        content()
    }
}

private fun appBorderColor(): Color {
    return if (isAmoledTheme()) Color(0xFF33363D) else Color(0xFFC9CEDD)
}

private fun appDividerColor(): Color {
    return if (isAmoledTheme()) Color(0xFF283044) else Color(0xFFD2D7E4)
}

private fun appTrackColor(): Color {
    return if (isAmoledTheme()) Navy800 else Color(0xFFE7EBF5)
}

@Composable
fun primaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = PrimaryBlue,
    contentColor = if (isAmoledTheme()) Color(0xFF001A42) else Color.White
)

fun isAmoledTheme(): Boolean {
    return Navy950 == Color(0xFF000000)
}

@Composable
private fun EmptyPanel(text: String) {
    ElevatedPanel {
        Text(text, color = TextDim, modifier = Modifier.padding(18.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(if (isAmoledTheme()) 14.dp else 28.dp)).background(tint.copy(alpha = if (isAmoledTheme()) 0.14f else 0.11f)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(27.dp))
    }
}

@Composable
private fun LabelText(text: String) {
    Text(text, color = PrimarySoft, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun MoneyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val dark = isAmoledTheme()
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (dark) PrimaryBlue else Color(0xFFEAF2FF),
            selectedLabelColor = if (dark) Color(0xFF001A42) else PrimaryBlue,
            containerColor = Navy800,
            labelColor = TextMuted
        )
    )
}

@Composable
private fun BottomNavigation(selectedTab: ScreenTab, onTabSelected: (ScreenTab) -> Unit) {
    val dark = isAmoledTheme()
    NavigationBar(containerColor = if (dark) Navy850 else Color(0xFFF7F9FE), tonalElevation = 0.dp) {
        ScreenTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = {
                    Text(
                        tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        fontSize = 11.sp,
                        lineHeight = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimarySoft,
                    selectedTextColor = PrimarySoft,
                    indicatorColor = if (dark) Color(0xFF112654) else Color(0xFFEAF2FF),
                    unselectedIconColor = if (dark) TextDim.copy(alpha = 0.62f) else TextPrimary,
                    unselectedTextColor = if (dark) TextDim.copy(alpha = 0.62f) else TextPrimary
                )
            )
        }
    }
}

@Composable
private fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = appBorderColor(),
    focusedContainerColor = Navy900,
    unfocusedContainerColor = Navy900,
    focusedLabelColor = PrimarySoft,
    unfocusedLabelColor = TextMuted,
    cursorColor = PrimaryBlue
)

private fun availableMonths(state: FinanceUiState): List<YearMonth> {
    val fromTransactions = state.transactions.map { it.month() }
    return (fromTransactions + YearMonth.now()).distinct().sortedDescending()
}

private fun categoryTotals(state: FinanceUiState): List<MonthlyCategoryTotal> {
    return state.categories.map { category ->
        val transactions = state.monthTransactions.filter { it.categoryId == category.id }
        MonthlyCategoryTotal(
            category = category,
            income = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount },
            expense = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
        )
    }
}

private fun LedgerTransaction.signedAmount(currency: CurrencyOption): String {
    return signedAmount(amount, type, currency)
}

private fun DetectedTransactionDraft.signedAmount(currency: CurrencyOption): String {
    return signedAmount(amount, type, currency)
}

private fun signedAmount(amount: Double, type: TransactionType, currency: CurrencyOption): String {
    val prefix = if (type == TransactionType.Income) "+" else "-"
    return "$prefix${money(amount, currency)}"
}

private fun TransactionType.amountColor(): Color {
    return if (this == TransactionType.Income) MoneyGreen else LossRed
}

private fun TransactionType.bankVerb(): String {
    return if (this == TransactionType.Income) "credited" else "debited"
}

private fun categoryColor(category: CategoryItem?, type: TransactionType): Color {
    if (category == null) return TextMuted
    return colorFromHex(category.colorHex)
}

private fun categoryColor(category: String, type: TransactionType): Color {
    if (type == TransactionType.Income) return MoneyGreen
    return when (category) {
        "Uncategorized" -> TextDim
        "Grocery" -> Color(0xFF38E68B)
        "Food" -> Color(0xFFFFC857)
        "Shopping" -> Color(0xFFFF4FB8)
        "Fuel" -> Color(0xFFFF8A3D)
        "Rent" -> Color(0xFFFF6B7A)
        else -> TextMuted
    }
}

private fun colorFromHex(hex: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(TextDim)
}

private fun FinanceUiState.money(value: Double): String {
    return money(value, currency)
}

private fun money(value: Double, currency: CurrencyOption): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    formatter.currency = Currency.getInstance(currency.currencyCode)
    formatter.maximumFractionDigits = 2
    return formatter.format(value)
}

private val chartColors = listOf(
    Color(0xFF4F8CFF),
    Color(0xFF38E68B),
    Color(0xFFFFC857),
    Color(0xFFFF4FB8),
    Color(0xFFFF8A3D),
    Color(0xFF9B5CFF),
    Color(0xFF00D1C1),
    Color(0xFFFF6B7A)
)

private val categoryPalette = listOf(
    "#8F95A3",
    "#4F8CFF",
    "#38E68B",
    "#FFC857",
    "#FF4FB8",
    "#FF8A3D",
    "#9B5CFF",
    "#00D1C1",
    "#FF6B7A",
    "#2DD4BF"
)
