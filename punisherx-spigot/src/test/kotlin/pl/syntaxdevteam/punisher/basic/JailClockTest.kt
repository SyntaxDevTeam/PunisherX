package pl.syntaxdevteam.punisher.basic

import kotlin.test.Test
import kotlin.test.assertEquals

class JailClockTest {
    @Test fun `offline time does not consume remaining sentence`() {
        val paused = JailClock.pause(11000, 1000)
        assertEquals(1010000L, JailClock.resume(paused, 1000000))
        assertEquals(paused, JailClock.pause(paused, 2000))
    }
    @Test fun `permanent and expired punishments stay unchanged`() {
        assertEquals(-1L, JailClock.pause(-1, 1000))
        assertEquals(-1L, JailClock.resume(-1, 1000))
        assertEquals(500L, JailClock.pause(500, 1000))
    }
}
