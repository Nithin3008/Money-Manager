package com.moneymanager.app.model

import com.moneymanager.app.data.AccountEntity
import com.moneymanager.app.data.BudgetEntity
import com.moneymanager.app.data.CategoryEntity
import com.moneymanager.app.data.TransactionEntity
import com.moneymanager.app.data.UserSettingsEntity

data class AppBackupData(
    val version: Int = 1,
    val settings: UserSettingsEntity? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList()
)
