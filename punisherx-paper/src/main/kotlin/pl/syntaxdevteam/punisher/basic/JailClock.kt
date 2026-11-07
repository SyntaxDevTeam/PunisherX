package pl.syntaxdevteam.punisher.basic

/** -1 means permanent; values below -1 encode a paused jail's remaining milliseconds. */
object JailClock {
    fun pause(end: Long, now: Long): Long = if (end > now) -(end - now + 2) else end
    fun resume(end: Long, now: Long): Long = if (end < -1L) now + (-end - 2) else end
}
