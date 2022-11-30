package org.fuchss.matrix.mensa.api

data class Meal(
    val name: String,
    val foodAdditiveNumbers: List<String>,
    val priceStudent: Double,
    val priceGuest: Double,
    val priceEmployee: Double,
    val pricePupil: Double,
    val priceAdditive: Double
)
