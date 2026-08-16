package com.moneymanager.app

import com.moneymanager.app.data.SmsTransactionNormalizer
import com.moneymanager.app.data.TransactionMessageParser
import com.moneymanager.app.model.AccountType
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.TransactionType
import com.moneymanager.app.viewmodel.PlannedSmsImport
import com.moneymanager.app.viewmodel.SmsImportPlanner
import com.moneymanager.app.viewmodel.SmsTransferSource
import java.io.File
import java.time.OffsetDateTime
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Temporary end-to-end verification: replays real June/July 2026 bank SMSes through the
 * full pipeline (parse -> filterImportBatch -> plan) exactly as a scan would run it.
 */
class JuneReplayVerificationTest {

    private data class Sms(val at: String, val sender: String, val body: String)

    private val messages = listOf(
        // -- ordinary expenses / incomes (representative of ~170 June rows) --
        Sms("2026-06-01T08:13:01+05:30", "BG-FEDBNK-S", "Rs 20000.00 sent via UPI on 01-06-2026 at 08:12:41 to HEMALATHA PONN.Ref:441026163012.Not you? Call 18004251199/SMS BLOCKUPI to 98950 88888 -Federal Bank"),
        Sms("2026-06-01T09:47:49+05:30", "AD-ICICIT-S", "ICICI Bank Acct XX317 debited for Rs 230.00 on 01-Jun-26; srinivasreddy31 credited. UPI:651870948446. Call 18002662 for dispute. SMS BLOCK 317 to 9215676766."),
        Sms("2026-06-01T20:05:34+05:30", "JK-ICICIT-S", "ICICI Bank Account XX317 credited:Rs. 64,629.00 on 01-Jun-26. Info NEFT-YESIG61520213666-UNIQUE. Available Balance is Rs. 69,422.02."),
        Sms("2026-06-02T12:55:03+05:30", "AD-ICICIT-S", "ICICI Bank Account XX317 is credited with Rs 20,000.00 on 02-Jun-26 by Account linked to mobile number XXXXX82020. IMPS Ref. no. 615384166398."),
        Sms("2026-06-02T19:27:26+05:30", "AX-ICICIT-S", "Dear Customer, Acct XX317 is credited with Rs 1000.00 on 02-Jun-26 from BALANARASIMHAIA. UPI:536987786897-ICICI Bank."),
        Sms("2026-06-13T20:43:56+05:30", "AX-ICICIT-S", "ICICI Bank Acct XX317 debited for Rs 520.46 on 13-Jun-26; D MART credited. UPI:307970457040. Call 18002662 for dispute. SMS BLOCK 317 to 9215676766."),
        Sms("2026-06-29T05:09:11+05:30", "AX-FEDBNK-S", "Dear Customer, Rs.414 credited as interest to your A/c XX6170 on 29JUN2026 04:19:29.BAL-Rs.55943.25.Use FedMobile App to view your A/c statement-Federal Bank"),

        // -- credit card: bill payment debit, settlement, statements, declined --
        Sms("2026-06-01T21:04:08+05:30", "JK-ICICIT-S", "ICICI Bank Acc XX317 debited Rs. 15,524.00 on 01-Jun-26 InfoBIL*INFT*FF15.Avl Bal Rs. 53,898.02.To dispute call 18002662 or SMS BLOCK 317 to 9215676766"),
        Sms("2026-06-02T06:01:04+05:30", "AD-ICICIT-S", "Dear Customer, Payment of INR 15,524.00 has been received on your ICICI Bank Credit Card Account 4xxx8010 on 01-JUN-26.Thank you."),
        Sms("2026-06-04T16:42:33+05:30", "AD-ICICIT-S", "ICICI Bank Credit Card XX8010 Statement is sent to ni*********hi@gmail.com. Total of Rs 15,524.00 or minimum of Rs 780.00 is due by 15-JUN-26."),
        Sms("2026-06-29T18:07:58+05:30", "VK-CREDIN-S", "Your credit card bill for ICICI Bank XXXX-8010 has been generated. Total amount: INR 41,128.00 Due date: July 16, 2026 Tap on the link to pay: https://link.cred.club/CREDIN/link/G8My__30 and avoid late payment fees. - CRED"),
        Sms("2026-06-06T17:20:20+05:30", "AD-ICICIT-S", "Transaction of INR 5,000.00 at SHARK FITNESS S on ICICI Bank Credit Card XX8010 was declined as transaction amount exceeds Per Transaction Limit. Reset Per Transaction Limit at icici.co/ICICIT/k/DUvfEIZIyPZ"),

        // -- credit card: spends, Paytm duplicate, UPI-on-card, refunds --
        Sms("2026-06-06T17:21:31+05:30", "AD-ICICIT-S", "Rs 5,000.00 spent on ICICI Bank Card XX8010 on 06-Jun-26 at SHARK FITNESS S. Avl Lmt: Rs 1,13,148.00. To dispute, call 18002662/SMS BLOCK 8010 to 9215676766. To convert this txn to EMI give a missed call on 9924667667. Know more about EMI conversion at https://icici.co/ICICIT/ar/y4BBZg"),
        Sms("2026-06-06T17:21:33+05:30", "AD-PYTMPS-S", "Paid Rs. 5000 to SHARK FITNESS STUDIO on 06-Jun-26 using ICICI Bank Credit Card. To view receipt, visit https://paytm.me/PYTMPS/CL5zE6P - Paytm Payments"),
        Sms("2026-06-06T19:00:06+05:30", "JD-ICICIT-S", "INR 2,216.00 spent using ICICI Bank Card XX8010 on 06-Jun-26 on TECHNO SPORTSWE. Avl Limit: INR 1,10,932.00. If not you, call 1800 2662/SMS BLOCK 8010 to 9215676766."),
        Sms("2026-06-11T13:21:13+05:30", "AD-ICICIT-S", "ICICI Bank Credit Card XX1003 debited for INR 250.00 on 11-Jun-26 for UPI-135909178578-AMAR SER. To dispute call 18001080/SMS BLOCK 1003 to 9215676766"),
        Sms("2026-06-18T20:38:43+05:30", "AX-ICICIT-S", "ASSPL refund of Rs 650.95 credited to your ICICI Bank Credit Card XX0006 on 18-JUN-26."),

        // -- self transfers: ICICI debit + nameless Federal credit (seconds apart) --
        Sms("2026-06-01T21:20:01+05:30", "AD-ICICIT-S", "ICICI Bank Acct XX317 debited for Rs 10100.00 on 01-Jun-26; DEVATHI N NITHI credited. UPI:651817989969. Call 18002662 for dispute. SMS BLOCK 317 to 9215676766."),
        Sms("2026-06-01T21:20:12+05:30", "AX-FEDBNK-S", "Dear Customer, Rs.10100 credited to your A/c XX6170 on 01JUN2026 21:20:00. BAL-Rs.51025.25-Federal Bank"),
        // -- self transfer: Federal debit + ICICI credit-from-self, same ref both legs --
        Sms("2026-06-12T08:51:48+05:30", "BT-FEDBNK-S", "Rs 25.00 sent via UPI on 12-06-2026 at 08:51:48 to DEVATHI N NITHI.Ref:616319308338.Not you? Call 18004251199/SMS BLOCKUPI to 98950 88888 -Federal Bank"),
        Sms("2026-06-12T08:51:51+05:30", "AD-ICICIT-S", "Dear Customer, Acct XX317 is credited with Rs 25.00 on 12-Jun-26 from DEVATHI N NITHI. UPI:616319308338-ICICI Bank."),
        // -- self transfer with an SMS delivery gap of ~3 hours --
        Sms("2026-06-29T08:28:08+05:30", "JX-ICICIT-S", "ICICI Bank Acct XX317 debited for Rs 2223.00 on 29-Jun-26; DEVATHI N NITHI credited. UPI:618021239968. Call 18002662 for dispute. SMS BLOCK 317 to 9215676766."),
        Sms("2026-06-29T11:31:09+05:30", "AX-FEDBNK-S", "Dear Customer, Rs.2223 credited to your A/c XX6170 on 29JUN2026 08:28:04. BAL-Rs.58166.25-Federal Bank"),

        // -- income followed by same-amount transfer to self (GANESH must stay income) --
        Sms("2026-06-26T21:10:29+05:30", "JD-ICICIT-S", "Dear Customer, Acct XX317 is credited with Rs 8517.00 on 26-Jun-26 from GANESH D BHAT. UPI:125354327848-ICICI Bank."),
        Sms("2026-06-26T21:14:31+05:30", "JX-ICICIT-S", "ICICI Bank Acct XX317 debited for Rs 8517.00 on 26-Jun-26; DEVATHI N NIKHI credited. UPI:597354304685. Call 18002662 for dispute. SMS BLOCK 317 to 9215676766."),
        // -- same-account coincidence (TEJA income + FUTUREOL expense, 5s apart) --
        Sms("2026-06-24T20:57:14+05:30", "JX-ICICIT-S", "Dear Customer, Acct XX317 is credited with Rs 2637.00 on 24-Jun-26 from TEJA S N. UPI:617519250049-ICICI Bank."),
        Sms("2026-06-24T20:57:19+05:30", "JD-ICICIT-S", "ICICI Bank Acct XX317 debited for Rs 2637.00 on 24-Jun-26; FUTUREOL PRIVAT credited. UPI:068680342221. Call 18002662 for dispute. SMS BLOCK 317 to 9215676766."),

        // -- RD auto-debit and forex remittance --
        Sms("2026-06-06T07:03:43+05:30", "AX-ICICIT-S", "ICICI Bank Acc XX317 debited Rs. 6,000.00 on 05-Jun-26 InfoTo RD Ac no 7.Avl Bal Rs. 9,680.34.To dispute call 18002662 or SMS BLOCK 317 to 9215676766"),
        Sms("2026-06-03T08:58:08+05:30", "AX-ICICIT-S", "ICICI Bank Acc XX317 debited Rs. 19,999.68 on 03-Jun-26 InfoNRS*USD206.72.Avl Bal Rs. 35,124.34.To dispute call 18002662 or SMS BLOCK 317 to 9215676766"),

        // -- July format: heavily truncated self name, second leg never arrives --
        Sms("2026-07-06T12:55:10+05:30", "AX-FEDBNK-S", "Debited Rs 2000.00 from a/c X6170 on 06Jul26 12:55 via UPI to DEVATHI N NI. Ref 618758297125.Bal Rs 44157.25. Not you?Call 18004251199 -Federal Bank")
    )

