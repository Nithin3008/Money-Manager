package com.moneymanager.app.viewmodel

import com.moneymanager.app.data.TransactionMessageParser
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.MoneyIcons
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryLearningTest {
    private fun cat(id: Long, name: String, iconKey: String = "category") = CategoryItem(
        id = id,
        name = name,
        iconKey = iconKey,
        icon = MoneyIcons.resolveCategoryIcon(iconKey),
        isDefault = true,
        colorHex = "#FFFFFF"
    )

    private val categories = listOf(
        cat(0, "Uncategorized"),
        cat(2, "Food", "food"),
        cat(7, "CC", "credit_card")
    )

    @Test
    fun creditCardOnUpiSpendIsCategorizedAsCc() {
        val msg = TransactionMessageParser.parse(
            "ICICI Bank Credit Card XX1003 debited for INR 250.00 on 11-Jun-26 " +
                "for UPI-135909178578-AMAR SER. To dispute call 18001080/SMS BLOCK 1003 to 9215676766",
            transactionTimestampMillis = 1_000L,
            sender = "ICICI"
        )!!

        val id = CategoryLearning.inferCategoryId(msg, categories, emptyList())
        assertEquals(7L, id)
    }

    @Test
    fun creditCardOnUpiCategoryWinsOverLearnedMerchantCategory() {
        // Even if the same UPI merchant was previously seen on a bank account and categorized
        // as Food, a credit-card-on-UPI spend must still land in CC.
        val priorBankFood = com.moneymanager.app.model.LedgerTransaction(
            id = 1,
            name = "AMAR SER",
            amount = 100.0,
            type = com.moneymanager.app.model.TransactionType.Expense,
            categoryId = 2,
            accountId = 1,
            timestampMillis = 500L,
            isAutoDetected = true,
            rawMessage = "Rs.100 debited from A/c 317 to AMAR SER via UPI"
        )
        val msg = TransactionMessageParser.parse(
            "ICICI Bank Credit Card XX1003 debited for INR 250.00 on 11-Jun-26 " +
                "for UPI-135909178578-AMAR SER.",
            transactionTimestampMillis = 1_000L,
            sender = "ICICI"
        )!!

        val id = CategoryLearning.inferCategoryId(msg, categories, listOf(priorBankFood))
        assertEquals(7L, id)
    }
}
