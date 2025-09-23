package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class UnMuteCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNMUTE)) {
            if (args.isNotEmpty()) {
                val player = args[0]
                val uuid = plugin.resolvePlayerUuid(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                if (punishments.isNotEmpty()) {
                    punishments.forEach { punishment ->
                        if (punishment.type == "MUTE") {
                            plugin.databaseHandler.removePunishment(uuid, punishment.type)
                        }
                    }
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("unmute", "unmute", mapOf("player" to player)))
                    val targetPlayer = Bukkit.getPlayer(player)
                    val muteMessage = plugin.messageHandler.getMessage("unmute", "unmute_message")
                    targetPlayer?.sendMessage(muteMessage)
                    plugin.logger.info("Player $player ($uuid) has been unmuted")
                } else {
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to player)))
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.getMessage("unmute", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNMUTE)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