    private val accounts = listOf(
        BankAccount(id = 1L, name = "ICICI Bank 317", balance = 60_000.0),
        BankAccount(id = 2L, name = "Federal Bank 6170", balance = 40_000.0),
        BankAccount(id = 3L, name = "ICICI Card 8010", balance = 0.0, type = AccountType.CreditCard),
        BankAccount(id = 4L, name = "ICICI Card 0006", balance = 0.0, type = AccountType.CreditCard),
        BankAccount(id = 5L, name = "ICICI Card 1003", balance = 0.0, type = AccountType.CreditCard)
    )

    @Test
    fun juneReplayProducesExpectedLedger() {
        TransactionMessageParser.selfName = "Devathi N Nithin"
        try {
            val parsed = messages.mapNotNull { sms ->
                TransactionMessageParser.parse(
                    message = sms.body,
                    transactionTimestampMillis = OffsetDateTime.parse(sms.at).toInstant().toEpochMilli(),
                    sender = sms.sender
                )
            }
            val filtered = SmsTransactionNormalizer.filterImportBatch(parsed)
            val planned = SmsImportPlanner.plan(filtered, accounts)

            val report = StringBuilder()
            report.appendLine("input SMSes: ${messages.size}")
            report.appendLine("parsed (survived artifact filters): ${parsed.size}")
            report.appendLine("after batch normalization: ${filtered.size}")
            report.appendLine("planned items: ${planned.size}")
            report.appendLine()

            val transfers = planned.filterIsInstance<PlannedSmsImport.Transfer>()
            val reviews = planned.filterIsInstance<PlannedSmsImport.TransferReview>()
            val rows = planned.filterIsInstance<PlannedSmsImport.Message>().map { it.message }
            val accountName = { id: Long? -> accounts.firstOrNull { it.id == id }?.name ?: "?" }

            report.appendLine("== AUTO TRANSFERS (${transfers.size}) ==")
            transfers.forEach { t ->
                report.appendLine(
                    "  Rs %.2f  %s -> %s  [%s, legs=%d]".format(
                        t.debit.amount, accountName(t.fromAccountId), accountName(t.toAccountId),
                        t.source, t.rawMessages.size
                    )
                )
            }
            report.appendLine("== TRANSFER REVIEWS (${reviews.size}) ==")
            reviews.forEach { r ->
                report.appendLine(
                    "  Rs %.2f  from=%s to=%s  [%s]  %s".format(
                        r.debit.amount, accountName(r.fromAccountId), accountName(r.toAccountId),
                        r.source, r.debit.counterparty
                    )
                )
            }
            report.appendLine("== LEDGER ROWS (${rows.size}) ==")
            rows.sortedBy { it.transactionTimestampMillis }.forEach { m ->
                val tag = when {
                    m.isCreditCardTransaction -> "CC-" + m.type.name.uppercase()
                    else -> m.type.name.uppercase()
                }
                val review = if (m.requiresUserReview) " (review)" else ""
                report.appendLine("  %-10s Rs %10.2f  %s%s".format(tag, m.amount, m.counterparty, review))
            }
            File(REPORT_PATH).writeText(report.toString())

            // --- credit card behavior ---
            val ccSpends = rows.filter { it.isCreditCardTransaction && it.type == TransactionType.Expense }
            val ccRefunds = rows.filter { it.isCreditCardTransaction && it.type == TransactionType.Income }
            assertEquals(3, ccSpends.size) // Shark Fitness, Techno Sportswear, AMAR SER (UPI-on-card)
            assertEquals(listOf(650.95), ccRefunds.map { it.amount })
            // Paytm duplicate, declined txn, settlement, both statements never became rows
            assertTrue(rows.none { "SHARK FITNESS STUDIO" in it.counterparty.uppercase() })
            assertEquals(1, rows.count { "SHARK FITNESS" in it.counterparty.uppercase() })
            // InfoBIL bill payment routed bank->card, not an expense
            val billPayment = (transfers + reviews).first { it.rawMessages.any { raw -> "InfoBIL" in raw } }
            assertTrue(rows.none { "InfoBIL" in (it.rawMessage) })

            // --- bank transfer behavior ---
            // 10100 pair, 25 pair, 2223 gap pair auto-resolved; 8517 + 2000 single legs auto-complete
            assertEquals(
                listOf(25.0, 2000.0, 2223.0, 8517.0, 10100.0),
                transfers.filter { it.source == SmsTransferSource.BankTransfer }.map { it.debit.amount }.sorted()
            )
            val tenK = transfers.first { it.debit.amount == 10_100.0 }
            assertEquals(1L, tenK.fromAccountId) // ICICI -> Federal
            assertEquals(2L, tenK.toAccountId)
            assertEquals(2, tenK.rawMessages.size)
            val singleLeg = transfers.first { it.debit.amount == 2_000.0 }
            assertEquals(2L, singleLeg.fromAccountId) // Federal -> ICICI, second SMS missing
            assertEquals(1L, singleLeg.toAccountId)
            assertEquals(1, singleLeg.rawMessages.size)
            // external money is never swallowed by transfer pairing
            assertTrue(rows.any { it.counterparty == "GANESH D BHAT" && it.type == TransactionType.Income })
            assertTrue(rows.any { it.counterparty == "TEJA S N" && it.type == TransactionType.Income })
            assertTrue(rows.any { it.counterparty == "FUTUREOL PRIVAT" && it.type == TransactionType.Expense })
            // RD auto-debit is a plain categorizable expense now (not a transfer); forex needs review
            assertTrue(rows.any { it.counterparty == "RD/FD Deposit" && it.type == TransactionType.Expense })
            assertTrue(rows.any { it.counterparty == "Foreign Remittance" && it.requiresUserReview })
            // ordinary income/expense survives untouched
            assertTrue(rows.any { "HEMALATHA PONN" in it.counterparty && it.type == TransactionType.Expense })
            assertTrue(rows.any { it.amount == 64_629.0 && it.type == TransactionType.Income })
            // bank interest stays income, never a transfer leg
            assertTrue(rows.any { it.amount == 414.0 && it.type == TransactionType.Income })
            assertEquals(1, reviews.size) // cc bill payment (card ambiguous) only; RD is now a normal expense row
        } finally {
            TransactionMessageParser.selfName = null
        }
    }

    companion object {
        const val REPORT_PATH =
            "C:\\Users\\Nithin\\AppData\\Local\\Temp\\claude\\C--Users-Nithin-Documents-New-project-money-manager\\e0105f15-4726-41e7-a039-f0cee3141a9c\\scratchpad\\june_replay_report.txt"

        @JvmStatic
        @AfterClass
        fun done() = Unit
    }
}
