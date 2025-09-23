package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class ClearAllCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CLEAR_ALL)) {
            if (args.isNotEmpty()) {
                val player = args[0]
                val uuid = plugin.resolvePlayerUuid(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                if (punishments.isNotEmpty()) {
                    punishments.forEach { punishment ->
                        if (punishment.type == "MUTE" || punishment.type == "BAN" || punishment.type == "WARN") {
                            plugin.databaseHandler.removePunishment(uuid, punishment.type, true)
                        }
                    }
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("clear", "clearall", mapOf("player" to player)))
                    val targetPlayer = Bukkit.getPlayer(player)
                    val getMessage = plugin.messageHandler.getMessage("clear", "clear_message")
                    targetPlayer?.sendMessage(getMessage)
                    plugin.logger.success("Player $player ($uuid) has been cleared of all punishments")
                } else {
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to player)))
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.getMessage("clear", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CLEAR_ALL)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
