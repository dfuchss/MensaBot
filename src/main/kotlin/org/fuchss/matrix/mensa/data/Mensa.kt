package org.fuchss.matrix.mensa.data

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class Mensa(
    val id: String,
    val name: String,
    val mensaLines: MutableMap<LocalDate, MutableList<MensaLine>> = mutableMapOf()
) {
    fun mensaOnlyWithLines(date: LocalDate, mensaLines: List<MensaLine>) = Mensa(id, name, mutableMapOf(date to mensaLines.toMutableList()))
    fun mensaLinesAtDate(date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): List<MensaLine>? = mensaLines[date]
}
