package org.fuchss.matrix.mensa.api

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

interface MensaAPI {
    suspend fun foodAtDate(date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): List<Mensa>
    suspend fun reload()
}
