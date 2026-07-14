package com.moneymanager.app.data

import com.moneymanager.app.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionMessageParserSafetyTest {
    @Test
    fun creditCardUpiMerchantDoesNotIncludeDisputeText() {
        val message = "ICICI Bank Credit Card XX1003 debited for INR 250.00 on 11-Jun-26 for UPI-135909178578-AMAR SER. To dispute call 18001080/SMS BLOCK 1003 to 9215676766"

        val parsed = TransactionMessageParser.parse(
            message = message,
            transactionTimestampMillis = 1_000L,
            sender = "ICICI"
        )

        assertTrue(SmsTransactionNormalizer.isCreditCardSpend(message))
        assertTrue(parsed != null)
        assertTrue(parsed?.isCreditCardTransaction ?: false)
        assertEquals(TransactionType.Expense, parsed?.type)
        assertEquals("ICICI CARD 1003", parsed?.bankName)
        assertEquals("AMAR SER", parsed?.counterparty)
    }

    @Test
    fun creditCardOtpSmsIsIgnored() {
        val message = "123456 is the OTP for txn of Rs.12171.00 at AMAZON on your HDFC Bank Credit Card xx4321. " +
            "Valid till 13:45. Do not share OTP with anyone."

        val parsed = TransactionMessageParser.parse(
            message = message,
            transactionTimestampMillis = 1_000L,
            sender = "HDFC"
        )

        assertTrue(SmsTransactionNormalizer.isOtpVerificationArtifact(message))
        assertTrue(parsed == null)
    }

    @Test
    fun otpSmsWithFutureDebitWordingIsIgnored() {
        val message = "Use OTP 111222 to complete payment of Rs.12171.00 at FLIPKART. " +
            "Amount will be debited from your ICICI Bank Credit Card XX1003."

        val parsed = TransactionMessageParser.parse(
            message = message,
            transactionTimestampMillis = 1_000L,
            sender = "ICICI"
        )

        assertTrue(SmsTransactionNormalizer.isOtpVerificationArtifact(message))
        assertTrue(parsed == null)
    }

    @Test
    fun spendAlertWarningAboutOtpSharingIsStillParsed() {
        val message = "You've spent Rs.12171.00 on HDFC Bank Credit Card xx4321 at AMAZON on 14-Jul-26. " +
            "Never share OTP/CVV/PIN with anyone."

        val parsed = TransactionMessageParser.parse(
            message = message,
            transactionTimestampMillis = 1_000L,
            sender = "HDFC"
        )

        assertTrue(!SmsTransactionNormalizer.isOtpVerificationArtifact(message))
        assertTrue(parsed != null)
        assertEquals(TransactionType.Expense, parsed?.type)
        assertTrue(parsed?.isCreditCardTransaction ?: false)
    }

    @Test
    fun failedTransactionSmsIsIgnored() {
        val parsed = TransactionMessageParser.parse(
            message = "Rs.1250 transaction failed on your HDFC card ending 4321 at AMAZON",
            transactionTimestampMillis = 1_000L,
            sender = "HDFC"
        )

        assertTrue(SmsTransactionNormalizer.isFailedTransactionArtifact("Rs.1250 transaction failed on your card"))
        assertTrue(parsed == null)
    }
}
