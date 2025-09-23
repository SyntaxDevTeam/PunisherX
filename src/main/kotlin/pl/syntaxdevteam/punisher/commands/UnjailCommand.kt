package pl.syntaxdevteam.punisher.commands

import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.common.TeleportUtils

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
        val uuid = plugin.resolvePlayerUuid(playerName)
        val player = Bukkit.getPlayer(uuid)

        if (player != null) {

            val spawnLocation = JailUtils.getUnjailLocation(plugin.config) ?: return

            TeleportUtils.teleportSafely(plugin, player, spawnLocation) { success ->
                if (success) {
                    player.gameMode = GameMode.SURVIVAL
                    plugin.messageHandler.getSmartMessage(
                        "unjail",
                        "unjail_message"
                    ).forEach { msg -> player.sendMessage(msg) }
                    plugin.logger.debug("<green>Player $playerName successfully unjailed.</green>")
                } else {
                    plugin.logger.debug("<red>Failed to teleport player $playerName during unjail.</red>")
                }
            }
        }

        plugin.cache.removePunishment(uuid)
        plugin.databaseHandler.removePunishment(uuid.toString(), "JAIL")

        val broadcastMessages =
            plugin.messageHandler.getSmartMessage("unjail", "broadcast", mapOf("player" to playerName))

        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_UNJAIL)) {
                broadcastMessages.forEach { msg -> onlinePlayer.sendMessage(msg) }
            }
        }

        plugin.messageHandler.getSmartMessage(
            "unjail",
            "success",
            mapOf("player" to playerName)
        ).forEach { stack.sender.sendMessage(it) }
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
