package org.fuchss.matrix.mensa

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * This is the configuration template of the mensa bot.
 * @param[prefix] the command prefix the bot listens to. By default, "mensa"
 * @param[baseUrl] the base url of the matrix server the bot shall use
 * @param[username] the username of the bot's account
 * @param[password] the password of the bot's account
 * @param[dataDirectory] the path to the databases and media folder
 * @param[timeToSendUpdates] the time the bot shall send updates about the meals every day (to subscribed rooms)
 * @param[admins] the matrix ids of the admins. E.g. "@user:invalid.domain"
 * @param[subscribers] the room ids of rooms that subscribed updates
 */
data class Config(
    @JsonProperty val prefix: String = "mensa",
    @JsonProperty val baseUrl: String,
    @JsonProperty val username: String,
    @JsonProperty val password: String,
    @JsonProperty val dataDirectory: String,
    @JsonProperty val timeToSendUpdates: LocalTime,
    @JsonProperty val admins: List<String>,
    @JsonProperty val subscribers: List<String>
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(Config::class.java)

        /**
         * Load the config from the file path. You can set "CONFIG_PATH" in the environment to override the default location ("./config.json").
         */
        fun load(): Config {
            val configPath = System.getenv("CONFIG_PATH") ?: "./config.json"
            val configFile = File(configPath)
            if (!configFile.exists()) {
                error("Config ${configFile.absolutePath} does not exist!")
            }

            val config: Config = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule()).readValue(configFile)
            log.info("Loaded config ${configFile.absolutePath}")
            return config
        }
    }

    /**
     * Determine whether a user id belongs to an admin.
     * @param[user] the user id to check
     * @return indicator for admin privileges
     */
    fun isAdmin(user: UserId?): Boolean {
        if (user == null) {
            return false
        }
        if (admins.isEmpty()) {
            return true
        }
        return user.full in admins
    }

    /**
     * Get all subscriptions (rooms that want daily meal updates) as [RoomId].
     */
    fun subscriptions() = subscribers.map { RoomId(it) }

    /**
     * Get the next time of the next reminder (notification to [subscriptions]).
     */
    fun nextReminder(): Date {
        var nextUpdate = timeToSendUpdates.toJavaLocalTime().atDate(LocalDate.now()).atZone(ZoneId.systemDefault())
        if (!nextUpdate.toInstant().isAfter(Instant.now())) {
            // Else use tomorrow ..
            nextUpdate = nextUpdate.plusDays(1)
        }
        return Date.from(nextUpdate.toInstant())
    }
}
