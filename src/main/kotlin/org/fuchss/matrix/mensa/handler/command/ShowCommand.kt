package org.fuchss.matrix.mensa.handler.command

import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.fuchss.matrix.bots.MatrixBot
import org.fuchss.matrix.bots.command.Command
import org.fuchss.matrix.mensa.TranslationService
import org.fuchss.matrix.mensa.api.CanteenApi
import org.fuchss.matrix.mensa.handler.sendCanteenEventToRoom

class ShowCommand(
    private val canteens: List<CanteenApi>,
    private val translationService: TranslationService
) : Command() {
    override val name: String = "show"
    override val help: String = "show the mensa plan for today, if id provided, show the mensa plan for the canteen with the id"
    override val params: String = "[canteen_id]"

    override suspend fun execute(
        matrixBot: MatrixBot,
        sender: UserId,
        roomId: RoomId,
        parameters: String,
        textEventId: EventId,
        textEvent: RoomMessageEventContent.TextBased.Text
    ) {
        val consideredCanteens = if (parameters.isEmpty()) canteens else canteens.filter { it.canteen().id == parameters }
        if (consideredCanteens.isEmpty()) {
            matrixBot.room().sendMessage(roomId) { text("No canteen with id $parameters found.") }
            return
        }
        sendCanteenEventToRoom(roomId, matrixBot, false, consideredCanteens, translationService)
    }
}
