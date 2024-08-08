package org.fuchss.matrix.mensa.handler.command

import TranslationService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.fuchss.matrix.bots.MatrixBot
import org.fuchss.matrix.bots.command.Command
import org.fuchss.matrix.mensa.api.CanteenAPI
import org.fuchss.matrix.mensa.handler.sendCanteenEventToRoom

class ShowCommand(private val canteen: CanteenAPI, private val translationService: TranslationService) : Command() {
    override val name: String = "show"
    override val help: String = "show the mensa plan for today"

    override suspend fun execute(
        matrixBot: MatrixBot,
        sender: UserId,
        roomId: RoomId,
        parameters: String,
        textEventId: EventId,
        textEvent: RoomMessageEventContent.TextBased.Text
    ) {
        sendCanteenEventToRoom(roomId, matrixBot, false, canteen, translationService)
    }
}
