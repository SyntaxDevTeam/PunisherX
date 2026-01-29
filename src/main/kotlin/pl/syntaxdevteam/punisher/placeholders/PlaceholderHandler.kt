package pl.syntaxdevteam.punisher.placeholders

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import pl.syntaxdevteam.message.MessageHandler
import java.text.SimpleDateFormat
import java.util.Date

class PlaceholderHandler(private val plugin: PunisherX) : PlaceholderExpansion() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val placeholderFormat: MessageHandler.MessageFormat? by lazy { resolvePlaceholderFormat() }

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
        resolveTargetName(player, params, "active_punishments_list")?.let {
            return getPunishmentList(it, includeHistory = false)
        }

        resolveTargetName(player, params, "punishment_history_list")?.let {
            return getPunishmentList(it, includeHistory = true)
        }

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
            placeholderMessage(punishment) + plugin.timeHandler.formatTime(remainingTime.toString())
        } else {
            null
        }
    }

    private fun resolveTargetName(player: Player?, params: String, baseKey: String): String? {
        if (params == baseKey) {
            return player?.name
        }

        if (params.startsWith("${baseKey}_")) {
            return params.removePrefix("${baseKey}_")
        }

        return null
    }

    private fun getPunishmentList(playerName: String, includeHistory: Boolean): String? {
        if (playerName.isBlank()) {
            return ""
        }

        val uuid = plugin.resolvePlayerUuid(playerName).toString()
        val limit = plugin.config.getInt("placeholders.punishment_list_limit").takeIf { it > 0 } ?: 5

        val punishments = if (includeHistory) {
            plugin.databaseHandler.getPunishmentHistory(uuid, limit, 0)
        } else {
            plugin.databaseHandler.getPunishments(uuid, limit, 0)
        }.take(limit)

        if (punishments.isEmpty()) {
            return placeholderMessage("punishment_list_empty")
        }

        val entryTemplate = placeholderMessage("punishment_list_entry")
        val formattedEntries = punishments.joinToString("\n") { formatPunishmentEntry(entryTemplate, it) }

        val wrapperKey = if (includeHistory) "punishment_history_list" else "active_punishments_list"
        return placeholderMessage(wrapperKey)
            .replace("<limit>", limit.toString())
            .replace("<list>", formattedEntries)
    }

    private fun formatPunishmentEntry(template: String, punishment: PunishmentData): String {
        val end = if (punishment.end <= 0) {
            placeholderMessage("punishment_list_permanent")
        } else {
            dateFormat.format(Date(punishment.end))
        }

        return template
            .replace("<type>", punishment.type)
            .replace("<reason>", punishment.reason)
            .replace("<operator>", punishment.operator)
            .replace("<start>", dateFormat.format(Date(punishment.start)))
            .replace("<end>", end)
    }

    private fun getAllPunishments(): String? {
        val totalPunishments = plugin.databaseHandler.countAllPunishments()
        return if (totalPunishments > 0) {
        plugin.logger.debug("Total active punishments: $totalPunishments")
        plugin.logger.debug("Formatting message for total active punishments: ${plugin.messageHandler.stringMessageToStringNoPrefix("placeholders", "total_active_punishments")}")
        placeholderMessage("total_active_punishments") + totalPunishments.toString()
        } else {
            null
        }

    }

    private fun getAllPunishmentHistory(): String? {
        val totalPunishmentHistory = plugin.databaseHandler.countAllPunishmentHistory()
        return if (totalPunishmentHistory > 0) {
            plugin.logger.debug("Total punishments: $totalPunishmentHistory")
            plugin.logger.debug("Formatting message for total punishments: ${plugin.messageHandler.stringMessageToStringNoPrefix("placeholders", "total_punishments")}")
            placeholderMessage("total_punishments").let {
                it + totalPunishmentHistory.toString()
            }
        } else {
            null
        }
    }

    private fun placeholderMessage(key: String, placeholders: Map<String, String> = emptyMap()): String {
        return placeholderFormat?.let {
            plugin.messageHandler.stringMessageToStringNoPrefix("placeholders", key, it, placeholders)
        } ?: plugin.messageHandler.stringMessageToStringNoPrefix("placeholders", key, placeholders)
    }

    private fun resolvePlaceholderFormat(): MessageHandler.MessageFormat? {
        val configured = plugin.config.getString("placeholders.message_format")?.trim()?.uppercase() ?: return null
        return runCatching { MessageHandler.MessageFormat.valueOf(configured) }
            .getOrElse {
                plugin.logger.warning("Unknown placeholders.message_format '$configured'. Using default MessageHandler formatting.")
                null
            }
    }
}
