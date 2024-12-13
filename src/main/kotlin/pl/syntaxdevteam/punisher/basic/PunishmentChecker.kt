package pl.syntaxdevteam.punisher.basic

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import pl.syntaxdevteam.punisher.PunisherX
import java.util.*

class PunishmentChecker(private val plugin: PunisherX) : Listener {


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        try {
            plugin.logger.debug("Checking punishment for player: ${event.name}")

            val uuid = plugin.uuidManager.getUUID(event.name).toString()
            val ip = event.address.hostAddress

            val punishments = plugin.databaseHandler.getPunishments(uuid) + plugin.databaseHandler.getPunishmentsByIP(ip)
            if (punishments.isEmpty()) {
                plugin.logger.debug("No punishments found for player: ${event.name}")
                return
            }

            punishments.forEach { punishment ->
                if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                    if (punishment.type == "BAN" || punishment.type == "BANIP") {
                        val endTime = punishment.end
                        val remainingTime = (endTime - System.currentTimeMillis()) / 1000
                        val duration = if (endTime == -1L) "permanent" else plugin.timeHandler.formatTime(remainingTime.toString())
                        val reason = punishment.reason
                        val kickMessages = when (punishment.type) {
                            "BAN" -> plugin.messageHandler.getComplexMessage("ban", "kick_message", mapOf("reason" to reason, "time" to duration))
                            "BANIP" -> plugin.messageHandler.getComplexMessage("banip", "kick_message", mapOf("reason" to reason, "time" to duration))
                            else -> emptyList()
                        }
                        val kickMessage = Component.text()
                        kickMessages.forEach { line ->
                            kickMessage.append(line)
                            kickMessage.append(Component.newline())
                        }
                        event.loginResult = AsyncPlayerPreLoginEvent.Result.KICK_BANNED
                        event.kickMessage(kickMessage.build())
                        plugin.logger.debug("Player ${event.name} was kicked for: $reason")
                    }
                } else {
                    plugin.databaseHandler.removePunishment(uuid, punishment.type, true)
                    plugin.logger.debug("Punishment for UUID: $uuid has expired and has been removed")
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in onPlayerPreLogin, report it urgently to the plugin author with the message: ${event.name}: ${e.message}")
            e.printStackTrace()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerChat(event: AsyncChatEvent) {
        try {
            val player = event.player
            val playerName = player.name
            val uuid = plugin.uuidManager.getUUID(playerName).toString()
            val messageComponent = event.message()
            val plainMessage = PlainTextComponentSerializer.plainText().serialize(messageComponent)
            val punishments = plugin.databaseHandler.getPunishments(uuid)
            if (punishments.isEmpty()) {
                plugin.logger.debug("No punishments found for player: ${player.name}")
                return
            }

            punishments.forEach { punishment ->
                if (punishment.type == "MUTE" && plugin.punishmentManager.isPunishmentActive(punishment)) {
                    val endTime = punishment.end
                    val remainingTime = (endTime - System.currentTimeMillis()) / 1000
                    val duration = if (endTime == -1L) "permanent" else plugin.timeHandler.formatTime(remainingTime.toString())
                    val reason = punishment.reason
                    event.isCancelled = true
                    val muteMessage = plugin.messageHandler.getMessage("mute", "mute_info_message", mapOf("reason" to reason, "time" to duration))
                    val formattedMessage = MiniMessage.miniMessage().deserialize(muteMessage)
                    val logMessage = plugin.messageHandler.getLogMessage("mute", "log", mapOf("player" to playerName, "message" to plainMessage))
                    val logFormattedMessage = MiniMessage.miniMessage().deserialize(logMessage)
                    plugin.logger.clearLog(logFormattedMessage)
                    player.sendMessage(formattedMessage)
                    return
                } else {
                    plugin.databaseHandler.removePunishment(uuid, punishment.type, true)
                    plugin.logger.debug("Punishment for UUID: $uuid has expired and has been removed")
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in onPlayerChat, report it urgently to the plugin author with the message: ${e.message}")
            e.printStackTrace()
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        try {
            val player = event.player
            val uuid = player.uniqueId.toString()
            val command = event.message.split(" ")[0].lowercase(Locale.getDefault()).removePrefix("/")

            if (plugin.config.getBoolean("mute_pm")) {
                val muteCommands = plugin.config.getStringList("mute_cmd")
                if (muteCommands.contains(command)) {
                    val punishments = plugin.databaseHandler.getPunishments(uuid)
                    punishments.forEach { punishment ->
                        if (punishment.type == "MUTE" && plugin.punishmentManager.isPunishmentActive(punishment)) {
                            val endTime = punishment.end
                            val remainingTime = (endTime - System.currentTimeMillis()) / 1000
                            val duration = if (endTime == -1L) "permanent" else plugin.timeHandler.formatTime(remainingTime.toString())
                            val reason = punishment.reason
                            event.isCancelled = true
                            val muteMessage = plugin.messageHandler.getMessage("mute", "mute_message", mapOf("reason" to reason, "time" to duration))
                            val formattedMessage = MiniMessage.miniMessage().deserialize(muteMessage)
                            player.sendMessage(formattedMessage)
                        } else {
                            plugin.databaseHandler.removePunishment(uuid, punishment.type, true)
                            plugin.logger.debug("Punishment for UUID: $uuid has expired and has been removed")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in onPlayerCommand, report it urgently to the plugin author with the message: ${event.player.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}
