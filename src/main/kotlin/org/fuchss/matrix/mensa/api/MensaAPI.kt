package org.fuchss.matrix.mensa.api

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * This interface defines the minimum functionality that a mensa provides.
 */
interface MensaAPI {
    /**
     * Retrieve foods of different mensas at a certain date.
     * @param[date] the date to consider
     * @return a map that contains mensas with food mapped to their lines at a certain day (remember: the key (mensa) may contain more information than the value (list of mensa lines))
     */
    suspend fun foodAtDate(date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Map<Mensa, List<MensaLine>>

    /**
     * Reload the API. Invalidate caches.
     */
    suspend fun reload()
}
