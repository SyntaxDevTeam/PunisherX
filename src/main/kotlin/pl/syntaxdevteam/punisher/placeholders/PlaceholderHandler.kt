package pl.syntaxdevteam.punisher.placeholders

import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import me.clip.placeholderapi.expansion.PlaceholderExpansion

@Suppress("UnstableApiUsage")
class PlaceholderHandler(private val plugin: PunisherX) : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "punisherx"
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
            "mute_remaining_time" -> getMuteEndTime(player.name) ?: ""
            "warn_remaining_time" -> getWarnEndTime(player.name) ?: ""
            "total_active_punishments" -> getAllPunishments() ?: ""
            "total_punishments" -> getAllPunishmentHistory() ?: ""
            else -> null
        }
    }

    private fun getMuteEndTime(player: String): String? {
        val uuid = plugin.uuidManager.getUUID(player)
        val punishments = plugin.databaseHandler.getPunishments(uuid.toString())
        val muteData = punishments.find { it.type == "MUTE" && it.end > System.currentTimeMillis() } ?: return null

        val remainingTime = (muteData.end - System.currentTimeMillis())  / 1000
        return if (remainingTime > 0) {
            plugin.messageHandler.getCleanMessage("placeholders", "mute_remaining_time") + plugin.timeHandler.formatTime(remainingTime.toString())
        } else {
            null
        }
    }

    private fun getWarnEndTime(player: String): String? {
        val uuid = plugin.uuidManager.getUUID(player)
        val punishments = plugin.databaseHandler.getPunishments(uuid.toString())
        val muteData = punishments.find { it.type == "WARN" && it.end > System.currentTimeMillis() } ?: return null

        val remainingTime = (muteData.end - System.currentTimeMillis())  / 1000
        return if (remainingTime > 0) {
            plugin.messageHandler.getCleanMessage("placeholders", "warn_remaining_time") + plugin.timeHandler.formatTime(remainingTime.toString())
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
