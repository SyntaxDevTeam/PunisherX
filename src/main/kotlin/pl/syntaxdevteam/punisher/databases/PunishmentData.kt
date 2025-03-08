package pl.syntaxdevteam.punisher.databases

/**
 * Data class for punishment data
 *
 * @param id The id of the punishment
 * @param uuid The uuid of the punished player
 * @param type The type of the punishment
 * @param reason The reason of the punishment
 * @param start The start of the punishment
 * @param end The end of the punishment
 * @param name The name of the punished player
 * @param operator The operator of the punishment
 */
data class PunishmentData(
    val id: Int,
    val uuid: String,
    val type: String,
    val reason: String,
    val start: Long,
    val end: Long,
    val name: String,
    val operator: String
)