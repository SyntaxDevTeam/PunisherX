package pl.syntaxdevteam.punisher.hooks

data class ModerationContext(
    val nick: String,
    val commandAuthorDiscordId: String,
    val createdAtMs: Long
)
