package com.moneymanager.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.moneymanager.app.model.AccountType
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
import com.moneymanager.app.model.RegistrationAccountInput
import com.moneymanager.app.model.ScreenTab
import com.moneymanager.app.model.ThemeMode
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.UiAccent
import com.moneymanager.app.model.UiSurface
import com.moneymanager.app.model.month
import com.moneymanager.app.model.shortLabel
import com.moneymanager.app.model.transactionDate
import com.moneymanager.app.data.SmsBankKeys
import com.moneymanager.app.ui.theme.LineColor
import com.moneymanager.app.ui.theme.LossRed
import com.moneymanager.app.ui.theme.MoneyGreen
import com.moneymanager.app.ui.theme.NavSolid
import com.moneymanager.app.ui.theme.Navy800
import com.moneymanager.app.ui.theme.OnAccent
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
        viewModel.scanForNewMessages()
        while (true) {
            delay(5 * 60 * 1000L)
            viewModel.scanForNewMessages()
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
            AnimatedVisibility(
                visible = state.selectedTab != ScreenTab.Settings,
                enter = scaleIn(tween(240, easing = EaseOutBack)) + fadeIn(tween(180)),
                exit = scaleOut(tween(160)) + fadeOut(tween(140))
            ) {
                Button(
                    onClick = {
                        when (state.selectedTab) {
                            ScreenTab.Budget -> viewModel.setBudgetSheet(true)
                            else -> viewModel.setTransactionSheet(true)
                        }
                    },
                    modifier = Modifier.size(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = OnAccent
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add", modifier = Modifier.size(30.dp))
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = state.selectedTab,
            transitionSpec = {
                // Slide the incoming screen in from the direction of travel along the nav bar:
                // moving to a higher-index tab enters from the right, lower index from the left.
                val forward = targetState.ordinal > initialState.ordinal
                val slide: (Int) -> Int = { full -> if (forward) full / 6 else -full / 6 }
                val slideOut: (Int) -> Int = { full -> if (forward) -full / 6 else full / 6 }
                (slideInHorizontally(tween(240, easing = FastOutSlowInEasing), slide) +
                    fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(240, easing = FastOutSlowInEasing), slideOut) +
                        fadeOut(tween(160))) using SizeTransform(clip = false)
            },
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
                if (tab == ScreenTab.Dashboard) {
                    item {
                        BrandHeader(
                            userName = state.userName,
                            onOpenSettings = { viewModel.selectTab(ScreenTab.Settings) }
                        )
                    }
                }
                when (tab) {
                    ScreenTab.Dashboard -> dashboardContent(
                        state = state,
                        onAcceptDraft = viewModel::acceptDetectedTransaction,
                        onIgnoreDraft = viewModel::ignoreDetectedTransaction,
                        onDeleteTransaction = viewModel::requestDeleteTransaction,
                        onEditTransaction = viewModel::requestEditTransactionCategory,
                        onDashboardPageSelected = viewModel::selectDashboardTransactionPage,
                        onDraftPageSelected = viewModel::selectDashboardDraftPage,
                        onSeeAllTransactions = { viewModel.selectTab(ScreenTab.Activity) }
                    )
                    ScreenTab.Activity -> activityContent(
                        state = state,
                        onDateFilterSelected = viewModel::setActivityDateFilter,
                        onDeleteTransaction = viewModel::requestDeleteTransaction,
                        onEditTransaction = viewModel::requestEditTransactionCategory,
                        onLoadMore = viewModel::loadMoreTransactions,
                        onScanSms = viewModel::scanForNewMessages
                    )
                    ScreenTab.Budget -> budgetContent(
                        state = state,
                        onDeleteBudget = viewModel::deleteBudget,
                        onAddBudget = { viewModel.setBudgetSheet(true) }
                    )
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
                        onCustomAccentApplied = viewModel::applyCustomAccent,
                        onPaletteColorAdded = viewModel::addPaletteColor,
                        onPaletteColorRemoved = viewModel::removePaletteColor,
                        onUiSurfaceSelected = viewModel::selectUiSurface,
                        onDeleteAccount = viewModel::deleteAccount,
                        onUpdateAccountBalance = viewModel::updateAccountBalance,
                        onAddAccount = viewModel::addBankAccount,
                        onAddCreditCard = viewModel::addCreditCardAccount,
                        onDefaultAccountSelected = viewModel::setDefaultAccount,
                        onDeleteCategory = viewModel::deleteCategory,
                        onUpdateCategory = viewModel::updateCategory,
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
            onSave = { name, amount, type, categoryId, accountId, description, timestamp ->
                viewModel.addTransaction(
                    name = name,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    description = description,
                    timestampMillis = timestamp
                )
            },
            onTransfer = viewModel::addTransfer,
            onCreateCategory = { viewModel.setCategorySheet(true) }
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
            palette = state.paletteColors,
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
            onSave = { id, name, amount, type, categoryId, accountId, timestamp, description ->
                viewModel.updateTransactionDetails(
                    transactionId = id,
                    type = type,
                    categoryId = categoryId,
                    description = description,
                    name = name,
                    amount = amount,
                    accountId = accountId,
                    timestampMillis = timestamp
                )
            },
            onDelete = viewModel::requestDeleteTransaction,
            onCreateCategory = { viewModel.setCategorySheet(true) }
        )
    }

    if (state.hasCompletedRegistration && state.bankAccounts.isNotEmpty() && state.defaultAccountId == null) {
        DefaultBankPrompt(
            accounts = state.bankAccounts,
            onSelected = viewModel::setDefaultAccount
        )
    }

    state.pendingDeleteTransactionId?.let { pendingId ->
        val pendingTransaction = state.transactions.firstOrNull { it.id == pendingId }
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteTransaction,
            title = { Text("Delete this transaction?") },
            text = {
                Text(
                    buildString {
                        pendingTransaction?.let {
                            append("${it.name} — ${state.money(it.amount)}. ")
                        }
                        append(
                            "The account balance will be adjusted back. This cannot be undone, " +
                                "and an SMS-detected transaction will not be re-imported by future scans."
                        )
                    },
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteTransaction) {
                    Text("Delete", color = LossRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteTransaction) {
                    Text("Cancel")
                }
            },
            containerColor = Navy850,
            titleContentColor = TextPrimary,
            textContentColor = TextMuted
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
    onDeleteBudget: (Long) -> Unit,
    onAddBudget: () -> Unit
) {
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Budgets",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Navy850)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
                Text(
                    state.selectedMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()),
                    color = TextMuted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
    item {
        FintrackBudgetHero(state)
    }
    item {
        SectionHeader("By category", "+ New budget", onAction = onAddBudget)
    }
    if (state.activeBudgets.isEmpty()) {
        item {
            EmptyStateCard(
                icon = Icons.Rounded.PieChart,
                title = "No budgets yet",
                caption = "Example: Grocery 5000, or Essentials for Grocery + Food + Fuel."
            )
        }
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
    onCustomAccentApplied: (String) -> Unit,
    onPaletteColorAdded: (String) -> Unit,
    onPaletteColorRemoved: (String) -> Unit,
    onUiSurfaceSelected: (UiSurface) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onUpdateAccountBalance: (Long, Double) -> Unit,
    onAddAccount: (String, Double) -> Unit,
    onAddCreditCard: (String, Double, List<String>) -> Unit,
    onDefaultAccountSelected: (Long?) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onUpdateCategory: (Long, String, String, String) -> Unit,
    onDeleteAllData: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onExportSmsDebug: () -> Unit
) {
    item {
        ProfileScreen(
            state = state,
            onAddCategory = onAddCategory,
            onCurrencySelected = onCurrencySelected,
            onThemeSelected = onThemeSelected,
            onUiAccentSelected = onUiAccentSelected,
            onCustomAccentApplied = onCustomAccentApplied,
            onPaletteColorAdded = onPaletteColorAdded,
            onPaletteColorRemoved = onPaletteColorRemoved,
            onUiSurfaceSelected = onUiSurfaceSelected,
            onDeleteAccount = onDeleteAccount,
            onUpdateAccountBalance = onUpdateAccountBalance,
            onAddAccount = onAddAccount,
            onAddCreditCard = onAddCreditCard,
            onDefaultAccountSelected = onDefaultAccountSelected,
            onDeleteCategory = onDeleteCategory,
            onUpdateCategory = onUpdateCategory,
            onDeleteAllData = onDeleteAllData,
            onExportData = onExportData,
            onImportData = onImportData,
            onExportSmsDebug = onExportSmsDebug
        )
    }
}

private enum class SettingsDetail(val title: String) {
    Accounts("Accounts"),
    DefaultAccount("Default account"),
    Currency("Currency"),
    Surface("Surface style"),
    Backup("Backup & restore")
}

@Composable
private fun ProfileScreen(
    state: FinanceUiState,
    onAddCategory: (Boolean) -> Unit,
    onCurrencySelected: (CurrencyOption) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onUiAccentSelected: (UiAccent) -> Unit,
    onCustomAccentApplied: (String) -> Unit,
    onPaletteColorAdded: (String) -> Unit,
    onPaletteColorRemoved: (String) -> Unit,
    onUiSurfaceSelected: (UiSurface) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onUpdateAccountBalance: (Long, Double) -> Unit,
    onAddAccount: (String, Double) -> Unit,
    onAddCreditCard: (String, Double, List<String>) -> Unit,
    onDefaultAccountSelected: (Long?) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onUpdateCategory: (Long, String, String, String) -> Unit,
    onDeleteAllData: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onExportSmsDebug: () -> Unit
) {
    var detail by remember { mutableStateOf<SettingsDetail?>(null) }
    var showCategories by remember { mutableStateOf(false) }
    var showColorLibrary by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    val defaultAccountName = state.accounts.firstOrNull { it.id == state.defaultAccountId }?.name ?: "None"

    Column(Modifier.fillMaxWidth()) {
        Text(
            "Profile",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        ProfileHeader(state)

        SettingsSectionLabel("Money setup")
        SettingsCard {
            SettingsRow(Icons.Rounded.AccountBalance, "Accounts", state.accounts.size.toString(), showDivider = true) { detail = SettingsDetail.Accounts }
            SettingsRow(Icons.Rounded.Category, "Categories", state.categories.size.toString(), showDivider = true) { showCategories = true }
            SettingsRow(Icons.Rounded.Star, "Default account", defaultAccountName, showDivider = true) { detail = SettingsDetail.DefaultAccount }
            SettingsRow(Icons.Rounded.CurrencyRupee, "Currency", state.currency.currencyCode, showDivider = false) { detail = SettingsDetail.Currency }
        }

        SettingsSectionLabel("Appearance")
        AppearanceCard(
            selectedTheme = state.themeMode,
            selectedAccent = state.uiAccent,
            customAccentHex = state.customAccentHex,
            darkMode = state.themeMode == ThemeMode.Dark,
            surfaceLabel = state.uiSurface.label,
            onThemeSelected = onThemeSelected,
            onAccentSelected = onUiAccentSelected,
            onCustomAccentApplied = onCustomAccentApplied,
            onOpenSurface = { detail = SettingsDetail.Surface },
            onOpenColors = { showColorLibrary = true }
        )

        SettingsSectionLabel("Data")
        SettingsCard {
            SettingsRow(Icons.Rounded.Backup, "Backup & restore", null, showDivider = false) { detail = SettingsDetail.Backup }
        }

        Spacer(Modifier.height(24.dp))
        DeleteDataPanel(onDeleteAllData = onDeleteAllData)
    }

    detail?.let { current ->
        SettingsDetailSheet(detail = current, onDismiss = { detail = null }) {
            when (current) {
                SettingsDetail.Accounts -> AccountSettingsGroup(
                    state = state,
                    onDelete = onDeleteAccount,
                    onUpdateBalance = onUpdateAccountBalance,
                    onAddAccount = onAddAccount,
                    onAddCreditCard = onAddCreditCard,
                    onDefaultAccountSelected = onDefaultAccountSelected
                )
                SettingsDetail.DefaultAccount -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Used for Home, Reports, and new manual transactions.",
                        color = TextDim,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    state.accounts.forEach { account ->
                        DefaultAccountOption(
                            account = account,
                            selected = state.defaultAccountId == account.id,
                            onClick = { onDefaultAccountSelected(account.id) }
                        )
                    }
                }
                SettingsDetail.Currency -> CurrencySelector(
                    selected = state.currency,
                    onSelected = onCurrencySelected
                )
                SettingsDetail.Surface -> UiSurfaceSelector(
                    selected = state.uiSurface,
                    darkMode = state.themeMode == ThemeMode.Dark,
                    onSelected = onUiSurfaceSelected
                )
                SettingsDetail.Backup -> BackupRestorePanel(
                    statusMessage = state.scanStatusMessage,
                    onExport = onExportData,
                    onImport = onImportData,
                    onExportSmsDebug = onExportSmsDebug
                )
            }
        }
    }

    if (showCategories) {
        CategoriesGridSheet(
            categories = state.categories,
            onDismiss = { showCategories = false },
            onEditCategory = { editingCategory = it },
            onNewCategory = { onAddCategory(true) }
        )
    }

    editingCategory?.let { category ->
        EditCategorySheet(
            category = category,
            palette = state.paletteColors,
            onDismiss = { editingCategory = null },
            onSave = { id, name, iconKey, colorHex ->
                onUpdateCategory(id, name, iconKey, colorHex)
                editingCategory = null
            },
            onDelete = { id ->
                onDeleteCategory(id)
                editingCategory = null
            }
        )
    }

    if (showColorLibrary) {
        ColorLibrarySheet(
            palette = state.paletteColors,
            categories = state.categories,
            accentHex = state.customAccentHex
                ?: if (state.themeMode == ThemeMode.Dark) state.uiAccent.darkHex else state.uiAccent.lightHex,
            onDismiss = { showColorLibrary = false },
            onAccentApplied = onCustomAccentApplied,
            onColorCreated = onPaletteColorAdded,
            onColorRemoved = onPaletteColorRemoved
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = TextDim,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, LineColor)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String?,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(22.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        value?.let {
            Text(it, color = TextDim, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextDim, modifier = Modifier.size(20.dp))
    }
    if (showDivider) HorizontalDivider(color = LineColor)
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextDim, fontSize = 11.5.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = appSwitchColors())
    }
    if (showDivider) HorizontalDivider(color = LineColor)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDetailSheet(
    detail: SettingsDetail,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy900,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(LineColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(detail.title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            content()
        }
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
    val limit = remember(state.activeBudgets) {
        state.activeBudgets.sumOf { it.limitAmount }
    }
    val spent = remember(state.activeBudgets, state.transactions) {
        state.activeBudgets.sumOf { budget ->
            state.transactions
                .filter {
                    it.type == TransactionType.Expense &&
                        !it.excludeFromSummary &&
                        it.month() == budget.month &&
                        it.categoryId in budget.categoryIds
                }
                .sumOf { it.amount }
        }
    }
    val progress = (spent / limit.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(
                "Total spent this month",
                color = OnAccent.copy(alpha = 0.72f),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    state.money(spent),
                    color = OnAccent,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    lineHeight = 40.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    " / ${state.money(limit)}",
                    color = OnAccent.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.16f))
            ) {
                DottedProgressFill(fraction = progress, color = OnAccent.copy(alpha = 0.9f))
            }
            Row(Modifier.padding(top = 16.dp)) {
                Column {
                    Text("Spent", color = OnAccent.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(state.money(spent), color = OnAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Left", color = OnAccent.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        state.money((limit - spent).coerceAtLeast(0.0)),
                        color = OnAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Wavy/beaded progress fill: a run of overlapping dots anchored to the bar's bottom edge. */
@Composable
private fun DottedProgressFill(fraction: Float, color: Color, dotRadius: Dp = 5.dp, step: Dp = 8.dp) {
    Canvas(Modifier.fillMaxSize()) {
        val fillWidth = size.width * fraction.coerceIn(0f, 1f)
        if (fillWidth <= 0f) return@Canvas
        val r = dotRadius.toPx()
        val stepPx = step.toPx()
        var x = stepPx / 2f
        while (x <= fillWidth) {
            drawCircle(color = color, radius = r, center = Offset(x, size.height))
            x += stepPx
        }
    }
}

@Composable
private fun DeleteDataPanel(onDeleteAllData: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showConfirm = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = LossRed.copy(alpha = if (isDarkTheme()) 0.10f else 0.07f)
        ),
        border = BorderStroke(1.dp, LossRed)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.DeleteForever,
                contentDescription = null,
                tint = LossRed,
                modifier = Modifier.size(22.dp)
            )
            Column(Modifier.weight(1f)) {
                Text("Delete all data", color = LossRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Cannot be undone", color = TextMuted, fontSize = 11.5.sp)
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
private fun RegistrationScreen(onComplete: (String, List<RegistrationAccountInput>, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    val accounts = remember { mutableStateListOf(AccountDraft()) }
    var defaultAccountIndex by remember { mutableStateOf(0) }
    val validWithOriginalIndex = accounts.mapIndexedNotNull { index, draft ->
        val amount = draft.balance.toDoubleOrNull()
        val accountName = draft.displayName()
        if (accountName.isBlank() || amount == null || amount < 0.0) {
            null
        } else {
            index to RegistrationAccountInput(
                name = accountName,
                balance = amount,
                type = draft.type
            )
        }
    }
    val defaultValidIndex = validWithOriginalIndex.indexOfFirst {
        it.first == defaultAccountIndex && it.second.type == AccountType.Bank
    }
        .takeIf { it >= 0 }
        ?: validWithOriginalIndex.indexOfFirst { it.second.type == AccountType.Bank }
    val hasValidBankAccount = validWithOriginalIndex.any { it.second.type == AccountType.Bank }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .statusBarsPadding()
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Savings, contentDescription = null, tint = OnAccent, modifier = Modifier.size(30.dp))
                }
                Text(
                    "Let's set up\nyour money.",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 34.sp,
                    modifier = Modifier.padding(top = 22.dp)
                )
                Text(
                    "We'll track balances from today and read your bank SMS to fill in the rest — nothing leaves your phone.",
                    color = TextDim,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            item {
                Text(
                    "YOUR NAME",
                    color = TextDim,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(top = 28.dp, bottom = 10.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Navy850)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = TextDim, modifier = Modifier.size(21.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue),
                        decorationBox = { inner ->
                            Box {
                                if (name.isEmpty()) {
                                    Text("Your name", color = TextDim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                                inner()
                            }
                        }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "STARTING ACCOUNTS",
                        color = TextDim,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { accounts.add(AccountDraft()) }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("+ Add", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            items(accounts.size) { index ->
                Box(Modifier.padding(bottom = 12.dp)) {
                    AccountDraftRow(
                        account = accounts[index],
                        accountNumber = index + 1,
                        isDefault = defaultAccountIndex == index,
                        canRemove = accounts.size > 1,
                        onTypeChanged = { type ->
                            accounts[index] = accounts[index].copy(type = type)
                            if (type == AccountType.CreditCard && defaultAccountIndex == index) {
                                defaultAccountIndex = accounts.indexOfFirst { it.type == AccountType.Bank }
                                    .takeIf { it >= 0 }
                                    ?: 0
                            }
                        },
                        onNameChanged = { accounts[index] = accounts[index].copy(name = it) },
                        onLastDigitsChanged = { accounts[index] = accounts[index].copy(lastDigits = it.filter(Char::isDigit).take(4)) },
                        onBalanceChanged = { accounts[index] = accounts[index].copy(balance = it) },
                        onSetDefault = { defaultAccountIndex = index },
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
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Navy850)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Text(
                        "SMS is read on-device only. Older history backfills but never changes your entered balances.",
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Navy950)
                .drawBehind {
                    drawLine(LineColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                }
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
            ) {
                Box(Modifier.size(width = 22.dp, height = 6.dp).clip(RoundedCornerShape(999.dp)).background(PrimaryBlue))
                Box(Modifier.size(6.dp).clip(CircleShape).background(Navy800))
                Box(Modifier.size(6.dp).clip(CircleShape).background(Navy800))
            }
            val enabled = name.isNotBlank() && validWithOriginalIndex.isNotEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (enabled) PrimaryBlue else PrimaryBlue.copy(alpha = 0.4f))
                    .clickable(enabled = enabled) {
                        onComplete(name, validWithOriginalIndex.map { it.second }, defaultValidIndex)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Text("Continue", color = OnAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = OnAccent, modifier = Modifier.size(21.dp))
            }
        }
    }
}

private data class AccountDraft(
    val name: String = "",
    val lastDigits: String = "",
    val balance: String = "",
    val type: AccountType = AccountType.Bank
) {
    fun displayName(): String = listOf(name.trim(), lastDigits.trim())
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

private enum class AddMoneyMode(val label: String) {
    Expense("Expense"),
    Income("Income"),
    Investment("Investment"),
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
    val fromBalance = from.balance
    val toBalance = to.balance
    val debit = if (amount > 0.0) " -> ${state.money(fromBalance - amount)}" else ""
    val credit = if (amount > 0.0) " -> ${state.money(toBalance + amount)}" else ""
    return "${from.name}: ${state.money(fromBalance)}$debit | ${to.name}: ${state.money(toBalance)}$credit"
}

@Composable
private fun AccountDraftRow(
    account: AccountDraft,
    accountNumber: Int,
    isDefault: Boolean,
    canRemove: Boolean,
    onTypeChanged: (AccountType) -> Unit,
    onNameChanged: (String) -> Unit,
    onLastDigitsChanged: (String) -> Unit,
    onBalanceChanged: (String) -> Unit,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, if (isDefault) PrimaryBlue else LineColor)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Navy850),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = TextMuted, modifier = Modifier.size(21.dp))
                }
                Text(
                    "${account.type.label} $accountNumber",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (canRemove) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = TextDim, modifier = Modifier.size(18.dp))
                    }
                }
            }
            ChipRow {
                AccountType.entries.forEach { type ->
                    MoneyChip(
                        label = type.label,
                        selected = account.type == type,
                        onClick = { onTypeChanged(type) }
                    )
                }
            }
            OutlinedTextField(
                value = account.name,
                onValueChange = onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (account.type == AccountType.Bank) "Bank name" else "Card name") },
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = account.lastDigits,
                onValueChange = onLastDigitsChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (account.type == AccountType.Bank) "Last 4 account digits" else "Last 4 card digits") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = account.balance,
                onValueChange = onBalanceChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (account.type == AccountType.Bank) "Current balance" else "Current outstanding") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = inputColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onSetDefault)
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isDefault) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = null,
                    tint = if (isDefault) PrimaryBlue else TextDim,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    if (isDefault) "Default account" else "Set as default",
                    color = if (isDefault) PrimaryBlue else TextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    state: FinanceUiState,
    onDismiss: () -> Unit,
    onSave: (String, Double, TransactionType, Long, Long?, String?, Long?) -> Unit,
    onTransfer: (String, Double, Long?, Long?) -> Unit,
    onCreateCategory: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AddMoneyMode.Expense) }
    var categoryId by remember { mutableStateOf(state.categories.first().id) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val defaultAccount = state.accounts.firstOrNull { it.id == state.defaultAccountId } ?: state.accounts.firstOrNull()
    var accountId by remember(state.defaultAccountId, state.accounts) { mutableStateOf(defaultAccount?.id) }
    var fromAccountId by remember(state.defaultAccountId, state.accounts) { mutableStateOf(defaultAccount?.id) }
    var toAccountId by remember(state.defaultAccountId, state.accounts) {
        mutableStateOf(state.accounts.firstOrNull { it.id != defaultAccount?.id }?.id)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()
    // Animate the slide-down before the state flag removes the sheet from composition.
    fun closeSheet(after: () -> Unit) {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { after() }
    }
    val transactionType = if (mode == AddMoneyMode.Income) TransactionType.Income else TransactionType.Expense
    val investmentCategoryId = remember(state.categories) {
        state.investmentCategoryIds.firstOrNull() ?: categoryId
    }
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val transferReady = mode != AddMoneyMode.Transfer ||
        (parsedAmount > 0.0 && fromAccountId != null && toAccountId != null && fromAccountId != toAccountId)
    val saveEnabled = if (mode == AddMoneyMode.Transfer) transferReady else parsedAmount > 0.0 && name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy950,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(tween(260, easing = FastOutSlowInEasing))
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SheetTitleBar(title = "New transaction", onDismiss = { closeSheet(onDismiss) })

                FintrackModePicker(selected = mode, onSelected = { mode = it })

                FintrackAmountCard(
                    state = state,
                    mode = mode,
                    amount = amount,
                    parsedAmount = parsedAmount,
                    onAmountChanged = { amount = it }
                )

                if (mode == AddMoneyMode.Transfer && state.accounts.size < 2) {
                    Text(
                        "Add at least two bank accounts in Settings before creating transfers.",
                        color = WarningAmber,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                TxNameField(
                    value = name,
                    placeholder = when (mode) {
                        AddMoneyMode.Transfer -> "Transfer note"
                        AddMoneyMode.Investment -> "Investment name"
                        else -> "What was it for?"
                    },
                    onValueChange = { name = it }
                )

                if (mode == AddMoneyMode.Transfer) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SheetSectionLabel("From account")
                        ChipRow {
                            state.accounts.forEach {
                                MoneyChip(it.name, selected = fromAccountId == it.id, onClick = {
                                    fromAccountId = it.id
                                    if (toAccountId == it.id) toAccountId = state.accounts.firstOrNull { account -> account.id != it.id }?.id
                                })
                            }
                        }
                        SheetSectionLabel("To account")
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
                } else if (mode == AddMoneyMode.Investment) {
                    Text(
                        "Saved separately from income, spending, budgets, and bank balances.",
                        color = TextDim,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SheetSectionLabel("Category")
                        TxCategoryChips(
                            categories = state.categories,
                            selectedId = categoryId,
                            onSelect = { categoryId = it },
                            onNew = onCreateCategory
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mode != AddMoneyMode.Transfer && mode != AddMoneyMode.Investment) {
                        AccountTile(state = state, accountId = accountId, onSelect = { accountId = it }, modifier = Modifier.weight(1f))
                    }
                    DateTile(date = selectedDate, onDateSelected = { selectedDate = it }, modifier = Modifier.weight(1f))
                }

                SheetSectionLabel("Notes")
                TxNotesField(value = notes, onValueChange = { notes = it })
            }

            SheetBottomAction(
                text = "Save ${mode.label.lowercase()}",
                enabled = saveEnabled,
                onClick = {
                    val timestamp = if (selectedDate == LocalDate.now()) null
                        else selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    closeSheet {
                        when (mode) {
                            AddMoneyMode.Transfer -> onTransfer(name, parsedAmount, fromAccountId, toAccountId)
                            AddMoneyMode.Investment -> onSave(name, parsedAmount, TransactionType.Expense, investmentCategoryId, null, notes, timestamp)
                            else -> onSave(name, parsedAmount, transactionType, categoryId, accountId, notes, timestamp)
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        Modifier
            .padding(top = 12.dp)
            .size(width = 40.dp, height = 5.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(LineColor)
    )
}

@Composable
private fun SheetTitleBar(
    title: String,
    onDismiss: () -> Unit,
    icon: ImageVector = Icons.Rounded.Close,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Navy850)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(22.dp))
        }
        Text(
            title,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(text, color = TextDim, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
}

@Composable
private fun TxNameField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Navy850)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.EditNote, contentDescription = null, tint = TextDim, modifier = Modifier.size(21.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = TextDim, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    inner()
                }
            }
        )
    }
}

@Composable
private fun TxNotesField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Navy850)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.StickyNote2, contentDescription = null, tint = TextDim, modifier = Modifier.size(21.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("Add a note, tag or reminder…", color = TextDim, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
                    inner()
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TxCategoryChips(
    categories: List<CategoryItem>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onNew: () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categories.forEach { category ->
            val selected = category.id == selectedId
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) PrimaryBlue else Navy850)
                    .border(1.dp, if (selected) PrimaryBlue else LineColor, RoundedCornerShape(999.dp))
                    .clickable { onSelect(category.id) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(category.icon, contentDescription = null, tint = if (selected) OnAccent else TextMuted, modifier = Modifier.size(18.dp))
                Text(category.name, color = if (selected) OnAccent else TextMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(999.dp))
                .dashedBorder(LineColor, 999.dp)
                .clickable(onClick = onNew)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = TextDim, modifier = Modifier.size(18.dp))
            Text("New", color = TextDim, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AccountTile(state: FinanceUiState, accountId: Long?, onSelect: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val label = state.accounts.firstOrNull { it.id == accountId }?.name ?: "None"
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Navy850)
                .clickable { open = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
            Column {
                Text("Account", color = TextDim, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); open = false })
            state.accounts.forEach { account ->
                DropdownMenuItem(text = { Text(account.name) }, onClick = { onSelect(account.id); open = false })
            }
        }
    }
}

@Composable
private fun DateTile(date: LocalDate, onDateSelected: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }
    val label = if (date == LocalDate.now()) "Today · ${date.monthDayLabel()}" else date.mediumDateLabel()
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Navy850)
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Event, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Column {
            Text("Date", color = TextDim, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
            Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    if (showPicker) {
        AppDatePickerSheet(
            initialDate = date,
            maxDate = LocalDate.now(),
            onDismiss = { showPicker = false },
            onConfirm = { onDateSelected(it); showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppDatePickerSheet(
    initialDate: LocalDate,
    maxDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = LocalDate.now()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy900,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 18.dp)) {
                val thisMonthStart = YearMonth.now().atDay(1)
                DatePickerQuickChip("Today", selectedDate == today, Modifier.weight(1f)) {
                    selectedDate = today; displayedMonth = YearMonth.from(today)
                }
                DatePickerQuickChip("Yesterday", selectedDate == today.minusDays(1), Modifier.weight(1f)) {
                    val d = today.minusDays(1); selectedDate = d; displayedMonth = YearMonth.from(d)
                }
                DatePickerQuickChip("This month", selectedDate == thisMonthStart, Modifier.weight(1f)) {
                    selectedDate = thisMonthStart; displayedMonth = YearMonth.from(thisMonthStart)
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column {
                    Text("SELECTED DATE", color = TextDim, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
                    Text(
                        selectedDate.format(dateReadoutFormatter),
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.EditCalendar, contentDescription = null, tint = OnAccent, modifier = Modifier.size(22.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text(
                    "${displayedMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                val canGoNext = displayedMonth < YearMonth.from(maxDate)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Navy800)
                            .clickable { displayedMonth = displayedMonth.minusMonths(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (canGoNext) Navy800 else Navy800.copy(alpha = 0.4f))
                            .clickable(enabled = canGoNext) { displayedMonth = displayedMonth.plusMonths(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = "Next month",
                            tint = if (canGoNext) TextMuted else TextDim,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                    Text(
                        label,
                        color = TextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                    )
                }
            }

            val leadBlanks = displayedMonth.atDay(1).dayOfWeek.value - 1
            val daysInMonth = displayedMonth.lengthOfMonth()
            val totalCells = leadBlanks + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - leadBlanks + 1
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..daysInMonth) {
                                val cellDate = displayedMonth.atDay(dayNum)
                                val isSelected = cellDate == selectedDate
                                val isToday = cellDate == today
                                val isFuture = cellDate > maxDate
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) PrimaryBlue else Color.Transparent)
                                        .clickable(enabled = !isFuture) { selectedDate = cellDate },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        dayNum.toString(),
                                        color = when {
                                            isSelected -> OnAccent
                                            isFuture -> TextDim.copy(alpha = 0.4f)
                                            else -> TextPrimary
                                        },
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isToday && !isSelected) {
                                        Box(
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 5.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryBlue)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Row(
                    Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Navy800)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cancel", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(PrimaryBlue)
                        .clickable { onConfirm(selectedDate) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(20.dp))
                    Text("Set date", color = OnAccent, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DatePickerQuickChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PrimaryBlue else Navy800)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) OnAccent else TextMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SheetBottomAction(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Navy950)
            .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (enabled) PrimaryBlue else PrimaryBlue.copy(alpha = 0.4f))
                .clickable(enabled = enabled, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(21.dp))
            Text(text, color = OnAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
    val display = when {
        parsedAmount <= 0.0 && amount.isBlank() -> state.money(0.0)
        else -> state.money(parsedAmount)
    }
    fun bump(delta: Int) {
        val next = ((amount.toDoubleOrNull() ?: 0.0) + delta)
        onAmountChanged(if (next % 1.0 == 0.0) next.toLong().toString() else next.toString())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Amount", color = OnAccent.copy(alpha = 0.72f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            androidx.compose.foundation.text.BasicTextField(
                value = amount,
                onValueChange = onAmountChanged,
                modifier = Modifier.padding(top = 4.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = OnAccent,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(OnAccent),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (amount.isEmpty()) {
                            Text(
                                display,
                                color = OnAccent,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAddChip("+100") { bump(100) }
                QuickAddChip("+500") { bump(500) }
                QuickAddChip("+1k") { bump(1000) }
            }
        }
    }
}

@Composable
private fun QuickAddChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = OnAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FintrackModePicker(
    selected: AddMoneyMode,
    onSelected: (AddMoneyMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Navy850)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AddMoneyMode.entries.forEach { mode ->
            val active = selected == mode
            val activeColor = when (mode) {
                AddMoneyMode.Income -> MoneyGreen
                AddMoneyMode.Expense -> LossRed
                AddMoneyMode.Investment -> OtherIncomeGold
                AddMoneyMode.Transfer -> PrimaryBlue
            }
            val activeInk = when (mode) {
                AddMoneyMode.Expense, AddMoneyMode.Income -> Color.White
                else -> OnAccent
            }
            val icon = when (mode) {
                AddMoneyMode.Income -> Icons.Rounded.NorthEast
                AddMoneyMode.Expense -> Icons.Rounded.SouthWest
                AddMoneyMode.Investment -> Icons.AutoMirrored.Rounded.TrendingUp
                AddMoneyMode.Transfer -> Icons.Rounded.SwapHoriz
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) activeColor else Color.Transparent)
                    .clickable { onSelected(mode) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (active) activeInk else TextMuted,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    mode.label,
                    color = if (active) activeInk else TextMuted,
                    fontSize = 12.5.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1
                )
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
    onSave: (Long, String, Double, TransactionType, Long, Long?, Long?, String?) -> Unit,
    onDelete: (Long) -> Unit,
    onCreateCategory: () -> Unit
) {
    if (transaction == null) return
    var name by remember(transaction.id) { mutableStateOf(transaction.name) }
    var amount by remember(transaction.id) { mutableStateOf(editableAmount(transaction.amount)) }
    var type by remember(transaction.id) { mutableStateOf(transaction.type) }
    var categoryId by remember(transaction.id) { mutableStateOf(transaction.categoryId) }
    var accountId by remember(transaction.id) { mutableStateOf(transaction.accountId) }
    var selectedDate by remember(transaction.id) { mutableStateOf(transaction.transactionDate()) }
    var notes by remember(transaction.id) { mutableStateOf(transaction.description.orEmpty()) }
    var showOriginalMessage by remember { mutableStateOf(false) }
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val originalDate = remember(transaction.id) { transaction.transactionDate() }
    val metaLabel = remember(transaction.id) {
        val bank = transaction.smsBankLabel
            ?: state.accounts.firstOrNull { it.id == transaction.accountId }?.name
            ?: "SMS"
        "Auto-added from SMS · $bank · ${transaction.transactionDate().mediumDateLabel()}"
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()
    // Button closes must play the slide-down animation before the state flag removes the
    // sheet; flipping the flag directly yanks it out mid-frame with no exit animation.
    fun closeSheet(after: () -> Unit) {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { after() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy950,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(tween(260, easing = FastOutSlowInEasing))
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SheetTitleBar(title = "Edit transaction", onDismiss = { closeSheet(onDismiss) }) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Navy850)
                            .clickable { onDelete(transaction.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(21.dp))
                    }
                }

                if (transaction.isAutoDetected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Navy850)
                            .clickable(enabled = transaction.rawMessage != null) { showOriginalMessage = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = TextDim, modifier = Modifier.size(17.dp))
                        Text(metaLabel, color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                TxTypePicker(type = type, onSelect = { type = it })

                FintrackAmountCard(
                    state = state,
                    mode = if (type == TransactionType.Income) AddMoneyMode.Income else AddMoneyMode.Expense,
                    amount = amount,
                    parsedAmount = parsedAmount,
                    onAmountChanged = { amount = it }
                )

                TxNameField(value = name, placeholder = "Transaction name", onValueChange = { name = it })

                SheetSectionLabel("Category")
                TxCategoryChips(
                    categories = state.categories,
                    selectedId = categoryId,
                    onSelect = { categoryId = it },
                    onNew = onCreateCategory
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountTile(state = state, accountId = accountId, onSelect = { accountId = it }, modifier = Modifier.weight(1f))
                    DateTile(date = selectedDate, onDateSelected = { selectedDate = it }, modifier = Modifier.weight(1f))
                }

                SheetSectionLabel("Notes")
                TxNotesField(value = notes, onValueChange = { notes = it })
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Navy950)
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val saveEnabled = parsedAmount > 0.0 && name.isNotBlank()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (saveEnabled) PrimaryBlue else PrimaryBlue.copy(alpha = 0.4f))
                        .clickable(enabled = saveEnabled) {
                            val timestamp = if (selectedDate == originalDate) transaction.timestampMillis
                                else selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            closeSheet { onSave(transaction.id, name, parsedAmount, type, categoryId, accountId, timestamp, notes) }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(21.dp))
                    Text("Save changes", color = OnAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(LossRed.copy(alpha = if (isDarkTheme()) 0.12f else 0.10f))
                        .clickable { onDelete(transaction.id) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = LossRed, modifier = Modifier.size(20.dp))
                    Text("Delete transaction", color = LossRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

private fun editableAmount(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

@Composable
private fun TxTypePicker(type: TransactionType, onSelect: (TransactionType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Navy850)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val options = listOf(
            Triple(TransactionType.Expense, Icons.Rounded.SouthWest, LossRed),
            Triple(TransactionType.Income, Icons.Rounded.NorthEast, MoneyGreen)
        )
        options.forEach { (option, icon, activeColor) ->
            val active = type == option
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) activeColor else Color.Transparent)
                    .clickable { onSelect(option) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                Icon(icon, contentDescription = null, tint = if (active) Color.White else TextMuted, modifier = Modifier.size(18.dp))
                Text(
                    option.name,
                    color = if (active) Color.White else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold
                )
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
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val selectedCategoryNames = remember(state.categories, selectedCategoryIds.toList()) {
        state.categories
            .filter { it.id in selectedCategoryIds }
            .joinToString { it.name }
    }

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
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
                .animateContentSize(tween(260, easing = FastOutSlowInEasing))
                .imePadding()
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SheetHeader(
                title = "Create Budget",
                subtitle = "Set a monthly limit and attach the categories it should watch.",
                onDismiss = onDismiss
            )

            BudgetPreviewCard(
                state = state,
                amount = parsedAmount,
                selectedCategoryCount = selectedCategoryIds.size,
                selectedCategoryNames = selectedCategoryNames
            )

            ElevatedPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Budget details", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Budget name") },
                        singleLine = true,
                        colors = inputColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Monthly limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = inputColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            ElevatedPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Categories", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (selectedCategoryIds.isEmpty()) "Pick at least one category." else "$selectedCategoryNames selected",
                                color = TextDim,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("${selectedCategoryIds.size}", color = PrimaryBlue, style = MaterialTheme.typography.headlineSmall)
                    }
                    BudgetCategoryGrid(
                        categories = state.categories,
                        selectedCategoryIds = selectedCategoryIds,
                        onToggleCategory = { categoryId ->
                            if (categoryId in selectedCategoryIds) {
                                selectedCategoryIds.remove(categoryId)
                            } else {
                                selectedCategoryIds.add(categoryId)
                            }
                        }
                    )
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
                    onClick = { onAdd(name.trim(), parsedAmount, selectedCategoryIds.toSet()) },
                    enabled = name.isNotBlank() && parsedAmount > 0.0 && selectedCategoryIds.isNotEmpty(),
                    modifier = Modifier.weight(1.45f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = primaryButtonColors()
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Budget", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimaryBlue, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMuted)
        }
    }
}

@Composable
private fun BudgetPreviewCard(
    state: FinanceUiState,
    amount: Double,
    selectedCategoryCount: Int,
    selectedCategoryNames: String
) {
    val onPrimary = primaryContentColor()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
        border = BorderStroke(1.dp, PrimaryBlue)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Monthly limit", color = onPrimary.copy(alpha = 0.68f), style = MaterialTheme.typography.labelMedium)
                    Text(
                        state.money(amount),
                        color = onPrimary,
                        style = MaterialTheme.typography.headlineLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (selectedCategoryCount == 0) "No categories selected" else selectedCategoryNames,
                        color = onPrimary.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(onPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PieChart, contentDescription = null, tint = PrimaryBlue)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BudgetPreviewPill("Categories", selectedCategoryCount.toString(), Modifier.weight(1f))
                BudgetPreviewPill("Cycle", "Monthly", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BudgetPreviewPill(label: String, value: String, modifier: Modifier) {
    val onPrimary = primaryContentColor()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(onPrimary.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Text(label, color = onPrimary.copy(alpha = 0.62f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = onPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun BudgetCategoryGrid(
    categories: List<CategoryItem>,
    selectedCategoryIds: List<Long>,
    onToggleCategory: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(2).forEach { rowCategories ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCategories.forEach { category ->
                    BudgetCategoryTile(
                        category = category,
                        selected = category.id in selectedCategoryIds,
                        onClick = { onToggleCategory(category.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCategories.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BudgetCategoryTile(
    category: CategoryItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = categoryColor(category, TransactionType.Expense)
    Card(
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) color.copy(alpha = 0.18f) else Navy800),
        border = BorderStroke(1.dp, if (selected) color else appBorderColor())
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = if (selected) 1f else 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = if (selected) {
                        accentContentColor()
                    } else {
                        color
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                category.name,
                color = if (selected) TextPrimary else TextMuted,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private val dateReadoutFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

private val CategoryPreviewInk = Color(0xFF16220A)

@Composable
private fun CategoryLivePreview(name: String, icon: ImageVector, color: Color, typeLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Navy850)
            .padding(horizontal = 20.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = CategoryPreviewInk, modifier = Modifier.size(38.dp))
        }
        Text(
            name.ifBlank { "New category" },
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text("$typeLabel · Preview", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun CategoryNameField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Navy850)
            .border(1.5.dp, PrimaryBlue, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.Label, contentDescription = null, tint = TextMuted, modifier = Modifier.size(21.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("Category name", color = TextDim, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    inner()
                }
            }
        )
    }
}

@Composable
private fun RowScope.CategoryTypePill(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) PrimaryBlue else Color.Transparent)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        Icon(icon, contentDescription = null, tint = if (active) OnAccent else TextMuted, modifier = Modifier.size(18.dp))
        Text(label, color = if (active) OnAccent else TextMuted, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun CategoryIconGridPicker(
    selectedKey: String,
    query: String = "",
    onSelect: (String) -> Unit
) {
    // Icons matching the typed category name float to the top; nothing is filtered out,
    // so when nothing matches the full grid is still there to pick from.
    val ordered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            MoneyIcons.allCategoryIcons
        } else {
            MoneyIcons.allCategoryIcons.sortedBy { option ->
                val label = option.label.lowercase()
                when {
                    label.startsWith(q) || option.key.startsWith(q) -> 0
                    label.contains(q) || option.key.contains(q) || q.contains(label) -> 1
                    else -> 2
                }
            }
        }
    }
    // 4-up labeled tiles: the label is what tells users what an icon means.
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ordered.chunked(4).forEach { rowIcons ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowIcons.forEach { option ->
                    val selected = option.key == selectedKey
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (selected) PrimaryBlue else Navy850)
                            .border(1.5.dp, if (selected) PrimaryBlue else LineColor, RoundedCornerShape(15.dp))
                            .clickable { onSelect(option.key) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            option.icon,
                            contentDescription = null,
                            tint = if (selected) OnAccent else TextMuted,
                            modifier = Modifier.size(21.dp)
                        )
                        Text(
                            option.label,
                            color = if (selected) OnAccent else TextDim,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
                repeat(4 - rowIcons.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryColorGridPicker(
    selectedHex: String,
    palette: List<String>,
    onSelect: (String) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        palette.forEach { hex ->
            val color = colorFromHex(hex)
            val selected = hex == selectedHex
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(if (selected) Modifier.border(2.5.dp, color, CircleShape) else Modifier)
                    .padding(if (selected) 4.dp else 2.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = CategoryPreviewInk, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddCategorySheet(
    palette: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf(MoneyIcons.frequentCategoryIcons.first().key) }
    var selectedColor by remember { mutableStateOf(palette.firstOrNull() ?: "#7EA2FF") }
    var isExpense by remember { mutableStateOf(true) }
    val selectedIcon = remember(selectedIconKey) {
        MoneyIcons.allCategoryIcons.firstOrNull { it.key == selectedIconKey }
            ?: MoneyIcons.frequentCategoryIcons.first()
    }
    val selectedColorValue = colorFromHex(selectedColor)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy950,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(tween(260, easing = FastOutSlowInEasing))
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SheetTitleBar(title = "New category", onDismiss = onDismiss)

                CategoryLivePreview(
                    name = name,
                    icon = selectedIcon.icon,
                    color = selectedColorValue,
                    typeLabel = if (isExpense) "Expense" else "Income"
                )

                SheetSectionLabel("Name")
                CategoryNameField(value = name, onValueChange = { name = it })

                // Type segment (affects preview label only)
                SheetSectionLabel("Type")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Navy850)
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryTypePill("Expense", Icons.Rounded.SouthWest, isExpense) { isExpense = true }
                    CategoryTypePill("Income", Icons.Rounded.NorthEast, !isExpense) { isExpense = false }
                }

                SheetSectionLabel("Icon")
                CategoryIconGridPicker(selectedKey = selectedIconKey, query = name, onSelect = { selectedIconKey = it })

                SheetSectionLabel("Color")
                CategoryColorGridPicker(selectedHex = selectedColor, palette = palette, onSelect = { selectedColor = it })
            }

            SheetBottomAction(
                text = "Create category",
                enabled = name.isNotBlank(),
                onClick = { onAdd(name.trim(), selectedIconKey, selectedColor) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCategorySheet(
    category: CategoryItem,
    palette: List<String>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var name by remember(category.id) { mutableStateOf(category.name) }
    var selectedIconKey by remember(category.id) { mutableStateOf(category.iconKey) }
    var selectedColor by remember(category.id) { mutableStateOf(category.colorHex) }
    val selectedIcon = remember(selectedIconKey) {
        MoneyIcons.allCategoryIcons.firstOrNull { it.key == selectedIconKey } ?: MoneyIcons.frequentCategoryIcons.first()
    }
    val selectedColorValue = colorFromHex(selectedColor)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy950,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(tween(260, easing = FastOutSlowInEasing))
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = if (category.isDefault) 96.dp else 150.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SheetTitleBar(title = "Edit category", onDismiss = onDismiss)

                CategoryLivePreview(
                    name = name,
                    icon = selectedIcon.icon,
                    color = selectedColorValue,
                    typeLabel = "Expense"
                )

                SheetSectionLabel("Name")
                CategoryNameField(value = name, onValueChange = { name = it })

                SheetSectionLabel("Icon")
                CategoryIconGridPicker(selectedKey = selectedIconKey, query = name, onSelect = { selectedIconKey = it })

                SheetSectionLabel("Color")
                CategoryColorGridPicker(selectedHex = selectedColor, palette = palette, onSelect = { selectedColor = it })
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Navy950)
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val saveEnabled = name.isNotBlank()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (saveEnabled) PrimaryBlue else PrimaryBlue.copy(alpha = 0.4f))
                        .clickable(enabled = saveEnabled) { onSave(category.id, name.trim(), selectedIconKey, selectedColor) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(21.dp))
                    Text("Save changes", color = OnAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (!category.isDefault) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(LossRed.copy(alpha = if (isDarkTheme()) 0.12f else 0.10f))
                            .clickable { showDeleteConfirm = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = LossRed, modifier = Modifier.size(20.dp))
                        Text("Delete category", color = LossRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${category.name}\"?") },
            text = { Text("Existing transactions in this category keep their history but the category will no longer be selectable.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete(category.id) }) {
                    Text("Delete", color = LossRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
            containerColor = Navy850,
            titleContentColor = TextPrimary,
            textContentColor = TextMuted
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesGridSheet(
    categories: List<CategoryItem>,
    onDismiss: () -> Unit,
    onEditCategory: (CategoryItem) -> Unit,
    onNewCategory: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    val filtered = remember(categories, query) {
        if (query.isBlank()) categories else categories.filter { it.name.contains(query, ignoreCase = true) }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Navy950,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Navy850)
                            .clickable {
                                if (searchOpen) {
                                    searchOpen = false
                                    query = ""
                                } else {
                                    onDismiss()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextMuted, modifier = Modifier.size(22.dp))
                    }
                    if (searchOpen) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isEmpty()) Text("Search categories", color = TextDim, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    inner()
                                }
                            }
                        )
                    } else {
                        Text(
                            "Categories",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Navy850)
                            .clickable {
                                searchOpen = !searchOpen
                                if (!searchOpen) query = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = if (searchOpen) "Close search" else "Search",
                            tint = TextMuted,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Text(
                    "${filtered.size} categories · tap a tile to edit",
                    color = TextDim,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )

                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Category,
                        title = "No categories found",
                        caption = "Try a different search or create a new category."
                    )
                } else {
                    filtered.chunked(3).forEach { rowCategories ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowCategories.forEach { category ->
                                CategoryGridTile(
                                    category = category,
                                    onClick = { onEditCategory(category) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowCategories.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            SheetBottomAction(
                text = "New category",
                enabled = true,
                onClick = onNewCategory,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun CategoryGridTile(category: CategoryItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = categoryColor(category, TransactionType.Expense)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Navy900)
            .border(1.dp, LineColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(color.copy(alpha = if (isDarkTheme()) 0.20f else 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Text(
                category.name,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 9.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Navy850),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = TextDim, modifier = Modifier.size(14.dp))
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
    val greeting = remember {
        when (java.time.LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Savings,
                contentDescription = null,
                tint = OnAccent,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(greeting, color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                userName,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Navy850)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = TextMuted,
                modifier = Modifier.size(22.dp)
            )
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
    val spent = remember(state.transactions, budget.month, budget.categoryIds) {
        state.transactions
            .filter {
                it.type == TransactionType.Expense &&
                    !it.excludeFromSummary &&
                    it.month() == budget.month &&
                    it.categoryId in budget.categoryIds
            }
            .sumOf { it.amount }
    }
    val progress = (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f)
    val over = spent > budget.limitAmount
    val names = remember(state.categories, budget.categoryIds) {
        state.categories.filter { it.id in budget.categoryIds }.joinToString(" · ") { it.name }
    }
    val budgetIcon = remember(state.categories, budget.categoryIds) {
        state.categories.firstOrNull { it.id in budget.categoryIds }?.icon
    } ?: Icons.Rounded.PieChart
    val redSoft = LossRed.copy(alpha = if (isDarkTheme()) 0.16f else 0.12f)
    val barColor = when {
        over -> LossRed
        progress >= 0.8f -> WarningAmber
        else -> PrimaryBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        border = BorderStroke(1.dp, if (over) LossRed else LineColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (over) redSoft else Navy850),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        budgetIcon,
                        contentDescription = null,
                        tint = if (over) LossRed else TextMuted,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        budget.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        names,
                        color = TextDim,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (over) {
                    Row(
                        modifier = Modifier
                            .height(24.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(redSoft)
                            .padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = LossRed, modifier = Modifier.size(14.dp))
                        Text("Over", color = LossRed, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(
                    onClick = { onDelete(budget.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete budget",
                        tint = TextDim,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Navy850)
            ) {
                DottedProgressFill(fraction = progress, color = barColor, dotRadius = 4.dp, step = 7.dp)
            }
            Row(Modifier.padding(top = 9.dp)) {
                Text(state.money(spent), color = barColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "of ${state.money(budget.limitAmount)}",
                    color = TextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(state: FinanceUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(state.userName.take(1).uppercase(), color = OnAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    state.userName,
                    color = OnAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${state.accounts.size} account${if (state.accounts.size == 1) "" else "s"} · ${state.currency.currencyCode} (${state.currency.symbol})",
                    color = OnAccent.copy(alpha = 0.72f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
    onAddCreditCard: (String, Double, List<String>) -> Unit,
    onDefaultAccountSelected: (Long?) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var newAccountType by remember { mutableStateOf(AccountType.Bank) }
    var newAccountName by remember { mutableStateOf("") }
    var newAccountLastDigits by remember { mutableStateOf("") }
    var newAccountBalance by remember { mutableStateOf("") }
    var newLinkedCards by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelText("ACCOUNTS")
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showAdd = !showAdd }) {
                Text(if (showAdd) "Cancel" else "Add account", color = PrimarySoft)
            }
        }
        if (state.bankAccounts.isNotEmpty()) {
            val defaultAccount = state.bankAccounts.firstOrNull { it.id == state.defaultAccountId }
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
                        state.bankAccounts.forEach { account ->
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
                        ChipRow {
                            AccountType.entries.forEach { type ->
                                MoneyChip(
                                    label = type.label,
                                    selected = newAccountType == type,
                                    onClick = { newAccountType = type }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = newAccountName,
                            onValueChange = { newAccountName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (newAccountType == AccountType.Bank) "Bank name" else "Card name") },
                            singleLine = true,
                            colors = inputColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newAccountLastDigits,
                            onValueChange = { newAccountLastDigits = it.filter(Char::isDigit).take(4) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (newAccountType == AccountType.Bank) "Last 4 account digits" else "Last 4 card digits") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = inputColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newAccountBalance,
                            onValueChange = { newAccountBalance = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (newAccountType == AccountType.Bank) "Current balance" else "Current outstanding") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = inputColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (newAccountType == AccountType.CreditCard) {
                            OutlinedTextField(
                                value = newLinkedCards,
                                onValueChange = { newLinkedCards = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Other cards on same bill (last 4, comma-separated)") },
                                placeholder = { Text("e.g. 0006, 1003") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = inputColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Text(
                                "For add-on cards billed together (one statement, one payment). " +
                                    "Spends on any of them count toward this card.",
                                color = TextDim,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Button(
                            onClick = {
                                val displayName = listOf(newAccountName.trim(), newAccountLastDigits.trim())
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                if (newAccountType == AccountType.Bank) {
                                    onAddAccount(displayName, newAccountBalance.toDoubleOrNull() ?: -1.0)
                                } else {
                                    val linked = newLinkedCards.split(",")
                                        .mapNotNull { it.filter(Char::isDigit).takeIf { d -> d.isNotEmpty() } }
                                    onAddCreditCard(displayName, newAccountBalance.toDoubleOrNull() ?: -1.0, linked)
                                }
                                newAccountName = ""
                                newAccountLastDigits = ""
                                newAccountBalance = ""
                                newLinkedCards = ""
                                newAccountType = AccountType.Bank
                                showAdd = false
                            },
                            enabled = newAccountName.isNotBlank() && (newAccountBalance.toDoubleOrNull() ?: -1.0) >= 0.0,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = primaryButtonColors()
                        ) {
                            Text(
                                if (newAccountType == AccountType.Bank) "Add bank account" else "Add credit card",
                                fontWeight = FontWeight.Bold
                            )
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
                                    Text(
                                        "${account.type.label} | ${if (account.type == AccountType.Bank) "Balance" else "Outstanding"} ${state.money(account.balance)}",
                                        color = TextDim,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
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
                                        label = { Text(if (account.type == AccountType.Bank) "Current balance anchor" else "Current outstanding") },
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
                                        enabled = account.type == AccountType.Bank && state.defaultAccountId != account.id,
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
                                            when {
                                                account.type == AccountType.CreditCard -> "Credit cards cannot be default bank"
                                                state.defaultAccountId == account.id -> "Default bank"
                                                else -> "Set as default bank"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        if (account.type == AccountType.Bank) {
                                            "This value is treated as the current bank balance. New bank transactions update it from here."
                                        } else {
                                            "This value is treated as current card outstanding. Card purchases increase it; bill payments decrease it."
                                        },
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
                    tint = primaryContentColor(),
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
private fun AppearanceCard(
    selectedTheme: ThemeMode,
    selectedAccent: UiAccent,
    customAccentHex: String?,
    darkMode: Boolean,
    surfaceLabel: String,
    onThemeSelected: (ThemeMode) -> Unit,
    onAccentSelected: (UiAccent) -> Unit,
    onCustomAccentApplied: (String) -> Unit,
    onOpenSurface: () -> Unit,
    onOpenColors: () -> Unit
) {
    var showAccentPicker by remember { mutableStateOf(false) }
    val currentAccentHex = customAccentHex
        ?: if (darkMode) selectedAccent.darkHex else selectedAccent.lightHex
    ElevatedPanel {
        Column(Modifier.padding(16.dp)) {
            Text("Theme", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePill(
                    label = "Dark",
                    icon = Icons.Rounded.DarkMode,
                    selected = selectedTheme == ThemeMode.Dark,
                    onClick = { onThemeSelected(ThemeMode.Dark) },
                    modifier = Modifier.weight(1f)
                )
                ThemePill(
                    label = "Light",
                    icon = Icons.Rounded.LightMode,
                    selected = selectedTheme == ThemeMode.Light,
                    onClick = { onThemeSelected(ThemeMode.Light) },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Accent",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 18.dp)
            )
            LazyRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "__custom") {
                    // Opens the color-wheel picker; ringed when a custom color is active.
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFFF5A5A), Color(0xFFFFD166), Color(0xFFB4F077),
                                        Color(0xFF3FE0C4), Color(0xFF7EA2FF), Color(0xFFB794F6),
                                        Color(0xFFFF6FAE), Color(0xFFFF5A5A)
                                    )
                                )
                            )
                            .border(
                                width = 2.5.dp,
                                color = if (customAccentHex != null) TextPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { showAccentPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Custom accent color", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                items(UiAccent.entries, key = { it.name }) { accent ->
                    val color = colorFromHex(if (darkMode) accent.darkHex else accent.lightHex)
                    val isSelected = customAccentHex == null && selectedAccent == accent
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = 2.5.dp,
                                color = if (isSelected) TextPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onAccentSelected(accent) }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenColors),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My colors", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("Shared palette", color = TextDim, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextDim, modifier = Modifier.size(20.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenSurface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Surface style", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(surfaceLabel, color = TextDim, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextDim, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showAccentPicker) {
        AccentPickerSheet(
            initialHex = currentAccentHex,
            onDismiss = { showAccentPicker = false },
            onApply = { hex ->
                showAccentPicker = false
                onCustomAccentApplied(hex)
            }
        )
    }
}

@Composable
private fun ThemePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PrimaryBlue else Navy800)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) OnAccent else TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            color = if (selected) OnAccent else TextMuted,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
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
    checkedThumbColor = primaryContentColor(),
    checkedTrackColor = PrimaryBlue,
    checkedBorderColor = PrimaryBlue,
    uncheckedThumbColor = TextMuted,
    uncheckedTrackColor = if (isAmoledTheme()) Navy800 else Color(0xFFE8EDF7),
    uncheckedBorderColor = appBorderColor()
)

@Composable
fun primaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = PrimaryBlue,
    contentColor = primaryContentColor()
)

@Composable
private fun AddModeChip(mode: AddMoneyMode, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val accent = when (mode) {
        AddMoneyMode.Income -> MoneyGreen
        AddMoneyMode.Expense -> LossRed
        AddMoneyMode.Investment -> OtherIncomeGold
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
            selectedLabelColor = if (selected) accentContentColor() else accent,
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
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = NavSolid),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScreenTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val pillWidth by animateFloatAsState(
                    targetValue = if (selected) 52f else 30f,
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    label = "navPill"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onTabSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (selected) 2.dp else 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = pillWidth.dp, height = if (selected) 30.dp else 22.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) PrimaryBlue else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) OnAccent else TextDim,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Text(
                        tab.label,
                        color = if (selected) TextPrimary else TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 9.5.sp,
                        lineHeight = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
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
    "#2F5FD0",
    "#087F7A",
    "#C0266D",
    "#A16207",
    "#6D28D9",
    "#0277BD",
    "#B45335",
    "#BE3A57",
    "#087C64",
    "#1D4ED8",
    "#138A4E",
    "#9F1239",
    "#FF8A65",
    "#35D6E7",
    "#F5C542"
)
