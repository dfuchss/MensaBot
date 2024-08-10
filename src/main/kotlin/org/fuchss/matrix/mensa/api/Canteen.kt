package org.fuchss.matrix.mensa.api

/**
 * This class represents a canteen with food.
 * @param[id] the unique id of the canteen
 * @param[name] a nice readable name of the canteen
 * @param[link] a link to the current canteen plan
 */
data class Canteen(
    val id: String,
    val name: String,
    val link: String? = null
)
