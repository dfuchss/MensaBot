package org.fuchss.matrix.mensa.request

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.jackson
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.fuchss.matrix.mensa.data.Mensa
import org.fuchss.matrix.mensa.data.Mensa.Companion.TZ

class MensaAPI {
    private lateinit var mensa: List<Mensa>

    suspend fun foodAtDate(date: LocalDate = Clock.System.todayIn(TimeZone.of(TZ))): List<Mensa> {
        if (!this::mensa.isInitialized) mensa = request()
        return mensa.map { m -> m to (m.mensaLines[date]?.toList() ?: listOf()) }.filter { (_, lines) -> lines.isNotEmpty() }.map { (m, f) -> m.with(date, f) }
    }

    private suspend fun request(): List<Mensa> {
        val client = HttpClient() {
            install(ContentNegotiation) {
                jackson()
            }
        }
        val response = client.request("https://www.sw-ka.de/json_interface/canteen") {
            method = HttpMethod.Get
        }

        return MensaParser().parseMensa(response.body())
    }


}