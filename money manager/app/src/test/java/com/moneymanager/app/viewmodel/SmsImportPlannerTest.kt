package com.moneymanager.app.viewmodel

import com.moneymanager.app.data.ParsedTransactionMessage
import com.moneymanager.app.data.SmsBankKeys
import com.moneymanager.app.data.TransactionMessageParser
import com.moneymanager.app.model.AccountType
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsImportPlannerTest {
    private val hdfcBank = BankAccount(
        id = 1L,
        name = "HDFC Bank 1234",
        balance = 50_000.0
    )
    private val iciciBank = BankAccount(
        id = 2L,
        name = "ICICI Bank 5678",
        balance = 25_000.0
    )
    private val hdfcCard = BankAccount(
        id = 3L,
        name = "HDFC Card 4321",
        balance = 8_000.0,
        type = AccountType.CreditCard
    )

    @Test
    fun plannerPairsCrossBankDebitAndCreditIntoOneTransfer() {
        val debit = parsed(
            bankName = "HDFC A/C 1234",
            amount = 10_000.0,
            type = TransactionType.Expense,
            raw = "Rs.10000 debited from A/c 1234 by IMPS transfer to ICICI Bank",
            accountHint = "1234",
            timestamp = 1_000L
        )
        val credit = parsed(
            bankName = "ICICI A/C 5678",
            amount = 10_000.0,
            type = TransactionType.Income,
            raw = "Rs.10000 credited to A/c 5678 by IMPS from HDFC Bank",
            accountHint = "5678",
            timestamp = 2_000L
        )

        val planned = SmsImportPlanner.plan(listOf(credit, debit), listOf(hdfcBank, iciciBank))

        assertEquals(1, planned.size)
        val transfer = planned.single() as PlannedSmsImport.Transfer
        assertEquals(SmsTransferSource.BankTransfer, transfer.source)
        assertEquals(hdfcBank.id, transfer.fromAccountId)
        assertEquals(iciciBank.id, transfer.toAccountId)
        assertEquals(2, transfer.rawMessages.size)
    }

    @Test
    fun plannerPairsKnownUserAccountsEvenWithoutSelfTransferText() {
        val debit = parsed(
            bankName = "HDFC A/C 1234",
            amount = 10_000.0,
            type = TransactionType.Expense,
            raw = "Rs.10000 debited from A/c 1234 by UPI Ref 999",
            accountHint = "1234",
            timestamp = 1_000L
        )
        val credit = parsed(
            bankName = "ICICI A/C 5678",
            amount = 10_000.0,
            type = TransactionType.Income,
            raw = "Rs.10000 credited to A/c 5678 by UPI Ref 999",
            accountHint = "5678",
            timestamp = 2_000L
        )

        val planned = SmsImportPlanner.plan(listOf(debit, credit), listOf(hdfcBank, iciciBank))

        assertEquals(1, planned.size)
        val transfer = planned.single() as PlannedSmsImport.Transfer
        assertEquals(hdfcBank.id, transfer.fromAccountId)
        assertEquals(iciciBank.id, transfer.toAccountId)
    }

    @Test
    fun plannerRoutesCreditCardBillPaymentFromBankToCard() {
        val debit = parsed(
            bankName = "HDFC A/C 1234",
            amount = 4_500.0,
            type = TransactionType.Expense,
            raw = "Rs.4500 debited from A/c 1234 for credit card payment",
            accountHint = "1234",
            timestamp = 1_000L
        )

        val planned = SmsImportPlanner.plan(listOf(debit), listOf(hdfcBank, hdfcCard))

        assertEquals(1, planned.size)
        val transfer = planned.single() as PlannedSmsImport.Transfer
        assertEquals(SmsTransferSource.CreditCardPayment, transfer.source)
        assertEquals(hdfcBank.id, transfer.fromAccountId)
        assertEquals(hdfcCard.id, transfer.toAccountId)
    }

    @Test
    fun unresolvedBankTransferPairBecomesReviewPlan() {
        val debit = parsed(
            bankName = "HDFC A/C 1234",
            amount = 10_000.0,
            type = TransactionType.Expense,
            raw = "Rs.10000 debited from A/c 1234 by IMPS transfer to ICICI Bank",
            accountHint = "1234",
            timestamp = 1_000L
        )
        val credit = parsed(
            bankName = "ICICI A/C 5678",
            amount = 10_000.0,
            type = TransactionType.Income,
            raw = "Rs.10000 credited to A/c 5678 by IMPS from HDFC Bank",
            accountHint = "5678",
            timestamp = 2_000L
        )

        val planned = SmsImportPlanner.plan(listOf(debit, credit), listOf(hdfcBank))

        assertEquals(1, planned.size)
        val review = planned.single() as PlannedSmsImport.TransferReview
        assertEquals(SmsTransferSource.BankTransfer, review.source)
        assertEquals(hdfcBank.id, review.fromAccountId)
        assertEquals(null, review.toAccountId)
    }

    @Test
    fun unresolvedCreditCardBillPaymentBecomesReviewPlan() {
        val debit = parsed(
            bankName = "HDFC A/C 1234",
            amount = 4_500.0,
            type = TransactionType.Expense,
            raw = "Rs.4500 debited from A/c 1234 for credit card payment",
            accountHint = "1234",
            timestamp = 1_000L
        )

        val planned = SmsImportPlanner.plan(listOf(debit), listOf(hdfcBank))

        assertEquals(1, planned.size)
        val review = planned.single() as PlannedSmsImport.TransferReview
        assertEquals(SmsTransferSource.CreditCardPayment, review.source)
        assertEquals(hdfcBank.id, review.fromAccountId)
        assertEquals(null, review.toAccountId)
    }

    @Test
    fun singleLegOwnDebitTransferBecomesReviewPlan() {
        val debit = parsed(
            bankName = "HDFC A/C 1234",
            amount = 7_500.0,
            type = TransactionType.Expense,
            raw = "Rs.7500 debited from A/c 1234 for self transfer to own account via IMPS",
            accountHint = "1234",
            timestamp = 1_000L
        ).copy(isInternalTransfer = true, excludeFromSummary = true)

        val planned = SmsImportPlanner.plan(listOf(debit), listOf(hdfcBank, iciciBank))

        assertEquals(1, planned.size)
        val review = planned.single() as PlannedSmsImport.TransferReview
        assertEquals(SmsTransferSource.BankTransfer, review.source)
        assertEquals(hdfcBank.id, review.fromAccountId)
        assertEquals(null, review.toAccountId)
    }

    @Test
    fun singleLegOwnCreditTransferBecomesReviewPlan() {
        val credit = parsed(
            bankName = "ICICI A/C 5678",
            amount = 7_500.0,
            type = TransactionType.Income,
            raw = "Rs.7500 credited to A/c 5678 from own account by IMPS",
            accountHint = "5678",
            timestamp = 1_000L
        ).copy(isInternalTransfer = true, excludeFromSummary = true)

        val planned = SmsImportPlanner.plan(listOf(credit), listOf(hdfcBank, iciciBank))

        assertEquals(1, planned.size)
        val review = planned.single() as PlannedSmsImport.TransferReview
        assertEquals(SmsTransferSource.BankTransfer, review.source)
        assertEquals(null, review.fromAccountId)
        assertEquals(iciciBank.id, review.toAccountId)
    }

    @Test
    fun smsAccountResolverPrefersLastDigitsOverSharedBankRoot() {
        val resolved = SmsBankKeys.resolveAccountId(
            smsBankLabel = "HDFC CARD 4321",
            accounts = listOf(hdfcBank, hdfcCard)
        )

        assertEquals(hdfcCard.id, resolved)
    }

    @Test
    fun parserUsesSenderBankBeforeCounterpartyBankMention() {
        val parsed = TransactionMessageParser.parse(
            message = "Rs.10000 credited to A/c 5678 by IMPS from HDFC Bank",
            transactionTimestampMillis = 1_000L,
            sender = "ICICI"
        )

        assertTrue(parsed != null)
        assertEquals("ICICI A/C 5678", parsed?.bankName)
    }

    private fun parsed(
        bankName: String,
        amount: Double,
        type: TransactionType,
        raw: String,
        accountHint: String,
        timestamp: Long
    ): ParsedTransactionMessage = ParsedTransactionMessage(
        bankName = bankName,
        name = if (type == TransactionType.Income) "Bank Credit" else "Bank Debit",
        amount = amount,
        type = type,
        counterparty = if (type == TransactionType.Income) "Bank Credit" else "Bank Debit",
        rawMessage = raw,
        transactionTimestampMillis = timestamp,
        accountHint = accountHint
    )
}
