package pl.syntaxdevteam.punisher.commands

import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

@Suppress("UnstableApiUsage")
class UnjailCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNJAIL)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }
        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("unjail", "usage"))
            return
        }

        val playerName = args[0]
        val uuid = plugin.uuidManager.getUUID(playerName)

        val player = Bukkit.getPlayer(uuid)
        if (player != null) {

            val spawnLocation = JailUtils.getUnjailLocation(plugin.config) ?: return

            if (plugin.server.name.contains("Folia")) {
                Bukkit.getServer().globalRegionScheduler.execute(plugin) {
                    try {
                        player.teleportAsync(spawnLocation).thenAccept { success ->
                            if (success) {
                                player.gameMode = GameMode.SURVIVAL
                                player.sendMessage(plugin.messageHandler.getMessage("unjail", "unjail_message"))
                                plugin.logger.debug("<green>Player $playerName successfully unjailed (Folia).</green>")
                            } else {
                                plugin.logger.debug("<red>Failed to teleport player $playerName during unjail (Folia).</red>")
                            }
                        }.exceptionally { throwable ->
                            plugin.logger.debug("<red>Error while teleporting $playerName during unjail: ${throwable.message}</red>")
                            null
                        }
                    } catch (e: Exception) {
                        plugin.logger.debug("<red>Error during Folia unjail for $playerName: ${e.message}</red>")
                    }
                }
            } else {
                player.teleport(spawnLocation)
                player.gameMode = GameMode.SURVIVAL
                player.sendMessage(plugin.messageHandler.getMessage("unjail", "unjail_message"))
            }
        }

        plugin.cache.removePunishment(uuid)
        plugin.databaseHandler.removePunishment(uuid.toString(), "JAIL")

        val broadcastMessage =
            plugin.messageHandler.getMessage("unjail", "broadcast", mapOf("player" to playerName))

        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_UNJAIL)) {
                onlinePlayer.sendMessage(broadcastMessage)
            }
        }

        stack.sender.sendMessage(
            plugin.messageHandler.getMessage("unjail", "success", mapOf("player" to playerName))
        )
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNJAIL)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
