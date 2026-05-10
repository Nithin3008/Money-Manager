package com.moneymanager.app.model

/** Heuristic keywords for SMS/counterparty text when salary category is not assigned. */
object SalaryIncomeRules {
    private val keywords = listOf(
        "salary",
        "payroll",
        "pay roll",
        "wages",
        "employer",
        "esi",
        "pf credit",
        "pf ",
        "statutory"
    )

    fun matchesSalaryKeywords(counterpartyOrName: String, rawMessage: String?): Boolean {
        val text = buildString {
            append(counterpartyOrName.lowercase())
            append(' ')
            append(rawMessage?.lowercase().orEmpty())
        }
        return keywords.any { it in text }
    }
}
