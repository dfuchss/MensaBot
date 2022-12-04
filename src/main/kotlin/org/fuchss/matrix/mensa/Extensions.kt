package org.fuchss.matrix.mensa

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.room.message.MessageBuilder
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.EventSubscriber
import net.folivo.trixnity.core.model.events.EventContent
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Same as [Flow.first] but with a defined timeout that leads to null if reached.
 * @param predicate a predicate to filter the results of [Flow.first]
 * @return the result of [Flow.first] or null
 */
suspend fun <T> Flow<T>.firstWithTimeout(timeout: Duration = 1000.milliseconds, predicate: suspend (T) -> Boolean): T? {
    val that = this
    return withTimeoutOrNull(timeout) { that.first { predicate(it) } }
}

/**
 * Format a markdown message and send it using a [MessageBuilder]
 * @param[markdown] the plain Markdown text
 */
fun MessageBuilder.markdown(markdown: String) {
    val document = Parser.builder().build().parse(markdown)
    val html = HtmlRenderer.builder().build().render(document)
    text(markdown, format = "org.matrix.custom.html", formattedBody = html)
}

/**
 * Subscribe to a certain class of event. Note that you can only subscribe for events that are sent by an admin by default.
 * @param[subscriber] the function to invoke for the events
 * @param[listenNonAdmins] whether you want to subscribe for events from non admins
 * @see MatrixBot.subscribe
 */
inline fun <reified T : EventContent> MatrixBot.subscribe(listenNonAdmins: Boolean = false, noinline subscriber: EventSubscriber<T>) {
    subscribe(T::class, subscriber, listenNonAdmins)
}
