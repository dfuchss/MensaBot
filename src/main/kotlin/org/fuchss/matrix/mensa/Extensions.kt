package org.fuchss.matrix.mensa

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.room.message.MessageBuilder
import net.folivo.trixnity.client.room.message.text
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

suspend fun <T> Flow<T>.firstWithTimeout(timeout: Duration = 1000.milliseconds, filter: (T) -> Boolean): T? {
    val that = this
    return withTimeoutOrNull(timeout) { that.first { filter(it) } }
}

fun MessageBuilder.markdown(markdown: String) {
    val document = Parser.builder().build().parse(markdown)
    val html = HtmlRenderer.builder().build().render(document)
    text(markdown, format = "org.matrix.custom.html", formattedBody = html)
}
