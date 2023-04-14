package org.fuchss.matrix.mensa.swka

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SWKAMensaTest {
    @Test
    fun testMensaAPI() {
        runBlocking {
            val mensa = SWKAMensa()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val someWorkDay = if (today.dayOfWeek.value > 5) today.plus(2, DateTimeUnit.DAY) else today
            val food = mensa.foodAtDate(someWorkDay)
            assertNotNull(food)
        }
    }

    @Test
    fun testWeekInYear() {
        val mensa = SWKAMensa()
        assertEquals(52, mensa.numberOfWeek(LocalDate(2023, 1, 1)))
        assertEquals(1, mensa.numberOfWeek(LocalDate(2023, 1, 2)))
        assertEquals(1, mensa.numberOfWeek(LocalDate(2023, 1, 3)))
        assertEquals(1, mensa.numberOfWeek(LocalDate(2023, 1, 4)))
        assertEquals(1, mensa.numberOfWeek(LocalDate(2023, 1, 5)))
        assertEquals(1, mensa.numberOfWeek(LocalDate(2023, 1, 6)))
        assertEquals(1, mensa.numberOfWeek(LocalDate(2023, 1, 7)))
        assertEquals(1, mensa.numberOfWeek(LocalDate(2023, 1, 8)))
        assertEquals(2, mensa.numberOfWeek(LocalDate(2023, 1, 9)))
        assertEquals(15, mensa.numberOfWeek(LocalDate(2023, 4, 14)))
        assertEquals(1, mensa.numberOfWeek(LocalDate(2024, 1, 1)))
        assertEquals(1, mensa.numberOfWeek(LocalDate(2025, 1, 1)))
        assertEquals(1, mensa.numberOfWeek(LocalDate(2026, 1, 1)))
        assertEquals(53, mensa.numberOfWeek(LocalDate(2027, 1, 1)))
    }
}
