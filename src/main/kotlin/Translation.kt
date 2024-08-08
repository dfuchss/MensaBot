import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import okhttp3.Credentials
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class TranslationService(private val config: TranslationConfig?) {
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
            translated = chatModel.generate(config.prompt.replaceFirst("{}", config.model).replaceFirst("{}", text))
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

    private fun createChatModel(config: TranslationConfig): ChatLanguageModel {
        val ollama = OllamaChatModel.builder().baseUrl(config.ollamaServerUrl).modelName(config.model).timeout(Duration.ofMinutes(5))
        if (config.ollamaUser != null && config.ollamaPassword != null) {
            ollama.customHeaders(mapOf("Authorization" to Credentials.basic(config.ollamaUser, config.ollamaPassword)))
        }
        return ollama.build()
    }
}

data class TranslationConfig(
    val ollamaServerUrl: String,
    val ollamaUser: String? = null,
    val ollamaPassword: String? = null,
    val model: String = "llama3.1:8b",
    val prompt: String = "Translate to English. No further output. Keep markdown. Add hint about the translation using {} at the end.\n{}"
)
