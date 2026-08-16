package com.moneymanager.app.widget

import android.content.Context
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.updateAll
import com.moneymanager.app.data.FinanceDatabase
import com.moneymanager.app.data.FinanceRepository
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.ui.money
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** One snapshot drives the widget so its figures always match the in-app numbers. */
internal data class WidgetSnapshot(
    val registered: Boolean,
    val inText: String,
    val outText: String,
    val uncategorizedCount: Int,
    val updatedAt: String
)

private val widgetTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Forwarded as an intent extra by Glance; MainActivity routes on it. */
internal val WidgetOpenTab = ActionParameters.Key<String>(com.moneymanager.app.MainActivity.EXTRA_OPEN_TAB)

/** Reads the same state the app itself uses; see MoneyViewModel and TransactionNotificationListener. */
internal suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val state = FinanceRepository(FinanceDatabase.get(context).dao()).loadState()
    val moneyIn = state.todayTransactions
        .filter { it.type == TransactionType.Income }
        .sumOf { it.amount }
    val moneyOut = state.todayTransactions
        .filter { it.type == TransactionType.Expense }
        .sumOf { it.amount }
    // Backlog of rows still showing "Set category"/Uncategorized in the transaction list.
    val uncategorized = state.transactions.count { tx ->
        tx.type != TransactionType.Transfer &&
            (state.categoriesById[tx.categoryId]?.name ?: "Uncategorized") == "Uncategorized"
    }
    return WidgetSnapshot(
        registered = state.hasCompletedRegistration,
        inText = "+ ${money(moneyIn, state.currency)}",
        outText = "- ${money(moneyOut, state.currency)}",
        uncategorizedCount = uncategorized,
        updatedAt = LocalTime.now().format(widgetTimeFormatter)
    )
}

object MoneyWidgets {
    /** Re-renders every placed widget. Cheap and coalesced; safe to call after any data change. */
    suspend fun refresh(context: Context) {
        runCatching { TodayWidget().updateAll(context) }
    }
}
