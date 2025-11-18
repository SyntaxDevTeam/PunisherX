package pl.syntaxdevteam.punisher.basic

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import pl.syntaxdevteam.core.SyntaxCore
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.*

class PunishmentChecker(private val plugin: PunisherX) : Listener {

    private val updateChecker = SyntaxCore.updateChecker

    fun handlePlayerJoin(event: PlayerJoinEvent) {
        val player    = event.player
        val name      = player.name
        val uuid      = player.uniqueId
        val radius    = plugin.config.getDouble("jail.radius", 10.0)
        val jailLoc   = JailUtils.getJailLocation(plugin.config)
        val unjailLoc = JailUtils.getUnjailLocation(
            plugin.config,
            plugin.hookHandler,
            player = player,
            safeTeleportService = plugin.safeTeleportService
        )
        if (PermissionChecker.isAuthor(uuid)) {
            player.sendMessage(
                plugin.messageHandler
                .formatMixedTextToMiniMessage(plugin.messageHandler.getPrefix() + " <green>Witaj, <b>$name</b><newline>    Ten serwer używa ${plugin.pluginMeta.name} ${plugin.pluginMeta.version} ❤",
                        TagResolver.empty())
            )
        }
        val locationsConfigured = jailLoc != null && unjailLoc != null
        if (!locationsConfigured) {
            plugin.logger.warning("Jail lub unjail location niezdefiniowane!")
        } else {
            plugin.schedulerAdapter.runAsync {
                val punishments = plugin.databaseHandler.getPunishments(uuid.toString())
                val isJailed = punishments.any { it.type == "JAIL" && plugin.punishmentManager.isPunishmentActive(it) }

                plugin.schedulerAdapter.runSync {
                    if (!player.isOnline) {
                        plugin.logger.debug("Pomijam teleportację ${player.name}, gracz jest offline")
                        return@runSync
                    }

                    val (targetLoc, targetMode) = when {
                        isJailed -> {
                            jailLoc to GameMode.ADVENTURE
                        }
                        isPlayerInJail(player.location, jailLoc, radius) -> {
                            unjailLoc to GameMode.SURVIVAL
                        }
                        else -> {
                            return@runSync
                        }
                    }

                    if (targetLoc.world == null) {
                        plugin.logger.warning("Brak świata dla $targetLoc")
                        return@runSync
                    }

                    plugin.safeTeleportService.teleportSafely(player, targetLoc) { success ->
                        if (success) {
                            player.gameMode = targetMode
                            plugin.logger.debug("Player ${player.name} teleported to $targetLoc and set to $targetMode")
                        } else {
                            plugin.logger.debug("<red>Failed to teleport ${player.name} to $targetLoc.</red>")
                        }
                    }
                    plugin.logger.debug("Scheduling teleport of $name to $targetLoc with gamemode $targetMode (jailed=$isJailed)")
                }
            }
        }

        if (PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.SEE_UPDATE)) {
            updateChecker.checkForUpdatesForPlayer(player)
            plugin.logger.debug("Checking for updates for player: $name")
        } else {
            plugin.logger.debug("Player $name does not have permission to see updates.")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        val playerName = player.name
        try {
            val uuid = player.uniqueId.toString()
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
                    val infoMessage = plugin.messageHandler.stringMessageToComponent(
                        messageKey,
                        "mute_info_message",
                        mapOf("reason" to reason, "time" to remainingTime)
                    )
                    val logMessage = plugin.messageHandler.stringMessageToComponentNoPrefix(
                        messageKey,
                        "log",
                        mapOf("player" to playerName, "message" to plainMessage)
                    )

                    plugin.logger.clearLog(logMessage)
                    player.sendMessage(infoMessage)
                    return
                } else {
                    plugin.databaseHandler.removePunishment(uuid, "MUTE", true)
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
            val uuid = player.uniqueId.toString()
            val originalCommand = event.message.split(" ")[0]
            val command = originalCommand.lowercase(Locale.getDefault()).removePrefix("/")
            val punishments = plugin.databaseHandler.getPunishments(uuid)

            if (plugin.config.getBoolean("mute.pm")) {
                val muteCommands = plugin.config.getStringList("mute.cmd")
                if (muteCommands.contains(command)) {
                    punishments.forEach { punishment ->
                        if (punishment.type == "MUTE" && plugin.punishmentManager.isPunishmentActive(punishment)) {
                            val endTime = punishment.end
                            val remainingTime = (endTime - System.currentTimeMillis()) / 1000
                            val duration = if (endTime == -1L) "permanent" else plugin.timeHandler.formatTime(remainingTime.toString())
                            val reason = punishment.reason
                            event.isCancelled = true
                            val muteMessage = plugin.messageHandler.stringMessageToComponent("mute", "mute_info_message", mapOf("reason" to reason, "time" to duration))

                            player.sendMessage(muteMessage)
                        } else if (punishment.type == "MUTE") {
                            plugin.databaseHandler.removePunishment(uuid, "MUTE", true)
                            plugin.logger.debug("Punishment for UUID: $uuid has expired and has been removed")
                        }
                    }
                }
            }

            var activeJailFound = false
            punishments.forEach { punishment ->
                if (punishment.type != "JAIL") {
                    return@forEach
                }

                if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                    activeJailFound = true
                } else {
                    plugin.databaseHandler.removePunishment(uuid, "JAIL", true)
                    plugin.logger.debug("Punishment for UUID: $uuid has expired and has been removed")
                }
            }

            if (!activeJailFound) {
                return
            }

            if (PermissionChecker.hasWithBypass(player, PermissionChecker.PermissionKey.BYPASS_JAIL)) {
                return
            }

            val allowedCommands = plugin.config.getStringList("jail.allowed_commands")
                .map { it.lowercase(Locale.getDefault()).removePrefix("/") }
                .toSet()

            val allowAllCommands = allowedCommands.contains("*")
            val commandAliases = mutableSetOf(command)
            if (":" in command) {
                commandAliases.add(command.substringAfter(":"))
            }

            val isAllowed = allowAllCommands || commandAliases.any { it in allowedCommands }

            if (!isAllowed) {
                event.isCancelled = true
                val message = plugin.messageHandler.stringMessageToComponent("jail", "command_blocked_message")
                player.sendMessage(message)
                plugin.logger.debug("Blocked command '$originalCommand' for jailed player ${player.name}")
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
        val uuid = player.uniqueId
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
        if (isPlayerInJail(player.location, jailLocation, radius)) {
            return
        }

        plugin.safeTeleportService.teleportSafely(player, jailLocation) { success ->
            if (success) {
                val message = plugin.messageHandler.stringMessageToComponent("jail", "jail_restrict_message", emptyMap())
                player.sendMessage(message)
            } else {
                plugin.logger.debug("<red>Failed to teleport player back to jail.</red>")
            }
        }
    }
}
