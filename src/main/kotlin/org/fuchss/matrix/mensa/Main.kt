package org.fuchss.matrix.mensa

import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.getRoomId
import net.folivo.trixnity.client.getSender
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.fuchss.matrix.mensa.request.MensaAPI
import kotlin.random.Random

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
        matrixBot.subscribeAllEvents { event -> println(event) }
        matrixBot.subscribe { event -> handleTextMessage(event, matrixBot) }
        matrixBot.startBlocking()
    }
}


private suspend fun handleTextMessage(event: Event<RoomMessageEventContent.TextMessageEventContent>, matrixBot: MatrixBot) {
    val message = event.content.body
    if (message.startsWith("!quit")) {
        matrixBot.quit()
    }

    if (message.startsWith("!name")) {
        val newNameInRoom = message.substring("!name".length).trim()
        if (newNameInRoom.isNotBlank()) matrixBot.renameInRoom(event.getRoomId()!!, newNameInRoom)
    }


    if (message.startsWith("!mensa")) {
        val food = MensaAPI().foodAtDate()
        var response = ""
        if (food.isEmpty() || food.all { mensa -> mensa.mensaLinesAtDate()!!.isEmpty() }) response = "Kein Essen heute :("
        else {
            for (m in food) {
                response += "# ${m.name}\n"
                for (l in m.mensaLinesAtDate()!!) {
                    response += "## ${l.title}\n"
                    for (meal in l.meals) response += "* ${meal.name}\n"
                }
            }
        }

        val document = Parser.builder().build().parse(response)
        val html = HtmlRenderer.builder().build().render(document)

        matrixBot.room().sendMessage(event.getRoomId()!!) {
            text(response, format = "org.matrix.custom.html", formattedBody = html)
        }
    }
}


