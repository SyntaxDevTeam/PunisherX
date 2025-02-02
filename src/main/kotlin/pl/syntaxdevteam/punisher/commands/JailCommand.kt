package pl.syntaxdevteam.punisher.commands

import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils

@Suppress("UnstableApiUsage")
class JailCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {

        if (!stack.sender.hasPermission("punisherx.jail")) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }
        if (args.isEmpty() || args.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("jail", "usage"))
            return
        }

        val playerName = args[0]
        val targetPlayer = Bukkit.getPlayer(playerName)
        val uuid = plugin.uuidManager.getUUID(playerName)
        val isForce = args.contains("--force")

        if (targetPlayer != null) {
            if (!isForce && targetPlayer.hasPermission("punisherx.bypass.jail")) {
                stack.sender.sendMessage(
                    plugin.messageHandler.getMessage("error", "bypass", mapOf("player" to playerName))
                )
                return
            }
        }

        var gtime: String?
        var reason: String
        try {
            gtime = args[1]
            plugin.timeHandler.parseTime(gtime)
            reason = args.slice(2 until args.size).filterNot { it == "--force" }.joinToString(" ")
        } catch (e: NumberFormatException) {
            gtime = null
            reason = args.slice(1 until args.size).filterNot { it == "--force" }.joinToString(" ")
        }

        val punishmentType = "JAIL"
        val start = System.currentTimeMillis()
        val end: Long? = if (gtime != null) start + plugin.timeHandler.parseTime(gtime) * 1000 else null

        val jailLocation = JailUtils.getJailLocation(plugin.config)
        if (jailLocation == null) {
            plugin.logger.debug("<red>No jail location found! Teleportation aborted.</red>")
            return
        }
        plugin.logger.debug("<yellow>Jail location: ${jailLocation}</yellow>")

        targetPlayer?.apply {
            if (plugin.server.name.contains("Folia")) {
                Bukkit.getServer().regionScheduler.execute(plugin, jailLocation) {
                    try {
                        if (!jailLocation.chunk.isLoaded) {
                            jailLocation.chunk.load()
                        }

                        Bukkit.getServer().globalRegionScheduler.execute(plugin) {
                            teleportAsync(jailLocation).thenAccept { success ->
                                if (success) {
                                    plugin.logger.debug("<green>Player successfully teleported to jail.</green>")
                                } else {
                                    plugin.logger.debug("<red>Failed to teleport player to jail.</red>")
                                }
                            }.exceptionally { throwable ->
                                plugin.logger.debug("<red>Teleportation error: ${throwable.message}</red>")
                                null
                            }
                        }
                    } catch (e: Exception) {
                        plugin.logger.debug("<red>Error in regionScheduler execution: ${e.message}</red>")
                    }
                }
            } else {
                if (!jailLocation.chunk.isLoaded) {
                    jailLocation.chunk.load()
                }
                try {
                    teleport(jailLocation)
                    plugin.logger.debug("<green>Player successfully teleported to jail.</green>")
                } catch (e: Exception) {
                    plugin.logger.debug("<red>Error while teleporting player: ${e.message}</red>")
                }
            }
            gameMode = GameMode.ADVENTURE

            plugin.logger.debug("Changing gamemode ($gameMode) and teleporting $name to $jailLocation")

            plugin.databaseHandler.addPunishment(name, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
            plugin.databaseHandler.addPunishmentHistory(name, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)

            plugin.cache.addOrUpdatePunishment(uuid, end ?: -1)

            sendMessage(
                plugin.messageHandler.getMessage(
                    "jail", "jail_message",
                    mapOf("reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
                )
            )
        }

        val broadcastMessage =
            plugin.messageHandler.getMessage(
                "jail", "broadcast",
                mapOf("player" to playerName, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
            )
        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (onlinePlayer.hasPermission("punisherx.see.jail")) {
                onlinePlayer.sendMessage(broadcastMessage)
            }
        }

        stack.sender.sendMessage(
            plugin.messageHandler.getMessage(
                "jail", "jail",
                mapOf("player" to playerName, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
            )
        )
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!stack.sender.hasPermission("punisherx.jail")) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> plugin.messageHandler.getReasons("jail", "reasons")
            else -> emptyList()
        }
    }

    private fun generateTimeSuggestions(): List<String> {
        val units = listOf("s", "m", "h", "d")
        return (1..999).flatMap { i -> units.map { unit -> "$i$unit" } }
    }
}
