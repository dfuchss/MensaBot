package org.fuchss.matrix.mensa

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

private val logger = LoggerFactory.getLogger("FlowHelper")
private val executor = Executors.newFixedThreadPool(2)

suspend fun <T> Flow<T?>.toStateFlow(default: T, validator: (T) -> Boolean): StateFlow<T> {
    val stateFlow = MutableStateFlow(default)
    executor.submit {
        logger.debug("$executor")
        runBlocking {
            collectLatest {
                if (it != null) stateFlow.emit(it)
                if (it != null && validator(it)) {
                    logger.debug("Reached final state of $stateFlow")
                    this.cancel()
                }
            }
        }
    }
    return stateFlow
}
