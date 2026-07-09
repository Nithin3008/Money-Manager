package com.moneymanager.app.data

import android.content.Context
import android.os.Process
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.moneymanager.app.model.TransactionType
import java.io.File
import java.util.Collections

class LiteRtLmTransactionInterpreter(
    context: Context,
    private val modelPath: String,
    private val selfName: String? = null
) : LocalLlmTransactionInterpreter, AutoCloseable {
    private val gson = Gson()
    private val lock = Any()
    private val engine: Any
    private val engineClass: Class<*>
    private val conversationClass: Class<*>

    init {
        val backendClass = Class.forName("com.google.ai.edge.litertlm.Backend")
        val cpuBackendClass = Class.forName("com.google.ai.edge.litertlm.Backend\$CPU")
        val engineConfigClass = Class.forName("com.google.ai.edge.litertlm.EngineConfig")
        engineClass = Class.forName("com.google.ai.edge.litertlm.Engine")
        conversationClass = Class.forName("com.google.ai.edge.litertlm.Conversation")

        val cpuBackend = cpuBackendClass
            .getConstructor(Integer::class.java)
            .newInstance(Integer.valueOf(1))
        val engineConfig = engineConfigClass
            .getConstructor(
                String::class.java,
                backendClass,
                backendClass,
                backendClass,
                Integer::class.java,
                Integer::class.java,
                String::class.java
            )
            .newInstance(
                modelPath,
                cpuBackend,
                null,
                null,
                null,
                null,
                File(context.cacheDir, "litert-lm").absolutePath
            )
        engine = engineClass.getConstructor(engineConfigClass).newInstance(engineConfig)
    }

    fun initialize() {
        engineClass.getMethod("initialize").invoke(engine)
    }

    override fun interpret(
        message: String,
        sender: String?,
        categories: List<LocalLlmCategoryOption>
    ): LocalLlmTransactionInterpretation? {
        return synchronized(lock) {
            runCatching {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                createConversation().use { conversation ->
                    val responseMessage = conversationClass
                        .getMethod("sendMessage", String::class.java, Map::class.java)
                        .invoke(conversation, buildPrompt(message, sender, categories), Collections.emptyMap<String, Any>())
                    val responseText = responseMessage?.toString() ?: return@use null
                    parseResponse(responseText)
                }
            }.getOrNull()
        }
    }

    override fun close() {
        engineClass.getMethod("close").invoke(engine)
    }

    private fun createConversation(): AutoCloseable {
        val conversationConfigClass = Class.forName("com.google.ai.edge.litertlm.ConversationConfig")
        val config = conversationConfigClass.getConstructor().newInstance()
        return engineClass
            .getMethod("createConversation", conversationConfigClass)
            .invoke(engine, config) as AutoCloseable
    }

    private fun buildPrompt(
        message: String,
        sender: String?,
        categories: List<LocalLlmCategoryOption>
    ): String {
        val categoryPrompt = if (categories.isEmpty()) {
            "No category list was provided. Use null for suggestedCategoryId."
        } else {
            categories.joinToString("\n") { "${it.id}: ${it.name}" }
        }
        val selfNameRule = selfName
            ?.takeIf { it.isNotBlank() }
            ?.let {
                "The user's own bank-registered name is \"$it\". UPI debits or credits whose counterparty is this name or a close spelling variant (truncation or one-letter typo) are the user's own money moving between accounts: set isInternalTransfer true."
            }
            .orEmpty()
        return """
            You parse Indian bank SMS alerts for a personal finance app.
            Return only one JSON object. Do not add markdown or explanation.
            Use null when a field is not present. Never invent an amount.
            Pick the transaction amount, not the available balance.
            Type must be exactly Income or Expense.
            Credit card refunds, reversals, chargebacks, and cashback are Income with isCreditCardTransaction true.
            Credit card purchases/spends are Expense with isCreditCardTransaction true.
            Credit card UPI messages like "Credit Card XX1234 debited ... for UPI-123-MERCHANT" are credit card spends; extract the merchant after the UPI reference.
            Credit card bill payments from a bank account are not card spend; mark isInternalTransfer true.
            For credit card purchases or refunds, choose the CC/Credit Card category id if it is in the allowed list.
            Internal transfers are own-account or self transfers between the user's bank accounts.
            $selfNameRule
            Payment-app receipts like "Paid Rs X to MERCHANT using ... Credit Card" duplicate the bank's own card alert; set confidence 0 for them.
            Auto-debits into the user's own RD or FD deposit ("Info To RD Ac") are internal transfers, not expenses.
            If categories are provided, suggest exactly one category id from the allowed list.
            Do not invent categories. If Indian shop or merchant name is unclear, choose Uncategorized if present.

            JSON schema:
            {
              "amount": number|null,
              "type": "Income"|"Expense"|null,
              "bankName": string|null,
              "accountHint": string|null,
              "counterparty": string|null,
              "isCreditCardTransaction": boolean|null,
              "isInternalTransfer": boolean|null,
              "suggestedCategoryId": number|null,
              "confidence": number
            }

            Allowed categories:
            $categoryPrompt

            Sender: ${sender.orEmpty()}
            SMS: $message
        """.trimIndent()
    }

    private fun parseResponse(response: String): LocalLlmTransactionInterpretation? {
        val json = response.substringAfter('{', missingDelimiterValue = "")
            .substringBeforeLast('}', missingDelimiterValue = "")
            .takeIf { it.isNotBlank() }
            ?.let { "{$it}" }
            ?: return null

        val parsed = try {
            gson.fromJson(json, LlmParserResponse::class.java)
        } catch (_: JsonSyntaxException) {
            return null
        } ?: return null

        return LocalLlmTransactionInterpretation(
            amount = parsed.amount?.takeIf { it > 0.0 },
            type = parsed.type.toTransactionType(),
            bankName = parsed.bankName?.cleanField(),
            accountHint = parsed.accountHint?.filter(Char::isDigit)?.takeLast(6),
            counterparty = parsed.counterparty?.cleanField(),
            isCreditCardTransaction = parsed.isCreditCardTransaction,
            isInternalTransfer = parsed.isInternalTransfer,
            suggestedCategoryId = parsed.suggestedCategoryId,
            confidence = parsed.confidence?.coerceIn(0.0, 1.0) ?: 0.0
        )
    }

    private fun String?.toTransactionType(): TransactionType? {
        return when (this?.trim()?.lowercase()) {
            "income", "credit", "credited" -> TransactionType.Income
            "expense", "debit", "debited" -> TransactionType.Expense
            "transfer", "internal transfer" -> TransactionType.Transfer
            else -> null
        }
    }

    private fun String.cleanField(): String? {
        return replace(Regex("""[\t\r\n]+"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(48)
            .takeIf { it.isNotBlank() }
    }

    private data class LlmParserResponse(
        val amount: Double? = null,
        val type: String? = null,
        val bankName: String? = null,
        val accountHint: String? = null,
        val counterparty: String? = null,
        val isCreditCardTransaction: Boolean? = null,
        val isInternalTransfer: Boolean? = null,
        val suggestedCategoryId: Long? = null,
        val confidence: Double? = null
    )
}
