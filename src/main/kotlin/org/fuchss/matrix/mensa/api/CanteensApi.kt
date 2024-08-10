package org.fuchss.matrix.mensa.api

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * This interface defines the minimum functionality that a canteen provides.
 */
interface CanteensApi {
    suspend fun foodToday() = foodAtDate(Clock.System.todayIn(TimeZone.currentSystemDefault()))

    /**
     * Retrieve foods of different canteens at a certain date.
     * @param[date] the date to consider
     * @return a map that contains canteens with food mapped to their lines at a certain day
     * (remember: the key (canteen) may contain more information than the value (list of canteen lines))
     */
    suspend fun foodAtDate(date: LocalDate): Map<Canteen, List<CanteenLine>>
}
