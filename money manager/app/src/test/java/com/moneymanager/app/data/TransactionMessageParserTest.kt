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
    fun localLlmInterpretationCanSelectAmountWhenSmsContainsBalance() {
        TransactionMessageParser.localLlmInterpreter = LocalLlmTransactionInterpreter { _, _, _ ->
            LocalLlmTransactionInterpretation(
                amount = 250.0,
                type = TransactionType.Expense,
                bankName = "HDFC",
                accountHint = "1234",
                counterparty = "Neighborhood Store",
                confidence = 0.9
            )
        }

        try {
            val parsed = TransactionMessageParser.parse(
                message = "HDFC credit card ending 1234 available limit Rs.24,000 after Rs.250 spent at Neighborhood Store",
                transactionTimestampMillis = millis("2026-04-11"),
                sender = "HDFCBK",
                useLocalLlm = true
            )

            assertNotNull(parsed)
            requireNotNull(parsed)
            assertEquals(250.0, parsed.amount, 0.001)
            assertEquals(TransactionType.Expense, parsed.type)
            assertEquals("Neighborhood Store", parsed.counterparty)
            assertEquals("HDFC CARD 1234", parsed.bankName)
            assertTrue(parsed.requiresUserReview)
        } finally {
            TransactionMessageParser.localLlmInterpreter = null
        }
    }

    @Test
    fun localLlmCanSuggestExistingUserCategoryForReview() {
        TransactionMessageParser.localLlmInterpreter = LocalLlmTransactionInterpreter { _, _, categories ->
            LocalLlmTransactionInterpretation(
                amount = 820.0,
                type = TransactionType.Expense,
                bankName = "HDFC",
                accountHint = "1234",
                counterparty = "Apollo Pharmacy",
                suggestedCategoryId = categories.firstOrNull { it.name == "Medicine" }?.id,
                confidence = 0.9
            )
        }

        try {
            val parsed = TransactionMessageParser.parse(
                message = "Rs.820 spent using HDFC credit card ending 1234 at Apollo Pharmacy",
                transactionTimestampMillis = millis("2026-04-11"),
                sender = "HDFCBK",
                categories = listOf(
                    LocalLlmCategoryOption(0, "Uncategorized"),
                    LocalLlmCategoryOption(9, "Medicine")
                ),
                useLocalLlm = true
            )

            assertNotNull(parsed)
            requireNotNull(parsed)
            assertEquals(9L, parsed.suggestedCategoryId)
            assertTrue(parsed.categoryRequiresUserReview)
            assertTrue(parsed.requiresUserReview)
        } finally {
            TransactionMessageParser.localLlmInterpreter = null
        }
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
