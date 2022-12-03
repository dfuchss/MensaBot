package org.fuchss.matrix.mensa

import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.getEventId
import net.folivo.trixnity.client.getRoomId
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.MessageBuilder
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.EncryptedEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import okio.Path.Companion.toOkioPath
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.fuchss.matrix.mensa.api.MensaAPI
import org.fuchss.matrix.mensa.swka.SWKAMensaAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random
import kotlin.time.Duration

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)
private val mensa: MensaAPI = SWKAMensaAPI()

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
            scope = scope,
            mediaStore = OkioMediaStore(File("media").toOkioPath())
        ).getOrThrow()

        val matrixBot = MatrixBot(matrixClient, config)
        // matrixBot.subscribeAllEvents { event -> println(event) }
        matrixBot.subscribe { event -> handleTextMessage(event.getRoomId()!!, event.content, matrixBot, config) }
        matrixBot.subscribe { event -> handleEncryptedTextMessage(event, matrixClient, matrixBot, config) }

        val timer = scheduleMensaMessages(matrixBot, config)
        matrixBot.startBlocking()

        // After Shutdown
        timer.cancel()
    }
}

private suspend fun handleTextMessage(roomId: RoomId, content: RoomMessageEventContent.TextMessageEventContent, matrixBot: MatrixBot, config: Config) {
    var message = content.body
    if (!message.startsWith("!${config.prefix}")) {
        return
    }

    message = message.substring("!${config.prefix}".length).trim()

    when (message.split(Regex(" "), 2)[0]) {
        "quit" -> matrixBot.quit()
        "help" -> help(roomId, matrixBot, config)
        "name" -> changeUsername(roomId, matrixBot, message)
        "show" -> printMensa(roomId, matrixBot, false)
        "subscribe" -> subscribe(roomId, matrixBot, config)
    }
}

private suspend fun help(roomId: RoomId, matrixBot: MatrixBot, config: Config) {
    val helpMessage = """
        This is the Mensa Bot. You can use the following commands:
        
        * `!${config.prefix} help - shows this help message`
        * `!${config.prefix} quit - quits the bot`
        * `!${config.prefix} name [NEW_NAME] - sets the display name of the bot to NEW_NAME (only for the room)`
        * `!${config.prefix} show - shows the mensa food for the day`
        * `!${config.prefix} subscribe - shows instructions to subscribe for the channel`
    """.trimIndent()

    matrixBot.room().sendMessage(roomId) { markdown(helpMessage) }
}

private suspend fun changeUsername(roomId: RoomId, matrixBot: MatrixBot, message: String) {
    val newNameInRoom = message.substring("name".length).trim()
    if (newNameInRoom.isNotBlank()) {
        matrixBot.renameInRoom(roomId, newNameInRoom)
    }
}

private suspend fun printMensa(roomId: RoomId, matrixBot: MatrixBot, scheduled: Boolean) {
    logger.info("Sending Mensa to Room ${roomId.full}")

    val mensas = mensa.foodAtDate()
    var response = ""
    if (mensas.isEmpty() || mensas.all { mensa -> mensa.mensaLinesAtDate()?.isEmpty() != false }) {
        if (!scheduled) {
            response = "Kein Essen heute :("
        } else {
            logger.info("Skipping sending of mensa plan to $roomId as there will be no food today.")
        }
    } else {
        for (mensa in mensas) {
            if (mensas.size != 1) response += "## ${mensa.name}\n"
            for (l in mensa.mensaLinesAtDate() ?: listOf()) {
                response += "### ${l.title}\n"
                for (meal in l.meals) response += "* ${meal.entry()}\n"
            }
        }
    }
    if (response.isNotBlank()) {
        matrixBot.room().sendMessage(roomId) { markdown(response.trim()) }
    }
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
                    mensa.reload()

                    for (roomId in config.subscriptions()) {
                        try {
                            printMensa(roomId, matrixBot, true)
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

// Handle Encrypted Messages ..

private suspend fun handleEncryptedTextMessage(event: Event<EncryptedEventContent>, matrixClient: MatrixClient, matrixBot: MatrixBot, config: Config) {
    val timelineEvent: StateFlow<TimelineEvent?> = matrixClient.room.getTimelineEvent(event.getEventId()!!, event.getRoomId()!!).toStateFlow(null) { it?.content?.getOrNull() != null }
    val success = waitForEvent { timelineEvent.value?.content?.getOrNull() }
    if (!success) {
        logger.error("Cannot decrypt event $event within the given time ..")
        return
    }

    val content = timelineEvent.value!!.content!!.getOrThrow()
    if (content is RoomMessageEventContent.TextMessageEventContent) {
        handleTextMessage(event.getRoomId()!!, content, matrixBot, config)
    }
}

private suspend fun waitForEvent(maxTimeToWait: Duration = Duration.parse("5s"), waitStepInMS: Long = 1000, getter: () -> Any?): Boolean {
    val start = Clock.System.now()
    while (Clock.System.now() - start < maxTimeToWait) {
        val ready = getter()
        if (ready != null) {
            return true
        }
        delay(waitStepInMS)
    }
    return false
}
