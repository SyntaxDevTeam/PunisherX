package pl.syntaxdevteam.punisher.basic

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.UpdateChecker
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.*

class PunishmentChecker(private val plugin: PunisherX) : Listener {

    private val updateChecker = UpdateChecker(plugin)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player    = event.player
        val name      = player.name
        val uuid      = plugin.uuidManager.getUUID(name).toString()
        val radius    = plugin.config.getDouble("jail.radius", 10.0)
        val jailLoc   = JailUtils.getJailLocation(plugin.config)
        val unjailLoc = JailUtils.getUnjailLocation(plugin.config)

        // 1. Sprawdź, czy gracz ma aktywną karę JAIL
        val punishments = plugin.databaseHandler.getPunishments(uuid)
        val isJailed = punishments.any { it.type == "JAIL" && plugin.punishmentManager.isPunishmentActive(it) }

        if (jailLoc == null || unjailLoc == null) {
            plugin.logger.warning("Jail lub unjail location niezdefiniowane!")
        } else {

            val (targetLoc, targetMode) = when {
                isJailed -> {
                    jailLoc   to GameMode.ADVENTURE
                }
                isPlayerInJail(player.location, jailLoc, radius) -> {
                    unjailLoc to GameMode.SURVIVAL
                }
                else -> {
                    return
                }
            }

            scheduleTeleport(player, targetLoc, targetMode)
            plugin.logger.debug("Scheduling teleport of $name to $targetLoc with gamemode $targetMode (jailed=$isJailed)")
        }

        if (PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.SEE_UPDATE)) {
            updateChecker.checkForUpdatesForPlayer(player)
            plugin.logger.debug("Checking for updates for player: $name")
        } else {
            plugin.logger.debug("Player $name does not have permission to see updates.")
        }
    }

    private fun scheduleTeleport(player: Player, targetLoc: Location, mode: GameMode) {
        val world = targetLoc.world ?: run {
            plugin.logger.warning("Brak świata dla $targetLoc")
            return
        }

        if (plugin.server.name.contains("Folia")) {
            Bukkit.getServer().regionScheduler.execute(plugin, targetLoc) {
                world.getChunkAtAsync(targetLoc.blockX shr 4, targetLoc.blockZ shr 4).thenRun {
                    Bukkit.getServer().globalRegionScheduler.execute(plugin) {
                        player.teleportAsync(targetLoc).thenAccept { success ->
                            if (success) {
                                player.gameMode = mode
                                plugin.logger.debug("Player ${player.name} teleported to $targetLoc and set to $mode")
                            } else {
                                plugin.logger.debug("<red>Failed to teleport ${player.name} to $targetLoc.</red>")
                            }
                        }.exceptionally { thr ->
                            plugin.logger.debug("<red>Teleportation error: ${thr.message}</red>")
                            null
                        }
                    }
                }.exceptionally { thr ->
                    plugin.logger.debug("<red>Chunk load error: ${thr.message}</red>")
                    null
                }
            }
        } else {
            val chunk = world.getChunkAt(targetLoc.blockX shr 4, targetLoc.blockZ shr 4)
            if (!chunk.isLoaded) {
                try {
                    chunk.load(true)
                } catch (e: Exception) {
                    plugin.logger.debug("<red>Error loading chunk for ${player.name}: ${e.message}</red>")
                }
            }
            try {
                player.teleport(targetLoc)
                player.gameMode = mode
                plugin.logger.debug("<green>Player ${player.name} teleported to $targetLoc and set to $mode.</green>")
            } catch (e: Exception) {
                plugin.logger.debug("<red>Error while teleporting ${player.name}: ${e.message}</red>")
            }
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
                    val logMessage = plugin.messageHandler.getLogMessage(
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

                            player.sendMessage(muteMessage)
                        } else {
                            plugin.databaseHandler.removePunishment(uuid, "MUTE", true)
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
                                    player.sendMessage(message)
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
                player.sendMessage(message)
            }
        }
    }
}
