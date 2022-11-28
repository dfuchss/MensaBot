package org.fuchss.matrix.mensa

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.IMatrixClient
import net.folivo.trixnity.client.getOriginTimestamp
import net.folivo.trixnity.client.getRoomId
import net.folivo.trixnity.client.getSender
import net.folivo.trixnity.client.room
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.EventSubscriber
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.EventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.subscribe
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class MatrixBot(private val matrixClient: IMatrixClient, private val config: Config) {

    private val logger = LoggerFactory.getLogger(MatrixBot::class.java)

    private val runningTimestamp = Clock.System.now()
    private val validStates = listOf(SyncState.RUNNING, SyncState.INITIAL_SYNC, SyncState.STARTED)
    private val runningLock = Semaphore(1, 1)
    private var running: Boolean = false

    init {
        matrixClient.api.sync.subscribe { event -> handleJoinEvent(event) }
    }

    suspend fun startBlocking() {
        running = true
        logger.info("Starting Sync!")
        matrixClient.startSync()
        delay(1000)

        logger.info("Waiting for events ..")
        runningLock.acquire()

        logger.info("Shutting down!")
        while (matrixClient.syncState.value in validStates) {
            delay(500)
        }
        running = false
        matrixClient.api.authentication.logoutAll().getOrThrow()
    }

    fun room() = matrixClient.room

    fun subscribeAllEvents(subscriber: EventSubscriber<EventContent>) = matrixClient.api.sync.subscribeAllEvents { event ->
        if (valid(event, true)) subscriber(event)
    }

    fun <T : EventContent> subscribe(clazz: KClass<T>, subscriber: EventSubscriber<T>, listenNonAdmins: Boolean = false) {
        matrixClient.api.sync.subscribe(clazz) { event -> if (valid(event, listenNonAdmins)) subscriber(event) }
    }

    suspend fun quit() {
        runningLock.release()
        matrixClient.stopSync()
    }

    private fun valid(event: Event<*>, listenNonAdmins: Boolean): Boolean {
        if (!config.isAdmin(event.getSender()) && !listenNonAdmins) return false
        if (event.getSender() == matrixClient.userId) return false
        if (event.getOriginTimestamp() == null || Instant.fromEpochMilliseconds(event.getOriginTimestamp()!!) < runningTimestamp) return false
        return true
    }

    private suspend fun handleJoinEvent(event: Event<MemberEventContent>) {
        if (!config.isAdmin(event.getSender())) return

        if (event.content.membership != Membership.JOIN) {
            logger.info("Got Membership Event: $event")
            return
        }

        val room = matrixClient.room.getById(event.getRoomId()!!).value ?: return
        if (room.membership != Membership.INVITE) return

        if (room.encryptionAlgorithm != null) {
            logger.error("Cannot join room $room because it's encrypted")
            return
        }

        logger.info("Joining Room: ${event.getRoomId()}")
        matrixClient.api.rooms.joinRoom(event.getRoomId()!!)
    }

    suspend fun renameInRoom(roomId: RoomId, newNameInRoom: String) {
        val members = matrixClient.api.rooms.getMembers(roomId).getOrThrow()
        val myself = members.firstOrNull { it.stateKey == matrixClient.userId.full }?.content ?: return
        val newState = myself.copy(displayName = newNameInRoom)
        matrixClient.api.rooms.sendStateEvent(roomId, newState, stateKey = matrixClient.userId.full, asUserId = matrixClient.userId).getOrThrow()
    }

    fun running() = running
}
