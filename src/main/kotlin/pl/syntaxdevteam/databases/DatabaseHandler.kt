package pl.syntaxdevteam.databases

interface DatabaseHandler {
    fun openConnection()
    fun closeConnection()
    fun createTables()
    fun addPunishment(name: String, uuid: String, reason: String, operator: String, punishmentType: String, start: Long, end: Long): Boolean
    fun addPunishmentHistory(name: String, uuid: String, reason: String, operator: String, punishmentType: String, start: Long, end: Long)
    fun removePunishment(uuidOrIp: String, punishmentType: String, removeAll: Boolean = false)
    fun getPunishments(uuid: String): List<PunishmentData>
    fun getPunishmentsByIP(ip: String): List<PunishmentData>
    fun getActiveWarnCount(uuid: String): Int
    fun getPunishmentHistory(uuid: String, limit: Int, offset: Int): List<PunishmentData>
    fun updatePunishmentReason(id: Int, newReason: String): Boolean
    fun exportDatabase()
    fun importDatabase()
}
