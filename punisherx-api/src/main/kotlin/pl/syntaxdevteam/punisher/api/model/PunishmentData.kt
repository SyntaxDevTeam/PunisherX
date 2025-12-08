package pl.syntaxdevteam.punisher.api.model

/**
 * Basic representation of a punishment exposed via the public API.
 */
data class PunishmentData(
    val id: Int,
    val uuid: String,
    val type: String,
    val reason: String,
    val start: Long,
    val end: Long,
    val name: String,
    val operator: String,
)
