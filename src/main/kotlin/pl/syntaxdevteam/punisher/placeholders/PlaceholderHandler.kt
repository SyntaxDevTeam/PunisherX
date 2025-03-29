package pl.syntaxdevteam.punisher.placeholders

import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import java.util.concurrent.CompletableFuture

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
            "mute_remaining_time" -> getMuteEndTime(player.name).getNow("")
            "warn_remaining_time" -> getWarnEndTime(player.name).getNow("")
            "total_punishments" -> getAllPunishmentHistory().getNow("")
            "total_active_punishments" -> getAllPunishments().getNow("")
            else -> null
        }
    }

    private fun getMuteEndTime(player: String): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            val uuid = plugin.uuidManager.getUUID(player)
            val punishments = plugin.databaseHandler.getPunishments(uuid.toString())
            val muteData = punishments.find { it.type == "MUTE" && it.end > System.currentTimeMillis() } ?: return@supplyAsync null

            val remainingTime = (muteData.end - System.currentTimeMillis()) / 1000
            if (remainingTime > 0) {
                plugin.messageHandler.getCleanMessage("placeholders", "mute_remaining_time") + plugin.timeHandler.formatTime(remainingTime.toString())
            } else {
                null
            }
        }
    }

    private fun getWarnEndTime(player: String): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            val uuid = plugin.uuidManager.getUUID(player)
            val punishments = plugin.databaseHandler.getPunishments(uuid.toString())
            val warnData = punishments.find { it.type == "WARN" && it.end > System.currentTimeMillis() } ?: return@supplyAsync null

            val remainingTime = (warnData.end - System.currentTimeMillis()) / 1000
            if (remainingTime > 0) {
                plugin.messageHandler.getCleanMessage("placeholders", "warn_remaining_time") + plugin.timeHandler.formatTime(remainingTime.toString())
            } else {
                null
            }
        }
    }

    private fun getAllPunishments(): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            val totalPunishments = plugin.databaseHandler.countAllPunishments()
            plugin.messageHandler.getCleanMessage("placeholders", "total_active_punishments") + totalPunishments.toString()
        }
    }

    private fun getAllPunishmentHistory(): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            val totalPunishmentHistory = plugin.databaseHandler.countAllPunishmentHistory()
            plugin.messageHandler.getCleanMessage("placeholders", "total_punishments") + totalPunishmentHistory.toString()
        }
    }
}
