package pl.syntaxdevteam.punisher.placeholders

import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import me.clip.placeholderapi.expansion.PlaceholderExpansion

class PlaceholderHandler(private val plugin: PunisherX) : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "prx"
    }

    override fun getAuthor(): String {
        return plugin.pluginMeta.authors.joinToString()
    }

    override fun getVersion(): String {
        return plugin.pluginMeta.version
    }

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) {
            return ""
        }

        return when (params) {
            "mute_remaining_time" -> getPunishmentEndTime(player.name, "MUTE") ?: ""
            "warn_remaining_time" -> getPunishmentEndTime(player.name, "WARN") ?: ""
            "jail_remaining_time" -> getPunishmentEndTime(player.name, "JAIL") ?: ""
            "total_active_punishments" -> getAllPunishments() ?: ""
            "total_punishments" -> getAllPunishmentHistory() ?: ""
            else -> null
        }
    }

    private fun getPunishmentEndTime(player: String, punishType: String): String? {
        val uuid = plugin.resolvePlayerUuid(player)
        val punishments = plugin.databaseHandler.getPunishments(uuid.toString())
        val punishData = punishments.find { it.type == punishType && it.end > System.currentTimeMillis() } ?: return null
        val punishment = when (punishType) {
            "MUTE" -> "mute_remaining_time"
            "WARN" -> "warn_remaining_time"
            "JAIL" -> "jail_remaining_time"
            else -> return null
        }
        val remainingTime = (punishData.end - System.currentTimeMillis())  / 1000
        return if (remainingTime > 0) {
            plugin.messageHandler.getCleanMessage("placeholders", punishment) + plugin.timeHandler.formatTime(remainingTime.toString())
        } else {
            null
        }
    }

    private fun getAllPunishments(): String? {
        val totalPunishments = plugin.databaseHandler.countAllPunishments()
        return if (totalPunishments > 0) {
        plugin.logger.debug("Total active punishments: $totalPunishments")
        plugin.logger.debug("Formatting message for total active punishments: ${plugin.messageHandler.getCleanMessage("placeholders", "total_active_punishments")}")
        plugin.messageHandler.getCleanMessage("placeholders", "total_active_punishments") + totalPunishments.toString()
        } else {
            null
        }

    }

    private fun getAllPunishmentHistory(): String? {
        val totalPunishmentHistory = plugin.databaseHandler.countAllPunishmentHistory()
        return if (totalPunishmentHistory > 0) {
            plugin.logger.debug("Total punishments: $totalPunishmentHistory")
            plugin.logger.debug("Formatting message for total punishments: ${plugin.messageHandler.getCleanMessage("placeholders", "total_punishments")}")
            plugin.messageHandler.getCleanMessage("placeholders", "total_punishments").let {
                it + totalPunishmentHistory.toString()
            }
        } else {
            null
        }
    }
}
