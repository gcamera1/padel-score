package com.gonzalocamera.padelcounter.mobile.ui.history

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

/**
 * El DatePicker de Material3 razona en medianoche UTC. Convertirlo mal hace que en husos
 * negativos el partido caiga en el día anterior — un bug invisible si el CI corre en UTC,
 * por eso cada caso se prueba con un huso negativo y uno positivo.
 */
class ManualMatchDateTest {

    private val buenosAires: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires") // UTC-3
    private val auckland: ZoneId = ZoneId.of("Pacific/Auckland")                  // UTC+12/13

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun utcMidnightOf(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun localDateTimeOf(millis: Long, zone: ZoneId): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    @Test
    fun `utcDateMillisToLocalNoon lands at local noon of the same day in a negative offset`() {
        val date = LocalDate.of(2026, 7, 26)

        val result = utcDateMillisToLocalNoon(utcMidnightOf(date), buenosAires)

        assertThat(localDateTimeOf(result, buenosAires))
            .isEqualTo(LocalDateTime.of(2026, 7, 26, 12, 0))
    }

    @Test
    fun `utcDateMillisToLocalNoon lands at local noon of the same day in a positive offset`() {
        val date = LocalDate.of(2026, 7, 26)

        val result = utcDateMillisToLocalNoon(utcMidnightOf(date), auckland)

        assertThat(localDateTimeOf(result, auckland))
            .isEqualTo(LocalDateTime.of(2026, 7, 26, 12, 0))
    }

    @Test
    fun `utcDateMillisToLocalNoon uses the system zone by default`() {
        TimeZone.setDefault(TimeZone.getTimeZone(buenosAires))
        val date = LocalDate.of(2026, 1, 5)

        val result = utcDateMillisToLocalNoon(utcMidnightOf(date))

        assertThat(localDateTimeOf(result, buenosAires))
            .isEqualTo(LocalDateTime.of(2026, 1, 5, 12, 0))
    }

    @Test
    fun `localMillisToUtcMidnight is the inverse of utcDateMillisToLocalNoon`() {
        for (zone in listOf(buenosAires, auckland)) {
            val utcMidnight = utcMidnightOf(LocalDate.of(2026, 7, 26))

            val roundTrip = localMillisToUtcMidnight(
                utcDateMillisToLocalNoon(utcMidnight, zone),
                zone,
            )

            assertThat(roundTrip).isEqualTo(utcMidnight)
        }
    }

    @Test
    fun `todayUtcMidnight matches today in the given zone`() {
        for (zone in listOf(buenosAires, auckland)) {
            val expected = utcMidnightOf(LocalDate.now(zone))

            assertThat(todayUtcMidnight(zone)).isEqualTo(expected)
        }
    }

    @Test
    fun `a date picked today never becomes tomorrow`() {
        for (zone in listOf(buenosAires, auckland)) {
            val noon = utcDateMillisToLocalNoon(todayUtcMidnight(zone), zone)

            assertThat(localDateTimeOf(noon, zone).toLocalDate()).isEqualTo(LocalDate.now(zone))
        }
    }
}
