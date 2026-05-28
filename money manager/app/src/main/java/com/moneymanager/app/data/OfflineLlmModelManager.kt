package com.moneymanager.app.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object OfflineLlmModelConfig {
    const val modelFileName: String = "mobile_actions_q8_ekv1024.litertlm"
    const val modelDisplayName: String = "FunctionGemma 270M Mobile Actions"
    const val modelPageUrl: String =
        "https://huggingface.co/litert-community/functiongemma-270m-ft-mobile-actions"
    const val downloadUrl: String =
        "$modelPageUrl/resolve/main/$modelFileName"
    const val minimumBytes: Long = 250L * 1024L * 1024L

    // Set after choosing a license-compliant hosted artifact with a stable checksum.
    val expectedSha256: String? = null
}

data class OfflineLlmModelDownloadResult(
    val file: File,
    val bytes: Long,
    val sha256: String
)

class OfflineLlmModelManager(context: Context) {
    private val appContext = context.applicationContext
    val modelDir: File = File(appContext.filesDir, "offline-llm")
    val modelFile: File = File(modelDir, OfflineLlmModelConfig.modelFileName)

    fun isModelReady(): Boolean {
        return modelFile.isFile && modelFile.length() >= OfflineLlmModelConfig.minimumBytes
    }

    fun deleteModel() {
        modelDir.deleteRecursively()
    }

    fun importModel(uri: Uri): OfflineLlmModelDownloadResult {
        modelDir.mkdirs()
        val partFile = File(modelDir, "${OfflineLlmModelConfig.modelFileName}.part")
        if (partFile.exists()) partFile.delete()

        val digest = MessageDigest.getInstance("SHA-256")
        var copiedBytes = 0L
        val resolver = appContext.contentResolver
        resolver.openInputStream(uri)?.use { input ->
            partFile.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    copiedBytes += read
                }
            }
        } ?: throw IllegalStateException("Could not open selected model file.")

        return finalizeModelFile(partFile, copiedBytes, digest)
    }

    fun downloadModel(): OfflineLlmModelDownloadResult {
        modelDir.mkdirs()
        val partFile = File(modelDir, "${OfflineLlmModelConfig.modelFileName}.part")
        if (partFile.exists()) partFile.delete()

        val connection = (URL(OfflineLlmModelConfig.downloadUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 60_000
            requestMethod = "GET"
        }

        try {
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN) {
                throw IllegalStateException(
                    "Model access needs license approval or a configured hosted model URL."
                )
            }
            if (status !in 200..299) {
                throw IllegalStateException("Model download failed with HTTP $status.")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var copiedBytes = 0L
            connection.inputStream.use { input ->
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        copiedBytes += read
                    }
                }
            }

            return finalizeModelFile(partFile, copiedBytes, digest)
        } finally {
            connection.disconnect()
        }
    }

    private fun finalizeModelFile(
        partFile: File,
        copiedBytes: Long,
        digest: MessageDigest
    ): OfflineLlmModelDownloadResult {
        if (copiedBytes < OfflineLlmModelConfig.minimumBytes) {
            partFile.delete()
            throw IllegalStateException("Selected file was too small to be the ${OfflineLlmModelConfig.modelDisplayName} model.")
        }

        val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
        OfflineLlmModelConfig.expectedSha256?.let { expected ->
            if (!sha256.equals(expected, ignoreCase = true)) {
                partFile.delete()
                throw IllegalStateException("Selected model checksum did not match.")
            }
        }

        if (modelFile.exists()) modelFile.delete()
        if (!partFile.renameTo(modelFile)) {
            partFile.copyTo(modelFile, overwrite = true)
            partFile.delete()
        }

        return OfflineLlmModelDownloadResult(
            file = modelFile,
            bytes = copiedBytes,
            sha256 = sha256
        )
    }
}
