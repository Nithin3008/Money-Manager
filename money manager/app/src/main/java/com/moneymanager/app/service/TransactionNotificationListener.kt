package com.moneymanager.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.moneymanager.app.data.FinanceDatabase
import com.moneymanager.app.data.FinanceRepository
import com.moneymanager.app.data.TransactionMessageParser
import com.moneymanager.app.model.DetectedTransactionDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val parsed = TransactionMessageParser.parse("$title $text") ?: return

        CoroutineScope(Dispatchers.IO).launch {
            FinanceRepository(FinanceDatabase.get(applicationContext).dao()).saveDraft(
                DetectedTransactionDraft(
                    id = 0,
                    bankName = parsed.bankName,
                    name = parsed.counterparty,
                    amount = parsed.amount,
                    type = parsed.type,
                    counterparty = parsed.counterparty,
                    rawMessage = parsed.rawMessage,
                    suggestedCategoryId = null,
                    detectedAtMillis = System.currentTimeMillis(),
                    transactionTimestampMillis = parsed.transactionTimestampMillis
                )
            )
        }
    }
}
