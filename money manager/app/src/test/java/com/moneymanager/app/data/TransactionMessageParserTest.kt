package com.moneymanager.app.data

import com.moneymanager.app.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionMessageParserTest {

    @Test
    fun selfTransferMessageIsMarkedExcludedFromSummary() {
        val parsed = TransactionMessageParser.parse(
            message = "Rs.5,000 debited from HDFC Bank A/c xx1234 and transferred to own A/c xx9876 via IMPS",
            transactionTimestampMillis = millis("2026-04-10"),
            sender = "HDFCBK"
        )

        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(TransactionType.Expense, parsed.type)
        assertEquals(5_000.0, parsed.amount, 0.001)
        assertEquals("1234", parsed.accountHint)
        assertTrue(parsed.isInternalTransfer)
        assertTrue(parsed.excludeFromSummary)
    }

    @Test
    fun sameUserTransferPairKeepsBothLegsButExcludesThemFromSummary() {
        val debit = parsed(
            bankName = "HDFC A/C 1234",
            amount = 12_000.0,
            type = TransactionType.Expense,
            raw = "Rs.12000 debited from A/c 1234 for self transfer to A/c 9876",
            timestamp = millis("2026-04-10")
        )
        val credit = parsed(
            bankName = "HDFC A/C 9876",
            amount = 12_000.0,
            type = TransactionType.Income,
            raw = "Rs.12000 credited to A/c 9876 from own account 1234",
            timestamp = millis("2026-04-10") + 60_000L
        )

        val filtered = SmsTransactionNormalizer.filterImportBatch(listOf(debit, credit))

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.isInternalTransfer })
        assertTrue(filtered.all { it.excludeFromSummary })
        assertTrue(filtered.any { it.type == TransactionType.Expense })
        assertTrue(filtered.any { it.type == TransactionType.Income })
    }

    @Test
    fun creditCardRepaymentStillDropsOnlyCardCreditLeg() {
        val debit = parsed(
            bankName = "HDFC A/C 1234",
            amount = 33_368.0,
            type = TransactionType.Expense,
            raw = "Rs.33368 debited from A/c 1234 for credit card payment",
            timestamp = millis("2026-04-30")
        )
        val cardCredit = parsed(
            bankName = "HDFC A/C 9494",
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
    fun paymentAppCardConfirmationIsDroppedAsDuplicateOfBankAlert() {
        val parsed = TransactionMessageParser.parse(
            message = "Paid Rs. 5000 to SHARK FITNESS STUDIO on 06-Jun-26 using ICICI Bank Credit Card. To view receipt, visit https://paytm.me/PYTMPS/CL5zE6P - Paytm Payments",
            transactionTimestampMillis = millis("2026-06-06"),
            sender = "AD-PYTMPS-S"
        )

        assertTrue(SmsTransactionNormalizer.isPaymentAppCardConfirmation(
            "Paid Rs. 5000 to SHARK FITNESS STUDIO on 06-Jun-26 using ICICI Bank Credit Card."
        ))
        assertTrue(parsed == null)
    }

    @Test
    fun bankCardSpendWithMaskedNumberIsNotTreatedAsPaymentAppDuplicate() {
        val parsed = TransactionMessageParser.parse(
            message = "Rs 5,000.00 spent on ICICI Bank Card XX8010 on 06-Jun-26 at SHARK FITNESS S. Avl Lmt: Rs 1,13,148.00. To dispute, call 18002662/SMS BLOCK 8010 to 9215676766.",
            transactionTimestampMillis = millis("2026-06-06"),
            sender = "AD-ICICIT-S"
        )

        assertNotNull(parsed)
        requireNotNull(parsed)
        assertTrue(parsed.isCreditCardTransaction)
        assertEquals(5_000.0, parsed.amount, 0.001)
    }

    @Test
    fun upiDebitToOwnNameBecomesInternalTransfer() {
        TransactionMessageParser.selfName = "Devathi N Nithin"
        try {
            val parsed = TransactionMessageParser.parse(
                message = "ICICI Bank Acct XX317 debited for Rs 10100.00 on 01-Jun-26; DEVATHI N NITHI credited. UPI:651817989969. Call 18002662 for dispute. SMS BLOCK 317 to 9215676766.",
                transactionTimestampMillis = millis("2026-06-01"),
                sender = "AD-ICICIT-S"
            )

            assertNotNull(parsed)
            requireNotNull(parsed)
            assertEquals(TransactionType.Expense, parsed.type)
            assertEquals("DEVATHI N NITHI", parsed.counterparty)
            assertTrue(parsed.isInternalTransfer)
            assertTrue(parsed.excludeFromSummary)
        } finally {
            TransactionMessageParser.selfName = null
        }
    }

    @Test
    fun upiDebitToOwnNameToleratesOneLetterTypoAndTruncation() {
        TransactionMessageParser.selfName = "Nithin"
        try {
            val parsed = TransactionMessageParser.parse(
                message = "ICICI Bank Acct XX317 debited for Rs 8517.00 on 26-Jun-26; DEVATHI N NIKHI credited. UPI:597354304685. Call 18002662 for dispute. SMS BLOCK 317 to 9215676766.",
                transactionTimestampMillis = millis("2026-06-26"),
                sender = "JX-ICICIT-S"
            )

            assertNotNull(parsed)
            requireNotNull(parsed)
            assertTrue(parsed.isInternalTransfer)
            assertTrue(parsed.excludeFromSummary)
        } finally {
            TransactionMessageParser.selfName = null
        }
    }

    @Test
    fun heavilyTruncatedOwnNameStillMarksInternalTransfer() {
        TransactionMessageParser.selfName = "Devathi N Nithin"
        try {
            val parsed = TransactionMessageParser.parse(
                message = "Debited Rs 2000.00 from a/c X6170 on 06Jul26 12:55 via UPI to DEVATHI N NI. Ref 618758297125.Bal Rs 44157.25. Not you?Call 18004251199 -Federal Bank",
                transactionTimestampMillis = millis("2026-07-06"),
                sender = "AX-FEDBNK-S"
            )

            assertNotNull(parsed)
            requireNotNull(parsed)
            assertEquals(TransactionType.Expense, parsed.type)
            assertEquals(2_000.0, parsed.amount, 0.001)
            assertEquals("DEVATHI N NI", parsed.counterparty)
            assertTrue(parsed.isInternalTransfer)
            assertTrue(parsed.excludeFromSummary)
        } finally {
            TransactionMessageParser.selfName = null
        }
    }

    @Test
    fun truncatedNameSequenceDoesNotMatchOtherPeople() {
        TransactionMessageParser.selfName = "Devathi N Nithin"
        try {
            val parsed = TransactionMessageParser.parse(
                message = "Debited Rs 500.00 from a/c X6170 on 06Jul26 12:55 via UPI to DEVARAJ K. Ref 618758297126.Bal Rs 44157.25. Not you?Call 18004251199 -Federal Bank",
                transactionTimestampMillis = millis("2026-07-06"),
                sender = "AX-FEDBNK-S"
            )

            assertNotNull(parsed)
            requireNotNull(parsed)
            assertEquals(TransactionType.Expense, parsed.type)
            assertFalse(parsed.isInternalTransfer)
        } finally {
            TransactionMessageParser.selfName = null
        }
    }

    @Test
    fun upiCreditFromOwnNameBecomesInternalTransfer() {
        TransactionMessageParser.selfName = "Devathi N Nithin"
        try {
            val parsed = TransactionMessageParser.parse(
                message = "Dear Customer, Acct XX317 is credited with Rs 25.00 on 12-Jun-26 from DEVATHI N NITHI. UPI:616319308338-ICICI Bank.",
                transactionTimestampMillis = millis("2026-06-12"),
                sender = "AD-ICICIT-S"
            )

            assertNotNull(parsed)
            requireNotNull(parsed)
            assertEquals(TransactionType.Income, parsed.type)
            assertEquals("DEVATHI N NITHI", parsed.counterparty)
            assertTrue(parsed.isInternalTransfer)
            assertTrue(parsed.excludeFromSummary)
        } finally {
            TransactionMessageParser.selfName = null
        }
    }

    @Test
    fun upiDebitToUnrelatedPartyStaysAnExpenseWhenSelfNameIsSet() {
        TransactionMessageParser.selfName = "Devathi N Nithin"
        try {
            val parsed = TransactionMessageParser.parse(
                message = "ICICI Bank Acct XX317 debited for Rs 70.00 on 10-Jun-26; SONIKA credited. UPI:307779497670. Call 18002662 for dispute. SMS BLOCK 317 to 9215676766.",
                transactionTimestampMillis = millis("2026-06-10"),
                sender = "AD-ICICIT-S"
            )

            assertNotNull(parsed)
            requireNotNull(parsed)
            assertEquals(TransactionType.Expense, parsed.type)
            assertFalse(parsed.isInternalTransfer)
            assertFalse(parsed.excludeFromSummary)
        } finally {
            TransactionMessageParser.selfName = null
        }
    }

    @Test
    fun rdAutoDebitIsInternalTransferWithCleanCounterparty() {
        val parsed = TransactionMessageParser.parse(
            message = "ICICI Bank Acc XX317 debited Rs. 6,000.00 on 05-Jun-26 InfoTo RD Ac no 7.Avl Bal Rs. 9,680.34.To dispute call 18002662 or SMS BLOCK 317 to 9215676766",
            transactionTimestampMillis = millis("2026-06-06"),
            sender = "AX-ICICIT-S"
        )

        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(TransactionType.Expense, parsed.type)
        assertEquals(6_000.0, parsed.amount, 0.001)
        assertEquals("RD/FD Deposit", parsed.counterparty)
        assertTrue(parsed.isInternalTransfer)
        assertTrue(parsed.excludeFromSummary)
    }

    @Test
    fun iciciInfoBilDebitIsCreditCardBillPaymentTransfer() {
        val parsed = TransactionMessageParser.parse(
            message = "ICICI Bank Acc XX317 debited Rs. 15,524.00 on 01-Jun-26 InfoBIL*INFT*FF15.Avl Bal Rs. 53,898.02.To dispute call 18002662 or SMS BLOCK 317 to 9215676766",
            transactionTimestampMillis = millis("2026-06-01"),
            sender = "JK-ICICIT-S"
        )

        assertTrue(SmsTransactionNormalizer.isCreditCardBillPaymentDebit(
            "ICICI Bank Acc XX317 debited Rs. 15,524.00 on 01-Jun-26 InfoBIL*INFT*FF15."
        ))
        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(15_524.0, parsed.amount, 0.001)
        assertEquals("Credit Card Payment", parsed.counterparty)
        assertTrue(parsed.isInternalTransfer)
        assertTrue(parsed.excludeFromSummary)
    }

    @Test
    fun selfMarkedTransferLegsPairInBatchAcrossHoursLongGap() {
        val debit = parsed(
            bankName = "ICICI A/C 317",
            amount = 2_223.0,
            type = TransactionType.Expense,
            raw = "ICICI Bank Acct XX317 debited for Rs 2223.00 on 29-Jun-26; DEVATHI N NITHI credited. UPI:618021239968.",
            timestamp = millis("2026-06-29")
        ).copy(isInternalTransfer = true, excludeFromSummary = true)
        val credit = parsed(
            bankName = "FEDERAL BANK A/C 6170",
            amount = 2_223.0,
            type = TransactionType.Income,
            raw = "Dear Customer, Rs.2223 credited to your A/c XX6170 on 29JUN2026 12:28:04. BAL-Rs.58166.25-Federal Bank",
            timestamp = millis("2026-06-29") + 4 * 60 * 60 * 1000L
        )

        val filtered = SmsTransactionNormalizer.filterImportBatch(listOf(debit, credit))

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.isInternalTransfer })
        assertTrue(filtered.all { it.excludeFromSummary })
    }

    @Test
    fun foreignCurrencyRemittanceIsFlaggedForReview() {
        val parsed = TransactionMessageParser.parse(
            message = "ICICI Bank Acc XX317 debited Rs. 19,999.68 on 03-Jun-26 InfoNRS*USD206.72.Avl Bal Rs. 35,124.34.To dispute call 18002662 or SMS BLOCK 317 to 9215676766",
            transactionTimestampMillis = millis("2026-06-03"),
            sender = "AX-ICICIT-S"
        )

        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(19_999.68, parsed.amount, 0.001)
        assertEquals("Foreign Remittance", parsed.counterparty)
        assertTrue(parsed.requiresUserReview)
    }

    @Test
    fun transactionReferenceIsExtractedFromAllBankFormats() {
        assertEquals(
            "651817989969",
            SmsTransactionNormalizer.transactionReference(
                "ICICI Bank Acct XX317 debited for Rs 10100.00 on 01-Jun-26; DEVATHI N NITHI credited. UPI:651817989969. Call 18002662 for dispute."
            )
        )
        assertEquals(
            "618758297125",
            SmsTransactionNormalizer.transactionReference(
                "Debited Rs 2000.00 from a/c X6170 on 06Jul26 12:55 via UPI to DEVATHI N NI. Ref 618758297125.Bal Rs 44157.25."
            )
        )
        assertEquals(
            "441026163012",
            SmsTransactionNormalizer.transactionReference(
                "Rs 20000.00 sent via UPI on 01-06-2026 at 08:12:41 to HEMALATHA PONN.Ref:441026163012.Not you? Call 18004251199"
            )
        )
        assertEquals(
            "615384166398",
            SmsTransactionNormalizer.transactionReference(
                "ICICI Bank Account XX317 is credited with Rs 20,000.00 on 02-Jun-26 by Account linked to mobile number XXXXX82020. IMPS Ref. no. 615384166398."
            )
        )
        assertEquals(
            null,
            SmsTransactionNormalizer.transactionReference(
                "ASSPL refund of Rs 650.95 credited to your ICICI Bank Credit Card XX0006 on 18-JUN-26."
            )
        )
    }

    private fun parsed(
        bankName: String,
        amount: Double,
        type: TransactionType,
        raw: String,
        timestamp: Long
    ): ParsedTransactionMessage = ParsedTransactionMessage(
        bankName = bankName,
        name = if (type == TransactionType.Income) "Bank Credit" else "Bank Transaction",
        amount = amount,
        type = type,
        counterparty = if (type == TransactionType.Income) "Bank Credit" else "Bank Transaction",
        rawMessage = raw,
        transactionTimestampMillis = timestamp,
        accountHint = bankName.substringAfterLast(' ', "").takeIf { it.isNotBlank() }
    )

    private fun millis(date: String): Long {
        return LocalDate.parse(date)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
