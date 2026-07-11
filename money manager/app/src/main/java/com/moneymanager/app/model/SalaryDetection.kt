package com.moneymanager.app.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

/** A recurring monthly credit that looks like the user's salary and awaits confirmation. */
@androidx.compose.runtime.Immutable
data class SalaryCandidate(
    val key: String,
    val displayName: String,
    val typicalAmount: Double,
    val monthsObserved: Int,
    val lastDate: LocalDate
)

/** A single credit event, detached from storage so detection can run on scanned SMS too. */
data class CreditObservation(
    val name: String,
    val amount: Double,
    val timestampMillis: Long
) {
    val date: LocalDate
        get() = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}

/**
 * Detects salary by recurrence instead of keywords: the same counterparty crediting a
 * similar amount roughly once a month is a far stronger salary signal than SMS text.
 */
object SalaryDetection {

    /**
     * Stable key for grouping credits from the same counterparty across months. Digit runs
     * are dropped because banks embed per-transaction references (NEFT/UTR numbers, dates)
     * in the counterparty text, which would make each month's salary look like a new sender.
     */
    fun salaryMatchKey(counterpartyName: String): String {
        return counterpartyName
            .lowercase()
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .replace(Regex("""\b\d+\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun matchesConfirmedSalary(state: FinanceUiState, tx: LedgerTransaction): Boolean {
        val key = state.salaryCounterpartyKey ?: return false
        return tx.type == TransactionType.Income && salaryMatchKey(tx.name) == key
    }

    /**
     * Finds the strongest unconfirmed recurring-credit group: at least two distinct months,
     * consistent amounts, at most a couple of credits per month. Among qualifying groups the
     * one with the largest typical amount wins, since salary is normally the biggest inflow.
     */
    fun detectCandidate(state: FinanceUiState): SalaryCandidate? {
        if (state.salaryCounterpartyKey != null) return null
        val credits = state.transactions.filter {
            it.type == TransactionType.Income &&
                it.amount > 0.0 &&
                !it.excludeFromSummary &&
                !it.isCreditCardTransaction &&
                !state.isInvestmentTransaction(it)
        }
        val salaryId = state.salaryCategoryId
        return credits
            .groupBy { salaryMatchKey(it.name) }
            .asSequence()
            .filter { (key, _) -> key.isNotBlank() && key !in state.dismissedSalaryKeys }
            .filter { (_, txs) -> salaryId == null || txs.none { it.categoryId == salaryId } }
            .mapNotNull { (key, txs) ->
                candidateFor(key, txs.map { CreditObservation(it.name, it.amount, it.timestampMillis) })
            }
            .maxByOrNull { it.typicalAmount }
    }

    /**
     * Same recurrence heuristic over raw credit events, e.g. scanned SMS that were never
     * imported. Used at onboarding to find the salary without importing full history.
     */
    fun detectRecurringCredit(observations: List<CreditObservation>): SalaryCandidate? {
        return observations
            .filter { it.amount > 0.0 }
            .groupBy { salaryMatchKey(it.name) }
            .asSequence()
            .filter { (key, _) -> key.isNotBlank() }
            .mapNotNull { (key, group) -> candidateFor(key, group) }
            .maxByOrNull { it.typicalAmount }
    }

    private fun candidateFor(key: String, credits: List<CreditObservation>): SalaryCandidate? {
        val byMonth = credits.groupBy { YearMonth.from(it.date) }
        if (byMonth.size < 2) return null
        if (byMonth.values.any { it.size > 2 }) return null

        val months = byMonth.keys.sorted()
        val gapsAreMonthly = months.zipWithNext().all { (a, b) ->
            val gap = (b.year - a.year) * 12 + (b.monthValue - a.monthValue)
            gap in 1..2
        }
        if (!gapsAreMonthly) return null

        val amounts = credits.map { it.amount }.sorted()
        val median = amounts[amounts.size / 2]
        val consistent = amounts.all { abs(it - median) <= median * 0.30 }
        if (!consistent) return null

        val latest = credits.maxBy { it.timestampMillis }
        return SalaryCandidate(
            key = key,
            displayName = latest.name,
            typicalAmount = median,
            monthsObserved = byMonth.size,
            lastDate = latest.date
        )
    }
}
