package com.moneymanager.app.data

import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.BudgetPlan
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.CurrencyOption
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.MoneyIcons
import com.moneymanager.app.model.ThemeMode
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.model.UiAccent
import com.moneymanager.app.model.UiSurface
import java.time.YearMonth

internal fun FinanceUiState.toSettingsEntity(): UserSettingsEntity = UserSettingsEntity(
    userName = userName,
    currencyCode = currency.currencyCode,
    themeMode = themeMode.name,
    uiAccent = uiAccent.name,
    uiSurface = uiSurface.name,
    salaryShiftIncomeEnabled = salaryShiftIncomeEnabled,
    salaryShiftWindowDays = salaryShiftWindowDays.coerceIn(1, 14),
    salaryCategoryId = salaryCategoryId,
    salaryKeywordsForUncategorized = salaryKeywordsForUncategorized,
    bankSmsSetupCompleted = bankSmsSetupCompleted,
    defaultAccountId = defaultAccountId,
    summaryAccountFilterIdsCsv = summarySelectedAccountIds.joinToString(",")
)

internal fun UserSettingsEntity.applyTo(current: FinanceUiState): FinanceUiState {
    return current.copy(
        userName = userName,
        currency = currencyCode.let { code ->
            CurrencyOption.entries.firstOrNull { it.currencyCode == code }
        } ?: CurrencyOption.INR,
        themeMode = themeMode.let { mode ->
            ThemeMode.entries.firstOrNull { it.name == mode }
        } ?: ThemeMode.Dark,
        uiAccent = uiAccent.let { accent ->
            UiAccent.entries.firstOrNull { it.name == accent }
        } ?: UiAccent.Sky,
        uiSurface = uiSurface.let { surface ->
            UiSurface.entries.firstOrNull { it.name == surface }
        } ?: UiSurface.Midnight,
        salaryShiftIncomeEnabled = salaryShiftIncomeEnabled,
        salaryShiftWindowDays = salaryShiftWindowDays.coerceIn(1, 14),
        salaryCategoryId = salaryCategoryId,
        salaryKeywordsForUncategorized = salaryKeywordsForUncategorized,
        bankSmsSetupCompleted = bankSmsSetupCompleted,
        defaultAccountId = defaultAccountId,
        summarySelectedAccountIds = summaryAccountFilterIdsCsv
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
    )
}

internal fun AccountEntity.toModel() = BankAccount(
    id = id,
    name = name,
    balance = balance,
    smsMatchKey = smsMatchKey
)

internal fun CategoryEntity.toModel() = CategoryItem(
    id = id,
    name = name,
    iconKey = iconKey,
    icon = MoneyIcons.resolveCategoryIcon(iconKey),
    isDefault = isDefault,
    colorHex = colorHex
)

internal fun CategoryItem.toEntity() = CategoryEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    isDefault = isDefault,
    colorHex = colorHex
)

internal fun TransactionEntity.toModel() = LedgerTransaction(
    id = id,
    name = name,
    amount = amount,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    accountId = accountId,
    timestampMillis = timestampMillis,
    isAutoDetected = isAutoDetected,
    rawMessage = rawMessage,
    smsBankLabel = smsBankLabel,
    excludeFromSummary = excludeFromSummary,
    isCreditCardTransaction = isCreditCardTransaction
)

internal fun LedgerTransaction.toEntity(id: Long = this.id) = TransactionEntity(
    id = id,
    name = name,
    amount = amount,
    type = type.name,
    categoryId = categoryId,
    accountId = accountId,
    timestampMillis = timestampMillis,
    isAutoDetected = isAutoDetected,
    rawMessage = rawMessage,
    smsBankLabel = smsBankLabel,
    excludeFromSummary = excludeFromSummary,
    isCreditCardTransaction = isCreditCardTransaction
)

internal fun BudgetEntity.toModel() = BudgetPlan(
    id = id,
    name = name,
    limitAmount = limitAmount,
    categoryIds = categoryIdsCsv.split(",").mapNotNull { it.toLongOrNull() }.toSet(),
    month = YearMonth.parse(month)
)

internal fun BudgetPlan.toEntity(id: Long = this.id) = BudgetEntity(
    id = id,
    name = name,
    limitAmount = limitAmount,
    categoryIdsCsv = categoryIds.joinToString(","),
    month = month.toString()
)

internal fun DetectedDraftEntity.toModel() = DetectedTransactionDraft(
    id = id,
    bankName = bankName,
    name = name,
    amount = amount,
    type = TransactionType.valueOf(type),
    counterparty = counterparty,
    rawMessage = rawMessage,
    suggestedCategoryId = suggestedCategoryId,
    detectedAtMillis = detectedAtMillis,
    transactionTimestampMillis = if (transactionTimestampMillis == 0L) detectedAtMillis else transactionTimestampMillis
)

internal fun DetectedTransactionDraft.toEntity(id: Long = this.id) = DetectedDraftEntity(
    id = id,
    bankName = bankName,
    name = name,
    amount = amount,
    type = type.name,
    counterparty = counterparty,
    rawMessage = rawMessage,
    suggestedCategoryId = suggestedCategoryId,
    detectedAtMillis = detectedAtMillis,
    transactionTimestampMillis = transactionTimestampMillis
)
