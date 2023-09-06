package org.fuchss.matrix.mensa.handler.command

import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.fuchss.matrix.bots.MatrixBot
import org.fuchss.matrix.bots.command.Command
import org.fuchss.matrix.bots.markdown
import org.fuchss.matrix.mensa.Config

class SubscribeCommand(private val config: Config) : Command() {

    override val name: String = "subscribe"
    override val help: String = "shows instructions to subscribe for the channel"

    override suspend fun execute(matrixBot: MatrixBot, sender: UserId, roomId: RoomId, parameters: String) {
        var message = "Please send `${roomId.full}` to a bot admin to subscribe. Your admins are:"
        message += if (config.admins.isEmpty()) " ???" else "\n"

        for (admin in config.admins) {
            message += "\n* $admin"
        }

        matrixBot.room().sendMessage(roomId) { markdown(message) }
    }
}
