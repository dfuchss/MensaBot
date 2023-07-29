package org.fuchss.matrix.mensa.api

/**
 * This class defines a canteen line that serves food.
 * @param[name] the name of the line
 * @param[meals] the food the line serves
 */
data class CanteenLine(val name: String, val meals: List<Meal>)
