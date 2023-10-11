package org.fuchss.matrix.mensa.swka

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import kotlinx.datetime.LocalDate
import org.fuchss.matrix.mensa.api.Canteen
import org.fuchss.matrix.mensa.api.CanteenAPI
import org.fuchss.matrix.mensa.api.CanteenLine
import org.fuchss.matrix.mensa.api.Meal
import org.fuchss.matrix.mensa.numberOfWeek
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory

class SWKAMensa : CanteenAPI {
    companion object {
        private val logger = LoggerFactory.getLogger(SWKAMensa::class.java)
        private const val SWKA_WEBSITE = //
            "https://www.sw-ka.de/de/hochschulgastronomie/speiseplan/mensa_adenauerring/?view=ok&c=adenauerring&STYLE=popup_plain&kw=%%%WoY%%%"
        private val LINES_TO_CONSIDER = listOf("Linie ", "Schnitzel", "[pizza]werk Pizza", "[pizza]werk Pasta", "[kœri]werk")
    }

    override suspend fun foodAtDate(date: LocalDate): Map<Canteen, List<CanteenLine>> {
        val week = numberOfWeek(date)
        val html = request(week)

        val document = Jsoup.parse(html)
        val tableOfDay = document.select("h1:contains(${date.dayOfMonth.pad()}.${date.monthNumber.pad()}) + table")
        if (tableOfDay.isEmpty()) {
            return emptyMap()
        }
        if (tableOfDay.size != 1) {
            logger.error("Found more than one table for ${date.dayOfMonth.pad()}.${date.monthNumber.pad()}")
            return emptyMap()
        }

        val mensaLinesRaw = tableOfDay[0].select("td[width=20%] + td")
        val mensaLines = mutableListOf<CanteenLine>()

        for (line in mensaLinesRaw) {
            val name = line.previousElementSibling()?.text()?.trim() ?: continue
            if (LINES_TO_CONSIDER.none { name.startsWith(it) }) {
                continue
            }

            val meals = mutableListOf<Meal>()
            for (meal in line.select("tr")) {
                parseMeal(meal)?.let { meals.add(it) }
            }
            if (meals.isNotEmpty() && !closed(meals)) {
                mensaLines.add(CanteenLine(name, meals))
            }
        }

        mensaLines.sortBy { it.name }

        val mensa = Canteen("adenauerring", "Mensa am Adenauerring")
        return mapOf(mensa to mensaLines)
    }

    private fun closed(meals: List<Meal>): Boolean {
        return meals.size == 1 && meals[0].name.lowercase() == "geschlossen"
    }

    private fun parseMeal(meal: Element): Meal? {
        val nameXPrice = meal.select("span.bg")
        if (nameXPrice.size != 2) {
            // Line is probably closed
            if (logger.isDebugEnabled) logger.debug("Unknown data in $meal")
            return null
        }
        // <span class="bg"> <b>Gebratene Hähnchenkeule Peperonata</b> <span>in Paprikasoße Curryreis</span> (1,Se,We)</span>
        val mealName = nameXPrice[0].text().split("(")[0].trim()
        // <span class="bg">3,20 €</span>
        val priceStudent = nameXPrice[1].text().split(" ")[0].replace(',', '.').trim().toDoubleOrNull() ?: Double.NaN
        // <td style="background: white;width:115px;" valign="top">[VG]</td>
        val additionalInformation = parseAdditionalInformation(meal.select("td")[0].text().trim())

        return Meal(
            name = mealName,
            foodAdditiveNumbers = emptyList(),
            priceStudent = priceStudent,
            priceGuest = Double.NaN,
            priceEmployee = Double.NaN,
            pricePupil = Double.NaN,
            fish = additionalInformation.contains("MSC"),
            pork = additionalInformation.contains("S") || additionalInformation.contains("SAT"),
            cow = additionalInformation.contains("R") || additionalInformation.contains("RAT"),
            vegan = additionalInformation.contains("VG"),
            vegetarian = additionalInformation.contains("VEG")
        )
    }

    private fun parseAdditionalInformation(additionalInformation: String): List<String> {
        if (additionalInformation.isBlank()) return emptyList()

        // E.g., "[VG,MV]"
        if (!additionalInformation.startsWith("[") || !additionalInformation.endsWith("]")) {
            logger.warn("Found invalid additional information format: $additionalInformation")
            return emptyList()
        }
        return additionalInformation.replace("[", "").replace("]", "").split(",").filter { it.isNotBlank() }
    }

    private suspend fun request(weekOfYear: Int): String {
        val client = HttpClient()
        val response =
            client.request(SWKA_WEBSITE.replace("%%%WoY%%%", weekOfYear.toString())) {
                method = HttpMethod.Get
            }
        return response.body()
    }
}

private fun Int.pad(): String = if (this < 10) "0$this" else this.toString()
