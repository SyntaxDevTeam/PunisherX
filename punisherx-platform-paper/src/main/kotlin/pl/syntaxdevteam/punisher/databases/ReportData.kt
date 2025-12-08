package pl.syntaxdevteam.punisher.databases

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Represents a report submitted by a player against another player.
 *
 * Provides a helper for formatting the entry into a readable summary so it
 * can be displayed in logs, chat messages or GUIs without duplicating
 * presentation code at the call site.
 */
data class ReportData(
    val id: Int,
    val player: UUID,
    val suspect: UUID,
    val reason: String,
    val filedAt: Instant
) {
    fun toSummary(
        zoneId: ZoneId = ZoneId.systemDefault(),
        formatter: DateTimeFormatter = DEFAULT_FORMATTER
    ): String {
        val timestamp = formatter.format(filedAt.atZone(zoneId))
        return "#$id $player -> $suspect @ $timestamp: $reason"
    }

    companion object {
        private val DEFAULT_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}

