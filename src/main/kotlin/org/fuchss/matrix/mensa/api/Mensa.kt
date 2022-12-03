package org.fuchss.matrix.mensa.api

import kotlinx.datetime.LocalDate

data class Mensa(
    val id: String,
    val name: String,
    val mensaLines: MutableMap<LocalDate, MutableList<MensaLine>> = mutableMapOf()
)
