package org.fuchss.matrix.mensa

import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.getEventId
import net.folivo.trixnity.client.getRoomId
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.repository.exposed.createExposedRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.EncryptedEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import okio.Path.Companion.toOkioPath
import org.fuchss.matrix.mensa.api.CanteenAPI
import org.fuchss.matrix.mensa.swka.SWKAMensa
import org.jetbrains.exposed.sql.Database
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)
private val mensa: CanteenAPI = SWKAMensa()

fun main() {
    runBlocking {
        val config = Config.load()

        val matrixClient = getMatrixClient(config)

        val matrixBot = MatrixBot(matrixClient, config)

        matrixBot.subscribe { event -> handleTextMessage(event.getRoomId(), event.content, matrixBot, config) }
        matrixBot.subscribe { event -> handleEncryptedTextMessage(event, matrixClient, matrixBot, config) }

        val timer = scheduleMensaMessages(matrixBot, config)

        val loggedOut = matrixBot.startBlocking()

        // After Shutdown
        timer.cancel()

        if (loggedOut) {
            // Cleanup database
            val databaseFiles = listOf(File(config.dataDirectory + "/database.mv.db"), File(config.dataDirectory + "/database.trace.db"))
            databaseFiles.filter { it.exists() }.forEach { it.delete() }
        }
    }
}

private suspend fun getMatrixClient(config: Config): MatrixClient {
    val existingMatrixClient = MatrixClient.fromStore(createRepositoriesModule(config), createMediaStore(config)).getOrThrow()
    if (existingMatrixClient != null) {
        return existingMatrixClient
    }

    val matrixClient = MatrixClient.login(
        baseUrl = Url(config.baseUrl),
        identifier = IdentifierType.User(config.username),
        password = config.password,
        repositoriesModule = createRepositoriesModule(config),
        mediaStore = createMediaStore(config),
        initialDeviceDisplayName = "${MatrixBot::class.java.`package`.name}-${Random.Default.nextInt()}"
    ).getOrThrow()

    return matrixClient
}

private suspend fun createRepositoriesModule(config: Config) =
    createExposedRepositoriesModule(database = Database.connect("jdbc:h2:${config.dataDirectory}/database;DB_CLOSE_DELAY=-1"))

private fun createMediaStore(config: Config) = OkioMediaStore(File(config.dataDirectory + "/media").toOkioPath())

private suspend fun handleEncryptedTextMessage(event: Event<EncryptedEventContent>, matrixClient: MatrixClient, matrixBot: MatrixBot, config: Config) {
    val roomId = event.getRoomId() ?: return
    val eventId = event.getEventId() ?: return

    logger.debug("Waiting for decryption of {} ..", event)
    val decryptedEvent = matrixClient.room.getTimelineEvent(roomId, eventId).firstWithTimeout { it?.content != null }
    if (decryptedEvent != null) {
        logger.debug("Decryption of {} was successful", event)
    }

    if (decryptedEvent == null) {
        logger.error("Cannot decrypt event $event within the given time ..")
        return
    }

    val content = decryptedEvent.content?.getOrNull() ?: return
    if (content is RoomMessageEventContent.TextMessageEventContent) {
        handleTextMessage(roomId, content, matrixBot, config)
    }
}

private suspend fun handleTextMessage(roomId: RoomId?, content: RoomMessageEventContent.TextMessageEventContent, matrixBot: MatrixBot, config: Config) {
    if (roomId == null) {
        return
    }

    var message = content.body
    if (!message.startsWith("!${config.prefix}")) {
        return
    }

    message = message.substring("!${config.prefix}".length).trim()

    when (message.split(Regex(" "), 2)[0]) {
        "quit" -> matrixBot.quit()
        "logout" -> matrixBot.quit(logout = true)
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
        * `!${config.prefix} logout - quits the bot and logs out all devices`
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

    val mensaToday = mensa.foodToday()
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

    matrixBot.room().sendMessage(roomId) { markdown(response.trim()) }
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
        config.nextReminder(),
        1.days.inWholeMilliseconds
    )
    return timer
}
