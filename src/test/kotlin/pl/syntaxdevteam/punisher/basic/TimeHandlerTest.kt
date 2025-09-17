package pl.syntaxdevteam.punisher.basic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimeHandlerTest {
    private val messageMap = mapOf(
        "formatTime.undefined" to "undefined",
        "formatTime.day.one" to "day_one",
        "formatTime.day.few" to "day_few",
        "formatTime.day.many" to "day_many",
        "formatTime.hour.one" to "hour_one",
        "formatTime.hour.few" to "hour_few",
        "formatTime.hour.many" to "hour_many",
        "formatTime.minute.one" to "minute_one",
        "formatTime.minute.few" to "minute_few",
        "formatTime.minute.many" to "minute_many",
        "formatTime.second.one" to "second_one",
        "formatTime.second.few" to "second_few",
        "formatTime.second.many" to "second_many",
        "error.no_data" to "no_data"
    )

    private val provider = TimeHandler.MessageProvider { path, key ->
        val messageKey = "$path.$key"
        messageMap[messageKey] ?: error("Unexpected message key: $messageKey")
    }

    private fun createHandler(currentTime: () -> Long = { 0L }) = TimeHandler(provider, currentTime)

    @Test
    fun `parseTime converts supported suffixes to seconds`() {
        val handler = createHandler()
        assertEquals(45L, handler.parseTime("45s"))
        assertEquals(120L, handler.parseTime("2m"))
        assertEquals(3600L, handler.parseTime("1h"))
        assertEquals(172800L, handler.parseTime("2d"))
        assertEquals(0L, handler.parseTime("3x"))
    }

    @Test
    fun `formatTime handles null numeric and suffixed inputs`() {
        val handler = createHandler()
        assertEquals("undefined", handler.formatTime(null))
        assertEquals("1 hour_one, 1 minute_one, 1 second_one", handler.formatTime("3661"))
        assertEquals("3 day_few", handler.formatTime("3d"))
        assertEquals("10 minute_many", handler.formatTime("10m"))
        assertEquals("undefined", handler.formatTime("5x"))
    }

    @Test
    fun `parseDate handles valid and invalid formats`() {
        val handler = createHandler()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val zone = ZoneId.systemDefault()
        val reference = LocalDateTime.of(2024, 12, 31, 23, 59, 59)
        val expected = reference.atZone(zone).toInstant().toEpochMilli()

        val parsed = handler.parseDate(reference.format(formatter))
        assertEquals(expected, parsed)
        assertNull(handler.parseDate("not-a-date"))
    }

    @Test
    fun `getOfflineDuration returns formatted difference or fallback`() {
        var currentMillis = 0L
        val handler = createHandler { currentMillis }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val zone = ZoneId.systemDefault()
        val currentInstant = Instant.parse("2025-01-01T00:00:00Z")
        val totalSeconds = (((((1L * 365 + 2L * 30 + 3L * 7 + 4L) * 24 + 5L) * 60 + 6L) * 60) + 7L)
        val lastInstant = currentInstant.minusSeconds(totalSeconds)
        val lastUpdated = LocalDateTime.ofInstant(lastInstant, zone).format(formatter)

        currentMillis = currentInstant.toEpochMilli()

        assertEquals("1Y 2M 3W 4d 5h 6m 7s", handler.getOfflineDuration(lastUpdated))
        assertEquals("no_data", handler.getOfflineDuration("invalid-date"))
    }
}