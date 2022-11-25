package org.fuchss.matrix.mensa

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.folivo.trixnity.core.model.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

data class Config(
    @JsonProperty val baseUrl: String,
    @JsonProperty val username: String,
    @JsonProperty val password: String,
    @JsonProperty val admins: List<String>
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(Config::class.java)
        fun load(): Config {
            val configPath = System.getenv("CONFIG_PATH") ?: "./config.json"
            val configFile = File(configPath)
            if (!configFile.exists())
                error("Config ${configFile.absolutePath} does not exist!")

            val config: Config = ObjectMapper().registerKotlinModule().readValue(configFile)
            log.info("Loaded config ${configFile.absolutePath}")
            return config
        }
    }

    fun isAdmin(user: UserId?): Boolean {
        if (user == null)
            return false
        if (admins.isEmpty())
            return true
        return user.full in admins
    }


}