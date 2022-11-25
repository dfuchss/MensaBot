package org.fuchss.matrix.mensa.data

import org.fuchss.matrix.mensa.request.dto.MealRaw

data class Meal(
    val name: String, val foodAdditiveNumbers: List<String>, val priceStudent: Double, val priceGuest: Double, val priceEmployee: Double, val pricePupil: Double, val priceAdditive: Double
) {
    companion object {
        fun fromMealRawData(mealRawData: MealRaw): Meal = Meal(
            mealRawData.meal(),
            mealRawData.foodAdditiveNumbers!!,
            mealRawData.priceStudent!!,
            mealRawData.priceGuest!!,
            mealRawData.priceEmployee!!,
            mealRawData.pricePupil!!,
            mealRawData.priceAdditive!!
        )
    }
}

