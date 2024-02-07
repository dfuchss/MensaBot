package org.fuchss.matrix.mensa

import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.login
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.events.roomIdOrNull
import net.folivo.trixnity.core.model.events.senderOrNull
import net.folivo.trixnity.core.subscribeContent
import org.fuchss.matrix.bots.MatrixBot
import org.fuchss.matrix.bots.command.ChangeUsernameCommand
import org.fuchss.matrix.bots.command.Command
import org.fuchss.matrix.bots.command.HelpCommand
import org.fuchss.matrix.bots.command.LogoutCommand
import org.fuchss.matrix.bots.command.QuitCommand
import org.fuchss.matrix.bots.helper.createMediaStore
import org.fuchss.matrix.bots.helper.createRepositoriesModule
import org.fuchss.matrix.bots.helper.handleEncryptedTextMessage
import org.fuchss.matrix.bots.helper.handleTextMessage
import org.fuchss.matrix.mensa.api.CanteenAPI
import org.fuchss.matrix.mensa.handler.command.ShowCommand
import org.fuchss.matrix.mensa.handler.command.SubscribeCommand
import org.fuchss.matrix.mensa.handler.sendCanteenEventToRoom
import org.fuchss.matrix.mensa.swka.SWKAMensa
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)
private val canteenAPI: CanteenAPI = SWKAMensa()

private lateinit var commands: List<Command>

fun main() {
    runBlocking {
        val config = Config.load()

        commands =
            listOf(
                HelpCommand(config, "MensaBot") {
                    commands
                },
                QuitCommand(config),
                LogoutCommand(config),
                ChangeUsernameCommand(),
                ShowCommand(canteenAPI),
                SubscribeCommand(config)
            )

        val matrixClient = getMatrixClient(config)

        val matrixBot = MatrixBot(matrixClient, config)

        matrixBot.subscribeContent { event -> handleTextMessage(commands, event.roomIdOrNull, event.senderOrNull, event.content, matrixBot, config) }
        matrixBot.subscribeContent { event -> handleEncryptedTextMessage(commands, event, matrixClient, matrixBot, config) }

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

    val matrixClient =
        MatrixClient.login(
            baseUrl = Url(config.baseUrl),
            identifier = IdentifierType.User(config.username),
            password = config.password,
            repositoriesModule = createRepositoriesModule(config),
            mediaStore = createMediaStore(config),
            initialDeviceDisplayName = "${MatrixBot::class.java.`package`.name}-${Random.Default.nextInt()}"
        ).getOrThrow()

    return matrixClient
}

private fun scheduleMensaMessages(
    matrixBot: MatrixBot,
    config: Config
): Timer {
    val timer = Timer(true)
    timer.schedule(
        object : TimerTask() {
            override fun run() {
                runBlocking {
                    logger.debug("Sending Mensa to Rooms (Scheduled) ...")

                    for (roomId in config.subscriptions()) {
                        try {
                            sendCanteenEventToRoom(roomId, matrixBot, true, canteenAPI)
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
