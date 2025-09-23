@file:Suppress("DEPRECATION")

package pl.syntaxdevteam.punisher.listeners

import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import pl.syntaxdevteam.punisher.PunisherX

class LegacyLoginListener(private val plugin: PunisherX) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val player = event.player
        try {
            plugin.logger.debug("Checking punishment for player: ${player.name}")

            val uuid = player.uniqueId.toString()
            val ip = event.address.hostAddress

            val punishments = plugin.databaseHandler.getPunishments(uuid) + plugin.databaseHandler.getPunishmentsByIP(ip)
            if (punishments.isEmpty()) {
                plugin.logger.debug("No punishments found for player: ${event.player.name}")
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
                            "BAN" -> plugin.messageHandler.getSmartMessage(
                                "ban",
                                "kick_message",
                                mapOf("reason" to reason, "time" to duration)
                            )
                            "BANIP" -> plugin.messageHandler.getSmartMessage(
                                "banip",
                                "kick_message",
                                mapOf("reason" to reason, "time" to duration)
                            )
                            else -> emptyList()
                        }
                        val kickMessage = Component.text()
                        kickMessages.forEach { line ->
                            kickMessage.append(line)
                            kickMessage.append(Component.newline())
                        }
                        event.disallow(PlayerLoginEvent.Result.KICK_BANNED, kickMessage.build())
                        plugin.logger.debug("Player ${event.player.name} was kicked for: $reason")
                    }
                } else {
                    plugin.databaseHandler.removePunishment(uuid, punishment.type, true)
                    plugin.logger.debug("Punishment for UUID: $uuid has expired and has been removed")
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in onPlayerPreLogin, report it urgently to the plugin author with the message: ${event.player.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}
