package org.fuchss.matrix.mensa

import net.folivo.trixnity.core.EventSubscriber
import net.folivo.trixnity.core.model.events.EventContent

inline fun <reified T : EventContent> MatrixBot.subscribe(noinline subscriber: EventSubscriber<T>) {
    subscribe(T::class, subscriber)
}