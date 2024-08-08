import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import okhttp3.Credentials
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class TranslationService(private val config: TranslationConfig?) {
    private val logger: Logger = LoggerFactory.getLogger(TranslationService::class.java)

    private val chatModel = config?.let { createChatModel(it) }

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

        try {
            return chatModel.generate(config.prompt.replaceFirst("{}", config.model).replaceFirst("{}", text))
        } catch (e: Exception) {
            logger.error("Error while translating text: $text", e)
            return text
        }
    }

    private fun createChatModel(config: TranslationConfig): ChatLanguageModel {
        val ollama = OllamaChatModel.builder().baseUrl(config.ollamaServerUrl).modelName(config.model).timeout(Duration.ofMinutes(5)).temperature(0.0)
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
    val prompt: String = "Translate to English. No further output. Keep markdown. Add hint about the translation by {} at the end.\n{}"
)
