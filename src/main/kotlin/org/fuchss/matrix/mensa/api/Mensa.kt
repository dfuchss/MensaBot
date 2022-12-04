package org.fuchss.matrix.mensa.api

import kotlinx.datetime.LocalDate

/**
 * This class represents a mensa with food.
 * @param[id] the unique id of the mensa
 * @param[name] a nice readable name of the mensa
 * @param[mensaLines] the mensa lines mapped by the date
 */
data class Mensa(
    val id: String,
    val name: String,
    val mensaLines: Map<LocalDate, List<MensaLine>>
)
