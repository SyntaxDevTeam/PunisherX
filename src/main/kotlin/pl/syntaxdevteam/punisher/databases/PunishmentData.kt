package pl.syntaxdevteam.punisher.databases

data class PunishmentData(val id: Int, val uuid: String, val type: String, val reason: String, val start: Long, val end: Long)