package org.fuchss.matrix.mensa.request.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MealRaw(
    @JsonProperty("nodata") val noMeal: Boolean?,
    @JsonProperty("meal") val name: String?,
    @JsonProperty("dish") val dish: String?,
    @JsonProperty("add") val foodAdditiveNumbers: List<String>?,
    @JsonProperty("price_1") val priceStudent: Double?,
    @JsonProperty("price_2") val priceGuest: Double?,
    @JsonProperty("price_3") val priceEmployee: Double?,
    @JsonProperty("price_4") val pricePupil: Double?,
    @JsonProperty("price_flag") val priceAdditive: Double?
) {
    fun meal(): String {
        if (dish.isNullOrBlank()) {
            return name ?: ""
        }
        return ((name ?: "") + " $dish").trim()
    }
}
