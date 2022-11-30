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

internal class SWKAMensaAPI : MensaAPI {
    private val mensaParser = SWKAMensaParser()
    private lateinit var mensa: List<Mensa>

    override suspend fun foodAtDate(date: LocalDate): List<Mensa> {
        if (!this::mensa.isInitialized) {
            mensa = request()
        }
        return mensa.map { m -> m to (m.mensaLines[date]?.toList() ?: listOf()) }.filter { (_, lines) -> lines.isNotEmpty() }.map { (m, f) -> m.mensaOnlyWithLines(date, f) }
    }

    override suspend fun reload() {
        mensa = request()
    }

    private suspend fun request(): List<Mensa> {
        val client = HttpClient() { install(ContentNegotiation) { jackson() } }
        val response = client.request("https://www.sw-ka.de/json_interface/canteen") {
            method = HttpMethod.Get
        }
        return mensaParser.parseMensa(response.body())
    }
}
