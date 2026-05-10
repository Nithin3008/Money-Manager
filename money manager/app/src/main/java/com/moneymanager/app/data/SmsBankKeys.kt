package com.moneymanager.app.data

import com.moneymanager.app.model.BankAccount
import java.util.Locale

object SmsBankKeys {
    fun normalize(label: String): String = label.uppercase(Locale.US).trim()

    /**
     * Maps SMS bank label to account id when [BankAccount.smsMatchKey] matches; otherwise single account fallback.
     */
    fun resolveAccountId(smsBankLabel: String?, accounts: List<BankAccount>): Long? {
        if (accounts.isEmpty()) return null
        if (accounts.size == 1) return accounts.first().id
        val label = smsBankLabel?.trim().orEmpty()
        if (label.isEmpty()) return null
        val key = normalize(label)
        val matched = accounts.firstOrNull { ac ->
            val mk = ac.smsMatchKey?.trim()?.let(::normalize).orEmpty()
            mk.isNotEmpty() && mk == key
        }
        if (matched != null) return matched.id
        return null
    }
}
