package org.fuchss.matrix.mensa.data

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class Mensa(
    val id: String, val name: String, val mensaLines: MutableMap<LocalDate, MutableList<MensaLine>> = mutableMapOf()
) {
    companion object {
        const val TZ = "Europe/Berlin"
    }

    fun with(date: LocalDate, food: List<MensaLine>) = Mensa(id, name, mutableMapOf(date to food.toMutableList()))
    fun mensaLinesAtDate(date: LocalDate = Clock.System.todayIn(TimeZone.of(TZ))): List<MensaLine>? = mensaLines[date]
}