package org.fuchss.matrix.mensa.swka.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.fuchss.matrix.mensa.api.Meal

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class MealRaw(
    @JsonProperty("nodata") val noMeal: Boolean?,
    @JsonProperty("meal") val name: String?,
    @JsonProperty("dish") val dish: String?,
    @JsonProperty("add") val foodAdditiveNumbers: List<String>?,
    @JsonProperty("price_1") val priceStudent: Double?,
    @JsonProperty("price_2") val priceGuest: Double?,
    @JsonProperty("price_3") val priceEmployee: Double?,
    @JsonProperty("price_4") val pricePupil: Double?,

    @JsonProperty("fish") val fish: Boolean?,
    @JsonProperty("pork") val pork: Boolean?,
    @JsonProperty("pork_aw") val porkAw: Boolean?, // Pork Animal Welfare
    @JsonProperty("cow") val cow: Boolean?,
    @JsonProperty("cow_aw") val cowAw: Boolean?, // Cow Animal Welfare
    @JsonProperty("vegan") val vegan: Boolean?,
    @JsonProperty("veg") val vegetarian: Boolean?
) {

    companion object {
        fun toMeal(mealRawData: MealRaw): Meal {
            if (mealRawData.noMeal != null && mealRawData.noMeal) {
                error("You cannot convert a \"no-meal\" meal. Constraints do not hold!")
            }

            return Meal(
                mealRawData.meal(),
                mealRawData.foodAdditiveNumbers!!,
                mealRawData.priceStudent!!,
                mealRawData.priceGuest!!,
                mealRawData.priceEmployee!!,
                mealRawData.pricePupil!!,
                mealRawData.fish!!,
                mealRawData.pork!! || mealRawData.porkAw!!,
                mealRawData.cow!! || mealRawData.cowAw!!,
                mealRawData.vegan!!,
                mealRawData.vegetarian!!
            )
        }
    }

    fun meal(): String {
        val realName = name ?: "Unknown Meal"
        val realDish = dish ?: ""

        return "$realName $realDish".trim()
    }
}
