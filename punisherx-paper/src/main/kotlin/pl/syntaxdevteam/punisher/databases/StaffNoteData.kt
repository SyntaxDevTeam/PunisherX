package pl.syntaxdevteam.punisher.databases

import java.util.UUID

/** Private staff-only observation, never a punishment or an anti-cheat verdict. */
data class StaffNoteData(
    val id: Long,
    val player: UUID,
    val author: String,
    val content: String,
    val createdAt: Long,
    val server: String
)
