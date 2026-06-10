package com.moneymanager.app.data

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
