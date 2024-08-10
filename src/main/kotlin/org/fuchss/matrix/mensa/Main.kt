package org.fuchss.matrix.mensa

import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.login
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import org.fuchss.matrix.bots.MatrixBot
import org.fuchss.matrix.bots.command.ChangeUsernameCommand
import org.fuchss.matrix.bots.command.Command
import org.fuchss.matrix.bots.command.HelpCommand
import org.fuchss.matrix.bots.command.LogoutCommand
import org.fuchss.matrix.bots.command.QuitCommand
import org.fuchss.matrix.bots.helper.createMediaStore
import org.fuchss.matrix.bots.helper.createRepositoriesModule
import org.fuchss.matrix.bots.helper.handleCommand
import org.fuchss.matrix.bots.helper.handleEncryptedCommand
import org.fuchss.matrix.mensa.api.CanteensApi
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

private lateinit var commands: List<Command>

fun main() {
    runBlocking {
        val config = Config.load()
        val canteenApi: CanteensApi = SWKAMensa()
        val translationService = TranslationService(config.translation)

        commands =
            listOf(
                HelpCommand(config, "MensaBot") {
                    commands
                },
                QuitCommand(config),
                LogoutCommand(config),
                ChangeUsernameCommand(),
                ShowCommand(canteenApi, translationService),
                SubscribeCommand(config)
            )

        val matrixClient = getMatrixClient(config)

        val matrixBot = MatrixBot(matrixClient, config)

        matrixBot.subscribeContent { event -> handleCommand(commands, event, matrixBot, config) }
        matrixBot.subscribeContent { event -> handleEncryptedCommand(commands, event, matrixBot, config) }

        val timer = scheduleMensaMessages(matrixBot, config, canteenApi, translationService)

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
    config: Config,
    canteenApi: CanteensApi,
    translationService: TranslationService
): Timer {
    val timer = Timer(true)
    timer.schedule(
        object : TimerTask() {
            override fun run() {
                runBlocking {
                    logger.debug("Sending Mensa to Rooms (Scheduled) ...")

                    for (roomId in config.subscriptions()) {
                        try {
                            sendCanteenEventToRoom(roomId, matrixBot, true, canteenApi, translationService)
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
