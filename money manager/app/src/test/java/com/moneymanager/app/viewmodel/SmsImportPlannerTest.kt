package com.moneymanager.app.viewmodel

import com.moneymanager.app.data.PAIRED_TRANSFER_SMS_DELIMITER
import com.moneymanager.app.data.ParsedTransactionMessage
import com.moneymanager.app.data.SmsBankKeys
import com.moneymanager.app.data.TransactionMessageParser
import com.moneymanager.app.model.AccountType
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun selfNameDebitPairsWithNamelessCreditOnOtherOwnAccount() {
        val iciciBank317 = BankAccount(id = 10L, name = "ICICI Bank 317", balance = 60_000.0)
        val federalBank6170 = BankAccount(id = 11L, name = "Federal Bank 6170", balance = 40_000.0)
        val debit = parsed(
            bankName = "ICICI A/C 317",
            amount = 10_100.0,
            type = TransactionType.Expense,
            raw = "ICICI Bank Acct XX317 debited for Rs 10100.00 on 01-Jun-26; DEVATHI N NITHI credited. UPI:651817989969.",
            accountHint = "317",
            timestamp = 1_000L,
            counterparty = "DEVATHI N NITHI"
        ).copy(isInternalTransfer = true, excludeFromSummary = true)
        val credit = parsed(
            bankName = "FEDERAL BANK A/C 6170",
            amount = 10_100.0,
            type = TransactionType.Income,
            raw = "Dear Customer, Rs.10100 credited to your A/c XX6170 on 01JUN2026 21:20:00. BAL-Rs.51025.25-Federal Bank",
            accountHint = "6170",
            timestamp = 12_000L,
            counterparty = "Your A"
        )

        val planned = SmsImportPlanner.plan(listOf(debit, credit), listOf(iciciBank317, federalBank6170))

        assertEquals(1, planned.size)
        val transfer = planned.single() as PlannedSmsImport.Transfer
        assertEquals(SmsTransferSource.BankTransfer, transfer.source)
        assertEquals(iciciBank317.id, transfer.fromAccountId)
        assertEquals(federalBank6170.id, transfer.toAccountId)
        assertEquals(2, transfer.rawMessages.size)
    }

    @Test
    fun selfNameDebitDoesNotConsumeSameAmountIncomeOnSameAccount() {
        val iciciBank317 = BankAccount(id = 10L, name = "ICICI Bank 317", balance = 60_000.0)
        val federalBank6170 = BankAccount(id = 11L, name = "Federal Bank 6170", balance = 40_000.0)
        val externalIncome = parsed(
            bankName = "ICICI A/C 317",
            amount = 8_517.0,
            type = TransactionType.Income,
            raw = "Dear Customer, Acct XX317 is credited with Rs 8517.00 on 26-Jun-26 from GANESH D BHAT. UPI:125354327848-ICICI Bank.",
            accountHint = "317",
            timestamp = 1_000L,
            counterparty = "GANESH D BHAT"
        )
        val selfDebit = parsed(
            bankName = "ICICI A/C 317",
            amount = 8_517.0,
            type = TransactionType.Expense,
            raw = "ICICI Bank Acct XX317 debited for Rs 8517.00 on 26-Jun-26; DEVATHI N NIKHI credited. UPI:597354304685.",
            accountHint = "317",
            timestamp = 242_000L,
            counterparty = "DEVATHI N NIKHI"
        ).copy(isInternalTransfer = true, excludeFromSummary = true)

        val planned = SmsImportPlanner.plan(
            listOf(externalIncome, selfDebit),
            listOf(iciciBank317, federalBank6170)
        )

        assertEquals(2, planned.size)
        assertTrue(planned.none { it is PlannedSmsImport.Transfer })
        val review = planned.filterIsInstance<PlannedSmsImport.TransferReview>().single()
        assertEquals(selfDebit.rawMessage, review.debit.rawMessage)
        val income = planned.filterIsInstance<PlannedSmsImport.Message>().single().message
        assertEquals(TransactionType.Income, income.type)
        assertFalse(income.excludeFromSummary)
    }

    @Test
    fun selfNameDebitDoesNotConsumeIncomeThatNamesAnExternalParty() {
        val iciciBank317 = BankAccount(id = 10L, name = "ICICI Bank 317", balance = 60_000.0)
        val federalBank6170 = BankAccount(id = 11L, name = "Federal Bank 6170", balance = 40_000.0)
        val externalIncome = parsed(
            bankName = "ICICI A/C 317",
            amount = 2_637.0,
            type = TransactionType.Income,
            raw = "Dear Customer, Acct XX317 is credited with Rs 2637.00 on 24-Jun-26 from TEJA S N. UPI:617519250049-ICICI Bank.",
            accountHint = "317",
            timestamp = 1_000L,
            counterparty = "TEJA S N"
        )
        val selfDebit = parsed(
            bankName = "FEDERAL BANK A/C 6170",
            amount = 2_637.0,
            type = TransactionType.Expense,
            raw = "Rs 2637.00 sent via UPI on 24-06-2026 at 20:57:00 to DEVATHI N NITHI.Ref:617519250050.",
            accountHint = "6170",
            timestamp = 5_000L,
            counterparty = "DEVATHI N NITHI"
        ).copy(isInternalTransfer = true, excludeFromSummary = true)

        val planned = SmsImportPlanner.plan(
            listOf(externalIncome, selfDebit),
            listOf(iciciBank317, federalBank6170)
        )

        assertTrue(planned.none { it is PlannedSmsImport.Transfer })
        val income = planned.filterIsInstance<PlannedSmsImport.Message>().single().message
        assertEquals("TEJA S N", income.counterparty)
        assertFalse(income.excludeFromSummary)
    }

    @Test
    fun selfTransferLegsPairAcrossHoursLongSmsDeliveryGap() {
        val iciciBank317 = BankAccount(id = 10L, name = "ICICI Bank 317", balance = 60_000.0)
        val federalBank6170 = BankAccount(id = 11L, name = "Federal Bank 6170", balance = 40_000.0)
        val debit = parsed(
            bankName = "ICICI A/C 317",
            amount = 5_000.0,
            type = TransactionType.Expense,
            raw = "ICICI Bank Acct XX317 debited for Rs 5000.00 on 12-Jun-26; DEVATHI N NITHI credited. UPI:652952167188.",
            accountHint = "317",
            timestamp = 1_000L,
            counterparty = "DEVATHI N NITHI"
        ).copy(isInternalTransfer = true, excludeFromSummary = true)
        val credit = parsed(
            bankName = "FEDERAL BANK A/C 6170",
            amount = 5_000.0,
            type = TransactionType.Income,
            raw = "Dear Customer, Rs.5000 credited to your A/c XX6170 on 12JUN2026 08:12:09. BAL-Rs.56025.25-Federal Bank",
            accountHint = "6170",
            timestamp = 1_000L + 3 * 60 * 60 * 1000L,
            counterparty = "Your A"
        )

        val planned = SmsImportPlanner.plan(listOf(debit, credit), listOf(iciciBank317, federalBank6170))

        assertEquals(1, planned.size)
        val transfer = planned.single() as PlannedSmsImport.Transfer
        assertEquals(iciciBank317.id, transfer.fromAccountId)
        assertEquals(federalBank6170.id, transfer.toAccountId)
    }

    @Test
    fun registeredAccountLegsPairAcrossLongGapWhenNoExternalPartyIsNamed() {
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
            timestamp = 1_000L + 30 * 60 * 1000L
        )

        val planned = SmsImportPlanner.plan(listOf(debit, credit), listOf(hdfcBank, iciciBank))

        assertEquals(1, planned.size)
        val transfer = planned.single() as PlannedSmsImport.Transfer
        assertEquals(hdfcBank.id, transfer.fromAccountId)
        assertEquals(iciciBank.id, transfer.toAccountId)
    }

    @Test
    fun externallyNamedLegsDoNotPairBeyondTightWindow() {
        val debit = parsed(
            bankName = "HDFC A/C 1234",
            amount = 10_000.0,
            type = TransactionType.Expense,
            raw = "HDFC Bank A/c 1234 debited for Rs 10000.00; SWIGGY LTD credited. UPI:111122223333.",
            accountHint = "1234",
            timestamp = 1_000L,
            counterparty = "SWIGGY LTD"
        )
        val credit = parsed(
            bankName = "ICICI A/C 5678",
            amount = 10_000.0,
            type = TransactionType.Income,
            raw = "Dear Customer, Acct XX5678 is credited with Rs 10000.00 on 24-Jun-26 from GANESH D BHAT. UPI:444455556666-ICICI Bank.",
            accountHint = "5678",
            timestamp = 1_000L + 30 * 60 * 1000L,
            counterparty = "GANESH D BHAT"
        )

        val planned = SmsImportPlanner.plan(listOf(debit, credit), listOf(hdfcBank, iciciBank))

        assertEquals(2, planned.size)
        assertTrue(planned.all { it is PlannedSmsImport.Message })
    }

    @Test
    fun singleLegSelfNameDebitAutoCompletesWhenOnlyOneOtherBankExists() {
        TransactionMessageParser.selfName = "Devathi N Nithin"
        try {
            val iciciBank317 = BankAccount(id = 10L, name = "ICICI Bank 317", balance = 60_000.0)
            val federalBank6170 = BankAccount(id = 11L, name = "Federal Bank 6170", balance = 40_000.0)
            val debit = parsed(
                bankName = "FEDERAL BANK A/C 6170",
                amount = 2_000.0,
                type = TransactionType.Expense,
                raw = "Debited Rs 2000.00 from a/c X6170 on 06Jul26 12:55 via UPI to DEVATHI N NI. Ref 618758297125.Bal Rs 44157.25. Not you?Call 18004251199 -Federal Bank",
                accountHint = "6170",
                timestamp = 1_000L,
                counterparty = "DEVATHI N NI"
            ).copy(isInternalTransfer = true, excludeFromSummary = true)

            val planned = SmsImportPlanner.plan(listOf(debit), listOf(iciciBank317, federalBank6170))

            assertEquals(1, planned.size)
            val transfer = planned.single() as PlannedSmsImport.Transfer
            assertEquals(SmsTransferSource.BankTransfer, transfer.source)
            assertEquals(federalBank6170.id, transfer.fromAccountId)
            assertEquals(iciciBank317.id, transfer.toAccountId)
        } finally {
            TransactionMessageParser.selfName = null
        }
    }

    @Test
    fun singleLegSelfNameDebitStaysReviewWhenDestinationIsAmbiguous() {
        TransactionMessageParser.selfName = "Devathi N Nithin"
        try {
            val iciciBank317 = BankAccount(id = 10L, name = "ICICI Bank 317", balance = 60_000.0)
            val federalBank6170 = BankAccount(id = 11L, name = "Federal Bank 6170", balance = 40_000.0)
            val debit = parsed(
                bankName = "FEDERAL BANK A/C 6170",
                amount = 2_000.0,
                type = TransactionType.Expense,
                raw = "Debited Rs 2000.00 from a/c X6170 on 06Jul26 12:55 via UPI to DEVATHI N NI. Ref 618758297125.Bal Rs 44157.25.",
                accountHint = "6170",
                timestamp = 1_000L,
                counterparty = "DEVATHI N NI"
            ).copy(isInternalTransfer = true, excludeFromSummary = true)

            val planned = SmsImportPlanner.plan(
                listOf(debit),
                listOf(iciciBank317, federalBank6170, hdfcBank)
            )

            assertEquals(1, planned.size)
            val review = planned.single() as PlannedSmsImport.TransferReview
            assertEquals(federalBank6170.id, review.fromAccountId)
            assertEquals(null, review.toAccountId)
        } finally {
            TransactionMessageParser.selfName = null
        }
    }

    @Test
    fun oppositeLegsOnSameAccountAreNeverPairedAsTransfer() {
        val iciciBank317 = BankAccount(id = 10L, name = "ICICI Bank 317", balance = 60_000.0)
        val federalBank6170 = BankAccount(id = 11L, name = "Federal Bank 6170", balance = 40_000.0)
        val income = parsed(
            bankName = "ICICI A/C 317",
            amount = 2_637.0,
            type = TransactionType.Income,
            raw = "Dear Customer, Acct XX317 is credited with Rs 2637.00 on 24-Jun-26 from TEJA S N. UPI:617519250049-ICICI Bank.",
            accountHint = "317",
            timestamp = 1_000L,
            counterparty = "TEJA S N"
        )
        val expense = parsed(
            bankName = "ICICI A/C 317",
            amount = 2_637.0,
            type = TransactionType.Expense,
            raw = "ICICI Bank Acct XX317 debited for Rs 2637.00 on 24-Jun-26; FUTUREOL PRIVAT credited. UPI:068680342221.",
            accountHint = "317",
            timestamp = 6_000L,
            counterparty = "FUTUREOL PRIVAT"
        )

        val planned = SmsImportPlanner.plan(
            listOf(income, expense),
            listOf(iciciBank317, federalBank6170)
        )

        assertEquals(2, planned.size)
        assertTrue(planned.all { it is PlannedSmsImport.Message })
    }

    @Test
    fun payAppBillPaymentRoutesFromPayingBankToCorrectIssuerCard() {
        // The bank debit names only the payment app; the attached card receipt (as
        // MoneyViewModel.attachCardPaymentReceipts joins it) carries the card + issuer.
        val iciciBank317 = BankAccount(id = 10L, name = "ICICI Bank 317", balance = 60_000.0)
        val hdfcCard0887 = BankAccount(
            id = 12L,
            name = "HDFC Card 0887",
            balance = 1_748.0,
            type = AccountType.CreditCard
        )
        val bankDebit = "ICICI Bank Acct XX317 debited for Rs 1748.00 on 21-Jul-26; " +
            "PhonePe credited. UPI:441675027689. Call 18002662 for dispute."
        val cardReceipt = "DEAR HDFCBANK CARDMEMBER, PAYMENT OF Rs. 1748.00 RECEIVED TOWARDS " +
            "YOUR CREDIT CARD ENDING WITH 0887 ON 21-7-2026.YOUR AVAILABLE LIMIT IS RS. 141000.00"
        val paired = parsed(
            bankName = "ICICI A/C 317",
            amount = 1_748.0,
            type = TransactionType.Expense,
            raw = bankDebit + PAIRED_TRANSFER_SMS_DELIMITER + cardReceipt,
            accountHint = "317",
            timestamp = 1_000L,
            counterparty = "Credit Card Payment"
        )

        val planned = SmsImportPlanner.plan(listOf(paired), listOf(iciciBank317, hdfcCard0887, hdfcBank))

        assertEquals(1, planned.size)
        val transfer = planned.single() as PlannedSmsImport.Transfer
        assertEquals(SmsTransferSource.CreditCardPayment, transfer.source)
        assertEquals(iciciBank317.id, transfer.fromAccountId)
        assertEquals(hdfcCard0887.id, transfer.toAccountId)
    }

    @Test
    fun spendOnLinkedAddOnCardResolvesToBillingGroupAccount() {
        // ICICI bills 3 cards (0006/1003/8010) under one account; user models it as a single
        // account whose primary is 8010 with 0006 and 1003 linked.
        val iciciGroup = BankAccount(
            id = 20L,
            name = "ICICI 8010",
            balance = 41_128.0,
            type = AccountType.CreditCard,
            linkedCardNumbers = listOf("0006", "1003")
        )
        val hdfcCard0887 = BankAccount(
            id = 21L,
            name = "HDFC 0887",
            balance = 0.0,
            type = AccountType.CreditCard
        )

        // A spend on the add-on card 0006 must land on the ICICI group, not the HDFC card.
        assertEquals(
            iciciGroup.id,
            SmsBankKeys.resolveAccountId("ICICI CARD 0006", listOf(iciciGroup, hdfcCard0887))
        )
        // A spend on 1003 too.
        assertEquals(
            iciciGroup.id,
            SmsBankKeys.resolveAccountId("ICICI CARD 1003", listOf(iciciGroup, hdfcCard0887))
        )
        // The primary card's own spends still resolve.
        assertEquals(
            iciciGroup.id,
            SmsBankKeys.resolveAccountId("ICICI CARD 8010", listOf(iciciGroup, hdfcCard0887))
        )
        // HDFC card spends stay on the HDFC account.
        assertEquals(
            hdfcCard0887.id,
            SmsBankKeys.resolveAccountId("HDFC CARD 0887", listOf(iciciGroup, hdfcCard0887))
        )
    }

    @Test
    fun anyIciciCardPoolsToTheSingleIciciCreditCardAccount() {
        val iciciGroup = BankAccount(
            id = 20L,
            name = "ICICI 0006",
            balance = 18_160.0,
            type = AccountType.CreditCard,
            linkedCardNumbers = listOf("1003", "8010", "5002")
        )
        val hdfcFreedom = BankAccount(id = 21L, name = "HDFC FREEDOM 0175", balance = 0.0, type = AccountType.CreditCard)
        val hdfcSwiggy = BankAccount(id = 22L, name = "HDFC SWIGGY 0887", balance = 0.0, type = AccountType.CreditCard)
        val cards = listOf(iciciGroup, hdfcFreedom, hdfcSwiggy)

        // A linked card (1003) pools to the ICICI account.
        assertEquals(iciciGroup.id, SmsBankKeys.resolveAccountId("ICICI CARD 1003", cards))
        // The primary card (0006) too.
        assertEquals(iciciGroup.id, SmsBankKeys.resolveAccountId("ICICI CARD 0006", cards))
        // Even an ICICI card number the user never listed still pools to the single ICICI
        // credit-card account, matched by issuer.
        assertEquals(iciciGroup.id, SmsBankKeys.resolveAccountId("ICICI CARD 4444", cards))
    }

    @Test
    fun cardHintReadsIciciConsolidatedAccountFormats() {
        assertEquals(
            "8010",
            SmsBankKeys.cardHint("Payment of INR 41128.00 received on your ICICI Bank Credit Card Account 4xxx8010")
        )
        assertEquals(
            "8010",
            SmsBankKeys.cardHint("Your credit card bill for ICICI Bank XXXX-8010 has been generated.")
        )
    }

    @Test
    fun issuerRootReadsBankNamedInsideCardReceipt() {
        val receipt = "DEAR HDFCBANK CARDMEMBER, PAYMENT OF Rs. 1748.00 RECEIVED TOWARDS " +
            "YOUR CREDIT CARD ENDING WITH 0887"
        assertEquals("HDFC", SmsBankKeys.issuerRoot(receipt))
    }

    private fun parsed(
        bankName: String,
        amount: Double,
        type: TransactionType,
        raw: String,
        accountHint: String,
        timestamp: Long,
        counterparty: String? = null
    ): ParsedTransactionMessage {
        val party = counterparty
            ?: if (type == TransactionType.Income) "Bank Credit" else "Bank Debit"
        return ParsedTransactionMessage(
            bankName = bankName,
            name = party,
            amount = amount,
            type = type,
            counterparty = party,
            rawMessage = raw,
            transactionTimestampMillis = timestamp,
            accountHint = accountHint
        )
    }
}
