package com.moneymanager.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.MonthlyCategoryTotal
import com.moneymanager.app.model.ScreenTab
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.month
import com.moneymanager.app.model.shortLabel
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
import java.time.YearMonth
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyManagerApp(viewModel: MoneyViewModel) {
    val state by viewModel.uiState.collectAsState()

    if (!state.hasCompletedRegistration) {
        RegistrationScreen(onComplete = viewModel::completeRegistration)
        return
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
                Button(
                    onClick = {
                        when (state.selectedTab) {
                            ScreenTab.Budget -> viewModel.setBudgetSheet(true)
                            else -> viewModel.setTransactionSheet(true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color(0xFF001A42)
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
                    onScan = viewModel::scanTodayMessages,
                    onMonthSelected = viewModel::selectMonth,
                    onOpenSummary = viewModel::selectTab,
                    onAcceptDraft = viewModel::acceptDetectedTransaction,
                    onIgnoreDraft = viewModel::ignoreDetectedTransaction
                )
                ScreenTab.Activity -> activityContent(state)
                ScreenTab.Budget -> budgetContent(state, viewModel::setBudgetSheet)
                ScreenTab.Summary -> summaryContent(state, viewModel::selectMonth)
                ScreenTab.Settings -> settingsContent(
                    state = state,
                    onAddCategory = viewModel::setCategorySheet,
                    onCurrencySelected = viewModel::selectCurrency
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

private fun androidx.compose.foundation.lazy.LazyListScope.dashboardContent(
    state: FinanceUiState,
    onScan: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
    onOpenSummary: (ScreenTab) -> Unit,
    onAcceptDraft: (Long, Long) -> Unit,
    onIgnoreDraft: (Long) -> Unit
) {
    item {
        HeroMetricCard(
            label = "Tracked Balance",
            value = state.money(state.trackedBalance),
            helper = "${state.accounts.size} bank account${if (state.accounts.size == 1) "" else "s"} tracked"
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
    item {
        MonthSelector(
            months = availableMonths(state),
            selected = state.selectedMonth,
            onSelected = onMonthSelected
        )
    }
    item {
        ActionPanel(
            title = "Monthly Summary",
            subtitle = "${state.selectedMonth.shortLabel()} income, expense, net total, graph, and category pie.",
            icon = Icons.Rounded.BarChart,
            action = "Open",
            onClick = { onOpenSummary(ScreenTab.Summary) }
        )
    }
    item {
        ScanStatusCard(
            isScanning = state.isScanningMessages,
            detectedCount = state.detectedDrafts.size,
            onScan = onScan
        )
    }
    if (state.detectedDrafts.isNotEmpty()) {
        item { SectionHeader("Detected Transactions", "Review") }
        items(state.detectedDrafts, key = { it.id }) {
            DetectedDraftRow(
                state = state,
                draft = it,
                onAccept = onAcceptDraft,
                onIgnore = onIgnoreDraft
            )
        }
    }
    item { SectionHeader("Recent Transactions", "Latest") }
    items(state.transactions.take(5), key = { it.id }) {
        TransactionRow(transaction = it, state = state)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.activityContent(state: FinanceUiState) {
    item { LargeTitle("Activity", "Add and review income or expenses by category.") }
    if (state.transactions.isEmpty()) {
        item { EmptyPanel("No transactions yet. Tap + to add your first income or expense.") }
    } else {
        items(state.transactions, key = { it.id }) {
            TransactionRow(transaction = it, state = state)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.budgetContent(
    state: FinanceUiState,
    onCreateBudget: (Boolean) -> Unit
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
        items(state.activeBudgets, key = { it.id }) {
            BudgetRow(budget = it, state = state)
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
    item { CategoryPieChart(state, categoryTotals(state)) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.settingsContent(
    state: FinanceUiState,
    onAddCategory: (Boolean) -> Unit,
    onCurrencySelected: (CurrencyOption) -> Unit
) {
    item {
        ProfileHeader(state)
    }
    item {
        SettingsGroup(
            title = "BANK ACCOUNTS",
            rows = state.accounts.map { "${it.name}  ${state.money(it.balance)}" }.ifEmpty { listOf("No accounts added") }
        )
    }
    item {
        CurrencySelector(
            selected = state.currency,
            onSelected = onCurrencySelected
        )
    }
    item {
        SettingsGroup(
            title = "CATEGORIES",
            rows = state.categories.map { if (it.isDefault) "${it.name}  Default" else "${it.name}  Custom" }
        )
    }
    item {
        Button(
            onClick = { onAddCategory(true) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFF001A42))
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFF001A42))
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
                    MoneyChip(it.name, selected = categoryId == it.id, onClick = { categoryId = it.id })
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFF001A42))
            ) {
                Text("Add Transaction", fontWeight = FontWeight.Bold)
            }
        }
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFF001A42))
            ) {
                Text("Save Budget", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategorySheet(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Navy900, contentColor = TextPrimary) {
        SheetContent(title = "Create Category") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category Name") },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = { onAdd(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFF001A42))
            ) {
                Text("Create Category", fontWeight = FontWeight.Bold)
            }
        }
    }
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

    ElevatedPanel {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Sms, MoneyGreen)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(draft.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("Detected from SMS", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                }
                    Text(draft.signedAmount(state.currency), color = draft.type.amountColor(), style = MaterialTheme.typography.titleLarge)
            }
            Text(draft.rawMessage, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            LabelText("CATEGORY")
            ChipRow {
                state.categories.forEach {
                    MoneyChip(it.name, selected = categoryId == it.id, onClick = { categoryId = it.id })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onAccept(draft.id, categoryId) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimarySoft, contentColor = Color(0xFF001A42)),
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
    Card(
        modifier = Modifier.fillMaxWidth().height(158.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = Color(0xCC00285D), style = MaterialTheme.typography.titleMedium)
            Text(
                value,
                color = Color(0xFF07265C),
                fontSize = 38.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(helper, color = Color(0xFF07265C), style = MaterialTheme.typography.bodyMedium)
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
private fun ScanStatusCard(isScanning: Boolean, detectedCount: Int, onScan: () -> Unit) {
    ActionPanel(
        title = if (isScanning) "Reading messages for today..." else "Message Detection",
        subtitle = if (isScanning) "Detecting NEFT, RTGS, debited, credited, and UPI alerts." else "$detectedCount pending detected transaction${if (detectedCount == 1) "" else "s"}",
        icon = Icons.Rounded.Sms,
        action = if (isScanning) "Scanning" else "Scan",
        onClick = onScan
    )
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
private fun TransactionRow(transaction: LedgerTransaction, state: FinanceUiState) {
    val category = state.categories.firstOrNull { it.id == transaction.categoryId }
    ElevatedPanel {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(category?.icon ?: Icons.AutoMirrored.Rounded.ReceiptLong, categoryColor(category?.name.orEmpty(), transaction.type))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    "${category?.name ?: "Miscellaneous"} | ${transaction.month().shortLabel()}${if (transaction.isAutoDetected) " | Auto" else ""}",
                    color = TextDim,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(transaction.signedAmount(state.currency), color = transaction.type.amountColor(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BudgetRow(budget: BudgetPlan, state: FinanceUiState) {
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
                trackColor = Navy800
            )
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
                    if (index != rows.lastIndex) HorizontalDivider(color = Color(0xFF283044), modifier = Modifier.padding(horizontal = 16.dp))
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy850),
        border = BorderStroke(1.dp, Color(0xFF242C40))
    ) {
        content()
    }
}

@Composable
private fun EmptyPanel(text: String) {
    ElevatedPanel {
        Text(text, color = TextDim, modifier = Modifier.padding(18.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
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
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryBlue,
            selectedLabelColor = Color(0xFF001A42),
            containerColor = Navy800,
            labelColor = TextMuted
        )
    )
}

@Composable
private fun BottomNavigation(selectedTab: ScreenTab, onTabSelected: (ScreenTab) -> Unit) {
    NavigationBar(containerColor = Navy850, tonalElevation = 0.dp) {
        ScreenTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimarySoft,
                    selectedTextColor = PrimarySoft,
                    indicatorColor = Color(0xFF213E78),
                    unselectedIconColor = TextDim.copy(alpha = 0.62f),
                    unselectedTextColor = TextDim.copy(alpha = 0.62f)
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
    unfocusedBorderColor = Color(0xFF424754),
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
    val prefix = if (type == TransactionType.Income) "+" else "-"
    return "$prefix${money(amount, currency)}"
}

private fun DetectedTransactionDraft.signedAmount(currency: CurrencyOption): String {
    val prefix = if (type == TransactionType.Income) "+" else "-"
    return "$prefix${money(amount, currency)}"
}

private fun TransactionType.amountColor(): Color {
    return if (this == TransactionType.Income) MoneyGreen else LossRed
}

private fun categoryColor(category: String, type: TransactionType): Color {
    if (type == TransactionType.Income) return MoneyGreen
    return when (category) {
        "Food", "Fuel" -> WarningAmber
        "Grocery", "Shopping" -> PrimarySoft
        "Rent" -> LossRed
        else -> TextMuted
    }
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

private val chartColors = listOf(PrimarySoft, MoneyGreen, WarningAmber, LossRed, PrimaryBlue)
