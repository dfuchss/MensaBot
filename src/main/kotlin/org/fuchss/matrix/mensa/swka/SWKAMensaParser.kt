package org.fuchss.matrix.mensa.swka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.fuchss.matrix.mensa.api.Mensa
import org.fuchss.matrix.mensa.api.MensaLine
import org.fuchss.matrix.mensa.swka.dto.MealRaw
import org.fuchss.matrix.mensa.swka.dto.MensaRaw

internal class SWKAMensaParser {
    private val orm = ObjectMapper().registerKotlinModule()
    private val validMensas: List<MensaRaw> = orm.readValue(SWKAMensaParser::class.java.getResourceAsStream("/valid_mensa.json")!!)

    fun parseMensa(mensaInfos: JsonNode): List<Mensa> {
        val mensaList: MutableList<Mensa> = mutableListOf()
        for (validMensa in validMensas) {
            if (!mensaInfos.has(validMensa.id)) {
                continue
            }

            val mensa = Mensa(validMensa.id, validMensa.name)
            val dateXLine = mensaInfos.get(mensa.id).jsonToObject<Map<Int, Map<String, List<MealRaw>>>>()
            for ((date, lineData) in dateXLine) {
                parseMensaLines(mensa, date, validMensa.lineNames, lineData)
            }

            if (mensa.mensaLines.isNotEmpty()) {
                mensaList.add(mensa)
            }
        }
        return mensaList
    }

    private fun parseMensaLines(mensa: Mensa, epochSeconds: Int, lineNames: Map<String, String>, lineData: Map<String, List<MealRaw>>) {
        val mensaLines = mutableListOf<MensaLine>()
        for ((lineId, lineName) in lineNames) {
            if (lineId !in lineData.keys) {
                continue
            }
            val rawLine = lineData[lineId]!!
            val meal = rawLine.filterNot { it.noMeal != null && it.noMeal }.map { MealRaw.toMeal(it) }
            if (meal.isNotEmpty()) {
                mensaLines.add(MensaLine(lineName, meal))
            }
        }

        if (mensaLines.isEmpty()) {
            return
        }

        val localDate = Instant.fromEpochSeconds(epochSeconds.toLong()).toLocalDateTime(TimeZone.of("Europe/Berlin")).date
        mensa.mensaLines.getOrPut(localDate) { mutableListOf() } += mensaLines
    }

    private inline fun <reified T> JsonNode.jsonToObject(): T {
        return orm.readValue(this.toString())
    }
}
