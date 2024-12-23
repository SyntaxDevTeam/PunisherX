package pl.syntaxdevteam.punisher.basic

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerMoveEvent
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
        val player = event.player
        val playerName = player.name
        try {
            val uuid = plugin.uuidManager.getUUID(playerName).toString()
            val messageComponent = event.message()
            val plainMessage = PlainTextComponentSerializer.plainText().serialize(messageComponent)

            val punishments = plugin.databaseHandler.getPunishments(uuid)
            if (punishments.isEmpty()) {
                plugin.logger.debug("No punishments found for player: $playerName")
                return
            }

            for (punishment in punishments) {
                if ((punishment.type == "MUTE" || punishment.type == "JAIL") && plugin.punishmentManager.isPunishmentActive(punishment)) {
                    event.isCancelled = true
                    val endTime = punishment.end
                    val remainingTime = if (endTime == -1L) "permanent" else plugin.timeHandler.formatTime(((endTime - System.currentTimeMillis()) / 1000).toString())
                    val reason = punishment.reason
                    val messageKey = if (punishment.type == "JAIL") "jail" else "mute"
                    val infoMessage = plugin.messageHandler.getMessage(
                        messageKey,
                        "mute_info_message",
                        mapOf("reason" to reason, "time" to remainingTime)
                    )
                    val formattedMessage = MiniMessage.miniMessage().deserialize(infoMessage)
                    val logMessage = plugin.messageHandler.getLogMessage(
                        messageKey,
                        "log",
                        mapOf("player" to playerName, "message" to plainMessage)
                    )
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
            plugin.logger.severe("Error in onPlayerChat, report it urgently to the plugin author: ${e.message}")
            e.printStackTrace()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        try {
            val player = event.player
            val uuid = plugin.uuidManager.getUUID(player.name).toString()
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

    private fun isPlayerInJail(playerLocation: Location, jailCenter: Location, radius: Double): Boolean {
        return playerLocation.world == jailCenter.world &&
                playerLocation.distance(jailCenter) <= radius
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val uuid = plugin.uuidManager.getUUID(player.name)
        val jailLocation = JailUtils.getJailLocation(plugin.config) ?: return
        val punishmentEnd = plugin.cache.getPunishmentEnd(uuid) ?: return

        if (!plugin.cache.isPlayerInCache(uuid)) {
            return
        }

        if (punishmentEnd != -1L && punishmentEnd < System.currentTimeMillis()) {
            plugin.cache.removePunishment(uuid)
            player.gameMode = GameMode.SURVIVAL
            return
        }

        val radius = plugin.config.getDouble("jail.radius", 10.0)
        if (plugin.server.name.contains("Folia")) {
            Bukkit.getServer().regionScheduler.execute(plugin, jailLocation) {
                try {
                    val isInJail = isPlayerInJail(player.location, jailLocation, radius)
                    if (!isInJail) {
                        Bukkit.getServer().globalRegionScheduler.execute(plugin) {
                            player.teleportAsync(jailLocation).thenAccept { success ->
                                if (success) {
                                    val message = plugin.messageHandler.getMessage(
                                        "jail",
                                        "jail_restrict_message",
                                        mapOf()
                                    )
                                    player.sendRichMessage(message)
                                } else {
                                    plugin.logger.debug("<red>Failed to teleport player back to jail in Folia.</red>")
                                }
                            }.exceptionally { throwable ->
                                plugin.logger.debug("<red>Error while teleporting back to jail: ${throwable.message}</red>")
                                null
                            }
                        }
                    }
                } catch (e: Exception) {
                    plugin.logger.debug("<red>Error while checking player in jail: ${e.message}</red>")
                }
            }
        } else {
            if (!isPlayerInJail(player.location, jailLocation, radius)) {
                player.teleport(jailLocation)
                val message = plugin.messageHandler.getMessage("jail", "jail_restrict_message", mapOf())
                player.sendRichMessage(message)
            }
        }
    }
}
