package org.fuchss.matrix.mensa.api

/**
 * This class defines a mensa line that serves food.
 * @param[name] the name of the line
 * @param[meals] the food the line serves
 */
data class MensaLine(val name: String, val meals: List<Meal>)
