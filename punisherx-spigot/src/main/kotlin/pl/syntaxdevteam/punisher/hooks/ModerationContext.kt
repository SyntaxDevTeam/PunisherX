package pl.syntaxdevteam.punisher.hooks
import pl.syntaxdevteam.punisher.compatibility.*

data class ModerationContext(
    val nick: String,
    val commandAuthorDiscordId: String,
    val createdAtMs: Long
)
