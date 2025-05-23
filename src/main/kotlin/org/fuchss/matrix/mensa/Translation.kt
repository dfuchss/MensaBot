package org.fuchss.matrix.mensa

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.ollama.OllamaChatModel
import okio.ByteString.Companion.encode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class TranslationService(
    private val config: TranslationConfig?
) {
    private val logger: Logger = LoggerFactory.getLogger(TranslationService::class.java)

    private val chatModel = config?.let { createChatModel(it) }

    private val translationCache = mutableMapOf<String, String>()
    private val translationCacheSize = 5

    @Synchronized
    fun translate(text: String): String {
        if (config == null) {
            logger.debug("No translation config found. Skipping translation.")
            return text
        }

        if (chatModel == null) {
            logger.error("ChatModel is null. Skipping translation.")
            return text
        }

        var translated = translationCache[text]
        if (translated != null) {
            return translated
        }

        try {
            translated = chatModel.chat(config.prompt.replaceFirst("{}", config.model).replaceFirst("{}", text))
        } catch (e: Exception) {
            logger.error("Error while translating text: $text", e)
            return text
        }

        if (translationCache.size >= translationCacheSize) {
            translationCache.remove(translationCache.keys.first())
        }
        translationCache[text] = translated

        return translated
    }

    private fun createChatModel(config: TranslationConfig): ChatModel {
        val ollama =
            OllamaChatModel
                .builder()
                .baseUrl(config.ollamaServerUrl)
                .modelName(config.model)
                .timeout(Duration.ofMinutes(15))
                .temperature(0.0)
        if (config.ollamaUser != null && config.ollamaPassword != null) {
            ollama.customHeaders(mapOf("Authorization" to "${config.ollamaUser}:${config.ollamaPassword}".encode().base64()))
        }
        return ollama.build()
    }
}

data class TranslationConfig(
    val ollamaServerUrl: String,
    val ollamaUser: String? = null,
    val ollamaPassword: String? = null,
    val model: String = "llama3.1:8b",
    val prompt: String =
        "Translate this menu to English. " +
            "Keep markdown and emojis. Just start with the markdown, no further output. " +
            "In the end add 'Translated by {}.'\n{}"
)
