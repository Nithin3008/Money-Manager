package com.moneymanager.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.moneymanager.app.data.TransactionMessageParser

class TransactionNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val parsed = TransactionMessageParser.parse("$title $text") ?: return

        // Next step: persist this as a DetectedTransactionDraft in Room, then show it in the app.
        android.util.Log.d(
            "MoneyManager",
            "Detected transaction from notification: ${parsed.name} ${parsed.amount} ${parsed.type}"
        )
    }
}
