package org.fuchss.matrix.mensa

import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.getRoomId
import net.folivo.trixnity.client.room.message.MessageBuilder
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.fuchss.matrix.mensa.request.MensaAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)
private var mensa = MensaAPI()

fun main() {
    runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val config = Config.load()

        val matrixClient = MatrixClient.login(
            baseUrl = Url(config.baseUrl),
            IdentifierType.User(config.username),
            config.password,
            initialDeviceDisplayName = "${MatrixBot::class.java.`package`.name}-${Random.Default.nextInt()}",
            repositoriesModule = createInMemoryRepositoriesModule(),
            scope = scope
        ).getOrThrow()

        val matrixBot = MatrixBot(matrixClient, config)
        // matrixBot.subscribeAllEvents { event -> println(event) }
        matrixBot.subscribe { event -> handleTextMessage(event, matrixBot, config) }

        val timer = scheduleMensaMessages(matrixBot, config)
        matrixBot.startBlocking()

        // After Shutdown
        timer.cancel()
    }
}

private suspend fun handleTextMessage(event: Event<RoomMessageEventContent.TextMessageEventContent>, matrixBot: MatrixBot, config: Config) {
    var message = event.content.body
    if (!message.startsWith("!${config.prefix}")) {
        return
    }

    message = message.substring("!${config.prefix}".length).trim()

    when (message.split(Regex(" "), 2)[0]) {
        "quit" -> matrixBot.quit()
        "help" -> help(event, matrixBot, config)
        "name" -> changeUsername(event, matrixBot, message)
        "show" -> printMensa(event.getRoomId()!!, matrixBot)
        "subscribe" -> subscribe(event.getRoomId()!!, matrixBot, config)
    }
}

private suspend fun help(event: Event<RoomMessageEventContent.TextMessageEventContent>, matrixBot: MatrixBot, config: Config) {
    val helpMessage = """
        This is the Mensa Bot. You can use the following commands:
        
        * `!${config.prefix} help - shows this help message`
        * `!${config.prefix} quit - quits the bot`
        * `!${config.prefix} name [NEW_NAME] - sets the display name of the bot to NEW_NAME (only for the room)`
        * `!${config.prefix} show - shows the mensa food for the day`
        * `!${config.prefix} subscribe - shows instructions to subscribe for the channel`
    """.trimIndent()

    matrixBot.room().sendMessage(event.getRoomId()!!) { markdown(helpMessage) }
}

private suspend fun changeUsername(event: Event<RoomMessageEventContent.TextMessageEventContent>, matrixBot: MatrixBot, message: String) {
    val newNameInRoom = message.substring("name".length).trim()
    if (newNameInRoom.isNotBlank()) {
        matrixBot.renameInRoom(event.getRoomId()!!, newNameInRoom)
    }
}

private suspend fun printMensa(roomId: RoomId, matrixBot: MatrixBot) {
    logger.info("Sending Mensa to Room ${roomId.full}")

    val mensas = mensa.foodAtDate()
    var response = ""
    if (mensas.isEmpty() || mensas.all { mensa -> mensa.mensaLinesAtDate()?.isEmpty() != false }) {
        response = "Kein Essen heute :("
    } else {
        for (mensa in mensas) {
            response += "# ${mensa.name}\n"
            for (l in mensa.mensaLinesAtDate() ?: listOf()) {
                response += "## ${l.title}\n"
                for (meal in l.meals) response += "* ${meal.name}\n"
            }
        }
    }

    matrixBot.room().sendMessage(roomId) { markdown(response) }
}

private suspend fun subscribe(roomId: RoomId, matrixBot: MatrixBot, config: Config) {
    var message = "Please send `${roomId.full}` to a bot admin to subscribe. Your admins are:"
    message += if (config.admins.isEmpty()) " ???" else "\n"

    for (admin in config.admins) {
        message += "\n* $admin"
    }

    matrixBot.room().sendMessage(roomId) { markdown(message) }
}

private fun scheduleMensaMessages(matrixBot: MatrixBot, config: Config): Timer {
    val timer = Timer(true)
    timer.schedule(
        object : TimerTask() {
            override fun run() {
                runBlocking {
                    logger.debug("Sending Mensa to Rooms (Scheduled) ...")
                    // Reinit Mensa API
                    mensa = MensaAPI()

                    for (roomId in config.subscriptions()) {
                        try {
                            printMensa(roomId, matrixBot)
                        } catch (e: Exception) {
                            logger.error(e.message, e)
                        }
                    }
                }
            }
        },
        config.nextTimer(),
        24 * 60 * 60 * 1000
    )
    return timer
}

private fun MessageBuilder.markdown(markdown: String) {
    val document = Parser.builder().build().parse(markdown)
    val html = HtmlRenderer.builder().build().render(document)
    text(markdown, format = "org.matrix.custom.html", formattedBody = html)
}
