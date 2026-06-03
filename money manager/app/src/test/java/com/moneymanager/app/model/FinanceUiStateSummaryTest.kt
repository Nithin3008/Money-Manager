package com.moneymanager.app.model

import com.moneymanager.app.data.ParsedTransactionMessage
import com.moneymanager.app.data.SmsTransactionNormalizer
import com.moneymanager.app.data.TransactionMessageParser
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceUiStateSummaryTest {

    private val bankA = BankAccount(id = 1L, name = "Primary 1234", balance = 50_000.0)
    private val bankB = BankAccount(id = 2L, name = "Second 9876", balance = 25_000.0)

    @Test
    fun summaryUsesCalendarMonthDepositsAndExpensesForDefaultAccount() {
        val state = state(
            defaultAccountId = bankA.id,
            transactions = listOf(
                tx(1, 65_000.0, TransactionType.Income, "2026-04-30", bankA.id, raw = "Salary credited"),
                tx(2, 16_000.0, TransactionType.Income, "2026-04-12", bankA.id, raw = "Refund credited"),
                tx(3, 10_000.0, TransactionType.Expense, "2026-04-13", bankA.id),
                tx(4, 45_000.0, TransactionType.Income, "2026-04-15", bankB.id),
                tx(5, 9_000.0, TransactionType.Income, "2026-05-01", bankA.id),
                tx(6, 999.0, TransactionType.Income, "2026-04-20", bankA.id, exclude = true)
            )
        )

        assertEquals(81_000.0, state.monthIncome, 0.001)
        assertEquals(65_000.0, state.monthSalaryIncome, 0.001)
        assertEquals(16_000.0, state.monthOtherIncome, 0.001)
        assertEquals(10_000.0, state.monthExpense, 0.001)
        assertEquals(71_000.0, state.monthReportNet, 0.001)
    }

    @Test
    fun explicitSummaryAccountSelectionOverridesDefaultAccount() {
        val state = state(
            defaultAccountId = bankA.id,
            summarySelectedAccountIds = setOf(bankB.id),
            transactions = listOf(
                tx(1, 65_000.0, TransactionType.Income, "2026-04-30", bankA.id),
                tx(2, 45_000.0, TransactionType.Income, "2026-04-15", bankB.id),
                tx(3, 5_000.0, TransactionType.Expense, "2026-04-16", bankB.id)
            )
        )

        assertEquals(45_000.0, state.monthIncome, 0.001)
        assertEquals(5_000.0, state.monthExpense, 0.001)
        assertEquals(40_000.0, state.monthReportNet, 0.001)
    }

    @Test
    fun currentBalanceUsesOnlySelectedBankAccount() {
        val defaultState = state(
            defaultAccountId = bankA.id,
            transactions = emptyList()
        )
        val selectedBankState = state(
            defaultAccountId = bankA.id,
            summarySelectedAccountIds = setOf(bankB.id),
            transactions = emptyList()
        )
        val allBanksState = state(
            defaultAccountId = null,
            transactions = emptyList()
        )

        assertEquals(50_000.0, defaultState.currentBalanceAnchor, 0.001)
        assertEquals(25_000.0, selectedBankState.currentBalanceAnchor, 0.001)
        assertEquals(75_000.0, allBanksState.currentBalanceAnchor, 0.001)
    }

    @Test
    fun bankScopedSummaryExcludesUnmappedTransactions() {
        val state = state(
            defaultAccountId = bankA.id,
            transactions = listOf(
                tx(1, 65_000.0, TransactionType.Income, "2026-04-30", bankA.id),
                tx(2, 50_000.0, TransactionType.Income, "2026-04-15", null),
                tx(3, 2_000.0, TransactionType.Expense, "2026-04-16", null)
            )
        )

        assertEquals(65_000.0, state.monthIncome, 0.001)
        assertEquals(0.0, state.monthExpense, 0.001)
    }

    @Test
    fun allAccountsSummaryIncludesUnmappedTransactionsWhenNoBankIsActive() {
        val state = state(
            defaultAccountId = null,
            transactions = listOf(
                tx(1, 65_000.0, TransactionType.Income, "2026-04-30", bankA.id),
                tx(2, 50_000.0, TransactionType.Income, "2026-04-15", null),
                tx(3, 2_000.0, TransactionType.Expense, "2026-04-16", null)
            )
        )

        assertEquals(115_000.0, state.monthIncome, 0.001)
        assertEquals(2_000.0, state.monthExpense, 0.001)
    }

    @Test
    fun manualTransferRowsAreExcludedFromIncomeAndExpenseReports() {
        val state = state(
            defaultAccountId = bankA.id,
            transactions = listOf(
                tx(1, 20_000.0, TransactionType.Expense, "2026-04-10", bankA.id, exclude = true),
                tx(2, 20_000.0, TransactionType.Income, "2026-04-10", bankA.id, exclude = true),
                tx(3, 2_500.0, TransactionType.Expense, "2026-04-11", bankA.id)
            )
        )

        assertEquals(0.0, state.monthIncome, 0.001)
        assertEquals(2_500.0, state.monthExpense, 0.001)
    }

    @Test
    fun balanceReconstructionBacktracksFromCurrentBalance() {
        val state = state(
            defaultAccountId = bankA.id,
            accounts = listOf(bankA.copy(balance = 50_000.0), bankB),
            transactions = listOf(
                tx(1, 20_000.0, TransactionType.Income, "2026-04-10", bankA.id),
                tx(2, 7_000.0, TransactionType.Expense, "2026-04-15", bankA.id),
                tx(3, 10_000.0, TransactionType.Income, "2026-05-02", bankA.id),
                tx(4, 5_000.0, TransactionType.Expense, "2026-05-05", bankA.id)
            )
        )

        assertEquals(50_000.0, state.currentBalanceAnchor, 0.001)
        assertEquals(32_000.0, state.balanceAtStartOfSelectedMonth, 0.001)
        assertEquals(45_000.0, state.balanceAtEndOfSelectedMonth, 0.001)
        assertEquals(13_000.0, state.calendarMonthNet, 0.001)
        assertEquals(0.0, state.selectedMonthReconciliationGap, 0.001)
    }

    @Test
    fun creditCardSpendDoesNotMoveBankBalance() {
        val state = state(
            defaultAccountId = bankA.id,
            accounts = listOf(bankA.copy(balance = 50_000.0), bankB),
            transactions = listOf(
                tx(1, 2_500.0, TransactionType.Expense, "2026-04-10", bankA.id, creditCard = true),
                tx(2, 5_000.0, TransactionType.Income, "2026-04-11", bankA.id)
            )
        )

        assertEquals(50_000.0, state.currentBalanceAnchor, 0.001)
        assertEquals(5_000.0, state.calendarMonthNet, 0.001)
    }

    @Test
    fun creditCardDueAndPaymentArtifactsAreNotLedgerTransactions() {
        val dueReminder = "Amount Due Rs.33368 on HDFC Bank Credit Card 9494. Pay instantly by 09/MAY/2026 via PayZapp"
        val paymentReceived = "Payment of Rs.33368.00 received towards your credit card ending with 9494"

        assertTrue(SmsTransactionNormalizer.isNonLedgerTransactionArtifact(dueReminder, TransactionType.Income))
        assertTrue(SmsTransactionNormalizer.isNonLedgerTransactionArtifact(paymentReceived, TransactionType.Income))
    }

    @Test
    fun creditCardRepaymentPairDropsOnlyTheCardCreditLeg() {
        val debit = parsed(
            amount = 33_368.0,
            type = TransactionType.Expense,
            raw = "Rs.33368 debited from A/c 1234 for credit card payment",
            timestamp = millis("2026-04-30")
        )
        val cardCredit = parsed(
            amount = 33_368.0,
            type = TransactionType.Income,
            raw = "Payment of Rs.33368 received towards your credit card ending 9494",
            timestamp = millis("2026-04-30") + 60_000L
        )

        val filtered = SmsTransactionNormalizer.filterImportBatch(listOf(debit, cardCredit))

        assertEquals(1, filtered.size)
        assertEquals(TransactionType.Expense, filtered.single().type)
        assertFalse(filtered.single().rawMessage.contains("received towards", ignoreCase = true))
    }

    @Test
    fun incomeSmsWithAccountHintParsesAsBankIncome() {
        val parsed = TransactionMessageParser.parse(
            message = "Rs.5000 credited to A/c XX1234 by NEFT from ACME LTD",
            transactionTimestampMillis = millis("2026-04-30"),
            sender = "HDFC"
        )

        assertTrue(parsed != null)
        assertEquals(TransactionType.Income, parsed?.type)
        assertEquals("HDFC A/C 1234", parsed?.bankName)
        assertFalse(parsed?.isCreditCardTransaction ?: true)
    }

    @Test
    fun cardSpendWithoutCreditCardTextStillGetsCardFlag() {
        val raw = "Rs.1250 debited on your HDFC card ending 4321 at AMAZON on 01-Jun"
        val parsed = TransactionMessageParser.parse(
            message = raw,
            transactionTimestampMillis = millis("2026-04-30"),
            sender = "HDFC"
        )

        assertTrue(SmsTransactionNormalizer.isCreditCardSpend(raw))
        assertTrue(parsed?.isCreditCardTransaction ?: false)
    }

    @Test
    fun cardBillPaymentDebitIsNotMarkedAsCardSpend() {
        val raw = "Rs.33368 debited from A/c 1234 for credit card payment"

        assertFalse(SmsTransactionNormalizer.isCreditCardSpend(raw))
    }

    private fun state(
        defaultAccountId: Long?,
        accounts: List<BankAccount> = listOf(bankA, bankB),
        summarySelectedAccountIds: Set<Long> = emptySet(),
        transactions: List<LedgerTransaction>
    ): FinanceUiState = FinanceUiState(
        isAppInitializing = false,
        selectedMonth = YearMonth.of(2026, 4),
        accounts = accounts,
        transactions = transactions,
        defaultAccountId = defaultAccountId,
        summarySelectedAccountIds = summarySelectedAccountIds
    )

    private fun tx(
        id: Long,
        amount: Double,
        type: TransactionType,
        date: String,
        accountId: Long?,
        raw: String? = null,
        exclude: Boolean = false,
        creditCard: Boolean = false
    ): LedgerTransaction = LedgerTransaction(
        id = id,
        name = if (type == TransactionType.Income) "Bank Credit" else "Spend",
        amount = amount,
        type = type,
        categoryId = 0L,
        accountId = accountId,
        timestampMillis = millis(date),
        rawMessage = raw,
        excludeFromSummary = exclude,
        isCreditCardTransaction = creditCard
    )

    private fun parsed(
        amount: Double,
        type: TransactionType,
        raw: String,
        timestamp: Long
    ): ParsedTransactionMessage = ParsedTransactionMessage(
        bankName = "HDFC A/C 1234",
        name = "Parsed",
        amount = amount,
        type = type,
        counterparty = if (type == TransactionType.Income) "Credit card" else "Credit card payment",
        rawMessage = raw,
        transactionTimestampMillis = timestamp
    )

    private fun millis(date: String): Long {
        return LocalDate.parse(date)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
