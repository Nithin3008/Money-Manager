package com.moneymanager.app.data

import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.AccountType
import com.moneymanager.app.model.BudgetPlan
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.ColorLibrary
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
    // The uiAccent column holds either an enum name or a "#hex" custom accent (no migration).
    uiAccent = customAccentHex ?: uiAccent.name,
    uiSurface = uiSurface.name,
    bankSmsSetupCompleted = bankSmsSetupCompleted,
    // Legacy salary + offline-LLM columns are kept in the schema (defaults) to avoid a migration.
    offlineLlmParsingEnabled = false,
    offlineLlmModelDownloaded = false,
    onboardedAtMillis = onboardedAtMillis,
    lastSuccessfulScanMillis = lastSuccessfulScanMillis,
    defaultAccountId = defaultAccountId,
    summaryAccountFilterIdsCsv = summarySelectedAccountIds.joinToString(","),
    // Cap so years of deletions cannot grow the row unbounded; oldest keys age out first,
    // and their SMS are far outside any future catch-up scan window anyway.
    dismissedSmsKeys = dismissedSmsKeys.toList().takeLast(2000).joinToString("\n"),
    paletteHexCsv = paletteColors.joinToString(",")
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
        } ?: UiAccent.Lime,
        // A "#hex" value in the column is a custom accent; otherwise it's a preset enum name.
        customAccentHex = uiAccent.takeIf { it.startsWith("#") },
        uiSurface = uiSurface.let { surface ->
            UiSurface.entries.firstOrNull { it.name == surface }
        } ?: UiSurface.Midnight,
        bankSmsSetupCompleted = bankSmsSetupCompleted,
        onboardedAtMillis = onboardedAtMillis,
        lastSuccessfulScanMillis = lastSuccessfulScanMillis,
        defaultAccountId = defaultAccountId,
        summarySelectedAccountIds = summaryAccountFilterIdsCsv
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet(),
        dismissedSmsKeys = dismissedSmsKeys
            .split("\n")
            .filter { it.isNotBlank() }
            .toCollection(LinkedHashSet()),
        // Empty column = never seeded (fresh install or pre-v21 upgrade); fall back to defaults.
        paletteColors = paletteHexCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { ColorLibrary.defaultPalette }
    )
}

internal fun AccountEntity.toModel() = BankAccount(
    id = id,
    name = name,
    balance = balance,
    smsMatchKey = smsMatchKey,
    type = AccountType.entries.firstOrNull { it.name == accountType } ?: AccountType.Bank,
    balanceAnchorAtMillis = balanceAnchorAtMillis,
    linkedCardNumbers = linkedCardsCsv.split(",").mapNotNull { it.trim().takeIf(String::isNotEmpty) }
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
    isCreditCardTransaction = isCreditCardTransaction,
    description = description,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId
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
    isCreditCardTransaction = isCreditCardTransaction,
    description = description,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId
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
    transactionTimestampMillis = if (transactionTimestampMillis == 0L) detectedAtMillis else transactionTimestampMillis,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId
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
    transactionTimestampMillis = transactionTimestampMillis,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId
)
