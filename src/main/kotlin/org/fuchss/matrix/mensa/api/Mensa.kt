package org.fuchss.matrix.mensa.api

/**
 * This class represents a mensa with food.
 * @param[id] the unique id of the mensa
 * @param[name] a nice readable name of the mensa
 */
data class Mensa(
    val id: String,
    val name: String
)
