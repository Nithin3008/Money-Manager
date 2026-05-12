package com.moneymanager.app.data

import com.moneymanager.app.model.BankAccount
import java.util.Locale

object SmsBankKeys {
    fun normalize(label: String): String = label.uppercase(Locale.US).trim()
    fun bankRoot(label: String?): String = normalize(label.orEmpty())
        .replace(Regex("""\bA/C\s*\d{3,6}\b"""), " ")
        .replace(Regex("""\bAC\s*\d{3,6}\b"""), " ")
        .replace(Regex("""\bACCOUNT\s*\d{3,6}\b"""), " ")
        .replace(Regex("""\bBANK\b"""), " ")
        .replace(Regex("""[^A-Z0-9 ]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    fun accountHint(label: String?): String? =
        Regex("""(?i)\b(?:A/C|ACCT|ACCOUNT|ACC|AC)\s*(?:X+|[*]+)?\s*([0-9]{3,6})\b""")
            .find(label.orEmpty())
            ?.groupValues
            ?.getOrNull(1)

    fun accountNameMatchesLabel(account: BankAccount, smsBankLabel: String?): Boolean {
        val root = bankRoot(smsBankLabel)
        if (root.isBlank()) return false
        val accountRoot = bankRoot(account.name)
        val explicitKeyRoot = bankRoot(account.smsMatchKey)
        val hint = accountHint(smsBankLabel)
        val accountName = normalize(account.name)
        val hintMatchesName = hint != null && hint in accountName
        return hintMatchesName ||
            (accountRoot.isNotBlank() && (root in accountRoot || accountRoot in root)) ||
            (explicitKeyRoot.isNotBlank() && (root in explicitKeyRoot || explicitKeyRoot in root))
    }

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
        val semanticMatches = accounts.filter { accountNameMatchesLabel(it, label) }
        if (semanticMatches.size == 1) return semanticMatches.first().id
        return null
    }
}
