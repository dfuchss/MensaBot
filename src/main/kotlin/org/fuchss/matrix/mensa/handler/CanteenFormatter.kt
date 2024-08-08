package org.fuchss.matrix.mensa.handler

import TranslationService
import net.folivo.trixnity.core.model.RoomId
import org.fuchss.matrix.bots.MatrixBot
import org.fuchss.matrix.bots.markdown
import org.fuchss.matrix.mensa.api.CanteenAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)

suspend fun sendCanteenEventToRoom(
    roomId: RoomId,
    matrixBot: MatrixBot,
    scheduled: Boolean,
    canteen: CanteenAPI,
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
        if (mensaToday.size != 1) response += "## ${mensa.name}\n"
        for (l in lines) {
            response += "### ${l.name}\n"
            for (meal in l.meals) response += "* ${meal.entry()}\n"
        }
    }

    if (response.isBlank()) {
        response = "Kein Essen heute :("
    }

    response = translationService.translate(response)

    matrixBot.room().sendMessage(roomId) { markdown(response.trim()) }
}
