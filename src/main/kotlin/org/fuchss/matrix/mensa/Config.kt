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

data class Config(
    @JsonProperty val prefix: String = "mensa",
    @JsonProperty val baseUrl: String,
    @JsonProperty val username: String,
    @JsonProperty val password: String,
    @JsonProperty val timeToSendUpdates: LocalTime,
    @JsonProperty val admins: List<String>,
    @JsonProperty val subscribers: List<String>
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(Config::class.java)
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

    fun isAdmin(user: UserId?): Boolean {
        if (user == null) {
            return false
        }
        if (admins.isEmpty()) {
            return true
        }
        return user.full in admins
    }

    fun subscriptions() = subscribers.map { RoomId(it) }
    fun nextTimer(): Date {
        val today = timeToSendUpdates.toJavaLocalTime().atDate(LocalDate.now()).atZone(ZoneId.systemDefault())
        if (today.toInstant().isAfter(Instant.now())) {
            return Date.from(today.toInstant())
        }
        // Else use tomorrow ..
        return Date.from(timeToSendUpdates.toJavaLocalTime().atDate(LocalDate.now().plusDays(1)).atZone(ZoneId.systemDefault()).toInstant())
    }
}
