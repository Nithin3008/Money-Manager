package com.moneymanager.app.data

import com.moneymanager.app.model.BankAccount
import java.util.Locale

object SmsBankKeys {
    fun normalize(label: String): String = label.uppercase(Locale.US).trim()
    fun bankRoot(label: String?): String = normalize(label.orEmpty())
        .replace(Regex("""\bA/C\s*\d{3,6}\b"""), " ")
        .replace(Regex("""\bAC\s*\d{3,6}\b"""), " ")
        .replace(Regex("""\bACCOUNT\s*\d{3,6}\b"""), " ")
        .replace(Regex("""\bCARD\s*\d{3,6}\b"""), " ")
        .replace(Regex("""\bBANK\b"""), " ")
        .replace(Regex("""\bCARD\b"""), " ")
        .replace(Regex("""[^A-Z0-9 ]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    fun accountHint(label: String?): String? =
        Regex("""(?i)\b(?:A/C|ACCT|ACCOUNT|ACC|AC|CARD|CC)\s*(?:X+|[*]+)?\s*([0-9]{3,6})\b""")
            .find(label.orEmpty())
            ?.groupValues
            ?.getOrNull(1)

    fun cardHint(label: String?): String? {
        val raw = label.orEmpty()
        val explicitCardHint = Regex(
            """(?i)\b(?:credit\s+)?(?:card|cc)\s*(?:no\.?|number|ending|ended|x+|[*]+)?\s*(?:with|in)?\s*(?:x+|[*]+)?\s*([0-9]{3,6})\b"""
        ).find(raw)?.groupValues?.getOrNull(1)
        if (explicitCardHint != null) return explicitCardHint

        val lower = raw.lowercase(Locale.US)
        if (!lower.contains("card") && !lower.contains("cc")) return null
        // ICICI consolidated statement/payment identifiers: "...Account 4xxx8010" and
        // "ICICI Bank XXXX-8010" name the billing account's last-4 without a card keyword.
        val maskedAccountHint = Regex("""(?i)\b\d?x{2,}[-\s]?([0-9]{3,6})\b""")
            .find(raw)?.groupValues?.getOrNull(1)
        if (maskedAccountHint != null) return maskedAccountHint
        return Regex("""(?i)\b(?:ending|ended|no\.?)\s*(?:with|in)?\s*([0-9]{3,6})\b""")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
    }

    private val knownIssuerRegex = Regex(
        """(?i)\b(hdfc|icici|sbi|axis|kotak|yes bank|idfc|indusind|canara|union bank|pnb|bank of baroda|""" +
            """indian bank|federal|bandhan|rbl|hsbc|standard chartered|onecard|slice)"""
    )

    /**
     * The bank named inside a card-side SMS ("DEAR HDFCBANK CARDMEMBER..."), used to pick
     * the right card account when the paying bank differs from the card's issuer.
     */
    fun issuerRoot(text: String?): String? =
        knownIssuerRegex.find(text.orEmpty())?.groupValues?.getOrNull(1)?.uppercase(Locale.US)

    /** Every card last-4 that maps to this account: the one in its name plus any linked cards. */
    fun cardHints(account: BankAccount): Set<String> {
        val fromName = accountHint(account.name)
            ?: Regex("""\b(\d{3,6})\b""").find(account.name)?.groupValues?.getOrNull(1)
        return (listOfNotNull(fromName) + account.linkedCardNumbers).toSet()
    }

    fun accountNameMatchesLabel(account: BankAccount, smsBankLabel: String?): Boolean {
        val root = bankRoot(smsBankLabel)
        if (root.isBlank()) return false
        val accountRoot = bankRoot(account.name)
        val explicitKeyRoot = bankRoot(account.smsMatchKey)
        val hint = accountHint(smsBankLabel)
        val accountName = normalize(account.name)
        val hintMatchesName = hint != null && hint in accountName
        val hintMatchesLinkedCard = hint != null && hint in cardHints(account)
        return hintMatchesName ||
            hintMatchesLinkedCard ||
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
        val hint = accountHint(label)
        if (hint != null) {
            val hintMatches = accounts.filter { account ->
                hint in normalize(account.name) ||
                    hint in normalize(account.smsMatchKey.orEmpty()) ||
                    hint in cardHints(account)
            }
            if (hintMatches.size == 1) return hintMatches.first().id
        }
        val semanticMatches = accounts.filter { accountNameMatchesLabel(it, label) }
        if (semanticMatches.size == 1) return semanticMatches.first().id
        return null
    }
}
