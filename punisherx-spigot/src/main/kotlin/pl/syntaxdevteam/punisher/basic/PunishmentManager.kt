package pl.syntaxdevteam.punisher.basic

import pl.syntaxdevteam.punisher.databases.PunishmentData


class PunishmentManager {

    fun isPunishmentActive(punishment: PunishmentData): Boolean {
        val currentTime = System.currentTimeMillis()
        return punishment.end > currentTime || punishment.end == -1L
    }
}
