package org.fuchss.matrix.mensa.handler.command

import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.fuchss.matrix.bots.MatrixBot
import org.fuchss.matrix.bots.command.Command
import org.fuchss.matrix.mensa.api.CanteenAPI
import org.fuchss.matrix.mensa.handler.sendCanteenEventToRoom

class ShowCommand(private val canteen: CanteenAPI) : Command() {
    override val name: String = "show"
    override val help: String = "show the mensa plan for today"

    override suspend fun execute(
        matrixBot: MatrixBot,
        sender: UserId,
        roomId: RoomId,
        parameters: String
    ) {
        sendCanteenEventToRoom(roomId, matrixBot, false, canteen)
    }
}
