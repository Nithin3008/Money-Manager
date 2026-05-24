package com.moneymanager.app.ui

import android.app.DatePickerDialog
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.unit.Dp
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
import com.moneymanager.app.model.UiAccent
import com.moneymanager.app.model.UiSurface
import com.moneymanager.app.model.month
import com.moneymanager.app.model.shortLabel
import com.moneymanager.app.model.transactionDate
import com.moneymanager.app.data.SmsBankKeys
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
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.FileDownload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyManagerApp(viewModel: MoneyViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = viewModel.getExportData()
                    context.contentResolver.openOutputStream(uri)?.use { 
                        it.write(json.toByteArray()) 
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (json != null) {
                        viewModel.importData(json)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

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
                    contentColor = if (fabDark) Color(0xFF141414) else Color.White
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Crossfade(
            targetState = state.selectedTab,
            animationSpec = tween(140, easing = FastOutSlowInEasing),
            label = "tabTransition"
        ) { tab ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    BrandHeader(
                        userName = state.userName,
                        onOpenSettings = { viewModel.selectTab(ScreenTab.Settings) }
                    )
                }
                when (tab) {
                    ScreenTab.Dashboard -> dashboardContent(
                        state = state,
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
                    ScreenTab.Budget -> budgetContent(state, viewModel::deleteBudget)
                    ScreenTab.Summary -> summaryContent(
                        state = state,
                        onMonthSelected = viewModel::selectMonth,
                        onToggleSummaryAccount = viewModel::toggleSummaryAccountInFilter,
                        onClearSummaryAccountFilter = viewModel::clearSummaryAccountFilter
                    )
                    ScreenTab.Settings -> settingsContent(
                        state = state,
                        onAddCategory = viewModel::setCategorySheet,
                        onCurrencySelected = viewModel::selectCurrency,
                        onThemeSelected = viewModel::selectThemeMode,
                        onUiAccentSelected = viewModel::selectUiAccent,
                        onUiSurfaceSelected = viewModel::selectUiSurface,
                        onSalaryShiftChanged = viewModel::setSalaryShiftIncomeEnabled,
                        onSalaryWindowDaysChanged = viewModel::setSalaryShiftWindowDays,
                        onSalaryCategorySelected = viewModel::setSalaryCategoryId,
                        onSalaryKeywordsToggled = viewModel::setSalaryKeywordsForUncategorized,
                        onDeleteAccount = viewModel::deleteAccount,
                        onUpdateAccountBalance = viewModel::updateAccountBalance,
                        onAddAccount = viewModel::addBankAccount,
                        onDefaultAccountSelected = viewModel::setDefaultAccount,
                        onDeleteCategory = viewModel::deleteCategory,
                        onCategoryColorSelected = viewModel::updateCategoryColor,
                        onDeleteAllData = viewModel::deleteAllSavedData,
                        onExportData = { exportLauncher.launch("MoneyManager_Backup_${LocalDate.now()}.json") },
                        onImportData = { importLauncher.launch(arrayOf("application/json")) },
                        onExportSmsDebug = viewModel::exportPreviousMonthSmsDebug
                    )
                }
            }
        }
    }

    if (state.showTransactionSheet) {
        AddTransactionSheet(
            state = state,
            onDismiss = { viewModel.setTransactionSheet(false) },
            onAdd = viewModel::addTransaction,
            onTransfer = viewModel::addTransfer
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

    if (state.hasCompletedRegistration && state.accounts.isNotEmpty() && state.defaultAccountId == null) {
        DefaultBankPrompt(
            accounts = state.accounts,
            onSelected = viewModel::setDefaultAccount
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

private fun androidx.compose.foundation.lazy.LazyListScope.budgetContent(
    state: FinanceUiState,
    onDeleteBudget: (Long) -> Unit
) {
    item {
        LargeTitle("Budget", "Plan spending, protect savings, and track limits.")
    }
    item {
        FintrackBudgetHero(state)
    }
    if (state.activeBudgets.isEmpty()) {
        item { EmptyPanel("No budgets yet. Example: Grocery 5000, or Essentials for Grocery + Food + Fuel.") }
    } else {
        items(state.activeBudgets, key = { "budget_${it.id}" }) {
            BudgetRow(budget = it, state = state, onDelete = onDeleteBudget)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.settingsContent(
    state: FinanceUiState,
    onAddCategory: (Boolean) -> Unit,
    onCurrencySelected: (CurrencyOption) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onUiAccentSelected: (UiAccent) -> Unit,
    onUiSurfaceSelected: (UiSurface) -> Unit,
    onSalaryShiftChanged: (Boolean) -> Unit,
    onSalaryWindowDaysChanged: (Int) -> Unit,
    onSalaryCategorySelected: (Long?) -> Unit,
    onSalaryKeywordsToggled: (Boolean) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onUpdateAccountBalance: (Long, Double) -> Unit,
    onAddAccount: (String, Double) -> Unit,
    onDefaultAccountSelected: (Long?) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onCategoryColorSelected: (Long, String) -> Unit,
    onDeleteAllData: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onExportSmsDebug: () -> Unit
) {
    item {
        LargeTitle("Profile", "Manage your Money Manager setup and data.")
    }
    item {
        ProfileHeader(state)
    }
    item {
        AccountSettingsGroup(
            state = state,
            onDelete = onDeleteAccount,
            onUpdateBalance = onUpdateAccountBalance,
            onAddAccount = onAddAccount,
            onDefaultAccountSelected = onDefaultAccountSelected
        )
    }
    item {
        CurrencySelector(
            selected = state.currency,
            onSelected = onCurrencySelected
        )
    }
    item {
        UiAccentSelector(
            selected = state.uiAccent,
            darkMode = true,
            onSelected = onUiAccentSelected
        )
    }
    item {
        SummaryBehaviorSettings(
            salaryShiftEnabled = state.salaryShiftIncomeEnabled,
            windowDays = state.salaryShiftWindowDays,
            onSalaryShiftChanged = onSalaryShiftChanged,
            onWindowDaysChanged = onSalaryWindowDaysChanged
        )
    }
    item {
        SalaryCategorySettings(
            state = state,
            onSalaryCategorySelected = onSalaryCategorySelected,
            onSalaryKeywordsToggled = onSalaryKeywordsToggled
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
        ActionPanel(
            title = "Create category",
            subtitle = "Add a custom spending label with its own icon and color.",
            icon = Icons.Rounded.Category,
            action = "Create",
            onClick = { onAddCategory(true) }
        )
    }
    item {
        BackupRestorePanel(
            statusMessage = state.scanStatusMessage,
            onExport = onExportData,
            onImport = onImportData,
            onExportSmsDebug = onExportSmsDebug
        )
    }
    item {
        DeleteDataPanel(onDeleteAllData = onDeleteAllData)
    }
}

@Composable
private fun BackupRestorePanel(
    statusMessage: String,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onExportSmsDebug: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText("BACKUP & RESTORE")
        ElevatedPanel {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Save, PrimarySoft)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Export & Import Data", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Save your data to a file, or restore from a previous backup.",
                            color = TextDim,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onExport,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrimarySoft)
                    ) {
                        Text("Export", color = PrimarySoft, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onImport,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = primaryButtonColors()
                    ) {
                        Text("Import", fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = appDividerColor())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Sms, WarningAmber)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Parser SMS Debug", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Export likely finance SMS from the previous month for parser tuning.",
                            color = TextDim,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                OutlinedButton(
                    onClick = onExportSmsDebug,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WarningAmber)
                ) {
                    Text("Export Previous Month SMS", color = WarningAmber, fontWeight = FontWeight.Bold)
                }
                if (statusMessage.contains("export", ignoreCase = true)) {
                    Text(statusMessage, color = TextDim, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FintrackBudgetHero(state: FinanceUiState) {
    val limit = state.activeBudgets.sumOf { it.limitAmount }
    val spent = state.activeBudgets.sumOf { budget ->
        state.transactions
            .filter {
                it.type == TransactionType.Expense &&
                    !it.excludeFromSummary &&
                    it.month() == budget.month &&
                    it.categoryId in budget.categoryIds
            }
            .sumOf { it.amount }
    }
    val progress = (spent / limit.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    ElevatedPanel {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("This month budgeted", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                    Text(state.money(limit), color = PrimaryBlue, style = MaterialTheme.typography.headlineLarge, maxLines = 1)
                }
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Navy800),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(78.dp)) {
                        drawArc(
                            color = Color(0xFF303030),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 11f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = PrimaryBlue,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 11f, cap = StrokeCap.Round)
                        )
                    }
                    Text("${(progress * 100).toInt()}%", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FintrackMiniStat("Spent", state.money(spent), LossRed, Modifier.weight(1f))
                FintrackMiniStat("Left", state.money((limit - spent).coerceAtLeast(0.0)), PrimaryBlue, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FintrackMiniStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(Navy800, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = TextDim, style = MaterialTheme.typography.labelSmall)
        Text(value, color = color, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun DeleteDataPanel(onDeleteAllData: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText("DATA")
        ElevatedPanel {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Warning, LossRed)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Saved app data", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Clear scanned SMS transactions, accounts, budgets, categories, and settings.",
                            color = TextDim,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LossRed)
                ) {
                    Text("Delete All Saved Data", color = LossRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete all saved data?") },
            text = {
                Text(
                    "This removes the local app database: transactions, detected drafts, accounts, budgets, custom categories, and setup choices.",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onDeleteAllData()
                    }
                ) {
                    Text("Delete", color = LossRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Navy850,
            titleContentColor = TextPrimary,
            textContentColor = TextMuted
        )
    }
}

@Composable
private fun RegistrationScreen(onComplete: (String, List<Pair<String, Double>>, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    val accounts = remember { mutableStateListOf(AccountDraft()) }
    var defaultAccountIndex by remember { mutableStateOf(0) }
    val validWithOriginalIndex = accounts.mapIndexedNotNull { index, draft ->
        val amount = draft.balance.toDoubleOrNull()
        val accountName = draft.displayName()
        if (accountName.isBlank() || amount == null || amount < 0.0) null else index to (accountName to amount)
    }
    val defaultValidIndex = validWithOriginalIndex.indexOfFirst { it.first == defaultAccountIndex }
        .takeIf { it >= 0 }
        ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .statusBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(start = 24.dp, top = 54.dp, end = 24.dp, bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FintrackAuthHero()
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Full name") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        item { LabelText("BANK ACCOUNTS") }
        items(accounts.size) { index ->
            AccountDraftRow(
                account = accounts[index],
                accountNumber = index + 1,
                canRemove = accounts.size > 1,
                onNameChanged = { accounts[index] = accounts[index].copy(name = it) },
                onLastDigitsChanged = { accounts[index] = accounts[index].copy(lastDigits = it.filter(Char::isDigit).take(4)) },
                onBalanceChanged = { accounts[index] = accounts[index].copy(balance = it) },
                onRemove = {
                    if (accounts.size > 1) {
                        accounts.removeAt(index)
                        defaultAccountIndex = when {
                            defaultAccountIndex == index -> 0
                            defaultAccountIndex > index -> defaultAccountIndex - 1
                            else -> defaultAccountIndex
                        }.coerceIn(0, accounts.lastIndex)
                    }
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
                Text("Add another account")
            }
        }
        item {
            ElevatedPanel {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Default bank", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Used for Today, Summary, and new manual transactions. Change it later in Settings.",
                        color = TextDim,
                        style = MaterialTheme.typography.bodySmall
                    )
                    ChipRow {
                        accounts.forEachIndexed { index, account ->
                            MoneyChip(
                                label = account.displayName().ifBlank { "Account ${index + 1}" },
                                selected = defaultAccountIndex == index,
                                onClick = { defaultAccountIndex = index }
                            )
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = { onComplete(name, validWithOriginalIndex.map { it.second }, defaultValidIndex) },
                enabled = name.isNotBlank() && validWithOriginalIndex.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(12.dp),
                colors = primaryButtonColors()
            ) {
                Text("Get Started", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FintrackAuthHero() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(34.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FintrackLogoMark(size = 34.dp)
            Spacer(Modifier.width(8.dp))
            Text("Money Manager", color = PrimaryBlue, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(180.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Take Control of Your Finances",
                color = PrimaryBlue,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Welcome to Money Manager! Your personal financial companion. Take control of your money effortlessly.",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private data class AccountDraft(
    val name: String = "",
    val lastDigits: String = "",
    val balance: String = ""
) {
    fun displayName(): String = listOf(name.trim(), lastDigits.trim())
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

private enum class AddMoneyMode(val label: String) {
    Expense("Expense"),
    Income("Income"),
    Transfer("Transfer")
}

@Composable
private fun DefaultBankPrompt(accounts: List<BankAccount>, onSelected: (Long?) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Choose default bank") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "This bank is used for Today, Summary, and new manual transactions unless you choose another bank.",
                    color = TextMuted
                )
                accounts.forEach { account ->
                    OutlinedButton(
                        onClick = { onSelected(account.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, appBorderColor())
                    ) {
                        Text(account.name, color = TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Text("Use", color = PrimarySoft)
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = Navy850,
        titleContentColor = TextPrimary,
        textContentColor = TextMuted
    )
}

private fun transferBalanceText(
    state: FinanceUiState,
    fromAccountId: Long?,
    toAccountId: Long?,
    amount: Double
): String {
    val from = state.accounts.firstOrNull { it.id == fromAccountId }
    val to = state.accounts.firstOrNull { it.id == toAccountId }
    if (from == null || to == null) {
        return "Transfers need two different bank accounts. They are excluded from income and expense reports."
    }
    val debit = if (amount > 0.0) " -> ${state.money(from.balance - amount)}" else ""
    val credit = if (amount > 0.0) " -> ${state.money(to.balance + amount)}" else ""
    return "${from.name}: ${state.money(from.balance)}$debit | ${to.name}: ${state.money(to.balance)}$credit"
}

@Composable
private fun AccountDraftRow(
    account: AccountDraft,
    accountNumber: Int,
    canRemove: Boolean,
    onNameChanged: (String) -> Unit,
    onLastDigitsChanged: (String) -> Unit,
    onBalanceChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    ElevatedPanel {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = PrimarySoft)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Account $accountNumber",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (canRemove) {
                    TextButton(onClick = onRemove) {
                        Text("Remove", color = LossRed)
                    }
                }
            }
            OutlinedTextField(
                value = account.name,
                onValueChange = onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bank name") },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = account.lastDigits,
                onValueChange = onLastDigitsChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Last 4 account digits") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = account.balance,
                onValueChange = onBalanceChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Current balance") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Text(
                "Example: HDFC + 4466 helps match SMS like A/c XX4466. Balance becomes the current anchor.",
                color = TextDim,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    state: FinanceUiState,
    onDismiss: () -> Unit,
    onAdd: (String, Double, TransactionType, Long, Long?, String?, Boolean) -> Unit,
    onTransfer: (String, Double, Long?, Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AddMoneyMode.Expense) }
    var categoryId by remember { mutableStateOf(state.categories.first().id) }
    val defaultAccount = state.accounts.firstOrNull { it.id == state.defaultAccountId } ?: state.accounts.firstOrNull()
    var accountId by remember(state.defaultAccountId, state.accounts) { mutableStateOf(defaultAccount?.id) }
    var fromAccountId by remember(state.defaultAccountId, state.accounts) { mutableStateOf(defaultAccount?.id) }
    var toAccountId by remember(state.defaultAccountId, state.accounts) {
        mutableStateOf(state.accounts.firstOrNull { it.id != defaultAccount?.id }?.id)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val transactionType = if (mode == AddMoneyMode.Income) TransactionType.Income else TransactionType.Expense
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val transferReady = mode != AddMoneyMode.Transfer ||
        (parsedAmount > 0.0 && fromAccountId != null && toAccountId != null && fromAccountId != toAccountId)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy950,
        contentColor = TextPrimary,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 46.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Navy800)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .animateContentSize(tween(260, easing = FastOutSlowInEasing))
                .imePadding()
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Add Transaction", color = PrimaryBlue, style = MaterialTheme.typography.headlineMedium)
                    Text("Record a payment, income, or transfer.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            FintrackAmountCard(
                state = state,
                mode = mode,
                amount = amount,
                parsedAmount = parsedAmount,
                onAmountChanged = { amount = it }
            )

            FintrackModePicker(
                selected = mode,
                accountsAvailable = state.accounts.size,
                onSelected = { mode = it }
            )

            if (mode == AddMoneyMode.Transfer && state.accounts.size < 2) {
                Text(
                    "Add at least two bank accounts in Settings before creating transfers.",
                    color = WarningAmber,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ElevatedPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Details", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (mode == AddMoneyMode.Transfer) "Transfer note" else "Transaction name") },
                        singleLine = true,
                        colors = inputColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (mode == AddMoneyMode.Transfer) {
                ElevatedPanel {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LabelText("FROM ACCOUNT")
                        ChipRow {
                            state.accounts.forEach {
                                MoneyChip(it.name, selected = fromAccountId == it.id, onClick = {
                                    fromAccountId = it.id
                                    if (toAccountId == it.id) toAccountId = state.accounts.firstOrNull { account -> account.id != it.id }?.id
                                })
                            }
                        }
                        LabelText("TO ACCOUNT")
                        ChipRow {
                            state.accounts.forEach {
                                MoneyChip(it.name, selected = toAccountId == it.id, onClick = {
                                    toAccountId = it.id
                                    if (fromAccountId == it.id) fromAccountId = state.accounts.firstOrNull { account -> account.id != it.id }?.id
                                })
                            }
                        }
                        Text(
                            transferBalanceText(state, fromAccountId, toAccountId, parsedAmount),
                            color = TextDim,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                ElevatedPanel {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LabelText("CATEGORY")
                        ChipRow {
                            state.categories.forEach {
                                CategoryChoiceChip(it, transactionType, selected = categoryId == it.id, onClick = { categoryId = it.id })
                            }
                        }
                        if (state.accounts.isNotEmpty()) {
                            LabelText("BANK ACCOUNT")
                            ChipRow {
                                MoneyChip("None", selected = accountId == null, onClick = { accountId = null })
                                state.accounts.forEach {
                                    MoneyChip(it.name, selected = accountId == it.id, onClick = { accountId = it.id })
                                }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, appBorderColor()),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (mode == AddMoneyMode.Transfer) {
                            onTransfer(name, parsedAmount, fromAccountId, toAccountId)
                        } else {
                            onAdd(name, parsedAmount, transactionType, categoryId, accountId, null, false)
                        }
                    },
                    enabled = if (mode == AddMoneyMode.Transfer) transferReady else parsedAmount > 0.0 && name.isNotBlank(),
                    modifier = Modifier.weight(1.45f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = primaryButtonColors()
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (mode == AddMoneyMode.Transfer) "Save Transfer" else "Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FintrackAmountCard(
    state: FinanceUiState,
    mode: AddMoneyMode,
    amount: String,
    parsedAmount: Double,
    onAmountChanged: (String) -> Unit
) {
    val accent = when (mode) {
        AddMoneyMode.Income -> MoneyGreen
        AddMoneyMode.Expense -> LossRed
        AddMoneyMode.Transfer -> PrimaryBlue
    }
    val signedPreview = when {
        parsedAmount <= 0.0 -> state.money(0.0)
        mode == AddMoneyMode.Income -> "+${state.money(parsedAmount)}"
        mode == AddMoneyMode.Expense -> "-${state.money(parsedAmount)}"
        else -> state.money(parsedAmount)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
        border = BorderStroke(1.dp, PrimaryBlue)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(mode.label, color = Color(0xFF141414).copy(alpha = 0.68f), style = MaterialTheme.typography.labelMedium)
                    Text(
                        signedPreview,
                        color = Color(0xFF141414),
                        style = MaterialTheme.typography.headlineLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141414)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (mode) {
                            AddMoneyMode.Income -> Icons.Rounded.TrendingUp
                            AddMoneyMode.Expense -> Icons.Rounded.Wallet
                            AddMoneyMode.Transfer -> Icons.Rounded.AccountBalance
                        },
                        contentDescription = null,
                        tint = accent
                    )
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF141414),
                    unfocusedTextColor = Color(0xFF141414),
                    focusedBorderColor = Color(0xFF141414),
                    unfocusedBorderColor = Color(0xFF141414).copy(alpha = 0.28f),
                    focusedContainerColor = PrimaryBlue,
                    unfocusedContainerColor = PrimaryBlue,
                    focusedLabelColor = Color(0xFF141414),
                    unfocusedLabelColor = Color(0xFF141414).copy(alpha = 0.64f),
                    cursorColor = Color(0xFF141414)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun FintrackModePicker(
    selected: AddMoneyMode,
    accountsAvailable: Int,
    onSelected: (AddMoneyMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AddMoneyMode.entries.forEach { mode ->
            val enabled = mode != AddMoneyMode.Transfer || accountsAvailable >= 2
            val active = selected == mode
            val accent = when (mode) {
                AddMoneyMode.Income -> MoneyGreen
                AddMoneyMode.Expense -> LossRed
                AddMoneyMode.Transfer -> PrimaryBlue
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = enabled) { onSelected(mode) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (active) accent else Navy850),
                border = BorderStroke(1.dp, if (active) accent else appBorderColor())
            ) {
                Column(
                    Modifier.fillMaxSize().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        when (mode) {
                            AddMoneyMode.Income -> Icons.Rounded.TrendingUp
                            AddMoneyMode.Expense -> Icons.Rounded.Wallet
                            AddMoneyMode.Transfer -> Icons.Rounded.AccountBalance
                        },
                        contentDescription = null,
                        tint = if (active && isAmoledTheme()) Color(0xFF141414) else if (enabled) accent else TextDim
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        mode.label,
                        color = if (active && isAmoledTheme()) Color(0xFF141414) else if (enabled) TextPrimary else TextDim,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
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
    onSave: (Long, TransactionType, Long, String?) -> Unit,
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
    var description by remember(transaction.id) { mutableStateOf(transaction.description.orEmpty()) }
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
        containerColor = Navy950,
        contentColor = TextPrimary,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 46.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Navy800)
            )
        }
    ) {
        Box(Modifier.fillMaxWidth().imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(tween(260, easing = FastOutSlowInEasing))
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Transaction", color = PrimaryBlue, style = MaterialTheme.typography.headlineMedium)
                        Text("Tap any card, update type or category, then save.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = if (type == TransactionType.Income) PrimaryBlue else Navy850),
                border = BorderStroke(1.dp, if (type == TransactionType.Income) PrimaryBlue else LossRed.copy(alpha = 0.7f))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (type == TransactionType.Income) "Income" else "Expense",
                                color = if (type == TransactionType.Income) Color(0xFF141414).copy(alpha = 0.68f) else TextDim,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                signedAmount(transaction.amount, type, state.currency),
                                color = if (type == TransactionType.Income) Color(0xFF141414) else LossRed,
                                style = MaterialTheme.typography.headlineLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (type == TransactionType.Income) Color(0xFF141414) else LossRed.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (type == TransactionType.Income) Icons.Rounded.TrendingUp else Icons.Rounded.Wallet,
                                contentDescription = null,
                                tint = if (type == TransactionType.Income) PrimaryBlue else LossRed
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (type == TransactionType.Income) Color(0xFF141414).copy(alpha = 0.12f) else Navy800, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            transaction.name,
                            color = if (type == TransactionType.Income) Color(0xFF141414) else TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                dateLabel,
                                color = if (type == TransactionType.Income) Color(0xFF141414).copy(alpha = 0.64f) else TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (transaction.isCreditCardTransaction) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "CC",
                                    color = if (type == TransactionType.Income) Color(0xFF141414) else OtherIncomeGold,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

                ElevatedPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Transaction type", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionType.entries.forEach { option ->
                            EditTransactionTypeTile(
                                type = option,
                                selected = type == option,
                                onClick = { type = option },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

                ElevatedPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Category", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Text("Choose where this transaction belongs.", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = { showAllCategories = !showAllCategories }) {
                            Text(if (showAllCategories) "Frequent" else "Show all", color = PrimarySoft)
                        }
                    }
                    EditCategoryGrid(
                        categories = visibleCategories,
                        selectedCategoryId = categoryId,
                        type = type,
                        showCustomCategoryForm = showCustomCategoryForm,
                        onCategorySelected = { categoryId = it },
                        onCustomSelected = { showCustomCategoryForm = !showCustomCategoryForm }
                    )
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
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = primaryButtonColors()
                        ) {
                            Text("Create Category", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

                ElevatedPanel {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Description", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("Optional note for this transaction.", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it.take(180) },
                            modifier = Modifier.fillMaxWidth().height(104.dp),
                            label = { Text("Add a note") },
                            minLines = 3,
                            maxLines = 4,
                            colors = inputColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                if (transaction.rawMessage != null) {
                ElevatedPanel {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(Icons.Rounded.Sms, PrimaryBlue)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Detected message", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                Text("Review the original SMS when needed.", color = TextDim, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        OutlinedButton(
                            onClick = { showOriginalMessage = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, appBorderColor()),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Show Original Message")
                        }
                    }
                }
            }

            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Navy950.copy(alpha = 0.96f))
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onDelete(transaction.id) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LossRed.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onSave(transaction.id, type, categoryId, description) },
                    modifier = Modifier.weight(1.45f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = primaryButtonColors()
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save", fontWeight = FontWeight.Bold)
                }
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

@Composable
private fun EditTransactionTypeTile(
    type: TransactionType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (type == TransactionType.Income) MoneyGreen else LossRed
    Card(
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) accent else Navy800),
        border = BorderStroke(1.dp, if (selected) accent else appBorderColor())
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                if (type == TransactionType.Income) Icons.Rounded.TrendingUp else Icons.Rounded.Wallet,
                contentDescription = null,
                tint = if (selected && isAmoledTheme()) Color(0xFF141414) else accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                type.name,
                color = if (selected && isAmoledTheme()) Color(0xFF141414) else TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EditCategoryGrid(
    categories: List<CategoryItem>,
    selectedCategoryId: Long,
    type: TransactionType,
    showCustomCategoryForm: Boolean,
    onCategorySelected: (Long) -> Unit,
    onCustomSelected: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(2).forEach { rowCategories ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCategories.forEach { category ->
                    EditCategoryTile(
                        label = category.name,
                        color = categoryColor(category, type),
                        selected = category.id == selectedCategoryId,
                        onClick = { onCategorySelected(category.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCategories.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        EditCategoryTile(
            label = "Custom category",
            color = PrimaryBlue,
            selected = showCustomCategoryForm,
            onClick = onCustomSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EditCategoryTile(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) color.copy(alpha = 0.22f) else Navy800),
        border = BorderStroke(1.dp, if (selected) color else appBorderColor())
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = if (selected) TextPrimary else TextMuted,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.Check, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy900,
        contentColor = TextPrimary
    ) {
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
    val selectedIcon = remember(selectedIconKey) {
        MoneyIcons.allCategoryIcons.firstOrNull { it.key == selectedIconKey }
            ?: MoneyIcons.frequentCategoryIcons.first()
    }
    val selectedColorValue = colorFromHex(selectedColor)
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
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(selectedColorValue.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(selectedColorValue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            selectedIcon.icon,
                            contentDescription = null,
                            tint = if (isAmoledTheme()) Color(0xFF141414) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Create Category", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (name.isBlank()) "Choose an icon and color for a new category." else name.trim(),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category Name") },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LabelText("ICON")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MoneyIcons.frequentCategoryIcons, key = { it.key }) { option ->
                        CategoryIconChip(
                            option = option,
                            selected = option.key == selectedIconKey,
                            onClick = { selectedIconKey = option.key }
                        )
                    }
                }
            }

            LabelText("COLOR")
            ColorSwatches(selected = selectedColor, onSelected = { selectedColor = it })

            Button(
                onClick = { onAdd(name.trim(), selectedIconKey, selectedColor) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = primaryButtonColors()
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Category", fontWeight = FontWeight.Bold)
            }
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
            selectedContainerColor = PrimaryBlue,
            selectedLabelColor = if (isAmoledTheme()) Color(0xFF141414) else Color.White,
            selectedLeadingIconColor = if (isAmoledTheme()) Color(0xFF141414) else Color.White,
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
            val selectedColor = selected == colorHex
            val scale by animateFloatAsState(
                targetValue = if (selectedColor) 1.08f else 1f,
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "swatchScale"
            )
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (selectedColor) Color.White else color.copy(alpha = 0.16f))
                    .border(
                        width = if (selectedColor) 3.dp else 1.dp,
                        color = if (selectedColor) Color.White else color.copy(alpha = 0.72f),
                        shape = CircleShape
                    )
                    .clickable { onSelected(colorHex) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (selectedColor) 32.dp else 34.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedColor) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetContent(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .animateContentSize(tween(260, easing = FastOutSlowInEasing))
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BrandHeader(userName: String, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenSettings),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FintrackLogoMark(size = 30.dp)
        Spacer(Modifier.width(8.dp))
        Text("Money Manager", style = MaterialTheme.typography.headlineMedium, color = PrimarySoft)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Navy800), contentAlignment = Alignment.Center) {
            Text(userName.take(1).uppercase(), color = PrimarySoft, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FintrackLogoMark(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = size.toPx() * 0.055f
        val c = PrimaryBlue
        val points = listOf(
            Offset(size.toPx() * 0.5f, size.toPx() * 0.08f),
            Offset(size.toPx() * 0.86f, size.toPx() * 0.28f),
            Offset(size.toPx() * 0.86f, size.toPx() * 0.72f),
            Offset(size.toPx() * 0.5f, size.toPx() * 0.92f),
            Offset(size.toPx() * 0.14f, size.toPx() * 0.72f),
            Offset(size.toPx() * 0.14f, size.toPx() * 0.28f)
        )
        points.indices.forEach { index ->
            drawLine(c, points[index], points[(index + 1) % points.size], strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
        drawLine(c, points[0], points[3], strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(c, points[1], points[4], strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(c, points[2], points[5], strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
private fun HeroMetricCard(label: String, value: String, helper: String) {
    val dark = isAmoledTheme()
    val container = if (dark) PrimaryBlue else MaterialTheme.colorScheme.primaryContainer
    val labelColor = if (dark) Color(0xFF141414).copy(alpha = 0.72f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
    val valueColor = if (dark) Color(0xFF141414) else MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        modifier = Modifier.fillMaxWidth().height(158.dp),
        shape = MaterialTheme.shapes.extraLarge,
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
            Text(helper, color = if (dark) Color(0xFF141414).copy(alpha = 0.72f) else PrimarySoft, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SummaryBehaviorSettings(
    salaryShiftEnabled: Boolean,
    windowDays: Int,
    onSalaryShiftChanged: (Boolean) -> Unit,
    onWindowDaysChanged: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText("MONTHLY SUMMARY")
        ElevatedPanel {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Payroll month for income", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Credits in the last days of a month can count toward the next month on Summary.",
                            color = TextDim,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = salaryShiftEnabled,
                        onCheckedChange = onSalaryShiftChanged,
                        colors = appSwitchColors()
                    )
                }
                if (salaryShiftEnabled) {
                    Text("Payday window (last N days)", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                    ChipRow {
                        listOf(3, 5, 7, 10, 14).forEach { days ->
                            MoneyChip(
                                "$days d",
                                selected = windowDays == days,
                                onClick = { onWindowDaysChanged(days) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BankSmsSetupScreen(
    state: FinanceUiState,
    onMapBank: (String, Long) -> Unit,
    onCreateAccount: (String, String) -> Unit,
    onSkip: () -> Unit,
    onDone: () -> Unit
) {
    val banks = remember(state.discoveredSmsBanks, state.transactions) {
        val fromDiscovery = state.discoveredSmsBanks
        val fromTx = state.transactions.mapNotNull { it.smsBankLabel }.distinct()
        usefulSmsAccountLabels(fromDiscovery + fromTx)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LargeTitle("Link bank SMS", "Map each detected SMS account label to a saved account, or name it now.")
        if (banks.isEmpty()) {
            Text(
                "We will detect bank names from transaction SMS after you use the app. You can skip and map accounts anytime.",
                color = TextDim,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            banks.forEach { bank ->
                var newAccountName by remember(bank) { mutableStateOf("") }
                val examples = remember(bank, state.transactions) {
                    state.transactions
                        .filter { SmsBankKeys.normalize(it.smsBankLabel.orEmpty()) == SmsBankKeys.normalize(bank) }
                        .take(2)
                }
                ElevatedPanel {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(bank, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        if (examples.isNotEmpty()) {
                            examples.forEach { tx ->
                                Text(
                                    "${tx.name} - ${state.money(tx.amount)}",
                                    color = TextDim,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        LabelText("MAP TO ACCOUNT")
                        ChipRow {
                            state.accounts.forEach { account ->
                                val keyMatch =
                                    account.smsMatchKey?.let { SmsBankKeys.normalize(it) == SmsBankKeys.normalize(bank) } == true
                                MoneyChip(
                                    label = account.name,
                                    selected = keyMatch,
                                    onClick = { onMapBank(bank, account.id) }
                                )
                            }
                        }
                        LabelText("OR CREATE ACCOUNT")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newAccountName,
                                onValueChange = { newAccountName = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Account name") },
                                singleLine = true,
                                colors = inputColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    onCreateAccount(bank, newAccountName)
                                    newAccountName = ""
                                },
                                enabled = newAccountName.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = primaryButtonColors()
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                Text("Skip")
            }
            Button(onClick = onDone, modifier = Modifier.weight(1f), colors = primaryButtonColors()) {
                Text("Continue")
            }
        }
    }
}

private fun usefulSmsAccountLabels(labels: List<String>): List<String> {
    val cleaned = labels
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
    val accountSpecific = cleaned.filter { it.contains(" A/C ", ignoreCase = true) }
    val accountSpecificBases = accountSpecific
        .map { SmsBankKeys.normalize(it.substringBefore(" A/C ")) }
        .toSet()
    return cleaned
        .filterNot { SmsBankKeys.normalize(it) in setOf("BANK", "ACCOUNT", "A/C", "CARD") }
        .filterNot { label ->
            !label.contains(" A/C ", ignoreCase = true) &&
                SmsBankKeys.normalize(label) in accountSpecificBases
        }
        .sortedWith(compareByDescending<String> { it.contains(" A/C ", ignoreCase = true) }.thenBy { it })
}

@Composable
private fun SalaryCategorySettings(
    state: FinanceUiState,
    onSalaryCategorySelected: (Long?) -> Unit,
    onSalaryKeywordsToggled: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText("SALARY ON SUMMARY")
        ElevatedPanel {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The income tile highlights salary separately; other income appears in gold.",
                    color = TextDim,
                    style = MaterialTheme.typography.bodyMedium
                )
                LabelText("SALARY CATEGORY")
                ChipRow {
                    MoneyChip(
                        "None",
                        selected = state.salaryCategoryId == null,
                        onClick = { onSalaryCategorySelected(null) }
                    )
                    state.categories.forEach { cat ->
                        MoneyChip(
                            cat.name,
                            selected = state.salaryCategoryId == cat.id,
                            onClick = { onSalaryCategorySelected(cat.id) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Salary keywords for Uncategorized",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Treat matching payroll SMS as salary when still Uncategorized.",
                            color = TextDim,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = state.salaryKeywordsForUncategorized,
                        onCheckedChange = onSalaryKeywordsToggled,
                        colors = appSwitchColors()
                    )
                }
            }
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
private fun BudgetRow(budget: BudgetPlan, state: FinanceUiState, onDelete: (Long) -> Unit) {
    val spent = state.transactions
        .filter {
            it.type == TransactionType.Expense &&
                !it.excludeFromSummary &&
                it.month() == budget.month &&
                it.categoryId in budget.categoryIds
        }
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
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = { onDelete(budget.id) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete budget",
                        tint = LossRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                color = if (over) LossRed else PrimarySoft,
                trackColor = appTrackColor()
            )
        }
    }
}

@Composable
internal fun SegmentAmountRow(label: String, value: String, color: Color, progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.04f, 1f),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "segmentProgress"
    )
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
            Text(value, color = TextMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            color = color,
            trackColor = appTrackColor()
        )
    }
}

@Composable
private fun ProfileHeader(state: FinanceUiState) {
    ElevatedPanel {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)).background(PrimaryBlue), contentAlignment = Alignment.Center) {
                Text(state.userName.take(1).uppercase(), color = Color(0xFF141414), fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
            Text(state.userName, color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
            Text("Local finance tracking", color = TextDim, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SettingsGroup(title: String, rows: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText(title)
        ElevatedPanel(animateSize = false) {
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
private fun AccountSettingsGroup(
    state: FinanceUiState,
    onDelete: (Long) -> Unit,
    onUpdateBalance: (Long, Double) -> Unit,
    onAddAccount: (String, Double) -> Unit,
    onDefaultAccountSelected: (Long?) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var newAccountLastDigits by remember { mutableStateOf("") }
    var newAccountBalance by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelText("BANK ACCOUNTS")
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showAdd = !showAdd }) {
                Text(if (showAdd) "Cancel" else "Add bank", color = PrimarySoft)
            }
        }
        if (state.accounts.isNotEmpty()) {
            val defaultAccount = state.accounts.firstOrNull { it.id == state.defaultAccountId }
            ElevatedPanel {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Default bank", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        defaultAccount?.let { "Currently using ${it.name} for new entries and summary defaults." }
                            ?: "Choose the bank to use first for new entries and reports.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Only one bank can be default at a time.",
                        color = TextDim,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.accounts.forEach { account ->
                            DefaultAccountOption(
                                account = account,
                                selected = state.defaultAccountId == account.id,
                                onClick = { onDefaultAccountSelected(account.id) }
                            )
                        }
                    }
                }
            }
        }
        ElevatedPanel {
            Column {
                if (showAdd) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newAccountName,
                            onValueChange = { newAccountName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Bank name") },
                            singleLine = true,
                            colors = inputColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newAccountLastDigits,
                            onValueChange = { newAccountLastDigits = it.filter(Char::isDigit).take(4) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Last 4 account digits") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = inputColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newAccountBalance,
                            onValueChange = { newAccountBalance = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Current balance") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = inputColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                val displayName = listOf(newAccountName.trim(), newAccountLastDigits.trim())
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                onAddAccount(displayName, newAccountBalance.toDoubleOrNull() ?: -1.0)
                                newAccountName = ""
                                newAccountLastDigits = ""
                                newAccountBalance = ""
                                showAdd = false
                            },
                            enabled = newAccountName.isNotBlank() && (newAccountBalance.toDoubleOrNull() ?: -1.0) >= 0.0,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = primaryButtonColors()
                        ) {
                            Text("Add bank account", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (state.accounts.isNotEmpty()) {
                        HorizontalDivider(color = appDividerColor(), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                if (state.accounts.isEmpty()) {
                    Text("No accounts added", color = TextDim, modifier = Modifier.padding(16.dp))
                } else {
                    state.accounts.forEachIndexed { index, account ->
                        var expanded by remember(account.id) { mutableStateOf(false) }
                        var balanceText by remember(account.id, account.balance) { mutableStateOf(account.balance.toString()) }
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded }
                                    .padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(account.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                    Text(state.money(account.balance), color = TextDim, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(if (expanded) "Hide" else "Edit", color = PrimarySoft, style = MaterialTheme.typography.labelMedium)
                            }
                            if (expanded) {
                                Column(
                                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = balanceText,
                                        onValueChange = { balanceText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Current balance anchor") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = inputColors(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                balanceText.toDoubleOrNull()?.let { onUpdateBalance(account.id, it) }
                                            },
                                            enabled = (balanceText.toDoubleOrNull() ?: -1.0) >= 0.0,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = primaryButtonColors()
                                        ) {
                                            Text("Save balance", fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = { onDelete(account.id) }) {
                                            Text("Delete", color = LossRed)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onDefaultAccountSelected(account.id) },
                                        enabled = state.defaultAccountId != account.id,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (state.defaultAccountId == account.id) MoneyGreen else PrimarySoft
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (state.defaultAccountId == account.id) MoneyGreen else PrimarySoft
                                        )
                                    ) {
                                        Text(
                                            if (state.defaultAccountId == account.id) "Default bank" else "Set as default bank",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        "This value is treated as the current balance. History is reconstructed backward from it.",
                                        color = TextDim,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
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
private fun DefaultAccountOption(
    account: BankAccount,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PrimaryBlue.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (selected) PrimarySoft else appBorderColor(),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (selected) PrimarySoft else TextDim,
                    shape = CircleShape
                )
                .background(if (selected) PrimarySoft else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (isAmoledTheme()) Color(0xFF141414) else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(account.name, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(
                if (selected) "Default for new entries and reports" else "Tap to make default",
                color = if (selected) PrimarySoft else TextDim,
                style = MaterialTheme.typography.bodySmall
            )
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
                    onCheckedChange = { onSelected(if (it) ThemeMode.Dark else ThemeMode.Light) },
                    colors = appSwitchColors()
                )
            }
        }
    }
}

@Composable
private fun UiAccentSelector(
    selected: UiAccent,
    darkMode: Boolean,
    onSelected: (UiAccent) -> Unit
) {
    val accentOptions = remember { UiAccent.entries.take(10) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText("APP COLOR")
        ElevatedPanel {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Choose the highlight color for buttons, cards, and navigation.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accentOptions, key = { it.name }) { accent ->
                        val color = colorFromHex(if (darkMode) accent.darkHex else accent.lightHex)
                        val isSelected = selected == accent
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1f,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            label = "accentScale"
                        )
                        Column(
                            modifier = Modifier
                                .width(58.dp)
                                .scale(scale)
                                .clickable { onSelected(accent) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else color.copy(alpha = 0.16f))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else color.copy(alpha = 0.72f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 32.dp else 34.dp)
                                        .clip(CircleShape)
                                        .background(color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Text(
                                accent.label,
                                color = if (isSelected) TextPrimary else TextMuted,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UiSurfaceSelector(
    selected: UiSurface,
    darkMode: Boolean,
    onSelected: (UiSurface) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText("SURFACE STYLE")
        ElevatedPanel {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Background, sheets and large cards", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(UiSurface.entries, key = { it.name }) { surface ->
                        val bg = colorFromHex(if (darkMode) surface.darkBackgroundHex else surface.lightBackgroundHex)
                        val card = colorFromHex(if (darkMode) surface.darkCardHex else surface.lightCardHex)
                        val panel = colorFromHex(if (darkMode) surface.darkPanelHex else surface.lightPanelHex)
                        val isSelected = selected == surface
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1f,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            label = "surfaceScale"
                        )
                        Column(
                            modifier = Modifier
                                .width(86.dp)
                                .scale(scale)
                                .clickable { onSelected(surface) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 76.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) Color.White else appBorderColor())
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else appBorderColor(),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(bg)
                                        .padding(7.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.78f)
                                            .height(13.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(card)
                                            .align(Alignment.TopStart)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.58f)
                                            .height(13.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(panel)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                surface.label,
                                color = if (isSelected) TextPrimary else TextMuted,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color(0xFF141414),
    checkedTrackColor = PrimaryBlue,
    checkedBorderColor = PrimaryBlue,
    uncheckedThumbColor = TextMuted,
    uncheckedTrackColor = if (isAmoledTheme()) Navy800 else Color(0xFFE8EDF7),
    uncheckedBorderColor = appBorderColor()
)

@Composable
fun primaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = PrimaryBlue,
    contentColor = if (isAmoledTheme()) Color(0xFF141414) else Color.White
)

@Composable
private fun AddModeChip(mode: AddMoneyMode, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val accent = when (mode) {
        AddMoneyMode.Income -> MoneyGreen
        AddMoneyMode.Expense -> LossRed
        AddMoneyMode.Transfer -> PrimaryBlue
    }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "addModeChipScale"
    )
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(50),
        label = { Text(mode.label, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (selected) accent else accent.copy(alpha = 0.14f),
            selectedLabelColor = if (selected && isAmoledTheme()) Color(0xFF141414) else accent,
            containerColor = if (isAmoledTheme()) Navy800 else Color.White,
            labelColor = TextMuted,
            disabledContainerColor = if (isAmoledTheme()) Navy800.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.42f),
            disabledLabelColor = TextDim.copy(alpha = 0.62f)
        )
    )
}

@Composable
private fun BottomNavigation(selectedTab: ScreenTab, onTabSelected: (ScreenTab) -> Unit) {
    Card(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 18.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = BorderStroke(1.dp, appBorderColor())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScreenTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.08f else 1f,
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    label = "navScale"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(if (selected) PrimaryBlue else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) Color(0xFF141414) else TextDim,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Text(
                        tab.label,
                        color = if (selected) PrimaryBlue else TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = appBorderColor(),
    focusedContainerColor = Navy850,
    unfocusedContainerColor = Navy850,
    focusedLabelColor = PrimarySoft,
    unfocusedLabelColor = TextMuted,
    cursorColor = PrimaryBlue
)

private val categoryPalette = listOf(
    "#B4F077",
    "#8BE35D",
    "#FF7A8A",
    "#FFD166",
    "#B589FF",
    "#38D5E8",
    "#FF9F43",
    "#F472B6",
    "#4ADE80",
    "#60A5FA",
    "#A78BFA",
    "#F87171",
    "#2DD4BF",
    "#FBBF24",
    "#94A3B8"
)
