package org.fuchss.matrix.mensa.swka

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.jackson
import kotlinx.datetime.LocalDate
import org.fuchss.matrix.mensa.api.Mensa
import org.fuchss.matrix.mensa.api.MensaAPI
import org.fuchss.matrix.mensa.api.MensaLine

/**
 * One implementation of [MensaAPI] for www.sw-ka.de .
 */
internal class SWKAMensaAPI : MensaAPI {
    private val mensaParser = SWKAMensaParser()
    private lateinit var mensa: List<Mensa>

    override suspend fun foodAtDate(date: LocalDate): Map<Mensa, List<MensaLine>> {
        if (!this::mensa.isInitialized) {
            mensa = request()
        }

        val result = mutableMapOf<Mensa, List<MensaLine>>()
        for (m in mensa) {
            val meals = m.mensaLines[date] ?: continue
            result[m] = meals
        }

        return result
    }

    override suspend fun reload() {
        mensa = request()
    }

    private suspend fun request(): List<Mensa> {
        val client = HttpClient { install(ContentNegotiation) { jackson() } }
        val response = client.request("https://www.sw-ka.de/json_interface/canteen") {
            method = HttpMethod.Get
        }
        return mensaParser.parseMensa(response.body())
    }
}
