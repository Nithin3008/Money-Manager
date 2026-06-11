package com.moneymanager.app.data

import com.moneymanager.app.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionMessageParserSafetyTest {
    @Test
    fun ordinaryBankDebitDoesNotInvokeLocalLlmEvenWhenRequested() {
        var invoked = false
        TransactionMessageParser.localLlmInterpreter = LocalLlmTransactionInterpreter { _, _, _ ->
            invoked = true
            LocalLlmTransactionInterpretation(confidence = 0.95)
        }

        try {
            val parsed = TransactionMessageParser.parse(
                message = "Rs.1250 debited from A/c 1234 at grocery store",
                transactionTimestampMillis = 1_000L,
                sender = "HDFC",
                useLocalLlm = true
            )

            assertTrue(parsed != null)
            assertFalse(invoked)
            assertFalse(parsed?.requiresUserReview ?: true)
        } finally {
            TransactionMessageParser.localLlmInterpreter = null
        }
    }

    @Test
    fun creditCardSpendCanInvokeLocalLlmAndRequiresReview() {
        var invoked = false
        TransactionMessageParser.localLlmInterpreter = LocalLlmTransactionInterpreter { _, _, _ ->
            invoked = true
            LocalLlmTransactionInterpretation(
                counterparty = "Amazon India",
                isCreditCardTransaction = true,
                confidence = 0.95
            )
        }

        try {
            val parsed = TransactionMessageParser.parse(
                message = "Rs.1250 spent using HDFC credit card ending 4321 at AMZN MKTP",
                transactionTimestampMillis = 1_000L,
                sender = "HDFC",
                useLocalLlm = true
            )

            assertTrue(parsed != null)
            assertTrue(invoked)
            assertTrue(parsed?.requiresUserReview ?: false)
            assertTrue(parsed?.isCreditCardTransaction ?: false)
        } finally {
            TransactionMessageParser.localLlmInterpreter = null
        }
    }

    @Test
    fun creditCardUpiUsageInvokesLocalLlmAndStaysInCcBucket() {
        var invoked = false
        val message = "ICICI Bank Credit Card XX1003 debited for INR 250.00 on 11-Jun-26 for UPI-135909178578-AMAR SER. To dispute call 18001080/SMS BLOCK 1003 to 9215676766"
        TransactionMessageParser.localLlmInterpreter = LocalLlmTransactionInterpreter { _, _, categories ->
            invoked = true
            LocalLlmTransactionInterpretation(
                type = TransactionType.Expense,
                counterparty = "AMAR SER",
                isCreditCardTransaction = true,
                suggestedCategoryId = categories.firstOrNull { it.name == "CC" }?.id,
                confidence = 0.95
            )
        }

        try {
            val parsed = TransactionMessageParser.parse(
                message = message,
                transactionTimestampMillis = 1_000L,
                sender = "ICICI",
                categories = listOf(
                    LocalLlmCategoryOption(0, "Uncategorized"),
                    LocalLlmCategoryOption(7, "CC")
                ),
                useLocalLlm = true
            )

            assertTrue(SmsTransactionNormalizer.isCreditCardSpend(message))
            assertTrue(parsed != null)
            assertTrue(invoked)
            assertTrue(parsed?.requiresUserReview ?: false)
            assertTrue(parsed?.isCreditCardTransaction ?: false)
            assertEquals(TransactionType.Expense, parsed?.type)
            assertEquals("ICICI CARD 1003", parsed?.bankName)
            assertEquals("AMAR SER", parsed?.counterparty)
            assertEquals(7L, parsed?.suggestedCategoryId)
        } finally {
            TransactionMessageParser.localLlmInterpreter = null
        }
    }

    @Test
    fun creditCardUpiMerchantDoesNotIncludeDisputeTextWithoutLlm() {
        val message = "ICICI Bank Credit Card XX1003 debited for INR 250.00 on 11-Jun-26 for UPI-135909178578-AMAR SER. To dispute call 18001080/SMS BLOCK 1003 to 9215676766"

        val parsed = TransactionMessageParser.parse(
            message = message,
            transactionTimestampMillis = 1_000L,
            sender = "ICICI",
            useLocalLlm = false
        )

        assertTrue(parsed != null)
        assertTrue(parsed?.isCreditCardTransaction ?: false)
        assertEquals(TransactionType.Expense, parsed?.type)
        assertEquals("ICICI CARD 1003", parsed?.bankName)
        assertEquals("AMAR SER", parsed?.counterparty)
    }

    @Test
    fun creditCardRefundInvokesLocalLlmAndIsNotFilteredAsSettlement() {
        var invoked = false
        val message = "Refund of Rs.1250 credited to your HDFC credit card ending 4321 from AMAZON"
        TransactionMessageParser.localLlmInterpreter = LocalLlmTransactionInterpreter { _, _, _ ->
            invoked = true
            LocalLlmTransactionInterpretation(
                type = TransactionType.Income,
                counterparty = "Amazon",
                isCreditCardTransaction = true,
                confidence = 0.95
            )
        }

        try {
            val parsed = TransactionMessageParser.parse(
                message = message,
                transactionTimestampMillis = 1_000L,
                sender = "HDFC",
                useLocalLlm = true
            )

            assertTrue(SmsTransactionNormalizer.isCreditCardRefund(message))
            assertFalse(SmsTransactionNormalizer.isCreditCardSettlementArtifact(message))
            assertTrue(parsed != null)
            assertTrue(invoked)
            assertTrue(parsed?.requiresUserReview ?: false)
            assertTrue(parsed?.isCreditCardTransaction ?: false)
            assertTrue(parsed?.type == TransactionType.Income)
        } finally {
            TransactionMessageParser.localLlmInterpreter = null
        }
    }

    @Test
    fun failedTransactionSmsIsIgnored() {
        val parsed = TransactionMessageParser.parse(
            message = "Rs.1250 transaction failed on your HDFC card ending 4321 at AMAZON",
            transactionTimestampMillis = 1_000L,
            sender = "HDFC",
            useLocalLlm = true
        )

        assertTrue(SmsTransactionNormalizer.isFailedTransactionArtifact("Rs.1250 transaction failed on your card"))
        assertTrue(parsed == null)
    }
}
