package com.moneymanager.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The "CC rows may carry one extra tag, everything else carries exactly one" rule. */
class SecondaryCategoryRuleTest {

    private val ccId = DefaultCategories.items.first { it.name == "CC" }.id
    private val shoppingId = DefaultCategories.items.first { it.name == "Shopping" }.id
    private val foodId = DefaultCategories.items.first { it.name == "Food" }.id
    private val state = FinanceUiState(isAppInitializing = false)

    @Test
    fun ccCategoryKeepsItsSecondTag() {
        val tagged = state.normalizeSecondaryCategory(tx(categoryId = ccId, secondaryCategoryId = shoppingId))

        assertEquals(shoppingId, tagged.secondaryCategoryId)
    }

    @Test
    fun cardFlaggedRowKeepsItsSecondTagEvenWhenPrimaryIsNotCc() {
        val tagged = state.normalizeSecondaryCategory(
            tx(categoryId = foodId, secondaryCategoryId = shoppingId, isCreditCard = true)
        )

        assertEquals(shoppingId, tagged.secondaryCategoryId)
    }

    @Test
    fun movingPrimaryOffCcDropsTheSecondTag() {
        val tagged = state.normalizeSecondaryCategory(tx(categoryId = foodId, secondaryCategoryId = shoppingId))

        assertNull(tagged.secondaryCategoryId)
    }

    @Test
    fun secondTagDuplicatingThePrimaryIsDropped() {
        val tagged = state.normalizeSecondaryCategory(
            tx(categoryId = ccId, secondaryCategoryId = ccId, isCreditCard = true)
        )

        assertNull(tagged.secondaryCategoryId)
    }

    @Test
    fun secondTagPointingAtADeletedCategoryIsDropped() {
        val tagged = state.normalizeSecondaryCategory(tx(categoryId = ccId, secondaryCategoryId = 9_999L))

        assertNull(tagged.secondaryCategoryId)
    }

    @Test
    fun onlyCcRowsAreOfferedASecondTag() {
        assertTrue(state.allowsSecondaryCategory(ccId, isCreditCardTransaction = false))
        assertTrue(state.allowsSecondaryCategory(foodId, isCreditCardTransaction = true))
        assertFalse(state.allowsSecondaryCategory(foodId, isCreditCardTransaction = false))
    }

    /** The options exclude CC and the primary, so a row can never hold more than two categories. */
    @Test
    fun secondTagOptionsExcludeCcAndThePrimary() {
        val options = state.secondaryCategoryOptions(ccId).map { it.id }

        assertFalse(ccId in options)
        assertTrue(shoppingId in options)
        assertEquals(state.categories.size - 1, options.size)
    }

    private fun tx(
        categoryId: Long,
        secondaryCategoryId: Long?,
        isCreditCard: Boolean = false
    ) = LedgerTransaction(
        id = 1L,
        name = "Card swipe",
        amount = 1_200.0,
        type = TransactionType.Expense,
        categoryId = categoryId,
        secondaryCategoryId = secondaryCategoryId,
        accountId = 1L,
        timestampMillis = 0L,
        isCreditCardTransaction = isCreditCard
    )
}
