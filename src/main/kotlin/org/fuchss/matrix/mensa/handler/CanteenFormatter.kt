package org.fuchss.matrix.mensa.handler

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import net.folivo.trixnity.core.model.RoomId
import org.fuchss.matrix.bots.MatrixBot
import org.fuchss.matrix.bots.markdown
import org.fuchss.matrix.mensa.TranslationService
import org.fuchss.matrix.mensa.api.CanteensApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)

suspend fun sendCanteenEventToRoom(
    roomId: RoomId,
    matrixBot: MatrixBot,
    scheduled: Boolean,
    canteen: CanteensApi,
    translationService: TranslationService
) {
    logger.info("Sending Mensa to Room ${roomId.full}")

    val mensaToday = canteen.foodToday()
    val noFood = mensaToday.isEmpty() || mensaToday.all { (_, lines) -> lines.isEmpty() }

    if (noFood && scheduled) {
        logger.debug("Skipping sending of mensa plan to {} as there will be no food today.", roomId)
        return
    }

    var response = ""
    for ((mensa, lines) in mensaToday) {
        response += if (mensa.link == null) "## ${mensa.name}\n" else "## [${mensa.name}](<${mensa.link}>)\n"
        for (l in lines) {
            response += "### ${l.name}\n"
            for (meal in l.meals) response += "* ${meal.entry()}\n"
        }
    }

    if (response.isBlank()) {
        response = "Kein Essen heute :("
    }

    response = translationService.translate(response).trim()
    // Crop translation indications ..
    if (response.contains("#") && !response.startsWith("#")) {
        response = response.substring(response.indexOf("#"))
    }

    matrixBot.room().sendMessage(roomId) { markdown(response) }
}
